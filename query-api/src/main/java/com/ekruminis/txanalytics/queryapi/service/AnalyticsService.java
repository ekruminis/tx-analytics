package com.ekruminis.txanalytics.queryapi.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ekruminis.txanalytics.queryapi.postgres.Experiment;
import com.ekruminis.txanalytics.queryapi.postgres.ExperimentRepository;
import com.ekruminis.txanalytics.queryapi.postgres.Miner;
import com.ekruminis.txanalytics.queryapi.postgres.MinerRepository;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRun;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRunRepository;
import com.ekruminis.txanalytics.queryapi.web.BlockStatsView;
import com.ekruminis.txanalytics.queryapi.web.DistributionView;
import com.ekruminis.txanalytics.queryapi.web.MinerPayoutView;
import com.ekruminis.txanalytics.queryapi.web.MinersView;
import com.ekruminis.txanalytics.queryapi.web.RunSummaryView;
import com.ekruminis.txanalytics.queryapi.web.RunView;
import com.ekruminis.txanalytics.queryapi.web.TimeseriesView;
import com.ekruminis.txanalytics.queryapi.web.TxFeeStatsView;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregate;
import co.elastic.clients.elasticsearch.core.SearchResponse;

@Service
public class AnalyticsService {

    private static final List<Double> SUMMARY_PERCENTS = List.of(50.0, 90.0, 99.0);
    private static final List<Double> DIST_PERCENTS =
            List.of(10.0, 25.0, 50.0, 75.0, 90.0, 95.0, 99.0, 99.9);

    private record MetricSpec(String field, boolean sum) {
    }

    private static final Map<String, MetricSpec> METRICS = Map.of(
            "payout", new MetricSpec("payout", false),
            "size", new MetricSpec("size", false),
            "mempool", new MetricSpec("mempoolSize", false),
            "offered", new MetricSpec("totalOfferedFee", true),
            "base_fee", new MetricSpec("baseFee", false),
            "burned", new MetricSpec("burned", true),
            "pool", new MetricSpec("pool", false));

    private final ElasticsearchClient es;
    private final SimulationRunRepository runRepo;
    private final ExperimentRepository experimentRepo;
    private final MinerRepository minerRepo;

    public AnalyticsService(ElasticsearchClient es,
                            SimulationRunRepository runRepo,
                            ExperimentRepository experimentRepo,
                            MinerRepository minerRepo) {
        this.es = es;
        this.runRepo = runRepo;
        this.experimentRepo = experimentRepo;
        this.minerRepo = minerRepo;
    }

    @Cacheable(cacheNames = "runSummary", key = "#runId", sync = true)
    public RunSummaryView summary(UUID runId) {
        SimulationRun run = requireRun(runId);
        Experiment exp = experimentRepo.findById(run.getExperimentId()).orElseThrow();
        RunView runView = RunView.of(run, exp);
        long sizeLimit = parseSizeLimit(run.getMechanismParams());
        return new RunSummaryView(runView,
                blockStats(runId, sizeLimit),
                txFees(runId, SUMMARY_PERCENTS));
    }

    @Cacheable(cacheNames = "feeDistribution", key = "#runId", sync = true)
    public TxFeeStatsView feeDistribution(UUID runId) {
        requireRun(runId);
        return txFees(runId, DIST_PERCENTS);
    }

    @Cacheable(cacheNames = "runMiners", key = "#runId", sync = true)
    public MinersView miners(UUID runId) {
        SimulationRun run = requireRun(runId);
        Map<Integer, Miner> stakeById = minerRepo.findByExperimentIdOrderByMinerIdAsc(run.getExperimentId())
                .stream().collect(Collectors.toMap(Miner::getMinerId, Function.identity()));

        SearchResponse<Void> resp = search("block_results", b -> b
                .size(0)
                .query(q -> q.term(t -> t.field("runId").value(FieldValue.of(runId.toString()))))
                .aggregations("miners", a -> a
                        .terms(t -> t.field("winnerMinerId").size(1000))
                        .aggregations("payout", sa -> sa.sum(su -> su.field("payout")))));

        List<MinerPayoutView> out = new ArrayList<>();
        resp.aggregations().get("miners").lterms().buckets().array().forEach(bucket -> {
            int minerId = (int) bucket.key();
            double totalPayout = bucket.aggregations().get("payout").sum().value();
            Miner m = stakeById.get(minerId);
            out.add(new MinerPayoutView(minerId,
                    m == null ? 0 : m.getStake(),
                    m == null ? 0 : m.getStakePct(),
                    bucket.docCount(), totalPayout));
        });
        out.sort(Comparator.comparingLong(MinerPayoutView::blocksWon).reversed());
        return new MinersView(runId, out);
    }

    @Cacheable(cacheNames = "runTimeseries", key = "#runId + '_' + #metric + '_' + #interval", sync = true)
    public TimeseriesView timeseries(UUID runId, String metric, String interval) {
        requireRun(runId);
        MetricSpec spec = METRICS.get(metric);
        if (spec == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "unknown metric: " + metric + " (valid: " + METRICS.keySet() + ")");
        }
        CalendarInterval ci = parseInterval(interval);

        SearchResponse<Void> resp = search("block_results", b -> b
                .size(0)
                .query(q -> q.term(t -> t.field("runId").value(FieldValue.of(runId.toString()))))
                .aggregations("ts", a -> a
                        .dateHistogram(dh -> dh.field("timestamp").calendarInterval(ci))
                        .aggregations("metric", ma -> spec.sum()
                                ? ma.sum(su -> su.field(spec.field()))
                                : ma.avg(av -> av.field(spec.field())))));

        List<TimeseriesView.Point> points = new ArrayList<>();
        resp.aggregations().get("ts").dateHistogram().buckets().array().forEach(bucket -> {
            Aggregate m = bucket.aggregations().get("metric");
            double value = spec.sum() ? m.sum().value() : m.avg().value();
            points.add(new TimeseriesView.Point(bucket.keyAsString(), value));
        });
        return new TimeseriesView(runId, metric, interval, points);
    }

    private BlockStatsView blockStats(UUID runId, long sizeLimit) {
        SearchResponse<Void> resp = search("block_results", b -> b
                .size(0)
                .query(q -> q.term(t -> t.field("runId").value(FieldValue.of(runId.toString()))))
                .aggregations("payout", a -> a.stats(s -> s.field("payout")))
                .aggregations("payoutPctls", a -> a.percentiles(p -> p.field("payout").percents(SUMMARY_PERCENTS)))
                .aggregations("size", a -> a.stats(s -> s.field("size")))
                .aggregations("mempool", a -> a.stats(s -> s.field("mempoolSize")))
                .aggregations("offered", a -> a.stats(s -> s.field("totalOfferedFee")))
                .aggregations("burned", a -> a.stats(s -> s.field("burned")))
                .aggregations("pool", a -> a.stats(s -> s.field("pool"))));

        Map<String, Aggregate> aggs = resp.aggregations();
        DistributionView payout = dist(aggs.get("payout"), aggs.get("payoutPctls"));
        DistributionView size = dist(aggs.get("size"), null);
        double avgFill = (size.avg() != null && sizeLimit > 0) ? size.avg() / sizeLimit : 0.0;
        return new BlockStatsView(
                payout.count(), payout, size, avgFill,
                dist(aggs.get("mempool"), null),
                dist(aggs.get("offered"), null),
                dist(aggs.get("burned"), null),
                dist(aggs.get("pool"), null));
    }

    private TxFeeStatsView txFees(UUID runId, List<Double> percents) {
        SearchResponse<Void> resp = search("tx_results", b -> b
                .size(0)
                .query(q -> q.term(t -> t.field("runId").value(FieldValue.of(runId.toString()))))
                .aggregations("offered", a -> a.stats(s -> s.field("offeredFee")))
                .aggregations("offeredPctls", a -> a.percentiles(p -> p.field("offeredFee").percents(percents)))
                .aggregations("paid", a -> a.stats(s -> s.field("paidFee")))
                .aggregations("paidPctls", a -> a.percentiles(p -> p.field("paidFee").percents(percents)))
                .aggregations("confirmed", a -> a.filter(f -> f.term(t -> t.field("confirmed").value(FieldValue.of(true)))))
                .aggregations("totalBurned", a -> a.sum(s -> s.field("burned"))));

        Map<String, Aggregate> aggs = resp.aggregations();
        DistributionView offered = dist(aggs.get("offered"), aggs.get("offeredPctls"));
        DistributionView paid = dist(aggs.get("paid"), aggs.get("paidPctls"));
        long txCount = paid.count();
        long confirmed = aggs.get("confirmed").filter().docCount();
        double totalPaid = paid.sum() == null ? 0.0 : paid.sum();
        double totalBurned = aggs.get("totalBurned").sum().value();
        return new TxFeeStatsView(txCount, confirmed, txCount - confirmed,
                offered, paid, totalPaid, totalBurned);
    }

    private static DistributionView dist(Aggregate statsAgg, Aggregate pctlsAgg) {
        StatsAggregate s = statsAgg.stats();
        if (s.count() == 0) {
            return DistributionView.empty();
        }
        Map<String, Double> pctls = new LinkedHashMap<>();
        if (pctlsAgg != null && pctlsAgg.tdigestPercentiles().values().isKeyed()) {
            pctlsAgg.tdigestPercentiles().values().keyed().forEach((k, v) -> {
                Double parsed = parseOrNull(v);
                if (parsed != null) {
                    pctls.put(k, parsed);
                }
            });
        }
        return new DistributionView(s.count(), s.min(), s.max(), s.avg(), s.sum(), pctls);
    }

    private SearchResponse<Void> search(String index,
                                        Function<co.elastic.clients.elasticsearch.core.SearchRequest.Builder,
                                                co.elastic.clients.util.ObjectBuilder<co.elastic.clients.elasticsearch.core.SearchRequest>> fn) {
        try {
            return es.search(s -> fn.apply(s.index(index)), Void.class);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "elasticsearch query failed", e);
        }
    }

    private SimulationRun requireRun(UUID runId) {
        return runRepo.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "run not found: " + runId));
    }

    private static long parseSizeLimit(String mechanismParams) {
        for (String pair : mechanismParams.split(",")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals("size_limit")) {
                return Long.parseLong(pair.substring(eq + 1));
            }
        }
        return 0;
    }

    private static Double parseOrNull(String v) {
        try {
            double d = Double.parseDouble(v);
            return Double.isNaN(d) ? null : d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static CalendarInterval parseInterval(String interval) {
        return switch (interval.toLowerCase()) {
            case "hour" -> CalendarInterval.Hour;
            case "day" -> CalendarInterval.Day;
            case "week" -> CalendarInterval.Week;
            case "month" -> CalendarInterval.Month;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "unknown interval: " + interval + " (valid: hour, day, week, month)");
        };
    }
}
