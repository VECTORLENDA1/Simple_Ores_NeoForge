package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.gravitycraft.GravityCoreRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// The menu of the Quasar: like the Stellar Terminal (the items of the network can be taken and stored,
/// see TerminalScreen), but at the bottom right it makes the recipes that the Quasar keeps (one tab each):
///  - TAB_CRAFTING: a crafting table grid (3x3);
///  - TAB_CORE: the ingredients of a Gravity Core recipe (one slot more appears for each item, up to CORE_SLOTS).
///    Only 3x3 of them are shown (CORE_VIEW): the mouse wheel moves them up and down. A core slot keeps an amount
///    too (up to a stack): the amounts of a tier of the recipe save the recipe with that tier;
///  - TAB_FURNACE: the input of a furnace recipe;
///  - TAB_JOBS: (only the screen) the auto-crafts being made.
/// The slots of the recipes only show items (they don't keep them): clicking one with an item in the mouse cursor
/// puts a picture of it there; clicking with an empty cursor (or right-clicking) empties it. JEI fills them too.
/// The core slots: left-click with items adds them (the same item) or puts them; right-click with items adds one;
/// right-click with an empty cursor takes one away, left-click empties it.
/// The result is worked out by the server. Buttons: SAVE, CLEAR, TAB + n, CORE_SCROLL + n, REMOVE + n, CANCEL_JOB + job id.
public class QuasarMenu extends AbstractContainerMenu implements NetworkMenu {
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

    public final BlockPos pos;
    public final TerminalLayout layout;
    private final Player player;
    private final SimpleContainer pictures = new SimpleContainer(INVENTORY_START);
    /// Server: the items that can go in each slot of the crafting grid (from JEI, or just the item put there).
    private final List<List<ItemStackTemplate>> choices = new ArrayList<>();
    /// Server: the Gravity Core recipe the core slots make (null = none).
    private RecipeHolder<GravityCoreRecipe> coreRecipe;
    /// Server: the items of all the core slots (the shown ones are copied to the pictures).
    private final List<ItemStack> cores = new ArrayList<>();
    /// The first row of core slots shown.
    public int coreScroll;
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
        addSlot(new PictureSlot(FURNACE_SLOT, left + 18 + 1, top + 18 + 1, TAB_FURNACE));
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
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < INVENTORY_START) {
            if (slotId != RESULT_SLOT && slots.get(slotId).isActive()) {
                ItemStack carried = getCarried();
                if (slotId >= CORE_START && slotId < CORE_START + CORE_VIEW) clickCore(slotId, button, carried);
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
        } else if (slot >= CORE_START && slot < CORE_START + CORE_VIEW) {
            if (coreIndex(slot) < CORE_SLOTS) clickCore(slot, picture.isEmpty() ? 0 : 1, picture);
            return;
        } else {
            pictures.setItem(slot, picture);
        }
        updateResult();
    }

    /// JEI: fills the crafting grid with the items of a recipe (for each slot, the items that can go there).
    public void fill(List<List<ItemStack>> slots) {
        tab = TAB_CRAFTING;
        for (int i = 0; i < GRID_SLOTS; i++) {
            List<ItemStack> items = i < slots.size() ? slots.get(i) : List.of();
            List<ItemStackTemplate> slotChoices = new ArrayList<>();
            for (ItemStack item : items) {
                if (!item.isEmpty()) slotChoices.add(ItemStackTemplate.fromNonEmptyStack(item.copyWithCount(1)));
            }
            pictures.setItem(i, items.isEmpty() || items.get(0).isEmpty() ? ItemStack.EMPTY : items.get(0).copyWithCount(1));
            choices.set(i, slotChoices);
        }
        updateResult();
    }

    /// The server works out what the slots of the tab shown make.
    private void updateResult() {
        if (!(player.level() instanceof ServerLevel level)) return;
        ItemStack result = ItemStack.EMPTY;
        coreRecipe = null;
        if (tab == TAB_CRAFTING) {
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < GRID_SLOTS; i++) items.add(pictures.getItem(i).copy());
            CraftingInput input = CraftingInput.of(3, 3, items);
            if (!input.isEmpty()) {
                result = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
                        .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
            }
        } else if (tab == TAB_CORE) {
            coreRecipe = findCoreRecipe(level);
            if (coreRecipe != null) result = coreRecipe.value().createResult(coreRecipe.value().getTier());
        } else if (tab == TAB_FURNACE) {
            ItemStack item = pictures.getItem(FURNACE_SLOT);
            if (!item.isEmpty()) {
                SingleRecipeInput input = new SingleRecipeInput(item);
                result = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level)
                        .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
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
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
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
                for (int i = 0; i < GRID_SLOTS; i++) {
                    ItemStack stack = pictures.getItem(i);
                    if (stack.isEmpty()) inputs.add(List.of());
                    else if (choices.get(i).isEmpty()) inputs.add(List.of(ItemStackTemplate.fromNonEmptyStack(stack.copyWithCount(1))));
                    else inputs.add(choices.get(i));
                }
                quasar.addPattern(Pattern.crafting(output, result.getCount(), inputs));
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
                if (i < GRID_SLOTS) choices.set(i, List.of());
            }
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
                && player.position().distanceToSqr(Vec3.atCenterOf(pos)) <= TerminalAction.RANGE * TerminalAction.RANGE;
    }
}
