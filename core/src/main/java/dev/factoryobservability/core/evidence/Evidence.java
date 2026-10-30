package dev.factoryobservability.core.evidence;

import java.util.Objects;

import dev.factoryobservability.core.Checks;

/** Coverage describes the provider's declared observation scope, not a queried history window. */
public record Evidence(Provenance provenance, DataQuality quality, double coverage, double confidence) {
    public Evidence {
        Objects.requireNonNull(provenance, "provenance");
        Objects.requireNonNull(quality, "quality");
        Checks.fraction(coverage, "coverage");
        Checks.fraction(confidence, "confidence");
    }
}
