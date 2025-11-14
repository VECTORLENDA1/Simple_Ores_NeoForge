package com.vector.simpleores.screen.custom;

import com.vector.simpleores.block.ModBlocks;
import com.vector.simpleores.block.entity.custom.SimpleCraftingTableEntity;
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

public class SimpleCraftingTableMenu extends AbstractContainerMenu {
    public final SimpleCraftingTableEntity blockEntity;
    public final Level level;

    public SimpleCraftingTableMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(26));
    }

    public SimpleCraftingTableMenu(int pContainerId, Inventory inv, BlockEntity Entity, ContainerData data) {
        super(ModMenuTypes.SIMPLE_CRAFTING_TABLE_MENU.get(), pContainerId);
        this.blockEntity = ((SimpleCraftingTableEntity) Entity);
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        /// INPUT_SLOTS ///
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 0, 18, 18));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 1, 36, 18));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 2, 54, 18));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 3, 72, 18));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 4, 90, 18));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 5, 18, 36));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 6, 36, 36));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 7, 54, 36));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 8, 72, 36));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 9, 90, 36));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 10, 18, 54));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 11, 36, 54));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 12, 54, 54));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 13, 72, 54));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 14, 90, 54));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 15, 18, 72));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 16, 36, 72));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 17, 54, 72));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 18, 72, 72));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 19, 90, 72));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 20, 18, 90));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 21, 36, 90));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 22, 54, 90));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 23, 72, 90));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 24, 90, 90));

        /// OUTPUT_SLOT ///
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 25, 145, 55) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                /// Don't call super.onTake() to avoid duplicate extraction
                if (level.isClientSide()) return;

                /// Perform consumption when player manually takes from output slot (not shift-click)
                Optional<RecipeHolder<SimpleCraftingTableRecipe>> recipeOptional = blockEntity.getCurrentRecipe();
                if (recipeOptional.isPresent()) {
                    SimpleCraftingTableRecipe recipe = recipeOptional.get().value();
                    int perCraft = Math.max(1, recipe.getResultItem(level.registryAccess()).getCount());
                    int taken = Math.max(1, stack.getCount());
                    int crafts = Math.max(1, taken / perCraft);
                    blockEntity.consumeIngredients(crafts);
                    blockEntity.updateResult(0);
                    SimpleCraftingTableMenu.this.broadcastChanges();
                } else if (blockEntity.hasVanillaMatch()) {
                    blockEntity.consumeVanillaOnce();
                    blockEntity.updateResult(0);
                    SimpleCraftingTableMenu.this.broadcastChanges();
                }
            }
        });
    }

    public static final int HOTBAR_SLOT_COUNT = 9;
    public static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    public static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    public static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    public static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    public static final int VANILLA_FIRST_SLOT_INDEX = 0;
    public static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    /// THIS YOU HAVE TO DEFINE!
    public static final int TE_INVENTORY_SLOT_COUNT = 26; /// must be the number of slots you have!

    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = this.slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = sourceSlot.getItem();
        ItemStack sourceStack = stackInSlot.copy();

        int teSlotStart = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
        int teOutputSlot = teSlotStart + 25;

        /// Shift-clicking FROM output slot
        if (pIndex == teOutputSlot) {
            Optional<RecipeHolder<SimpleCraftingTableRecipe>> recipeOptional = blockEntity.getCurrentRecipe();

            if (recipeOptional.isPresent()) {
                /// Handle custom 5x5 recipes
                SimpleCraftingTableRecipe recipe = recipeOptional.get().value();
                ItemStack resultItem = recipe.getResultItem(level.registryAccess());
                int resultCountPerCraft = resultItem.getCount();

                /// Calculate max possible crafts
                int maxCraftsPossible = Integer.MAX_VALUE;
                for (int i = 0; i < SimpleCraftingTableEntity.INPUT_SLOT.length; i++) {
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
                pPlayer, ModBlocks.SIMPLE_CRAFTING_TABLE.get());
    }

    public void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 124 + i * 18));
            }
        }
    }

    public void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 182));
        }
    }
}