package com.vector.simpleores.gravity;

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
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/// O "cerebro" do Nucleo Gravitacional:
///  1. apanha os itens largados perto do nucleo e poe-nos em orbita;
///  2. quando os itens em orbita formam uma receita, comeca o colapso;
///  3. no fim do colapso gasta os ingredientes e entrega o resultado.
///
/// Os itens em orbita sao guardados aqui (nao ha fisica a serio).
/// O desenho da orbita e feito no GravityCoreRenderer.
public class GravityCoreBlockEntity extends BlockEntity {
    /// Distancia (em blocos) a que o nucleo apanha itens.
    public static final double CAPTURE_RADIUS = 3.0;
    /// Marca posta nos itens que o nucleo cria, para nao os voltar a apanhar.
    private static final String CRAFTED_TAG = "simpleores_gravity_output";

    /// Itens em orbita.
    private final List<ItemStack> items = new ArrayList<>();
    /// Jogador que atirou o ultimo item (recebe o resultado). null = foi uma maquina.
    @Nullable
    private UUID owner = null;
    /// Progresso do colapso: craftTotal = 0 significa que nao esta a colapsar.
    private int craftTime = 0;
    private int craftTotal = 0;

    public GravityCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVITY_CORE_BE.get(), pos, state);
    }

    public CoreTier getTier() {
        return ((GravityCoreBlock) getBlockState().getBlock()).tier;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    /// 0.0 = colapso a comecar, 1.0 = colapso terminado. 0 tambem quando nao esta a colapsar.
    public float getCraftProgress(float partialTick) {
        if (craftTotal <= 0) return 0;
        return Math.min(1f, (craftTime + partialTick) / craftTotal);
    }

    // ---------------------------------------------------------------- tick

    /// Corre 20 vezes por segundo, no servidor e no cliente.
    public void tick() {
        if (level == null) return;

        if (craftTotal > 0) {
            craftTime++;
            // O cliente so conta o tempo para a animacao; quem decide e o servidor
            if (!level.isClientSide() && craftTime >= craftTotal) {
                finishCraft();
            }
            return;
        }

        // Procura itens novos 4 vezes por segundo (chega bem e poupa o servidor)
        if (!level.isClientSide() && level.getGameTime() % 5 == 0) {
            captureItems();
        }
    }

    // ---------------------------------------------------------------- apanhar itens

    private void captureItems() {
        AABB area = new AABB(worldPosition).inflate(CAPTURE_RADIUS);
        boolean capturedSomething = false;

        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (entity.entityTags().contains(CRAFTED_TAG)) continue;

            ItemStack stack = entity.getItem().copy();
            int before = stack.getCount();
            addToOrbit(stack);
            if (stack.getCount() == before) continue; // nao coube nada

            capturedSomething = true;
            // Quem atirou o item recebe o resultado. Se nao foi um jogador (ex: dropper), fica null.
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

    /// Junta o stack aos itens em orbita. O que nao couber fica no stack.
    private void addToOrbit(ItemStack stack) {
        // Primeiro junta a stacks iguais que ja estejam em orbita
        for (ItemStack inOrbit : items) {
            if (stack.isEmpty()) return;
            if (ItemStack.isSameItemSameComponents(inOrbit, stack)) {
                int space = inOrbit.getMaxStackSize() - inOrbit.getCount();
                int moved = Math.min(space, stack.getCount());
                inOrbit.grow(moved);
                stack.shrink(moved);
            }
        }
        // Depois cria uma orbita nova, se ainda houver lugar
        if (!stack.isEmpty() && items.size() < getTier().maxItems) {
            items.add(stack.copy());
            stack.setCount(0);
        }
    }

    // ---------------------------------------------------------------- crafting

    private Optional<RecipeHolder<GravityCoreRecipe>> findRecipe() {
        if (!(level instanceof ServerLevel serverLevel)) return Optional.empty();
        GravityCoreRecipe.Input input = new GravityCoreRecipe.Input(items, getTier().level);
        return serverLevel.recipeAccess().getRecipeFor(ModRecipes.GRAVITY_COLLAPSE_TYPE.get(), input, serverLevel);
    }

    private void tryStartCraft() {
        findRecipe().ifPresent(recipe -> {
            craftTime = 0;
            craftTotal = Math.max(1, recipe.value().getTime());
            changed();
        });
    }

    private void finishCraft() {
        craftTime = 0;
        craftTotal = 0;

        // Volta a confirmar a receita (os itens podem ter sido retirados entretanto)
        Optional<RecipeHolder<GravityCoreRecipe>> recipe = findRecipe();
        if (recipe.isPresent()) {
            consume(recipe.get().value());
            deliver(recipe.get().value().getResult().create());
            flash();
        }

        changed();
        tryStartCraft(); // se ainda sobrarem itens para outra receita, continua
    }

    /// Gasta os ingredientes da receita.
    private void consume(GravityCoreRecipe recipe) {
        for (GravityCoreRecipe.Part part : recipe.getParts()) {
            int missing = part.count();
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

    // ---------------------------------------------------------------- entregar o resultado

    /// Jogador -> vai para o inventario (se estiver cheio, cai a frente dele).
    /// Maquina -> vai para o inventario por baixo do nucleo (funil, bau...) ou salta para cima do nucleo.
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
            ItemEntity entity = new ItemEntity(level,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.5 + getTier().coreSize / 2 + 0.3, worldPosition.getZ() + 0.5,
                    result, 0, 0.3, 0);
            entity.addTag(CRAFTED_TAG);
            level.addFreshEntity(entity);
        }
    }

    /// A "mini supernova" no fim do colapso.
    private void flash() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    40, 0.2, 0.2, 0.2, 0.25);
        }
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1f, 1.5f);
    }

    /// Clique com a mao vazia: devolve ao jogador tudo o que esta em orbita e cancela o colapso.
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
        changed();
    }

    /// Quando o bloco e partido, larga os itens que estavam em orbita.
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        for (ItemStack stack : items) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        }
        items.clear();
    }

    // ---------------------------------------------------------------- guardar e sincronizar

    /// Guarda as alteracoes e envia-as para os clientes (para o renderer as desenhar).
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
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        items.addAll(input.read("items", ItemStack.CODEC.listOf()).orElse(List.of()));
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        craftTime = input.getIntOr("craft_time", 0);
        craftTotal = input.getIntOr("craft_total", 0);
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
