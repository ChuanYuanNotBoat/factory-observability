package dev.factoryobservability.minecraft.observation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class WorldIdentityStoreTest {
    @TempDir Path directory;

    @Test void identitiesAreStableLocallySaltedAndDoNotPersistEndpoints() throws IOException {
        var first = new WorldIdentityStore(directory.resolve("first"));
        String id = first.identify("multiplayer", "private.example:25565");
        assertEquals(id, new WorldIdentityStore(directory.resolve("first")).identify("multiplayer", "private.example:25565"));
        assertNotEquals(id, first.identify("singleplayer", "private.example:25565"));
        assertNotEquals(id, new WorldIdentityStore(directory.resolve("second")).identify("multiplayer", "private.example:25565"));
        assertFalse(new String(Files.readAllBytes(directory.resolve("first/world-identity.salt")), StandardCharsets.ISO_8859_1).contains("private.example"));
    }
}
