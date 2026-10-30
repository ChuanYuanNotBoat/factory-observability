package dev.factoryobservability.core.observation;

public record EntityPosition(double x, double y, double z) implements ObservationPayload {
    public EntityPosition {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Entity position must be finite");
        }
    }
}
