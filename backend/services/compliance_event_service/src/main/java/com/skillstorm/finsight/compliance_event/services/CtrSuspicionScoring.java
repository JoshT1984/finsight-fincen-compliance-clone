package com.skillstorm.finsight.compliance_event.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.skillstorm.finsight.compliance_event.repositories.CtrAggregationRow;
import com.skillstorm.finsight.compliance_event.repositories.CtrRiskSignalsRow;

public final class CtrSuspicionScoring {

    private CtrSuspicionScoring() {}

    public record ScoreResult(int score, String band, List<String> drivers) {}


    public static ScoreResult score(CtrAggregationRow agg, CtrRiskSignalsRow signals, CtrRiskSignalsRow windowSignals) {
        int score = 0;
        List<String> drivers = new ArrayList<>();

        BigDecimal totalIn = nz(agg.getTotalCashIn());
        BigDecimal totalOut = nz(agg.getTotalCashOut());

        // 1) Cash deposit > $10K
        if (totalIn.compareTo(new BigDecimal("10000.00")) > 0) {
            score += 20;
            drivers.add("CASH_IN_OVER_10K");
        }

        // Optional symmetry: cash out > $10K
        if (totalOut.compareTo(new BigDecimal("10000.00")) > 0) {
            score += 20;
            drivers.add("CASH_OUT_OVER_10K");
        }

        // 2) Structuring pattern: multiple deposits, total > 10K, max single deposit < 10K
        if (signals != null) {
            BigDecimal maxIn = nz(signals.getMaxCashIn());
            Long txnCountBoxed = signals.getTxnCount();
            long txnCount = txnCountBoxed != null ? txnCountBoxed.longValue() : 0L;
            if (totalIn.compareTo(new BigDecimal("10000.00")) > 0
                && txnCount >= 2
                && maxIn.compareTo(new BigDecimal("10000.00")) < 0) {
                score += 30;
                drivers.add("STRUCTURING_PATTERN");
            }

            // 3) Multiple same-day branch usage (distinct locations > 1)
            Long locsBoxed = signals.getDistinctLocationCount();
            long locs = locsBoxed != null ? locsBoxed.longValue() : 0L;
            if (locs > 1) {
                score += 20;
                drivers.add("MULTI_LOCATION_SAME_DAY");
            }

            // 4) Velocity spike (simple v1 proxy): high txn count in a day
            if (txnCount >= 5) {
                score += 15;
                drivers.add("VELOCITY_SPIKE_PROXY");
            }
        }

        // 5) 72-hour window: high frequency cash activity
        if (windowSignals != null) {
            Long windowTxnCountBoxed = windowSignals.getTxnCount();
            long windowTxnCount = windowTxnCountBoxed != null ? windowTxnCountBoxed.longValue() : 0L;
            if (windowTxnCount >= 10) {
                score += 15;
                drivers.add("High frequency cash activity within 72 hours");
            }
        }

        if (score > 100) score = 100;

        String band = bandFor(score);
        return new ScoreResult(score, band, drivers);
    }

    public static String bandFor(int score) {
        if (score >= 60) return "HIGH";
        if (score >= 40) return "REVIEW";
        return "LOW";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
