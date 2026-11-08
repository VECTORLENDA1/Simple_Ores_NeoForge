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
import com.exoticmoss.simpleores.starstorage.gas.Gas;
import com.exoticmoss.simpleores.starstorage.gas.Gases;
import net.minecraft.resources.Identifier;
import com.exoticmoss.simpleores.starstorage.horizon.EventHorizonBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

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
/// "horizons" = the Event Horizons of the field: the inventories and tanks they touch are part of the network too
/// (see External).
public record StarNetwork(List<StellarDriveBlockEntity> drives, List<QuasarBlockEntity> quasars, boolean coreCrafting,
                          List<EventHorizonBlockEntity> horizons) {
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
        return magnetar == null ? new StarNetwork(List.of(), List.of(), false, List.of()) : around(level, magnetar.getBlockPos());
    }

    /// Every drive and Quasar inside the field of the Magnetar at this position, and inside the fields of the
    /// Magnetars its Wormhole Links point to (and theirs...), when their chunks are loaded.
    public static StarNetwork around(Level level, BlockPos magnetarPos) {
        List<StellarDriveBlockEntity> drives = new ArrayList<>();
        List<QuasarBlockEntity> quasars = new ArrayList<>();
        List<EventHorizonBlockEntity> horizons = new ArrayList<>();
        boolean[] coreCrafting = {false};
        collect(level, magnetarPos, drives, quasars, horizons, coreCrafting, new HashSet<>());
        return new StarNetwork(drives, quasars, coreCrafting[0], horizons);
    }

    private static void collect(Level level, BlockPos magnetarPos, List<StellarDriveBlockEntity> drives,
                                List<QuasarBlockEntity> quasars, List<EventHorizonBlockEntity> horizons, boolean[] coreCrafting,
                                Set<GlobalPos> visited) {
        if (!visited.add(GlobalPos.of(level.dimension(), magnetarPos))) return;
        MagnetarBlockEntity magnetar = level.getBlockEntity(magnetarPos) instanceof MagnetarBlockEntity found ? found : null;
        int radius = magnetar == null ? FIELD_RADIUS : magnetar.radius();
        forEachBlockEntity(level, magnetarPos, radius, blockEntity -> {
            if (blockEntity.isRemoved()) return;
            if (blockEntity instanceof StellarDriveBlockEntity drive && !drives.contains(drive)) drives.add(drive);
            else if (blockEntity instanceof QuasarBlockEntity quasar && !quasars.contains(quasar)) quasars.add(quasar);
            else if (blockEntity instanceof EventHorizonBlockEntity horizon && !horizons.contains(horizon)) horizons.add(horizon);
            else if (blockEntity instanceof AdvancedTerminalBlockEntity) coreCrafting[0] = true;
        });
        if (magnetar == null || !(level instanceof ServerLevel serverLevel)) return;
        for (GlobalPos link : magnetar.links()) {
            ServerLevel other = serverLevel.getServer().getLevel(link.dimension());
            BlockPos otherPos = link.pos();
            if (other == null || other.getChunkSource().getChunkNow(otherPos.getX() >> 4, otherPos.getZ() >> 4) == null) continue;
            if (other.getBlockEntity(otherPos) instanceof MagnetarBlockEntity) collect(other, otherPos, drives, quasars, horizons, coreCrafting, visited);
        }
    }

    /// True if the network keeps things: Stellar Drives, or Event Horizons.
    public boolean hasStorage() {
        return !drives.isEmpty() || !horizons.isEmpty();
    }

    // ---------------------------------------------------------------- inventories of other mods (Event Horizons)

    /// An inventory and its tanks touched by an Event Horizon of the network (null = it has none of them).
    private record External(EventHorizonBlockEntity horizon, @Nullable ItemSlots items, @Nullable FluidTanks fluids,
                            @Nullable Gases.Tanks gases) {
    }

    /// The inventories and tanks the Event Horizons touch (each one once; never a block of the star storage), the
    /// highest priority first.
    private List<External> externals() {
        if (horizons.isEmpty()) return List.of();
        List<External> list = new ArrayList<>();
        Set<GlobalPos> seen = new HashSet<>();
        for (EventHorizonBlockEntity horizon : horizons) {
            Level level = horizon.getLevel();
            if (level == null || horizon.isRemoved()) continue;
            for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values()) {
                BlockPos at = horizon.getBlockPos().relative(side);
                if (!level.isLoaded(at)) continue;
                BlockEntity target = level.getBlockEntity(at);
                if (target == null || target.getClass().getName().startsWith("com.exoticmoss.simpleores.")) continue;
                if (!seen.add(GlobalPos.of(level.dimension(), at))) continue;
                ItemSlots items = ItemSlots.of(level.getCapability(Capabilities.Item.BLOCK, at, side.getOpposite()));
                if (items == null) items = ItemSlots.of(level.getCapability(Capabilities.Item.BLOCK, at, null));
                FluidTanks fluids = FluidTanks.of(level.getCapability(Capabilities.Fluid.BLOCK, at, side.getOpposite()));
                if (fluids == null) fluids = FluidTanks.of(level.getCapability(Capabilities.Fluid.BLOCK, at, null));
                Gases.Tanks gases = Gases.tanks(level, at, side.getOpposite());
                if (gases == null) gases = Gases.tanks(level, at, null);
                if (items != null || fluids != null || gases != null) list.add(new External(horizon, items, fluids, gases));
            }
        }
        list.sort((a, b) -> Integer.compare(b.horizon().priority(), a.horizon().priority()));
        return list;
    }

    /// Puts the stack in an inventory of an Event Horizon (the slots with the same item first). Returns what didn't fit.
    private static ItemStack insertExternal(External external, ItemStack stack) {
        if (external.items() == null || !external.horizon().accepts(stack)) return stack;
        ItemSlots handler = external.items();
        ItemStack left = stack;
        for (int slot = 0; slot < handler.getSlots() && !left.isEmpty(); slot++) {
            if (ItemStack.isSameItemSameComponents(handler.getStackInSlot(slot), left)) left = handler.insertItem(slot, left, false);
        }
        for (int slot = 0; slot < handler.getSlots() && !left.isEmpty(); slot++) {
            if (handler.getStackInSlot(slot).isEmpty()) left = handler.insertItem(slot, left, false);
        }
        return left;
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
                totals.merge(entry.item(), (long) entry.count(), StarNetwork::add);
            }
        });
        for (External external : externals()) {
            if (external.items() == null) continue;
            for (int slot = 0; slot < external.items().getSlots(); slot++) {
                ItemStack stack = external.items().getStackInSlot(slot);
                if (stack.isEmpty() || !external.horizon().accepts(stack)) continue;
                totals.merge(ItemStackTemplate.fromNonEmptyStack(stack.copyWithCount(1)), (long) stack.getCount(), StarNetwork::add);
            }
        }
        List<Map.Entry<ItemStackTemplate, Long>> list = new ArrayList<>(totals.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return list;
    }

    public int cellCount() {
        int[] count = {0};
        forEachCell((drive, cell, item) -> count[0]++);
        return count[0];
    }

    /// Items kept in all the cells (and in the inventories of the Event Horizons).
    public long usedItems() {
        long[] used = {0};
        forEachCell((drive, cell, item) -> used[0] = add(used[0], StarCellItem.getContents(cell).totalCount()));
        for (External external : externals()) {
            if (external.items() == null) continue;
            for (int slot = 0; slot < external.items().getSlots(); slot++) used[0] = add(used[0], external.items().getStackInSlot(slot).getCount());
        }
        return used[0];
    }

    /// How many items fit in all the cells (and in the inventories of the Event Horizons).
    public long capacity() {
        long[] capacity = {0};
        forEachCell((drive, cell, item) -> capacity[0] = add(capacity[0], item.tier.capacity));
        for (External external : externals()) {
            if (external.items() == null) continue;
            for (int slot = 0; slot < external.items().getSlots(); slot++) capacity[0] = add(capacity[0], external.items().getSlotLimit(slot));
        }
        return capacity[0];
    }

    // ---------------------------------------------------------------- putting in and taking out

    /// Puts the stack in the network. Returns what did not fit (EMPTY = everything fit).
    /// First the inventories of the Event Horizons with a priority above 0, then the cells that already have this item
    /// (so the same item stays together), then any cell with room, then the other inventories of the Event Horizons.
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof StarCellItem && !StarCellItem.isEmptyCell(stack)
                || stack.getItem() instanceof FluidCellItem && !FluidCellItem.getContents(stack).isEmpty()) return stack;
        List<External> externals = externals();
        ItemStack first = stack.copy();
        for (External external : externals) {
            if (first.isEmpty()) return ItemStack.EMPTY;
            if (external.horizon().priority() > 0) first = insertExternal(external, first);
        }
        ItemStack left = insertCells(first);
        for (External external : externals) {
            if (left.isEmpty()) break;
            if (external.horizon().priority() <= 0) left = insertExternal(external, left);
        }
        return left;
    }

    private ItemStack insertCells(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
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
        // Then the inventories of the Event Horizons (the lowest priority first)
        List<External> externals = externals();
        for (int i = externals.size() - 1; i >= 0 && taken[0] < amount; i--) {
            External external = externals.get(i);
            if (external.items() == null || !external.horizon().accepts(template)) continue;
            ItemSlots handler = external.items();
            for (int slot = 0; slot < handler.getSlots() && taken[0] < amount; slot++) {
                if (!ItemStack.isSameItemSameComponents(handler.getStackInSlot(slot), template)) continue;
                while (taken[0] < amount) {
                    ItemStack out = handler.extractItem(slot, Math.min(amount - taken[0], template.getMaxStackSize()), false);
                    if (out.isEmpty()) break;
                    taken[0] += out.getCount();
                }
            }
        }
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
            for (FluidContents.Entry entry : FluidCellItem.getContents(cell).entries()) totals.merge(entry.fluid(), entry.amount(), StarNetwork::add);
        });
        for (External external : externals()) {
            if (external.fluids() == null) continue;
            for (int tank = 0; tank < external.fluids().getTanks(); tank++) {
                FluidStack fluid = external.fluids().getFluidInTank(tank);
                if (!fluid.isEmpty() && external.horizon().acceptsFluid(fluid.getFluid())) totals.merge(fluid.getFluid(), (long) fluid.getAmount(), StarNetwork::add);
            }
        }
        List<FluidContents.Entry> list = new ArrayList<>();
        totals.forEach((fluid, amount) -> list.add(new FluidContents.Entry(fluid, amount)));
        list.sort((a, b) -> Long.compare(b.amount(), a.amount()));
        return list;
    }

    /// mB of liquids kept / that fit in all the Fluid Cells.
    public long fluidUsed() {
        long[] used = {0};
        forEachFluidCell((drive, cell, item) -> used[0] = add(used[0], FluidCellItem.getContents(cell).total()));
        for (External external : externals()) {
            if (external.fluids() == null) continue;
            for (int tank = 0; tank < external.fluids().getTanks(); tank++) used[0] = add(used[0], external.fluids().getFluidInTank(tank).getAmount());
        }
        return used[0];
    }

    public long fluidCapacity() {
        long[] capacity = {0};
        forEachFluidCell((drive, cell, item) -> capacity[0] = Math.min(Long.MAX_VALUE / 2, capacity[0] + item.tier.fluidCapacity));
        for (External external : externals()) {
            if (external.fluids() == null) continue;
            for (int tank = 0; tank < external.fluids().getTanks(); tank++) {
                capacity[0] = Math.min(Long.MAX_VALUE / 2, capacity[0] + external.fluids().getTankCapacity(tank));
            }
        }
        return capacity[0];
    }

    /// How many mB of this liquid the network has.
    public long fluidAmount(Fluid fluid) {
        long[] amount = {0};
        forEachFluidCell((drive, cell, item) -> amount[0] = add(amount[0], FluidCellItem.getContents(cell).amountOf(fluid)));
        for (External external : externals()) {
            if (external.fluids() == null) continue;
            for (int tank = 0; tank < external.fluids().getTanks(); tank++) {
                FluidStack inside = external.fluids().getFluidInTank(tank);
                if (inside.getFluid() == fluid && external.horizon().acceptsFluid(fluid)) amount[0] = add(amount[0], inside.getAmount());
            }
        }
        return amount[0];
    }

    /// Puts up to "amount" mB of this liquid in the network (first the cells that already have it).
    /// Returns how many mB went in.
    public long insertFluid(Fluid fluid, long amount) {
        List<External> externals = externals();
        long[] left = {amount};
        // The tanks of the Event Horizons with a priority above 0 first, then the cells, then the other tanks
        for (External external : externals) {
            if (external.horizon().priority() > 0) left[0] -= fillExternal(external, fluid, left[0]);
        }
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
        for (External external : externals) {
            if (external.horizon().priority() <= 0) left[0] -= fillExternal(external, fluid, left[0]);
        }
        return amount - left[0];
    }

    private static long fillExternal(External external, Fluid fluid, long amount) {
        if (external.fluids() == null || amount <= 0 || !external.horizon().acceptsFluid(fluid)) return 0;
        return external.fluids().fill(new FluidStack(fluid, (int) Math.min(amount, Integer.MAX_VALUE)), FluidTanks.FluidAction.EXECUTE);
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
        List<External> externals = externals();
        for (int i = externals.size() - 1; i >= 0 && taken[0] < amount; i--) {
            FluidTanks tanks = externals.get(i).fluids();
            if (tanks == null || !externals.get(i).horizon().acceptsFluid(fluid)) continue;
            int want = (int) Math.min(amount - taken[0], Integer.MAX_VALUE);
            taken[0] += tanks.drain(new FluidStack(fluid, want), FluidTanks.FluidAction.EXECUTE).getAmount();
        }
        return taken[0];
    }

    // ---------------------------------------------------------------- gases of Mekanism (Fluid Cells, see Gases)

    /// Every gas of the network with its total amount (mB), most first.
    public List<Gas> gases() {
        Map<Identifier, Long> totals = new LinkedHashMap<>();
        forEachFluidCell((drive, cell, item) -> {
            for (Gas gas : FluidCellItem.getContents(cell).gases()) totals.merge(gas.id(), gas.amount(), StarNetwork::add);
        });
        for (External external : externals()) {
            if (external.gases() == null) continue;
            for (int tank = 0; tank < external.gases().count(); tank++) {
                Identifier gas = external.gases().gas(tank);
                if (gas != null) totals.merge(gas, external.gases().amount(tank), StarNetwork::add);
            }
        }
        List<Gas> list = new ArrayList<>();
        totals.forEach((gas, amount) -> list.add(new Gas(gas, amount)));
        list.sort((a, b) -> Long.compare(b.amount(), a.amount()));
        return list;
    }

    /// How many mB of this gas the network has.
    public long gasAmount(Identifier gas) {
        long[] amount = {0};
        forEachFluidCell((drive, cell, item) -> amount[0] = add(amount[0], FluidCellItem.getContents(cell).amountOfGas(gas)));
        for (External external : externals()) {
            if (external.gases() == null) continue;
            for (int tank = 0; tank < external.gases().count(); tank++) {
                if (gas.equals(external.gases().gas(tank))) amount[0] = add(amount[0], external.gases().amount(tank));
            }
        }
        return amount[0];
    }

    /// Puts up to "amount" mB of this gas in the network (like the liquids: the tanks of the Event Horizons with a
    /// priority above 0, the cells that have it, any cell, the other tanks). Returns how many mB went in.
    public long insertGas(Identifier gas, long amount) {
        List<External> externals = externals();
        long[] left = {amount};
        for (External external : externals) {
            if (external.horizon().priority() > 0 && external.gases() != null) left[0] -= external.gases().fill(gas, left[0], false);
        }
        for (int pass = 0; pass < 2 && left[0] > 0; pass++) {
            final boolean onlyWithGas = pass == 0;
            forEachFluidCell((drive, cell, item) -> {
                if (left[0] <= 0) return;
                if (onlyWithGas && FluidCellItem.getContents(cell).amountOfGas(gas) <= 0) return;
                long put = item.insertGas(cell, gas, left[0]);
                if (put > 0) {
                    left[0] -= put;
                    drive.cellsChanged();
                }
            });
        }
        for (External external : externals) {
            if (left[0] <= 0) break;
            if (external.horizon().priority() <= 0 && external.gases() != null) left[0] -= external.gases().fill(gas, left[0], false);
        }
        return amount - left[0];
    }

    /// Takes up to "amount" mB of this gas out of the network. Returns how many mB were taken.
    public long extractGas(Identifier gas, long amount) {
        long[] taken = {0};
        forEachFluidCell((drive, cell, item) -> {
            if (taken[0] >= amount) return;
            long got = item.extractGas(cell, gas, amount - taken[0]);
            if (got > 0) {
                taken[0] += got;
                drive.cellsChanged();
            }
        });
        List<External> externals = externals();
        for (int i = externals.size() - 1; i >= 0 && taken[0] < amount; i--) {
            Gases.Tanks tanks = externals.get(i).gases();
            if (tanks != null) taken[0] += tanks.drain(gas, amount - taken[0], false);
        }
        return taken[0];
    }

    // ---------------------------------------------------------------- text

    /// Thousand (k), million (M), billion (B), trillion (T), quadrillion (Q), quintillion (E).
    private static final String[] UNITS = {"k", "M", "B", "T", "Q", "E"};

    /// A short amount that always fits in a slot: "950", "14.4k", "144k", "2.1M", "3.2B", "9.2E".
    public static String format(long count) {
        if (count < 1000) return String.valueOf(Math.max(0, count));
        double value = count;
        int unit = -1;
        while (value >= 999.95 && unit < UNITS.length - 1) {
            value /= 1000;
            unit++;
        }
        return String.format(Locale.ROOT, value < 99.95 ? "%.1f%s" : "%.0f%s", value, UNITS[unit]);
    }

    /// a + b, but never more than the biggest number (a creative tank has "infinite" amounts: added to other amounts
    /// they would go round to negative numbers).
    public static long add(long a, long b) {
        long sum = a + b;
        return ((a ^ sum) & (b ^ sum)) < 0 ? (a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE) : sum;
    }
}
