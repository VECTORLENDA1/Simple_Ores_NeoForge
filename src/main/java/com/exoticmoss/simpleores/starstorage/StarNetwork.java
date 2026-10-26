package com.exoticmoss.simpleores.starstorage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/// The Magnetar network: every Stellar Drive inside the magnetic field of a Magnetar Core.
/// There are no cables: the field is a cube of FIELD_RADIUS blocks in every direction around the Magnetar.
/// The network is found again every time it is used, so placing or breaking a drive works right away.
public record StarNetwork(List<StellarDriveBlockEntity> drives) {
    /// Size of the magnetic field of a Magnetar (blocks in every direction).
    public static final int FIELD_RADIUS = 16;

    // ---------------------------------------------------------------- finding the network

    /// The Magnetar whose field reaches this position (the nearest one). null = none.
    public static @Nullable MagnetarBlockEntity findMagnetar(Level level, BlockPos pos) {
        MagnetarBlockEntity[] best = {null};
        double[] bestDistance = {Double.MAX_VALUE};
        forEachBlockEntity(level, pos, FIELD_RADIUS, blockEntity -> {
            if (blockEntity instanceof MagnetarBlockEntity magnetar && !magnetar.isRemoved()) {
                double distance = magnetar.getBlockPos().distSqr(pos);
                if (distance < bestDistance[0]) {
                    best[0] = magnetar;
                    bestDistance[0] = distance;
                }
            }
        });
        return best[0];
    }

    /// The network of the Magnetar whose field reaches this position (empty if there is no Magnetar).
    public static StarNetwork at(Level level, BlockPos pos) {
        MagnetarBlockEntity magnetar = findMagnetar(level, pos);
        return magnetar == null ? new StarNetwork(List.of()) : around(level, magnetar.getBlockPos());
    }

    /// Every drive inside the field of the Magnetar at this position.
    public static StarNetwork around(Level level, BlockPos magnetarPos) {
        List<StellarDriveBlockEntity> drives = new ArrayList<>();
        forEachBlockEntity(level, magnetarPos, FIELD_RADIUS, blockEntity -> {
            if (blockEntity instanceof StellarDriveBlockEntity drive && !drive.isRemoved()) drives.add(drive);
        });
        return new StarNetwork(drives);
    }

    /// Goes through the block entities of the loaded chunks inside the cube around "center".
    /// (Much faster than looking at every block of the cube.)
    private static void forEachBlockEntity(Level level, BlockPos center, int radius, Consumer<BlockEntity> action) {
        int minX = center.getX() - radius, maxX = center.getX() + radius;
        int minY = center.getY() - radius, maxY = center.getY() + radius;
        int minZ = center.getZ() - radius, maxZ = center.getZ() + radius;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : List.copyOf(chunk.getBlockEntities().values())) {
                    BlockPos p = blockEntity.getBlockPos();
                    if (p.getX() >= minX && p.getX() <= maxX && p.getY() >= minY && p.getY() <= maxY
                            && p.getZ() >= minZ && p.getZ() <= maxZ) {
                        action.accept(blockEntity);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- what is inside

    /// Every kind of item in the network with its total amount (all cells together), most first.
    public List<Map.Entry<ItemStackTemplate, Long>> contents() {
        Map<ItemStackTemplate, Long> totals = new LinkedHashMap<>();
        forEachCell((drive, cell, item) -> {
            for (CellContents.Entry entry : StarCellItem.getContents(cell).entries()) {
                totals.merge(entry.item(), (long) entry.count(), Long::sum);
            }
        });
        List<Map.Entry<ItemStackTemplate, Long>> list = new ArrayList<>(totals.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return list;
    }

    public int cellCount() {
        int[] count = {0};
        forEachCell((drive, cell, item) -> count[0]++);
        return count[0];
    }

    /// Items kept in all the cells.
    public long usedItems() {
        long[] used = {0};
        forEachCell((drive, cell, item) -> used[0] += StarCellItem.getContents(cell).totalCount());
        return used[0];
    }

    /// How many items fit in all the cells.
    public long capacity() {
        long[] capacity = {0};
        forEachCell((drive, cell, item) -> capacity[0] += item.tier.capacity);
        return capacity[0];
    }

    // ---------------------------------------------------------------- putting in and taking out

    /// Puts the stack in the network. Returns what did not fit (EMPTY = everything fit).
    /// First the cells that already have this item (so the same item stays together), then any cell with room.
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof StarCellItem && !StarCellItem.isEmptyCell(stack)) return stack;
        ItemStack left = stack.copy();
        for (int pass = 0; pass < 2 && !left.isEmpty(); pass++) {
            final boolean onlyWithItem = pass == 0;
            forEachCell((drive, cell, item) -> {
                if (left.isEmpty()) return;
                if (onlyWithItem && StarCellItem.getContents(cell).indexOf(left) < 0) return;
                int amount = Math.min(left.getCount(), item.space(cell));
                if (amount <= 0) return;
                item.insert(cell, left, amount);
                left.shrink(amount);
                drive.cellsChanged();
            });
        }
        return left;
    }

    /// Takes up to "amount" of this item out of the network. Returns what was taken (EMPTY = nothing).
    public ItemStack extract(ItemStack template, int amount) {
        int[] taken = {0};
        forEachCell((drive, cell, item) -> {
            if (taken[0] >= amount) return;
            int got = item.extract(cell, template, amount - taken[0]);
            if (got > 0) {
                taken[0] += got;
                drive.cellsChanged();
            }
        });
        return taken[0] == 0 ? ItemStack.EMPTY : template.copyWithCount(taken[0]);
    }

    private interface CellAction {
        void accept(StellarDriveBlockEntity drive, ItemStack cell, StarCellItem item);
    }

    /// Every cell of the network: its kind and how many items it has (for the bars of the terminal).
    public java.util.List<TerminalContents.Cell> cells() {
        java.util.List<TerminalContents.Cell> list = new ArrayList<>();
        forEachCell((drive, cell, item) -> list.add(new TerminalContents.Cell(item.tier.ordinal(), StarCellItem.getContents(cell).totalCount())));
        return list;
    }

    private void forEachCell(CellAction action) {
        for (StellarDriveBlockEntity drive : drives) {
            for (ItemStack cell : drive.getCells()) {
                if (cell.getItem() instanceof StarCellItem item) action.accept(drive, cell, item);
            }
        }
    }

    // ---------------------------------------------------------------- text

    /// "950", "14.4k", "2.1M", "3.2B".
    public static String format(long count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1_000_000) return String.format(Locale.ROOT, "%.1fk", count / 1000.0);
        if (count < 1_000_000_000) return String.format(Locale.ROOT, "%.1fM", count / 1_000_000.0);
        return String.format(Locale.ROOT, "%.1fB", count / 1_000_000_000.0);
    }
}
