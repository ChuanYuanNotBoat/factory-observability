package dev.factoryobservability.core.observation;

import java.util.Objects;

/** Every client disappearance is censored: it is not evidence of pickup, destruction or zero inventory. */
public record VisibilityEnded(Reason reason) implements ObservationPayload {
    public VisibilityEnded {
        Objects.requireNonNull(reason, "reason");
    }

    public enum Reason { MENU_CLOSED, CHUNK_UNLOADED, LOST_VISIBILITY, SESSION_ENDED, SOURCE_ERROR, BUDGET_LIMIT }
}
