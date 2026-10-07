package com.vector.simpleores.block.entity.custom;

import com.vector.simpleores.block.entity.CraftingTableItemHandler;
import com.vector.simpleores.block.entity.ModBlockEntities;
import com.vector.simpleores.recipe.ModRecipes;
import com.vector.simpleores.recipe.UltraCraftingTableRecipe;
import com.vector.simpleores.recipe.UltraCraftingTableRecipeInput;
import com.vector.simpleores.screen.custom.UltraCraftingTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.Optional;

public class UltraCraftingTableEntity extends BlockEntity implements MenuProvider {
    // Cache for last matched vanilla crafting bounding box in 7x7 and recipe
    private int lastVanillaMinX = -1;
    private int lastVanillaMinY = -1;
    private int lastVanillaWidth = 0;
    private int lastVanillaHeight = 0;
    @Nullable
    private RecipeHolder<CraftingRecipe> lastVanillaRecipe = null;
    private ItemStack lastVanillaResult = ItemStack.EMPTY;
    public boolean isUpdating = false;
    public final CraftingTableItemHandler itemHandler = new CraftingTableItemHandler(50) {
        @Override
        protected void onContentsChanged(int slot, ItemStack previousContents) {
            if (slot == OUTPUT_SLOT || isUpdating) return;
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                updateResult(0);
            }
        }
    };

    public static final int[] INPUT_SLOT = new int[49];
    public static final int OUTPUT_SLOT = 49;

    public UltraCraftingTableEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.ULTRA_CRAFTING_TABLE_BE.get(), pPos, pBlockState);
        for (int i = 0; i < 49; i++) {
            INPUT_SLOT[i] = i;
        }
    }

    private void clearVanillaCache() {
        lastVanillaRecipe = null;
        lastVanillaResult = ItemStack.EMPTY;
        lastVanillaMinX = -1;
        lastVanillaMinY = -1;
        lastVanillaWidth = 0;
        lastVanillaHeight = 0;
    }

    public void updateResult(int p) {
        // First try custom 7x7 recipe
        Optional<RecipeHolder<UltraCraftingTableRecipe>> opt = getCurrentRecipe();
        if (opt.isPresent()) {
            clearVanillaCache();

            UltraCraftingTableRecipe recipe = opt.get().value();
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
                ItemStack result = recipe.getResultItem();
                if (result.getMaxStackSize() == 1) {
                    result.setCount(1);
                } else {
                    result.setCount(result.getCount() * maxCrafts);
                }
                itemHandler.setStackInSlot(OUTPUT_SLOT, result);
            }
            return;
        }

        // Then try vanilla 3x3 recipes anywhere in the 7x7 grid
        Optional<RecipeHolder<CraftingRecipe>> vanillaOpt = findVanillaRecipe();
        if (vanillaOpt.isPresent()) {
            // For vanilla we output a single craft result at a time (safer for container items)
            itemHandler.setStackInSlot(OUTPUT_SLOT, lastVanillaResult.copy());
        } else {
            itemHandler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
        }
    }

    public void consumeIngredients(int times) {
        Optional<RecipeHolder<UltraCraftingTableRecipe>> opt = getCurrentRecipe();
        if (opt.isEmpty()) return;
        UltraCraftingTableRecipe recipe = opt.get().value();

        isUpdating = true;
        try {
            for (int i = 0; i < INPUT_SLOT.length; i++) {
                int req = recipe.getRequiredCountForSlot(i);
                if (req > 0) {
                    itemHandler.extractItem(i, req * times);
                }
            }
        } finally {
            isUpdating = false;
        }

        setChanged();
        if (!level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        updateResult(0);
    }

    private Optional<RecipeHolder<CraftingRecipe>> findVanillaRecipe() {
        clearVanillaCache();
        // O RecipeManager so existe no servidor (desde a 1.21.2 o cliente nao o tem).
        if (!(level instanceof ServerLevel serverLevel)) return Optional.empty();

        // Compute tight bounding box of non-empty inputs in the 7x7 grid
        int minX = 7, minY = 7, maxX = -1, maxY = -1;
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 7; x++) {
                ItemStack st = itemHandler.getStackInSlot(y * 7 + x);
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
        // Vanilla crafting grid is max 3x3; if the used area exceeds this, no vanilla recipe
        if (width > 3 || height > 3) {
            return Optional.empty();
        }

        CraftingInput input = buildInputForBox(minX, minY, width, height);
        Optional<RecipeHolder<CraftingRecipe>> opt = serverLevel.recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, serverLevel);
        if (opt.isPresent()) {
            lastVanillaRecipe = opt.get();
            // getResultItem(RegistryAccess) foi removido; o resultado obtem-se com assemble(input)
            lastVanillaResult = opt.get().value().assemble(input);
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
                int slot = (minY + r) * 7 + (minX + c);
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
        return lastVanillaResult.copy();
    }

    public void consumeVanillaOnce() {
        if (lastVanillaRecipe == null || level == null) return;
        CraftingRecipe recipe = lastVanillaRecipe.value();
        // Build input for the cached bounding box
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
                    int slot = (minY + r) * 7 + (minX + c);
                    ItemStack inSlot = itemHandler.getStackInSlot(slot);

                    // Consume one from this position if there was an item
                    if (!inSlot.isEmpty()) {
                        inSlot.shrink(1);
                        if (inSlot.getCount() <= 0) {
                            inSlot = ItemStack.EMPTY;
                        }
                    }

                    // Apply remainder for this position (aligned index)
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
        if (!level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        // Recompute match after consumption
        updateResult(0);
    }

    // saveAdditional/loadAdditional usam agora ValueOutput/ValueInput (em vez de CompoundTag + Provider)
    // e o inventario serializa-se com serialize()/deserialize().
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        itemHandler.serialize(output.child("inventory"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("inventory").ifPresent(itemHandler::deserialize);
    }

    // Substitui o antigo onRemove() do bloco: larga o conteudo quando o bloco e removido.
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        drops();
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (level.isClientSide()) return;

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

    public Optional<RecipeHolder<UltraCraftingTableRecipe>> getCurrentRecipe() {
        // Receitas so existem no servidor (ServerLevel#recipeAccess)
        if (!(level instanceof ServerLevel serverLevel)) return Optional.empty();
        return serverLevel.recipeAccess()
                .getRecipeFor(ModRecipes.ULTRA_CRAFTING_TABLE_TYPE.get(),
                        new UltraCraftingTableRecipeInput(itemHandler), serverLevel);
    }

    public void drops() {
        if (!(level instanceof ServerLevel)) return;
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (i != OUTPUT_SLOT) {
                inventory.setItem(i, itemHandler.getStackInSlot(i));
            }
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simpleores.ultra_crafting_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new UltraCraftingTableMenu(id, playerInv, this, null);
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