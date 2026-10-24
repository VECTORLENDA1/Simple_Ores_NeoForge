package com.exoticmoss.simpleores;

import net.minecraft.resources.ResourceKey;

import java.util.function.Supplier;

/// A registered object (item, block, ...) together with its registry key.
/// On Fabric everything is registered right away, so get() simply returns the object
/// (it replaces NeoForge's DeferredItem / DeferredBlock / DeferredHolder).
public final class Registered<R, T extends R> implements Supplier<T> {
    private final ResourceKey<R> key;
    private final T value;

    public Registered(ResourceKey<R> key, T value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    public ResourceKey<R> getKey() {
        return key;
    }
}
