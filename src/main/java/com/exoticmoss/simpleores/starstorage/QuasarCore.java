package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.SimpleOres;
import com.exoticmoss.simpleores.gravitycraft.GravityCoreRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/// Sent by the client when JEI's "+" button is used on a Gravity Core recipe with the Quasar screen open:
/// the recipe is saved in the Quasar (the upgrades of the cores are not allowed).
public record QuasarCore(ResourceKey<Recipe<?>> recipe) implements CustomPacketPayload {
    public static final Type<QuasarCore> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "quasar_core"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuasarCore> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.RECIPE), QuasarCore::recipe,
            QuasarCore::new);

    @Override
    public Type<QuasarCore> type() {
        return TYPE;
    }

    public static void handle(QuasarCore message, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof QuasarMenu menu)) return;
        if (!(player.level().getBlockEntity(menu.pos) instanceof QuasarBlockEntity quasar)) return;
        Optional<RecipeHolder<?>> holder = player.level().recipeAccess().byKey(message.recipe());
        if (holder.isEmpty() || !(holder.get().value() instanceof GravityCoreRecipe recipe) || recipe.getUpgradeTier() != null) return;

        int tier = recipe.getTier();
        List<List<ItemStackTemplate>> inputs = new ArrayList<>();
        List<Integer> amounts = new ArrayList<>();
        for (GravityCoreRecipe.Part part : recipe.getParts()) {
            inputs.add(part.item().items().limit(QuasarFill.MAX_CHOICES)
                    .map(item -> ItemStackTemplate.fromNonEmptyStack(new ItemStack(item))).toList());
            amounts.add(part.countFor(tier));
        }
        ItemStack result = recipe.createResult(tier);
        if (result.isEmpty()) return;
        quasar.addPattern(new Pattern(ItemStackTemplate.fromNonEmptyStack(result.copyWithCount(1)), result.getCount(),
                inputs, amounts, Optional.of(message.recipe()), tier, false));
        QuasarPatterns.send(player, menu.pos);
    }
}
