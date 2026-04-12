package com.ea.bfaq.client.gui;

import com.ea.bfaq.BF1Q;
import com.ea.bfaq.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;

public class ClassSelectionScreen extends Screen
{
    private static final ResourceLocation BACKGROUND = new ResourceLocation(BF1Q.MODID, "textures/gui/class_selection.png");
    
    private static final String[] CLASS_NAMES = {"assault", "medic", "recon", "support"};
    private static final String[] CLASS_DISPLAY_NAMES = {"突击兵", "医疗兵", "侦察兵", "支援兵"};
    
    public ClassSelectionScreen()
    {
        super(Component.literal("选择你的兵种"));
    }
    
    @Override
    protected void init()
    {
        super.init();
        
        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;
        
        for (int i = 0; i < CLASS_NAMES.length; i++)
        {
            final String className = CLASS_NAMES[i];
            final String displayName = CLASS_DISPLAY_NAMES[i];
            
            Button button = Button.builder(
                Component.literal(displayName),
                btn -> selectClass(className)
            ).bounds(centerX - 60, startY + i * 25, 120, 20).build();
            
            this.addRenderableWidget(button);
        }
    }
    
    private void selectClass(String className)
    {
        NetworkHandler.INSTANCE.send(
            PacketDistributor.SERVER.noArg(),
            new NetworkHandler.ClassSelectionResponsePacket(className)
        );
        this.onClose();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(guiGraphics);
        
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 40, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "请选择你的兵种（此选择将永久保存）", this.width / 2, 60, 0xAAAAAA);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean shouldCloseOnEsc()
    {
        return false;
    }
}
