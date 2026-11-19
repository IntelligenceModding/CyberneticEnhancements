package de.artemis.cyberneticenhancements.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;

public final class PsychosisTimerOverlay {
    public static int bottom() {
        return !GameplayHudVisibility.shows(GameplayHudVisibility.Element.PSYCHOSIS_TIMER, ClientConfig.SHOW_CYBERPSYCHOSIS_TIMER.get())
                || PsychosisOverlayClientState.get().entries().isEmpty() ? 0 : 3 + 38 * PsychosisOverlayClientState.get().entries().size();
    }
    public static void render(GuiGraphics g) {
        var mc = Minecraft.getInstance();
        if (bottom() == 0) return;
        int width=Math.min(280,g.guiWidth()-16),x=(g.guiWidth()-width)/2,y=3;
        for(var entry:PsychosisOverlayClientState.get().entries()) {
            CyberwareHudOverlay.drawCardFrame(g,x,y,width,34);
            float ratio=PsychosisOverlayClientState.getRatio(entry);
            String time=(int)Math.ceil(ratio*entry.totalSeconds())+"s";
            int timeX=x+width-8-mc.font.width(time);
            String title=Component.translatable("hud.cyberneticenhancements.psychosis").getString();
            g.drawString(mc.font,mc.font.plainSubstrByWidth(title,Math.max(1,timeX-x-20)),x+8,y+5,UiRenderer.TEXT,true);
            g.drawString(mc.font,time,timeX,y+5,0xFFE16D7A,true);
            int barX=x+8,barY=y+22,barWidth=width-16;
            g.fill(barX,barY,barX+barWidth,barY+5,0xFF30232D);
            int filled=Math.clamp(Math.round(barWidth*ratio),0,barWidth);
            if(filled>0){g.fill(barX,barY,barX+filled,barY+5,0xFFB7475B);g.fill(barX,barY,barX+filled,barY+1,0xFFE16D7A);}
            for(int n=1;n<10;n++){int notch=barX+barWidth*n/10;g.fill(notch,barY+1,notch+1,barY+5,0xAA111820);}
            y+=38;
        }
    }
    private PsychosisTimerOverlay() {}
}
