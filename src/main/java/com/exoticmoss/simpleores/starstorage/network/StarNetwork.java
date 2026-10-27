package com.exoticmoss.simpleores.starstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import com.exoticmoss.simpleores.starstorage.terminal.AdvancedTerminalBlockEntity;
import com.exoticmoss.simpleores.starstorage.cell.CellContents;
import com.exoticmoss.simpleores.starstorage.cell.FluidCellItem;
import com.exoticmoss.simpleores.starstorage.cell.FluidContents;
import com.exoticmoss.simpleores.starstorage.magnetar.MagnetarBlockEntity;
import com.exoticmoss.simpleores.starstorage.quasar.Pattern;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarBlockEntity;
import com.exoticmoss.simpleores.starstorage.cell.StarCellItem;
import com.exoticmoss.simpleores.starstorage.drive.StellarDriveBlockEntity;
import com.exoticmoss.simpleores.starstorage.terminal.TerminalContents;

/// The Magnetar network: every Stellar Drive inside the magnetic field of a Magnetar Core.
/// There are no cables: the field is a cube of FIELD_RADIUS blocks in every direction around the Magnetar
/// (more with Magnetic Field Upgrades). Wormhole Links join the networks of other Magnetars.
/// The network is found again every time it is used, so placing or breaking a drive works right away.
/// "coreCrafting" = the network has a Stellar Terminal II: only then the Gravity Core recipes of the Quasars can be crafted.
public record StarNetwork(List<StellarDriveBlockEntity> drives, List<QuasarBlockEntity> quasars, boolean coreCrafting) {
    /// Size of the magnetic field of a Magnetar without upgrades (blocks in every direction).
    public static final int FIELD_RADIUS = 16;

    // ---------------------------------------------------------------- finding the network

    /// True if "pos" is inside the field of this Magnetar.
    public static boolean inField(MagnetarBlockEntity magnetar, BlockPos pos) {
        BlockPos magnetarPos = magnetar.getBlockPos();
        int radius = magnetar.radius();
        return Math.abs(pos.getX() - magnetarPos.getX()) <= radius && Math.abs(pos.getY() - magnetarPos.getY()) <= radius
                && Math.abs(pos.getZ() - magnetarPos.getZ()) <= radius;
    }

    /// The Magnetar whose field reaches this position (the nearest one). null = none.
    public static @Nullable MagnetarBlockEntity findMagnetar(Level level, BlockPos pos) {
        MagnetarBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (MagnetarBlockEntity magnetar : MagnetarBlockEntity.loaded()) {
            if (magnetar.getLevel() != level || magnetar.isRemoved() || !inField(magnetar, pos)) continue;
            double distance = magnetar.getBlockPos().distSqr(pos);
            if (distance < bestDistance) {
                best = magnetar;
                bestDistance = distance;
            }
        }
        return best;
    }

    /// The network of the Magnetar whose field reaches this position (empty if there is no Magnetar).
    public static StarNetwork at(Level level, BlockPos pos) {
        MagnetarBlockEntity magnetar = findMagnetar(level, pos);
        return magnetar == null ? new StarNetwork(List.of(), List.of(), false) : around(level, magnetar.getBlockPos());
    }

    /// Every drive and Quasar inside the field of the Magnetar at this position, and inside the fields of the
    /// Magnetars its Wormhole Links point to (and theirs...), when their chunks are loaded.
    public static StarNetwork around(Level level, BlockPos magnetarPos) {
        List<StellarDriveBlockEntity> drives = new ArrayList<>();
        List<QuasarBlockEntity> quasars = new ArrayList<>();
        boolean[] coreCrafting = {false};
        collect(level, magnetarPos, drives, quasars, coreCrafting, new HashSet<>());
        return new StarNetwork(drives, quasars, coreCrafting[0]);
    }

    private static void collect(Level level, BlockPos magnetarPos, List<StellarDriveBlockEntity> drives,
                                List<QuasarBlockEntity> quasars, boolean[] coreCrafting, Set<GlobalPos> visited) {
        if (!visited.add(GlobalPos.of(level.dimension(), magnetarPos))) return;
        MagnetarBlockEntity magnetar = level.getBlockEntity(magnetarPos) instanceof MagnetarBlockEntity found ? found : null;
        int radius = magnetar == null ? FIELD_RADIUS : magnetar.radius();
        forEachBlockEntity(level, magnetarPos, radius, blockEntity -> {
            if (blockEntity.isRemoved()) return;
            if (blockEntity instanceof StellarDriveBlockEntity drive && !drives.contains(drive)) drives.add(drive);
            else if (blockEntity instanceof QuasarBlockEntity quasar && !quasars.contains(quasar)) quasars.add(quasar);
            else if (blockEntity instanceof AdvancedTerminalBlockEntity) coreCrafting[0] = true;
        });
        if (magnetar == null || !(level instanceof ServerLevel serverLevel)) return;
        for (GlobalPos link : magnetar.links()) {
            ServerLevel other = serverLevel.getServer().getLevel(link.dimension());
            BlockPos otherPos = link.pos();
            if (other == null || other.getChunkSource().getChunkNow(otherPos.getX() >> 4, otherPos.getZ() >> 4) == null) continue;
            if (other.getBlockEntity(otherPos) instanceof MagnetarBlockEntity) collect(other, otherPos, drives, quasars, coreCrafting, visited);
        }
    }

    /// Every recipe of the Quasars of the network (an item can have more than one recipe).
    public List<Pattern> patterns() {
        List<Pattern> list = new ArrayList<>();
        for (QuasarBlockEntity quasar : quasars) {
            for (Pattern pattern : quasar.getPatterns()) {
                if (!list.contains(pattern)) list.add(pattern);
            }
        }
        return list;
    }

    /// Goes through the block entities of the loaded chunks inside the cube around "center".
    /// (Much faster than looking at every block of the cube.)
    public static void forEachBlockEntity(Level level, BlockPos center, int radius, Consumer<BlockEntity> action) {
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
        if (stack.isEmpty() || stack.getItem() instanceof StarCellItem && !StarCellItem.isEmptyCell(stack)
                || stack.getItem() instanceof FluidCellItem && !FluidCellItem.getContents(stack).isEmpty()) return stack;
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
        forEachCell((drive, cell, item) -> list.add(new TerminalContents.Cell(item.tier.ordinal(), StarCellItem.getContents(cell).totalCount(), false)));
        forEachFluidCell((drive, cell, item) -> list.add(new TerminalContents.Cell(item.tier.ordinal(), FluidCellItem.getContents(cell).total(), true)));
        return list;
    }

    private void forEachCell(CellAction action) {
        for (StellarDriveBlockEntity drive : drives) {
            for (ItemStack cell : drive.getCells()) {
                if (cell.getItem() instanceof StarCellItem item) action.accept(drive, cell, item);
            }
        }
    }

    // ---------------------------------------------------------------- liquids (Fluid Cells)

    private interface FluidCellAction {
        void accept(StellarDriveBlockEntity drive, ItemStack cell, FluidCellItem item);
    }

    private void forEachFluidCell(FluidCellAction action) {
        for (StellarDriveBlockEntity drive : drives) {
            for (ItemStack cell : drive.getCells()) {
                if (cell.getItem() instanceof FluidCellItem item) action.accept(drive, cell, item);
            }
        }
    }

    /// Every liquid of the network with its total amount (mB), most first.
    public List<FluidContents.Entry> fluids() {
        Map<Fluid, Long> totals = new LinkedHashMap<>();
        forEachFluidCell((drive, cell, item) -> {
            for (FluidContents.Entry entry : FluidCellItem.getContents(cell).entries()) totals.merge(entry.fluid(), entry.amount(), Long::sum);
        });
        List<FluidContents.Entry> list = new ArrayList<>();
        totals.forEach((fluid, amount) -> list.add(new FluidContents.Entry(fluid, amount)));
        list.sort((a, b) -> Long.compare(b.amount(), a.amount()));
        return list;
    }

    /// mB of liquids kept / that fit in all the Fluid Cells.
    public long fluidUsed() {
        long[] used = {0};
        forEachFluidCell((drive, cell, item) -> used[0] += FluidCellItem.getContents(cell).total());
        return used[0];
    }

    public long fluidCapacity() {
        long[] capacity = {0};
        forEachFluidCell((drive, cell, item) -> capacity[0] = Math.min(Long.MAX_VALUE / 2, capacity[0] + item.tier.fluidCapacity));
        return capacity[0];
    }

    /// How many mB of this liquid the network has.
    public long fluidAmount(Fluid fluid) {
        long[] amount = {0};
        forEachFluidCell((drive, cell, item) -> amount[0] += FluidCellItem.getContents(cell).amountOf(fluid));
        return amount[0];
    }

    /// Puts up to "amount" mB of this liquid in the network (first the cells that already have it).
    /// Returns how many mB went in.
    public long insertFluid(Fluid fluid, long amount) {
        long[] left = {amount};
        for (int pass = 0; pass < 2 && left[0] > 0; pass++) {
            final boolean onlyWithFluid = pass == 0;
            forEachFluidCell((drive, cell, item) -> {
                if (left[0] <= 0) return;
                if (onlyWithFluid && FluidCellItem.getContents(cell).amountOf(fluid) <= 0) return;
                long put = item.insert(cell, fluid, left[0]);
                if (put > 0) {
                    left[0] -= put;
                    drive.cellsChanged();
                }
            });
        }
        return amount - left[0];
    }

    /// Takes up to "amount" mB of this liquid out of the network. Returns how many mB were taken.
    public long extractFluid(Fluid fluid, long amount) {
        long[] taken = {0};
        forEachFluidCell((drive, cell, item) -> {
            if (taken[0] >= amount) return;
            long got = item.extract(cell, fluid, amount - taken[0]);
            if (got > 0) {
                taken[0] += got;
                drive.cellsChanged();
            }
        });
        return taken[0];
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
