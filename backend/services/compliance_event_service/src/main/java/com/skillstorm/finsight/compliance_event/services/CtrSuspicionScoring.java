package com.skillstorm.finsight.compliance_event.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.skillstorm.finsight.compliance_event.repositories.CtrAggregationRow;
import com.skillstorm.finsight.compliance_event.repositories.CtrRiskSignalsRow;

public final class CtrSuspicionScoring {

    private CtrSuspicionScoring() {
    }

    // Tunables
    private static final BigDecimal TEN_K = new BigDecimal("10000.00");
    private static final BigDecimal TWELVE_K = new BigDecimal("12000.00");
    private static final BigDecimal FIFTY_K = new BigDecimal("50000.00");

    public enum Driver {
        CASH_DEPOSIT_OVER_10K,
        CASH_DEPOSIT_OVER_12K,
        STRUCTURING_PATTERN,
        MULTIPLE_SAME_DAY_BRANCH_USAGE,
        HIGH_FREQUENCY_72H,
        UNUSUAL_VS_BASELINE
    }

    public record ExternalSignals(
            boolean watchlistHit,
            boolean priorSars,
            boolean priorCases,
            BigDecimal baselineAvgDailyCashTotal) {
    }

    public record ScoreResult(int score, String band, List<String> drivers) {
    }

    public static ScoreResult score(
            CtrAggregationRow agg,
            CtrRiskSignalsRow daySignals,
            CtrRiskSignalsRow windowSignals,
            ExternalSignals external) {
        int score = 0;
        List<String> drivers = new ArrayList<>();

        BigDecimal cashIn = nz(agg.getTotalCashIn());
        BigDecimal cashTotal = nz(agg.getTotalCashAmount());
        int txnCount = toInt(agg.getTxnCount());
        int distinctLocations = (daySignals != null) ? toInt(daySignals.getDistinctLocationCount()) : 0;

        // 1) Cash deposit amount weighting:
        // Make >= 12k punch much harder (this is what you asked for).
        if (cashIn.compareTo(TWELVE_K) >= 0) {
            score += 45;
            drivers.add(Driver.CASH_DEPOSIT_OVER_12K.name());
        } else if (cashIn.compareTo(TEN_K) >= 0) {
            score += 35;
            drivers.add(Driver.CASH_DEPOSIT_OVER_10K.name());
        }

        // 2) Structuring: make easier to trigger
        // Old logic required txnCount>=3; now >=2 AND "any sub-10k indicator" from
        // daySignals when present.
        // If you don't have that signal, we still treat txnCount>=2 with cashIn>=12k as
        // suspicious enough to add points.
        boolean anyUnder10k = (daySignals != null && nzLong(daySignals.getCashInUnder10kCount()) > 0);
        if ((txnCount >= 2 && anyUnder10k) || (txnCount >= 2 && cashIn.compareTo(TWELVE_K) >= 0)) {
            score += 25;
            drivers.add(Driver.STRUCTURING_PATTERN.name());
        }

        // 3) Multiple branch usage same day
        if (distinctLocations >= 2) {
            score += 20;
            drivers.add(Driver.MULTIPLE_SAME_DAY_BRANCH_USAGE.name());
        }

        // 4) Very high absolute cash total
        if (cashTotal.compareTo(FIFTY_K) >= 0) {
            score += 15;
        }

        // 5) 72-hour high-frequency window (if your repo provides this)
        if (windowSignals != null && nzLong(windowSignals.getTxnCount()) >= 6) {
            score += 15;
            drivers.add(Driver.HIGH_FREQUENCY_72H.name());
        }

        // 6) Unusual vs baseline (optional, but helpful)
        BigDecimal baseline = external != null ? nz(external.baselineAvgDailyCashTotal()) : BigDecimal.ZERO;
        if (baseline.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = cashTotal.divide(baseline, 2, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("3.00")) >= 0) {
                score += 15;
                drivers.add(Driver.UNUSUAL_VS_BASELINE.name());
            }
        }

        // Clamp 0..100
        if (score < 0)
            score = 0;
        if (score > 100)
            score = 100;

        String band = score >= 70 ? "HIGH" : score >= 40 ? "MEDIUM" : "LOW";

        return new ScoreResult(score, band, drivers);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static long nzLong(Long v) {
        return v == null ? 0L : v;
    }

    private static int toInt(Long v) {
        if (v == null)
            return 0;
        if (v > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        if (v < Integer.MIN_VALUE)
            return Integer.MIN_VALUE;
        return v.intValue();
    }

}
