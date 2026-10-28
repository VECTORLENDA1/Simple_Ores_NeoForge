package com.exoticmoss.simpleores.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;

/// Writes the data of a block entity (the way later versions of Minecraft do it): a value with its codec, or a number.
public record ValueOutput(CompoundTag tag, HolderLookup.Provider registries) {
    public <T> void store(String key, Codec<T> codec, T value) {
        codec.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), value).result().ifPresent(nbt -> tag.put(key, nbt));
    }

    public <T> void storeNullable(String key, Codec<T> codec, T value) {
        if (value != null) store(key, codec, value);
    }

    public void putInt(String key, int value) {
        tag.putInt(key, value);
    }

    public void putLong(String key, long value) {
        tag.putLong(key, value);
    }
}
