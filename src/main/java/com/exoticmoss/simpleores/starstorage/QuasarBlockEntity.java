package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/// The Quasar block entity: the recipes saved in it (see Pattern). Every Quasar inside the field of a Magnetar
/// gives its recipes to the network: the Stellar Terminal shows those items as craftable.
public class QuasarBlockEntity extends BlockEntity {
    /// How many recipes one Quasar can keep.
    public static final int MAX_PATTERNS = 54;

    private final List<Pattern> patterns = new ArrayList<>();
    /// Auto-craft jobs with Gravity Core steps (they take time). Not saved: they stop if the world is closed.
    private final List<Autocraft.Job> jobs = new ArrayList<>();

    public QuasarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUASAR_BE.get(), pos, state);
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    /// Saves a recipe. A recipe for the same item replaces the old one. false = the Quasar is full.
    public boolean addPattern(Pattern pattern) {
        patterns.removeIf(old -> old.makes(pattern.output().create()));
        if (patterns.size() >= MAX_PATTERNS) return false;
        patterns.add(pattern);
        setChanged();
        return true;
    }

    public void removePattern(int index) {
        if (index < 0 || index >= patterns.size()) return;
        patterns.remove(index);
        setChanged();
    }

    public void startJob(Autocraft.Job job) {
        jobs.add(job);
    }

    public List<Autocraft.Job> getJobs() {
        return jobs;
    }

    public void cancelJob(int id) {
        jobs.removeIf(job -> job.id == id);
    }

    /// Server: works on the jobs (each one works every few ticks, faster with Solar Wind Upgrades;
    /// the finished and the stuck ones go away).
    public void serverTick() {
        if (jobs.isEmpty() || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        jobs.removeIf(job -> !job.tick(serverLevel) || job.done());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("patterns", Pattern.CODEC.listOf(), patterns);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        patterns.clear();
        patterns.addAll(input.read("patterns", Pattern.CODEC.listOf()).orElse(List.of()));
    }
}
