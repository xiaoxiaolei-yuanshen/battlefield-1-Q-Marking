package com.ea.bfaq.client;

// 关注b站UID:545778318谢谢喵
// 关注b站UID:1157669161谢谢喵

import com.ea.bfaq.BF1Q;
import com.ea.bfaq.SoundEvents;
import com.ea.bfaq.mark.MarkData;
import com.ea.bfaq.mark.MarkManager;
import com.ea.bfaq.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BF1Q.MODID, value = Dist.CLIENT)
public class MarkKeyHandler
{
    private static final double MARK_RANGE = 150.0D;
    private static final Random RANDOM = new Random();
    private static long lastEnemySoundPlayTime = 0;
    private static final long SOUND_COOLDOWN_MS = 500; // 0.5秒音效冷却
    private static long lastMarkTime = 0;
    private static final long MARK_COOLDOWN_MS = 200; // 0.2秒标记冷却

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
            
            if (target != null && isValidTarget(target))
            {
                boolean isFriendly = isFriendlyTarget(target, mc.player);
                MarkData.MarkType markType = getMarkTypeForEntity(target);
                markEntity(target, markType, isFriendly);
            }
        }
    }

    private static MarkData.MarkType getMarkTypeForEntity(Entity entity)
    {
        if (entity instanceof Player)
        {
            UUID playerUUID = entity.getUUID();
            String playerClass = com.ea.bfaq.commands.ZYCommand.getPlayerClass(playerUUID);
            if (playerClass != null)
            {
                return switch (playerClass) {
                    case "assault" -> MarkData.MarkType.ASSAULT;
                    case "medic" -> MarkData.MarkType.MEDIC;
                    case "recon" -> MarkData.MarkType.RECON;
                    case "support" -> MarkData.MarkType.SUPPORT;
                    default -> MarkData.MarkType.random();
                };
            }
            return MarkData.MarkType.random();
        }
        
        if (entity instanceof Ravager)
        {
            return MarkData.MarkType.RAIDER;
        }
        
        if (entity instanceof ElderGuardian || entity instanceof Guardian)
        {
            return MarkData.MarkType.TANK;
        }
        
        if (entity instanceof Warden)
        {
            return MarkData.MarkType.RIFLE;
        }
        
        if (entity instanceof EnderDragon || entity instanceof Phantom || entity instanceof Ghast || entity instanceof WitherBoss)
        {
            return MarkData.MarkType.PILOT;
        }
        
        if (entity instanceof Blaze)
        {
            return MarkData.MarkType.FLAMETHROWER;
        }
        
        if (entity instanceof IronGolem || entity instanceof SnowGolem)
        {
            return MarkData.MarkType.SENTRY;
        }
        
        if (entity instanceof Parrot)
        {
            return MarkData.MarkType.PILOT;
        }
        
        String entityName = entity.getType().toString().toLowerCase();
        
        if (entityName.contains("hoglin") || entityName.contains("zoglin"))
        {
            return MarkData.MarkType.RAIDER;
        }
        
        if (entityName.contains("horse") || entityName.contains("llama") || entityName.contains("mule") || 
            entityName.contains("donkey") || entityName.contains("strider") || entityName.contains("trader_llama"))
        {
            return MarkData.MarkType.RAIDER;
        }
        
        if (entityName.contains("chest_minecart"))
        {
            return MarkData.MarkType.TRENCH_FIGHTER;
        }
        else if (entityName.contains("hopper_minecart"))
        {
            return MarkData.MarkType.INVADER;
        }
        
        if (entityName.contains("skeleton_horse") || entityName.contains("zombie_horse"))
        {
            return MarkData.MarkType.RAIDER;
        }
        
        // 所有其他实体都使用随机标记类型
        return MarkData.MarkType.random();
    }

    private static Entity getEntityLookingAt(LocalPlayer player, double maxDistance)
    {
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 lookVector = player.getLookAngle();
        Vec3 endPosition = eyePosition.add(lookVector.x * maxDistance, lookVector.y * maxDistance, lookVector.z * maxDistance);
        
        AABB searchBox = player.getBoundingBox().expandTowards(lookVector.scale(maxDistance)).inflate(3.0D);
        
        List<Entity> entities = player.level().getEntities(player, searchBox, entity -> entity instanceof LivingEntity || entity.getType().toString().toLowerCase().contains("chest_minecart") || entity.getType().toString().toLowerCase().contains("hopper_minecart"));
        
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
        long currentTime = System.currentTimeMillis();
        
        // 检查标记冷却
        if (currentTime - lastMarkTime < MARK_COOLDOWN_MS)
        {
            return;
        }
        
        // 检查目标是否已经被标记
        if (com.ea.bfaq.mark.MarkManager.getInstance().hasActiveMark(entity.getUUID()))
        {
            return;
        }
        
        // 先在本地添加标记，这样即使服务器没有mod也能显示
        com.ea.bfaq.mark.MarkManager.getInstance().addMark(entity.getUUID(), markType, isFriendly);
        
        // 移除标记触发动画
        if (!isFriendly && entity instanceof LivingEntity)
        {
            // com.ea.bfaq.client.animation.AnimationController.getInstance().playMarkEnemyAnimation();
        }
        
        NetworkHandler.INSTANCE.sendToServer(
                new NetworkHandler.MarkPacket(entity.getUUID(), markType, isFriendly)
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
        
        // 检查音效冷却
        if (currentTime - lastEnemySoundPlayTime < SOUND_COOLDOWN_MS)
        {
            lastMarkTime = currentTime;
            return;
        }
        
        // 普通标记音效只在本地播放，彩蛋音效通过网络广播给其他玩家
        switch (markType)
        {
            case ASSAULT:
                com.ea.bfaq.client.sound.AssaultSound.play();
                break;
            case MEDIC:
                // 10%概率播放彩蛋音效
                if (new java.util.Random().nextInt(100) < 10)
                {
                    // 彩蛋音效通过网络广播给其他玩家
                    NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.MarkSoundPacket("medic_easter"));
                    // 本地也播放彩蛋音效
                    com.ea.bfaq.client.sound.MedicSound.playEaster();
                }
                else
                {
                    // 普通音效只在本地播放
                    com.ea.bfaq.client.sound.MedicSound.play();
                }
                break;
            case SUPPORT:
                // 10%概率播放彩蛋音效
                if (new java.util.Random().nextInt(100) < 10)
                {
                    // 彩蛋音效通过网络广播给其他玩家
                    NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.MarkSoundPacket("support_easter"));
                    // 本地也播放彩蛋音效
                    com.ea.bfaq.client.sound.SupportSound.playEaster();
                }
                else
                {
                    // 普通音效只在本地播放
                    com.ea.bfaq.client.sound.SupportSound.play();
                }
                break;
            case RECON:
                com.ea.bfaq.client.sound.ReconSound.play();
                break;
            case RAIDER:
                com.ea.bfaq.client.sound.RaiderSound.play();
                break;
            case FLAMETHROWER:
                com.ea.bfaq.client.sound.FlamethrowerSound.play();
                break;
            case SENTRY:
                com.ea.bfaq.client.sound.SentrySound.play();
                break;
            case PILOT:
                boolean isBomber = entity instanceof EnderDragon || entity instanceof WitherBoss;
                com.ea.bfaq.client.sound.PilotSound.play(isBomber);
                break;
            case TRENCH_FIGHTER:
                com.ea.bfaq.client.sound.TrenchFighterSound.play();
                break;
            case INVADER:
                com.ea.bfaq.client.sound.InvaderSound.play();
                break;
        }
        
        // 显示UI，只在本地标记时显示
        if (markType == MarkData.MarkType.FLAMETHROWER)
        {
            com.ea.bfaq.client.gui.TopBarGUI.setShowFlamethrowerUI(true);
        }
        else if (markType == MarkData.MarkType.SENTRY)
        {
            com.ea.bfaq.client.gui.TopBarGUI.setShowUI(true);
        }
        else if (markType == MarkData.MarkType.TRENCH_FIGHTER && entity.getType().toString().toLowerCase().contains("chest_minecart"))
        {
            com.ea.bfaq.client.gui.TopBarGUI.setShowMinecartUI(true);
        }
        else if (markType == MarkData.MarkType.INVADER && entity.getType().toString().toLowerCase().contains("hopper_minecart"))
        {
            com.ea.bfaq.client.gui.TopBarGUI.setShowHopperUI(true);
        }
        
        lastEnemySoundPlayTime = currentTime;
        lastMarkTime = currentTime;
    }
}