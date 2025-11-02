package com.vector.simpleores.block.entity.custom;

import com.vector.simpleores.block.entity.ModBlockEntities;
import com.vector.simpleores.recipe.ModRecipes;
import com.vector.simpleores.recipe.SimpleCraftingTableRecipe;
import com.vector.simpleores.recipe.SimpleCraftingTableRecipeInput;
import com.vector.simpleores.screen.custom.SimpleCraftingTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public class SimpleCraftingTableEntity extends BlockEntity implements MenuProvider {
    public boolean isUpdating = false;
    public final ItemStackHandler itemHandler = new ItemStackHandler(26) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot == OUTPUT_SLOT || isUpdating) return;
            setChanged();
            if (!level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                updateResult(0);
            }
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == OUTPUT_SLOT) {
                Optional<RecipeHolder<SimpleCraftingTableRecipe>> opt = getCurrentRecipe();
                if (opt.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                SimpleCraftingTableRecipe recipe = opt.get().value();
                ItemStack currentOutput = super.getStackInSlot(slot);

                if (currentOutput.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                int canExtract = Math.min(amount, currentOutput.getCount());
                if (canExtract <= 0) {
                    return ItemStack.EMPTY;
                }

                ItemStack toExtract = currentOutput.copy();
                toExtract.setCount(canExtract);

                if (!simulate) {
                    int extractedCrafts = canExtract / recipe.getResultItem(level.registryAccess()).getCount();
                    if (extractedCrafts > 0) {
                        consumeIngredients(extractedCrafts);
                    }
                    super.extractItem(slot, canExtract, false);
                    updateResult(0);
                }

                return toExtract;
            }
            return super.extractItem(slot, amount, simulate);
        }
    };

    public static final int[] INPUT_SLOT = new int[25];
    public static final int OUTPUT_SLOT = 25;

    public Lazy<IItemHandler> lazyItemHandler = Lazy.of(() -> itemHandler);

    public SimpleCraftingTableEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.SIMPLE_CRAFTING_TABLE_BE.get(), pPos, pBlockState);
        for (int i = 0; i < 25; i++) {
            INPUT_SLOT[i] = i;
        }
    }

    public void updateResult(int p) {
        Optional<RecipeHolder<SimpleCraftingTableRecipe>> opt = getCurrentRecipe();
        if (opt.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
            return;
        }
        SimpleCraftingTableRecipe recipe = opt.get().value();
        int maxCrafts = Integer.MAX_VALUE;
        for (int i = 0; i < INPUT_SLOT.length; i++) {
            int req = recipe.getRequiredCountForSlot(i);
            if (req > 0) {
                ItemStack in = itemHandler.getStackInSlot(i);
                maxCrafts = Math.min(maxCrafts, in.getCount() / req);
            }
        }
        if (maxCrafts <= 0) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
        } else {
            ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
            if (result.getMaxStackSize() == 1) {
                result.setCount(1);
            } else {
                result.setCount(result.getCount() * maxCrafts);
            }
            itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        }
    }

    public void consumeIngredients(int times) {
        Optional<RecipeHolder<SimpleCraftingTableRecipe>> opt = getCurrentRecipe();
        if (opt.isEmpty()) return;
        SimpleCraftingTableRecipe recipe = opt.get().value();

        isUpdating = true;
        try {
            for (int i = 0; i < INPUT_SLOT.length; i++) {
                int req = recipe.getRequiredCountForSlot(i);
                if (req > 0) {
                    itemHandler.extractItem(i, req * times, false);
                }
            }
        } finally {
            isUpdating = false;
        }

        setChanged();
        if (!level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        updateResult(0);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = Lazy.of(() -> itemHandler);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.put("inventory", itemHandler.serializeNBT(pRegistries));
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        itemHandler.deserializeNBT(pRegistries, pTag.getCompound("inventory"));
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (level.isClientSide) return;

        boolean isEmpty = true;
        for (int i = 0; i < INPUT_SLOT.length; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                isEmpty = false;
                break;
            }
        }

        if (isEmpty) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
        }
    }

    public Optional<RecipeHolder<SimpleCraftingTableRecipe>> getCurrentRecipe() {
        return this.level.getRecipeManager()
                .getRecipeFor(ModRecipes.SIMPLE_CRAFTING_TABLE_TYPE.get(),
                        new SimpleCraftingTableRecipeInput(itemHandler), level);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (i!= OUTPUT_SLOT) {
                inventory.setItem(i, itemHandler.getStackInSlot(i));
            }
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.weaponseffect.simple_crafting_table");
    }


    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SimpleCraftingTableMenu(id, playerInv, this, null);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }
}
