package ru.vkr.ldpcapp.service.phy.math;

public final class Complex {
    private final double re;
    private final double im;

    public Complex(double re, double im) {
        this.re = re;
        this.im = im;
    }

    public double re() {
        return re;
    }

    public double im() {
        return im;
    }

    public Complex add(Complex other) {
        return new Complex(re + other.re, im + other.im);
    }

    public Complex subtract(Complex other) {
        return new Complex(re - other.re, im - other.im);
    }

    public double abs2() {
        return re * re + im * im;
    }

    public static Complex multiply(Complex a, Complex b) {
        return new Complex(
                a.re * b.re - a.im * b.im,
                a.re * b.im + a.im * b.re
        );
    }

    public static Complex divide(Complex a, Complex b) {
        double denominator = Math.max(1e-9, b.re * b.re + b.im * b.im);
        return new Complex(
                (a.re * b.re + a.im * b.im) / denominator,
                (a.im * b.re - a.re * b.im) / denominator
        );
    }
}