package com.exoticmoss.simpleores.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;

import java.util.Optional;

/// Reads the data of a block entity (the way later versions of Minecraft do it), see ValueOutput.
public record ValueInput(CompoundTag tag, HolderLookup.Provider registries) {
    public <T> Optional<T> read(String key, Codec<T> codec) {
        if (!tag.contains(key)) return Optional.empty();
        return codec.parse(RegistryOps.create(NbtOps.INSTANCE, registries), tag.get(key)).result();
    }

    public Optional<int[]> getIntArray(String key) {
        return tag.contains(key) ? Optional.of(tag.getIntArray(key)) : Optional.empty();
    }

    public int getIntOr(String key, int fallback) {
        return tag.contains(key) ? tag.getInt(key) : fallback;
    }

    public long getLongOr(String key, long fallback) {
        return tag.contains(key) ? tag.getLong(key) : fallback;
    }
}
