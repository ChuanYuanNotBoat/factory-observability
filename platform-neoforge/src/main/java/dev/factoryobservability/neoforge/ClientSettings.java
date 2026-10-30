package dev.factoryobservability.neoforge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public record ClientSettings(int samplePeriodTicks, int maximumMenuSlots, int maximumEntities,
        int maximumNodes, int maximumSamples, int queueCapacity, int eventsPerTick) {
    public ClientSettings {
        if (samplePeriodTicks < 1 || samplePeriodTicks > 1200 || maximumMenuSlots < 1 || maximumMenuSlots > 1024
                || maximumEntities < 1 || maximumEntities > 1024 || maximumNodes <= maximumEntities
                || maximumNodes > 4096 || maximumSamples < 100 || maximumSamples > 1_000_000
                || queueCapacity < 1024 || queueCapacity > 65536 || eventsPerTick < 1 || eventsPerTick > 4096) {
            throw new IllegalArgumentException("Observation settings exceed supported bounds");
        }
    }

    public static ClientSettings defaults() { return new ClientSettings(20, 256, 128, 512, 20000, 8192, 512); }

    public static ClientSettings load(Path file) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, gson.toJson(defaults()) + "\n", StandardCharsets.UTF_8);
            return defaults();
        }
        ClientSettings result = gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), ClientSettings.class);
        if (result == null) { throw new IllegalArgumentException("Observation config must contain an object"); }
        return result;
    }
}
