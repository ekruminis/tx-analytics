package ZBRA.engine;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import com.ekruminis.txanalytics.wire.BlockResult;
import com.ekruminis.txanalytics.wire.TxResult;

import ZBRA.blockchain.Block;
import ZBRA.blockchain.Data;
import ZBRA.blockchain.Miner;
import ZBRA.blockchain.Transaction;
import ZBRA.kafka.BlockResultPublisher;
import ZBRA.kafka.TxResultPublisher;
import ZBRA.persistence.BlockEntity;
import ZBRA.persistence.BlockRepository;
import ZBRA.persistence.Experiment;
import ZBRA.persistence.ExperimentRepository;
import ZBRA.persistence.MinerEntity;
import ZBRA.persistence.MinerRepository;
import ZBRA.persistence.SimulationRun;
import ZBRA.persistence.SimulationRunRepository;
import ZBRA.tfm.AbstractTFM;
import ZBRA.tfm.Burning2ndPrice;
import ZBRA.tfm.EIP1559;
import ZBRA.tfm.FirstPrice;
import ZBRA.tfm.ReservePool;
import ZBRA.tfm.SecondPrice;

public class SimulationEngine {

    private final SimulationRun.RunProperties props;
    private final ExperimentRepository experimentRepo;
    private final SimulationRunRepository runRepo;
    private final BlockRepository blockRepo;
    private final MinerRepository minerRepo;
    private final BlockResultPublisher blockResultPublisher;
    private final TxResultPublisher txResultPublisher;
    private final Instant blockTimeGenesis;
    private final long blockTimeIntervalSeconds;

    private AbstractTFM tfm;
    private Experiment experiment;
    private SimulationRun run;
    private final List<Miner> miners = new ArrayList<>();
    private final ArrayList<Block> blockchain = new ArrayList<>();
    private final ArrayList<Transaction> mempool = new ArrayList<>();
    private Random winnerRng;
    private int totalStake;
    private long sizeLimit;
    private long target;
    private int window;
    private boolean initialised = false;

    public SimulationEngine(SimulationRun.RunProperties props,
                            ExperimentRepository experimentRepo,
                            SimulationRunRepository runRepo,
                            BlockRepository blockRepo,
                            MinerRepository minerRepo,
                            BlockResultPublisher blockResultPublisher,
                            TxResultPublisher txResultPublisher,
                            Instant blockTimeGenesis,
                            long blockTimeIntervalSeconds) {
        this.props = props;
        this.experimentRepo = experimentRepo;
        this.runRepo = runRepo;
        this.blockRepo = blockRepo;
        this.minerRepo = minerRepo;
        this.blockResultPublisher = blockResultPublisher;
        this.txResultPublisher = txResultPublisher;
        this.blockTimeGenesis = blockTimeGenesis;
        this.blockTimeIntervalSeconds = blockTimeIntervalSeconds;
    }

    public void mineCycle(String datasetHash, long cycle, List<Transaction> newTxs) {
        if (!initialised) {
            initialise(datasetHash);
        }

        mempool.addAll(newTxs);
        Miner winner = getWinningMiner();

        Block previous = blockchain.get(blockchain.size() - 1);
        Data results = tfm.fetchValidTX(mempool, sizeLimit, blockchain, winner, target);

        int height = previous.getIndex() + 1;
        long timestamp = blockTimeGenesis.plusSeconds(height * blockTimeIntervalSeconds).toEpochMilli();
        byte[] merkleRoot = merkleRoot(results.getConfirmed());
        String merkleRootHex = HexFormat.of().formatHex(merkleRoot);

        String currentHash = hashBlock(previous.getCurrentHash(), height, winner.getID(),
                results.getRewards(), results.getSize(), merkleRoot);
        Block block = new Block(height, winner.getID(), previous.getCurrentHash(),
                currentHash, sizeLimit, results);

        if (!blockRepo.existsByRunIdAndHeight(run.getId(), height)) {
            blockRepo.save(new BlockEntity(run, block, merkleRootHex));
            blockResultPublisher.publish(
                    buildBlockResult(height, timestamp, winner, results, merkleRootHex));
            txResultPublisher.publish(buildTxResults(height, timestamp, results));
        }

        blockchain.add(block);
        if (blockchain.size() > window) {
            blockchain.subList(0, blockchain.size() - window).clear();
        }

        System.out.printf("[cycle %d] height=%d winner=%d confirmed=%d mempool=%d payout=%.8f%s%n",
                cycle, height, winner.getID(), results.getConfirmed().size(),
                mempool.size(), block.getRewards().doubleValue(),
                tfmSpecificLog(results));
    }

    private List<TxResult> buildTxResults(int height, long timestamp, Data results) {
        String runId = run.getId().toString();
        String tfm = props.tfm();
        List<TxResult> out = new ArrayList<>(results.getConfirmed().size()
                + results.getUnconfirmed().size());
        switch (tfm) {
            case "first_price", "reserve_pool" -> {
                for (Transaction tx : results.getConfirmed()) {
                    out.add(new TxResult(runId, tfm, height, timestamp, tx.getHash(), tx.getSize(),
                            tx.getTotalFee(), tx.getTotalFee(), true, null));
                }
            }
            case "second_price" -> {
                double eff = results.getBaseFee();
                for (Transaction tx : results.getConfirmed()) {
                    out.add(new TxResult(runId, tfm, height, timestamp, tx.getHash(), tx.getSize(),
                            tx.getTotalFee(), eff * tx.getWeight(), true, null));
                }
            }
            case "eip1559" -> {
                double baseFee = results.getBaseFee();
                for (Transaction tx : results.getConfirmed()) {
                    out.add(new TxResult(runId, tfm, height, timestamp, tx.getHash(), tx.getSize(),
                            tx.getTotalFee(), tx.getTotalFee(), true, baseFee * tx.getWeight()));
                }
            }
            case "burning_second_price" -> {
                double eff = results.getBaseFee();
                for (Transaction tx : results.getConfirmed()) {
                    out.add(new TxResult(runId, tfm, height, timestamp, tx.getHash(), tx.getSize(),
                            tx.getTotalFee(), eff * tx.getWeight(), true, null));
                }
                for (Transaction tx : results.getUnconfirmed()) {
                    out.add(new TxResult(runId, tfm, height, timestamp, tx.getHash(), tx.getSize(),
                            tx.getTotalFee(), 0.0, false, null));
                }
            }
            default -> throw new IllegalStateException("no TxResult mapping for tfm: " + tfm);
        }
        return out;
    }

    private BlockResult buildBlockResult(int height, long timestamp, Miner winner, Data results, String merkleRootHex) {
        String runId = run.getId().toString();
        String tfm = props.tfm();
        int winnerId = winner.getID();
        double payout = results.getRewards().doubleValue();
        double size = results.getSize();
        long txCount = results.getConfirmed().size();
        long mempoolSize = results.getMempoolSize();
        double totalOfferedFee = results.getConfirmed().stream()
                .mapToDouble(Transaction::getTotalFee).sum();
        return switch (tfm) {
            case "first_price" -> new BlockResult(
                    runId, tfm, height, timestamp, winnerId, payout, size, txCount, mempoolSize,
                    totalOfferedFee, merkleRootHex,
                    null, null, null, null, null, null, null);
            case "second_price" -> new BlockResult(
                    runId, tfm, height, timestamp, winnerId, payout, size, txCount, mempoolSize,
                    totalOfferedFee, merkleRootHex,
                    results.getBaseFee(), null, null, null, null, null, null);
            case "eip1559" -> new BlockResult(
                    runId, tfm, height, timestamp, winnerId, payout, size, txCount, mempoolSize,
                    totalOfferedFee, merkleRootHex,
                    results.getBaseFee(), results.getBurned().doubleValue(), null, null,
                    null, null, null);
            case "reserve_pool" -> new BlockResult(
                    runId, tfm, height, timestamp, winnerId, payout, size, txCount, mempoolSize,
                    totalOfferedFee, merkleRootHex,
                    results.getBaseFee(), null, results.getPool().doubleValue(), null,
                    results.getPoolEffect() != null ? results.getPoolEffect().doubleValue() : 0.0,
                    results.isTakeFromPublic(),
                    null);
            case "burning_second_price" -> new BlockResult(
                    runId, tfm, height, timestamp, winnerId, payout, size, txCount, mempoolSize,
                    totalOfferedFee, merkleRootHex,
                    results.getBaseFee(), results.getBurned().doubleValue(), null,
                    results.getUnconfirmed().size(),
                    null, null,
                    results.getRewards().doubleValue() + results.getBurned().doubleValue());
            default -> throw new IllegalStateException("no BlockResult mapping for tfm: " + tfm);
        };
    }

    private String tfmSpecificLog(Data results) {
        return switch (props.tfm()) {
            case "second_price" -> String.format(" eff_fee=%.4e",
                    results.getBaseFee());
            case "eip1559" -> String.format(" base_fee=%.4e burned=%.8f",
                    results.getBaseFee(), results.getBurned().doubleValue());
            case "reserve_pool" -> String.format(" base_fee=%.4e pool=%.4f",
                    results.getBaseFee(), results.getPool().doubleValue());
            case "burning_second_price" -> String.format(" eff_fee=%.4e burned=%.8f unconfirmed=%d",
                    results.getBaseFee(), results.getBurned().doubleValue(),
                    results.getUnconfirmed().size());
            default -> "";
        };
    }

    private Miner getWinningMiner() {
        int r = winnerRng.nextInt(totalStake);
        int cumulative = 0;
        for (Miner miner : miners) {
            cumulative += miner.getStake();
            if (r < cumulative) {
                return miner;
            }
        }
        throw new IllegalStateException("no winner selected — stake table inconsistent");
    }

    private static String hashBlock(String parentHash, int height, int winnerId,
                                    BigDecimal rewards, double size,
                                    byte[] merkleRoot) {
        MessageDigest md = sha256();
        md.update((parentHash + "|" + height + "|" + winnerId + "|"
                + rewards.toPlainString() + "|" + size + "|")
                .getBytes(StandardCharsets.UTF_8));
        md.update(merkleRoot);
        return HexFormat.of().formatHex(md.digest());
    }

    private static byte[] merkleRoot(List<Transaction> confirmed) {
        if (confirmed.isEmpty()) {
            return new byte[32];
        }
        List<byte[]> level = new ArrayList<>(confirmed.size());
        for (Transaction tx : confirmed) {
            level.add(HexFormat.of().parseHex(tx.getHash()));
        }
        while (level.size() > 1) {
            List<byte[]> next = new ArrayList<>((level.size() + 1) / 2);
            for (int i = 0; i < level.size(); i += 2) {
                byte[] left = level.get(i);
                byte[] right = (i + 1 < level.size()) ? level.get(i + 1) : left;
                MessageDigest md = sha256();
                md.update(left);
                md.update(right);
                next.add(sha256().digest(md.digest()));
            }
            level = next;
        }
        return level.get(0);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private void initialise(String datasetHash) {
        this.tfm = buildTfm(props.tfm());

        this.experiment = Experiment.fromProperties(props, datasetHash);
        boolean expNew = !experimentRepo.existsById(experiment.getId());
        if (expNew) {
            experimentRepo.save(experiment);
        }

        this.winnerRng = new Random(props.seed());

        RandomGenerator stakeRng = new Well19937c(props.seed());
        PoissonDistribution stakeDist = new PoissonDistribution(
                stakeRng, 500,
                PoissonDistribution.DEFAULT_EPSILON,
                PoissonDistribution.DEFAULT_MAX_ITERATIONS);
        for (int minerId = 1; minerId <= props.numMiners(); minerId++) {
            miners.add(new Miner(minerId, Math.max(1, stakeDist.sample())));
        }
        this.totalStake = miners.stream().mapToInt(Miner::getStake).sum();

        if (expNew) {
            for (Miner miner : miners) {
                minerRepo.save(new MinerEntity(experiment, miner, totalStake));
            }
        }

        this.run = SimulationRun.fromProperties(props, experiment);
        boolean runNew = !runRepo.existsById(run.getId());
        if (runNew) {
            runRepo.save(run);
        }

        Map<String, String> params = parseParams(run.getMechanismParams());
        this.sizeLimit = Long.parseLong(params.get("size_limit"));
        this.target = params.containsKey("target") ? Long.parseLong(params.get("target")) : 0L;
        this.window = params.containsKey("window") ? Integer.parseInt(params.get("window")) : 1;

        Block anchor = new Block();
        anchor.setCurrentHash(HexFormat.of().formatHex(new byte[32]));
        if (params.containsKey("base_fee")) {
            anchor.setBaseFee(Double.parseDouble(params.get("base_fee")));
        }
        if (target > 0) {
            anchor.setWeight(target);
        }
        if (params.containsKey("reserve_base")) {
            anchor.updatePool(new BigDecimal(params.get("reserve_base")));
        }
        blockchain.add(anchor);

        this.initialised = true;
        System.out.printf("Initialised run %s [%s] experiment=%s miners=%d totalStake=%d%n",
                run.getId(), props.tfm(), experiment.getId(), miners.size(), totalStake);
    }

    private static Map<String, String> parseParams(String canonical) {
        Map<String, String> m = new HashMap<>();
        if (canonical == null || canonical.isBlank()) {
            return m;
        }
        for (String pair : canonical.split(",")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                m.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return m;
    }

    private AbstractTFM buildTfm(String name) {
        return switch (name) {
            case "first_price" -> new FirstPrice();
            case "second_price" -> new SecondPrice();
            case "eip1559" -> new EIP1559();
            case "reserve_pool" -> new ReservePool();
            case "burning_second_price" -> new Burning2ndPrice();
            default -> throw new IllegalArgumentException("Unsupported tfm: " + name);
        };
    }
}
