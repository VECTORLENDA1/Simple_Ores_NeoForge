package com.vector.simpleores.gravitycraft;

import com.mojang.serialization.Codec;
import com.vector.simpleores.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
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

/// The "brain" of the Gravity Core:
///  1. it captures items dropped near the core and puts them in orbit;
///  2. it works out which recipes the orbiting items can still become (the "candidates"),
///     which are shown in the selection bar above the core (see CoreSelectionBar);
///  3. the Craft starts when the recipe the player picked (the "target") is complete,
///     or by itself when only one recipe is possible and it is complete;
///  4. when the Craft ends, it uses up the ingredients and delivers the result.
///
/// The orbiting items are only stored here (there is no real physics).
/// The orbit itself is drawn by GravityCoreRenderer.
public class GravityCoreBlockEntity extends BlockEntity {
    /// Distance (in blocks) at which the core captures items.
    public static final double CAPTURE_RADIUS = 2.0;
    /// Tag added to items created by a core, so that cores don't capture them again.
    private static final String CRAFTED_TAG = "simpleores_gravity_output";
    /// Maximum number of candidates sent to the client (the bar scrolls through them).
    public static final int MAX_CANDIDATES = 50;
    private static final Codec<ResourceKey<Recipe<?>>> RECIPE_KEY_CODEC = ResourceKey.codec(Registries.RECIPE);

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
    /// Recipe the player picked in the selection bar (or with JEI's "Move Items").
    /// It stays picked while there are items in orbit; when the orbit is empty it is cleared.
    @Nullable
    private ResourceKey<Recipe<?>> target = null;
    /// Recipe being crafted right now. null = not crafting.
    @Nullable
    private ResourceKey<Recipe<?>> crafting = null;
    /// Recipes the orbiting items can still become, best first. Shown in the selection bar.
    /// When a target is picked, this only has the target.
    private final List<CoreCandidate> candidates = new ArrayList<>();
    /// Server: the candidates must be worked out again (e.g. after the world is loaded).
    private boolean candidatesOutdated = true;

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

    /// Recipes shown in the selection bar (synced from the server).
    public List<CoreCandidate> getCandidates() {
        return candidates;
    }

    /// Recipe picked by the player. null = none picked.
    public @Nullable ResourceKey<Recipe<?>> getTarget() {
        return target;
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

        if (!level.isClientSide() && candidatesOutdated) {
            onItemsChanged();
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
        boolean refusedSomething = false;

        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (entity.entityTags().contains(CRAFTED_TAG)) continue;
            Player thrower = entity.getOwner() instanceof Player player ? player : null;

            // An item that doesn't fit the picked recipe (or any recipe together with the orbiting
            // items) is not captured: it goes back to the player who threw it.
            // Items thrown by machines just stay on the ground.
            if (!accepts(entity.getItem())) {
                if (thrower != null) {
                    giveToPlayer(thrower, entity.getItem().copy());
                    entity.discard();
                    refusedSomething = true;
                }
                continue;
            }

            ItemStack stack = entity.getItem().copy();
            int before = stack.getCount();
            addToOrbit(stack);
            if (stack.getCount() == before) continue; // nothing fit

            capturedSomething = true;
            // Whoever threw the item receives the result. If it wasn't a player (e.g. a dropper), it stays null.
            owner = thrower != null ? thrower.getUUID() : null;

            if (stack.isEmpty()) entity.discard();
            else entity.setItem(stack);
        }

        if (refusedSomething) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 0.5f);
        }
        if (capturedSomething) {
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 0.6f);
            onItemsChanged();
        }
    }

    /// Can this item join the orbit?
    ///  - with a picked recipe: only if it is one of its ingredients;
    ///  - without one: only if at least one recipe uses it together with every orbiting item.
    private boolean accepts(ItemStack stack) {
        if (target != null) {
            return findByKey(target).map(holder -> holder.value().uses(stack)).orElse(true);
        }
        List<ItemStack> together = new ArrayList<>(items);
        together.add(stack);
        int coreTier = getTier().level;
        return allRecipes().stream()
                .anyMatch(holder -> holder.value().worksInTier(coreTier) && holder.value().usesAll(together));
    }

    /// True while the core is crafting (it doesn't accept new items until it finishes).
    public boolean isBusy() {
        return craftTotal > 0;
    }

    /// Used by JEI's "Move Items" button: puts items straight from a player into the orbit
    /// and picks the recipe chosen in JEI. Returns whatever didn't fit (it goes back to the player).
    public List<ItemStack> insertFromPlayer(Player player, List<ItemStack> stacks, RecipeHolder<GravityCoreRecipe> recipe) {
        List<ItemStack> leftovers = new ArrayList<>();
        for (ItemStack stack : stacks) {
            addToOrbit(stack);
            if (!stack.isEmpty()) leftovers.add(stack);
        }
        owner = player.getUUID();
        target = recipe.id();
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 0.6f);
        onItemsChanged();
        return leftovers;
    }

    /// Called when a player clicks a recipe in the selection bar (see SelectCoreRecipe).
    /// recipe = null means "unpick" (clicking the recipe that is already picked).
    public void selectRecipe(Player player, @Nullable ResourceKey<Recipe<?>> recipe) {
        if (recipe == null) {
            target = null;
        } else {
            // Only recipes shown in the bar can be picked (never trust the client)
            if (isBusy() || candidates.stream().noneMatch(candidate -> candidate.id().equals(recipe))) return;
            target = recipe;
            owner = player.getUUID();
        }
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, recipe == null ? 0.8f : 1.4f);
        onItemsChanged();
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

    /// Every recipe the core can make: the Gravity Core recipes of the server
    /// plus the vanilla crafting recipes (see VanillaCoreRecipe).
    @SuppressWarnings("unchecked")
    private List<RecipeHolder<GravityCoreRecipe>> allRecipes() {
        if (!(level instanceof ServerLevel serverLevel)) return List.of();
        List<RecipeHolder<GravityCoreRecipe>> recipes = new ArrayList<>();
        for (RecipeHolder<?> holder : serverLevel.recipeAccess().getRecipes()) {
            if (holder.value() instanceof GravityCoreRecipe) recipes.add((RecipeHolder<GravityCoreRecipe>) holder);
        }
        recipes.addAll(VanillaCoreRecipe.all(serverLevel));
        return recipes;
    }

    /// Finds a recipe the core can make by its ID (Gravity Core or vanilla). Empty if it doesn't exist (anymore).
    @SuppressWarnings("unchecked")
    private Optional<RecipeHolder<GravityCoreRecipe>> findByKey(ResourceKey<Recipe<?>> key) {
        if (!(level instanceof ServerLevel serverLevel)) return Optional.empty();
        Optional<RecipeHolder<GravityCoreRecipe>> vanilla = VanillaCoreRecipe.find(serverLevel, key);
        if (vanilla.isPresent()) return vanilla;
        return serverLevel.recipeAccess().byKey(key)
                .filter(holder -> holder.value() instanceof GravityCoreRecipe)
                .map(holder -> (RecipeHolder<GravityCoreRecipe>) (RecipeHolder<?>) holder);
    }

    /// Order of the recipes in the selection bar: Gravity Core recipes before vanilla ones,
    /// core upgrades first, then higher tier recipes, then recipes with more ingredients.
    private static Comparator<RecipeHolder<GravityCoreRecipe>> priority(int coreTier) {
        return Comparator
                .comparing((RecipeHolder<GravityCoreRecipe> holder) -> !(holder.value() instanceof VanillaCoreRecipe))
                .thenComparing(holder -> holder.value().getUpgradeTier() != null)
                .thenComparingInt(holder -> holder.value().getTier())
                .thenComparingInt(holder -> totalIngredientCount(holder.value(), coreTier));
    }

    /// Sum of how many items every ingredient of the recipe needs, for the given core tier.
    private static int totalIngredientCount(GravityCoreRecipe recipe, int coreTier) {
        int total = 0;
        for (GravityCoreRecipe.Part part : recipe.getParts()) {
            total += part.countFor(coreTier);
        }
        return total;
    }

    /// True if the recipe can be crafted with the orbiting items right now.
    private boolean canCraft(RecipeHolder<GravityCoreRecipe> recipe) {
        return recipe.value().matches(new GravityCoreRecipe.Input(items, getTier().level), level);
    }

    /// Called whenever the orbiting items (or the picked recipe) change:
    /// works out the candidates again, starts the Craft if possible and syncs everything to the clients.
    private void onItemsChanged() {
        candidatesOutdated = false;
        if (items.isEmpty()) target = null; // nothing left in orbit: the picked recipe is cleared
        updateCandidates();
        tryStartCraft();
        changed();
    }

    /// Works out the recipes the orbiting items can still become (see "candidates").
    private void updateCandidates() {
        candidates.clear();
        if (items.isEmpty()) return;
        int coreTier = getTier().level;

        if (target != null) {
            Optional<RecipeHolder<GravityCoreRecipe>> picked = findByKey(target);
            if (picked.isPresent()) {
                candidates.add(CoreCandidate.of(picked.get(), items, coreTier));
                return;
            }
            target = null; // the recipe no longer exists (e.g. a datapack was removed)
        }

        allRecipes().stream()
                .filter(holder -> holder.value().worksInTier(coreTier) && holder.value().usesAll(items))
                .sorted(priority(coreTier).reversed())
                .limit(MAX_CANDIDATES)
                .forEach(holder -> candidates.add(CoreCandidate.of(holder, items, coreTier)));
    }

    /// Starts the Craft if:
    ///  - the player picked a recipe and all its items are orbiting; or
    ///  - nothing was picked, but only ONE recipe is possible and all its items are orbiting
    ///    (no doubt about what the player wants, so there's no need to click).
    /// When more than one recipe is possible, the core waits for the player to pick one.
    private void tryStartCraft() {
        if (isBusy() || items.isEmpty()) return;

        ResourceKey<Recipe<?>> key = target;
        if (key == null && candidates.size() == 1 && candidates.get(0).complete()) {
            key = candidates.get(0).id();
        }
        if (key == null) return;

        findByKey(key).filter(this::canCraft).ifPresent(recipe -> {
            crafting = recipe.id();
            craftTime = 0;
            // The duration can depend on the tier of this core ("time_per_tier" in the recipe)
            craftTotal = Math.max(1, recipe.value().timeFor(getTier().level));
            upgradeTarget = recipe.value().getUpgradeTier();
            if (upgradeTarget != null) {
                level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.5f, 0.5f);
            }
        });
    }

    private void finishCraft() {
        ResourceKey<Recipe<?>> key = crafting;
        crafting = null;
        craftTime = 0;
        craftTotal = 0;
        upgradeTarget = null;

        // Check the recipe again (items may have been taken out in the meantime)
        Optional<RecipeHolder<GravityCoreRecipe>> recipe = key == null ? Optional.empty() : findByKey(key).filter(this::canCraft);
        if (recipe.isPresent()) {
            List<ItemStack> used = consume(recipe.get().value());
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
            // Vanilla recipes give back what stays in the crafting table (e.g. empty buckets)
            if (recipe.get().value() instanceof VanillaCoreRecipe vanilla) {
                for (ItemStack remainder : vanilla.remainders(used)) {
                    deliver(remainder);
                }
            }
            flash();
        }

        // If there are still enough items for the same recipe, the next Craft starts right away
        onItemsChanged();
    }

    /// Uses up the ingredients of the recipe. Returns the items that were used.
    private List<ItemStack> consume(GravityCoreRecipe recipe) {
        List<ItemStack> used = new ArrayList<>();
        int coreTier = getTier().level;
        for (GravityCoreRecipe.Part part : recipe.getParts()) {
            int missing = part.countFor(coreTier);
            for (ItemStack stack : items) {
                if (missing <= 0) break;
                if (part.item().test(stack)) {
                    int taken = Math.min(missing, stack.getCount());
                    used.add(stack.copyWithCount(taken));
                    stack.shrink(taken);
                    missing -= taken;
                }
            }
        }
        items.removeIf(ItemStack::isEmpty);
        return used;
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
            giveToPlayer(player, result);
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

    /// Puts the stack in the player's inventory. If it's full, it drops in front of the player
    /// (tagged, so the cores don't capture it again).
    private void giveToPlayer(Player player, ItemStack stack) {
        player.getInventory().add(stack);
        if (!stack.isEmpty()) {
            ItemEntity dropped = player.drop(stack, false);
            if (dropped != null) dropped.addTag(CRAFTED_TAG);
        }
    }

    /// The "mini supernova" at the end of a normal Craft.
    private void flash() {
        Vec3 c = center();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 40, 0.2, 0.2, 0.2, 0.25);
        }
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1f, 1.5f);
    }

    /// Right-click on the core with an empty hand: gives every orbiting item back to the player,
    /// cancels the Craft and clears the picked recipe.
    public void giveItemsBack(Player player) {
        for (ItemStack stack : items) {
            giveToPlayer(player, stack);
        }
        items.clear();
        crafting = null;
        craftTime = 0;
        craftTotal = 0;
        upgradeTarget = null;
        onItemsChanged(); // also clears the picked recipe and the selection bar
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
        output.storeNullable("target", RECIPE_KEY_CODEC, target);
        output.storeNullable("crafting", RECIPE_KEY_CODEC, crafting);
        output.store("candidates", CoreCandidate.CODEC.listOf(), candidates);
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
        target = input.read("target", RECIPE_KEY_CODEC).orElse(null);
        crafting = input.read("crafting", RECIPE_KEY_CODEC).orElse(null);
        candidates.clear();
        candidates.addAll(input.read("candidates", CoreCandidate.CODEC.listOf()).orElse(List.of()));
        candidatesOutdated = true; // the server works them out again on the next tick (recipes may have changed)
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