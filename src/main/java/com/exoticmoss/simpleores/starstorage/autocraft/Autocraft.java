package com.exoticmoss.simpleores.starstorage.autocraft;

import com.exoticmoss.simpleores.gravitycraft.GravityCoreBlockEntity;
import com.exoticmoss.simpleores.gravitycraft.GravityCoreRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.exoticmoss.simpleores.starstorage.magnetar.MagnetarBlockEntity;
import com.exoticmoss.simpleores.starstorage.quasar.Pattern;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarBlockEntity;
import com.exoticmoss.simpleores.starstorage.network.StarNetwork;

/// The auto-craft of the Quasar.
///  1. plan: works out everything that is needed, also the sub-recipes (planks -> sticks -> tool), using what
///     the network has; if something is missing, it says what and how many (nothing is crafted);
///  2. run: crafts every step in order, taking the ingredients from the network and putting the results back.
///     Crafting table steps are done at once. Gravity Core steps take time: the ingredients go to the free
///     Gravity Cores of the field of the Magnetar, and their results come back to the network by themselves.
///     So a plan with Gravity Core steps becomes a Job, worked on by a Quasar of the network every few ticks (faster with Solar Wind Upgrades)
///     (each step waits until its ingredients are in the network).
public final class Autocraft {
    /// Sub-recipes can go this deep at most (stops recipes that need themselves).
    private static final int MAX_DEPTH = 16;
    /// At most this many crafts in one request (only a safety limit: big requests are crafted little by little).
    public static final long MAX_CRAFTS = 1_000_000_000_000_000L;
    /// Plans with only crafting table steps and at most this many crafts are crafted at once; bigger ones become
    /// a Job of a Quasar (a little every few ticks, so the server doesn't freeze).
    private static final long CRAFTS_AT_ONCE = 512;
    /// A Job makes at most this many crafting table crafts each time it works.
    private static final int CRAFTS_PER_WORK = 256;
    /// A job that makes no progress for this many ticks is cancelled (5 minutes).
    private static final int GIVE_UP_TICKS = 6000;

    /// One step of the plan: craft "times" times the pattern, with these items (null = empty slot).
    private record Step(Pattern pattern, List<ItemStackTemplate> items, long times) {
    }

    /// The result of a request: what is missing (empty = it was crafted / started), or an error message key.
    public record Result(Map<ItemStackTemplate, Long> missing, String error) {
        public boolean ok() {
            return missing.isEmpty() && error == null;
        }
    }

    private final Map<ItemStackTemplate, Long> stock = new HashMap<>();
    private final List<Pattern> patterns;
    private final List<Step> steps = new ArrayList<>();
    private final Map<ItemStackTemplate, Long> missing = new LinkedHashMap<>();
    private long crafts;
    /// The items being planned now (this one and the ones above it), so a recipe never needs its own result.
    private final java.util.Set<ItemStackTemplate> making = new java.util.HashSet<>();

    private Autocraft(StarNetwork network, boolean advanced) {
        for (Map.Entry<ItemStackTemplate, Long> entry : network.contents()) stock.put(entry.getKey(), entry.getValue());
        this.patterns = network.patterns().stream().filter(pattern -> advanced || !pattern.advanced()).toList();
    }

    /// Plans and, if nothing is missing, crafts "amount" of "item" into the network.
    /// "advanced" = the Gravity Core recipes can be used (the network has a Stellar Terminal II).
    /// "chosen" = the recipe chosen by the player (empty = the first one that has everything).
    public static Result request(ServerLevel level, StarNetwork network, BlockPos pos, ItemStackTemplate item, long amount,
                                 boolean advanced, Optional<Pattern> chosen) {
        Autocraft craft = new Autocraft(network, advanced);
        if (craft.patternsFor(item).isEmpty()) return new Result(Map.of(), "screen.simpleores.terminal.craft.no_recipe");
        // The items asked for are always crafted (even if the network already has some)
        if (chosen.isPresent() && craft.patterns.contains(chosen.get()) && chosen.get().makes(item.create())) {
            craft.craft(chosen.get(), item, amount, 0);
        } else {
            craft.craftAny(item, amount, 0);
        }
        if (craft.crafts > MAX_CRAFTS) return new Result(Map.of(), "screen.simpleores.terminal.craft.too_big");
        if (!craft.missing.isEmpty()) return new Result(craft.missing, null);
        if (craft.steps.stream().anyMatch(step -> step.pattern().takesTime()) || craft.crafts > CRAFTS_AT_ONCE) {
            if (network.quasars().isEmpty()) return new Result(Map.of(), "screen.simpleores.terminal.craft.no_recipe");
            // A Quasar of this dimension if there is one (the network can reach others with Wormhole Links)
            QuasarBlockEntity quasar = network.quasars().stream().filter(other -> other.getLevel() == level).findFirst()
                    .orElse(network.quasars().get(0));
            quasar.startJob(new Job(craft.steps, level, pos, item, amount));
            return new Result(Map.of(), null);
        }
        Job job = new Job(craft.steps, level, pos, item, amount);
        while (!job.done()) {
            if (!job.work(level, network)) return new Result(Map.of(), "screen.simpleores.terminal.craft.changed");
        }
        return new Result(Map.of(), null);
    }

    /// The recipes for this item (in the order of the Quasars).
    private List<Pattern> patternsFor(ItemStackTemplate item) {
        ItemStack stack = item.create();
        return patterns.stream().filter(pattern -> pattern.makes(stack)).toList();
    }

    // ---------------------------------------------------------------- plan

    /// Uses "amount" of "item" from the network, crafting what is not there. false = something is missing.
    private boolean need(ItemStackTemplate item, long amount, int depth) {
        long have = stock.getOrDefault(item, 0L);
        long used = Math.min(have, amount);
        if (used > 0) stock.put(item, have - used);
        amount -= used;
        if (amount <= 0) return true;
        // An item that is being crafted further up is not crafted again to make itself
        // (e.g. ingot -> block -> ingot): what the network doesn't have of it is missing
        if (patternsFor(item).isEmpty() || depth >= MAX_DEPTH || making.contains(item)) {
            missing.merge(item, amount, Long::sum);
            return false;
        }
        return craftAny(item, amount, depth);
    }

    /// Plans crafting "amount" of "item" with the first of its recipes that has everything it needs
    /// (if none has, the first one: it says what is missing).
    private boolean craftAny(ItemStackTemplate item, long amount, int depth) {
        List<Pattern> options = patternsFor(item);
        if (options.size() > 1) {
            for (Pattern option : options) {
                // Try it; if something is missing, everything goes back to how it was
                Map<ItemStackTemplate, Long> stockBefore = new HashMap<>(stock);
                Map<ItemStackTemplate, Long> missingBefore = new LinkedHashMap<>(missing);
                int stepsBefore = steps.size();
                long craftsBefore = crafts;
                if (craft(option, item, amount, depth) && crafts <= MAX_CRAFTS) return true;
                stock.clear();
                stock.putAll(stockBefore);
                missing.clear();
                missing.putAll(missingBefore);
                while (steps.size() > stepsBefore) steps.remove(steps.size() - 1);
                crafts = craftsBefore;
            }
        }
        return craft(options.get(0), item, amount, depth);
    }

    /// Plans crafting at least "amount" of "item" with "pattern" (the extra goes to the stock).
    private boolean craft(Pattern pattern, ItemStackTemplate item, long amount, int depth) {
        making.add(item);
        try {
            return craftSteps(pattern, item, amount, depth);
        } finally {
            making.remove(item);
        }
    }

    private boolean craftSteps(Pattern pattern, ItemStackTemplate item, long amount, int depth) {
        long times = (amount + pattern.count() - 1) / pattern.count();
        crafts += times;
        if (crafts > MAX_CRAFTS) return false;
        boolean ok = true;
        List<ItemStackTemplate> items = new ArrayList<>();
        for (int i = 0; i < pattern.inputs().size(); i++) {
            List<ItemStackTemplate> choices = pattern.inputs().get(i);
            if (choices.isEmpty()) {
                items.add(null);
                continue;
            }
            long needed = times * pattern.amount(i);
            ItemStackTemplate choice = choose(choices, needed);
            items.add(choice);
            ok &= need(choice, needed, depth + 1);
        }
        steps.add(new Step(pattern, items, times));
        long extra = times * pattern.count() - amount;
        if (extra > 0) stock.merge(item, extra, Long::sum);
        return ok;
    }

    /// The item to use for an ingredient: one the network has enough of, else one it has some of,
    /// else one that has a recipe, else the first one.
    private ItemStackTemplate choose(List<ItemStackTemplate> choices, long needed) {
        for (ItemStackTemplate choice : choices) {
            if (stock.getOrDefault(choice, 0L) >= needed) return choice;
        }
        for (ItemStackTemplate choice : choices) {
            if (stock.getOrDefault(choice, 0L) > 0) return choice;
        }
        for (ItemStackTemplate choice : choices) {
            if (!patternsFor(choice).isEmpty()) return choice;
        }
        return choices.get(0);
    }

    // ---------------------------------------------------------------- run

    /// The steps of a plan being crafted. "pos" is a position inside the field of the Magnetar (the terminal),
    /// in the dimension "dimension".
    public static class Job {
        private static int nextId = 1;
        /// Solar Wind Upgrades of the Magnetar (0 to 4): every how many ticks the job works,
        private static final int[] INTERVAL = {10, 5, 2, 1, 1};
        /// and how many sets of ingredients one Gravity Core gets at a time (as many as fit in its orbit).
        private static final int[] SETS = {1, 4, 16, 64, Integer.MAX_VALUE};

        public final int id = nextId++;
        public final ItemStackTemplate output;
        public final long amount;
        private final List<Step> steps;
        private final BlockPos pos;
        private final ResourceKey<Level> dimension;
        private int index;
        private int ticks;
        /// Items of the output back in the network (see arrived).
        private long made;
        private boolean arrivedNow;
        /// True while a Gravity Core of the field still works for this network (after every step was given out).
        private boolean coresWorking;
        private long done;
        private int idle;
        /// What the job waits for now (a message key, for the jobs tab of the Quasar).
        private String status = "screen.simpleores.quasar.job.working";
        /// Furnace results still to be collected: furnace -> item and how many.
        private final Map<BlockPos, FurnaceWait> furnaces = new HashMap<>();
        /// The same for the machines of other mods.
        private final Map<BlockPos, FurnaceWait> machines = new HashMap<>();

        private record FurnaceWait(ItemStackTemplate output, long expected) {
        }

        private Job(List<Step> steps, ServerLevel level, BlockPos pos, ItemStackTemplate output, long amount) {
            this.steps = steps;
            this.pos = pos.immutable();
            this.dimension = level.dimension();
            this.output = output;
            this.amount = amount;
        }

        /// Done when every step was given out and the results came back to the network
        /// (or no Gravity Core is still working for it: a recipe can give fewer items than planned).
        public boolean done() {
            return index >= steps.size() && furnaces.isEmpty() && machines.isEmpty() && (made >= amount || !coresWorking);
        }

        public int stepIndex() {
            return Math.min(index, steps.size());
        }

        public int stepCount() {
            return steps.size();
        }

        public String status() {
            return status;
        }

        /// One sub-craft of the job (for the jobs tab): what it makes, how many, how many are made, and if it is
        /// being made now.
        public record SubCraft(ItemStackTemplate output, long amount, long made, boolean current) {
        }

        /// The sub-crafts: every step but the last one (the last one makes what was asked for), the steps that make
        /// the same item together, in the order they are made.
        public List<SubCraft> subCrafts() {
            Map<ItemStackTemplate, SubCraft> merged = new LinkedHashMap<>();
            for (int i = 0; i < steps.size() - 1; i++) {
                Step step = steps.get(i);
                long count = step.pattern().count();
                long stepMade = i < index ? step.times() : i == index ? done : 0;
                SubCraft one = new SubCraft(step.pattern().output(), step.times() * count, stepMade * count, i == index);
                merged.merge(one.output(), one, (a, b) -> new SubCraft(a.output(), a.amount() + b.amount(),
                        a.made() + b.made(), a.current() || b.current()));
            }
            return new ArrayList<>(merged.values());
        }

        /// How many of the items asked for are made: every craft of every step counts the same
        /// (with one step, it is just the items made).
        public long made() {
            return made;
        }

        /// Items of the output that came back to the network (from the cores, the furnaces or the crafting
        /// table). Returns how many this job took (the rest can count for another job).
        long arrived(ItemStack stack) {
            if (made >= amount || !ItemStack.isSameItemSameComponents(stack, output.create())) return 0;
            long counted = Math.min(stack.getCount(), amount - made);
            made += counted;
            arrivedNow = true;
            return counted;
        }

        /// A Gravity Core gave items to the network of "networkPos": they count for the jobs waiting for them.
        public static void arrived(ServerLevel level, BlockPos networkPos, ItemStack stack) {
            long left = stack.getCount();
            for (QuasarBlockEntity quasar : StarNetwork.at(level, networkPos).quasars()) {
                for (Job job : quasar.getJobs()) {
                    if (left <= 0) return;
                    if (job.dimension == level.dimension() && job.pos.equals(networkPos)) {
                        left -= job.arrived(stack.copyWithCount((int) left));
                    }
                }
            }
        }
        /// Quasar: every tick (the job works every INTERVAL ticks). false = the job is cancelled (no progress for too long).
        public boolean tick(ServerLevel quasarLevel) {
            ServerLevel level = quasarLevel.getServer().getLevel(dimension);
            if (level == null) return false;
            MagnetarBlockEntity magnetar = StarNetwork.findMagnetar(level, pos);
            int interval = INTERVAL[magnetar == null ? 0 : magnetar.speed()];
            if (++ticks < interval) return true;
            ticks = 0;
            StarNetwork network = StarNetwork.at(level, pos);
            boolean progress = work(level, network);
            idle = progress ? 0 : idle + interval;
            return idle < GIVE_UP_TICKS;
        }

        /// Does as much as it can now. Returns true if something was done.
        boolean work(ServerLevel level, StarNetwork network) {
            boolean progress = collectFurnaces(level, network) | collectMachines(level, network) | arrivedNow;
            arrivedNow = false;
            while (index < steps.size()) {
                Step step = steps.get(index);
                // The last step stops when enough came back (some recipes give more than planned)
                if (index == steps.size() - 1 && made >= amount) {
                    index++;
                    break;
                }
                boolean stepProgress;
                if (step.pattern().isCore()) stepProgress = workCore(level, network, step);
                else if (step.pattern().furnace()) stepProgress = workFurnace(level, network, step);
                else if (step.pattern().isMachine()) stepProgress = workMachine(level, network, step);
                else stepProgress = workCrafting(level, network, step);
                progress |= stepProgress;
                if (done < step.times()) return progress;
                index++;
                done = 0;
            }
            coresWorking = index >= steps.size() && made < amount && coresWorkingFor(level);
            if (!furnaces.isEmpty()) status = "screen.simpleores.quasar.job.waiting_furnaces";
            else if (coresWorking || !machines.isEmpty()) status = "screen.simpleores.quasar.job.waiting_results";
            return progress || furnaces.isEmpty() && machines.isEmpty() && !coresWorking;
        }

        /// True if a Gravity Core of the field still has items for the network of this job.
        private boolean coresWorkingFor(ServerLevel level) {
            if (steps.stream().noneMatch(step -> step.pattern().isCore())) return false;
            MagnetarBlockEntity magnetar = StarNetwork.findMagnetar(level, pos);
            if (magnetar == null) return false;
            boolean[] working = {false};
            StarNetwork.forEachBlockEntity(level, magnetar.getBlockPos(), magnetar.radius(), blockEntity -> {
                if (blockEntity instanceof GravityCoreBlockEntity core && core.isWorkingFor(pos)) working[0] = true;
            });
            return working[0];
        }

        /// Crafting table: crafts as many times as possible now.
        private boolean workCrafting(ServerLevel level, StarNetwork network, Step step) {
            boolean progress = false;
            int count = 0;
            RecipeHolder<CraftingRecipe> last = null;
            while (done < step.times() && count++ < CRAFTS_PER_WORK) {
                List<ItemStack> grid = new ArrayList<>();
                for (ItemStackTemplate item : step.items()) {
                    if (item == null) {
                        grid.add(ItemStack.EMPTY);
                        continue;
                    }
                    ItemStack got = network.extract(item.create(), 1);
                    if (got.isEmpty()) {
                        giveBack(network, grid);
                        status = "screen.simpleores.quasar.job.waiting_items";
                        return progress;
                    }
                    grid.add(got);
                }
                while (grid.size() < Pattern.SIZE) grid.add(ItemStack.EMPTY);
                CraftingInput input = CraftingInput.of(3, 3, grid);
                // The same recipe as the last craft (much faster than looking for it again)
                Optional<RecipeHolder<CraftingRecipe>> recipe = last != null && last.value().matches(input, level)
                        ? Optional.of(last) : level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
                if (recipe.isEmpty()) {
                    giveBack(network, grid);
                    return progress;
                }
                last = recipe.get();
                ItemStack result = recipe.get().value().assemble(input);
                NonNullList<ItemStack> remaining = recipe.get().value().getRemainingItems(input);
                arrived(result);
                network.insert(result);
                for (ItemStack rest : remaining) {
                    if (!rest.isEmpty()) network.insert(rest);
                }
                done++;
                progress = true;
            }
            return progress;
        }

        /// Gravity Core: gives sets of ingredients to the free cores (of a high enough tier). With Solar Wind Upgrades,
        /// more sets at a time, and also to the cores already making this recipe for this network.
        /// The step is done when every set was given (the results come back to the network by themselves).
        @SuppressWarnings("unchecked")
        private boolean workCore(ServerLevel level, StarNetwork network, Step step) {
            Optional<RecipeHolder<?>> holder = level.recipeAccess().byKey(step.pattern().coreRecipe().get());
            if (holder.isEmpty() || !(holder.get().value() instanceof GravityCoreRecipe)) return false;
            RecipeHolder<GravityCoreRecipe> recipe = (RecipeHolder<GravityCoreRecipe>) (RecipeHolder<?>) holder.get();
            MagnetarBlockEntity magnetar = StarNetwork.findMagnetar(level, pos);
            if (magnetar == null) return false;
            int speed = magnetar.speed();
            List<GravityCoreBlockEntity> cores = new ArrayList<>();
            StarNetwork.forEachBlockEntity(level, magnetar.getBlockPos(), magnetar.radius(), blockEntity -> {
                if (blockEntity instanceof GravityCoreBlockEntity core && core.getTier().level >= step.pattern().coreTier()
                        && (core.isFree() || speed > 0 && core.isMaking(recipe.id(), pos))) {
                    cores.add(core);
                }
            });
            boolean progress = false;
            status = cores.isEmpty() ? "screen.simpleores.quasar.job.waiting_cores" : "screen.simpleores.quasar.job.working";
            List<GravityCoreRecipe.Part> parts = recipe.value().getParts();
            for (GravityCoreBlockEntity core : cores) {
                if (done >= step.times()) break;
                // One set, with the amounts of the tier of this core (bigger cores can need other amounts)
                List<ItemStack> set = new ArrayList<>();
                for (int i = 0; i < step.items().size(); i++) {
                    ItemStackTemplate item = step.items().get(i);
                    if (item == null) continue;
                    int amount = i < parts.size() ? parts.get(i).countFor(core.getTier().level) : step.pattern().amount(i);
                    set.add(item.create().copyWithCount(amount));
                }
                int sets = core.setsThatFit(set, (int) Math.min(SETS[speed], step.times() - done));
                if (sets <= 0) continue;
                // Take them from the network (only whole sets go to the core)
                List<ItemStack> got = new ArrayList<>();
                for (ItemStack one : set) got.add(network.extract(one, one.getCount() * sets));
                int whole = sets;
                for (int i = 0; i < set.size(); i++) whole = Math.min(whole, got.get(i).getCount() / set.get(i).getCount());
                List<ItemStack> give = new ArrayList<>();
                for (int i = 0; i < set.size(); i++) {
                    ItemStack stack = got.get(i);
                    int keep = whole * set.get(i).getCount();
                    if (stack.getCount() > keep) {
                        network.insert(stack.copyWithCount(stack.getCount() - keep));
                        stack.setCount(keep);
                    }
                    if (!stack.isEmpty()) give.add(stack);
                }
                if (whole <= 0) {
                    status = "screen.simpleores.quasar.job.waiting_items";
                    continue;
                }
                // In stacks of the normal size (like the orbits of the core)
                List<ItemStack> stacks = new ArrayList<>();
                for (ItemStack stack : give) {
                    while (!stack.isEmpty()) stacks.add(stack.split(stack.getMaxStackSize()));
                }
                giveBack(network, core.insertFromNetwork(stacks, recipe, pos));
                done += whole;
                progress = true;
            }
            return progress;
        }

        /// Furnace: puts the input in the furnaces of the field (their input slot), as many as fit.
        /// The cooked items are collected from those furnaces by collectFurnaces.
        private boolean workFurnace(ServerLevel level, StarNetwork network, Step step) {
            MagnetarBlockEntity magnetar = StarNetwork.findMagnetar(level, pos);
            ItemStackTemplate input = step.items().isEmpty() ? null : step.items().get(0);
            if (magnetar == null || input == null) return false;
            List<FurnaceBlockEntity> list = new ArrayList<>();
            StarNetwork.forEachBlockEntity(level, magnetar.getBlockPos(), magnetar.radius(), blockEntity -> {
                if (blockEntity instanceof FurnaceBlockEntity furnace) list.add(furnace);
            });
            status = list.isEmpty() ? "screen.simpleores.quasar.job.waiting_furnaces" : "screen.simpleores.quasar.job.working";
            boolean progress = false;
            ItemStack item = input.create();
            for (FurnaceBlockEntity furnace : list) {
                if (done >= step.times()) break;
                ItemStack current = furnace.getItem(0);
                if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, item)) continue;
                int room = item.getMaxStackSize() - current.getCount();
                int send = (int) Math.min(room, step.times() - done);
                if (send <= 0) continue;
                ItemStack got = network.extract(item.copyWithCount(1), send);
                if (got.isEmpty()) {
                    status = "screen.simpleores.quasar.job.waiting_items";
                    return progress;
                }
                if (current.isEmpty()) furnace.setItem(0, got);
                else {
                    ItemStack grown = current.copy();
                    grown.grow(got.getCount());
                    furnace.setItem(0, grown);
                }
                furnace.setChanged();
                done += got.getCount();
                long expected = (long) got.getCount() * step.pattern().count();
                furnaces.merge(furnace.getBlockPos(), new FurnaceWait(step.pattern().output(), expected),
                        (a, b) -> new FurnaceWait(a.output(), a.expected() + b.expected()));
                progress = true;
            }
            return progress;
        }

        /// Takes the cooked items of the furnaces used by this job to the network.
        /// A furnace stops being watched when all its items came out (or someone else took them).
        private boolean collectFurnaces(ServerLevel level, StarNetwork network) {
            boolean progress = false;
            for (Map.Entry<BlockPos, FurnaceWait> entry : new ArrayList<>(furnaces.entrySet())) {
                BlockPos furnacePos = entry.getKey();
                FurnaceWait wait = entry.getValue();
                if (!(level.getBlockEntity(furnacePos) instanceof Container furnace) || furnace.getContainerSize() < 3) {
                    furnaces.remove(furnacePos);
                    continue;
                }
                ItemStack out = furnace.getItem(2);
                long expected = wait.expected();
                if (!out.isEmpty() && ItemStack.isSameItemSameComponents(out, wait.output().create())) {
                    int take = (int) Math.min(out.getCount(), expected);
                    ItemStack left = network.insert(out.copyWithCount(take));
                    int taken = take - left.getCount();
                    if (taken > 0) arrived(out.copyWithCount(taken));
                    if (taken > 0) {
                        ItemStack rest = out.copy();
                        rest.shrink(taken);
                        furnace.setItem(2, rest);
                        furnace.setChanged();
                        expected -= taken;
                        progress = true;
                    }
                }
                boolean nothingLeft = furnace.getItem(0).isEmpty() && furnace.getItem(2).isEmpty();
                if (expected <= 0 || nothingLeft) furnaces.remove(furnacePos);
                else furnaces.put(furnacePos, new FurnaceWait(wait.output(), expected));
            }
            return progress;
        }

        // ---------------------------------------------------------------- machines of other mods

        /// Machine: puts sets of ingredients in the machines of the recipe in the field (the machine chooses the
        /// slots; with Mekanism, through the sides set as inputs). One set at a time, or more with Solar Wind
        /// Upgrades. The results are taken out by collectMachines.
        private boolean workMachine(ServerLevel level, StarNetwork network, Step step) {
            MagnetarBlockEntity magnetar = StarNetwork.findMagnetar(level, pos);
            if (magnetar == null) return false;
            List<BlockPos> list = new ArrayList<>();
            StarNetwork.forEachBlockEntity(level, magnetar.getBlockPos(), magnetar.radius(), blockEntity -> {
                if (step.pattern().madeBy(blockEntity.getBlockState())) list.add(blockEntity.getBlockPos());
            });
            status = list.isEmpty() ? "screen.simpleores.quasar.job.waiting_machines" : "screen.simpleores.quasar.job.working";
            List<ItemStack> set = new ArrayList<>();
            for (int i = 0; i < step.items().size(); i++) {
                ItemStackTemplate item = step.items().get(i);
                if (item != null) set.add(item.create().copyWithCount(step.pattern().amount(i)));
            }
            int speed = magnetar.speed();
            boolean progress = false;
            for (BlockPos machinePos : list) {
                if (done >= step.times()) break;
                var handler = com.exoticmoss.simpleores.starstorage.network.StarAccess.items(level, machinePos);
                if (handler == null) continue;
                var sides = com.exoticmoss.simpleores.starstorage.network.StarAccess.itemSides(level, machinePos);
                long sets = Math.min(SETS[speed], step.times() - done);
                int given = 0;
                while (given < sets) {
                    // The whole set from the network, then all of it into the machine (else everything goes back)
                    List<ItemStack> got = new ArrayList<>();
                    boolean all = true;
                    for (ItemStack one : set) {
                        ItemStack taken = network.extract(one, one.getCount());
                        if (!taken.isEmpty()) got.add(taken);
                        if (taken.getCount() < one.getCount()) all = false;
                    }
                    if (!all) {
                        giveBack(network, got);
                        status = "screen.simpleores.quasar.job.waiting_items";
                        break;
                    }
                    if (!com.exoticmoss.simpleores.starstorage.network.StarAccess.insertAll(handler, sides, got)) {
                        giveBack(network, got);
                        break;
                    }
                    given++;
                }
                if (given <= 0) continue;
                done += given;
                machines.merge(machinePos, new FurnaceWait(step.pattern().output(), (long) given * step.pattern().count()),
                        (a, b) -> new FurnaceWait(a.output(), a.expected() + b.expected()));
                progress = true;
                if (status.equals("screen.simpleores.quasar.job.waiting_items")) break;
            }
            return progress;
        }

        /// Takes the results out of the machines used by this job to the network.
        private boolean collectMachines(ServerLevel level, StarNetwork network) {
            boolean progress = false;
            for (Map.Entry<BlockPos, FurnaceWait> entry : new ArrayList<>(machines.entrySet())) {
                BlockPos machinePos = entry.getKey();
                FurnaceWait wait = entry.getValue();
                var handler = com.exoticmoss.simpleores.starstorage.network.StarAccess.items(level, machinePos);
                if (handler == null) {
                    machines.remove(machinePos);
                    continue;
                }
                var sides = com.exoticmoss.simpleores.starstorage.network.StarAccess.itemSides(level, machinePos);
                ItemStack output = wait.output().create();
                int taken = com.exoticmoss.simpleores.starstorage.network.StarAccess.takeOut(network, handler, sides, output,
                        (int) Math.min(Integer.MAX_VALUE, wait.expected()));
                if (taken <= 0) continue;
                arrived(output.copyWithCount(taken));
                long expected = wait.expected() - taken;
                if (expected <= 0) machines.remove(machinePos);
                else machines.put(machinePos, new FurnaceWait(wait.output(), expected));
                progress = true;
            }
            return progress;
        }
    }

    private static void giveBack(StarNetwork network, List<ItemStack> items) {
        for (ItemStack item : items) {
            if (!item.isEmpty()) network.insert(item);
        }
    }
}
