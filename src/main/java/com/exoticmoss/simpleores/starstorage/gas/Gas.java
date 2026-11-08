package com.exoticmoss.simpleores.starstorage.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/// An amount (mB) of a gas of Mekanism (its chemicals: gases, infuse types, pigments, slurries), by its id. The gases
/// are kept by id, so the mod works without Mekanism (see Gases).
public record Gas(Identifier id, long amount) {
    public static final Codec<Gas> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Identifier.CODEC.fieldOf("gas").forGetter(Gas::id),
            Codec.LONG.fieldOf("amount").forGetter(Gas::amount)
    ).apply(inst, Gas::new));

    public static final StreamCodec<ByteBuf, Gas> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, Gas::id,
            ByteBufCodecs.VAR_LONG, Gas::amount,
            Gas::new);
}
