package com.exoticmoss.simpleores.starstorage.quasar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/// The recipes of every Quasar, kept by the world (not only by the block): when a Quasar is broken, its item keeps the
/// id of its recipes (and how many there are), and the Quasar placed with that item gets them back. The recipes stay
/// here even if the block is lost, so they are never gone.
public class QuasarRecipes extends SavedData {
    private static final String NAME = "simpleores_quasar_recipes";

    /// The id of the recipes of a Quasar and how many there are (kept by the item of a broken Quasar).
    public record Id(UUID id, int count) {
        public static final Codec<Id> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Id::id),
                Codec.INT.optionalFieldOf("count", 0).forGetter(Id::count)
        ).apply(inst, Id::new));
        public static final StreamCodec<ByteBuf, Id> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Id::id,
                ByteBufCodecs.VAR_INT, Id::count,
                Id::new);
    }

    private final Map<UUID, List<Pattern>> recipes = new HashMap<>();

    public static QuasarRecipes get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(QuasarRecipes::new, QuasarRecipes::load, null), NAME);
    }

    public List<Pattern> get(UUID id) {
        return recipes.getOrDefault(id, List.of());
    }

    public void put(UUID id, List<Pattern> patterns) {
        recipes.put(id, new ArrayList<>(patterns));
        setDirty();
    }

    public void remove(UUID id) {
        if (recipes.remove(id) != null) setDirty();
    }

    private static QuasarRecipes load(CompoundTag tag, HolderLookup.Provider registries) {
        QuasarRecipes data = new QuasarRecipes();
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        CompoundTag all = tag.getCompound("quasars");
        for (String key : all.getAllKeys()) {
            try {
                UUID id = UUID.fromString(key);
                Pattern.CODEC.listOf().parse(ops, all.get(key)).result().ifPresent(list -> data.recipes.put(id, new ArrayList<>(list)));
            } catch (IllegalArgumentException e) {
                // (not an id)
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        CompoundTag all = new CompoundTag();
        recipes.forEach((id, list) -> Pattern.CODEC.listOf().encodeStart(ops, list).result().ifPresent(saved -> all.put(id.toString(), saved)));
        tag.put("quasars", all);
        return tag;
    }
}
