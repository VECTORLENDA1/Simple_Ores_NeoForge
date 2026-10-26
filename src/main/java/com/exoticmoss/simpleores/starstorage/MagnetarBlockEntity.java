package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import com.exoticmoss.simpleores.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/// The Magnetar Core block entity. It keeps no items of the network (they are in the cells), only its upgrades
/// (put in its screen, see MagnetarMenu):
///  - SPEED_SLOT: Solar Wind Upgrades (up to MAX_UPGRADES): the auto-crafts give items to the Gravity Cores
///    more often and more at a time;
///  - RANGE_SLOT: Magnetic Field Upgrades (up to MAX_UPGRADES): each one makes the field RANGE_PER_UPGRADE blocks bigger;
///  - LINK_SLOTS: Wormhole Links: the drives and Quasars of the Magnetars they point to (any distance, any
///    dimension, while their chunks are loaded) are part of this network too.
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
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("upgrades", ItemStack.OPTIONAL_CODEC.listOf(), upgrades.getItems());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        List<ItemStack> items = input.read("upgrades", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < SLOTS; i++) upgrades.setItem(i, i < items.size() ? items.get(i) : ItemStack.EMPTY);
    }
}
