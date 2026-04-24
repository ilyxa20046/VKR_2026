package ru.vkr.ldpcapp.service;

import javafx.concurrent.Task;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimulationService {

    private static final double INV_SQRT2 = 1.0 / Math.sqrt(2.0);
    private static final double INV_SQRT10 = 1.0 / Math.sqrt(10.0);
    private static final double BASE_SYMBOL_RATE_MBAUD = 20.0;

    private static final LdpcCode EDUCATIONAL_CODE = buildEducationalCode();
    private static final LdpcCode QC_LIKE_CODE = buildQcInspiredCode();

    public Task<List<ResultPoint>> createTask(SimulationConfig config) {
        return new Task<>() {
            @Override
            protected List<ResultPoint> call() {
                List<Double> snrPoints = config.buildSnrPoints();
                List<ResultPoint> results = new ArrayList<>();
                int totalPoints = Math.max(1, snrPoints.size());

                updateProgress(0, totalPoints);
                updateMessage("Подготовка OFDM/MIMO-like исследовательского эксперимента...");

                for (int i = 0; i < snrPoints.size(); i++) {
                    if (isCancelled()) {
                        updateMessage("Расчёт отменён");
                        break;
                    }

                    double snr = snrPoints.get(i);
                    updateMessage(String.format(
                            "Расчёт: %s / %s / %s / %s / %s, SNR = %.2f дБ (%d/%d)",
                            config.getModulation(),
                            config.getChannelModel(),
                            config.getWaveform(),
                            config.getSpatialMode(),
                            SimulationConfig.getProfileName(config.getLdpcProfile()),
                            snr,
                            i + 1,
                            totalPoints
                    ));
                    results.add(simulatePoint(snr, config, i));
                    updateProgress(i + 1, totalPoints);
                }

                updateMessage("Исследовательское моделирование завершено");
                return results;
            }
        };
    }

    public List<ResultPoint> runBlocking(SimulationConfig config) {
        List<ResultPoint> results = new ArrayList<>();
        List<Double> snrPoints = config.buildSnrPoints();
        for (int i = 0; i < snrPoints.size(); i++) {
            results.add(simulatePoint(snrPoints.get(i), config, i));
        }
        return results;
    }

    private ResultPoint simulatePoint(double snrDb, SimulationConfig config, int index) {
        LdpcCode code = getCode(config.getLdpcProfile());
        Random uncodedRandom = new Random(config.getSeed() + 1009L * (index + 1));
        Random codedRandom = new Random(config.getSeed() + 5003L * (index + 1));
        double sigmaUncoded = sigmaFromEbN0(snrDb, config.getModulation(), 1.0);
        double sigmaCoded = sigmaFromEbN0(snrDb, config.getModulation(), code.rate);
        int codewordsPerBlock = config.getInfoBlockLength() / code.k;
        int targetBlocks = config.isAdaptiveStopEnabled() ? config.getMaxBlocksPerSnr() : config.getBlocks();

        int uncodedBitErrors = 0;
        int uncodedBlockErrors = 0;

        int codedBitErrors = 0;
        int codedBlockErrors = 0;
        double iterationsSum = 0.0;
        int successfulCodewords = 0;
        int processedBlocks = 0;

        for (int block = 0; block < targetBlocks; block++) {
            processedBlocks++;

            BlockStat uncodedResult = simulateUncodedBlock(config, sigmaUncoded, uncodedRandom);
            uncodedBitErrors += uncodedResult.bitErrors;
            if (uncodedResult.blockError) {
                uncodedBlockErrors++;
            }

            boolean blockHasError = false;
            for (int word = 0; word < codewordsPerBlock; word++) {
                int[] info = randomBits(code.k, codedRandom);
                int[] encoded = encodeLdpc(info, code);
                Transmission transmission = transmitBits(encoded, config, sigmaCoded, codedRandom);
                double[] llr = demapToLlr(transmission.received, transmission.gains, sigmaCoded, config);
                DecodeResult decoded = decodeLdpcFromLlr(llr, code, config.getMaxIterations(), config.getNormalization());

                iterationsSum += decoded.iterationsUsed;
                if (decoded.success) {
                    successfulCodewords++;
                }

                for (int i = 0; i < code.k; i++) {
                    if (decoded.decodedInfo[i] != info[i]) {
                        codedBitErrors++;
                        blockHasError = true;
                    }
                }
            }

            if (blockHasError) {
                codedBlockErrors++;
            }

            if (shouldStopAdaptive(config, codedBitErrors, codedBlockErrors, processedBlocks)) {
                break;
            }
        }

        int totalBlocks = Math.max(1, processedBlocks);
        int totalBits = totalBlocks * config.getInfoBlockLength();
        int totalCodewords = totalBlocks * codewordsPerBlock;

        double berLdpc = safeDivide(codedBitErrors, totalBits);
        double blerLdpc = safeDivide(codedBlockErrors, totalBlocks);
        double averageIterations = safeDivide(iterationsSum, totalCodewords);
        double successRatio = safeDivide(successfulCodewords, totalCodewords);
        double spectralEfficiency = estimateSpectralEfficiency(config, code.rate, blerLdpc);
        double throughputMbps = estimateThroughputMbps(config, code.rate, blerLdpc);

        Interval berCi = wilsonInterval(codedBitErrors, totalBits, config.getConfidenceLevel());
        Interval blerCi = wilsonInterval(codedBlockErrors, totalBlocks, config.getConfidenceLevel());

        return new ResultPoint(
                snrDb,
                safeDivide(uncodedBitErrors, totalBits),
                berLdpc,
                safeDivide(uncodedBlockErrors, totalBlocks),
                blerLdpc,
                averageIterations,
                successRatio,
                throughputMbps,
                spectralEfficiency,
                berCi.low(),
                berCi.high(),
                blerCi.low(),
                blerCi.high(),
                codedBitErrors,
                codedBlockErrors,
                totalBits,
                totalBlocks,
                config.getConfidenceLevel()
        );
    }

    private BlockStat simulateUncodedBlock(SimulationConfig config, double sigma, Random random) {
        int[] info = randomBits(config.getInfoBlockLength(), random);
        Transmission transmission = transmitBits(info, config, sigma, random);
        double[] llr = demapToLlr(transmission.received, transmission.gains, sigma, config);
        int bitErrors = 0;
        for (int i = 0; i < config.getInfoBlockLength(); i++) {
            int hard = llr[i] < 0.0 ? 1 : 0;
            if (hard != info[i]) {
                bitErrors++;
            }
        }
        return new BlockStat(bitErrors, bitErrors > 0);
    }

    private Transmission transmitBits(int[] bits, SimulationConfig config, double sigma, Random random) {
        Constellation constellation = buildConstellation(config.getModulation());
        int bitsPerSymbol = constellation.bitsPerSymbol;
        int symbolCount = (int) Math.ceil((double) bits.length / bitsPerSymbol);
        Complex[][] received = new Complex[symbolCount][];
        Complex[][] gains = new Complex[symbolCount][];

        WaveformProfile waveformProfile = getWaveformProfile(config);
        int branches = getSpatialBranches(config.getSpatialMode());
        Complex[] blockGains = createChannelGains(config.getChannelModel(), branches, random);
        int remainingSpan = 0;

        for (int symbolIndex = 0; symbolIndex < symbolCount; symbolIndex++) {
            if (SimulationConfig.CHANNEL_RAYLEIGH.equals(config.getChannelModel())
                    && SimulationConfig.WAVEFORM_SC.equals(config.getWaveform())
                    && remainingSpan <= 0) {
                blockGains = createChannelGains(config.getChannelModel(), branches, random);
                remainingSpan = waveformProfile.fadingSpan;
            }

            int from = symbolIndex * bitsPerSymbol;
            int to = Math.min(bits.length, from + bitsPerSymbol);
            int[] symbolBits = new int[bitsPerSymbol];
            System.arraycopy(bits, from, symbolBits, 0, to - from);
            Complex symbol = constellation.lookup(symbolBits);

            Complex[] rxBranches = new Complex[branches];
            Complex[] hBranches = new Complex[branches];
            for (int branch = 0; branch < branches; branch++) {
                Complex h = SimulationConfig.CHANNEL_AWGN.equals(config.getChannelModel())
                        ? new Complex(1.0, 0.0)
                        : SimulationConfig.WAVEFORM_SC.equals(config.getWaveform())
                        ? blockGains[branch]
                        : createOfdmSubcarrierGain(waveformProfile.taps, random);
                Complex noise = new Complex(sigma * gaussian(random), sigma * gaussian(random));
                rxBranches[branch] = multiply(h, symbol).add(noise);
                hBranches[branch] = h;
            }

            if (SimulationConfig.CHANNEL_RAYLEIGH.equals(config.getChannelModel())
                    && SimulationConfig.WAVEFORM_SC.equals(config.getWaveform())) {
                remainingSpan--;
            }

            received[symbolIndex] = rxBranches;
            gains[symbolIndex] = hBranches;
        }

        return new Transmission(received, gains);
    }

    private double[] demapToLlr(Complex[][] received, Complex[][] gains, double sigma, SimulationConfig config) {
        Constellation constellation = buildConstellation(config.getModulation());
        double noiseVariance = 2.0 * sigma * sigma;
        double[] llr = new double[received.length * constellation.bitsPerSymbol];
        int llrIndex = 0;
        boolean useOneTapEqualizer = !SimulationConfig.WAVEFORM_SC.equals(config.getWaveform())
                && SimulationConfig.EQUALIZER_ZF.equals(config.getEqualizerMode());

        for (int symbolIndex = 0; symbolIndex < received.length; symbolIndex++) {
            Complex[] yBranches = received[symbolIndex];
            Complex[] hBranches = gains[symbolIndex];

            for (int bitIndex = 0; bitIndex < constellation.bitsPerSymbol; bitIndex++) {
                double min0 = Double.POSITIVE_INFINITY;
                double min1 = Double.POSITIVE_INFINITY;

                for (ConstellationEntry entry : constellation.entries) {
                    double distance = 0.0;
                    for (int branch = 0; branch < yBranches.length; branch++) {
                        Complex observed = yBranches[branch];
                        Complex channel = hBranches[branch];
                        if (useOneTapEqualizer) {
                            observed = divide(observed, channel);
                            channel = new Complex(1.0, 0.0);
                        }
                        Complex expected = multiply(channel, entry.symbol);
                        Complex diff = observed.subtract(expected);
                        distance += diff.abs2();
                    }
                    if (entry.bits[bitIndex] == 0) {
                        min0 = Math.min(min0, distance);
                    } else {
                        min1 = Math.min(min1, distance);
                    }
                }

                llr[llrIndex++] = (min1 - min0) / noiseVariance;
            }
        }

        return llr;
    }

    private int[] encodeLdpc(int[] infoBits, LdpcCode code) {
        int[] parityBits = new int[code.m];
        for (int row = 0; row < code.m; row++) {
            int parityEquation = 0;
            for (int column : code.messageTaps[row]) {
                parityEquation ^= infoBits[column];
            }
            parityBits[row] = parityEquation ^ (row > 0 ? parityBits[row - 1] : 0);
        }

        int[] encoded = new int[code.n];
        System.arraycopy(infoBits, 0, encoded, 0, code.k);
        System.arraycopy(parityBits, 0, encoded, code.k, code.m);
        return encoded;
    }

    private DecodeResult decodeLdpcFromLlr(double[] inputLlr, LdpcCode code, int maxIterations, double normalization) {
        double[] channelLlr = new double[code.n];
        System.arraycopy(inputLlr, 0, channelLlr, 0, code.n);
        double[][] q = new double[code.checkToVars.length][];
        double[][] r = new double[code.checkToVars.length][];
        for (int check = 0; check < code.checkToVars.length; check++) {
            int degree = code.checkToVars[check].length;
            q[check] = new double[degree];
            r[check] = new double[degree];
            for (int edge = 0; edge < degree; edge++) {
                int variable = code.checkToVars[check][edge];
                q[check][edge] = channelLlr[variable];
                r[check][edge] = 0.0;
            }
        }

        int[] hard = hardDecision(channelLlr);

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            for (int check = 0; check < code.checkToVars.length; check++) {
                double[] messages = q[check];
                int signProduct = 1;
                double min1 = Double.POSITIVE_INFINITY;
                double min2 = Double.POSITIVE_INFINITY;
                int minIndex = -1;

                for (int edge = 0; edge < messages.length; edge++) {
                    double value = messages[edge];
                    int sign = value < 0.0 ? -1 : 1;
                    double absolute = Math.abs(value);
                    signProduct *= sign;

                    if (absolute < min1) {
                        min2 = min1;
                        min1 = absolute;
                        minIndex = edge;
                    } else if (absolute < min2) {
                        min2 = absolute;
                    }
                }

                for (int edge = 0; edge < messages.length; edge++) {
                    double value = messages[edge];
                    int sign = value < 0.0 ? -1 : 1;
                    int extrinsicSign = signProduct * sign;
                    double minimum = edge == minIndex ? min2 : min1;
                    r[check][edge] = normalization * extrinsicSign * minimum;
                }
            }

            double[] posterior = channelLlr.clone();
            for (int variable = 0; variable < code.n; variable++) {
                for (EdgeRef edgeRef : code.varToChecks[variable]) {
                    posterior[variable] += r[edgeRef.check][edgeRef.edge];
                }
            }

            hard = hardDecision(posterior);
            if (checkSyndrome(hard, code)) {
                return new DecodeResult(sliceInfo(hard, code.k), iteration, true);
            }

            for (int variable = 0; variable < code.n; variable++) {
                for (EdgeRef edgeRef : code.varToChecks[variable]) {
                    q[edgeRef.check][edgeRef.edge] = posterior[variable] - r[edgeRef.check][edgeRef.edge];
                }
            }
        }

        return new DecodeResult(sliceInfo(hard, code.k), maxIterations, checkSyndrome(hard, code));
    }

    private boolean checkSyndrome(int[] bits, LdpcCode code) {
        for (int[] check : code.checkToVars) {
            int syndrome = 0;
            for (int variable : check) {
                syndrome ^= bits[variable];
            }
            if (syndrome != 0) {
                return false;
            }
        }
        return true;
    }

    private int[] hardDecision(double[] llr) {
        int[] bits = new int[llr.length];
        for (int i = 0; i < llr.length; i++) {
            bits[i] = llr[i] < 0.0 ? 1 : 0;
        }
        return bits;
    }

    private int[] sliceInfo(int[] bits, int k) {
        int[] info = new int[k];
        System.arraycopy(bits, 0, info, 0, k);
        return info;
    }

    private int[] randomBits(int length, Random random) {
        int[] bits = new int[length];
        for (int i = 0; i < length; i++) {
            bits[i] = random.nextBoolean() ? 1 : 0;
        }
        return bits;
    }

    private double gaussian(Random random) {
        double u1;
        double u2;
        do {
            u1 = random.nextDouble();
        } while (u1 == 0.0);
        do {
            u2 = random.nextDouble();
        } while (u2 == 0.0);
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }

    private Complex rayleigh(Random random) {
        return new Complex(gaussian(random) * INV_SQRT2, gaussian(random) * INV_SQRT2);
    }

    private double sigmaFromEbN0(double snrDb, String modulation, double codeRate) {
        double ebN0 = Math.pow(10.0, snrDb / 10.0);
        int bitsPerSymbol = buildConstellation(modulation).bitsPerSymbol;
        return Math.sqrt(1.0 / (2.0 * bitsPerSymbol * codeRate * ebN0));
    }

    private double estimateThroughputMbps(SimulationConfig config, double codeRate, double blerLdpc) {
        double bitsPerSymbol = buildConstellation(config.getModulation()).bitsPerSymbol;
        double usefulFraction = getWaveformProfile(config).usefulFraction;
        double successFactor = Math.max(0.0, 1.0 - blerLdpc);
        return BASE_SYMBOL_RATE_MBAUD * bitsPerSymbol * codeRate * usefulFraction * successFactor;
    }

    private double estimateSpectralEfficiency(SimulationConfig config, double codeRate, double blerLdpc) {
        double bitsPerSymbol = buildConstellation(config.getModulation()).bitsPerSymbol;
        double usefulFraction = getWaveformProfile(config).usefulFraction;
        double successFactor = Math.max(0.0, 1.0 - blerLdpc);
        return bitsPerSymbol * codeRate * usefulFraction * successFactor;
    }

    private WaveformProfile getWaveformProfile(SimulationConfig config) {
        if (SimulationConfig.WAVEFORM_OFDM128.equals(config.getWaveform())) {
            double cpPenalty = 128.0 / (128.0 + Math.max(0, config.getCyclicPrefix()));
            double eqFactor = SimulationConfig.EQUALIZER_ZF.equals(config.getEqualizerMode()) ? 0.95 : 0.85;
            return new WaveformProfile(0.93 * cpPenalty * eqFactor, 1, 4);
        }
        if (SimulationConfig.WAVEFORM_OFDM64.equals(config.getWaveform())) {
            double cpPenalty = 64.0 / (64.0 + Math.max(0, config.getCyclicPrefix()));
            double eqFactor = SimulationConfig.EQUALIZER_ZF.equals(config.getEqualizerMode()) ? 0.94 : 0.82;
            return new WaveformProfile(0.90 * cpPenalty * eqFactor, 1, 3);
        }
        return new WaveformProfile(1.0, 12, 1);
    }

    private int getSpatialBranches(String spatialMode) {
        return SimulationConfig.SPATIAL_2X2.equals(spatialMode) ? 4 : 1;
    }

    private Complex[] createChannelGains(String channelModel, int branches, Random random) {
        Complex[] gains = new Complex[branches];
        for (int branch = 0; branch < branches; branch++) {
            gains[branch] = SimulationConfig.CHANNEL_AWGN.equals(channelModel)
                    ? new Complex(1.0, 0.0)
                    : rayleigh(random);
        }
        return gains;
    }
    private boolean shouldStopAdaptive(SimulationConfig config, int codedBitErrors, int codedBlockErrors, int processedBlocks) {
        if (!config.isAdaptiveStopEnabled()) {
            return false;
        }
        if (processedBlocks < config.getBlocks()) {
            return false;
        }
        return codedBitErrors >= config.getMinErrorEventsPerSnr()
                && codedBlockErrors >= config.getMinErrorEventsPerSnr();
    }

    private Interval wilsonInterval(int errors, int total, double confidenceLevel) {
        if (total <= 0) {
            return new Interval(0.0, 0.0);
        }
        double p = (double) errors / total;
        double z = zScore(confidenceLevel);
        double z2 = z * z;
        double denominator = 1.0 + z2 / total;
        double center = (p + z2 / (2.0 * total)) / denominator;
        double radius = (z / denominator) * Math.sqrt((p * (1.0 - p) + z2 / (4.0 * total)) / total);
        return new Interval(
                Math.max(0.0, center - radius),
                Math.min(1.0, center + radius)
        );
    }

    private double zScore(double confidenceLevel) {
        if (confidenceLevel >= 0.999) return 3.29;
        if (confidenceLevel >= 0.99) return 2.58;
        if (confidenceLevel >= 0.98) return 2.33;
        if (confidenceLevel >= 0.95) return 1.96;
        if (confidenceLevel >= 0.90) return 1.64;
        if (confidenceLevel >= 0.80) return 1.28;
        return 1.0;
    }

    private double safeDivide(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private double safeDivide(double numerator, int denominator) {
        return denominator == 0 ? 0.0 : numerator / denominator;
    }

    private double safeDivide(double numerator, double denominator) {
        return Math.abs(denominator) < 1e-12 ? 0.0 : numerator / denominator;
    }

    private LdpcCode getCode(String profile) {
        return SimulationConfig.PROFILE_QC.equals(profile) ? QC_LIKE_CODE : EDUCATIONAL_CODE;
    }

    private static LdpcCode buildEducationalCode() {
        int k = 12;
        int m = 12;
        int[] offsets = {0, 3, 7};
        int[][] taps = new int[m][offsets.length];
        for (int row = 0; row < m; row++) {
            for (int i = 0; i < offsets.length; i++) {
                taps[row][i] = (row + offsets[i]) % k;
            }
        }
        return createSystematicCode(k, m, taps);
    }

    private static LdpcCode buildQcInspiredCode() {
        int z = 8;
        int k = z * 6;
        int m = z * 6;
        int[][] baseGraph = {
                {0, 1, 3, 5},
                {0, 2, 4, 5},
                {1, 2, 3, 4},
                {0, 2, 4, 5},
                {1, 3, 4, 5},
                {0, 1, 2, 3}
        };
        int[][] shifts = {
                {0, 1, 2, 5},
                {3, 0, 4, 6},
                {2, 5, 1, 7},
                {6, 1, 3, 0},
                {4, 6, 2, 1},
                {5, 7, 4, 0}
        };

        int[][] taps = new int[m][4];
        int rowIndex = 0;
        for (int groupRow = 0; groupRow < baseGraph.length; groupRow++) {
            for (int localRow = 0; localRow < z; localRow++) {
                for (int j = 0; j < 4; j++) {
                    int group = baseGraph[groupRow][j];
                    int shift = shifts[groupRow][j];
                    taps[rowIndex][j] = group * z + ((localRow + shift) % z);
                }
                rowIndex++;
            }
        }
        return createSystematicCode(k, m, taps);
    }

    private static LdpcCode createSystematicCode(int k, int m, int[][] messageTaps) {
        int n = k + m;
        int[][] checkToVars = new int[m][];
        for (int row = 0; row < m; row++) {
            int extra = row > 0 ? 1 : 0;
            int[] vars = new int[messageTaps[row].length + 1 + extra];
            int idx = 0;
            for (int tap : messageTaps[row]) {
                vars[idx++] = tap;
            }
            vars[idx++] = k + row;
            if (row > 0) {
                vars[idx] = k + row - 1;
            }
            checkToVars[row] = vars;
        }

        List<EdgeRef>[] varToChecks = new List[n];
        for (int i = 0; i < n; i++) {
            varToChecks[i] = new ArrayList<>();
        }
        for (int check = 0; check < checkToVars.length; check++) {
            for (int edge = 0; edge < checkToVars[check].length; edge++) {
                int variable = checkToVars[check][edge];
                varToChecks[variable].add(new EdgeRef(check, edge));
            }
        }

        return new LdpcCode(k, m, n, 0.5, messageTaps, checkToVars, varToChecks);
    }

    private Constellation buildConstellation(String modulation) {
        if (SimulationConfig.MOD_BPSK.equals(modulation)) {
            return new Constellation(
                    1,
                    List.of(
                            new ConstellationEntry(new int[]{0}, new Complex(1.0, 0.0)),
                            new ConstellationEntry(new int[]{1}, new Complex(-1.0, 0.0))
                    )
            );
        }

        if (SimulationConfig.MOD_QPSK.equals(modulation)) {
            return new Constellation(
                    2,
                    List.of(
                            new ConstellationEntry(new int[]{0, 0}, new Complex(INV_SQRT2, INV_SQRT2)),
                            new ConstellationEntry(new int[]{0, 1}, new Complex(-INV_SQRT2, INV_SQRT2)),
                            new ConstellationEntry(new int[]{1, 1}, new Complex(-INV_SQRT2, -INV_SQRT2)),
                            new ConstellationEntry(new int[]{1, 0}, new Complex(INV_SQRT2, -INV_SQRT2))
                    )
            );
        }

        if (SimulationConfig.MOD_16QAM.equals(modulation)) {
            return buildSquareQamConstellation(2); // 2 бита на I и 2 на Q => 16-QAM
        }
        if (SimulationConfig.MOD_64QAM.equals(modulation)) {
            return buildSquareQamConstellation(3); // 3 + 3 => 64-QAM
        }
        if (SimulationConfig.MOD_256QAM.equals(modulation)) {
            return buildSquareQamConstellation(4); // 4 + 4 => 256-QAM
        }

        throw new IllegalArgumentException("Неподдерживаемая модуляция: " + modulation);
    }

    private Constellation buildSquareQamConstellation(int bitsPerAxis) {
        int levels = 1 << bitsPerAxis;
        int bitsPerSymbol = bitsPerAxis * 2;
        double norm = Math.sqrt((2.0 / 3.0) * (levels * levels - 1.0)); // sqrt(10), sqrt(42), sqrt(170)

        List<ConstellationEntry> entries = new ArrayList<>();
        int combinationsPerAxis = 1 << bitsPerAxis;

        for (int iIndex = 0; iIndex < combinationsPerAxis; iIndex++) {
            int[] iBits = toBits(iIndex, bitsPerAxis);
            int iLevel = grayPamLevel(iBits);

            for (int qIndex = 0; qIndex < combinationsPerAxis; qIndex++) {
                int[] qBits = toBits(qIndex, bitsPerAxis);
                int qLevel = grayPamLevel(qBits);

                int[] bits = new int[bitsPerSymbol];
                System.arraycopy(iBits, 0, bits, 0, bitsPerAxis);
                System.arraycopy(qBits, 0, bits, bitsPerAxis, bitsPerAxis);

                entries.add(new ConstellationEntry(
                        bits,
                        new Complex(iLevel / norm, qLevel / norm)
                ));
            }
        }

        return new Constellation(bitsPerSymbol, entries);
    }

    private int[] toBits(int value, int width) {
        int[] bits = new int[width];
        for (int i = width - 1; i >= 0; i--) {
            bits[i] = value & 1;
            value >>= 1;
        }
        return bits;
    }

    private int grayPamLevel(int[] grayBits) {
        int gray = 0;
        for (int bit : grayBits) {
            gray = (gray << 1) | (bit & 1);
        }

        // Gray -> Binary
        int binary = 0;
        for (int g = gray; g > 0; g >>= 1) {
            binary ^= g;
        }

        int levels = 1 << grayBits.length;
        return 2 * binary - (levels - 1); // odd levels: ... -3,-1,+1,+3 ...
    }

    private int gray16Level(int b0, int b1) {
        if (b0 == 0 && b1 == 0) return -3;
        if (b0 == 0) return -1;
        if (b1 == 1) return 1;
        return 3;
    }

    private Complex multiply(Complex a, Complex b) {
        return new Complex(a.re * b.re - a.im * b.im, a.re * b.im + a.im * b.re);
    }

    private Complex divide(Complex a, Complex b) {
        double denominator = Math.max(1e-9, b.re * b.re + b.im * b.im);
        return new Complex(
                (a.re * b.re + a.im * b.im) / denominator,
                (a.im * b.re - a.re * b.im) / denominator
        );
    }

    private Complex createOfdmSubcarrierGain(int taps, Random random) {
        Complex aggregate = new Complex(0.0, 0.0);
        for (int tap = 0; tap < taps; tap++) {
            double weight = 1.0 / Math.sqrt(tap + 1.0);
            Complex tapGain = new Complex(gaussian(random) * INV_SQRT2 * weight, gaussian(random) * INV_SQRT2 * weight);
            aggregate = aggregate.add(tapGain);
        }
        return aggregate;
    }

    private static class WaveformProfile {
        private final double usefulFraction;
        private final int fadingSpan;
        private final int taps;

        private WaveformProfile(double usefulFraction, int fadingSpan, int taps) {
            this.usefulFraction = usefulFraction;
            this.fadingSpan = fadingSpan;
            this.taps = taps;
        }
    }

    private static class BlockStat {
        private final int bitErrors;
        private final boolean blockError;

        private BlockStat(int bitErrors, boolean blockError) {
            this.bitErrors = bitErrors;
            this.blockError = blockError;
        }
    }

    private static class Transmission {
        private final Complex[][] received;
        private final Complex[][] gains;

        private Transmission(Complex[][] received, Complex[][] gains) {
            this.received = received;
            this.gains = gains;
        }
    }

    private record EdgeRef(int check, int edge) {
    }

    private record DecodeResult(int[] decodedInfo, int iterationsUsed, boolean success) {
    }

    private record LdpcCode(int k, int m, int n, double rate, int[][] messageTaps, int[][] checkToVars, List<EdgeRef>[] varToChecks) {
    }
    private record Interval(double low, double high) {
    }

    private static class Complex {
        private final double re;
        private final double im;

        private Complex(double re, double im) {
            this.re = re;
            this.im = im;
        }

        private Complex add(Complex other) {
            return new Complex(re + other.re, im + other.im);
        }

        private Complex subtract(Complex other) {
            return new Complex(re - other.re, im - other.im);
        }

        private double abs2() {
            return re * re + im * im;
        }
    }

    private static class ConstellationEntry {
        private final int[] bits;
        private final Complex symbol;

        private ConstellationEntry(int[] bits, Complex symbol) {
            this.bits = bits;
            this.symbol = symbol;
        }
    }

    private static class Constellation {
        private final int bitsPerSymbol;
        private final List<ConstellationEntry> entries;

        private Constellation(int bitsPerSymbol, List<ConstellationEntry> entries) {
            this.bitsPerSymbol = bitsPerSymbol;
            this.entries = entries;
        }

        private Complex lookup(int[] bits) {
            outer:
            for (ConstellationEntry entry : entries) {
                if (entry.bits.length != bits.length) {
                    continue;
                }
                for (int i = 0; i < bits.length; i++) {
                    if (entry.bits[i] != bits[i]) {
                        continue outer;
                    }
                }
                return entry.symbol;
            }
            return entries.get(0).symbol;
        }
    }
}
