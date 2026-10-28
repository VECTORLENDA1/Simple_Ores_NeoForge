package com.exoticmoss.simpleores.starstorage.binary;

import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.exoticmoss.simpleores.util.ValueInput;
import com.exoticmoss.simpleores.util.ValueOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.exoticmoss.simpleores.starstorage.network.NetworkHandler;
import com.exoticmoss.simpleores.starstorage.network.StarAccess;
import com.exoticmoss.simpleores.starstorage.network.StarFluids;
import com.exoticmoss.simpleores.starstorage.network.StarNetwork;

/// The Binary Star block entity: the rules of the slots of the blocks around it (RADIUS blocks, see SlotRule),
/// set in the Binary Star screen. It uses the network of the Magnetar whose field it is in.
/// Every WORK_TICKS ticks it does the work:
///  - TAKE slots: their items go to the network;
///  - KEEP slots: they are filled from the network up to a full stack of their item;
///  - BOTH: anything that is not the item of the slot goes to the network, and the slot is kept full of its item.
/// Works with every block that has an inventory (chests, barrels, furnaces, hoppers, brewing stands, the machines of
/// other mods...) and with tanks of liquids (TAKE: the liquid goes to the Fluid Cells; KEEP: the tank is kept full of
/// the liquid). It uses the transfer API of NeoForge (see StarAccess).
/// It is also the door of the network for other mods: their pipes can put in and take out items and liquids.
/// The output slots of the machines around are set to TAKE by themselves (see scanOutputs).
public class BinaryStarBlockEntity extends BlockEntity {
    /// Every how many ticks the Binary Star moves the items (10 ticks = half a second).
    public static final int WORK_TICKS = 10;
    /// The rules of the tanks (liquids) use the slot numbers TANKS + tank (their item is a bucket of the liquid).
    public static final int TANKS = 10_000;
    /// How far (blocks in every direction) the blocks it takes care of can be, without upgrades;
    /// each Magnetic Field Upgrade (up to MAX_UPGRADES, in the slot of its screen) adds RANGE_PER_UPGRADE.
    public static final int RADIUS = 8;
    public static final int RANGE_PER_UPGRADE = 8;
    public static final int MAX_UPGRADES = 4;

    /// The rules of each block (position -> slot -> rule).
    private final Map<BlockPos, Map<Integer, SlotRule>> rules = new HashMap<>();
    /// The recipes of the Quasars (machine recipes) each machine makes (position -> recipes). A recipe linked to
    /// machines is only made in those machines, and a machine with recipes only makes those (see Autocraft).
    private final Map<BlockPos, List<com.exoticmoss.simpleores.starstorage.quasar.Pattern>> links = new HashMap<>();
    /// The order of the blocks on the screen: in the order they were found (a new block goes to the end).
    private final List<BlockPos> order = new ArrayList<>();
    /// Every how many ticks the blocks around are looked at for new machines (5 seconds).
    public static final int SCAN_TICKS = 100;
    /// The blocks whose output slots were already set to TAKE by themselves (see scanOutputs): a rule the player
    /// removes later is not put back.
    private final java.util.Set<BlockPos> configured = new java.util.HashSet<>();
    private int ticks;
    /// The slot of the Magnetic Field Upgrades.
    private final SimpleContainer upgrades = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            BinaryStarBlockEntity.this.setChanged();
        }
    };

    public BinaryStarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BINARY_STAR_BE.get(), pos, state);
    }

    // ---------------------------------------------------------------- the rules

    /// The rules of the block at "pos", by slot (empty if none).
    public Map<Integer, SlotRule> rulesOf(BlockPos pos) {
        return rules.getOrDefault(pos, Map.of());
    }

    /// Sets (or, with null, removes) the rule of one slot.
    public void setRule(BlockPos pos, int slot, SlotRule rule) {
        Map<Integer, SlotRule> slots = rules.computeIfAbsent(pos.immutable(), p -> new TreeMap<>());
        if (rule == null) slots.remove(slot);
        else slots.put(slot, rule);
        if (slots.isEmpty()) rules.remove(pos);
        setChanged();
    }

    /// The recipes linked to each machine.
    public Map<BlockPos, List<com.exoticmoss.simpleores.starstorage.quasar.Pattern>> links() {
        return links;
    }

    /// The recipes linked to the machine at "pos" (empty if none).
    public List<com.exoticmoss.simpleores.starstorage.quasar.Pattern> linksOf(BlockPos pos) {
        return links.getOrDefault(pos, List.of());
    }

    /// Links the recipe to the machine (or, if it was linked, unlinks it).
    public void toggleLink(BlockPos pos, com.exoticmoss.simpleores.starstorage.quasar.Pattern pattern) {
        List<com.exoticmoss.simpleores.starstorage.quasar.Pattern> list = new ArrayList<>(linksOf(pos));
        if (!list.remove(pattern)) list.add(pattern);
        if (list.isEmpty()) links.remove(pos);
        else links.put(pos.immutable(), list);
        setChanged();
    }

    /// Where the block is in the order of the screen (-1 = not in it yet).
    public int orderOf(BlockPos pos) {
        return order.indexOf(pos);
    }

    /// New blocks: they go to the end of the order of the screen.
    public void addToOrder(List<BlockPos> blocks) {
        for (BlockPos pos : blocks) {
            if (!order.contains(pos)) order.add(pos.immutable());
        }
        if (!blocks.isEmpty()) setChanged();
    }

    /// Removes every rule of the block at "pos".
    public void clearRules(BlockPos pos) {
        if (rules.remove(pos) != null) setChanged();
    }

    public SimpleContainer getUpgrades() {
        return upgrades;
    }

    /// How far (blocks in every direction) the blocks it takes care of can be.
    public int radius() {
        return RADIUS + Math.min(MAX_UPGRADES, upgrades.getItem(0).getCount()) * RANGE_PER_UPGRADE;
    }

    /// True if the block at "pos" is close enough to this Binary Star.
    public boolean inReach(BlockPos pos) {
        int radius = radius();
        return Math.abs(pos.getX() - worldPosition.getX()) <= radius && Math.abs(pos.getY() - worldPosition.getY()) <= radius
                && Math.abs(pos.getZ() - worldPosition.getZ()) <= radius;
    }

    /// When the Binary Star is broken, its upgrades drop on the ground.
    /// Called by the block when it is broken (onRemove).
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) Containers.dropContents(level, pos, upgrades);
    }

    // ---------------------------------------------------------------- the work

    public void serverTick() {
        if (level != null && ticks % SCAN_TICKS == 0) scanOutputs();
        if (++ticks % WORK_TICKS != 0 || rules.isEmpty() || level == null) return;
        StarNetwork network = StarNetwork.at(level, worldPosition);
        if (network.drives().isEmpty()) return;
        for (Map.Entry<BlockPos, Map<Integer, SlotRule>> entry : new ArrayList<>(rules.entrySet())) {
            BlockPos pos = entry.getKey();
            if (!inReach(pos) || !level.isLoaded(pos)) continue;
            IItemHandler items = StarAccess.items(level, pos);
            IFluidHandler fluids = StarAccess.fluids(level, pos);
            // Some mods (Mekanism...) only let things in and out through the sides of the block
            List<IItemHandler> itemSides = items == null ? List.of() : StarAccess.itemSides(level, pos);
            List<IFluidHandler> fluidSides = fluids == null ? List.of() : StarAccess.fluidSides(level, pos);
            for (SlotRule rule : entry.getValue().values()) {
                if (rule.slot() >= TANKS) {
                    int tank = rule.slot() - TANKS;
                    if (fluids != null && tank < fluids.getTanks()) workTank(network, fluids, fluidSides, tank, rule);
                } else if (items != null && rule.slot() < items.getSlots()) {
                    workSlot(network, items, itemSides, rule.slot(), rule, pos);
                }
            }
        }
    }

    private void workSlot(StarNetwork network, IItemHandler items, List<IItemHandler> sides,
                          int slot, SlotRule rule, BlockPos pos) {
        ItemStack inside = StarAccess.stack(items, slot);
        // The results an auto-craft of a Quasar waits for are taken by the Quasar itself (to the storage of its job)
        boolean forJob = !inside.isEmpty() && level instanceof net.minecraft.server.level.ServerLevel server
                && com.exoticmoss.simpleores.starstorage.autocraft.Autocraft.Job.waitsFor(server, network, pos, inside);
        if (rule.keeps()) {
            // BOTH: what is not one of the items of the slot goes out first
            if (rule.takes() && !forJob && !inside.isEmpty() && rule.items().stream().noneMatch(item -> ItemStack.isSameItemSameComponents(inside, item.create()))) {
                StarAccess.takeItems(network, items, sides, slot);
            }
            // Each item (a normal slot takes only the first that fits; a trash can takes them all)
            for (com.exoticmoss.simpleores.util.ItemStackTemplate item : rule.items()) StarAccess.keepItems(network, items, sides, slot, item.create());
        } else if (rule.takes() && !forJob) {
            StarAccess.takeItems(network, items, sides, slot);
        }
    }

    /// A tank: the same as a slot, with the liquid of the bucket of the rule.
    private static void workTank(StarNetwork network, IFluidHandler fluids, List<IFluidHandler> sides,
                                 int tank, SlotRule rule) {
        List<Fluid> kept = new ArrayList<>();
        if (rule.keeps()) {
            for (com.exoticmoss.simpleores.util.ItemStackTemplate item : rule.items()) {
                Fluid fluid = StarFluids.fromBucket(item.create());
                if (fluid != null) kept.add(fluid);
            }
        }
        if (!kept.isEmpty()) {
            FluidStack current = fluids.getFluidInTank(tank);
            if (rule.takes() && !current.isEmpty() && !kept.contains(current.getFluid())) StarAccess.takeFluid(network, fluids, sides, tank);
            for (Fluid fluid : kept) StarAccess.keepFluid(network, fluids, sides, tank, fluid);
        } else if (rule.takes()) {
            StarAccess.takeFluid(network, fluids, sides, tank);
        }
    }

    // ---------------------------------------------------------------- the network for other mods

    private NetworkHandler.Items itemHandler;
    private NetworkHandler.Fluids fluidHandler;

    /// The pipes, hoppers and machines of other mods that touch the Binary Star see the whole network
    /// (see NetworkHandler): they can put items and liquids in it and take them out.
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.BINARY_STAR_BE.get(), (binary, side) -> binary.items());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.BINARY_STAR_BE.get(), (binary, side) -> binary.fluids());
    }

    private @Nullable NetworkHandler.Items items() {
        if (level == null || level.isClientSide()) return null;
        if (itemHandler == null) itemHandler = new NetworkHandler.Items(level, worldPosition);
        return itemHandler;
    }

    private @Nullable NetworkHandler.Fluids fluids() {
        if (level == null || level.isClientSide()) return null;
        if (fluidHandler == null) fluidHandler = new NetworkHandler.Fluids(level, worldPosition);
        return fluidHandler;
    }

    /// The output slots (SlotRoles: the ones the screen of the machine shows as outputs) of every new block in reach
    /// get a TAKE rule by themselves, so the results of the machines go to the network without clicking each slot.
    private void scanOutputs() {
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        StarNetwork.forEachBlockEntity(level, worldPosition, radius(), blockEntity -> {
            if (blockEntity.isRemoved() || blockEntity instanceof BinaryStarBlockEntity) return;
            BlockPos pos = blockEntity.getBlockPos().immutable();
            IItemHandler items = StarAccess.items(level, pos);
            // (also the blocks with only tanks: they stay in the order of the screen)
            if (items != null || StarAccess.fluids(level, pos) != null) seen.add(pos);
            if (items == null) return;
            if (!configured.add(pos)) return;
            List<Integer> roles = SlotRoles.items(level, pos, items);
            Map<Integer, SlotRule> slots = rulesOf(pos);
            for (int slot = 0; slot < roles.size(); slot++) {
                if (roles.get(slot) == SlotRoles.OUTPUT && !slots.containsKey(slot)) setRule(pos, slot, new SlotRule(slot, SlotRule.TAKE, List.of()));
            }
            setChanged();
        });
        // A block that is gone (or out of reach): if a machine is put there again, it is set again
        if (configured.retainAll(seen)) setChanged();
        if (order.retainAll(seen)) setChanged();
    }

    // ---------------------------------------------------------------- saving

    /// The recipes linked to one machine, as they are saved.
    private record BlockLinks(BlockPos pos, List<com.exoticmoss.simpleores.starstorage.quasar.Pattern> recipes) {
        static final Codec<BlockLinks> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(BlockLinks::pos),
                com.exoticmoss.simpleores.starstorage.quasar.Pattern.CODEC.listOf().fieldOf("recipes").forGetter(BlockLinks::recipes)
        ).apply(inst, BlockLinks::new));
    }

    /// The rules of one block, as they are saved.
    private record BlockRules(BlockPos pos, List<SlotRule> slots) {
        static final Codec<BlockRules> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(BlockRules::pos),
                SlotRule.CODEC.listOf().fieldOf("slots").forGetter(BlockRules::slots)
        ).apply(inst, BlockRules::new));
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput output = new ValueOutput(tag, registries);
        List<BlockRules> list = new ArrayList<>();
        rules.forEach((pos, slots) -> list.add(new BlockRules(pos, new ArrayList<>(slots.values()))));
        output.store("rules", BlockRules.CODEC.listOf(), list);
        output.store("upgrades", ItemStack.OPTIONAL_CODEC, upgrades.getItem(0));
        output.store("configured", BlockPos.CODEC.listOf(), new ArrayList<>(configured));
        output.store("order", BlockPos.CODEC.listOf(), new ArrayList<>(order));
        List<BlockLinks> linkList = new ArrayList<>();
        links.forEach((pos, recipes) -> linkList.add(new BlockLinks(pos, recipes)));
        output.store("links", BlockLinks.CODEC.listOf(), linkList);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput input = new ValueInput(tag, registries);
        upgrades.setItem(0, input.read("upgrades", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        links.clear();
        for (BlockLinks block : input.read("links", BlockLinks.CODEC.listOf()).orElse(List.of())) {
            if (!block.recipes().isEmpty()) links.put(block.pos(), new ArrayList<>(block.recipes()));
        }
        order.clear();
        order.addAll(input.read("order", BlockPos.CODEC.listOf()).orElse(List.of()));
        configured.clear();
        configured.addAll(input.read("configured", BlockPos.CODEC.listOf()).orElse(List.of()));
        rules.clear();
        for (BlockRules block : input.read("rules", BlockRules.CODEC.listOf()).orElse(List.of())) {
            Map<Integer, SlotRule> slots = new TreeMap<>();
            for (SlotRule rule : block.slots()) slots.put(rule.slot(), rule);
            if (!slots.isEmpty()) rules.put(block.pos(), slots);
        }
    }
}
