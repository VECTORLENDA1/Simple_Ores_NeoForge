package com.vector.simpleores.gravitycraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/// One recipe shown in the selection bar above a Gravity Core.
///
/// The client doesn't decide anything: the server works out the candidates (the recipes that
/// the orbiting items can still become) and sends them to the client with the block update,
/// already with everything the bar needs to draw them (result + what is missing).
///
/// @param id     the recipe ID (sent back to the server when the player picks it)
/// @param result what the recipe gives in this core (always 1 item, the real amount is in "count")
/// @param count  how many items the recipe gives in this core (can be more than a stack, e.g. 128)
/// @param needs  every ingredient: how many are orbiting and how many the recipe needs
public record CoreCandidate(ResourceKey<Recipe<?>> id, ItemStack result, int count, List<Need> needs) {
    public static final Codec<CoreCandidate> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceKey.codec(Registries.RECIPE).fieldOf("id").forGetter(CoreCandidate::id),
            ItemStack.CODEC.fieldOf("result").forGetter(CoreCandidate::result),
            Codec.INT.fieldOf("count").forGetter(CoreCandidate::count),
            Need.CODEC.listOf().fieldOf("needs").forGetter(CoreCandidate::needs)
    ).apply(inst, CoreCandidate::new));

    /// One ingredient of the candidate: icon (first item of the ingredient), amount orbiting and amount needed.
    public record Need(ItemStack icon, int have, int need) {
        public static final Codec<Need> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ItemStack.CODEC.fieldOf("icon").forGetter(Need::icon),
                Codec.INT.fieldOf("have").forGetter(Need::have),
                Codec.INT.fieldOf("need").forGetter(Need::need)
        ).apply(inst, Need::new));

        public boolean done() {
            return have >= need;
        }
    }

    /// True when every ingredient is already orbiting (the recipe can start right now).
    public boolean complete() {
        for (Need need : needs) {
            if (!need.done()) return false;
        }
        return true;
    }

    /// Builds the candidate of a recipe for the given orbiting items and core tier.
    public static CoreCandidate of(RecipeHolder<GravityCoreRecipe> holder, List<ItemStack> items, int coreTier) {
        GravityCoreRecipe recipe = holder.value();
        List<Need> needs = new ArrayList<>();
        for (GravityCoreRecipe.Part part : recipe.getParts()) {
            ItemStack icon = part.item().items().findFirst().map(item -> new ItemStack(item, 1)).orElse(ItemStack.EMPTY);
            if (icon.isEmpty()) continue; // empty tag: nothing to show
            needs.add(new Need(icon, GravityCoreRecipe.countMatching(part, items), part.countFor(coreTier)));
        }
        ItemStack result = recipe.createResult(coreTier);
        // The saved/synced stack can't hold more than 99 items, so the amount goes separately
        return new CoreCandidate(holder.id(), result.copyWithCount(1), result.getCount(), needs);
    }
}