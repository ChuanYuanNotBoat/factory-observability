package dev.factoryobservability.core.observation;

/** Typed payloads can be extended as lifecycle and machine observations are introduced. */
public sealed interface ObservationPayload permits ResourceSnapshot, VisibilityStarted, VisibilityEnded, EntityPosition {}
