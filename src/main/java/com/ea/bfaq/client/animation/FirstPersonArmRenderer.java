package com.ea.bfaq.client.animation;

// 关注b站UID:545778318谢谢喵
// 关注b站UID:1157669161谢谢喵

import com.ea.bfaq.BF1Q;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = BF1Q.MODID, value = Dist.CLIENT)
public class FirstPersonArmRenderer
{
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player instanceof AbstractClientPlayer))
        {
            return;
        }
        
        AnimationController controller = AnimationController.getInstance();
        if (!controller.isPlaying())
        {
            return;
        }
        
        if (controller.getAnimationTime() >= controller.getAnimationLength() - 0.05f)
        {
            return;
        }
        
        if (event.getHand() != net.minecraft.world.InteractionHand.OFF_HAND)
        {
            return;
        }
        
        controller.update();
        
        event.setCanceled(true);
        
        AbstractClientPlayer player = (AbstractClientPlayer) mc.player;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();
        
        renderLeftArmWithAnimation(player, poseStack, bufferSource, packedLight);
    }
    
    private static void renderLeftArmWithAnimation(AbstractClientPlayer player, 
                                                    PoseStack poseStack, 
                                                    MultiBufferSource bufferSource,
                                                    int packedLight)
    {
        AnimationController controller = AnimationController.getInstance();
        
        PlayerRenderer playerRenderer = (PlayerRenderer) Minecraft.getInstance()
                .getEntityRenderDispatcher().getRenderer(player);
        PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();
        
        ResourceLocation skinLocation = playerRenderer.getTextureLocation(player);
        RenderType renderType = RenderType.entityTranslucentCull(skinLocation);
        
        Vector3f rotation = controller.getLeftArmRotation();
        Vector3f position = controller.getLeftArmPosition();
        
        ModelPart leftArm = playerModel.leftArm;
        ModelPart leftSleeve = playerModel.leftSleeve;
        
        poseStack.pushPose();
        
        poseStack.translate(-0.55, 0.1, -0.1);
        
        poseStack.scale(0.7F, 0.7F, 0.7F);
        
        poseStack.translate(
            position.x() * 0.0625F,
            position.y() * 0.0625F,
            position.z() * 0.0625F
        );
        
        // 移除初始旋转，使用原始模型方向
        
        Quaternionf quaternion = new Quaternionf();
        quaternion.rotationXYZ(
            (float) Math.toRadians(rotation.x()),
            (float) Math.toRadians(rotation.y()),
            (float) Math.toRadians(rotation.z())
        );
        poseStack.mulPose(quaternion);
        
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        leftArm.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        leftSleeve.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        
        poseStack.popPose();
    }
}
