package de.artemis.cyberneticenhancements.client;

import com.mojang.blaze3d.vertex.*;
import de.artemis.cyberneticenhancements.common.cyberware.CyberpsychosisClientState;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.AbstractVillager;

public final class PsychosisPresentation {
    private static float intensity;
    private static long lastFrame;

    public static boolean obscures(LivingEntity entity) {
        var mc = Minecraft.getInstance();
        return CyberpsychosisClientState.isControlLocked() && amount()>.01F && mc.player != null && entity!=mc.player && mc.player.isAlive()
                && (entity instanceof Player || entity instanceof AbstractCityNpcEntity || entity instanceof AbstractVillager);
    }

    public static float amount() {
        var mc = Minecraft.getInstance();
        long now = net.minecraft.Util.getMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(.1F, (now - lastFrame) / 1000F);
        lastFrame = now;
        if (mc.player == null || !mc.player.isAlive()) { intensity = 0; return 0; }
        float target = CyberpsychosisClientState.isControlLocked() ? PsychosisIntroClient.buildup() : 0;
        intensity += (target - intensity) * (1 - (float)Math.exp(-dt / .18));
        return intensity;
    }

    public static void renderFrame(GuiGraphics g) {
        amount();
        if (intensity < .01F) return;
        int w = g.guiWidth(), h = g.guiHeight();
        for (int i = 0; i < Math.min(80, Math.min(w,h) / 5); i++) {
            int depth = Math.min(80, Math.min(w,h) / 5);
            int alpha = (int)(100 * Math.pow(1F - (float)i/depth, 2) * intensity);
            int color = alpha << 24 | 0x910A28;
            g.fill(RenderType.guiOverlay(), i, i, w-i, i+1, color); g.fill(RenderType.guiOverlay(), i, h-i-1, w-i, h-i, color);
            g.fill(RenderType.guiOverlay(), i, i, i+1, h-i, color); g.fill(RenderType.guiOverlay(), w-i-1, i, w-i, h-i, color);
        }
        // Stable short bursts rather than per-frame noise; keep the center of vision clear.
        long phase = net.minecraft.Util.getMillis() / 110;
        for (int i = 0; i < 64; i++) {
            int seed = (int)(phase * 1103515245L + i * 12345L);
            int y = Math.floorMod(seed >>> 5, h), length = 12 + Math.floorMod(seed >>> 13, Math.max(1,w / 7));
            int color = (int)(intensity * (35 + Math.floorMod(seed, 80))) << 24 | (i % 3 == 0 ? 0x42DFDC : 0xFF3150);
            if (i % 2 == 0) g.fill(RenderType.guiOverlay(), 0, y, Math.min(length, w), y+1+Math.floorMod(seed >>> 9, 4), color);
            else g.fill(RenderType.guiOverlay(), Math.max(0, w-length), y, w, y+1, color);
        }
        for (int i = 0; i < 12; i++) {
            int seed = (int)(phase * 7919 + i * 104729);
            int x = Math.floorMod(seed, Math.max(1,w-30));
            int length = 12 + Math.floorMod(seed >>> 8, Math.max(1,w/5));
            int y = i % 2 == 0 ? Math.floorMod(seed >>> 5, Math.max(1,h/9)) : h-1-Math.floorMod(seed >>> 5,Math.max(1,h/9));
            g.fill(RenderType.guiOverlay(), x,y,Math.min(w,x+length),Math.min(h,y+2),(int)(90*intensity)<<24|0xDB244B);
            g.fill(RenderType.guiOverlay(), Math.max(0,x-3),Math.max(0,y-2),Math.min(w,x+length-3),y,(int)(45*intensity)<<24|0x49C8D4);
        }
    }

    public static MultiBufferSource silhouette(MultiBufferSource buffers) {
        return type -> {
            RenderType shadow = type;
            if (type instanceof de.artemis.cyberneticenhancements.mixin.CorpseRenderTypes.CompositeAccess access) {
                var state = (de.artemis.cyberneticenhancements.mixin.CorpseRenderTypes.StateAccess)(Object)access.cyberneticenhancements$state();
                var texture = (de.artemis.cyberneticenhancements.mixin.CorpseRenderTypes.TextureAccess)(Object)state.cyberneticenhancements$texture();
                var location = texture.cyberneticenhancements$location();
                if(location.isPresent()) shadow = RenderType.entityTranslucent(location.get());
            }
            return new BlackPixels(buffers.getBuffer(shadow), net.minecraft.Util.getMillis()/240D, amount());
        };
    }

    public static void face(LivingEntity entity, PoseStack pose, MultiBufferSource buffers) {
        if (de.artemis.cyberneticenhancements.client.render.RipperStationBlockEntityRenderer.isShadowPass()) return;
        pose.pushPose();
        pose.translate(0, entity.getEyeHeight(), 0);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        var vertices = buffers.getBuffer(RenderType.lightning());
        double time = net.minecraft.Util.getMillis() / 1000D + entity.getId() * .73;
        var m = pose.last().pose();
        // A fractured, orbiting veil: no eyes, mouth, or recognizable facial features.
        for (int i=0;i<28;i++) {
            double angle = i * 2.39996 + time * (i%2==0 ? .6 : -.45);
            float radius = .22F + (i%5)*.025F;
            float x = (float)Math.cos(angle)*radius;
            float y = (float)Math.sin(angle)*radius*.85F;
            float size = .018F + (i%4)*.009F;
            int alpha = 45 + (int)(45*(1+Math.sin(time*3+i)));
            int color = alpha<<24 | (i%7==0 ? 0x657D89 : 0x16131F);
            quad(vertices,m,x,y,x+size*2,y+size,.38F,color);
        }
        for(int i=0;i<9;i++) {
            float y = -.19F+i*.045F;
            float offset = (float)Math.sin(time*2.3+i*1.7)*.025F;
            float width = .23F-(Math.abs(i-4)*.021F);
            quad(vertices,m,-width+offset,y,width+offset,y+.049F,.37F,0xD0080810);
        }
        pose.popPose();
    }

    private static void quad(VertexConsumer v, org.joml.Matrix4f m, float x,float y,float right,float top,float z,int color) {
        color = (int)((color>>>24)*intensity)<<24 | color&0xFFFFFF;
        v.addVertex(m,x,y,z).setColor(color); v.addVertex(m,x,top,z).setColor(color);
        v.addVertex(m,right,top,z).setColor(color); v.addVertex(m,right,y,z).setColor(color);
    }

    private record BlackPixels(VertexConsumer delegate, double time, float weight) implements VertexConsumer {
        public VertexConsumer addVertex(float x,float y,float z) { float wave=(float)Math.sin(y*23+time)*.012F*weight; delegate.addVertex(x+wave,y,z+wave*.5F); return this; }
        public VertexConsumer setColor(int r,int g,int b,int a) { delegate.setColor((int)(r+(9-r)*weight),(int)(g+(10-g)*weight),(int)(b+(17-b)*weight),(int)(a*(1-weight*35/255))); return this; }
        public VertexConsumer setUv(float u,float v) { delegate.setUv(u,v); return this; }
        public VertexConsumer setUv1(int u,int v) { delegate.setUv1(u,v); return this; }
        public VertexConsumer setUv2(int u,int v) { delegate.setUv2(u,v); return this; }
        public VertexConsumer setNormal(float x,float y,float z) { delegate.setNormal(x,y,z); return this; }
    }
    private PsychosisPresentation() {}
}
