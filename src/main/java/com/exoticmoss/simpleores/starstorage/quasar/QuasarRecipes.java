package com.exoticmoss.simpleores.starstorage.quasar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.resources.Identifier;
import com.exoticmoss.simpleores.SimpleOres;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/// The recipes of every Quasar, kept by the world (not only by the block): when a Quasar is broken, its item keeps the
/// id of its recipes (and how many there are), and the Quasar placed with that item gets them back. The recipes stay
/// here even if the block is lost, so they are never gone.
public class QuasarRecipes extends SavedData {
    private static final String NAME = "quasar_recipes";

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

    private static final Codec<QuasarRecipes> CODEC = Codec.unboundedMap(Codec.STRING, Pattern.CODEC.listOf()).xmap(saved -> {
        QuasarRecipes data = new QuasarRecipes();
        saved.forEach((key, list) -> {
            try {
                data.recipes.put(UUID.fromString(key), new ArrayList<>(list));
            } catch (IllegalArgumentException e) {
                // (not an id)
            }
        });
        return data;
    }, data -> {
        Map<String, List<Pattern>> saved = new HashMap<>();
        data.recipes.forEach((id, list) -> saved.put(id.toString(), list));
        return saved;
    });

    private static final SavedDataType<QuasarRecipes> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SimpleOres.MODID, NAME), level -> new QuasarRecipes(), level -> CODEC);

    private final Map<UUID, List<Pattern>> recipes = new HashMap<>();

    public static QuasarRecipes get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
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
}
