package com.exoticmoss.simpleores.starstorage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/// The menu of the Stellar Terminal: a normal crafting grid (3x3 + result) and the player's inventory.
/// The items of the network are not slots: the screen shows them and asks the server (TerminalAction).
///  - Shift + click on an item of the inventory stores it in the network.
///  - Closing the screen gives back the items left in the crafting grid.
public class TerminalMenu extends AbstractContainerMenu {
    public static final int RESULT_SLOT = 0;
    public static final int CRAFT_START = 1;
    public static final int CRAFT_END = 10;
    public static final int INVENTORY_START = 10;
    public static final int INVENTORY_END = 46;

    public final BlockPos pos;
    public final TerminalLayout layout;
    private final Player player;
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();

    public TerminalMenu(int id, Inventory inventory, BlockPos pos, int rows) {
        super(ModMenus.TERMINAL.get(), id);
        this.pos = pos;
        this.layout = new TerminalLayout(rows);
        this.player = inventory.player;
        int top = layout.inventoryTop();

        addSlot(new ResultSlot(player, craftSlots, resultSlots, 0, layout.resultX() + 1, layout.resultY() + 1));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(craftSlots, column + row * 3,
                        TerminalLayout.craftLeft() + column * TerminalLayout.SLOT + 1, top + row * TerminalLayout.SLOT + 1));
            }
        }
        addStandardInventorySlots(inventory, TerminalLayout.inventoryLeft() + 1, top + 1);
    }

    // ---------------------------------------------------------------- crafting

    @Override
    public void slotsChanged(Container container) {
        if (container == craftSlots && player.level() instanceof ServerLevel level) updateResult(level);
        super.slotsChanged(container);
    }

    /// Works out what the crafting grid makes.
    private void updateResult(ServerLevel level) {
        CraftingInput input = craftSlots.asCraftInput();
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> recipe = input.isEmpty() ? Optional.empty()
                : level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (recipe.isPresent()) {
            result = recipe.get().value().assemble(input);
            resultSlots.setRecipeUsed(recipe.get());
        } else {
            resultSlots.setRecipeUsed(null);
        }
        resultSlots.setItem(0, result);
        broadcastChanges();
    }

    /// Closing the screen: the items left in the crafting grid go back to the player.
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) clearContainer(player, craftSlots);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).getBlock() instanceof StellarTerminalBlock
                && player.position().distanceToSqr(Vec3.atCenterOf(pos)) <= TerminalAction.RANGE * TerminalAction.RANGE;
    }

    // ---------------------------------------------------------------- shift + click

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index >= INVENTORY_START) {
            // Inventory -> network (only the server changes the network; the client gets the new inventory)
            if (player instanceof ServerPlayer serverPlayer) {
                ItemStack left = StarNetwork.at(player.level(), pos).insert(stack.copy());
                if (left.getCount() != stack.getCount()) {
                    slot.set(left);
                    TerminalContents.send(serverPlayer, pos);
                }
            }
            return ItemStack.EMPTY;
        }

        // Result or crafting grid -> inventory
        if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, index == RESULT_SLOT)) return ItemStack.EMPTY;
        if (index == RESULT_SLOT) slot.onQuickCraft(stack, copy);
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        if (index == RESULT_SLOT) player.drop(stack, false);
        return copy;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }
}
