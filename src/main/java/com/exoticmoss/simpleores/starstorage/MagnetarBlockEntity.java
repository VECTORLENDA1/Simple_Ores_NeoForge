package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import com.exoticmoss.simpleores.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/// The Magnetar Core block entity. It keeps no items of the network (they are in the cells), only its upgrades
/// (put in its screen, see MagnetarMenu):
///  - SPEED_SLOT: Solar Wind Upgrades (up to MAX_UPGRADES): the auto-crafts give items to the Gravity Cores
///    more often and more at a time;
///  - RANGE_SLOT: Magnetic Field Upgrades (up to MAX_UPGRADES): each one makes the field RANGE_PER_UPGRADE blocks bigger;
///  - LINK_SLOTS: Wormhole Links: the drives and Quasars of the Magnetars they point to (any distance, any
///    dimension) are part of this network too. While it has links, the Magnetar keeps the chunks of its field
///    and of the fields of those Magnetars loaded (see StarChunks).
/// It also keeps the experience of the auto-crafts of the Gravity Cores (taken in the Stellar Terminal).
public class MagnetarBlockEntity extends BlockEntity {
    public static final int SPEED_SLOT = 0;
    public static final int RANGE_SLOT = 1;
    public static final int LINK_START = 2;
    public static final int LINK_SLOTS = 4;
    public static final int SLOTS = LINK_START + LINK_SLOTS;
    public static final int MAX_UPGRADES = 4;
    public static final int RANGE_PER_UPGRADE = 16;
    /// The biggest field a Magnetar can have.
    public static final int MAX_RADIUS = StarNetwork.FIELD_RADIUS + MAX_UPGRADES * RANGE_PER_UPGRADE;

    /// Every Magnetar loaded (server and client), so the one of a position is found fast.
    private static final Set<MagnetarBlockEntity> LOADED = ConcurrentHashMap.newKeySet();

    private final SimpleContainer upgrades = new SimpleContainer(SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            MagnetarBlockEntity.this.setChanged();
        }
    };

    /// Experience of the auto-crafts (the cooking in the Gravity Cores).
    private long experience;
    /// The chunks kept loaded (pos = chunk x, 0, chunk z).
    private final Set<GlobalPos> forced = new HashSet<>();
    private int ticks;

    public MagnetarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGNETAR_BE.get(), pos, state);
    }

    public static Set<MagnetarBlockEntity> loaded() {
        return LOADED;
    }

    public SimpleContainer getUpgrades() {
        return upgrades;
    }

    /// Can this item go in this upgrade slot?
    public static boolean fits(int slot, ItemStack stack) {
        if (slot == SPEED_SLOT) return stack.is(ModItems.SOLAR_WIND_UPGRADE.get());
        if (slot == RANGE_SLOT) return stack.is(ModItems.MAGNETIC_FIELD_UPGRADE.get());
        return stack.is(ModItems.WORMHOLE_LINK.get()) && stack.has(ModDataComponents.LINK.get());
    }

    /// Solar Wind Upgrades (0 to MAX_UPGRADES).
    public int speed() {
        return Math.min(MAX_UPGRADES, upgrades.getItem(SPEED_SLOT).getCount());
    }

    /// The size of the field (blocks in every direction).
    public int radius() {
        return StarNetwork.FIELD_RADIUS + Math.min(MAX_UPGRADES, upgrades.getItem(RANGE_SLOT).getCount()) * RANGE_PER_UPGRADE;
    }

    /// The Magnetars the Wormhole Links point to.
    public List<GlobalPos> links() {
        List<GlobalPos> list = new ArrayList<>();
        for (int i = LINK_START; i < SLOTS; i++) {
            GlobalPos link = upgrades.getItem(i).get(ModDataComponents.LINK.get());
            if (link != null) list.add(link);
        }
        return list;
    }

    public long getExperience() {
        return experience;
    }

    public void addExperience(int amount) {
        experience += amount;
        setChanged();
    }

    /// Takes all the experience (it goes to a player).
    public long takeExperience() {
        long taken = experience;
        experience = 0;
        setChanged();
        return taken;
    }

    // ---------------------------------------------------------------- chunk loading

    /// Server: checks the chunks to keep loaded every 5 seconds.
    public void serverTick() {
        if (++ticks % 100 == 1 && level instanceof ServerLevel serverLevel) updateChunks(serverLevel);
    }

    /// With Wormhole Links: keeps the chunks of this field and of the fields of the linked Magnetars loaded.
    /// Without links: no chunk is kept.
    private void updateChunks(ServerLevel serverLevel) {
        Set<GlobalPos> wanted = new HashSet<>();
        List<GlobalPos> links = links();
        if (!links.isEmpty()) {
            addField(wanted, serverLevel.dimension(), worldPosition, radius());
            for (GlobalPos link : links) {
                ServerLevel other = serverLevel.getServer().getLevel(link.dimension());
                if (other == null) continue;
                BlockPos pos = link.pos();
                // The field of the linked Magnetar is known once its chunk is loaded
                boolean loaded = other.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
                int radius = loaded && other.getBlockEntity(pos) instanceof MagnetarBlockEntity magnetar ? magnetar.radius() : 0;
                addField(wanted, link.dimension(), pos, radius);
            }
        }
        boolean changed = false;
        for (GlobalPos chunk : new ArrayList<>(forced)) {
            if (!wanted.contains(chunk)) {
                force(serverLevel, chunk, false);
                forced.remove(chunk);
                changed = true;
            }
        }
        for (GlobalPos chunk : wanted) {
            if (forced.add(chunk)) {
                force(serverLevel, chunk, true);
                changed = true;
            }
        }
        if (changed) setChanged();
    }

    private static void addField(Set<GlobalPos> chunks, ResourceKey<Level> dimension, BlockPos center, int radius) {
        for (int x = (center.getX() - radius) >> 4; x <= (center.getX() + radius) >> 4; x++) {
            for (int z = (center.getZ() - radius) >> 4; z <= (center.getZ() + radius) >> 4; z++) {
                chunks.add(GlobalPos.of(dimension, new BlockPos(x, 0, z)));
            }
        }
    }

    private void force(ServerLevel serverLevel, GlobalPos chunk, boolean add) {
        ServerLevel target = serverLevel.getServer().getLevel(chunk.dimension());
        if (target != null) StarChunks.CONTROLLER.forceChunk(target, worldPosition, chunk.pos().getX(), chunk.pos().getZ(), add, true);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        LOADED.add(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        LOADED.remove(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LOADED.remove(this);
    }

    /// When the Magnetar is broken, its upgrades drop on the ground.
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropContents(level, pos, upgrades);
        if (level instanceof ServerLevel serverLevel) {
            // The chunks are not kept loaded any more, and the experience drops
            for (GlobalPos chunk : forced) force(serverLevel, chunk, false);
            forced.clear();
            if (experience > 0) ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), (int) Math.min(experience, Integer.MAX_VALUE));
            experience = 0;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("upgrades", ItemStack.OPTIONAL_CODEC.listOf(), upgrades.getItems());
        output.putLong("experience", experience);
        output.store("forced_chunks", GlobalPos.CODEC.listOf(), new ArrayList<>(forced));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        List<ItemStack> items = input.read("upgrades", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < SLOTS; i++) upgrades.setItem(i, i < items.size() ? items.get(i) : ItemStack.EMPTY);
        experience = input.getLongOr("experience", 0L);
        forced.clear();
        forced.addAll(input.read("forced_chunks", GlobalPos.CODEC.listOf()).orElse(List.of()));
    }
}
