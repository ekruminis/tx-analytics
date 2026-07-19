package com.ekruminis.txanalytics.queryapi.service;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ekruminis.txanalytics.queryapi.postgres.SimulationRun;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRunRepository;
import com.ekruminis.txanalytics.queryapi.web.ComparisonView;
import com.ekruminis.txanalytics.queryapi.web.DistributionView;
import com.ekruminis.txanalytics.queryapi.web.MetricRanking;
import com.ekruminis.txanalytics.queryapi.web.RankedRun;
import com.ekruminis.txanalytics.queryapi.web.RunSummaryView;
import com.ekruminis.txanalytics.queryapi.web.TxFeeStatsView;

@Service
public class CompareService {

    private final AnalyticsService analytics;
    private final SimulationRunRepository runRepo;

    public CompareService(AnalyticsService analytics, SimulationRunRepository runRepo) {
        this.analytics = analytics;
        this.runRepo = runRepo;
    }

    @Cacheable(cacheNames = "comparison",
            key = "#experiment + '|' + #tfmCsv + '|' + #runsCsv",
            sync = true)
    public ComparisonView compare(UUID experiment, String tfmCsv, String runsCsv) {
        List<UUID> runIds = resolveRuns(experiment, tfmCsv, runsCsv);
        if (runIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no runs matched the comparison request");
        }
        List<RunSummaryView> summaries = runIds.stream().map(analytics::summary).toList();
        return new ComparisonView(summaries, rankings(summaries));
    }

    private List<UUID> resolveRuns(UUID experiment, String tfmCsv, String runsCsv) {
        boolean hasRuns = runsCsv != null && !runsCsv.isBlank();
        boolean hasExp = experiment != null;
        boolean hasTfm = tfmCsv != null && !tfmCsv.isBlank();

        if (hasRuns && hasExp) {
            throw badRequest("provide either 'experiment' or 'runs', not both");
        }
        if (!hasRuns && !hasExp) {
            throw badRequest("provide 'experiment' (optionally with 'tfm') or 'runs'");
        }
        if (hasTfm && !hasExp) {
            throw badRequest("'tfm' filter requires 'experiment'");
        }

        if (hasRuns) {
            return Arrays.stream(runsCsv.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(CompareService::parseUuid)
                    .toList();
        }

        List<SimulationRun> runs = runRepo.findByExperimentIdOrderByTfmType(experiment);
        if (hasTfm) {
            Set<String> tfms = Arrays.stream(tfmCsv.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
            runs = runs.stream().filter(r -> tfms.contains(r.getTfmType())).toList();
        }
        return runs.stream().map(SimulationRun::getId).toList();
    }

    private static List<MetricRanking> rankings(List<RunSummaryView> summaries) {
        List<MetricRanking> out = new ArrayList<>();
        out.add(rank(summaries, "avg_payout",
                "average miner payout per block", true, s -> s.blocks().payout().avg()));
        out.add(rank(summaries, "payout_spread",
                "payout max minus min (lower = flatter, fairer income)", false,
                s -> spread(s.blocks().payout())));
        out.add(rank(summaries, "avg_paid_fee",
                "average fee users actually paid (lower = cheaper for users)", false,
                s -> s.fees().paidFee().avg()));
        out.add(rank(summaries, "user_savings",
                "total offered minus total paid (higher = users kept more)", true,
                s -> savings(s.fees())));
        out.add(rank(summaries, "total_burned",
                "total fees burned", true, s -> s.fees().totalBurned()));
        out.add(rank(summaries, "avg_fill_ratio",
                "average block fill ratio", true, s -> s.blocks().avgFillRatio()));
        return out;
    }

    private static MetricRanking rank(List<RunSummaryView> summaries, String metric, String description,
                                      boolean higherIsBetter, Function<RunSummaryView, Double> extractor) {
        Comparator<Map.Entry<RunSummaryView, Double>> byValue =
                Comparator.comparingDouble(Map.Entry::getValue);
        List<Map.Entry<RunSummaryView, Double>> sorted = summaries.stream()
                .map(s -> new SimpleEntry<>(s, extractor.apply(s)))
                .filter(e -> e.getValue() != null)
                .sorted(higherIsBetter ? byValue.reversed() : byValue)
                .collect(Collectors.toList());

        List<RankedRun> ranked = new ArrayList<>(sorted.size());
        int position = 1;
        for (Map.Entry<RunSummaryView, Double> e : sorted) {
            ranked.add(new RankedRun(e.getKey().run().id(), e.getKey().run().tfm(), e.getValue(), position++));
        }
        return new MetricRanking(metric, description, higherIsBetter, ranked);
    }

    private static Double spread(DistributionView d) {
        return (d.max() != null && d.min() != null) ? d.max() - d.min() : null;
    }

    private static Double savings(TxFeeStatsView f) {
        Double offered = f.offeredFee().sum();
        Double paid = f.paidFee().sum();
        return (offered != null && paid != null) ? offered - paid : null;
    }

    private static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw badRequest("not a valid run id: " + s);
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
