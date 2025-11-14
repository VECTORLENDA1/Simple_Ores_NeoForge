package com.vector.simpleores.screen.custom;

import com.vector.simpleores.block.ModBlocks;
import com.vector.simpleores.block.entity.custom.AtomicCraftingTableEntity;
import com.vector.simpleores.block.entity.custom.SimpleCraftingTableEntity;
import com.vector.simpleores.recipe.AtomicCraftingTableRecipe;
import com.vector.simpleores.recipe.SimpleCraftingTableRecipe;
import com.vector.simpleores.screen.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.Optional;

public class AtomicCraftingTableMenu extends AbstractContainerMenu {
    public final AtomicCraftingTableEntity blockEntity;
    public final Level level;
    private boolean suppressOutputOnTakeOnce = false;

    public AtomicCraftingTableMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(82));
    }

    public AtomicCraftingTableMenu(int pContainerId, Inventory inv, BlockEntity Entity, ContainerData data) {
        super(ModMenuTypes.ATOMIC_CRAFTING_TABLE_MENU.get(), pContainerId);
        this.blockEntity = ((AtomicCraftingTableEntity) Entity);
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);


        /// INPUT_SLOTS ///
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 0, 18, 18));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 1, 36, 18));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 2, 54, 18));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 3, 72, 18));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 4, 90, 18));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 5, 108, 18));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 6, 126, 18));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 7, 144, 18));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 8, 162, 18));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 9, 18, 36));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 10, 36, 36));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 11, 54, 36));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 12, 72, 36));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 13, 90, 36));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 14, 108, 36));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 15, 126, 36));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 16, 144, 36));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 17, 162, 36));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 18, 18, 54));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 19, 36, 54));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 20, 54, 54));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 21, 72, 54));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 22, 90, 54));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 23, 108, 54));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 24, 126, 54));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 25, 144, 54));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 26, 162, 54));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 27, 18, 72));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 28, 36, 72));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 29, 54, 72));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 30, 72, 72));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 31, 90, 72));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 32, 108, 72));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 33, 126, 72));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 34, 144, 72));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 35, 162, 72));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 36, 18, 90));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 37, 36, 90));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 38, 54, 90));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 39, 72, 90));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 40, 90, 90));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 41, 108, 90));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 42, 126, 90));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 43, 144, 90));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 44, 162, 90));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 45, 18, 108));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 46, 36, 108));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 47, 54, 108));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 48, 72, 108));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 49, 90, 108));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 50, 108, 108));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 51, 126, 108));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 52, 144, 108));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 53, 162, 108));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 54, 18, 126));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 55, 36, 126));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 56, 54, 126));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 57, 72, 126));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 58, 90, 126));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 59, 108, 126));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 60, 126, 126));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 61, 144, 126));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 62, 162, 126));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 63, 18, 144));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 64, 36, 144));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 65, 54, 144));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 66, 72, 144));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 67, 90, 144));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 68, 108, 144));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 69, 126, 144));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 70, 144, 144));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 71, 162, 144));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 72, 18, 162));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 73, 36, 162));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 74, 54, 162));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 75, 72, 162));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 76, 90, 162));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 77, 108, 162));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 78, 126, 162));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 79, 144, 162));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 80, 162, 162));

        /// OUTPUT_SLOTS ///
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 81, 218, 90) {




            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                if (level.isClientSide()) return;

                if (suppressOutputOnTakeOnce) {
                    suppressOutputOnTakeOnce = false;
                    return;
                }

                /// Perform consumption when player manually takes from output slot
                Optional<RecipeHolder<AtomicCraftingTableRecipe>> recipeOptional = blockEntity.getCurrentRecipe();
                if (recipeOptional.isPresent()) {
                    AtomicCraftingTableRecipe recipe = recipeOptional.get().value();
                    int perCraft = Math.max(1, recipe.getResultItem(level.registryAccess()).getCount());
                    int taken = Math.max(1, stack.getCount());
                    int crafts = Math.max(1, taken / perCraft);
                    blockEntity.consumeIngredients(crafts);
                    blockEntity.updateResult(0);
                    AtomicCraftingTableMenu.this.broadcastChanges();
                } else if (blockEntity.hasVanillaMatch()) {
                    blockEntity.consumeVanillaOnce();
                    blockEntity.updateResult(0);
                    AtomicCraftingTableMenu.this.broadcastChanges();
                }
            }
        });
    }


    /// CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    /// must assign a slot number to each of the slots used by the GUI.
    /// For this container, we can see both the tile inventory's slots as well as the player inventory slots and the hotbar.
    /// Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    ///  0 - 8 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 - 8)
    ///  9 - 35 = player inventory slots (which map to the InventoryPlayer slot numbers 9 - 35)
    ///  36 - 44 = TileInventory slots, which map to our TileEntity slot numbers 0 - 8)
    public static final int HOTBAR_SLOT_COUNT = 9;
    public static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    public static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    public static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    public static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    public static final int VANILLA_FIRST_SLOT_INDEX = 0;
    public static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    /// THIS YOU HAVE TO DEFINE!
    public static final int TE_INVENTORY_SLOT_COUNT = 82;  /// must be the number of slots you have!

    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = this.slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = sourceSlot.getItem();
        ItemStack sourceStack = stackInSlot.copy();

        int teSlotStart = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
        int teOutputSlot = teSlotStart + 81;

        /// Shift-clicking FROM output slot
        if (pIndex == teOutputSlot) {
            Optional<RecipeHolder<AtomicCraftingTableRecipe>> recipeOptional = blockEntity.getCurrentRecipe();

            if (recipeOptional.isPresent()) {
                /// Handle custom 9x9 recipes
                AtomicCraftingTableRecipe recipe = recipeOptional.get().value();
                ItemStack resultItem = recipe.getResultItem(level.registryAccess());
                int resultCountPerCraft = resultItem.getCount();

                /// Calculate max possible crafts
                int maxCraftsPossible = Integer.MAX_VALUE;
                for (int i = 0; i < AtomicCraftingTableEntity.INPUT_SLOT.length; i++) {
                    int required = recipe.getRequiredCountForSlot(i);
                    if (required > 0) {
                        ItemStack ingredient = blockEntity.itemHandler.getStackInSlot(i);
                        if (ingredient.isEmpty() || ingredient.getCount() < required) {
                            maxCraftsPossible = 0;
                            break;
                        }
                        maxCraftsPossible = Math.min(maxCraftsPossible, ingredient.getCount() / required);
                    }
                }

                if (maxCraftsPossible > 0) {
                    ItemStack toMove = resultItem.copy();
                    toMove.setCount(resultCountPerCraft * maxCraftsPossible);
                    int originalCount = toMove.getCount();

                    /// Try to move to player inventory
                    if (this.moveItemStackTo(toMove, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, true)) {
                        int movedCount = originalCount - toMove.getCount();
                        int actualMovedCrafts = movedCount / resultCountPerCraft;
                        if (actualMovedCrafts > 0) {
                            /// Consume ingredients for all crafts that were moved
                            blockEntity.consumeIngredients(actualMovedCrafts);
                            blockEntity.updateResult(0);
                            this.broadcastChanges();

                            /// Clear the output slot to prevent duplicate extraction
                            sourceSlot.set(ItemStack.EMPTY);
                            return sourceStack;
                        }
                    }
                }
            } else if (blockEntity.hasVanillaMatch()) {
                /// Handle vanilla 3x3 recipes
                int totalMoved = 0;
                int safety = 256;

                while (safety-- > 0 && blockEntity.hasVanillaMatch()) {
                    ItemStack preview = blockEntity.getVanillaResultPreview();
                    if (preview.isEmpty()) break;

                    int maxStack = preview.getMaxStackSize();
                    int remainingToFill = Math.max(0, maxStack - totalMoved);
                    if (remainingToFill <= 0) break;

                    ItemStack toMove = preview.copy();
                    if (toMove.getCount() > remainingToFill) {
                        toMove.setCount(remainingToFill);
                    }

                    int original = toMove.getCount();
                    if (!this.moveItemStackTo(toMove, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, true)) {
                        break;
                    }

                    int moved = original - toMove.getCount();
                    if (moved <= 0) break;

                    totalMoved += moved;

                    /// Consume one vanilla craft
                    blockEntity.consumeVanillaOnce();
                    blockEntity.updateResult(0);
                    this.broadcastChanges();

                    if (totalMoved >= maxStack) break;
                }

                if (totalMoved > 0) {
                    /// Clear the output slot
                    sourceSlot.set(ItemStack.EMPTY);
                    return sourceStack;
                }
            }

            return ItemStack.EMPTY;
        }
        /// Shift-clicking FROM player inventory TO crafting grid
        else if (pIndex < VANILLA_SLOT_COUNT) {
            if (!this.moveItemStackTo(stackInSlot, teSlotStart, teOutputSlot, false)) {
                return ItemStack.EMPTY;
            }
        }
        /// Shift-clicking FROM crafting grid TO player inventory
        else if (pIndex >= teSlotStart && pIndex < teOutputSlot) {
            if (!this.moveItemStackTo(stackInSlot, VANILLA_FIRST_SLOT_INDEX, VANILLA_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        }

        if (stackInSlot.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        if (stackInSlot.getCount() == sourceStack.getCount()) {
            return ItemStack.EMPTY;
        }

        return sourceStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                pPlayer, ModBlocks.ATOMIC_CRAFTING_TABLE.get());
    }

    public void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 45 + j * 18, 197 + i * 18));
            }
        }
    }

    public void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 45 + i * 18, 255));
        }
    }
}