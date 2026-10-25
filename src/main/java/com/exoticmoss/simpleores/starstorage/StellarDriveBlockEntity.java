package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/// Keeps the Star Cells put in a Stellar Drive (in the order they were put in).
public class StellarDriveBlockEntity extends BlockEntity {
    private final List<ItemStack> cells = new ArrayList<>();

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

    /// Saves the changes and shows on the front how many cells are inside.
    public void cellsChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.hasProperty(StellarDriveBlock.CELLS) && state.getValue(StellarDriveBlock.CELLS) != cells.size()) {
                level.setBlock(worldPosition, state.setValue(StellarDriveBlock.CELLS, cells.size()), 3);
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
        cells.clear();
        cells.addAll(input.read("cells", ItemStack.CODEC.listOf()).orElse(List.of()));
    }
}
