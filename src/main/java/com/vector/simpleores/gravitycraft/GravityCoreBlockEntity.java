package com.vector.simpleores.gravitycraft;

import com.vector.simpleores.block.entity.ModBlockEntities;
import com.vector.simpleores.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static java.util.Comparator.comparing;

/// The "brain" of the Gravity Core:
///  1. it captures items dropped near the core and puts them in orbit;
///  2. when the orbiting items match a recipe, the Craft starts;
///  3. when the Craft ends, it uses up the ingredients and delivers the result.
///
/// The orbiting items are only stored here (there is no real physics).
/// The orbit itself is drawn by GravityCoreRenderer.
public class GravityCoreBlockEntity extends BlockEntity {
    /// Distance (in blocks) at which the core captures items.
    public static final double CAPTURE_RADIUS = 2.0;
    /// Tag added to items created by a core, so that cores don't capture them again.
    private static final String CRAFTED_TAG = "simpleores_gravity_output";

    /// Items in orbit.
    private final List<ItemStack> items = new ArrayList<>();
    /// Player who threw the last item (receives the result). null = it was a machine.
    @Nullable
    private UUID owner = null;
    /// Craft progress. craftTotal = 0 means the core is not crafting.
    private int craftTime = 0;
    private int craftTotal = 0;
    /// Tier the core is turning into during an upgrade. null = not upgrading.
    @Nullable
    private CoreTier upgradeTarget = null;

    /// Angle of the orbit, used only by the client for the animation.
    /// Every tick it grows by the current speed, so when the speed changes
    /// (e.g. during a Craft or an upgrade) the items speed up smoothly instead of jumping.
    private float orbitAngle = 0;
    private float prevOrbitAngle = 0;

    public GravityCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVITY_CORE_BE.get(), pos, state);
    }

    public CoreTier getTier() {
        return ((GravityCoreBlock) getBlockState().getBlock()).tier;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public @Nullable CoreTier getUpgradeTarget() {
        return upgradeTarget;
    }

    /// Orbit angle for this frame (smoothed between the last two ticks).
    public float getOrbitAngle(float partialTick) {
        return Mth.lerp(partialTick, prevOrbitAngle, orbitAngle);
    }

    /// How fast the items orbit right now. During a Craft they go up to 5x faster,
    /// and during an upgrade the speed also slowly changes to the speed of the new tier.
    private float currentOrbitSpeed() {
        float progress = getCraftProgress(0);
        CoreTier to = upgradeTarget != null ? upgradeTarget : getTier();
        float tierSpeed = Mth.lerp(progress, getTier().orbitSpeed, to.orbitSpeed);
        return 0.05f * tierSpeed * (1 + 4 * progress);
    }

    /// 0.0 = Craft starting, 1.0 = Craft finished. Also 0 when the core is not crafting.
    public float getCraftProgress(float partialTick) {
        if (craftTotal <= 0) return 0;
        return Math.min(1f, (craftTime + partialTick) / craftTotal);
    }

    /// Center of the sphere in the world. The sphere sits on top of the block below it.
    private Vec3 center() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + getTier().coreSize / 2, worldPosition.getZ() + 0.5);
    }

    // ---------------------------------------------------------------- tick

    /// Runs 20 times per second, on the server and on the client.
    public void tick() {
        if (level == null) return;

        // Client: move the orbit forward a little (animation only)
        if (level.isClientSide()) {
            prevOrbitAngle = orbitAngle;
            orbitAngle += currentOrbitSpeed();
        }

        if (craftTotal > 0) {
            craftTime++;
            // The client only counts the time for the animation; the server makes the decisions
            if (!level.isClientSide()) {
                if (upgradeTarget != null) upgradeEffects();
                if (craftTime >= craftTotal) finishCraft();
            }
            return;
        }

        // Look for new items 4 times per second (good enough and cheap for the server)
        if (!level.isClientSide() && level.getGameTime() % 5 == 0) {
            captureItems();
        }
    }

    // ---------------------------------------------------------------- capturing items

    private void captureItems() {
        AABB area = new AABB(worldPosition).inflate(CAPTURE_RADIUS);
        boolean capturedSomething = false;

        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (entity.entityTags().contains(CRAFTED_TAG)) continue;

            ItemStack stack = entity.getItem().copy();
            int before = stack.getCount();
            addToOrbit(stack);
            if (stack.getCount() == before) continue; // nothing fit

            capturedSomething = true;
            // Whoever threw the item receives the result. If it wasn't a player (e.g. a dropper), it stays null.
            owner = entity.getOwner() instanceof Player player ? player.getUUID() : null;

            if (stack.isEmpty()) entity.discard();
            else entity.setItem(stack);
        }

        if (capturedSomething) {
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 0.6f);
            changed();
            tryStartCraft();
        }
    }

    /// True while the core is crafting (it doesn't accept new items until it finishes).
    public boolean isBusy() {
        return craftTotal > 0;
    }

    /// Used by JEI's "Move Items" button: puts items straight from a player into the orbit.
    /// Returns whatever didn't fit (it goes back to the player).
    public List<ItemStack> insertFromPlayer(Player player, List<ItemStack> stacks) {
        List<ItemStack> leftovers = new ArrayList<>();
        for (ItemStack stack : stacks) {
            addToOrbit(stack);
            if (!stack.isEmpty()) leftovers.add(stack);
        }
        owner = player.getUUID();
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 0.6f);
        changed();
        tryStartCraft();
        return leftovers;
    }

    /// Adds the stack to the orbiting items. Whatever doesn't fit stays in the stack.
    private void addToOrbit(ItemStack stack) {
        // First merge with identical stacks that are already in orbit
        for (ItemStack inOrbit : items) {
            if (stack.isEmpty()) return;
            if (ItemStack.isSameItemSameComponents(inOrbit, stack)) {
                int space = inOrbit.getMaxStackSize() - inOrbit.getCount();
                int moved = Math.min(space, stack.getCount());
                inOrbit.grow(moved);
                stack.shrink(moved);
            }
        }
        // Then start a new orbit, if there is still room
        if (!stack.isEmpty() && items.size() < getTier().maxItems) {
            items.add(stack.copy());
            stack.setCount(0);
        }
    }

    // ---------------------------------------------------------------- crafting

    @SuppressWarnings("unchecked")
    private Optional<RecipeHolder<GravityCoreRecipe>> findRecipe() {
        if (!(level instanceof ServerLevel serverLevel)) return Optional.empty();
        int coreTier = getTier().level;
        GravityCoreRecipe.Input input = new GravityCoreRecipe.Input(items, coreTier);

        return serverLevel.recipeAccess().getRecipes().stream()
                .filter(holder -> holder.value() instanceof GravityCoreRecipe)
                .map(holder -> (RecipeHolder<GravityCoreRecipe>) (RecipeHolder<?>) holder)
                .filter(holder -> holder.value().matches(input, serverLevel))
                .max(Comparator
                        .comparing((RecipeHolder<GravityCoreRecipe> holder) -> holder.value().getUpgradeTier() != null)
                        .thenComparingInt(holder -> holder.value().getTier())
                        .thenComparingInt(holder -> totalIngredientCount(holder.value(), coreTier)));
    }

    /// Sum of how many items every ingredient of the recipe needs, for the given core tier.
    private static int totalIngredientCount(GravityCoreRecipe recipe, int coreTier) {
        int total = 0;
        for (GravityCoreRecipe.Part part : recipe.getParts()) {
            total += part.countFor(coreTier);
        }
        return total;
    }

    /// Called whenever the orbiting items change. Starts the craft right away if they match a recipe.
    /// A recipe only matches when EVERY orbiting item belongs to it (see GravityCoreRecipe.matches),
    /// so a player building a bigger recipe can block smaller ones just by dropping one of its
    /// unique items first — no waiting needed.
    private void tryStartCraft() {
        findRecipe().ifPresent(recipe -> {
            craftTime = 0;
            // The duration can depend on the tier of this core ("time_per_tier" in the recipe)
            craftTotal = Math.max(1, recipe.value().timeFor(getTier().level));
            upgradeTarget = recipe.value().getUpgradeTier();
            if (upgradeTarget != null) {
                level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.5f, 0.5f);
            }
            changed();
        });
    }

    private void finishCraft() {
        craftTime = 0;
        craftTotal = 0;
        upgradeTarget = null;

        // Check the recipe again (items may have been taken out in the meantime)
        Optional<RecipeHolder<GravityCoreRecipe>> recipe = findRecipe();
        if (recipe.isPresent()) {
            consume(recipe.get().value());
            // The amount can depend on the tier of this core ("count_per_tier" in the recipe)
            ItemStack result = recipe.get().value().createResult(getTier().level);

            if (recipe.get().value().getUpgradeTier() != null) {
                finishUpgrade(result);
                return; // the block no longer exists
            }
            // Deliver in normal stacks (e.g. 128 items = 2 stacks of 64)
            while (!result.isEmpty()) {
                deliver(result.split(result.getMaxStackSize()));
            }
            flash();
        }

        changed();
        tryStartCraft(); // if the leftover items match another recipe, keep going
    }

    /// Uses up the ingredients of the recipe.
    private void consume(GravityCoreRecipe recipe) {
        int coreTier = getTier().level;
        for (GravityCoreRecipe.Part part : recipe.getParts()) {
            int missing = part.countFor(coreTier);
            for (ItemStack stack : items) {
                if (missing <= 0) break;
                if (part.item().test(stack)) {
                    int taken = Math.min(missing, stack.getCount());
                    stack.shrink(taken);
                    missing -= taken;
                }
            }
        }
        items.removeIf(ItemStack::isEmpty);
    }

    // ---------------------------------------------------------------- upgrades

    /// Special effects while the core is upgrading: sparks around the sphere and a rising hum.
    private void upgradeEffects() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        Vec3 c = center();
        float progress = (float) craftTime / craftTotal;
        double r = getTier().coreSize / 2 + 0.3;

        if (craftTime % 2 == 0) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 4, r, r, r, 0.05);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 6, r, r, r, 0.02);
        }
        if (craftTime % 20 == 0) {
            // The sound gets higher as the upgrade gets closer to the end
            level.playSound(null, worldPosition, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 1.5f, 0.6f + progress);
        }
    }

    /// End of an upgrade: big flash, then the core drops itself on the ground as the new tier.
    private void finishUpgrade(ItemStack upgradedCore) {
        Vec3 c = center();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 3, 0.4, 0.4, 0.4, 0);
            serverLevel.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 80, 0.3, 0.3, 0.3, 0.3);
        }
        level.playSound(null, worldPosition, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6f, 1.5f);

        // Items that were orbiting but not used drop on the ground too
        for (ItemStack stack : items) {
            spawnItem(stack, c);
        }
        items.clear();

        spawnItem(upgradedCore, c);
        level.removeBlock(worldPosition, false);
    }

    // ---------------------------------------------------------------- delivering the result

    /// Player -> goes to the player's inventory (if it's full, it drops in front of the player).
    /// Machine -> goes to the inventory under the core (hopper, chest...) or pops out on top of the core.
    private void deliver(ItemStack result) {
        Player player = owner == null ? null : level.getPlayerByUUID(owner);
        if (player != null) {
            player.getInventory().add(result);
            if (!result.isEmpty()) {
                ItemEntity dropped = player.drop(result, false);
                if (dropped != null) dropped.addTag(CRAFTED_TAG);
            }
            return;
        }

        ResourceHandler<ItemResource> below = level.getCapability(Capabilities.Item.BLOCK, worldPosition.below(), Direction.UP);
        if (below != null) {
            result = ItemUtil.insertItemReturnRemaining(below, result, false, null);
        }
        if (!result.isEmpty()) {
            Vec3 c = center();
            spawnItem(result, new Vec3(c.x, c.y + getTier().coreSize / 2, c.z));
        }
    }

    /// Spawns an item that jumps up a little. It is tagged so the cores don't capture it again.
    private void spawnItem(ItemStack stack, Vec3 pos) {
        ItemEntity entity = new ItemEntity(level, pos.x, pos.y, pos.z, stack, 0, 0.3, 0);
        entity.addTag(CRAFTED_TAG);
        level.addFreshEntity(entity);
    }

    /// The "mini supernova" at the end of a normal Craft.
    private void flash() {
        Vec3 c = center();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 40, 0.2, 0.2, 0.2, 0.25);
        }
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1f, 1.5f);
    }

    /// Right-click with an empty hand: gives every orbiting item back to the player and cancels the Craft.
    public void giveItemsBack(Player player) {
        for (ItemStack stack : items) {
            player.getInventory().add(stack);
            if (!stack.isEmpty()) {
                ItemEntity dropped = player.drop(stack, false);
                if (dropped != null) dropped.addTag(CRAFTED_TAG);
            }
        }
        items.clear();
        craftTime = 0;
        craftTotal = 0;
        upgradeTarget = null;
        changed();
    }

    /// When the block is broken, the orbiting items drop on the ground.
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        for (ItemStack stack : items) {
            spawnItem(stack, center());
        }
        items.clear();
    }

    // ---------------------------------------------------------------- saving and syncing

    /// Saves the changes and sends them to the clients (so the renderer can draw them).
    private void changed() {
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("items", ItemStack.CODEC.listOf(), items);
        output.storeNullable("owner", UUIDUtil.CODEC, owner);
        output.putInt("craft_time", craftTime);
        output.putInt("craft_total", craftTotal);
        output.storeNullable("upgrade_target", CoreTier.CODEC, upgradeTarget);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        items.addAll(input.read("items", ItemStack.CODEC.listOf()).orElse(List.of()));
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        craftTime = input.getIntOr("craft_time", 0);
        craftTotal = input.getIntOr("craft_total", 0);
        upgradeTarget = input.read("upgrade_target", CoreTier.CODEC).orElse(null);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}