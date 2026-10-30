package dev.factoryobservability.neoforge;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceKind;
import dev.factoryobservability.core.resource.ResourceStack;
import dev.factoryobservability.core.resource.ResourceUnit;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

/** Registry-aware canonical component encoding; codec failures must never merge distinct variants. */
public final class ItemIdentityMapper {
    private ItemIdentityMapper() {}

    public static ResourceStack map(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) { throw new IllegalArgumentException("Empty stacks do not have a resource identity"); }
        var location = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Map<String, String> qualifiers = Map.of();
        if (!stack.getComponentsPatch().isEmpty()) {
            JsonElement encoded = DataComponentPatch.CODEC
                    .encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), stack.getComponentsPatch()).getOrThrow();
            qualifiers = Map.of("components_sha256", hash(canonical(encoded).toString()));
        }
        return new ResourceStack(new ResourceId(location.getNamespace(), location.getPath(), ResourceKind.ITEM, qualifiers),
                stack.getCount(), ResourceUnit.ITEMS);
    }

    private static JsonElement canonical(JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject sorted = new JsonObject();
            new TreeMap<>(value.getAsJsonObject().asMap()).forEach((key, child) -> sorted.add(key, canonical(child)));
            return sorted;
        }
        if (value.isJsonArray()) {
            JsonArray result = new JsonArray();
            value.getAsJsonArray().forEach(child -> result.add(canonical(child)));
            return result;
        }
        return value;
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
