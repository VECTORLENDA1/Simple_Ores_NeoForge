package com.vector.simpleores.gravity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.vector.simpleores.SimpleOres;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// Desenha o Nucleo Gravitacional: a esfera, a aura, os itens em orbita
/// e (so no buraco negro) o disco de acrecao.
public class GravityCoreRenderer implements BlockEntityRenderer<GravityCoreBlockEntity, GravityCoreRenderer.State> {
    /// Textura branca: a cor vem do CoreTier.
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(SimpleOres.MODID, "textures/entity/gravity_core.png");
    /// Luz maxima: o nucleo brilha sempre, mesmo as escuras.
    private static final int FULL_BRIGHT = 0xF000F0;
    /// Quantas divisoes tem a esfera. Mais = mais redonda (mas mais pesada).
    private static final int SPHERE_DETAIL = 24;

    /// Copia dos dados do nucleo que o desenho precisa (o Minecraft separa "ler dados" de "desenhar").
    public static class State extends BlockEntityRenderState {
        CoreTier tier = CoreTier.SUN;
        float time;
        float progress;
        List<ItemStackRenderState> items = new ArrayList<>();
    }

    private final ItemModelResolver itemModelResolver;

    public GravityCoreRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GravityCoreBlockEntity core, State state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(core, state, partialTick, cameraPos, breakProgress);
        state.tier = core.getTier();
        state.time = (core.getLevel().getGameTime() % 100000) + partialTick;
        state.progress = core.getCraftProgress(partialTick);

        state.items = new ArrayList<>();
        List<ItemStack> items = core.getItems();
        for (int i = 0; i < items.size(); i++) {
            ItemStackRenderState itemState = new ItemStackRenderState();
            itemModelResolver.updateForTopItem(itemState, items.get(i), ItemDisplayContext.GROUND, core.getLevel(), null, i);
            state.items.add(itemState);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        CoreTier tier = state.tier;
        float time = state.time;
        float progress = state.progress;

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5); // centro do bloco

        // ---- Esfera + aura (a esfera "respira" devagar)
        float radius = tier.coreSize / 2 * (1 + 0.03f * Mth.sin(time * 0.15f));
        RenderType type = RenderTypes.entityTranslucentEmissive(WHITE);
        collector.submitCustomGeometry(pose, type, (p, v) -> sphere(p, v, radius, tier.coreColor));
        collector.submitCustomGeometry(pose, type, (p, v) -> sphere(p, v, radius * 1.2f, withAlpha(tier.glowColor, 80)));

        // ---- Disco de acrecao (so no buraco negro)
        if (tier == CoreTier.BLACK_HOLE) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(time * 2));
            collector.submitCustomGeometry(pose, type, (p, v) -> ring(p, v, radius * 1.3f, radius * 2.0f, withAlpha(tier.glowColor, 140)));
            pose.popPose();
        }

        // ---- Itens em orbita. Durante o colapso aproximam-se do centro (espiral) e giram mais depressa.
        int count = state.items.size();
        for (int i = 0; i < count; i++) {
            float angle = time * 0.05f * tier.orbitSpeed * (1 + 4 * progress) + i * Mth.TWO_PI / count;
            float orbit = tier.orbitRadius * (1 - progress);
            float bob = 0.1f * Mth.sin(time * 0.1f + i);

            pose.pushPose();
            pose.translate(Mth.cos(angle) * orbit, bob, Mth.sin(angle) * orbit);
            pose.mulPose(Axis.YP.rotation(-angle));
            state.items.get(i).submit(pose, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }

        pose.popPose();
    }

    /// Desenha uma esfera com quadrados pequenos (latitude x longitude).
    private static void sphere(PoseStack.Pose pose, VertexConsumer v, float radius, int color) {
        for (int lat = 0; lat < SPHERE_DETAIL; lat++) {
            float t1 = Mth.PI * lat / SPHERE_DETAIL;
            float t2 = Mth.PI * (lat + 1) / SPHERE_DETAIL;
            for (int lon = 0; lon < SPHERE_DETAIL; lon++) {
                float p1 = Mth.TWO_PI * lon / SPHERE_DETAIL;
                float p2 = Mth.TWO_PI * (lon + 1) / SPHERE_DETAIL;
                spherePoint(pose, v, radius, t1, p1, color);
                spherePoint(pose, v, radius, t1, p2, color);
                spherePoint(pose, v, radius, t2, p2, color);
                spherePoint(pose, v, radius, t2, p1, color);
            }
        }
    }

    private static void spherePoint(PoseStack.Pose pose, VertexConsumer v, float radius, float theta, float phi, int color) {
        float x = Mth.sin(theta) * Mth.cos(phi);
        float y = Mth.cos(theta);
        float z = Mth.sin(theta) * Mth.sin(phi);
        vertex(pose, v, x * radius, y * radius, z * radius, x, y, z, color);
    }

    /// Desenha um anel plano (dos dois lados) entre o raio interior e o exterior.
    private static void ring(PoseStack.Pose pose, VertexConsumer v, float inner, float outer, int color) {
        for (int i = 0; i < SPHERE_DETAIL * 2; i++) {
            float a1 = Mth.TWO_PI * i / (SPHERE_DETAIL * 2);
            float a2 = Mth.TWO_PI * (i + 1) / (SPHERE_DETAIL * 2);
            // face de cima
            vertex(pose, v, Mth.cos(a1) * inner, 0, Mth.sin(a1) * inner, 0, 1, 0, color);
            vertex(pose, v, Mth.cos(a1) * outer, 0, Mth.sin(a1) * outer, 0, 1, 0, color);
            vertex(pose, v, Mth.cos(a2) * outer, 0, Mth.sin(a2) * outer, 0, 1, 0, color);
            vertex(pose, v, Mth.cos(a2) * inner, 0, Mth.sin(a2) * inner, 0, 1, 0, color);
            // face de baixo (ordem inversa)
            vertex(pose, v, Mth.cos(a2) * inner, 0, Mth.sin(a2) * inner, 0, -1, 0, color);
            vertex(pose, v, Mth.cos(a2) * outer, 0, Mth.sin(a2) * outer, 0, -1, 0, color);
            vertex(pose, v, Mth.cos(a1) * outer, 0, Mth.sin(a1) * outer, 0, -1, 0, color);
            vertex(pose, v, Mth.cos(a1) * inner, 0, Mth.sin(a1) * inner, 0, -1, 0, color);
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer v, float x, float y, float z,
                               float nx, float ny, float nz, int color) {
        v.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }

    /// Muda a transparencia de uma cor (alpha: 0 = invisivel, 255 = opaco).
    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    /// A esfera e as orbitas saem fora do bloco: aumenta a zona em que o Minecraft as desenha.
    @Override
    public AABB getRenderBoundingBox(GravityCoreBlockEntity core) {
        return new AABB(core.getBlockPos()).inflate(3);
    }
}
