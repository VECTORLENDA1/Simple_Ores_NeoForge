package com.vector.simpleores.gravitycraft;

import com.vector.simpleores.SimpleOres;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

/// Sent by the client when the player right-clicks a recipe in the selection bar above a Gravity Core
/// (see CoreSelectionBar). An empty recipe means "unpick the recipe that is picked".
public record SelectCoreRecipe(BlockPos pos, Optional<ResourceKey<Recipe<?>>> recipeId) implements CustomPacketPayload {
    /// How far (in blocks) the player can be from the core.
    public static final double RANGE = 4;

    public static final Type<SelectCoreRecipe> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "select_core_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectCoreRecipe> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SelectCoreRecipe::pos,
            ByteBufCodecs.optional(ResourceKey.streamCodec(Registries.RECIPE)), SelectCoreRecipe::recipeId,
            SelectCoreRecipe::new);

    @Override
    public Type<SelectCoreRecipe> type() {
        return TYPE;
    }

    /// Runs on the server. The core itself checks that the recipe is one of its candidates.
    public static void handle(SelectCoreRecipe message, IPayloadContext context) {
        Player player = context.player();
        if (player.position().distanceToSqr(Vec3.atCenterOf(message.pos())) > RANGE * RANGE) return;
        if (player.level().getBlockEntity(message.pos()) instanceof GravityCoreBlockEntity core) {
            core.selectRecipe(player, message.recipeId().orElse(null));
        }
    }
}