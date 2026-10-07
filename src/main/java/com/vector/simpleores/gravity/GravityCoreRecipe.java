package com.vector.simpleores.gravity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vector.simpleores.recipe.ModRecipes;
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
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

/// Uma receita do Nucleo Gravitacional. Exemplo de ficheiro JSON
/// (em data/simpleores/recipe/gravity_core/):
///
/// {
///   "type": "simpleores:gravity_collapse",
///   "tier": 1,                                         <- nivel minimo do nucleo
///   "time": 100,                                       <- duracao do colapso em ticks (20 ticks = 1 segundo)
///   "ingredients": [
///     { "item": "simpleores:raw_ignithra", "count": 4 },
///     { "item": "#c:ingots/iron" }                     <- "#" = tag; "count" e opcional (1 por defeito)
///   ],
///   "result": { "id": "simpleores:ignithra", "count": 6 }
/// }
public class GravityCoreRecipe implements Recipe<GravityCoreRecipe.Input> {

    /// Um ingrediente com quantidade (ex: 4x raw_ignithra).
    public record Part(Ingredient item, int count) {
        public static final Codec<Part> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Ingredient.CODEC.fieldOf("item").forGetter(Part::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Part::count)
        ).apply(inst, Part::new));
    }

    /// O que esta em orbita num nucleo: os itens e o nivel do nucleo.
    public record Input(List<ItemStack> items, int tier) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    private final int tier;
    private final int time;
    private final List<Part> ingredients;
    private final ItemStackTemplate result;

    public GravityCoreRecipe(int tier, int time, List<Part> ingredients, ItemStackTemplate result) {
        this.tier = tier;
        this.time = time;
        this.ingredients = ingredients;
        this.result = result;
    }

    public int getTier() {
        return tier;
    }

    public int getTime() {
        return time;
    }

    public List<Part> getParts() {
        return ingredients;
    }

    public ItemStackTemplate getResult() {
        return result;
    }

    /// A receita serve se o nucleo tiver o nivel necessario e houver itens suficientes de cada ingrediente.
    @Override
    public boolean matches(Input input, Level level) {
        if (input.tier() < tier) return false;

        for (Part part : ingredients) {
            int found = 0;
            for (ItemStack stack : input.items()) {
                if (part.item().test(stack)) found += stack.getCount();
            }
            if (found < part.count()) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(Input input) {
        return result.create();
    }

    // As receitas do nucleo nao aparecem no livro de receitas vanilla
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
    public RecipeSerializer<GravityCoreRecipe> getSerializer() {
        return ModRecipes.GRAVITY_COLLAPSE_SERIALIZER.get();
    }

    @Override
    public RecipeType<GravityCoreRecipe> getType() {
        return ModRecipes.GRAVITY_COLLAPSE_TYPE.get();
    }

    // Como ler/escrever a receita em JSON (CODEC) e enviar pela rede (STREAM_CODEC)
    public static final MapCodec<GravityCoreRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.INT.optionalFieldOf("tier", 1).forGetter(GravityCoreRecipe::getTier),
            Codec.INT.optionalFieldOf("time", 100).forGetter(GravityCoreRecipe::getTime),
            Part.CODEC.listOf().fieldOf("ingredients").forGetter(GravityCoreRecipe::getParts),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(GravityCoreRecipe::getResult)
    ).apply(inst, GravityCoreRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GravityCoreRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
}
