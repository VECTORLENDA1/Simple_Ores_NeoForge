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


        // INPUT_SLOTS //
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

        // OUTPUT_SLOTS //
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 25, 145, 55) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }


    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots as well as the player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    //  0 - 8 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 - 8)
    //  9 - 35 = player inventory slots (which map to the InventoryPlayer slot numbers 9 - 35)
    //  36 - 44 = TileInventory slots, which map to our TileEntity slot numbers 0 - 8)
    public static final int HOTBAR_SLOT_COUNT = 9;
    public static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    public static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    public static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    public static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    public static final int VANILLA_FIRST_SLOT_INDEX = 0;
    public static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    public static final int TE_INVENTORY_SLOT_COUNT = 26;  // must be the number of slots you have!
    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        ItemStack sourceStack = ItemStack.EMPTY;
        Slot sourceSlot = this.slots.get(pIndex);
        int teSlotStart = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
        int teOutputSlot = teSlotStart + 25;

        if (sourceSlot != null && sourceSlot.hasItem()) {
            ItemStack stackInSlot = sourceSlot.getItem();
            sourceStack = stackInSlot.copy();

            if (pIndex == teOutputSlot) {
                Optional<RecipeHolder<SimpleCraftingTableRecipe>> recipeOptional = blockEntity.getCurrentRecipe();

                if (recipeOptional.isPresent()) {
                    SimpleCraftingTableRecipe recipe = recipeOptional.get().value();
                    ItemStack resultItem = recipe.getResultItem(level.registryAccess());
                    int resultCountPerCraft = resultItem.getCount();
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
                        int canMoveTotal = 0;
                        ItemStack toMoveTemplate = resultItem.copy();
                        toMoveTemplate.setCount(resultCountPerCraft * maxCraftsPossible);

                        ItemStack toMove = toMoveTemplate.copy();
                        for (int i = VANILLA_FIRST_SLOT_INDEX; i < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT && !toMove.isEmpty(); ++i) {
                            Slot targetSlot = this.slots.get(i);
                            ItemStack targetStack = targetSlot.getItem();
                            if (targetSlot.mayPlace(toMove)) {
                                int moveAmount = Math.min(toMove.getCount(), targetSlot.getMaxStackSize() - targetStack.getCount());
                                if (moveAmount > 0) {
                                    toMove.shrink(moveAmount);
                                    canMoveTotal += moveAmount;
                                }
                            }
                        }

                        if (canMoveTotal > 0) {
                            int actualMovedItems = Math.min(canMoveTotal, resultCountPerCraft * maxCraftsPossible);
                            ItemStack craftedItem = resultItem.copy();
                            craftedItem.setCount(actualMovedItems);

                            int originalCount = craftedItem.getCount();

                            if (this.moveItemStackTo(craftedItem, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                                int movedCount = originalCount - craftedItem.getCount();
                                int actualMovedCrafts = movedCount / resultCountPerCraft;
                                blockEntity.consumeIngredients(actualMovedCrafts);
                                blockEntity.updateResult(0);
                            }
                        }
                    }
                }

            } else if (pIndex < VANILLA_SLOT_COUNT) {
                if (!this.moveItemStackTo(stackInSlot, teSlotStart, teOutputSlot, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (pIndex >= teSlotStart && pIndex < teOutputSlot) {
                if (!this.moveItemStackTo(stackInSlot, VANILLA_FIRST_SLOT_INDEX, VANILLA_SLOT_COUNT, false)) {
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

            sourceSlot.onTake(playerIn, stackInSlot);
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