package com.vector.simpleores.block.entity.custom;

import com.vector.simpleores.block.entity.ModBlockEntities;
import com.vector.simpleores.recipe.ModRecipes;
import com.vector.simpleores.recipe.SimpleCraftingTableRecipe;
import com.vector.simpleores.recipe.SimpleCraftingTableRecipeInput;
import com.vector.simpleores.screen.custom.SimpleCraftingTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public class SimpleCraftingTableEntity extends BlockEntity implements MenuProvider {
    /// Cache for last matched vanilla crafting bounding box in 5x5 and recipe
    private int lastVanillaMinX = -1;
    private int lastVanillaMinY = -1;
    private int lastVanillaWidth = 0;
    private int lastVanillaHeight = 0;
    @Nullable
    private RecipeHolder<CraftingRecipe> lastVanillaRecipe = null;
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
            /// Do NOT perform crafting consumption logic here; it's handled by the output Slot's onTake/quickMoveStack
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
        /// First try custom 5x5 recipe
        Optional<RecipeHolder<SimpleCraftingTableRecipe>> opt = getCurrentRecipe();
        if (opt.isPresent()) {
            lastVanillaRecipe = null;
            lastVanillaMinX = -1;
            lastVanillaMinY = -1;
            lastVanillaWidth = 0;
            lastVanillaHeight = 0;

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
            return;
        }

        /// Then try vanilla 3x3 recipes anywhere in the 5x5 grid
        Optional<RecipeHolder<CraftingRecipe>> vanillaOpt = findVanillaRecipe();
        if (vanillaOpt.isPresent()) {
            RecipeHolder<CraftingRecipe> rh = vanillaOpt.get();
            ItemStack result = rh.value().getResultItem(level.registryAccess()).copy();
            /// For vanilla we output a single craft result at a time (safer for container items)
            result.setCount(result.getCount());
            itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        } else {
            itemHandler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
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

    private Optional<RecipeHolder<CraftingRecipe>> findVanillaRecipe() {
        lastVanillaRecipe = null;
        lastVanillaMinX = -1;
        lastVanillaMinY = -1;
        lastVanillaWidth = 0;
        lastVanillaHeight = 0;
        if (level == null) return Optional.empty();

        /// Compute tight bounding box of non-empty inputs in the 5x5 grid
        int minX = 5, minY = 5, maxX = -1, maxY = -1;
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                ItemStack st = itemHandler.getStackInSlot(y * 5 + x);
                if (!st.isEmpty()) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return Optional.empty(); // no items
        }
        int width = (maxX - minX + 1);
        int height = (maxY - minY + 1);
        /// Vanilla crafting grid is max 3x3; if the used area exceeds this, no vanilla recipe
        if (width > 3 || height > 3) {
            return Optional.empty();
        }

        CraftingInput input = buildInputForBox(minX, minY, width, height);
        Optional<RecipeHolder<CraftingRecipe>> opt = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level);
        if (opt.isPresent()) {
            lastVanillaRecipe = opt.get();
            lastVanillaMinX = minX;
            lastVanillaMinY = minY;
            lastVanillaWidth = width;
            lastVanillaHeight = height;
        }
        return opt;
    }

    private CraftingInput buildInputForBox(int minX, int minY, int width, int height) {
        NonNullList<ItemStack> items = NonNullList.withSize(width * height, ItemStack.EMPTY);
        int idx = 0;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                int slot = (minY + r) * 5 + (minX + c);
                ItemStack in = itemHandler.getStackInSlot(slot);
                if (!in.isEmpty()) {
                    ItemStack one = in.copy();
                    one.setCount(1);
                    items.set(idx, one);
                } else {
                    items.set(idx, ItemStack.EMPTY);
                }
                idx++;
            }
        }
        return CraftingInput.of(width, height, items);
    }

    public boolean hasVanillaMatch() {
        return lastVanillaRecipe != null;
    }

    public ItemStack getVanillaResultPreview() {
        if (lastVanillaRecipe == null) return ItemStack.EMPTY;
        return lastVanillaRecipe.value().getResultItem(level.registryAccess()).copy();
    }

    public void consumeVanillaOnce() {
        if (lastVanillaRecipe == null || level == null) return;
        CraftingRecipe recipe = lastVanillaRecipe.value();
        /// Build input for the cached bounding box
        int minX = lastVanillaMinX;
        int minY = lastVanillaMinY;
        int width = lastVanillaWidth;
        int height = lastVanillaHeight;
        if (minX < 0 || minY < 0 || width <= 0 || height <= 0) return;
        CraftingInput input = buildInputForBox(minX, minY, width, height);
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(input);

        isUpdating = true;
        try {
            int idx = 0;
            for (int r = 0; r < height; r++) {
                for (int c = 0; c < width; c++) {
                    int slot = (minY + r) * 5 + (minX + c);
                    ItemStack inSlot = itemHandler.getStackInSlot(slot);

                    /// Consume one from this position if there was an item
                    if (!inSlot.isEmpty()) {
                        inSlot.shrink(1);
                        if (inSlot.getCount() <= 0) {
                            inSlot = ItemStack.EMPTY;
                        }
                    }

                    /// Apply remainder for this position (aligned index)
                    ItemStack rem = idx < remaining.size() ? remaining.get(idx) : ItemStack.EMPTY;
                    if (!rem.isEmpty()) {
                        if (inSlot.isEmpty()) {
                            itemHandler.setStackInSlot(slot, rem.copy());
                        } else if (ItemStack.isSameItemSameComponents(inSlot, rem)) {
                            int space = Math.min(rem.getCount(), inSlot.getMaxStackSize() - inSlot.getCount());
                            if (space > 0) {
                                inSlot.grow(space);
                                rem.shrink(space);
                            }
                            if (!rem.isEmpty()) {
                                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, rem.copy());
                            }
                            itemHandler.setStackInSlot(slot, inSlot);
                        } else {
                            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, rem.copy());
                            itemHandler.setStackInSlot(slot, inSlot);
                        }
                    } else {
                        itemHandler.setStackInSlot(slot, inSlot);
                    }
                    idx++;
                }
            }
        } finally {
            isUpdating = false;
        }

        setChanged();
        if (!level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        /// Recompute match after consumption
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
        return Component.translatable("block.simpleores.simple_crafting_table");
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
