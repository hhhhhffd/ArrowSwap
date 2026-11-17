package com.arrowswap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.slf4j.LoggerFactory;

final class ArrowConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("arrowswap.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ArrowConfig current;
    JsonElement first;
    JsonElement second;

    static ArrowConfig get() {
        if (current == null) current = load();
        return current;
    }

    private static ArrowConfig load() {
        if (Files.isRegularFile(FILE)) {
            try (var reader = Files.newBufferedReader(FILE)) {
                ArrowConfig loaded = GSON.fromJson(reader, ArrowConfig.class);
                if (loaded != null) return loaded;
            } catch (IOException | RuntimeException e) {
                LoggerFactory.getLogger("Arrow Swap").warn("Could not read arrow presets", e);
            }
        }
        return new ArrowConfig();
    }

    static RegistryOps<JsonElement> ops(Minecraft client) {
        return RegistryOps.create(JsonOps.INSTANCE, client.level != null ? client.level.registryAccess()
                : RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }

    static ItemStack standard(int preset) {
        return PotionContents.createItemStack(Items.TIPPED_ARROW, preset == 0 ? Potions.POISON : Potions.SLOWNESS);
    }

    ItemStack getPreset(Minecraft client, int preset) {
        JsonElement data = preset == 0 ? first : second;
        if (data == null) return standard(preset);
        try {
            return ItemStack.CODEC.parse(ops(client), data).result()
                    .filter(ArrowSwap::isArrow).map(stack -> stack.copyWithCount(1)).orElseGet(() -> standard(preset));
        } catch (RuntimeException e) {
            return standard(preset);
        }
    }

    boolean apply(Minecraft client, ItemStack first, ItemStack second) {
        var a = ItemStack.CODEC.encodeStart(ops(client), first.copyWithCount(1)).result();
        var b = ItemStack.CODEC.encodeStart(ops(client), second.copyWithCount(1)).result();
        if (a.isEmpty() || b.isEmpty()) return false;
        JsonElement previousA = this.first, previousB = this.second;
        this.first = a.get();
        this.second = b.get();
        Path temp = null;
        try {
            Files.createDirectories(FILE.getParent());
            temp = Files.createTempFile(FILE.getParent(), "arrowswap-", ".tmp");
            try (var writer = Files.newBufferedWriter(temp)) { GSON.toJson(this, writer); }
            try {
                Files.move(temp, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            this.first = previousA;
            this.second = previousB;
            LoggerFactory.getLogger("Arrow Swap").warn("Could not save arrow presets", e);
            return false;
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            }
        }
    }
}
