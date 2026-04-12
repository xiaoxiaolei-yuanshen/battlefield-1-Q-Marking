package com.ea.bfaq.client.gui;

import com.ea.bfaq.BF1Q;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BF1Q.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TopBarGUI
{
    private static boolean showUI = false;
    private static boolean showFlamethrowerUI = false;
    private static boolean showMinecartUI = false;
    private static boolean showHopperUI = false;
    private static long uiStartTime = 0;
    private static long flamethrowerUIStartTime = 0;
    private static long minecartUIStartTime = 0;
    private static long hopperUIStartTime = 0;
    private static final long UI_DURATION = 5000; // 5秒UI持续时长
    private static final long FADE_DURATION = 450; // 0.45秒淡入淡出时长
    
    public static final IGuiOverlay TOP_BAR_OVERLAY = (gui, guiGraphics, partialTick, width, height) -> {
        // 检查UI是否应该显示
        if (showUI) {
            // 检查是否过期
            if (System.currentTimeMillis() - uiStartTime > UI_DURATION) {
                showUI = false;
            } else {
                renderTopBar(gui, guiGraphics, width, height);
            }
        }
        
        // 检查喷火兵UI是否应该显示
        if (showFlamethrowerUI) {
            // 检查是否过期
            if (System.currentTimeMillis() - flamethrowerUIStartTime > UI_DURATION) {
                showFlamethrowerUI = false;
            } else {
                renderFlamethrowerTopBar(gui, guiGraphics, width, height);
            }
        }
        
        // 检查战壕奇兵UI是否应该显示
        if (showMinecartUI) {
            // 检查是否过期
            if (System.currentTimeMillis() - minecartUIStartTime > UI_DURATION) {
                showMinecartUI = false;
            } else {
                renderMinecartTopBar(gui, guiGraphics, width, height);
            }
        }
        
        // 检查入侵者UI是否应该显示
        if (showHopperUI) {
            // 检查是否过期
            if (System.currentTimeMillis() - hopperUIStartTime > UI_DURATION) {
                showHopperUI = false;
            } else {
                renderHopperTopBar(gui, guiGraphics, width, height);
            }
        }
    };
    
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event)
    {
        event.registerAboveAll("top_bar", TOP_BAR_OVERLAY);
    }
    
    public static void setShowUI(boolean show) {
        showUI = show;
        if (show) {
            // 显示哨兵UI时，隐藏喷火兵UI
            showFlamethrowerUI = false;
            // 记录UI显示的开始时间
            uiStartTime = System.currentTimeMillis();
        }
    }
    
    public static void setShowFlamethrowerUI(boolean show) {
        showFlamethrowerUI = show;
        if (show) {
            // 显示喷火兵UI时，隐藏哨兵UI
            showUI = false;
            showMinecartUI = false;
            // 记录喷火兵UI显示的开始时间
            flamethrowerUIStartTime = System.currentTimeMillis();
        }
    }
    
    public static void setShowMinecartUI(boolean show) {
        showMinecartUI = show;
        if (show) {
            // 显示战壕奇兵UI时，隐藏其他UI
            showUI = false;
            showFlamethrowerUI = false;
            showHopperUI = false;
            // 记录战壕奇兵UI显示的开始时间
            minecartUIStartTime = System.currentTimeMillis();
        }
    }
    
    public static void setShowHopperUI(boolean show) {
        showHopperUI = show;
        if (show) {
            // 显示入侵者UI时，隐藏其他UI
            showUI = false;
            showFlamethrowerUI = false;
            showMinecartUI = false;
            // 记录入侵者UI显示的开始时间
            hopperUIStartTime = System.currentTimeMillis();
        }
    }
    
    private static void renderTopBar(ForgeGui gui, GuiGraphics guiGraphics, int width, int height)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null)
        {
            return;
        }
        
        // 计算当前UI显示的时间
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - uiStartTime;
        
        // 计算透明度（淡入淡出效果）
        float alpha = 1.0F;
        if (elapsedTime < FADE_DURATION) {
            // 淡入
            alpha = (float) elapsedTime / FADE_DURATION;
        } else if (elapsedTime > UI_DURATION - FADE_DURATION) {
            // 淡出
            alpha = 1.0F - (float) (elapsedTime - (UI_DURATION - FADE_DURATION)) / FADE_DURATION;
        }
        
        // 确保透明度在0-1之间
        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        
        // 绘制像boss血条一样的长方形
        int barHeight = 20; // 增加高度
        int barWidth = width / 3; // 宽度为屏幕的三分之一
        int barX = width / 3; // 居中显示
        int barY = 30; // 向下移动一点
        
        // 计算带透明度的颜色（更深的背景）
        int color = (int) (0x60 * alpha) << 24 | 0x222222;
        
        // 绘制长方形，使用更深的颜色，透明度调小
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, color);
        
        // 在中间添加白色的繁体文字，每个字添加空格
        String text = "已 發 現 哨 兵 裝 備";
        
        // 保存当前的PoseStack状态
        guiGraphics.pose().pushPose();
        
        // 计算文字位置
        int textWidth = mc.font.width(text);
        float scale = 1.5F; // 缩放因子
        float scaledTextWidth = textWidth * scale;
        float scaledTextHeight = mc.font.lineHeight * scale;
        
        // 计算居中位置
        float textX = barX + (barWidth - scaledTextWidth) / 2;
        float textY = barY + (barHeight - scaledTextHeight) / 2;
        
        // 移动到文字位置并缩放
        guiGraphics.pose().translate(textX, textY, 0);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        
        // 计算带透明度的文字颜色
        int textColor = (int) (255 * alpha) << 24 | 0xFFFFFF;
        
        // 绘制文字
        guiGraphics.drawString(mc.font, text, 0, 0, textColor);
        
        // 恢复PoseStack状态
        guiGraphics.pose().popPose();
    }
    
    private static void renderFlamethrowerTopBar(ForgeGui gui, GuiGraphics guiGraphics, int width, int height)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null)
        {
            return;
        }
        
        // 计算当前UI显示的时间
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - flamethrowerUIStartTime;
        
        // 计算透明度（淡入淡出效果）
        float alpha = 1.0F;
        if (elapsedTime < FADE_DURATION) {
            // 淡入
            alpha = (float) elapsedTime / FADE_DURATION;
        } else if (elapsedTime > UI_DURATION - FADE_DURATION) {
            // 淡出
            alpha = 1.0F - (float) (elapsedTime - (UI_DURATION - FADE_DURATION)) / FADE_DURATION;
        }
        
        // 确保透明度在0-1之间
        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        
        // 绘制像boss血条一样的长方形
        int barHeight = 20; // 高度
        int barWidth = width / 3; // 宽度为屏幕的三分之一
        int barX = width / 3; // 居中显示
        int barY = 30; // 向下移动一点
        
        // 计算带透明度的颜色（更深的背景）
        int color = (int) (0x60 * alpha) << 24 | 0x222222;
        
        // 绘制长方形，使用更深的颜色，透明度调小
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, color);
        
        // 在中间添加白色的繁体文字，每个字添加空格
        String text = "已 發 現 噴 火 兵 裝 備";
        
        // 保存当前的PoseStack状态
        guiGraphics.pose().pushPose();
        
        // 计算文字位置
        int textWidth = mc.font.width(text);
        float scale = 1.5F; // 缩放因子
        float scaledTextWidth = textWidth * scale;
        float scaledTextHeight = mc.font.lineHeight * scale;
        
        // 计算居中位置
        float textX = barX + (barWidth - scaledTextWidth) / 2;
        float textY = barY + (barHeight - scaledTextHeight) / 2;
        
        // 移动到文字位置并缩放
        guiGraphics.pose().translate(textX, textY, 0);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        
        // 计算带透明度的文字颜色
        int textColor = (int) (255 * alpha) << 24 | 0xFFFFFF;
        
        // 绘制文字
        guiGraphics.drawString(mc.font, text, 0, 0, textColor);
        
        // 恢复PoseStack状态
        guiGraphics.pose().popPose();
    }
    
    private static void renderMinecartTopBar(ForgeGui gui, GuiGraphics guiGraphics, int width, int height)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null)
        {
            return;
        }
        
        // 计算当前UI显示的时间
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - minecartUIStartTime;
        
        // 计算透明度（淡入淡出效果）
        float alpha = 1.0F;
        if (elapsedTime < FADE_DURATION) {
            // 淡入
            alpha = (float) elapsedTime / FADE_DURATION;
        } else if (elapsedTime > UI_DURATION - FADE_DURATION) {
            // 淡出
            alpha = 1.0F - (float) (elapsedTime - (UI_DURATION - FADE_DURATION)) / FADE_DURATION;
        }
        
        // 确保透明度在0-1之间
        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        
        // 绘制像boss血条一样的长方形
        int barHeight = 20; // 高度
        int barWidth = width * 2 / 5; // 宽度为屏幕的五分之二
        int barX = width / 2 - barWidth / 2; // 居中显示
        int barY = 30; // 向下移动一点
        
        // 计算带透明度的颜色（更深的背景）
        int color = (int) (0x60 * alpha) << 24 | 0x222222;
        
        // 绘制长方形，使用更深的颜色，透明度调小
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, color);
        
        // 在中间添加白色的繁体文字，每个字添加空格
        String text = "已 發 現 戰 壕 奇 兵 裝 備";
        
        // 保存当前的PoseStack状态
        guiGraphics.pose().pushPose();
        
        // 计算文字位置
        int textWidth = mc.font.width(text);
        float scale = 1.5F; // 缩放因子
        float scaledTextWidth = textWidth * scale;
        float scaledTextHeight = mc.font.lineHeight * scale;
        
        // 计算居中位置
        float textX = barX + (barWidth - scaledTextWidth) / 2;
        float textY = barY + (barHeight - scaledTextHeight) / 2;
        
        // 移动到文字位置并缩放
        guiGraphics.pose().translate(textX, textY, 0);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        
        // 计算带透明度的文字颜色
        int textColor = (int) (255 * alpha) << 24 | 0xFFFFFF;
        
        // 绘制文字
        guiGraphics.drawString(mc.font, text, 0, 0, textColor);
        
        // 恢复PoseStack状态
        guiGraphics.pose().popPose();
    }
    
    private static void renderHopperTopBar(ForgeGui gui, GuiGraphics guiGraphics, int width, int height)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null)
        {
            return;
        }
        
        // 计算当前UI显示的时间
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - hopperUIStartTime;
        
        // 计算透明度（淡入淡出效果）
        float alpha = 1.0F;
        if (elapsedTime < FADE_DURATION) {
            // 淡入
            alpha = (float) elapsedTime / FADE_DURATION;
        } else if (elapsedTime > UI_DURATION - FADE_DURATION) {
            // 淡出
            alpha = 1.0F - (float) (elapsedTime - (UI_DURATION - FADE_DURATION)) / FADE_DURATION;
        }
        
        // 确保透明度在0-1之间
        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        
        // 绘制像boss血条一样的长方形
        int barHeight = 20; // 高度
        int barWidth = width * 2 / 5; // 宽度为屏幕的五分之二
        int barX = width / 2 - barWidth / 2; // 居中显示
        int barY = 30; // 向下移动一点
        
        // 计算带透明度的颜色（更深的背景）
        int color = (int) (0x60 * alpha) << 24 | 0x222222;
        
        // 绘制长方形，使用更深的颜色，透明度调小
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, color);
        
        // 在中间添加白色的繁体文字，每个字添加空格
        String text = "已 發 現 入 侵 者 裝 備";
        
        // 保存当前的PoseStack状态
        guiGraphics.pose().pushPose();
        
        // 计算文字位置
        int textWidth = mc.font.width(text);
        float scale = 1.5F; // 缩放因子
        float scaledTextWidth = textWidth * scale;
        float scaledTextHeight = mc.font.lineHeight * scale;
        
        // 计算居中位置
        float textX = barX + (barWidth - scaledTextWidth) / 2;
        float textY = barY + (barHeight - scaledTextHeight) / 2;
        
        // 移动到文字位置并缩放
        guiGraphics.pose().translate(textX, textY, 0);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        
        // 计算带透明度的文字颜色
        int textColor = (int) (255 * alpha) << 24 | 0xFFFFFF;
        
        // 绘制文字
        guiGraphics.drawString(mc.font, text, 0, 0, textColor);
        
        // 恢复PoseStack状态
        guiGraphics.pose().popPose();
    }
}
