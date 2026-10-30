package dev.factoryobservability.core;

import java.util.Objects;

/** Shared validation for immutable domain values. */
public final class Checks {
    private Checks() {}

    public static String text(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public static double nonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
        return value;
    }

    public static double fraction(double value, String name) {
        nonNegative(value, name);
        if (value > 1) {
            throw new IllegalArgumentException(name + " must be in [0, 1]");
        }
        return value;
    }
}
