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

    /// The light of the stars (their glow, disk and beams): see-through and bright like entityTranslucentEmissive, and
    /// drawn in the order it was given (not piece by piece from the farthest to the closest, which mixes the glow and
    /// the disk of a star in a different order in each place: a line and teeth on the disk). In the world it is
    /// drawn star by star, the farthest star first (see SubmitNodeCollector), like the later versions of Minecraft do.
    public static RenderType glow(ResourceLocation texture) {
        return Glow.GLOW.apply(texture);
    }

    /// The same light, but it doesn't hide what is drawn after it (it writes no depth): for the light shell of a star
    /// that is looked at and the box of the area of a Magnetar or Binary Star, which are drawn over everything else.
    public static RenderType overlay(ResourceLocation texture) {
        return Glow.OVERLAY_TYPE.apply(texture);
    }

    /// True if this is a render type of the light of the stars (see glow and overlay).
    public static boolean isGlow(RenderType type) {
        return GLOWS.contains(type);
    }

    /// True if this is a render type made by overlay.
    public static boolean isOverlay(RenderType type) {
        return OVERLAYS.contains(type);
    }

    /// The render types made by overlay.
    private static final java.util.Set<RenderType> OVERLAYS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /// The render types made by glow and overlay.
    private static final java.util.Set<RenderType> GLOWS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static RenderType remember(RenderType type) {
        GLOWS.add(type);
        return type;
    }

    private static RenderType rememberOverlay(RenderType type) {
        OVERLAYS.add(type);
        return remember(type);
    }

    /// (only to reach the parts of a render type, which only the render types themselves can use)
    private static final class Glow extends RenderType {
        private static final Function<ResourceLocation, RenderType> GLOW = Util.memoize(texture -> remember(create("simpleores_glow",
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(false))));
        private static final Function<ResourceLocation, RenderType> OVERLAY_TYPE = Util.memoize(texture -> rememberOverlay(create("simpleores_overlay",
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false,
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
