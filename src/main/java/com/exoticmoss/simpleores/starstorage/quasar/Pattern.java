package com.exoticmoss.simpleores.starstorage.quasar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;
import java.util.Optional;

/// A recipe saved in a Quasar:
///  - output / count: what it makes;
///  - inputs: the items of each ingredient; each one has the items that can be used (from JEI, e.g. every kind
///    of planks), or none for an empty slot. The first one is used when there are many;
///  - amounts: how many of each ingredient (1 for the crafting table recipes);
///  - coreRecipe / coreTier: for the Gravity Core recipes, the recipe and the smallest tier of core that makes it
///    (empty = a crafting table recipe, with the 9 slots of the grid as inputs);
///  - furnace: a furnace recipe (one input, cooked in the furnaces of the field);
///  - machines: a recipe of a machine of another mod (from JEI): the blocks that make it. The ingredients are put
///    in one of those blocks in the field of the Magnetar, and the result is taken out of it.
public record Pattern(ItemStackTemplate output, int count, List<List<ItemStackTemplate>> inputs, List<Integer> amounts,
                      Optional<ResourceKey<Recipe<?>>> coreRecipe, int coreTier, boolean furnace, List<ItemStackTemplate> machines) {
    public static final int SIZE = 9;
    private static final Codec<ResourceKey<Recipe<?>>> RECIPE_KEY_CODEC = ResourceKey.codec(Registries.RECIPE);

    public static final Codec<Pattern> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStackTemplate.CODEC.fieldOf("output").forGetter(Pattern::output),
            Codec.INT.fieldOf("count").forGetter(Pattern::count),
            ItemStackTemplate.CODEC.listOf().listOf().fieldOf("inputs").forGetter(Pattern::inputs),
            Codec.INT.listOf().optionalFieldOf("amounts", List.of()).forGetter(Pattern::amounts),
            RECIPE_KEY_CODEC.optionalFieldOf("core_recipe").forGetter(Pattern::coreRecipe),
            Codec.INT.optionalFieldOf("core_tier", 0).forGetter(Pattern::coreTier),
            Codec.BOOL.optionalFieldOf("furnace", false).forGetter(Pattern::furnace),
            ItemStackTemplate.CODEC.listOf().optionalFieldOf("machines", List.of()).forGetter(Pattern::machines)
    ).apply(inst, Pattern::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Pattern> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /// A crafting table recipe (the 9 slots of the grid).
    public static Pattern crafting(ItemStackTemplate output, int count, List<List<ItemStackTemplate>> inputs) {
        return new Pattern(output, count, inputs, List.of(), Optional.empty(), 0, false, List.of());
    }

    /// A furnace recipe (one input).
    public static Pattern furnace(ItemStackTemplate output, int count, ItemStackTemplate input) {
        return new Pattern(output, count, List.of(List.of(input)), List.of(), Optional.empty(), 0, true, List.of());
    }

    /// A recipe of a machine (the 9 slots of the grid, each with its amount).
    public static Pattern machine(ItemStackTemplate output, int count, List<List<ItemStackTemplate>> inputs, List<Integer> amounts,
                                  List<ItemStackTemplate> machines) {
        return new Pattern(output, count, inputs, amounts, Optional.empty(), 0, false, machines);
    }

    /// Gravity Core recipes need level 2 (Stellar Terminal II, Quasar); crafting table and furnace recipes work in
    /// every terminal.
    public boolean advanced() {
        return isCore();
    }

    /// Recipes that take time (Gravity Core, furnace, machine): a plan with them becomes a Job of a Quasar.
    public boolean takesTime() {
        return isCore() || furnace || isMachine();
    }

    public boolean isMachine() {
        return !machines.isEmpty();
    }

    /// True if the block is one of the machines of this recipe.
    public boolean madeBy(net.minecraft.world.level.block.state.BlockState state) {
        net.minecraft.world.item.Item item = state.getBlock().asItem();
        return machines.stream().anyMatch(machine -> machine.create().is(item));
    }

    public boolean isCore() {
        return coreRecipe.isPresent();
    }

    /// How many of the ingredient "index" are needed.
    public int amount(int index) {
        return index < amounts.size() ? amounts.get(index) : 1;
    }

    public ItemStack outputStack() {
        ItemStack stack = output.create();
        stack.setCount(count);
        return stack;
    }

    /// True if this pattern makes this item.
    public boolean makes(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(output.create(), stack);
    }
}
