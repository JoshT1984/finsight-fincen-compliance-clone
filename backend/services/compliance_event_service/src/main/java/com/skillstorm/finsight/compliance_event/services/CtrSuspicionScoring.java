package com.skillstorm.finsight.compliance_event.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.skillstorm.finsight.compliance_event.repositories.CtrAggregationRow;
import com.skillstorm.finsight.compliance_event.repositories.CtrRiskSignalsRow;

public final class CtrSuspicionScoring {

    private CtrSuspicionScoring() {
    }

    public record ScoreResult(int score, String band, List<String> drivers) {
    }

    /**
     * Optional context signals that may come from other services (customer profile,
     * KYC, device graph, etc.).
     *
     * In the current MVP these are passed in as booleans so the scoring "triggers"
     * are already in place,
     * even if upstream data sources are not yet wired.
     */
    public record ExternalSignals(
            boolean addressMismatch,
            boolean linkedAccounts,
            boolean highRiskGeography,
            java.math.BigDecimal baselineAvgDailyCashTotal) {
        public static ExternalSignals empty() {
            return new ExternalSignals(false, false, false, null);
        }
    }

    public static ScoreResult score(
            CtrAggregationRow agg,
            CtrRiskSignalsRow signals,
            CtrRiskSignalsRow windowSignals,
            ExternalSignals external) {
        int score = 0;
        List<String> drivers = new ArrayList<>();

        if (external == null) {
            external = ExternalSignals.empty();
        }

        BigDecimal totalIn = nz(agg.getTotalCashIn());
        BigDecimal totalOut = nz(agg.getTotalCashOut());

        // 1) Cash deposit over $10,000 (+20)
        if (totalIn.compareTo(new BigDecimal("10000.00")) > 0) {
            score += 20;
            drivers.add("CASH_DEPOSIT_OVER_10K");
        }

        // Optional symmetry: cash out > $10K
        if (totalOut.compareTo(new BigDecimal("10000.00")) > 0) {
            score += 20;
            drivers.add("CASH_OUT_OVER_10K");
        }

        // 2) Structuring pattern detected (+30)
        // Heuristic: 2+ deposits "just under" threshold in a day.
        if (signals != null) {
            BigDecimal maxIn = nz(signals.getMaxCashIn());
            Long txnCountBoxed = signals.getTxnCount();
            long txnCount = txnCountBoxed != null ? txnCountBoxed.longValue() : 0L;

            Long nearThreshBoxed = signals.getCashInNearThresholdCount();
            long nearThresh = nearThreshBoxed != null ? nearThreshBoxed.longValue() : 0L;

            if (nearThresh >= 2 || (totalIn.compareTo(new BigDecimal("10000.00")) > 0 && txnCount >= 2
                    && maxIn.compareTo(new BigDecimal("10000.00")) < 0)) {
                score += 30;
                drivers.add("STRUCTURING_PATTERN");
            }

            // 3) Multiple same-day branch usage (+20)
            Long locsBoxed = signals.getDistinctLocationCount();
            long locs = locsBoxed != null ? locsBoxed.longValue() : 0L;
            if (locs > 1) {
                score += 20;
                drivers.add("MULTIPLE_SAME_DAY_BRANCH_USAGE");
            }
        }

        // 4) Unregistered or mismatched address (+25)
        if (external.addressMismatch()) {
            score += 25;
            drivers.add("UNREGISTERED_OR_MISMATCHED_ADDRESS");
        }

        // 5) Velocity spike (+15)
        // Rule: cash activity exceeds 3x customer baseline.
        if (external.baselineAvgDailyCashTotal != null
                && external.baselineAvgDailyCashTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal todayTotal = nz(agg.getTotalCashAmount());
            BigDecimal threshold = external.baselineAvgDailyCashTotal.multiply(new BigDecimal("3.0"));
            if (todayTotal.compareTo(threshold) > 0) {
                score += 15;
                drivers.add("VELOCITY_SPIKE");
            }
        }

        // 6) Linked accounts detected (+20)
        if (external.linkedAccounts()) {
            score += 20;
            drivers.add("LINKED_ACCOUNTS_DETECTED");
        }

        // 7) High-risk geography involvement (+25)
        if (external.highRiskGeography()) {
            score += 25;
            drivers.add("HIGH_RISK_GEOGRAPHY_INVOLVEMENT");
        }

        // Additional (supporting) signal: 72-hour high frequency cash activity
        if (windowSignals != null) {
            Long windowTxnCountBoxed = windowSignals.getTxnCount();
            long windowTxnCount = windowTxnCountBoxed != null ? windowTxnCountBoxed.longValue() : 0L;
            if (windowTxnCount >= 10) {
                score += 15;
                drivers.add("HIGH_FREQUENCY_CASH_ACTIVITY_72H");
            }
        }

        if (score > 100)
            score = 100;

        String band = bandFor(score);
        return new ScoreResult(score, band, drivers);
    }

    public static String bandFor(int score) {
        if (score >= 60)
            return "HIGH";
        if (score >= 40)
            return "REVIEW";
        return "LOW";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
