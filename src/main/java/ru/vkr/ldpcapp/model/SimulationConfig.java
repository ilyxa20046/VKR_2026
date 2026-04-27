package ru.vkr.ldpcapp.model;

public class SimulationConfig {

    public static final String MOD_BPSK = "BPSK";
    public static final String MOD_QPSK = "QPSK";
    public static final String MOD_16QAM = "16-QAM";
    public static final String MOD_64QAM = "64-QAM";
    public static final String MOD_256QAM = "256-QAM";

    public static final String CHANNEL_AWGN = "AWGN";
    public static final String CHANNEL_RAYLEIGH = "Rayleigh";

    public static final String PROFILE_EDU = "edu-24-12";
    public static final String PROFILE_QC = "qcldpc-96-48";
    public static final String PROFILE_POLAR = "polar-128-64";
    public static final String PROFILE_5GNR_BG1 = "5gnr-bg1-z8";

    public static final String WAVEFORM_SC = "Single-carrier";
    public static final String WAVEFORM_OFDM64 = "OFDM-64";
    public static final String WAVEFORM_OFDM128 = "OFDM-128";

    public static final String SPATIAL_SISO = "SISO";
    public static final String SPATIAL_2X2 = "2x2 Diversity";

    public static final String NR_BG_AUTO = "AUTO";
    public static final String NR_BG1 = "BG1";
    public static final String NR_BG2 = "BG2";
    public static final String SNR_DOMAIN_EB_N0 = "Eb/N0";
    public static final String SNR_DOMAIN_ES_N0 = "Es/N0";

    

    public static final int CRC_NONE = 0;
    public static final int CRC_16 = 16;
    public static final String BLER_BY_BIT_MISMATCH = "Bit mismatch";
    public static final String BLER_BY_CRC_FAIL = "CRC fail";
    private String snrDomain = SNR_DOMAIN_EB_N0;

    public static final String EQUALIZER_NONE = "None";
    public static final String EQUALIZER_ZF = "One-tap ZF";
    public static final boolean DEFAULT_ADAPTIVE_STOP_ENABLED = true;
    public static final int DEFAULT_MIN_ERROR_EVENTS_PER_SNR = 50;
    public static final int DEFAULT_MAX_BLOCKS_PER_SNR = 2000;
    public static final double DEFAULT_CONFIDENCE_LEVEL = 0.95;


    private int infoBlockLength;
    private double snrStart;
    private double snrEnd;
    private double snrStep;
    private int blocks;
    private int maxIterations;
    private double normalization;
    private int seed;
    private String modulation;
    private String channelModel;
    private String ldpcProfile;
    private String waveform;
    private String spatialMode;
    private int cyclicPrefix;
    private String equalizerMode;
    private boolean adaptiveStopEnabled;
    private int minErrorEventsPerSnr;
    private int maxBlocksPerSnr;
    private double confidenceLevel;

    private String nrBaseGraph = NR_BG_AUTO;
    private int liftingSize = 8;
    private boolean crcEnabled = false;
    private int crcBits = CRC_NONE;
    private boolean segmentationEnabled = false;
    private boolean rateMatchingEnabled = false;
    private int targetCodewordLength = 0; // 0 => использовать n кодового слова
    private String blerCriterion = BLER_BY_BIT_MISMATCH;

    public SimulationConfig() {
    }

    public SimulationConfig(
            int infoBlockLength,
            double snrStart,
            double snrEnd,
            double snrStep,
            int blocks,
            int maxIterations,
            double normalization,
            int seed,
            String modulation,
            String channelModel,
            String ldpcProfile,
            String waveform,
            String spatialMode,
            int cyclicPrefix,
            String equalizerMode
    ) {
        this.infoBlockLength = infoBlockLength;
        this.snrStart = snrStart;
        this.snrEnd = snrEnd;
        this.snrStep = snrStep;
        this.blocks = blocks;
        this.maxIterations = maxIterations;
        this.normalization = normalization;
        this.seed = seed;
        this.modulation = modulation;
        this.channelModel = channelModel;
        this.ldpcProfile = ldpcProfile;
        this.waveform = waveform;
        this.spatialMode = spatialMode;
        this.cyclicPrefix = cyclicPrefix;
        this.equalizerMode = equalizerMode;
        this.adaptiveStopEnabled = DEFAULT_ADAPTIVE_STOP_ENABLED;
        this.minErrorEventsPerSnr = DEFAULT_MIN_ERROR_EVENTS_PER_SNR;
        this.maxBlocksPerSnr = DEFAULT_MAX_BLOCKS_PER_SNR;
        this.confidenceLevel = DEFAULT_CONFIDENCE_LEVEL;
    }
    public SimulationConfig(
            int infoBlockLength,
            double snrStart,
            double snrEnd,
            double snrStep,
            int blocks,
            int maxIterations,
            double normalization,
            int seed,
            String modulation,
            String channelModel,
            String ldpcProfile,
            String waveform,
            String spatialMode,
            int cyclicPrefix,
            String equalizerMode,
            boolean adaptiveStopEnabled,
            int minErrorEventsPerSnr,
            int maxBlocksPerSnr,
            double confidenceLevel
    ) {
        this(
                infoBlockLength,
                snrStart,
                snrEnd,
                snrStep,
                blocks,
                maxIterations,
                normalization,
                seed,
                modulation,
                channelModel,
                ldpcProfile,
                waveform,
                spatialMode,
                cyclicPrefix,
                equalizerMode
        );
        this.adaptiveStopEnabled = adaptiveStopEnabled;
        this.minErrorEventsPerSnr = minErrorEventsPerSnr;
        this.maxBlocksPerSnr = maxBlocksPerSnr;
        this.confidenceLevel = confidenceLevel;
    }

    public int getInfoBlockLength() {
        return infoBlockLength;
    }

    public void setInfoBlockLength(int infoBlockLength) {
        this.infoBlockLength = infoBlockLength;
    }

    public double getSnrStart() {
        return snrStart;
    }

    public void setSnrStart(double snrStart) {
        this.snrStart = snrStart;
    }

    public double getSnrEnd() {
        return snrEnd;
    }

    public void setSnrEnd(double snrEnd) {
        this.snrEnd = snrEnd;
    }

    public double getSnrStep() {
        return snrStep;
    }

    public void setSnrStep(double snrStep) {
        this.snrStep = snrStep;
    }

    public int getBlocks() {
        return blocks;
    }

    public void setBlocks(int blocks) {
        this.blocks = blocks;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public double getNormalization() {
        return normalization;
    }

    public void setNormalization(double normalization) {
        this.normalization = normalization;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public String getModulation() {
        return modulation;
    }

    public void setModulation(String modulation) {
        this.modulation = modulation;
    }

    public String getChannelModel() {
        return channelModel;
    }

    public void setChannelModel(String channelModel) {
        this.channelModel = channelModel;
    }

    public String getLdpcProfile() {
        return ldpcProfile;
    }

    public void setLdpcProfile(String ldpcProfile) {
        this.ldpcProfile = ldpcProfile;
    }

    public String getWaveform() {
        return waveform;
    }

    public void setWaveform(String waveform) {
        this.waveform = waveform;
    }

    public String getSpatialMode() {
        return spatialMode;
    }

    public void setSpatialMode(String spatialMode) {
        this.spatialMode = spatialMode;
    }

    public int getCyclicPrefix() {
        return cyclicPrefix;
    }

    public void setCyclicPrefix(int cyclicPrefix) {
        this.cyclicPrefix = cyclicPrefix;
    }

    public String getEqualizerMode() {
        return equalizerMode;
    }

    public void setEqualizerMode(String equalizerMode) {
        this.equalizerMode = equalizerMode;
    }
    public boolean isAdaptiveStopEnabled() {
        return adaptiveStopEnabled;
    }

    public void setAdaptiveStopEnabled(boolean adaptiveStopEnabled) {
        this.adaptiveStopEnabled = adaptiveStopEnabled;
    }

    public int getMinErrorEventsPerSnr() {
        return minErrorEventsPerSnr;
    }

    public void setMinErrorEventsPerSnr(int minErrorEventsPerSnr) {
        this.minErrorEventsPerSnr = minErrorEventsPerSnr;
    }

    public int getMaxBlocksPerSnr() {
        return maxBlocksPerSnr;
    }

    public void setMaxBlocksPerSnr(int maxBlocksPerSnr) {
        this.maxBlocksPerSnr = maxBlocksPerSnr;
    }

    public double getConfidenceLevel() {
        return confidenceLevel;
    }

    public String getNrBaseGraph() {
        return nrBaseGraph;
    }

    public void setNrBaseGraph(String nrBaseGraph) {
        this.nrBaseGraph = nrBaseGraph;
    }

    public int getLiftingSize() {
        return liftingSize;
    }

    public void setLiftingSize(int liftingSize) {
        this.liftingSize = liftingSize;
    }

    public boolean isCrcEnabled() {
        return crcEnabled;
    }

    public void setCrcEnabled(boolean crcEnabled) {
        this.crcEnabled = crcEnabled;
    }

    public int getCrcBits() {
        return crcBits;
    }

    public void setCrcBits(int crcBits) {
        this.crcBits = crcBits;
    }

    public boolean isSegmentationEnabled() {
        return segmentationEnabled;
    }

    public void setSegmentationEnabled(boolean segmentationEnabled) {
        this.segmentationEnabled = segmentationEnabled;
    }

    public boolean isRateMatchingEnabled() {
        return rateMatchingEnabled;
    }

    public void setRateMatchingEnabled(boolean rateMatchingEnabled) {
        this.rateMatchingEnabled = rateMatchingEnabled;
    }

    public int getTargetCodewordLength() {
        return targetCodewordLength;
    }

    public void setTargetCodewordLength(int targetCodewordLength) {
        this.targetCodewordLength = targetCodewordLength;
    }
    public String getBlerCriterion() {
        return blerCriterion;
    }

    public void setBlerCriterion(String blerCriterion) {
        this.blerCriterion = blerCriterion;
    }
    public void setConfidenceLevel(double confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public String getSnrDomain() {
        return snrDomain;
    }

    public void setSnrDomain(String snrDomain) {
        this.snrDomain = snrDomain;
    }
}
