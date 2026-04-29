package ru.vkr.ldpcapp.service;

public final class AppMetadata {

    public static final String PRODUCT_NAME = "LDPC Исследовательская Студия";
    public static final String PRODUCT_SUBTITLE = "Мультиплатформенное графическое приложение для исследования помехоустойчивого кодирования в каналах 5G";
    public static final String VERSION = "1.4.3";
    public static final String VENDOR = "Ульянов И.В.";
    public static final String AUTHOR = "Выпускная квалификационная работа";
    public static final String STACK = "Java 17 · JavaFX 17 · Maven";
    public static final String PLATFORMS = "Windows · Linux · RED OS";
    public static final String RESEARCH_SCOPE = "BPSK, QPSK, 16-QAM, 64-QAM, 256-QAM · AWGN, Rayleigh · LDPC, 5G profiles · BER, BLER, энергетический выигрыш";

    private AppMetadata() {
    }

    public static String buildAboutText() {
        return PRODUCT_NAME + "\n\n"
                + PRODUCT_SUBTITLE + "\n\n"
                + "Версия: " + VERSION + "\n"
                + "Проект: " + AUTHOR + "\n"
                + "Разработчик: " + VENDOR + "\n"
                + "Стек: " + STACK + "\n"
                + "Платформы: " + PLATFORMS + "\n\n"
                + "Исследовательские возможности:\n"
                + RESEARCH_SCOPE + "\n\n"
                + "Приложение предназначено для моделирования цифровой передачи данных в зашумлённых каналах, сравнения сценариев с кодированием и без кодирования, анализа BER(SNR), BLER(SNR), энергетического выигрыша и подготовки материалов для выпускной квалификационной работы.";
    }
}
