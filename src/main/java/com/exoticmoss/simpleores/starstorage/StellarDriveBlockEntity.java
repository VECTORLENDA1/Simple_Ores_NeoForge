package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// Keeps the Star Cells put in a Stellar Drive (in the order they were put in).
public class StellarDriveBlockEntity extends BlockEntity {
    private final List<ItemStack> cells = new ArrayList<>();
    /// Client only: the kinds of the cells inside (CellTier ordinals), to draw them. The client never gets
    /// the items of the cells (they can be millions), only this.
    private int[] clientTiers = new int[0];

    public StellarDriveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STELLAR_DRIVE_BE.get(), pos, state);
    }

    /// The cells inside. The network changes them directly, and then calls cellsChanged().
    public List<ItemStack> getCells() {
        return cells;
    }

    /// Puts a cell in. false = the drive is full.
    public boolean addCell(ItemStack cell) {
        if (cells.size() >= StellarDriveBlock.MAX_CELLS) return false;
        cells.add(cell);
        cellsChanged();
        return true;
    }

    /// Takes out the last cell put in (EMPTY = no cells).
    public ItemStack removeLastCell() {
        if (cells.isEmpty()) return ItemStack.EMPTY;
        ItemStack cell = cells.remove(cells.size() - 1);
        cellsChanged();
        return cell;
    }

    /// Client: the kinds of the cells inside (CellTier ordinals), in order.
    public int[] getClientTiers() {
        return clientTiers;
    }

    /// Added to the CellTier ordinal of the Fluid Cells (in "tiers").
    public static final int FLUID = 100;

    /// The kinds of the cells inside (CellTier ordinals, + FLUID for the Fluid Cells), in order.
    private int[] tiers() {
        return cells.stream().mapToInt(cell -> cell.getItem() instanceof StarCellItem item ? item.tier.ordinal()
                : cell.getItem() instanceof FluidCellItem fluid ? FLUID + fluid.tier.ordinal() : 0).toArray();
    }

    /// Saves the changes. When cells are put in or taken out, the clients are told (to draw the stars).
    public void cellsChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.hasProperty(StellarDriveBlock.CELLS) && state.getValue(StellarDriveBlock.CELLS) != cells.size()) {
                level.setBlock(worldPosition, state.setValue(StellarDriveBlock.CELLS, cells.size()), 3);
            }
            if (!java.util.Arrays.equals(clientTiers, tiers())) {
                clientTiers = tiers();
                level.sendBlockUpdated(worldPosition, state, state, 3);
            }
        }
    }

    /// When the drive is broken, the cells drop on the ground (with all their items inside).
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            for (ItemStack cell : cells) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, cell);
            }
        }
        cells.clear();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("cells", ItemStack.CODEC.listOf(), cells);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // The server reads the cells; the client only gets the kinds of the cells (getUpdateTag)
        cells.clear();
        cells.addAll(input.read("cells", ItemStack.CODEC.listOf()).orElse(List.of()));
        clientTiers = input.getIntArray("tiers").orElseGet(this::tiers);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /// Sent to the clients: only the kinds of the cells (enough to draw them).
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("tiers", tiers());
        return tag;
    }
}
