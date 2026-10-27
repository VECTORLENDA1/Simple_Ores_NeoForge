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
import net.neoforged.neoforge.transfer.ResourceHandler;
import org.jspecify.annotations.Nullable;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropContents(level, pos, upgrades);
    }

    // ---------------------------------------------------------------- the work

    public void serverTick() {
        if (++ticks % WORK_TICKS != 0 || rules.isEmpty() || level == null) return;
        StarNetwork network = StarNetwork.at(level, worldPosition);
        if (network.drives().isEmpty()) return;
        for (Map.Entry<BlockPos, Map<Integer, SlotRule>> entry : new ArrayList<>(rules.entrySet())) {
            BlockPos pos = entry.getKey();
            if (!inReach(pos) || !level.isLoaded(pos)) continue;
            ResourceHandler<ItemResource> items = StarAccess.items(level, pos);
            ResourceHandler<FluidResource> fluids = StarAccess.fluids(level, pos);
            // Some mods (Mekanism...) only let things in and out through the sides of the block
            List<ResourceHandler<ItemResource>> itemSides = items == null ? List.of() : StarAccess.itemSides(level, pos);
            List<ResourceHandler<FluidResource>> fluidSides = fluids == null ? List.of() : StarAccess.fluidSides(level, pos);
            for (SlotRule rule : entry.getValue().values()) {
                if (rule.slot() >= TANKS) {
                    int tank = rule.slot() - TANKS;
                    if (fluids != null && tank < fluids.size()) workTank(network, fluids, fluidSides, tank, rule);
                } else if (items != null && rule.slot() < items.size()) {
                    workSlot(network, items, itemSides, rule.slot(), rule);
                }
            }
        }
    }

    private static void workSlot(StarNetwork network, ResourceHandler<ItemResource> items, List<ResourceHandler<ItemResource>> sides,
                                 int slot, SlotRule rule) {
        if (rule.keeps()) {
            // BOTH: what is not one of the items of the slot goes out first
            ItemStack inside = StarAccess.stack(items, slot);
            if (rule.takes() && !inside.isEmpty() && rule.items().stream().noneMatch(item -> ItemStack.isSameItemSameComponents(inside, item.create()))) {
                StarAccess.takeItems(network, items, sides, slot);
            }
            // Each item (a normal slot takes only the first that fits; a trash can takes them all)
            for (net.minecraft.world.item.ItemStackTemplate item : rule.items()) StarAccess.keepItems(network, items, sides, slot, item.create());
        } else if (rule.takes()) {
            StarAccess.takeItems(network, items, sides, slot);
        }
    }

    /// A tank: the same as a slot, with the liquid of the bucket of the rule.
    private static void workTank(StarNetwork network, ResourceHandler<FluidResource> fluids, List<ResourceHandler<FluidResource>> sides,
                                 int tank, SlotRule rule) {
        List<Fluid> kept = new ArrayList<>();
        if (rule.keeps()) {
            for (net.minecraft.world.item.ItemStackTemplate item : rule.items()) {
                Fluid fluid = StarFluids.fromBucket(item.create());
                if (fluid != null) kept.add(fluid);
            }
        }
        if (!kept.isEmpty()) {
            FluidResource current = fluids.getResource(tank);
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
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.BINARY_STAR_BE.get(), (binary, side) -> binary.items());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.BINARY_STAR_BE.get(), (binary, side) -> binary.fluids());
    }

    private NetworkHandler.@Nullable Items items() {
        if (level == null || level.isClientSide()) return null;
        if (itemHandler == null) itemHandler = new NetworkHandler.Items(level, worldPosition);
        return itemHandler;
    }

    private NetworkHandler.@Nullable Fluids fluids() {
        if (level == null || level.isClientSide()) return null;
        if (fluidHandler == null) fluidHandler = new NetworkHandler.Fluids(level, worldPosition);
        return fluidHandler;
    }

    // ---------------------------------------------------------------- saving

    /// The rules of one block, as they are saved.
    private record BlockRules(BlockPos pos, List<SlotRule> slots) {
        static final Codec<BlockRules> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(BlockRules::pos),
                SlotRule.CODEC.listOf().fieldOf("slots").forGetter(BlockRules::slots)
        ).apply(inst, BlockRules::new));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        List<BlockRules> list = new ArrayList<>();
        rules.forEach((pos, slots) -> list.add(new BlockRules(pos, new ArrayList<>(slots.values()))));
        output.store("rules", BlockRules.CODEC.listOf(), list);
        output.store("upgrades", ItemStack.OPTIONAL_CODEC, upgrades.getItem(0));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        upgrades.setItem(0, input.read("upgrades", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        rules.clear();
        for (BlockRules block : input.read("rules", BlockRules.CODEC.listOf()).orElse(List.of())) {
            Map<Integer, SlotRule> slots = new TreeMap<>();
            for (SlotRule rule : block.slots()) slots.put(rule.slot(), rule);
            if (!slots.isEmpty()) rules.put(block.pos(), slots);
        }
    }
}
