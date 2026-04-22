package ru.vkr.ldpcapp.service;

public final class AppMetadata {

    public static final String PRODUCT_NAME = "LDPC Research Studio";
    public static final String PRODUCT_SUBTITLE = "Мультиплатформенное графическое приложение для исследования помехоустойчивого кодирования в каналах 5G";
    public static final String VERSION = "1.0.0";
    public static final String VENDOR = "VKR Research Project";
    public static final String AUTHOR = "Выпускная квалификационная работа";
    public static final String STACK = "Java 17 · JavaFX 17 · Maven";
    public static final String PLATFORMS = "Windows · Linux · RED OS";
    public static final String RESEARCH_SCOPE = "BPSK, QPSK, 16-QAM · AWGN, Rayleigh · LDPC, QC-inspired 5G-like profiles · BER, BLER, energy gain";

    private AppMetadata() {
    }

    public static String buildAboutText() {
        return PRODUCT_NAME + "\n\n"
                + PRODUCT_SUBTITLE + "\n\n"
                + "Версия: " + VERSION + "\n"
                + "Проект: " + AUTHOR + "\n"
                + "Поставщик: " + VENDOR + "\n"
                + "Стек: " + STACK + "\n"
                + "Платформы: " + PLATFORMS + "\n\n"
                + "Исследовательские возможности:\n"
                + RESEARCH_SCOPE + "\n\n"
                + "Приложение предназначено для моделирования цифровой передачи данных в зашумлённых каналах, сравнения сценариев с кодированием и без кодирования, анализа BER(SNR), BLER(SNR), энергетического выигрыша и подготовки материалов для выпускной квалификационной работы.";
    }
}
