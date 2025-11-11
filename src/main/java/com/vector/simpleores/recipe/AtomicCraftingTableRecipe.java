package com.vector.simpleores.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class AtomicCraftingTableRecipe implements Recipe<AtomicCraftingTableRecipeInput> {
    public static final int WIDTH = 9;
    public static final int HEIGHT = 9;

    private final NonNullList<Ingredient> recipeItems;
    private final ItemStack result;
    private final String[] pattern;
    private final Map<Character, Ingredient> ingredient;

    public AtomicCraftingTableRecipe(String[] pattern, Map<Character, Ingredient> keys, ItemStack result) {
        this.pattern = pattern;
        this.ingredient = keys;
        this.result = result;
        this.recipeItems = createRecipeItems(pattern, keys, WIDTH, HEIGHT);
    }

    public int getRequiredCountForSlot(int slotIndex) {
        Ingredient ing = recipeItems.get(slotIndex);
        return ing == Ingredient.EMPTY ? 0 : 1;
    }

    private static NonNullList<Ingredient> createRecipeItems(String[] pattern, Map<Character, Ingredient> keys, int width, int height) {
        NonNullList<Ingredient> items = NonNullList.withSize(width * height, Ingredient.EMPTY);
        for (int i = 0; i < pattern.length; i++) {
            for (int j = 0; j < pattern[i].length(); j++) {
                char c = pattern[i].charAt(j);
                items.set(i * width + j, keys.getOrDefault(c, Ingredient.EMPTY));
            }
        }
        return items;
    }

    @Override
    public boolean matches(AtomicCraftingTableRecipeInput inv, Level level) {
        for (int i = 0; i < WIDTH * HEIGHT; i++) {
            ItemStack slotStack = inv.getItem(i);
            Ingredient ingredient = recipeItems.get(i);

            if (!ingredient.test(slotStack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return recipeItems;
    }

    @Override
    public ItemStack assemble(AtomicCraftingTableRecipeInput pContainer, HolderLookup.Provider pRegistries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return pWidth >= WIDTH && pHeight >= HEIGHT;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider pRegistries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ATOMIC_CRAFTING_TABLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ATOMIC_CRAFTING_TABLE_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<AtomicCraftingTableRecipe> {
        public static final MapCodec<AtomicCraftingTableRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.STRING.listOf().fieldOf("pattern").forGetter(recipe -> Arrays.asList(recipe.pattern)),
                Codec.unboundedMap(Codec.STRING, Ingredient.CODEC).fieldOf("ingredient")
                        .forGetter(recipe -> recipe.ingredient.entrySet().stream()
                                .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue))),
                ItemStack.CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
        ).apply(inst, (pattern, key, result) -> {
            Map<Character, Ingredient> charMap = key.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().charAt(0),
                            Map.Entry::getValue
                    ));
            return new AtomicCraftingTableRecipe(pattern.toArray(new String[0]), charMap, result);
        }));

        public static final StreamCodec<RegistryFriendlyByteBuf, AtomicCraftingTableRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

        @Override
        public MapCodec<AtomicCraftingTableRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AtomicCraftingTableRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
