package dev.factoryobservability.compat.api;

import java.util.Set;

/** Discovery and lifecycle wiring belong to loader modules, never Core. */
public interface IntegrationAdapter {
    String id();

    String targetModId();

    Set<AdapterCapability> capabilities();

    boolean isApplicable(RuntimeContext context);

    void register(AdapterRegistrar registrar);
}
