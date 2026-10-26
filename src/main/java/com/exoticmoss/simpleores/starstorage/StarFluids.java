package com.exoticmoss.simpleores.starstorage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/// Liquids and their buckets (the liquids go in and out of the network with buckets: any liquid that has
/// a bucket, also of other mods).
public final class StarFluids {
    public static final long BUCKET = 1000;
    private static Map<Item, Fluid> byBucket;

    private StarFluids() {
    }

    /// The liquid inside this bucket (null = not a full bucket).
    public static @Nullable Fluid fromBucket(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.BUCKET)) return null;
        if (byBucket == null) {
            Map<Item, Fluid> map = new HashMap<>();
            for (Fluid fluid : BuiltInRegistries.FLUID) {
                if (fluid == Fluids.EMPTY || !fluid.isSource(fluid.defaultFluidState())) continue;
                Item bucket = fluid.getBucket();
                if (bucket != Items.AIR) map.putIfAbsent(bucket, fluid);
            }
            byBucket = map;
        }
        return byBucket.get(stack.getItem());
    }

    /// A full bucket of this liquid (EMPTY if it has no bucket).
    public static ItemStack bucket(Fluid fluid) {
        Item bucket = fluid.getBucket();
        return bucket == Items.AIR ? ItemStack.EMPTY : new ItemStack(bucket);
    }

    public static Component name(Fluid fluid) {
        return fluid.getFluidType().getDescription();
    }

    /// "500 mB", "12 B", "14.4k B".
    public static String buckets(long mb) {
        if (mb < BUCKET) return mb + " mB";
        return StarNetwork.format(mb / BUCKET) + " B";
    }
}
