package com.exoticmoss.simpleores.util.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/// The render types used by the renderers of this mod.
public final class RenderTypes {
    private RenderTypes() {
    }

    public static RenderType entityCutout(ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    /// The light of the stars (their glow, disk and beams): see-through and bright like entityTranslucentEmissive, but
    /// it doesn't hide what is drawn after it (it writes no depth). Otherwise the glow of a star cuts holes in the
    /// stars and the glows next to it.
    public static RenderType glow(ResourceLocation texture) {
        return Glow.GLOW.apply(texture);
    }

    /// True if this is the render type of the light of the stars (see glow).
    public static boolean isGlow(RenderType type) {
        return GLOWS.contains(type);
    }

    /// The render types made by glow.
    private static final java.util.Set<RenderType> GLOWS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static RenderType remember(RenderType type) {
        GLOWS.add(type);
        return type;
    }

    /// (only to reach the parts of a render type, which only the render types themselves can use)
    private static final class Glow extends RenderType {
        private static final Function<ResourceLocation, RenderType> GLOW = Util.memoize(texture -> remember(create("simpleores_glow",
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(false))));

        private Glow(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                     boolean sortOnUpload, Runnable setup, Runnable clear) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
        }
    }
}
