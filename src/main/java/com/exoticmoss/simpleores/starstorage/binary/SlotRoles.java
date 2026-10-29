package com.exoticmoss.simpleores.starstorage.binary;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/// What each slot of a block is for (green = input, red = output on the Binary Star screen), read the way the
/// screen of the block itself shows it:
///  - Mekanism: each slot of its machines has a kind (input, extra, output...) that its screen uses for the
///    colors of the slots. Its slots are found with the item handler of the block (the same order as the
///    Binary Star sees them). Read without needing Mekanism to be installed (by name);
///  - Industrial Foregoing (and the other mods made with Titanium): the inventories of the machine (see titanium);
///  - blocks like the furnace (WorldlyContainer): a slot that takes items through a side is an input, a slot
///    that only gives items is an output;
///  - anything else: not known (no color).
public final class SlotRoles {
    public static final int UNKNOWN = 0;
    public static final int INPUT = 1;
    public static final int OUTPUT = 2;

    /// Items tried on the slots of a WorldlyContainer (a slot that takes none of them and the item in it is not
    /// an input; the furnace output takes nothing).
    private static final List<ItemStack> PROBES = List.of(new ItemStack(Items.COBBLESTONE), new ItemStack(Items.COAL),
            new ItemStack(Items.IRON_ORE), new ItemStack(Items.BLAZE_POWDER), new ItemStack(Items.NETHER_WART), new ItemStack(Items.POTION));

    private SlotRoles() {
    }

    /// The role of every slot of "handler" (the item handler of the block at "pos").
    public static List<Integer> items(Level level, BlockPos pos, IItemHandler handler) {
        int size = handler.getSlots();
        List<Integer> roles = mekanism(handler, size);
        if (roles != null) return roles;
        roles = titanium(handler, size);
        if (roles != null) return roles;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof WorldlyContainer container && container.getContainerSize() == size) return worldly(container, size);
        return unknown(size);
    }

    public static List<Integer> unknown(int size) {
        List<Integer> roles = new ArrayList<>(size);
        for (int i = 0; i < size; i++) roles.add(UNKNOWN);
        return roles;
    }

    // ---------------------------------------------------------------- Mekanism

    /// Mekanism: its handler (ProxyItemHandler) keeps the block (a sided handler) and the side, and the block gives
    /// the slots of that side (getInventorySlots); each slot
    /// (BasicInventorySlot) has its kind (getSlotType: INPUT, EXTRA, OUTPUT, POWER...). null = not a Mekanism block.
    private static List<Integer> mekanism(IItemHandler handler, int size) {
        try {
            if (!(proxiedSlots(handler) instanceof List<?> slots) || slots.size() != size) return null;
            List<Integer> roles = new ArrayList<>(size);
            for (Object slot : slots) {
                Method type = slot == null ? null : find(slot.getClass(), "getSlotType");
                Object kind = type == null ? null : type.invoke(slot);
                String name = kind instanceof Enum<?> value ? value.name() : "";
                roles.add(switch (name) {
                    case "INPUT", "EXTRA" -> INPUT;
                    case "OUTPUT" -> OUTPUT;
                    default -> UNKNOWN;
                });
            }
            return roles;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /// The slots behind a Mekanism proxy handler (null = not one).
    private static Object proxiedSlots(IItemHandler handler) throws ReflectiveOperationException {
        Method containers = find(handler.getClass(), "getProxiedContainers");
        if (containers != null) return containers.invoke(handler);
        Object sided = null;
        Direction side = null;
        for (Class<?> c = handler.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (java.lang.reflect.Field field : c.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object value = field.get(handler);
                if (value instanceof Direction direction) side = direction;
                else if (value != null && sided == null && findSlots(value.getClass()) != null) sided = value;
            }
        }
        if (sided == null) return null;
        return findSlots(sided.getClass()).invoke(sided, side);
    }

    private static Method findSlots(Class<?> type) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals("getInventorySlots") && method.getParameterCount() == 1
                    && method.getParameterTypes()[0] == Direction.class) return method;
        }
        return null;
    }

    /// A method without parameters of the class or of a class above it (also not public).
    private static Method find(Class<?> type, String name) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- Titanium (Industrial Foregoing)

    /// Titanium: the handler of the machine (MultiInvCapabilityHandler) has its inventories in the order of the
    /// screen. An inventory named "output", or that can only push items out (not pull them in), is an output; the
    /// others are inputs. null = not a Titanium machine.
    private static List<Integer> titanium(IItemHandler handler, int size) {
        if (!handler.getClass().getSimpleName().equals("MultiInvCapabilityHandler")) return null;
        try {
            java.lang.reflect.Field field = handler.getClass().getDeclaredField("inventoryHandlers");
            field.setAccessible(true);
            if (!(field.get(handler) instanceof List<?> inventories)) return null;
            List<Integer> roles = new ArrayList<>(size);
            for (Object inventory : inventories) {
                if (!(inventory instanceof IItemHandler slots)) return null;
                Method name = find(inventory.getClass(), "getName");
                Method modes = find(inventory.getClass(), "getValidFacingModes");
                String text = name == null ? "" : String.valueOf(name.invoke(inventory)).toLowerCase(java.util.Locale.ROOT);
                boolean pull = true, push = false;
                if (modes != null && modes.invoke(inventory) instanceof Object[] valid) {
                    pull = false;
                    for (Object mode : valid) {
                        String mode2 = mode instanceof Enum<?> value ? value.name() : "";
                        pull |= mode2.equals("PULL");
                        push |= mode2.equals("PUSH");
                    }
                }
                int role = text.contains("output") || push && !pull ? OUTPUT : INPUT;
                for (int i = 0; i < slots.getSlots(); i++) roles.add(role);
            }
            return roles.size() == size ? roles : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- like the furnace

    private static List<Integer> worldly(WorldlyContainer container, int size) {
        boolean[] input = new boolean[size];
        boolean[] output = new boolean[size];
        for (Direction side : Direction.values()) {
            for (int slot : container.getSlotsForFace(side)) {
                if (slot < 0 || slot >= size) continue;
                ItemStack inside = container.getItem(slot);
                List<ItemStack> tries = new ArrayList<>(PROBES);
                if (!inside.isEmpty()) tries.add(0, inside.copyWithCount(1));
                for (ItemStack probe : tries) {
                    if (container.canPlaceItem(slot, probe) && container.canPlaceItemThroughFace(slot, probe, side)) {
                        input[slot] = true;
                        break;
                    }
                }
                if (container.canTakeItemThroughFace(slot, inside.isEmpty() ? PROBES.get(0) : inside, side)) output[slot] = true;
            }
        }
        List<Integer> roles = new ArrayList<>(size);
        for (int i = 0; i < size; i++) roles.add(input[i] ? INPUT : output[i] ? OUTPUT : UNKNOWN);
        return roles;
    }
}
