package com.exoticmoss.simpleores.starstorage.binary;

import com.exoticmoss.simpleores.SimpleOres;
import com.exoticmoss.simpleores.starstorage.network.StarNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Optional;
import com.exoticmoss.simpleores.starstorage.network.StarAccess;
import com.exoticmoss.simpleores.starstorage.network.StarFluids;

/// Sent by the client when the player changes the rules in the Binary Star screen.
/// The server checks everything again, changes the rules and sends back the new view.
public record BinaryAction(BlockPos binary, BlockPos block, int action, int slot) implements CustomPacketPayload {
    /// Only asks for the view again (the screen does it every second).
    public static final int REFRESH = 0;
    /// Left-click on a slot: next mode (nothing -> take -> keep -> input and output -> nothing).
    /// With an item in the mouse cursor: that item becomes the item the slot keeps.
    public static final int CLICK = 1;
    /// Right-click on a slot: no rule.
    public static final int REMOVE = 2;
    /// Every slot of the block: TAKE.
    public static final int TAKE_ALL = 3;
    /// No rules for the block.
    public static final int CLEAR = 4;

    /// How far (in blocks) the player can be from the Binary Star.
    public static final double RANGE = 8;

    public static final Type<BinaryAction> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "binary_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BinaryAction> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BinaryAction::binary,
            BlockPos.STREAM_CODEC, BinaryAction::block,
            ByteBufCodecs.VAR_INT, BinaryAction::action,
            ByteBufCodecs.VAR_INT, BinaryAction::slot,
            BinaryAction::new);

    @Override
    public Type<BinaryAction> type() {
        return TYPE;
    }

    public static void handle(BinaryAction message, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof BinaryMenu)) return;
        BlockPos pos = message.binary();
        if (player.position().distanceToSqr(Vec3.atCenterOf(pos)) > RANGE * RANGE) return;
        if (!(player.level().getBlockEntity(pos) instanceof BinaryStarBlockEntity binary)) return;

        BlockPos block = message.block();
        ResourceHandler<ItemResource> items = StarAccess.items(player.level(), block);
        ResourceHandler<FluidResource> fluids = StarAccess.fluids(player.level(), block);
        int slotCount = items == null ? 0 : items.size();
        int tankCount = fluids == null ? 0 : fluids.size();
        if (message.action() != REFRESH && binary.inReach(block) && (slotCount > 0 || tankCount > 0)) {
            int slot = message.slot();
            boolean tank = slot >= BinaryStarBlockEntity.TANKS;
            boolean valid = tank ? slot - BinaryStarBlockEntity.TANKS < tankCount : slot >= 0 && slot < slotCount;
            switch (message.action()) {
                case CLICK -> {
                    if (!valid) break;
                    // The item that is there now: for a tank, a bucket of its liquid
                    ItemStack inside = tank ? tankBucket(fluids, slot - BinaryStarBlockEntity.TANKS) : StarAccess.stack(items, slot);
                    ItemStack carried = player.containerMenu.getCarried();
                    // A tank keeps a liquid: only a full bucket can be chosen with the mouse cursor
                    if (tank && !carried.isEmpty() && StarFluids.fromBucket(carried) == null) break;
                    click(binary, block, inside, slot, carried);
                }
                case REMOVE -> binary.setRule(block, slot, null);
                case TAKE_ALL -> {
                    // Every slot that is not an input of the machine (a chest: every slot)
                    StarNetwork network = StarNetwork.at(player.level(), pos);
                    List<Integer> slotRoles = slotCount == 0 ? List.of() : StarAccess.itemRoles(items, StarAccess.itemSides(player.level(), block), slotCount, StarAccess.itemProbes(player, network));
                    List<Integer> tankRoles = tankCount == 0 ? List.of() : StarAccess.fluidRoles(fluids, StarAccess.fluidSides(player.level(), block), tankCount, StarAccess.fluidProbes(network));
                    boolean machine = slotRoles.contains(StarAccess.OUTPUT) || slotRoles.contains(StarAccess.UNKNOWN);
                    boolean tankMachine = tankRoles.contains(StarAccess.OUTPUT) || tankRoles.contains(StarAccess.UNKNOWN);
                    for (int i = 0; i < slotCount; i++) {
                        if (!machine || slotRoles.get(i) != StarAccess.INPUT) takeAll(binary, block, i);
                    }
                    for (int t = 0; t < tankCount; t++) {
                        if (!tankMachine || tankRoles.get(t) != StarAccess.INPUT) takeAll(binary, block, BinaryStarBlockEntity.TANKS + t);
                    }
                }
                case CLEAR -> binary.clearRules(block);
                default -> {
                }
            }
        }
        BinaryView.send(player, pos, false);
    }

    private static void takeAll(BinaryStarBlockEntity binary, BlockPos block, int slot) {
        SlotRule rule = binary.rulesOf(block).get(slot);
        binary.setRule(block, slot, new SlotRule(slot, (rule == null ? 0 : rule.mode()) | SlotRule.TAKE,
                rule == null ? List.of() : rule.items()));
    }

    /// A bucket of the liquid of the tank (EMPTY if the tank is empty or the liquid has no bucket).
    private static ItemStack tankBucket(ResourceHandler<FluidResource> fluids, int tank) {
        FluidResource fluid = fluids.getResource(tank);
        return fluid.isEmpty() ? ItemStack.EMPTY : StarFluids.bucket(fluid.getFluid());
    }

    /// Left-click on a slot: with an item in the mouse cursor, the slot keeps that item too (or, if it was kept
    /// already, not any more: a slot can keep many items, e.g. a trash can); else the next mode.
    /// The modes that keep an item need one: the one already chosen, or the one in the slot (else they are skipped).
    /// "inside" = what is in the slot now (for a tank: a bucket of its liquid).
    private static void click(BinaryStarBlockEntity binary, BlockPos block, ItemStack inside, int slot, ItemStack carried) {
        SlotRule old = binary.rulesOf(block).get(slot);
        int mode = old == null ? 0 : old.mode();
        List<ItemStackTemplate> items = old == null ? List.of() : old.items();
        if (!carried.isEmpty()) {
            SlotRule rule = new SlotRule(slot, (mode & SlotRule.TAKE) | SlotRule.KEEP, items)
                    .toggle(ItemStackTemplate.fromNonEmptyStack(carried.copyWithCount(1)));
            // The last item taken away: the slot doesn't keep anything any more
            if (rule.items().isEmpty()) rule = (mode & SlotRule.TAKE) != 0 ? new SlotRule(slot, SlotRule.TAKE, List.of()) : null;
            binary.setRule(block, slot, rule);
            return;
        }
        if (items.isEmpty() && !inside.isEmpty()) {
            items = List.of(ItemStackTemplate.fromNonEmptyStack(inside.copyWithCount(1)));
        }
        int[] order = {0, SlotRule.TAKE, SlotRule.KEEP, SlotRule.BOTH};
        int at = 0;
        for (int i = 0; i < order.length; i++) if (order[i] == mode) at = i;
        int next = 0;
        for (int step = 1; step <= order.length; step++) {
            next = order[(at + step) % order.length];
            if ((next & SlotRule.KEEP) == 0 || !items.isEmpty()) break;
        }
        // (the items stay in the rule also in TAKE mode, so they are there again in the next modes)
        binary.setRule(block, slot, next == 0 ? null : new SlotRule(slot, next, items));
    }
}
