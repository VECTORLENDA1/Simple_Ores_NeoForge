package com.exoticmoss.simpleores.starstorage.quasar;

import com.exoticmoss.simpleores.gravitycraft.GravityCoreRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.exoticmoss.simpleores.util.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.exoticmoss.simpleores.starstorage.ModMenus;
import com.exoticmoss.simpleores.starstorage.network.NetworkMenu;
import com.exoticmoss.simpleores.starstorage.network.StarNetwork;
import com.exoticmoss.simpleores.starstorage.terminal.TerminalAction;
import com.exoticmoss.simpleores.starstorage.terminal.TerminalContents;
import com.exoticmoss.simpleores.starstorage.terminal.TerminalLayout;

/// The menu of the Quasar: like the Stellar Terminal (the items of the network can be taken and stored,
/// see TerminalScreen), but at the bottom right it makes the recipes that the Quasar keeps (one tab each):
///  - TAB_CRAFTING: a crafting table grid (3x3). JEI's "+" on the recipe of a machine of another mod also puts it
///    here: the ingredients with their amounts, and the result of the recipe (saved as the recipe of that machine);
///  - TAB_CORE: the ingredients of a Gravity Core recipe (one slot more appears for each item, up to CORE_SLOTS).
///    Only 3x3 of them are shown (CORE_VIEW): the mouse wheel moves them up and down. A core slot keeps an amount
///    too (up to a stack): the amounts of a tier of the recipe save the recipe with that tier;
///  - TAB_FURNACE: the input of a furnace recipe;
///  - TAB_JOBS: (only the screen) the auto-crafts being made.
/// The slots of the recipes only show items (they don't keep them): clicking one with an item in the mouse cursor
/// puts a picture of it there; clicking with an empty cursor (or right-clicking) empties it. JEI fills them too.
/// The core slots: left-click with items adds them (the same item) or puts them; right-click with items adds one;
/// right-click with an empty cursor takes one away, left-click empties it.
/// With the recipe of a machine, the slots of the grid keep an amount too (clicks like the core slots; shift + click
/// adds or takes away one), and shift + click on the result changes how many it makes.
/// The mouse wheel on a slot of the grid that has many items (from JEI) picks one of them: then only that one is saved.
/// The result is worked out by the server. Buttons: SAVE, CLEAR, TAB + n, CORE_SCROLL + n, REMOVE + n, CANCEL_JOB + job id, PICK.
public class QuasarMenu extends AbstractContainerMenu implements NetworkMenu, com.exoticmoss.simpleores.starstorage.network.StarTabs.Anchored {
    /// The block the player is next to (this one, or the one whose screen opened this one with a tab).
    private BlockPos anchor;

    @Override
    public BlockPos menuPos() {
        return pos;
    }

    @Override
    public BlockPos anchor() {
        return anchor == null ? pos : anchor;
    }

    @Override
    public void setAnchor(BlockPos anchor) {
        this.anchor = anchor;
    }


    public static final int GRID_SLOTS = Pattern.SIZE;
    public static final int RESULT_SLOT = GRID_SLOTS;
    public static final int CORE_START = RESULT_SLOT + 1;
    /// The most ingredients a Gravity Core recipe can have (the Black Hole).
    public static final int CORE_SLOTS = 20;
    /// The core slots shown at once (3 rows of 3; the wheel scrolls one row).
    public static final int CORE_VIEW = 9;
    public static final int CORE_COLUMNS = 3;
    public static final int FURNACE_SLOT = CORE_START + CORE_VIEW;
    public static final int INVENTORY_START = FURNACE_SLOT + 1;

    public static final int TAB_CRAFTING = 0;
    public static final int TAB_CORE = 1;
    public static final int TAB_FURNACE = 2;
    public static final int TAB_JOBS = 3;

    public static final int SAVE = 0;
    public static final int CLEAR = 1;
    public static final int TAB = 10;
    public static final int CORE_SCROLL = 20;
    public static final int REMOVE = 100;
    public static final int CANCEL_JOB = 10_000;
    /// PICK + slot * 2 (+ 1 = the other way): the mouse wheel on a slot of the grid with many items.
    public static final int PICK = 1_000_000;
    /// The most of one ingredient in the recipe of a machine.
    public static final int MAX_AMOUNT = 99;

    public final BlockPos pos;
    public final TerminalLayout layout;
    private final Player player;
    private final SimpleContainer pictures = new SimpleContainer(INVENTORY_START);
    /// Server: the items that can go in each slot of the crafting grid (from JEI, or just the item put there).
    private final List<List<ItemStackTemplate>> choices = new ArrayList<>();
    /// Server: for the recipe of a machine (from JEI): what it makes (EMPTY = a crafting table recipe), how many of
    /// each slot of the grid, and the machines that make it.
    private ItemStack machineOutput = ItemStack.EMPTY;
    private final int[] amounts = new int[GRID_SLOTS];
    /// Server: the item picked with the mouse wheel in each slot of the grid (-1 = none: every item can be used).
    private final int[] picked = new int[GRID_SLOTS];
    private List<ItemStackTemplate> machines = List.of();
    /// Server: the Gravity Core recipe the core slots make (null = none).
    private RecipeHolder<GravityCoreRecipe> coreRecipe;
    /// Server: the items of all the core slots (the shown ones are copied to the pictures).
    private final List<ItemStack> cores = new ArrayList<>();
    /// The first row of core slots shown.
    public int coreScroll;
    /// Client: true while the grid has the recipe of a machine (sent by the server).
    private boolean syncedMachine;
    /// Client: how many core slots have an item (sent by the server).
    private int syncedCoreCount;
    /// The tab shown (the client tells the server with TAB + n).
    public int tab = TAB_CRAFTING;

    public QuasarMenu(int id, Inventory inventory, BlockPos pos, int rows) {
        super(ModMenus.QUASAR.get(), id);
        this.pos = pos;
        this.layout = new TerminalLayout(rows);
        this.player = inventory.player;
        int top = layout.inventoryTop();
        int left = TerminalLayout.craftLeft();
        for (int i = 0; i < GRID_SLOTS; i++) choices.add(List.of());
        java.util.Arrays.fill(amounts, 1);
        java.util.Arrays.fill(picked, -1);
        for (int i = 0; i < GRID_SLOTS; i++) {
            addSlot(new PictureSlot(i, left + (i % 3) * 18 + 1, top + (i / 3) * 18 + 1, TAB_CRAFTING));
        }
        addSlot(new PictureSlot(RESULT_SLOT, layout.resultX() + 1, layout.resultY() + 1, -1));
        for (int i = 0; i < CORE_VIEW; i++) {
            addSlot(new PictureSlot(CORE_START + i, left + (i % CORE_COLUMNS) * 18 + 1, top + (i / CORE_COLUMNS) * 18 + 1, TAB_CORE));
        }
        for (int i = 0; i < CORE_SLOTS; i++) cores.add(ItemStack.EMPTY);
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return player.level().isClientSide() ? syncedCoreCount : coreCount();
            }

            @Override
            public void set(int value) {
                syncedCoreCount = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return isMachine() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                syncedMachine = value == 1;
            }
        });
        addSlot(new PictureSlot(FURNACE_SLOT, left + 18 + 1, top + 18 + 1, TAB_FURNACE));
        addPlayerSlots(inventory, TerminalLayout.inventoryLeft() + 1, top + 1);
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

    /// How many core slots have an item (they are always the first ones).
    public int coreCount() {
        if (player.level().isClientSide()) return syncedCoreCount;
        int count = 0;
        while (count < CORE_SLOTS && !cores.get(count).isEmpty()) count++;
        return count;
    }

    /// The most the core slots can be scrolled (rows).
    public int maxCoreScroll() {
        int shown = Math.min(CORE_SLOTS, coreCount() + 1);
        int rows = (shown + CORE_COLUMNS - 1) / CORE_COLUMNS;
        return Math.max(0, rows - CORE_VIEW / CORE_COLUMNS);
    }

    /// The core slot (0 to CORE_SLOTS - 1) shown in the slot "slot" of the menu.
    private int coreIndex(int slot) {
        return coreScroll * CORE_COLUMNS + slot - CORE_START;
    }

    /// Server: copies the core slots shown to the pictures.
    private void showCores() {
        coreScroll = Math.max(0, Math.min(coreScroll, maxCoreScroll()));
        for (int i = 0; i < CORE_VIEW; i++) {
            int index = coreIndex(CORE_START + i);
            pictures.setItem(CORE_START + i, index < CORE_SLOTS ? cores.get(index) : ItemStack.EMPTY);
        }
    }

    /// A slot of the recipes: it shows a picture of an item, it never gives or takes real items.
    /// It is only there in its tab (the core slots: one more than the items put).
    private class PictureSlot extends Slot {
        private final int slotTab;

        PictureSlot(int index, int x, int y, int slotTab) {
            super(pictures, index, x, y);
            this.slotTab = slotTab;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean isActive() {
            if (slotTab == -1) return tab != TAB_JOBS;
            if (slotTab != tab) return false;
            if (slotTab != TAB_CORE) return true;
            int core = coreIndex(index);
            return core < CORE_SLOTS && core <= coreCount();
        }
    }

    // ---------------------------------------------------------------- the pictures

    @Override
    public void clicked(int slotId, int button, ClickType input, Player player) {
        if (slotId >= 0 && slotId < INVENTORY_START) {
            // The result of the recipe of a machine: shift + click changes how many it makes
            if (slotId == RESULT_SLOT && isMachine() && input == ClickType.QUICK_MOVE && tab == TAB_CRAFTING) {
                machineOutput.setCount(Math.max(1, Math.min(MAX_AMOUNT, machineOutput.getCount() + (button == 0 ? 1 : -1))));
                updateResult();
                return;
            }
            if (slotId != RESULT_SLOT && slots.get(slotId).isActive()) {
                ItemStack carried = getCarried();
                if (slotId >= CORE_START && slotId < CORE_START + CORE_VIEW) clickCore(slotId, button, carried);
                else if (slotId < GRID_SLOTS && isMachine()) clickAmount(slotId, button, carried, input == ClickType.QUICK_MOVE);
                else setPicture(slotId, carried.isEmpty() || button == 1 ? ItemStack.EMPTY : carried);
            }
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    /// A click on a core slot: changes its item and amount (see the top of this class).
    private void clickCore(int slot, int button, ItemStack carried) {
        int index = coreIndex(slot);
        if (index >= CORE_SLOTS) return;
        ItemStack current = cores.get(index);
        boolean same = !current.isEmpty() && ItemStack.isSameItemSameComponents(current, carried);
        int count;
        if (carried.isEmpty()) count = button == 1 ? current.getCount() - 1 : 0;
        else if (button == 1) count = same ? current.getCount() + 1 : 1;
        else count = same ? current.getCount() + carried.getCount() : carried.getCount();
        ItemStack item = carried.isEmpty() ? current : carried;
        setCore(index, count <= 0 || item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(Math.min(count, item.getMaxStackSize())));
    }

    /// A click on a slot of the grid with the recipe of a machine: changes its amount, like the core slots
    /// (left-click with items adds them, right-click with items adds one, right-click with an empty cursor takes one
    /// away, left-click with an empty cursor empties it; another item takes the place of the one there).
    /// Shift + left-click adds one, shift + right-click takes one away.
    private void clickAmount(int slot, int button, ItemStack carried, boolean shift) {
        ItemStack current = pictures.getItem(slot);
        if (shift) {
            if (current.isEmpty()) return;
            int count = amounts[slot] + (button == 0 ? 1 : -1);
            if (count <= 0) {
                setPicture(slot, ItemStack.EMPTY);
                return;
            }
            amounts[slot] = Math.min(count, MAX_AMOUNT);
            pictures.setItem(slot, current.copyWithCount(Math.min(amounts[slot], 99)));
            updateResult();
            return;
        }
        boolean same = !current.isEmpty() && !carried.isEmpty() && (ItemStack.isSameItemSameComponents(current, carried)
                || choices.get(slot).stream().anyMatch(choice -> ItemStack.isSameItemSameComponents(choice.create(), carried)));
        int count;
        if (carried.isEmpty()) count = button == 1 ? amounts[slot] - 1 : 0;
        else if (button == 1) count = same ? amounts[slot] + 1 : 1;
        else count = same ? amounts[slot] + carried.getCount() : carried.getCount();
        if (count <= 0) {
            setPicture(slot, ItemStack.EMPTY);
            return;
        }
        if (!same && !carried.isEmpty()) setPicture(slot, carried);
        amounts[slot] = Math.min(count, MAX_AMOUNT);
        pictures.setItem(slot, pictures.getItem(slot).copyWithCount(Math.min(amounts[slot], 99)));
        updateResult();
    }

    /// Changes a core slot. They stay together: a new item goes after the last one, an emptied slot closes the gap.
    private void setCore(int index, ItemStack stack) {
        List<ItemStack> items = new ArrayList<>(cores);
        if (stack.isEmpty()) items.set(index, ItemStack.EMPTY);
        else items.set(Math.min(index, coreCount()), stack);
        items.removeIf(ItemStack::isEmpty);
        for (int i = 0; i < CORE_SLOTS; i++) cores.set(i, i < items.size() ? items.get(i) : ItemStack.EMPTY);
        showCores();
        updateResult();
    }

    /// Server: the client scrolled the core slots.
    private void scrollCores(int row) {
        coreScroll = row;
        showCores();
        broadcastChanges();
    }

    /// Puts a picture of the item in a slot of the recipes (EMPTY = empties it).
    /// On a core slot, an item already there gets one more.
    public void setPicture(int slot, ItemStack stack) {
        if (slot < 0 || slot >= INVENTORY_START || slot == RESULT_SLOT) return;
        ItemStack picture = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (slot < GRID_SLOTS) {
            pictures.setItem(slot, picture);
            choices.set(slot, picture.isEmpty() ? List.of() : List.of(ItemStackTemplate.fromNonEmptyStack(picture)));
            amounts[slot] = 1;
            picked[slot] = -1;
        } else if (slot >= CORE_START && slot < CORE_START + CORE_VIEW) {
            if (coreIndex(slot) < CORE_SLOTS) clickCore(slot, picture.isEmpty() ? 0 : 1, picture);
            return;
        } else {
            pictures.setItem(slot, picture);
        }
        updateResult();
    }

    /// JEI: fills the crafting grid with the items of a recipe (for each slot, the items that can go there).
    /// The recipe of a machine also has the amounts, its result and the machines.
    public void fill(QuasarFill fill) {
        tab = TAB_CRAFTING;
        List<List<ItemStack>> slots = fill.slots();
        for (int i = 0; i < GRID_SLOTS; i++) {
            List<ItemStack> items = i < slots.size() ? slots.get(i) : List.of();
            List<ItemStackTemplate> slotChoices = new ArrayList<>();
            for (ItemStack item : items) {
                if (!item.isEmpty()) slotChoices.add(ItemStackTemplate.fromNonEmptyStack(item.copyWithCount(1)));
            }
            amounts[i] = Math.max(1, i < fill.amounts().size() ? fill.amounts().get(i) : 1);
            // The picture shows the amount (up to the most a stack can show)
            pictures.setItem(i, items.isEmpty() || items.get(0).isEmpty() ? ItemStack.EMPTY : items.get(0).copyWithCount(Math.min(amounts[i], 99)));
            choices.set(i, slotChoices);
            picked[i] = -1;
        }
        machineOutput = fill.output().copy();
        List<ItemStackTemplate> list = new ArrayList<>();
        for (ItemStack machine : fill.machines()) {
            if (!machine.isEmpty()) list.add(ItemStackTemplate.fromNonEmptyStack(machine.copyWithCount(1)));
        }
        machines = machineOutput.isEmpty() ? List.of() : list;
        if (machines.isEmpty()) machineOutput = ItemStack.EMPTY;
        updateResult();
    }

    /// The mouse wheel on a slot of the grid: shows the next (or the last) of its items, and only that one is saved.
    private void pick(int slot, int step) {
        List<ItemStackTemplate> slotChoices = choices.get(slot);
        if (slotChoices.size() < 2 || pictures.getItem(slot).isEmpty()) return;
        int current = picked[slot] >= 0 ? picked[slot] : 0;
        picked[slot] = Math.floorMod(current + step, slotChoices.size());
        pictures.setItem(slot, slotChoices.get(picked[slot]).create().copyWithCount(Math.min(amounts[slot], 99)));
        updateResult();
    }

    /// True while the grid has the recipe of a machine (its slots keep amounts).
    public boolean isMachine() {
        return player.level().isClientSide() ? syncedMachine : !machineOutput.isEmpty();
    }

    /// The server works out what the slots of the tab shown make.
    private void updateResult() {
        if (!(player.level() instanceof ServerLevel level)) return;
        ItemStack result = ItemStack.EMPTY;
        coreRecipe = null;
        if (tab == TAB_CRAFTING && isMachine()) {
            result = machineOutput.copyWithCount(Math.min(machineOutput.getCount(), 99));
        } else if (tab == TAB_CRAFTING) {
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < GRID_SLOTS; i++) items.add(pictures.getItem(i).copy());
            CraftingInput input = CraftingInput.of(3, 3, items);
            if (!input.isEmpty()) {
                result = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                        .map(holder -> holder.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
            }
        } else if (tab == TAB_CORE) {
            coreRecipe = findCoreRecipe(level);
            if (coreRecipe != null) result = coreRecipe.value().createResult(coreRecipe.value().getTier());
        } else if (tab == TAB_FURNACE) {
            ItemStack item = pictures.getItem(FURNACE_SLOT);
            if (!item.isEmpty()) {
                SingleRecipeInput input = new SingleRecipeInput(item);
                result = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level)
                        .map(holder -> holder.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
            }
        }
        pictures.setItem(RESULT_SLOT, result);
        broadcastChanges();
    }

    /// The Gravity Core recipe whose ingredients are exactly the items of the core slots (null = none).
    @SuppressWarnings("unchecked")
    private RecipeHolder<GravityCoreRecipe> findCoreRecipe(ServerLevel level) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < coreCount(); i++) items.add(cores.get(i));
        if (items.isEmpty()) return null;
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof GravityCoreRecipe recipe) || recipe.getUpgradeTier() != null) continue;
            if (recipe.getParts().size() != items.size()) continue;
            boolean every = true;
            for (GravityCoreRecipe.Part part : recipe.getParts()) {
                if (items.stream().noneMatch(part.item())) every = false;
            }
            if (every && recipe.usesAll(items)) return (RecipeHolder<GravityCoreRecipe>) holder;
        }
        return null;
    }

    /// The tier whose amounts are the amounts of the core slots (the lowest tier of the recipe if none is).
    private int coreTier(GravityCoreRecipe recipe) {
        for (int tier = recipe.getTier(); tier <= 4; tier++) {
            boolean every = true;
            for (GravityCoreRecipe.Part part : recipe.getParts()) {
                ItemStack item = cores.stream().filter(stack -> !stack.isEmpty() && part.item().test(stack)).findFirst().orElse(ItemStack.EMPTY);
                if (item.getCount() != part.countFor(tier)) every = false;
            }
            if (every) return tier;
        }
        return recipe.getTier();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= TAB && id < TAB + 4) {
            tab = id - TAB;
            updateResult();
            return true;
        }
        if (id >= PICK && id < PICK + GRID_SLOTS * 2) {
            pick((id - PICK) / 2, (id - PICK) % 2 == 0 ? -1 : 1);
            return true;
        }
        if (id >= CORE_SCROLL && id < CORE_SCROLL + CORE_SLOTS) {
            scrollCores(id - CORE_SCROLL);
            return true;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level().getBlockEntity(pos) instanceof QuasarBlockEntity quasar)) return false;
        if (id >= CANCEL_JOB) {
            for (QuasarBlockEntity other : StarNetwork.at(player.level(), pos).quasars()) other.cancelJob(id - CANCEL_JOB);
            QuasarJobs.send(serverPlayer, pos);
            return true;
        }
        if (id == SAVE) {
            ItemStack result = pictures.getItem(RESULT_SLOT);
            if (result.isEmpty()) return false;
            ItemStackTemplate output = ItemStackTemplate.fromNonEmptyStack(result.copyWithCount(1));
            if (tab == TAB_CRAFTING) {
                List<List<ItemStackTemplate>> inputs = new ArrayList<>();
                List<Integer> slotAmounts = new ArrayList<>();
                for (int i = 0; i < GRID_SLOTS; i++) {
                    ItemStack stack = pictures.getItem(i);
                    if (stack.isEmpty()) inputs.add(List.of());
                    else if (choices.get(i).isEmpty()) inputs.add(List.of(ItemStackTemplate.fromNonEmptyStack(stack.copyWithCount(1))));
                    else if (picked[i] >= 0) inputs.add(List.of(choices.get(i).get(picked[i])));
                    else inputs.add(choices.get(i));
                    slotAmounts.add(stack.isEmpty() ? 1 : amounts[i]);
                }
                if (isMachine()) quasar.addPattern(Pattern.machine(output, machineOutput.getCount(), inputs, slotAmounts, machines));
                else quasar.addPattern(Pattern.crafting(output, result.getCount(), inputs));
            } else if (tab == TAB_CORE && coreRecipe != null) {
                quasar.addPattern(QuasarCore.pattern(coreRecipe, coreTier(coreRecipe.value())));
            } else if (tab == TAB_FURNACE) {
                quasar.addPattern(Pattern.furnace(output, result.getCount(),
                        ItemStackTemplate.fromNonEmptyStack(pictures.getItem(FURNACE_SLOT).copyWithCount(1))));
            }
        } else if (id == CLEAR) {
            for (int i = 0; i < INVENTORY_START; i++) {
                if (i == RESULT_SLOT) continue;
                pictures.setItem(i, ItemStack.EMPTY);
                if (i < GRID_SLOTS) {
                    choices.set(i, List.of());
                    picked[i] = -1;
                }
            }
            machineOutput = ItemStack.EMPTY;
            machines = List.of();
            for (int i = 0; i < CORE_SLOTS; i++) cores.set(i, ItemStack.EMPTY);
            coreScroll = 0;
            updateResult();
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
                && com.exoticmoss.simpleores.starstorage.network.StarTabs.near(player, anchor(), TerminalAction.RANGE);
    }

    /// The 27 slots of the inventory and the 9 of the hotbar below them (4 pixels lower).
    private void addPlayerSlots(net.minecraft.world.entity.player.Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new net.minecraft.world.inventory.Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new net.minecraft.world.inventory.Slot(inventory, column, left + column * 18, top + 58));
        }
    }
}
