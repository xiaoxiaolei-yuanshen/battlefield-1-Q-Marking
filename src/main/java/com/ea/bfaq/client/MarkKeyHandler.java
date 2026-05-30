package com.ea.bfaq.client;

// 关注b站UID:545778318谢谢喵
// 关注b站UID:1157669161谢谢喵

import com.ea.bfaq.BF1Q;
import com.ea.bfaq.mark.MarkData;
import com.ea.bfaq.mark.MarkManager;
import com.ea.bfaq.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BF1Q.MODID, value = Dist.CLIENT)
public class MarkKeyHandler
{
    private static final double MARK_RANGE = 150.0D;
    private static long lastMarkTime = 0;
    private static final long MARK_COOLDOWN_MS = 200;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        if (KeyBindings.MARK_ENEMY.consumeClick())
        {
            Entity target = getEntityLookingAt(mc.player, MARK_RANGE);
            System.out.println("[BFQ-Debug] 标记按键按下, target: " + (target != null ? target.getType().toString() : "null"));

            if (target != null && isValidTarget(target))
            {
                System.out.println("[BFQ-Debug] 目标有效, 开始处理");
                boolean isFriendly = isFriendlyTarget(target, mc.player);
                MarkData.MarkType markType = getMarkTypeForEntity(target);
                System.out.println("[BFQ-Debug] 标记类型: " + markType);
                markEntity(target, markType, isFriendly);
            }
            else
            {
                System.out.println("[BFQ-Debug] 目标无效或为空");
            }
        }
    }

    private static MarkData.MarkType getMarkTypeForEntity(Entity entity)
    {
        System.out.println("[getMarkTypeForEntity] ========== 开始 ==========");
        System.out.println("[getMarkTypeForEntity] 实体类型: " + entity.getType().toString());

        if (entity instanceof Player)
        {
            UUID playerUUID = entity.getUUID();
            String playerClass = com.ea.bfaq.network.NetworkHandler.ClientClassManager.getPlayerClass(playerUUID);
            System.out.println("[getMarkTypeForEntity] 玩家: " + entity.getName().getString() + ", UUID: " + playerUUID + ", 职业: " + (playerClass != null ? playerClass : "null"));
            if (playerClass != null)
            {
                MarkData.MarkType result = switch (playerClass) {
                    case "assault" -> MarkData.MarkType.ASSAULT;
                    case "medic" -> MarkData.MarkType.MEDIC;
                    case "recon" -> MarkData.MarkType.RECON;
                    case "support" -> MarkData.MarkType.SUPPORT;
                    default -> MarkData.MarkType.random(null);
                };
                System.out.println("[getMarkTypeForEntity] 返回: " + result);
                System.out.println("[getMarkTypeForEntity] ========== 结束 ==========");
                return result;
            }
            System.out.println("[getMarkTypeForEntity] 职业为null，随机选择");
            MarkData.MarkType randomResult = MarkData.MarkType.random(null);
            System.out.println("[getMarkTypeForEntity] 返回: " + randomResult);
            System.out.println("[getMarkTypeForEntity] ========== 结束 ==========");
            return randomResult;
        }
        
        if (entity instanceof Ravager)
        {
            System.out.println("[getMarkTypeForEntity] Ravager 返回 RAIDER");
            System.out.println("[getMarkTypeForEntity] ========== 结束 ==========");
            return MarkData.MarkType.RAIDER;
        }

        if (entity instanceof ElderGuardian || entity instanceof Guardian)
        {
            System.out.println("[getMarkTypeForEntity] Guardian 返回 TANK");
            System.out.println("[getMarkTypeForEntity] ========== 结束 ==========");
            return MarkData.MarkType.TANK;
        }

        if (entity instanceof Warden)
        {
            System.out.println("[getMarkTypeForEntity] Warden 返回 RIFLE");
            System.out.println("[getMarkTypeForEntity] ========== 结束 ==========");
            return MarkData.MarkType.RIFLE;
        }

        if (entity instanceof EnderDragon || entity instanceof Phantom || entity instanceof Ghast || entity instanceof WitherBoss)
        {
            System.out.println("[getMarkTypeForEntity] PILOT类型生物 返回 PILOT");
            System.out.println("[getMarkTypeForEntity] ========== 结束 ==========");
            return MarkData.MarkType.PILOT;
        }

        if (entity instanceof Blaze)
        {
            System.out.println("[getMarkTypeForEntity] Blaze 返回 FLAMETHROWER");
            System.out.println("[getMarkTypeForEntity] ========== 结束 ==========");
            return MarkData.MarkType.FLAMETHROWER;
        }

        if (entity instanceof IronGolem || entity instanceof SnowGolem)
        {
            System.out.println("[getMarkTypeForEntity] Golem 返回 SENTRY");
            System.out.println("[getMarkTypeForEntity] ========== 结束 ==========");
            return MarkData.MarkType.SENTRY;
        }

        if (entity instanceof Parrot)
        {
            System.out.println("[getMarkTypeForEntity] Parrot 返回 PILOT");
            System.out.println("[getMarkTypeForEntity] ========== 结束 ==========");
            return MarkData.MarkType.PILOT;
        }

        String entityName = entity.getType().toString().toLowerCase();
        System.out.println("[getMarkTypeForEntity] 生物实体名称: " + entityName);

        if (entityName.contains("creeper"))
        {
            System.out.println("[getMarkTypeForEntity] 苦力怕使用随机");
            MarkData.MarkType randomResult = MarkData.MarkType.random(null);
            System.out.println("[getMarkTypeForEntity] 返回: " + randomResult);
            System.out.println("[getMarkTypeForEntity] ========== 结束 ==========");
            return randomResult;
        }

        if (entityName.contains("hoglin") || entityName.contains("zoglin"))
        {
            System.out.println("[BFQ-Debug] Hoglin/Zoglin 返回 RAIDER");
            return MarkData.MarkType.RAIDER;
        }

        if (entityName.contains("horse") || entityName.contains("llama") || entityName.contains("mule") ||
            entityName.contains("donkey") || entityName.contains("strider") || entityName.contains("trader_llama"))
        {
            System.out.println("[BFQ-Debug] 马类 返回 RAIDER");
            return MarkData.MarkType.RAIDER;
        }

        if (entityName.contains("tnt_minecart"))
        {
            return MarkData.MarkType.TANK_HUNTER_EQUIPMENT;
        }

        if (entityName.contains("chest_minecart"))
        {
            return MarkData.MarkType.TRENCH_FIGHTER_EQUIPMENT;
        }
        else if (entityName.contains("hopper_minecart"))
        {
            return MarkData.MarkType.INVADER_EQUIPMENT;
        }
        else if (entityName.contains("piglin_brute") || entityName.contains("vindicator"))
        {
            System.out.println("[BFQ-Debug] 猪灵蛮兵/Vindicator 返回 TRENCH_FIGHTER");
            return MarkData.MarkType.TRENCH_FIGHTER;
        }

        if (entityName.contains("skeleton_horse") || entityName.contains("zombie_horse"))
        {
            System.out.println("[BFQ-Debug] 骷髅马/僵尸马 返回 RAIDER");
            return MarkData.MarkType.RAIDER;
        }

        // 所有其他实体使用随机
        System.out.println("[BFQ-Debug] 其他生物使用随机");
        return MarkData.MarkType.random(null);
    }

    private static Entity getEntityLookingAt(LocalPlayer player, double maxDistance)
    {
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 lookVector = player.getLookAngle();
        Vec3 endPosition = eyePosition.add(lookVector.x * maxDistance, lookVector.y * maxDistance, lookVector.z * maxDistance);
        
        AABB searchBox = player.getBoundingBox().expandTowards(lookVector.scale(maxDistance)).inflate(3.0D);
        
        List<Entity> entities = player.level().getEntities(player, searchBox, entity -> entity instanceof LivingEntity || entity.getType().toString().toLowerCase().contains("chest_minecart") || entity.getType().toString().toLowerCase().contains("hopper_minecart") || entity.getType().toString().toLowerCase().contains("tnt_minecart"));
        
        Entity closestEntity = null;
        double closestDistance = maxDistance;
        
        for (Entity entity : entities)
        {
            double inflateAmount = entity instanceof EnderDragon ? 10.0D : 0.3D;
            AABB entityBox = entity.getBoundingBox().inflate(inflateAmount);
            Optional<Vec3> hitResult = entityBox.clip(eyePosition, endPosition);
            
            if (hitResult.isPresent())
            {
                double distance = eyePosition.distanceTo(hitResult.get());
                if (distance < closestDistance)
                {
                    closestDistance = distance;
                    closestEntity = entity;
                }
            }
        }
        
        return closestEntity;
    }

    private static boolean isValidTarget(Entity entity)
    {
        if (entity instanceof LivingEntity)
        {
            if (entity instanceof Player)
            {
                return true;
            }

            if (entity instanceof Mob)
            {
                return true;
            }

            if (entity instanceof Animal)
            {
                return true;
            }

            if (entity instanceof Villager)
            {
                return true;
            }

            if (entity instanceof EnderDragon)
            {
                return true;
            }
        }
        else
        {
            // 支持非生物实体，如箱子矿车
            String entityName = entity.getType().toString().toLowerCase();
            if (entityName.contains("minecart"))
            {
                return true;
            }
        }

        return false;
    }
    
    private static boolean isSameTeam(Player player1, Player player2)
    {
        if (player1 == null || player2 == null)
        {
            return false;
        }
        
        net.minecraft.world.scores.Team team1 = player1.getTeam();
        net.minecraft.world.scores.Team team2 = player2.getTeam();
        
        if (team1 == null || team2 == null)
        {
            return false;
        }
        
        return team1.getName().equals(team2.getName());
    }
    
    public static boolean hasTeam(Player player)
    {
        return player != null && player.getTeam() != null;
    }

    private static boolean isFriendlyTarget(Entity entity, LocalPlayer player)
    {
        if (entity instanceof Player targetPlayer)
        {
            return isSameTeam(player, targetPlayer);
        }
        
        if (entity instanceof Cat)
        {
            return true;
        }
        
        if (entity instanceof TamableAnimal tamable)
        {
            // 鹦鹉即使未被驯服也应该是友好的
            if (tamable instanceof Parrot)
            {
                return true;
            }
            return tamable.isTame();
        }
        
        if (entity instanceof Villager || entity instanceof WanderingTrader)
        {
            return true;
        }
        
        if (entity instanceof IronGolem ironGolem)
        {
            return ironGolem.isPlayerCreated();
        }
        
        if (entity instanceof SnowGolem)
        {
            return true;
        }
        
        String entityName = entity.getType().toString().toLowerCase();
        
        // 蠹虫是敌对生物
        if (entityName.contains("silverfish"))
        {
            return false;
        }
        
        if (entityName.contains("allay") || entityName.contains("cod") || 
            entityName.contains("salmon") || entityName.contains("squid") || 
            entityName.contains("glow_squid") || entityName.contains("dolphin") || 
            entityName.contains("parrot") || entityName.contains("tadpole"))
        {
            return true;
        }
        
        // 河豚是敌对生物
        if (entityName.contains("pufferfish"))
        {
            return false;
        }
        
        // 其他鱼类是友好生物
        if (entityName.contains("fish"))
        {
            return true;
        }
        
        // 矿车是敌对生物
        if (entityName.contains("minecart"))
        {
            return false;
        }
        
        if (entityName.contains("skeleton_horse") || entityName.contains("zombie_horse") || 
            entityName.contains("hoglin") || entityName.contains("zoglin"))
        {
            return false;
        }
        
        if (entityName.contains("horse") || entityName.contains("llama") || entityName.contains("mule") || 
            entityName.contains("donkey") || entityName.contains("strider") || entityName.contains("trader_llama"))
        {
            return true;
        }
        
        if (entity instanceof Animal)
        {
            return true;
        }
        
        return false;
    }

    private static void markEntity(Entity entity, MarkData.MarkType markType, boolean isFriendly)
    {
        System.out.println("[markEntity] ========== 开始 ==========");
        System.out.println("[markEntity] 目标: " + entity.getName().getString() + " (" + entity.getUUID() + ")");
        System.out.println("[markEntity] 传入的 markType: " + markType + ", isFriendly: " + isFriendly);
        
        long currentTime = System.currentTimeMillis();
        
        // 检查标记冷却
        if (currentTime - lastMarkTime < MARK_COOLDOWN_MS)
        {
            System.out.println("[markEntity] 在标记冷却中，返回");
            System.out.println("[markEntity] ========== 结束 ==========");
            return;
        }
        
        // 检查目标是否已经被标记
        boolean hasMark = com.ea.bfaq.mark.MarkManager.getInstance().hasActiveMark(entity.getUUID());
        
        // 先在本地添加标记，这样即使服务器没有mod也能显示
        MarkData.MarkType finalMarkType = com.ea.bfaq.mark.MarkManager.getInstance().addMark(entity.getUUID(), markType, isFriendly);
        
        NetworkHandler.INSTANCE.sendToServer(
                new NetworkHandler.MarkPacket(entity.getUUID(), finalMarkType, isFriendly)
        );
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            lastMarkTime = currentTime;
            return;
        }
        
        // 友军标记不播放音效
        if (isFriendly)
        {
            lastMarkTime = currentTime;
            return;
        }

        // 目标已被标记时不重复播放音效
        if (hasMark)
        {
            lastMarkTime = currentTime;
            return;
        }

        // 播放标记音效
        com.ea.bfaq.client.sound.MarkSoundPlayer.playMarkSound(entity, finalMarkType);

        // 显示UI，只在本地标记时显示
        if (finalMarkType == MarkData.MarkType.FLAMETHROWER)
        {
            com.ea.bfaq.client.gui.TopBarGUI.setShowFlamethrowerUI(true);
        }
        else if (finalMarkType == MarkData.MarkType.SENTRY)
        {
            com.ea.bfaq.client.gui.TopBarGUI.setShowUI(true);
        }
        else if (finalMarkType == MarkData.MarkType.TRENCH_FIGHTER_EQUIPMENT && entity.getType().toString().toLowerCase().contains("chest_minecart"))
        {
            com.ea.bfaq.client.gui.TopBarGUI.setShowMinecartUI(true);
        }
        else if (finalMarkType == MarkData.MarkType.INVADER_EQUIPMENT && entity.getType().toString().toLowerCase().contains("hopper_minecart"))
        {
            com.ea.bfaq.client.gui.TopBarGUI.setShowHopperUI(true);
        }
        else if (finalMarkType == MarkData.MarkType.TANK_HUNTER_EQUIPMENT && entity.getType().toString().toLowerCase().contains("tnt_minecart"))
        {
            com.ea.bfaq.client.gui.TopBarGUI.setShowTankHunterUI(true);
        }

        lastMarkTime = currentTime;
    }
}