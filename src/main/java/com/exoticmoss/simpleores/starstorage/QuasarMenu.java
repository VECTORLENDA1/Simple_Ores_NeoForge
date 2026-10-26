package com.exoticmoss.simpleores.starstorage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/// The menu of the Quasar: like the Stellar Terminal (the items of the network can be taken and stored,
/// see TerminalScreen), but instead of a crafting grid it has a recipe grid (3x3 + result): it doesn't craft,
/// it only makes the recipes that the Quasar keeps.
/// The recipe grid only shows items (it doesn't keep them): clicking a slot of the grid with an item in the
/// mouse cursor puts a picture of it there; clicking with an empty cursor (or right-clicking) empties it.
/// JEI's "+" button fills the grid too (QuasarFill). The result is worked out by the server.
/// Buttons: SAVE saves the recipe in the Quasar, CLEAR empties the grid, REMOVE + n removes the saved recipe n.
public class QuasarMenu extends AbstractContainerMenu implements NetworkMenu {
    public static final int GRID_SLOTS = Pattern.SIZE;
    public static final int RESULT_SLOT = GRID_SLOTS;
    public static final int INVENTORY_START = GRID_SLOTS + 1;
    public static final int SAVE = 0;
    public static final int CLEAR = 1;
    public static final int REMOVE = 100;

    public final BlockPos pos;
    public final TerminalLayout layout;
    private final Player player;
    private final SimpleContainer grid = new SimpleContainer(GRID_SLOTS + 1);
    /// Server: the items that can go in each slot of the grid (from JEI, or just the item put there).
    private final List<List<ItemStackTemplate>> choices = new ArrayList<>();

    public QuasarMenu(int id, Inventory inventory, BlockPos pos, int rows) {
        super(ModMenus.QUASAR.get(), id);
        this.pos = pos;
        this.layout = new TerminalLayout(rows);
        this.player = inventory.player;
        int top = layout.inventoryTop();
        for (int i = 0; i < GRID_SLOTS; i++) choices.add(List.of());
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new PictureSlot(column + row * 3, TerminalLayout.craftLeft() + column * TerminalLayout.SLOT + 1, top + row * TerminalLayout.SLOT + 1));
            }
        }
        addSlot(new PictureSlot(RESULT_SLOT, layout.resultX() + 1, layout.resultY() + 1));
        addStandardInventorySlots(inventory, TerminalLayout.inventoryLeft() + 1, top + 1);
    }

    @Override
    public BlockPos networkPos() {
        return pos;
    }

    @Override
    public TerminalLayout layout() {
        return layout;
    }

    @Override
    public int resultSlot() {
        return RESULT_SLOT;
    }

    /// A slot of the recipe grid: it shows a picture of an item, it never gives or takes real items.
    private class PictureSlot extends Slot {
        PictureSlot(int index, int x, int y) {
            super(grid, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    // ---------------------------------------------------------------- the grid

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < GRID_SLOTS) {
            ItemStack carried = getCarried();
            if (carried.isEmpty() || button == 1) setSlot(slotId, ItemStack.EMPTY, List.of());
            else setSlot(slotId, carried.copyWithCount(1), List.of(ItemStackTemplate.fromNonEmptyStack(carried.copyWithCount(1))));
            return;
        }
        if (slotId == RESULT_SLOT) return;
        super.clicked(slotId, button, input, player);
    }

    private void setSlot(int slot, ItemStack stack, List<ItemStackTemplate> slotChoices) {
        grid.setItem(slot, stack);
        choices.set(slot, slotChoices);
        updateResult();
    }

    /// JEI: an item dragged from the JEI list onto a slot of the grid.
    public void setPicture(int slot, ItemStack stack) {
        if (slot < 0 || slot >= GRID_SLOTS) return;
        if (stack.isEmpty()) setSlot(slot, ItemStack.EMPTY, List.of());
        else setSlot(slot, stack.copyWithCount(1), List.of(ItemStackTemplate.fromNonEmptyStack(stack.copyWithCount(1))));
    }

    /// JEI: fills the grid with the items of a recipe (for each slot, the items that can go there).
    public void fill(List<List<ItemStack>> slots) {
        for (int i = 0; i < GRID_SLOTS; i++) {
            List<ItemStack> items = i < slots.size() ? slots.get(i) : List.of();
            List<ItemStackTemplate> slotChoices = new ArrayList<>();
            for (ItemStack item : items) {
                if (!item.isEmpty()) slotChoices.add(ItemStackTemplate.fromNonEmptyStack(item.copyWithCount(1)));
            }
            grid.setItem(i, items.isEmpty() || items.get(0).isEmpty() ? ItemStack.EMPTY : items.get(0).copyWithCount(1));
            choices.set(i, slotChoices);
        }
        updateResult();
    }

    /// The server works out what the grid makes.
    private void updateResult() {
        if (!(player.level() instanceof ServerLevel level)) return;
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < GRID_SLOTS; i++) items.add(grid.getItem(i).copy());
        CraftingInput input = CraftingInput.of(3, 3, items);
        ItemStack result = input.isEmpty() ? ItemStack.EMPTY
                : level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
        grid.setItem(RESULT_SLOT, result);
        broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level().getBlockEntity(pos) instanceof QuasarBlockEntity quasar)) return false;
        if (id == SAVE) {
            ItemStack result = grid.getItem(RESULT_SLOT);
            if (result.isEmpty()) return false;
            List<List<ItemStackTemplate>> inputs = new ArrayList<>();
            for (int i = 0; i < GRID_SLOTS; i++) {
                ItemStack stack = grid.getItem(i);
                if (stack.isEmpty()) inputs.add(List.of());
                else if (choices.get(i).isEmpty()) inputs.add(List.of(ItemStackTemplate.fromNonEmptyStack(stack.copyWithCount(1))));
                else inputs.add(choices.get(i));
            }
            quasar.addPattern(new Pattern(ItemStackTemplate.fromNonEmptyStack(result.copyWithCount(1)), result.getCount(), inputs));
        } else if (id == CLEAR) {
            for (int i = 0; i < GRID_SLOTS; i++) setSlot(i, ItemStack.EMPTY, List.of());
        } else if (id >= REMOVE) {
            quasar.removePattern(id - REMOVE);
        }
        QuasarPatterns.send(serverPlayer, pos);
        return true;
    }

    // ---------------------------------------------------------------- the rest

    /// Shift + click on an item of the inventory stores it in the network (like in the terminal).
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < INVENTORY_START || !(player instanceof ServerPlayer serverPlayer)) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack left = StarNetwork.at(player.level(), pos).insert(stack.copy());
        if (left.getCount() != stack.getCount()) {
            slot.set(left);
            TerminalContents.send(serverPlayer, pos);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).getBlock() instanceof QuasarBlock
                && player.position().distanceToSqr(Vec3.atCenterOf(pos)) <= TerminalAction.RANGE * TerminalAction.RANGE;
    }
}
