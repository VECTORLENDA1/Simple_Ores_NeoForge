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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// JEI's "Move Items" (+) button for Gravity Core recipes.
///
/// How it works:
///  1. the player opens their inventory, finds a Gravity Core recipe in JEI and clicks "+";
///  2. the client sends this message to the server (with the recipe ID);
///  3. the server takes the ingredients from the player's inventory and puts them
///     into the orbit of the nearest Gravity Core (within RANGE blocks).
///
/// Shift + click moves as many sets of ingredients as the player has (up to MAX_SETS).
public record MoveItemsToCore(ResourceKey<Recipe<?>> recipeId, boolean moveAll) implements CustomPacketPayload {
    /// How far (in blocks) the player can be from the core.
    public static final int RANGE = 4;
    /// Maximum number of ingredient sets moved with shift + click.
    public static final int MAX_SETS = 64;

    // How the message is identified and sent over the network
    public static final Type<MoveItemsToCore> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "move_items_to_core"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MoveItemsToCore> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.RECIPE), MoveItemsToCore::recipeId,
            ByteBufCodecs.BOOL, MoveItemsToCore::moveAll,
            MoveItemsToCore::new);

    @Override
    public Type<MoveItemsToCore> type() {
        return TYPE;
    }

    /// Runs on the server when the message arrives. It checks everything again,
    /// because the server never trusts the client.
    public static void handle(MoveItemsToCore message, IPayloadContext context) {
        Player player = context.player();
        if (!(player.level() instanceof ServerLevel level)) return;

        Optional<RecipeHolder<?>> holder = level.recipeAccess().byKey(message.recipeId());
        if (holder.isEmpty() || !(holder.get().value() instanceof GravityCoreRecipe recipe)) return;

        GravityCoreBlockEntity core = findCore(player, recipe);
        if (core == null) return;

        int sets = Math.min(setsInInventory(player, recipe), message.moveAll() ? MAX_SETS : 1);
        if (sets <= 0) return;

        List<ItemStack> taken = takeFromInventory(player, recipe, sets);
        for (ItemStack leftover : core.insertFromPlayer(player, taken)) {
            player.getInventory().placeItemBackInInventory(leftover);
        }
    }

    /// Finds the nearest Gravity Core (within RANGE) that can make this recipe and isn't busy. null = none found.
    public static @Nullable GravityCoreBlockEntity findCore(Player player, GravityCoreRecipe recipe) {
        BlockPos center = player.blockPosition();
        GravityCoreBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RANGE, -RANGE, -RANGE), center.offset(RANGE, RANGE, RANGE))) {
            if (player.level().getBlockEntity(pos) instanceof GravityCoreBlockEntity core
                    && !core.isBusy() && recipe.worksInTier(core.getTier().level)) {
                double distance = pos.distSqr(center);
                if (distance < bestDistance) {
                    best = core;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    /// How many items of this ingredient the player has (armor and offhand are not counted).
    public static int countInInventory(Player player, GravityCoreRecipe.Part part) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (part.item().test(stack)) count += stack.getCount();
        }
        return count;
    }

    /// How many full sets of ingredients the player has (0 = something is missing).
    public static int setsInInventory(Player player, GravityCoreRecipe recipe) {
        int sets = Integer.MAX_VALUE;
        for (GravityCoreRecipe.Part part : recipe.getParts()) {
            sets = Math.min(sets, countInInventory(player, part) / part.count());
        }
        return sets;
    }

    /// Removes the ingredients for the given number of sets from the player's inventory and returns them.
    private static List<ItemStack> takeFromInventory(Player player, GravityCoreRecipe recipe, int sets) {
        List<ItemStack> taken = new ArrayList<>();
        for (GravityCoreRecipe.Part part : recipe.getParts()) {
            int missing = part.count() * sets;
            for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
                if (missing <= 0) break;
                if (part.item().test(stack)) {
                    ItemStack piece = stack.split(Math.min(missing, stack.getCount()));
                    missing -= piece.getCount();
                    taken.add(piece);
                }
            }
        }
        player.getInventory().setChanged();
        return taken;
    }
}
