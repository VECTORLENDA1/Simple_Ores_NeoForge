package com.vector.simpleores.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SimpleCraftingTableRecipe implements Recipe<SimpleCraftingTableRecipeInput> {
    public static final int WIDTH = 5;
    public static final int HEIGHT = 5;

    // Ingredient.EMPTY ja nao existe: uma posicao vazia do padrao e agora um Optional vazio
    private final List<Optional<Ingredient>> recipeItems;
    private final ItemStackTemplate result;
    private final String[] pattern;
    private final Map<Character, Ingredient> ingredient;

    public SimpleCraftingTableRecipe(String[] pattern, Map<Character, Ingredient> keys, ItemStackTemplate result) {
        this.pattern = pattern;
        this.ingredient = keys;
        this.result = result;
        this.recipeItems = createRecipeItems(pattern, keys, WIDTH, HEIGHT);
    }

    public int getRequiredCountForSlot(int slotIndex) {
        return recipeItems.get(slotIndex).isPresent() ? 1 : 0;
    }

    private static List<Optional<Ingredient>> createRecipeItems(String[] pattern, Map<Character, Ingredient> keys, int width, int height) {
        List<Optional<Ingredient>> items = new ArrayList<>(Collections.nCopies(width * height, Optional.empty()));
        for (int i = 0; i < pattern.length; i++) {
            for (int j = 0; j < pattern[i].length(); j++) {
                char c = pattern[i].charAt(j);
                items.set(i * width + j, Optional.ofNullable(keys.get(c)));
            }
        }
        return List.copyOf(items);
    }

    @Override
    public boolean matches(SimpleCraftingTableRecipeInput inv, Level level) {
        for (int i = 0; i < WIDTH * HEIGHT; i++) {
            ItemStack slotStack = inv.getItem(i);
            Optional<Ingredient> ingredient = recipeItems.get(i);

            // Posicao vazia no padrao exige slot vazio; caso contrario o item tem de corresponder
            boolean ok = ingredient.map(ing -> ing.test(slotStack)).orElse(slotStack.isEmpty());
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    public List<Optional<Ingredient>> getIngredients() {
        return recipeItems;
    }

    public ItemStackTemplate getResult() {
        return result;
    }

    public ItemStack getResultItem() {
        return result.create();
    }

    @Override
    public ItemStack assemble(SimpleCraftingTableRecipeInput pContainer) {
        return result.create();
    }

    // Estas receitas nao aparecem no livro de receitas vanilla
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<SimpleCraftingTableRecipe> getSerializer() {
        return ModRecipes.SIMPLE_CRAFTING_TABLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<SimpleCraftingTableRecipe> getType() {
        return ModRecipes.SIMPLE_CRAFTING_TABLE_TYPE.get();
    }

    // RecipeSerializer passou a ser um record (codec + streamCodec), por isso basta expor os codecs
    public static final MapCodec<SimpleCraftingTableRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.STRING.listOf().fieldOf("pattern").forGetter(recipe -> Arrays.asList(recipe.pattern)),
            Codec.unboundedMap(Codec.STRING, Ingredient.CODEC).fieldOf("ingredient")
                    .forGetter(recipe -> recipe.ingredient.entrySet().stream()
                            .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue))),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
    ).apply(inst, (pattern, key, result) -> {
        Map<Character, Ingredient> charMap = key.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().charAt(0),
                        Map.Entry::getValue
                ));
        return new SimpleCraftingTableRecipe(pattern.toArray(new String[0]), charMap, result);
    }));

    public static final StreamCodec<RegistryFriendlyByteBuf, SimpleCraftingTableRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
}
