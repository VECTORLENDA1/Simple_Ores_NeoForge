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
import com.exoticmoss.simpleores.util.ItemStackTemplate;
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
///  2. run: first every item of the network the plan uses is taken out of the network into the job (like the
///     crafting CPUs of Applied Energistics 2): the job has its own storage, so nothing else can use those items and
///     nothing is counted twice. Then it crafts every step, taking the ingredients from its storage; the results of
///     the sub-crafts go to its storage too, and only what was asked for goes to the network. When the job ends (or
///     is cancelled), what is left in its storage goes back to the network.
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
    /// The extra items the planned crafts make (e.g. 9 when 8 are needed): used before the network.
    private final Map<ItemStackTemplate, Long> extras = new HashMap<>();
    /// The items of the network the plan uses (they go into the storage of the job when it starts).
    private final Map<ItemStackTemplate, Long> fromNetwork = new HashMap<>();
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
            Job job = new Job(craft.steps, level, pos, item, amount);
            if (!job.reserve(network, craft.fromNetwork)) return new Result(Map.of(), "screen.simpleores.terminal.craft.changed");
            quasar.startJob(job);
            return new Result(Map.of(), null);
        }
        Job job = new Job(craft.steps, level, pos, item, amount);
        if (!job.reserve(network, craft.fromNetwork)) return new Result(Map.of(), "screen.simpleores.terminal.craft.changed");
        while (!job.done()) {
            if (!job.work(level, network)) {
                job.finish(network);
                return new Result(Map.of(), "screen.simpleores.terminal.craft.changed");
            }
        }
        job.finish(network);
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
        // First the extra items of the planned crafts, then the network
        long extra = extras.getOrDefault(item, 0L);
        long fromExtra = Math.min(extra, amount);
        if (fromExtra > 0) extras.put(item, extra - fromExtra);
        amount -= fromExtra;
        if (amount <= 0) return true;
        long have = stock.getOrDefault(item, 0L);
        long used = Math.min(have, amount);
        if (used > 0) {
            stock.put(item, have - used);
            fromNetwork.merge(item, used, Long::sum);
        }
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
                Map<ItemStackTemplate, Long> extrasBefore = new HashMap<>(extras);
                Map<ItemStackTemplate, Long> fromNetworkBefore = new HashMap<>(fromNetwork);
                Map<ItemStackTemplate, Long> missingBefore = new LinkedHashMap<>(missing);
                int stepsBefore = steps.size();
                long craftsBefore = crafts;
                if (craft(option, item, amount, depth) && crafts <= MAX_CRAFTS) return true;
                stock.clear();
                stock.putAll(stockBefore);
                extras.clear();
                extras.putAll(extrasBefore);
                fromNetwork.clear();
                fromNetwork.putAll(fromNetworkBefore);
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
        if (extra > 0) extras.merge(item, extra, Long::sum);
        return ok;
    }

    /// The item to use for an ingredient: one the network has enough of, else one it has some of,
    /// else one that has a recipe, else the first one.
    private ItemStackTemplate choose(List<ItemStackTemplate> choices, long needed) {
        for (ItemStackTemplate choice : choices) {
            if (available(choice) >= needed) return choice;
        }
        for (ItemStackTemplate choice : choices) {
            if (available(choice) > 0) return choice;
        }
        for (ItemStackTemplate choice : choices) {
            if (!patternsFor(choice).isEmpty()) return choice;
        }
        return choices.get(0);
    }

    private long available(ItemStackTemplate item) {
        return stock.getOrDefault(item, 0L) + extras.getOrDefault(item, 0L);
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
        /// How many times each step was made (or given out, for the steps that take time).
        private final long[] doneSteps;
        /// How many items each step made (came back to the network).
        private final long[] madeSteps;
        /// The step being worked on now (inside work): how many times it was made, and up to how many times it
        /// can be made now.
        private long done;
        private long target;
        private int idle;
        /// What the job waits for now (a message key, for the jobs tab of the Quasar).
        private String status = "screen.simpleores.quasar.job.working";
        /// Furnace results still to be collected: furnace -> item and how many.
        private final Map<BlockPos, FurnaceWait> furnaces = new HashMap<>();
        /// The same for the machines of other mods (a machine can make more than one item of the job).
        private final Map<MachineKey, FurnaceWait> machines = new HashMap<>();

        private record MachineKey(BlockPos pos, ItemStackTemplate output) {
        }

        /// How many of each ingredient of a step were put in a machine.
        private record SentKey(BlockPos pos, int step) {
        }

        private final Map<SentKey, long[]> sentToMachines = new HashMap<>();
        /// At most this many of one ingredient are sent to one machine each time the job works.
        private static final int MAX_SEND = 1024;
        /// The index of the step being worked on now (inside work).
        private int stepNow;

        /// Every machine (and item) used by this job: what comes out of them is always taken, so nothing stays
        /// in their outputs.
        private final java.util.Set<MachineKey> usedMachines = new java.util.HashSet<>();

        private record FurnaceWait(ItemStackTemplate output, long expected) {
        }

        /// The storage of the job: the items it took from the network and the results of its sub-crafts.
        private final Map<ItemStackTemplate, Long> storage = new HashMap<>();
        /// How many of each ingredient each machine step sent (all the machines together): never more than the step needs.
        private final Map<Integer, long[]> stepSent = new HashMap<>();

        private static ItemStackTemplate key(ItemStack stack) {
            return ItemStackTemplate.fromNonEmptyStack(stack.copyWithCount(1));
        }

        /// Takes the items the plan uses out of the network into the storage of the job. false = the network
        /// doesn't have them any more (nothing is taken).
        boolean reserve(StarNetwork network, Map<ItemStackTemplate, Long> items) {
            List<ItemStack> taken = new ArrayList<>();
            for (Map.Entry<ItemStackTemplate, Long> entry : items.entrySet()) {
                long left = entry.getValue();
                while (left > 0) {
                    ItemStack got = network.extract(entry.getKey().create(), (int) Math.min(left, Integer.MAX_VALUE));
                    if (got.isEmpty()) break;
                    taken.add(got);
                    left -= got.getCount();
                }
                if (left > 0) {
                    for (ItemStack stack : taken) network.insert(stack);
                    return false;
                }
            }
            for (ItemStack stack : taken) store(stack);
            return true;
        }

        /// Up to "amount" of the item, from the storage of the job (EMPTY if it has none).
        private ItemStack take(ItemStack item, int amount) {
            ItemStackTemplate key = key(item);
            long have = storage.getOrDefault(key, 0L);
            int got = (int) Math.min(have, amount);
            if (got <= 0) return ItemStack.EMPTY;
            if (have - got <= 0) storage.remove(key);
            else storage.put(key, have - got);
            return item.copyWithCount(got);
        }

        private void store(ItemStack stack) {
            if (!stack.isEmpty()) storage.merge(key(stack), (long) stack.getCount(), Long::sum);
        }

        private void giveBack(List<ItemStack> items) {
            for (ItemStack item : items) store(item);
        }

        /// How many of the item the storage of the job has.
        private long stored(ItemStackTemplate item) {
            return storage.getOrDefault(key(item.create()), 0L);
        }

        /// Results made for this job: what was asked for goes to the network (and counts), the results of the
        /// sub-crafts stay in the storage of the job.
        private void receive(StarNetwork network, ItemStack stack) {
            if (stack.isEmpty()) return;
            madeBySteps(stack);
            if (ItemStack.isSameItemSameComponents(stack, output.create())) {
                countFinal(stack.getCount());
                ItemStack left = network.insert(stack.copy());
                store(left);
            } else {
                store(stack);
            }
        }

        private void countFinal(long count) {
            if (made >= amount) return;
            made += Math.min(count, amount - made);
            arrivedNow = true;
        }

        /// What the storage of the job has now (saved with the Quasar: the jobs are not saved, so when the world
        /// is loaded again these items go back to the network).
        public Map<ItemStackTemplate, Long> stored() {
            return storage;
        }

        /// The job ended (done or cancelled): what is left in its storage goes back to the network.
        public void finish(StarNetwork network) {
            for (Map.Entry<ItemStackTemplate, Long> entry : storage.entrySet()) {
                long left = entry.getValue();
                while (left > 0) {
                    int part = (int) Math.min(left, Integer.MAX_VALUE);
                    ItemStack rest = network.insert(entry.getKey().create().copyWithCount(part));
                    left -= part;
                    if (!rest.isEmpty()) break;
                }
            }
            storage.clear();
        }

        /// The same, from the Quasar (it finds the network of the job).
        public void finish(ServerLevel quasarLevel) {
            ServerLevel level = quasarLevel.getServer().getLevel(dimension);
            if (level != null) finish(StarNetwork.at(level, pos));
        }

        private Job(List<Step> steps, ServerLevel level, BlockPos pos, ItemStackTemplate output, long amount) {
            this.steps = steps;
            this.doneSteps = new long[steps.size()];
            this.madeSteps = new long[steps.size()];
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
        /// Items that came back to the network: they count for the sub-crafts that make that item (their bar only
        /// grows with what is really made, not with what was sent to the machines).
        private void madeBySteps(ItemStack stack) {
            long left = stack.getCount();
            for (int i = 0; i < steps.size() - 1 && left > 0; i++) {
                Step step = steps.get(i);
                if (!ItemStack.isSameItemSameComponents(stack, step.pattern().output().create())) continue;
                long room = step.times() * step.pattern().count() - madeSteps[i];
                long counted = Math.min(left, room);
                if (counted <= 0) continue;
                madeSteps[i] += counted;
                left -= counted;
            }
        }

        public List<SubCraft> subCrafts() {
            Map<ItemStackTemplate, SubCraft> merged = new LinkedHashMap<>();
            for (int i = 0; i < steps.size() - 1; i++) {
                Step step = steps.get(i);
                long count = step.pattern().count();
                long total = step.times() * count;
                SubCraft one = new SubCraft(step.pattern().output(), total, Math.min(madeSteps[i], total),
                        doneSteps[i] > 0 && madeSteps[i] < total);
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

        /// A Gravity Core finished items for the network of "networkPos": the results of the sub-crafts a job
        /// waits for go to the storage of that job, what was asked for counts for its job. Returns what goes to the
        /// network.
        public static ItemStack deliver(ServerLevel level, BlockPos networkPos, ItemStack stack) {
            long left = stack.getCount();
            for (QuasarBlockEntity quasar : StarNetwork.at(level, networkPos).quasars()) {
                for (Job job : quasar.getJobs()) {
                    if (left <= 0) return ItemStack.EMPTY;
                    if (job.dimension != level.dimension() || !job.pos.equals(networkPos)) continue;
                    if (ItemStack.isSameItemSameComponents(stack, job.output.create())) {
                        job.countFinal(left);
                        continue;
                    }
                    long room = job.coreRoom(stack);
                    long taken = Math.min(left, room);
                    if (taken <= 0) continue;
                    job.madeBySteps(stack.copyWithCount((int) taken));
                    job.store(stack.copyWithCount((int) taken));
                    left -= taken;
                }
            }
            return stack.copyWithCount((int) left);
        }

        /// True if a job of the network takes this item out of the machine at "machinePos" itself (a Binary Star
        /// leaves it there, so it goes to the storage of the job and counts).
        public static boolean waitsFor(ServerLevel level, StarNetwork network, BlockPos machinePos, ItemStack stack) {
            for (QuasarBlockEntity quasar : network.quasars()) {
                for (Job job : quasar.getJobs()) {
                    if (job.dimension != level.dimension()) continue;
                    for (MachineKey key : job.usedMachines) {
                        if (key.pos().equals(machinePos) && ItemStack.isSameItemSameComponents(stack, key.output().create())) return true;
                    }
                }
            }
            return false;
        }

        /// How many of the item the Gravity Core steps of this job still have to make.
        private long coreRoom(ItemStack stack) {
            long room = 0;
            for (int i = 0; i < steps.size() - 1; i++) {
                Step step = steps.get(i);
                if (!step.pattern().isCore() || !ItemStack.isSameItemSameComponents(stack, step.pattern().output().create())) continue;
                room += step.times() * step.pattern().count() - madeSteps[i];
            }
            return Math.max(0, room);
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
            // Every step that is not done yet works as much as it can now (it doesn't wait for the steps before it:
            // what has its ingredients is made). The steps are in order (the sub-crafts first), so what one makes
            // can be used by the next ones in the same work.
            String firstStatus = null;
            index = steps.size();
            for (int i = 0; i < steps.size(); i++) {
                Step step = steps.get(i);
                // The last step stops when enough came back (some recipes give more than planned)
                if (i == steps.size() - 1 && made >= amount) doneSteps[i] = step.times();
                if (doneSteps[i] >= step.times()) continue;
                // A sub-craft only makes what is still missing for the steps that use it (what the storage of the job
                // has and what is still in the machines and furnaces count), so nothing more than needed is sent
                target = step.times();
                if (i < steps.size() - 1 && !step.pattern().isCore()) {
                    long demand = demand(i);
                    if (demand <= 0) {
                        // Nothing uses it any more: this step is done
                        doneSteps[i] = step.times();
                        madeSteps[i] = Math.max(madeSteps[i], step.times() * step.pattern().count());
                        continue;
                    }
                    ItemStackTemplate output = step.pattern().output();
                    long deficit = demand - stored(output) - inFlight(output);
                    if (deficit <= 0) {
                        index = Math.min(index, i);
                        continue;
                    }
                    long count = Math.max(1, step.pattern().count());
                    target = Math.min(step.times(), doneSteps[i] + (deficit + count - 1) / count);
                }
                done = doneSteps[i];
                stepNow = i;
                status = "screen.simpleores.quasar.job.working";
                boolean stepProgress;
                if (step.pattern().isCore()) stepProgress = workCore(level, network, step);
                else if (step.pattern().furnace()) stepProgress = workFurnace(level, network, step);
                else if (step.pattern().isMachine()) stepProgress = workMachine(level, network, step);
                else stepProgress = workCrafting(level, network, step);
                doneSteps[i] = done;
                progress |= stepProgress;
                if (done < step.times()) {
                    index = Math.min(index, i);
                    if (firstStatus == null && !stepProgress) firstStatus = status;
                    if (stepProgress) firstStatus = "screen.simpleores.quasar.job.working";
                }
            }
            if (index < steps.size()) {
                if (firstStatus != null) status = firstStatus;
                return progress;
            }
            coresWorking = index >= steps.size() && made < amount && coresWorkingFor(level);
            if (!furnaces.isEmpty()) status = "screen.simpleores.quasar.job.waiting_furnaces";
            else if (coresWorking || !machines.isEmpty()) status = "screen.simpleores.quasar.job.waiting_results";
            return progress || furnaces.isEmpty() && machines.isEmpty() && !coresWorking;
        }

        /// How many of the item made by step "index" the steps after it still need (for the crafts not made yet).
        private long demand(int index) {
            ItemStack output = steps.get(index).pattern().output().create();
            long demand = 0;
            for (int j = index + 1; j < steps.size(); j++) {
                Step step = steps.get(j);
                long left = step.times() - doneSteps[j];
                if (left <= 0) continue;
                for (int k = 0; k < step.items().size(); k++) {
                    ItemStackTemplate item = step.items().get(k);
                    if (item != null && ItemStack.isSameItemSameComponents(item.create(), output)) demand += left * step.pattern().amount(k);
                }
            }
            return demand;
        }

        /// How many of the item are still to come out of the machines and furnaces used by this job.
        private long inFlight(ItemStackTemplate output) {
            ItemStack stack = output.create();
            long count = 0;
            for (FurnaceWait wait : machines.values()) {
                if (ItemStack.isSameItemSameComponents(wait.output().create(), stack)) count += wait.expected();
            }
            for (FurnaceWait wait : furnaces.values()) {
                if (ItemStack.isSameItemSameComponents(wait.output().create(), stack)) count += wait.expected();
            }
            return count;
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
            while (done < target && count++ < CRAFTS_PER_WORK) {
                List<ItemStack> grid = new ArrayList<>();
                for (ItemStackTemplate item : step.items()) {
                    if (item == null) {
                        grid.add(ItemStack.EMPTY);
                        continue;
                    }
                    ItemStack got = take(item.create(), 1);
                    if (got.isEmpty()) {
                        giveBack(grid);
                        status = "screen.simpleores.quasar.job.waiting_items";
                        return progress;
                    }
                    grid.add(got);
                }
                while (grid.size() < Pattern.SIZE) grid.add(ItemStack.EMPTY);
                CraftingInput input = CraftingInput.of(3, 3, grid);
                // The same recipe as the last craft (much faster than looking for it again)
                Optional<RecipeHolder<CraftingRecipe>> recipe = last != null && last.value().matches(input, level)
                        ? Optional.of(last) : level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
                if (recipe.isEmpty()) {
                    giveBack(grid);
                    return progress;
                }
                last = recipe.get();
                ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
                NonNullList<ItemStack> remaining = recipe.get().value().getRemainingItems(input);
                receive(network, result);
                for (ItemStack rest : remaining) store(rest);
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
            Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(step.pattern().coreRecipe().get());
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
                if (done >= target) break;
                // One set, with the amounts of the tier of this core (bigger cores can need other amounts)
                List<ItemStack> set = new ArrayList<>();
                for (int i = 0; i < step.items().size(); i++) {
                    ItemStackTemplate item = step.items().get(i);
                    if (item == null) continue;
                    int amount = i < parts.size() ? parts.get(i).countFor(core.getTier().level) : step.pattern().amount(i);
                    set.add(item.create().copyWithCount(amount));
                }
                int sets = core.setsThatFit(set, (int) Math.min(SETS[speed], target - done));
                if (sets <= 0) continue;
                // Take them from the storage of the job (only whole sets go to the core)
                List<ItemStack> got = new ArrayList<>();
                for (ItemStack one : set) got.add(take(one, one.getCount() * sets));
                int whole = sets;
                for (int i = 0; i < set.size(); i++) whole = Math.min(whole, got.get(i).getCount() / set.get(i).getCount());
                List<ItemStack> give = new ArrayList<>();
                for (int i = 0; i < set.size(); i++) {
                    ItemStack stack = got.get(i);
                    int keep = whole * set.get(i).getCount();
                    if (stack.getCount() > keep) {
                        store(stack.copyWithCount(stack.getCount() - keep));
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
                giveBack(core.insertFromNetwork(stacks, recipe, pos));
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
            List<Container> list = new ArrayList<>();
            StarNetwork.forEachBlockEntity(level, magnetar.getBlockPos(), magnetar.radius(), blockEntity -> {
                if (isFurnace(blockEntity)) list.add((Container) blockEntity);
            });
            status = list.isEmpty() ? "screen.simpleores.quasar.job.waiting_furnaces" : "screen.simpleores.quasar.job.working";
            boolean progress = false;
            ItemStack item = input.create();
            for (Container furnace : list) {
                if (done >= target) break;
                ItemStack current = furnace.getItem(0);
                if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, item)) continue;
                int room = item.getMaxStackSize() - current.getCount();
                int send = (int) Math.min(room, target - done);
                if (send <= 0) continue;
                ItemStack got = take(item, send);
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
                furnaces.merge(((net.minecraft.world.level.block.entity.BlockEntity) furnace).getBlockPos(), new FurnaceWait(step.pattern().output(), expected),
                        (a, b) -> new FurnaceWait(a.output(), a.expected() + b.expected()));
                progress = true;
            }
            return progress;
        }

        /// A furnace the auto-craft can use: the furnace of Minecraft, and the furnaces of Iron Furnaces (iron, gold,
        /// diamond, rainbow, crystal...) while they work as a furnace (not as a generator or a factory). Both have the
        /// same slots: 0 = input, 1 = fuel, 2 = result.
        private static boolean isFurnace(net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
            if (blockEntity instanceof FurnaceBlockEntity) return true;
            if (!(blockEntity instanceof Container container) || container.getContainerSize() < 3) return false;
            for (Class<?> type = blockEntity.getClass(); type != null; type = type.getSuperclass()) {
                if (!type.getName().equals("ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase")) continue;
                try {
                    return !(type.getMethod("isFurnace").invoke(blockEntity) instanceof Boolean furnace) || furnace;
                } catch (ReflectiveOperationException | RuntimeException e) {
                    return true;
                }
            }
            return false;
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
                    int taken = (int) Math.min(out.getCount(), expected);
                    if (taken > 0) receive(network, out.copyWithCount(taken));
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
        /// slots; with Mekanism, through the sides set as inputs). The crafts are split evenly between the machines
        /// (each one never gets more than its part). The results are taken out by collectMachines.
        private boolean workMachine(ServerLevel level, StarNetwork network, Step step) {
            MagnetarBlockEntity magnetar = StarNetwork.findMagnetar(level, pos);
            if (magnetar == null) return false;
            List<BlockPos> list = new ArrayList<>();
            // The recipes linked to machines in the Binary Stars of the field
            Map<BlockPos, List<Pattern>> links = new HashMap<>();
            StarNetwork.forEachBlockEntity(level, magnetar.getBlockPos(), magnetar.radius(), blockEntity -> {
                if (step.pattern().madeBy(blockEntity.getBlockState())) list.add(blockEntity.getBlockPos());
                if (blockEntity instanceof com.exoticmoss.simpleores.starstorage.binary.BinaryStarBlockEntity binary) {
                    binary.links().forEach((machine, recipes) -> links.computeIfAbsent(machine, p -> new ArrayList<>()).addAll(recipes));
                }
            });
            // A machine with linked recipes makes only those; a recipe linked to machines goes only to those
            boolean linkedRecipe = links.values().stream().anyMatch(recipes -> recipes.contains(step.pattern()));
            list.removeIf(machine -> links.containsKey(machine) ? !links.get(machine).contains(step.pattern()) : linkedRecipe);
            status = list.isEmpty() ? "screen.simpleores.quasar.job.waiting_machines" : "screen.simpleores.quasar.job.working";
            // The ingredients of one craft and how many of each
            List<ItemStack> items = new ArrayList<>();
            List<Integer> amounts = new ArrayList<>();
            for (int i = 0; i < step.items().size(); i++) {
                ItemStackTemplate item = step.items().get(i);
                if (item == null) continue;
                items.add(item.create());
                amounts.add(Math.max(1, step.pattern().amount(i)));
            }
            if (items.isEmpty()) return false;
            int speed = magnetar.speed();
            boolean progress = false;
            // The crafts of the step are split between the machines: each one gets its part (the first ones one
            // more when they don't split evenly), with every ingredient in its right amount
            list.sort(java.util.Comparator.naturalOrder());
            int machineCount = list.size();
            for (int m = 0; m < machineCount; m++) {
                BlockPos machinePos = list.get(m);
                if (done >= target) break;
                long share = step.times() / machineCount + (m < step.times() % machineCount ? 1 : 0);
                // An altar with pedestals (Mystical Agriculture, Draconic Evolution): one craft at a time, see Altars
                Altars.Altar altar = Altars.of(level, machinePos);
                if (altar != null) {
                    if (workAltar(level, altar, machinePos, step, items, amounts, share)) progress = true;
                    continue;
                }
                var handler = com.exoticmoss.simpleores.starstorage.network.StarAccess.items(level, machinePos);
                if (handler == null) continue;
                var sides = com.exoticmoss.simpleores.starstorage.network.StarAccess.itemSides(level, machinePos);
                // Each ingredient goes in by itself, as much as fits (one doesn't wait for room for the others):
                // the crafts given to a machine are the whole sets it got
                long[] sent = sentToMachines.computeIfAbsent(new SentKey(machinePos, stepNow), key -> new long[items.size()]);
                long[] total = stepSent.computeIfAbsent(stepNow, key -> new long[items.size()]);
                long setsBefore = sets(sent, amounts);
                long goal = Math.min(share, setsBefore + Math.min(SETS[speed], target - done));
                for (int k = 0; k < items.size(); k++) {
                    // (all the machines together never get more of an ingredient than the step needs)
                    long want = Math.min(Math.min(goal * amounts.get(k) - sent[k], MAX_SEND),
                            step.times() * amounts.get(k) - total[k]);
                    if (want <= 0) continue;
                    ItemStack got = take(items.get(k), (int) want);
                    if (got.isEmpty()) {
                        status = "screen.simpleores.quasar.job.waiting_items";
                        continue;
                    }
                    int put = com.exoticmoss.simpleores.starstorage.network.StarAccess.insertSome(handler, sides, got);
                    if (put < got.getCount()) store(got.copyWithCount(got.getCount() - put));
                    if (put > 0) {
                        sent[k] += put;
                        total[k] += put;
                        progress = true;
                    }
                }
                long given = sets(sent, amounts) - setsBefore;
                if (given <= 0) continue;
                done += given;
                usedMachines.add(new MachineKey(machinePos, step.pattern().output()));
                machines.merge(new MachineKey(machinePos, step.pattern().output()),
                        new FurnaceWait(step.pattern().output(), given * step.pattern().count()),
                        (a, b) -> new FurnaceWait(a.output(), a.expected() + b.expected()));
            }
            return progress;
        }

        /// Altar: when it is free, the first ingredient goes in its middle and the others one on each pedestal, then
        /// it is started. What a craft leaves on the pedestals (like empty buckets) goes back to the job.
        /// The result is taken out of the altar by collectMachines.
        private boolean workAltar(ServerLevel level, Altars.Altar altar, BlockPos machinePos, Step step,
                                  List<ItemStack> items, List<Integer> amounts, long share) {
            long[] sent = sentToMachines.computeIfAbsent(new SentKey(machinePos, stepNow), key -> new long[items.size()]);
            long[] total = stepSent.computeIfAbsent(stepNow, key -> new long[items.size()]);
            if (altar.busy()) return false;
            List<net.neoforged.neoforge.items.IItemHandler> free = new ArrayList<>();
            for (BlockPos at : altar.pedestals()) {
                var pedestal = Altars.pedestal(level, at);
                if (pedestal == null || pedestal.getSlots() < 1) continue;
                store(pedestal.extractItem(0, 64, false));
                if (pedestal.getStackInSlot(0).isEmpty()) free.add(pedestal);
            }
            if (done >= target || sets(sent, amounts) >= share) return false;
            int onPedestals = -1;
            for (int amount : amounts) onPedestals += amount;
            if (free.size() < onPedestals) {
                status = "screen.simpleores.quasar.job.waiting_machines";
                return false;
            }
            // The items of one craft (all of them, or nothing)
            List<ItemStack> got = new ArrayList<>();
            for (int k = 0; k < items.size(); k++) {
                ItemStack part = take(items.get(k), amounts.get(k));
                if (part.getCount() < amounts.get(k)) {
                    store(part);
                    giveBack(got);
                    status = "screen.simpleores.quasar.job.waiting_items";
                    return false;
                }
                got.add(part);
            }
            ItemStack left = altar.center().insertItem(0, got.get(0).split(1), false);
            if (!left.isEmpty()) {
                store(left);
                giveBack(got);
                return false;
            }
            int next = 0;
            for (ItemStack stack : got) {
                while (!stack.isEmpty() && next < free.size()) store(free.get(next++).insertItem(0, stack.split(1), false));
                store(stack);
            }
            altar.start().run();
            for (int k = 0; k < items.size(); k++) {
                sent[k] += amounts.get(k);
                total[k] += amounts.get(k);
            }
            done++;
            usedMachines.add(new MachineKey(machinePos, step.pattern().output()));
            machines.merge(new MachineKey(machinePos, step.pattern().output()),
                    new FurnaceWait(step.pattern().output(), step.pattern().count()),
                    (a, b) -> new FurnaceWait(a.output(), a.expected() + b.expected()));
            return true;
        }

        /// The whole sets in what was sent to a machine.
        private static long sets(long[] sent, List<Integer> amounts) {
            long sets = Long.MAX_VALUE;
            for (int k = 0; k < sent.length; k++) sets = Math.min(sets, sent[k] / amounts.get(k));
            return sets;
        }

        /// Takes the results out of the machines used by this job (to its storage, or to the network if it is what
        /// was asked for): all of that item that is in the machine (also when the recipe gives more than it said).
        private boolean collectMachines(ServerLevel level, StarNetwork network) {
            boolean progress = false;
            for (MachineKey key : new ArrayList<>(usedMachines)) {
                var handler = com.exoticmoss.simpleores.starstorage.network.StarAccess.items(level, key.pos());
                if (handler == null) {
                    usedMachines.remove(key);
                    machines.remove(key);
                    continue;
                }
                var sides = com.exoticmoss.simpleores.starstorage.network.StarAccess.itemSides(level, key.pos());
                ItemStack output = key.output().create();
                int taken = com.exoticmoss.simpleores.starstorage.network.StarAccess.extractAll(handler, sides, output);
                if (taken <= 0) continue;
                receive(network, output.copyWithCount(taken));
                FurnaceWait wait = machines.get(key);
                if (wait != null) {
                    long expected = wait.expected() - taken;
                    if (expected <= 0) machines.remove(key);
                    else machines.put(key, new FurnaceWait(wait.output(), expected));
                }
                progress = true;
            }
            return progress;
        }
    }
}
