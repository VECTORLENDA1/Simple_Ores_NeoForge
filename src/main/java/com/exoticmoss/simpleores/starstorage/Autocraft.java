package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.gravitycraft.GravityCoreBlockEntity;
import com.exoticmoss.simpleores.gravitycraft.GravityCoreRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
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

/// The auto-craft of the Quasar.
///  1. plan: works out everything that is needed, also the sub-recipes (planks -> sticks -> tool), using what
///     the network has; if something is missing, it says what and how many (nothing is crafted);
///  2. run: crafts every step in order, taking the ingredients from the network and putting the results back.
///     Crafting table steps are done at once. Gravity Core steps take time: the ingredients go to the free
///     Gravity Cores of the field of the Magnetar, and their results come back to the network by themselves.
///     So a plan with Gravity Core steps becomes a Job, worked on by a Quasar of the network every half second
///     (each step waits until its ingredients are in the network).
public final class Autocraft {
    /// Sub-recipes can go this deep at most (stops recipes that need themselves).
    private static final int MAX_DEPTH = 16;
    /// At most this many crafts in one request (so a huge request can't freeze the server).
    public static final int MAX_CRAFTS = 20_000;
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

    private Autocraft(StarNetwork network, boolean cores) {
        for (Map.Entry<ItemStackTemplate, Long> entry : network.contents()) stock.put(entry.getKey(), entry.getValue());
        this.patterns = network.patterns().stream().filter(pattern -> cores || !pattern.isCore()).toList();
    }

    /// Plans and, if nothing is missing, crafts "amount" of "item" into the network.
    /// "cores" = the Gravity Core recipes can be used (Stellar Terminal level 2).
    public static Result request(ServerLevel level, StarNetwork network, BlockPos pos, ItemStackTemplate item, long amount, boolean cores) {
        Autocraft craft = new Autocraft(network, cores);
        Pattern pattern = craft.patternFor(item);
        if (pattern == null) return new Result(Map.of(), "screen.simpleores.terminal.craft.no_recipe");
        // The items asked for are always crafted (even if the network already has some)
        craft.craft(pattern, item, amount, 0);
        if (craft.crafts > MAX_CRAFTS) return new Result(Map.of(), "screen.simpleores.terminal.craft.too_big");
        if (!craft.missing.isEmpty()) return new Result(craft.missing, null);
        if (craft.steps.stream().anyMatch(step -> step.pattern().isCore())) {
            if (network.quasars().isEmpty()) return new Result(Map.of(), "screen.simpleores.terminal.craft.no_recipe");
            network.quasars().get(0).startJob(new Job(craft.steps, pos));
            return new Result(Map.of(), null);
        }
        Job job = new Job(craft.steps, pos);
        while (!job.done()) {
            if (!job.work(level, network)) return new Result(Map.of(), "screen.simpleores.terminal.craft.changed");
        }
        return new Result(Map.of(), null);
    }

    private Pattern patternFor(ItemStackTemplate item) {
        ItemStack stack = item.create();
        for (Pattern pattern : patterns) {
            if (pattern.makes(stack)) return pattern;
        }
        return null;
    }

    // ---------------------------------------------------------------- plan

    /// Uses "amount" of "item" from the network, crafting what is not there. false = something is missing.
    private boolean need(ItemStackTemplate item, long amount, int depth) {
        long have = stock.getOrDefault(item, 0L);
        long used = Math.min(have, amount);
        if (used > 0) stock.put(item, have - used);
        amount -= used;
        if (amount <= 0) return true;
        Pattern pattern = patternFor(item);
        if (pattern == null || depth >= MAX_DEPTH) {
            missing.merge(item, amount, Long::sum);
            return false;
        }
        return craft(pattern, item, amount, depth);
    }

    /// Plans crafting at least "amount" of "item" with "pattern" (the extra goes to the stock).
    private boolean craft(Pattern pattern, ItemStackTemplate item, long amount, int depth) {
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
            if (patternFor(choice) != null) return choice;
        }
        return choices.get(0);
    }

    // ---------------------------------------------------------------- run

    /// The steps of a plan being crafted. "pos" is a position inside the field of the Magnetar (the terminal).
    public static class Job {
        private final List<Step> steps;
        private final BlockPos pos;
        private int index;
        private long done;
        private int idle;

        private Job(List<Step> steps, BlockPos pos) {
            this.steps = steps;
            this.pos = pos.immutable();
        }

        public boolean done() {
            return index >= steps.size();
        }

        /// Quasar: works on the job (every half second). false = the job is cancelled (no progress for too long).
        public boolean tick(ServerLevel level) {
            StarNetwork network = StarNetwork.at(level, pos);
            boolean progress = work(level, network);
            idle = progress ? 0 : idle + GravityCoreTicks.HALF_SECOND;
            return idle < GIVE_UP_TICKS;
        }

        /// Does as much as it can now. Returns true if something was done.
        boolean work(ServerLevel level, StarNetwork network) {
            boolean progress = false;
            while (!done()) {
                Step step = steps.get(index);
                boolean stepProgress = step.pattern().isCore() ? workCore(level, network, step) : workCrafting(level, network, step);
                progress |= stepProgress;
                if (done < step.times()) return progress;
                index++;
                done = 0;
            }
            return true;
        }

        /// Crafting table: crafts as many times as possible now.
        private boolean workCrafting(ServerLevel level, StarNetwork network, Step step) {
            boolean progress = false;
            while (done < step.times()) {
                List<ItemStack> grid = new ArrayList<>();
                for (ItemStackTemplate item : step.items()) {
                    if (item == null) {
                        grid.add(ItemStack.EMPTY);
                        continue;
                    }
                    ItemStack got = network.extract(item.create(), 1);
                    if (got.isEmpty()) {
                        giveBack(network, grid);
                        return progress;
                    }
                    grid.add(got);
                }
                while (grid.size() < Pattern.SIZE) grid.add(ItemStack.EMPTY);
                CraftingInput input = CraftingInput.of(3, 3, grid);
                Optional<RecipeHolder<CraftingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
                if (recipe.isEmpty()) {
                    giveBack(network, grid);
                    return progress;
                }
                ItemStack result = recipe.get().value().assemble(input);
                NonNullList<ItemStack> remaining = recipe.get().value().getRemainingItems(input);
                network.insert(result);
                for (ItemStack rest : remaining) {
                    if (!rest.isEmpty()) network.insert(rest);
                }
                done++;
                progress = true;
            }
            return progress;
        }

        /// Gravity Core: gives one set of ingredients to each free core (of a high enough tier).
        /// The step is done when every set was given (the results come back to the network by themselves).
        @SuppressWarnings("unchecked")
        private boolean workCore(ServerLevel level, StarNetwork network, Step step) {
            Optional<RecipeHolder<?>> holder = level.recipeAccess().byKey(step.pattern().coreRecipe().get());
            if (holder.isEmpty() || !(holder.get().value() instanceof GravityCoreRecipe)) return false;
            RecipeHolder<GravityCoreRecipe> recipe = (RecipeHolder<GravityCoreRecipe>) (RecipeHolder<?>) holder.get();
            MagnetarBlockEntity magnetar = StarNetwork.findMagnetar(level, pos);
            if (magnetar == null) return false;
            List<GravityCoreBlockEntity> cores = new ArrayList<>();
            StarNetwork.forEachBlockEntity(level, magnetar.getBlockPos(), StarNetwork.FIELD_RADIUS, blockEntity -> {
                if (blockEntity instanceof GravityCoreBlockEntity core && core.isFree() && core.getTier().level >= step.pattern().coreTier()) {
                    cores.add(core);
                }
            });
            boolean progress = false;
            for (GravityCoreBlockEntity core : cores) {
                if (done >= step.times()) break;
                List<ItemStack> set = new ArrayList<>();
                boolean complete = true;
                for (int i = 0; i < step.items().size(); i++) {
                    ItemStackTemplate item = step.items().get(i);
                    if (item == null) continue;
                    int amount = step.pattern().amount(i);
                    ItemStack got = network.extract(item.create(), amount);
                    if (!got.isEmpty()) set.add(got);
                    if (got.getCount() < amount) {
                        complete = false;
                        break;
                    }
                }
                if (!complete) {
                    giveBack(network, set);
                    return progress;
                }
                giveBack(network, core.insertFromNetwork(set, recipe, pos));
                done++;
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

    /// How often a Quasar works on its jobs (ticks).
    public static final class GravityCoreTicks {
        public static final int HALF_SECOND = 10;

        private GravityCoreTicks() {
        }
    }
}
