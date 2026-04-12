package com.ea.bfaq.client;

import com.ea.bfaq.BF1Q;
import com.ea.bfaq.SoundEvents;
import com.ea.bfaq.mark.MarkData;
import com.ea.bfaq.mark.MarkManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@Mod.EventBusSubscriber(modid = BF1Q.MODID, value = Dist.CLIENT)

public class MarkRenderer
{
    private static final float MARK_HEIGHT_OFFSET = 0.5f;
    private static final float MIN_SCALE = 0.3f;
    private static final float MAX_SCALE = 0.8f;
    private static final float MAX_DISTANCE = 64.0f;
    private static final float DRAGON_SCALE_MULTIPLIER = 2.5f;
    private static final float GHAST_SCALE_MULTIPLIER = 1.8f;
    private static final Map<UUID, TeamMarkType> teamMarkTypes = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();
    private static final Map<UUID, Long> lastSupportMarkSoundTime = new ConcurrentHashMap<>();
    private static final long SOUND_COOLDOWN_MS = 2000;
    
    private enum TeamMarkType
    {
        ASSAULT("assault_team"),
        MEDIC("medic_team"),
        RECON("recon_team"),
        SUPPORT("support_team");
        
        private final String textureName;
        
        TeamMarkType(String textureName)
        {
            this.textureName = textureName;
        }
        
        public String getTextureName()
        {
            return textureName;
        }
        
        public static TeamMarkType random()
        {
            TeamMarkType[] types = values();
            return types[RANDOM.nextInt(types.length)];
        }
    }

    private static boolean isSameScoreboardTeam(net.minecraft.world.entity.player.Player player1, net.minecraft.world.entity.player.Player player2)
    {
        net.minecraft.world.scores.Team team1 = player1.getTeam();
        net.minecraft.world.scores.Team team2 = player2.getTeam();
        if (team1 == null || team2 == null)
        {
            return false;
        }
        return team1.getName().equals(team2.getName());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY)
        {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }
        
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = mc.renderBuffers().bufferSource();
        float partialTick = event.getPartialTick();
        
        Map<UUID, MarkData> marks = MarkManager.getInstance().getAllMarksMap();
        boolean isTeammateSystemActive = com.ea.bfaq.network.NetworkHandler.ClientTeammateManager.hasSelection();
        
        mc.level.entitiesForRendering().forEach(entity -> {
            if (entity != mc.player && entity instanceof net.minecraft.world.entity.player.Player targetPlayer)
            {
                UUID targetUUID = targetPlayer.getUUID();
                String playerClass = com.ea.bfaq.network.NetworkHandler.ClientClassManager.getPlayerClass(targetUUID);
                boolean isTeammate = isTeammateSystemActive && com.ea.bfaq.network.NetworkHandler.ClientTeammateManager.isSelectedTeammate(targetUUID);
                boolean isSameTeam = isSameScoreboardTeam(mc.player, targetPlayer);
                boolean hasManualMark = marks.containsKey(targetUUID);
                
                if (playerClass != null)
                {
                    if (isTeammate)
                    {
                        renderClassMark(poseStack, bufferSource, targetPlayer, playerClass, true, partialTick);
                    }
                    else if (isSameTeam)
                    {
                        renderClassMarkFriendly(poseStack, bufferSource, targetPlayer, playerClass, partialTick);
                    }
                    else if (hasManualMark)
                    {
                        MarkData mark = marks.get(targetUUID);
                        renderClassMark(poseStack, bufferSource, targetPlayer, playerClass, mark.isFriendly(), partialTick);
                    }
                }
                else
                {
                    if (hasManualMark)
                    {
                        MarkData mark = marks.get(targetUUID);
                        renderMark(poseStack, bufferSource, targetPlayer, mark.getMarkType(), mark.isFriendly(), partialTick);
                    }
                    else if (isTeammate)
                    {
                        renderTeamMark(poseStack, bufferSource, targetPlayer, partialTick);
                    }
                    else if (isSameTeam)
                    {
                        renderFriendlyMark(poseStack, bufferSource, targetPlayer, partialTick);
                    }
                }
            }
            else if (entity instanceof LivingEntity && marks.containsKey(entity.getUUID()))
            {
                MarkData mark = marks.get(entity.getUUID());
                renderMark(poseStack, bufferSource, (LivingEntity) entity, mark.getMarkType(), mark.isFriendly(), partialTick);
            }
            else if (marks.containsKey(entity.getUUID()))
            {
                // 支持非生物实体，如箱子矿车
                MarkData mark = marks.get(entity.getUUID());
                renderEntityMark(poseStack, bufferSource, entity, mark.getMarkType(), mark.isFriendly(), partialTick);
            }
        });
    }
    
    private static void renderMark(PoseStack poseStack, MultiBufferSource bufferSource, LivingEntity entity, MarkData.MarkType markType, boolean isFriendly, float partialTick)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            return;
        }

        String textureName = markType.getTextureName() + (isFriendly ? "_ally" : "");
        ResourceLocation texture = new ResourceLocation(BF1Q.MODID, "textures/mark/" + textureName + ".png");
        
        double distance = mc.player.distanceTo(entity);
        float scale = calculateScale(distance, entity);
        
        double entityX = entity.xOld + (entity.getX() - entity.xOld) * partialTick;
        double entityY = entity.yOld + (entity.getY() - entity.yOld) * partialTick;
        double entityZ = entity.zOld + (entity.getZ() - entity.zOld) * partialTick;
        
        poseStack.pushPose();
        
        // 为特殊实体增加额外的高度偏移
        float additionalOffset = 0.0f;
        if (entity instanceof net.minecraft.world.entity.monster.Witch)
        {
            additionalOffset = 0.3f;
        }
        else if (entity instanceof net.minecraft.world.entity.monster.ElderGuardian)
        {
            additionalOffset = 0.5f;
        }
        else if (entity instanceof net.minecraft.world.entity.monster.Ghast)
        {
            additionalOffset = 1.2f;
        }
        else if (entity instanceof net.minecraft.world.entity.animal.sniffer.Sniffer)
        {
            additionalOffset = 0.4f;
        }
        else if (entity instanceof net.minecraft.world.entity.monster.Ravager)
        {
            additionalOffset = 0.3f;
        }
        
        poseStack.translate(entityX - mc.gameRenderer.getMainCamera().getPosition().x, 
                           entityY - mc.gameRenderer.getMainCamera().getPosition().y + entity.getBbHeight() + MARK_HEIGHT_OFFSET + additionalOffset, 
                           entityZ - mc.gameRenderer.getMainCamera().getPosition().z);
        
        poseStack.mulPose(Axis.YP.rotationDegrees(-mc.gameRenderer.getMainCamera().getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(mc.gameRenderer.getMainCamera().getXRot()));
        
        poseStack.scale(scale, scale, scale);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        Matrix4f matrix = poseStack.last().pose();
        
        float size = 1.0f;
        int packedLight = 0xF000F0;
        
        buffer.vertex(matrix, -size, -size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(0.0f, 1.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, size, -size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(1.0f, 1.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, size, size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(1.0f, 0.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, -size, size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(0.0f, 0.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();

        poseStack.popPose();
    }
    
    private static void renderEntityMark(PoseStack poseStack, MultiBufferSource bufferSource, Entity entity, MarkData.MarkType markType, boolean isFriendly, float partialTick)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            return;
        }

        String textureName = markType.getTextureName() + (isFriendly ? "_ally" : "");
        ResourceLocation texture = new ResourceLocation(BF1Q.MODID, "textures/mark/" + textureName + ".png");
        
        double distance = mc.player.distanceTo(entity);
        float scale = calculateEntityScale(distance);
        
        double entityX = entity.xOld + (entity.getX() - entity.xOld) * partialTick;
        double entityY = entity.yOld + (entity.getY() - entity.yOld) * partialTick;
        double entityZ = entity.zOld + (entity.getZ() - entity.zOld) * partialTick;
        
        poseStack.pushPose();
        
        // 为矿车增加额外的高度偏移
        float additionalOffset = 0.5f;
        
        poseStack.translate(entityX - mc.gameRenderer.getMainCamera().getPosition().x, 
                           entityY - mc.gameRenderer.getMainCamera().getPosition().y + entity.getBbHeight() + MARK_HEIGHT_OFFSET + additionalOffset, 
                           entityZ - mc.gameRenderer.getMainCamera().getPosition().z);
        
        poseStack.mulPose(Axis.YP.rotationDegrees(-mc.gameRenderer.getMainCamera().getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(mc.gameRenderer.getMainCamera().getXRot()));
        
        poseStack.scale(scale, scale, scale);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        Matrix4f matrix = poseStack.last().pose();
        
        float size = 1.0f;
        int packedLight = 0xF000F0;
        
        buffer.vertex(matrix, -size, -size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(0.0f, 1.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, size, -size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(1.0f, 1.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, size, size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(1.0f, 0.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, -size, size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(0.0f, 0.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();

        poseStack.popPose();
    }
    
    private static float calculateEntityScale(double distance)
    {
        float baseScale;
        
        if (distance >= MAX_DISTANCE)
        {
            baseScale = MAX_SCALE;
        }
        else
        {
            float ratio = (float) (distance / MAX_DISTANCE);
            baseScale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * ratio;
        }
        
        return baseScale;
    }
    
    private static void renderClassMark(PoseStack poseStack, MultiBufferSource bufferSource, net.minecraft.world.entity.player.Player player, String className, boolean isFriendly, float partialTick)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            return;
        }

        String textureName;
        if (isFriendly)
        {
            textureName = className + "_team";
        }
        else
        {
            textureName = className;
        }
        
        ResourceLocation texture = new ResourceLocation(BF1Q.MODID, "textures/mark/" + textureName + ".png");
        
        double distance = mc.player.distanceTo(player);
        float scale = calculateScale(distance, player);
        
        double entityX = player.xOld + (player.getX() - player.xOld) * partialTick;
        double entityY = player.yOld + (player.getY() - player.yOld) * partialTick;
        double entityZ = player.zOld + (player.getZ() - player.zOld) * partialTick;
        
        poseStack.pushPose();
        
        // 为玩家增加额外的高度偏移，避免挡住玩家名字
        float additionalOffset = 0.4f;
        
        poseStack.translate(entityX - mc.gameRenderer.getMainCamera().getPosition().x, 
                           entityY - mc.gameRenderer.getMainCamera().getPosition().y + player.getBbHeight() + MARK_HEIGHT_OFFSET + additionalOffset, 
                           entityZ - mc.gameRenderer.getMainCamera().getPosition().z);
        
        poseStack.mulPose(Axis.YP.rotationDegrees(-mc.gameRenderer.getMainCamera().getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(mc.gameRenderer.getMainCamera().getXRot()));
        
        poseStack.scale(scale, scale, scale);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        Matrix4f matrix = poseStack.last().pose();
        
        float size = 1.0f;
        int packedLight = 0xF000F0;
        
        buffer.vertex(matrix, -size, -size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(0.0f, 1.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, size, -size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(1.0f, 1.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, size, size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(1.0f, 0.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, -size, size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(0.0f, 0.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();

        poseStack.popPose();
    }
    
    private static void renderClassMarkFriendly(PoseStack poseStack, MultiBufferSource bufferSource, net.minecraft.world.entity.player.Player player, String className, float partialTick)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            return;
        }

        String textureName = className + "_ally";
        ResourceLocation texture = new ResourceLocation(BF1Q.MODID, "textures/mark/" + textureName + ".png");
        
        double distance = mc.player.distanceTo(player);
        float scale = calculateScale(distance, player);
        
        double entityX = player.xOld + (player.getX() - player.xOld) * partialTick;
        double entityY = player.yOld + (player.getY() - player.yOld) * partialTick;
        double entityZ = player.zOld + (player.getZ() - player.zOld) * partialTick;
        
        poseStack.pushPose();
        
        // 为玩家增加额外的高度偏移，避免挡住玩家名字
        float additionalOffset = 0.4f;
        
        poseStack.translate(entityX - mc.gameRenderer.getMainCamera().getPosition().x, 
                           entityY - mc.gameRenderer.getMainCamera().getPosition().y + player.getBbHeight() + MARK_HEIGHT_OFFSET + additionalOffset, 
                           entityZ - mc.gameRenderer.getMainCamera().getPosition().z);
        
        poseStack.mulPose(Axis.YP.rotationDegrees(-mc.gameRenderer.getMainCamera().getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(mc.gameRenderer.getMainCamera().getXRot()));
        
        poseStack.scale(scale, scale, scale);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        Matrix4f matrix = poseStack.last().pose();
        
        float size = 1.0f;
        int packedLight = 0xF000F0;
        
        buffer.vertex(matrix, -size, -size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(0.0f, 1.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, size, -size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(1.0f, 1.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, size, size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(1.0f, 0.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, -size, size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(0.0f, 0.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();

        poseStack.popPose();
    }
    
    private static void renderTeamMark(PoseStack poseStack, MultiBufferSource bufferSource, net.minecraft.world.entity.player.Player player, float partialTick)
    {
        TeamMarkType markType = teamMarkTypes.computeIfAbsent(player.getUUID(), uuid -> TeamMarkType.random());
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            return;
        }

        String textureName = markType.getTextureName();
        ResourceLocation texture = new ResourceLocation(BF1Q.MODID, "textures/mark/" + textureName + ".png");
        
        double distance = mc.player.distanceTo(player);
        float scale = calculateScale(distance, player);
        
        double entityX = player.xOld + (player.getX() - player.xOld) * partialTick;
        double entityY = player.yOld + (player.getY() - player.yOld) * partialTick;
        double entityZ = player.zOld + (player.getZ() - player.zOld) * partialTick;
        
        poseStack.pushPose();
        
        // 为玩家增加额外的高度偏移，避免挡住玩家名字
        float additionalOffset = 0.4f;
        
        poseStack.translate(entityX - mc.gameRenderer.getMainCamera().getPosition().x, 
                           entityY - mc.gameRenderer.getMainCamera().getPosition().y + player.getBbHeight() + MARK_HEIGHT_OFFSET + additionalOffset, 
                           entityZ - mc.gameRenderer.getMainCamera().getPosition().z);
        
        poseStack.mulPose(Axis.YP.rotationDegrees(-mc.gameRenderer.getMainCamera().getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(mc.gameRenderer.getMainCamera().getXRot()));
        
        poseStack.scale(scale, scale, scale);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        Matrix4f matrix = poseStack.last().pose();
        
        float size = 1.0f;
        int packedLight = 0xF000F0;
        
        buffer.vertex(matrix, -size, -size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(0.0f, 1.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, size, -size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(1.0f, 1.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, size, size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(1.0f, 0.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, -size, size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(0.0f, 0.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();

        poseStack.popPose();
    }
    
    private static void renderFriendlyMark(PoseStack poseStack, MultiBufferSource bufferSource, net.minecraft.world.entity.player.Player player, float partialTick)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            return;
        }

        ResourceLocation texture = new ResourceLocation(BF1Q.MODID, "textures/mark/assault_ally.png");
        
        double distance = mc.player.distanceTo(player);
        float scale = calculateScale(distance, player);
        
        double entityX = player.xOld + (player.getX() - player.xOld) * partialTick;
        double entityY = player.yOld + (player.getY() - player.yOld) * partialTick;
        double entityZ = player.zOld + (player.getZ() - player.zOld) * partialTick;
        
        poseStack.pushPose();
        
        // 为玩家增加额外的高度偏移，避免挡住玩家名字
        float additionalOffset = 0.4f;
        
        poseStack.translate(entityX - mc.gameRenderer.getMainCamera().getPosition().x, 
                           entityY - mc.gameRenderer.getMainCamera().getPosition().y + player.getBbHeight() + MARK_HEIGHT_OFFSET + additionalOffset, 
                           entityZ - mc.gameRenderer.getMainCamera().getPosition().z);
        
        poseStack.mulPose(Axis.YP.rotationDegrees(-mc.gameRenderer.getMainCamera().getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(mc.gameRenderer.getMainCamera().getXRot()));
        
        poseStack.scale(scale, scale, scale);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        Matrix4f matrix = poseStack.last().pose();
        
        float size = 1.0f;
        int packedLight = 0xF000F0;
        
        buffer.vertex(matrix, -size, -size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(0.0f, 1.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, size, -size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(1.0f, 1.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, size, size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(1.0f, 0.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();
              
        buffer.vertex(matrix, -size, size, 0.0f)
              .color(255, 255, 255, 255)
              .uv(0.0f, 0.0f)
              .overlayCoords(OverlayTexture.NO_OVERLAY)
              .uv2(packedLight)
              .normal(0, 0, 1)
              .endVertex();

        poseStack.popPose();
    }
    
    private static float calculateScale(double distance, LivingEntity entity)
    {
        float baseScale;
        
        if (distance >= MAX_DISTANCE)
        {
            baseScale = MAX_SCALE;
        }
        else
        {
            float ratio = (float) (distance / MAX_DISTANCE);
            baseScale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * ratio;
        }
        
        if (entity instanceof EnderDragon)
        {
            return baseScale * DRAGON_SCALE_MULTIPLIER;
        }
        
        if (entity instanceof Ghast)
        {
            return baseScale * GHAST_SCALE_MULTIPLIER;
        }
        
        return baseScale;
    }
    
    private static void playSupportMarkSound(UUID playerUUID)
    {
        long currentTime = System.currentTimeMillis();
        Long lastSoundTime = lastSupportMarkSoundTime.get(playerUUID);
        
        if (lastSoundTime == null || currentTime - lastSoundTime > SOUND_COOLDOWN_MS)
        {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null)
            {
                String playerClass = com.ea.bfaq.network.NetworkHandler.ClientClassManager.getPlayerClass(playerUUID);
                if (playerClass != null)
                {
                    switch (playerClass)
                    {
                        case "assault":
                            com.ea.bfaq.client.sound.AssaultSound.play();
                            break;
                        case "medic":
                            com.ea.bfaq.client.sound.MedicSound.play();
                            break;
                        case "support":
                            com.ea.bfaq.client.sound.SupportSound.play();
                            break;
                        case "recon":
                            com.ea.bfaq.client.sound.ReconSound.play();
                            break;
                    }
                }
                
                lastSupportMarkSoundTime.put(playerUUID, currentTime);
            }
        }
    }
}
