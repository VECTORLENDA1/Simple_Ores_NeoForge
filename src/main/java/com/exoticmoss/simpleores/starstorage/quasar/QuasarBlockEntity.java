package com.exoticmoss.simpleores.starstorage.quasar;

import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.exoticmoss.simpleores.util.ValueInput;
import com.exoticmoss.simpleores.util.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import com.exoticmoss.simpleores.starstorage.autocraft.Autocraft;

/// The Quasar block entity: the recipes saved in it (see Pattern). Every Quasar inside the field of a Magnetar
/// gives its recipes to the network: the Stellar Terminal shows those items as craftable.
public class QuasarBlockEntity extends BlockEntity {
    /// How many recipes one Quasar can keep.
    public static final int MAX_PATTERNS = 10_000;

    private final List<Pattern> patterns = new ArrayList<>();
    /// The id of the recipes in the world (QuasarRecipes): they are always kept there too, and the item of the
    /// broken Quasar keeps the id. null = no recipes saved yet.
    private java.util.UUID recipesId;
    /// Placed with the item of a broken Quasar: its recipes come back on the next tick.
    private boolean restore;
    /// Auto-craft jobs with Gravity Core steps (they take time). Not saved: they stop if the world is closed.
    private final List<Autocraft.Job> jobs = new ArrayList<>();
    /// The items that were in the storage of the jobs when the world was saved (the jobs themselves are not saved):
    /// they go back to the network as soon as the Quasar works again.
    private final java.util.Map<com.exoticmoss.simpleores.util.ItemStackTemplate, Long> toReturn = new java.util.HashMap<>();

    /// An item and how many, as saved.
    private record Stored(com.exoticmoss.simpleores.util.ItemStackTemplate item, long count) {
        static final com.mojang.serialization.Codec<Stored> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(inst -> inst.group(
                com.exoticmoss.simpleores.util.ItemStackTemplate.CODEC.fieldOf("item").forGetter(Stored::item),
                com.mojang.serialization.Codec.LONG.fieldOf("count").forGetter(Stored::count)
        ).apply(inst, Stored::new));
    }

    public QuasarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUASAR_BE.get(), pos, state);
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    /// Saves a recipe (the same recipe again replaces the old one). false = the Quasar is full.
    public boolean addPattern(Pattern pattern) {
        // Only the same recipe is replaced: other recipes for the same item stay (the auto-craft picks one that works)
        patterns.removeIf(old -> old.equals(pattern));
        if (patterns.size() >= MAX_PATTERNS) return false;
        patterns.add(pattern);
        setChanged();
        backup();
        return true;
    }

    public void removePattern(int index) {
        if (index < 0 || index >= patterns.size()) return;
        patterns.remove(index);
        setChanged();
        backup();
    }

    /// The recipes are also kept by the world (QuasarRecipes).
    private void backup() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        if (recipesId == null) recipesId = java.util.UUID.randomUUID();
        QuasarRecipes.get(serverLevel).put(recipesId, patterns);
    }

    /// Placed with the item of a broken Quasar: its recipes come back (with a new id, so two Quasars never share one).
    private void restoreRecipes(net.minecraft.server.level.ServerLevel serverLevel) {
        restore = false;
        if (recipesId == null) return;
        QuasarRecipes store = QuasarRecipes.get(serverLevel);
        List<Pattern> saved = store.get(recipesId);
        if (saved.isEmpty()) return;
        for (Pattern pattern : saved) {
            if (!patterns.contains(pattern) && patterns.size() < MAX_PATTERNS) patterns.add(pattern);
        }
        store.remove(recipesId);
        recipesId = null;
        backup();
        setChanged();
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        QuasarRecipes.Id id = input.get(com.exoticmoss.simpleores.starstorage.ModDataComponents.QUASAR_RECIPES.get());
        if (id != null) {
            recipesId = id.id();
            restore = true;
        }
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (recipesId != null && !patterns.isEmpty()) {
            components.set(com.exoticmoss.simpleores.starstorage.ModDataComponents.QUASAR_RECIPES.get(), new QuasarRecipes.Id(recipesId, patterns.size()));
        }
    }

    @Override
    public void removeComponentsFromTag(net.minecraft.nbt.CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("recipes_id");
    }

    public void startJob(Autocraft.Job job) {
        jobs.add(job);
    }

    public List<Autocraft.Job> getJobs() {
        return jobs;
    }

    public void cancelJob(int id) {
        jobs.removeIf(job -> {
            if (job.id != id) return false;
            // What the job keeps goes back to the network
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) job.finish(serverLevel);
            return true;
        });
    }

    /// Server: works on the jobs (each one works every few ticks, faster with Solar Wind Upgrades;
    /// the finished and the stuck ones go away).
    public void serverTick() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        if (restore) restoreRecipes(serverLevel);
        // (the recipes saved before they were also kept by the world)
        if (recipesId == null && !patterns.isEmpty()) backup();
        if (!toReturn.isEmpty()) returnItems();
        if (jobs.isEmpty()) return;
        // (the storage of the jobs changes all the time: it is saved with the Quasar)
        setChanged();
        jobs.removeIf(job -> {
            if (job.tick(serverLevel) && !job.done()) return false;
            job.finish(serverLevel);
            return true;
        });
    }

    /// The items of the jobs of before the world was loaded go back to the network (what doesn't fit waits).
    private void returnItems() {
        var network = com.exoticmoss.simpleores.starstorage.network.StarNetwork.at(level, worldPosition);
        if (network.drives().isEmpty()) return;
        for (var entry : new ArrayList<>(toReturn.entrySet())) {
            long left = entry.getValue();
            while (left > 0) {
                int part = (int) Math.min(left, Integer.MAX_VALUE);
                net.minecraft.world.item.ItemStack rest = network.insert(entry.getKey().create().copyWithCount(part));
                left -= part - rest.getCount();
                if (!rest.isEmpty()) break;
            }
            if (left <= 0) toReturn.remove(entry.getKey());
            else toReturn.put(entry.getKey(), left);
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput output = new ValueOutput(tag, registries);
        java.util.Map<com.exoticmoss.simpleores.util.ItemStackTemplate, Long> stored = new java.util.HashMap<>(toReturn);
        for (Autocraft.Job job : jobs) job.stored().forEach((item, count) -> stored.merge(item, count, Long::sum));
        List<Stored> list = new ArrayList<>();
        stored.forEach((item, count) -> list.add(new Stored(item, count)));
        output.store("job_items", Stored.CODEC.listOf(), list);
        output.store("patterns", Pattern.CODEC.listOf(), patterns);
        if (recipesId != null) tag.putUUID("recipes_id", recipesId);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput input = new ValueInput(tag, registries);
        toReturn.clear();
        for (Stored stored : input.read("job_items", Stored.CODEC.listOf()).orElse(List.of())) toReturn.merge(stored.item(), stored.count(), Long::sum);
        patterns.clear();
        patterns.addAll(input.read("patterns", Pattern.CODEC.listOf()).orElse(List.of()));
        if (tag.hasUUID("recipes_id")) recipesId = tag.getUUID("recipes_id");
    }
}
