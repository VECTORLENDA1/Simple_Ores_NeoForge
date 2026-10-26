package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// The Binary Star block entity: the rules of the slots of the blocks around it (RADIUS blocks, see SlotRule),
/// set in the Binary Star screen. It uses the network of the Magnetar whose field it is in.
/// Every WORK_TICKS ticks it does the work:
///  - TAKE slots: their items go to the network;
///  - KEEP slots: they are filled from the network up to a full stack of their item;
///  - BOTH: anything that is not the item of the slot goes to the network, and the slot is kept full of its item.
/// Works with every block that has an inventory (chests, barrels, furnaces, hoppers, brewing stands...).
public class BinaryStarBlockEntity extends BlockEntity {
    /// Every how many ticks the Binary Star moves the items (10 ticks = half a second).
    public static final int WORK_TICKS = 10;
    /// How far (blocks in every direction) the blocks it takes care of can be.
    public static final int RADIUS = 8;

    /// The rules of each block (position -> slot -> rule).
    private final Map<BlockPos, Map<Integer, SlotRule>> rules = new HashMap<>();
    private int ticks;

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

    /// True if the block at "pos" is close enough to this Binary Star.
    public boolean inReach(BlockPos pos) {
        return Math.abs(pos.getX() - worldPosition.getX()) <= RADIUS && Math.abs(pos.getY() - worldPosition.getY()) <= RADIUS
                && Math.abs(pos.getZ() - worldPosition.getZ()) <= RADIUS;
    }

    // ---------------------------------------------------------------- the work

    public void serverTick() {
        if (++ticks % WORK_TICKS != 0 || rules.isEmpty() || level == null) return;
        StarNetwork network = StarNetwork.at(level, worldPosition);
        if (network.drives().isEmpty()) return;
        for (Map.Entry<BlockPos, Map<Integer, SlotRule>> entry : new ArrayList<>(rules.entrySet())) {
            BlockPos pos = entry.getKey();
            if (!inReach(pos) || !level.isLoaded(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            for (SlotRule rule : entry.getValue().values()) {
                if (rule.slot() >= container.getContainerSize()) continue;
                if (rule.keeps()) {
                    ItemStack item = rule.item().get().create();
                    // BOTH: what is not the item of the slot goes out first
                    if (rule.takes() && !ItemStack.isSameItemSameComponents(container.getItem(rule.slot()), item)) {
                        take(network, container, rule.slot());
                    }
                    keep(network, container, rule.slot(), item);
                } else if (rule.takes()) {
                    take(network, container, rule.slot());
                }
            }
        }
    }

    /// The items of the slot go to the network (what doesn't fit stays).
    private static void take(StarNetwork network, Container container, int slot) {
        ItemStack stack = container.getItem(slot);
        if (stack.isEmpty()) return;
        ItemStack left = network.insert(stack.copy());
        if (left.getCount() == stack.getCount()) return;
        container.setItem(slot, left);
        container.setChanged();
    }

    /// The slot is filled with "item" from the network, up to a full stack.
    private static void keep(StarNetwork network, Container container, int slot, ItemStack item) {
        ItemStack current = container.getItem(slot);
        int max = Math.min(item.getMaxStackSize(), container.getMaxStackSize(item));
        if (current.isEmpty()) {
            if (!container.canPlaceItem(slot, item)) return;
            ItemStack taken = network.extract(item.copyWithCount(1), max);
            if (taken.isEmpty()) return;
            container.setItem(slot, taken);
        } else {
            if (!ItemStack.isSameItemSameComponents(current, item) || current.getCount() >= max) return;
            ItemStack taken = network.extract(item.copyWithCount(1), max - current.getCount());
            if (taken.isEmpty()) return;
            ItemStack grown = current.copy();
            grown.grow(taken.getCount());
            container.setItem(slot, grown);
        }
        container.setChanged();
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
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        rules.clear();
        for (BlockRules block : input.read("rules", BlockRules.CODEC.listOf()).orElse(List.of())) {
            Map<Integer, SlotRule> slots = new TreeMap<>();
            for (SlotRule rule : block.slots()) slots.put(rule.slot(), rule);
            if (!slots.isEmpty()) rules.put(block.pos(), slots);
        }
    }
}
