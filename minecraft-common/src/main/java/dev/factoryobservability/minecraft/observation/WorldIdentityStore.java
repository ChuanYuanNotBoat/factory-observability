package dev.factoryobservability.minecraft.observation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/** Only a local random salt is persisted; endpoints and local save paths are never written here. */
public final class WorldIdentityStore {
    private final byte[] salt;

    public WorldIdentityStore(Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve("world-identity.salt");
        if (Files.exists(file)) {
            salt = Files.readAllBytes(file);
            if (salt.length != 32) { throw new IOException("Invalid local world identity salt"); }
        } else {
            salt = new byte[32];
            new SecureRandom().nextBytes(salt);
            Files.write(file, salt, java.nio.file.StandardOpenOption.CREATE_NEW);
        }
    }

    public String identify(String kind, String canonicalLocation) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            digest.update(kind.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            return HexFormat.of().formatHex(digest.digest(canonicalLocation.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
