package com.ea.bfaq;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundEvents
{
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BF1Q.MODID);

    public static final RegistryObject<SoundEvent> ASSAULT_A = registerSound("mark.assault_a");
    public static final RegistryObject<SoundEvent> ASSAULT_B = registerSound("mark.assault_b");
    public static final RegistryObject<SoundEvent> ASSAULT_C = registerSound("mark.assault_c");
    public static final RegistryObject<SoundEvent> MEDIC_A = registerSound("mark.medic_a");
    public static final RegistryObject<SoundEvent> MEDIC_B = registerSound("mark.medic_b");
    public static final RegistryObject<SoundEvent> MEDIC_C = registerSound("mark.medic_c");
    public static final RegistryObject<SoundEvent> MEDIC_EASTER = registerSound("mark.medic_easter");
    public static final RegistryObject<SoundEvent> SUPPORT_A = registerSound("mark.support_a");
    public static final RegistryObject<SoundEvent> SUPPORT_B = registerSound("mark.support_b");
    public static final RegistryObject<SoundEvent> SUPPORT_C = registerSound("mark.support_c");
    public static final RegistryObject<SoundEvent> RECON_A = registerSound("mark.recon_a");
    public static final RegistryObject<SoundEvent> RECON_B = registerSound("mark.recon_b");
    public static final RegistryObject<SoundEvent> RECON_C = registerSound("mark.recon_c");
    public static final RegistryObject<SoundEvent> BOMBER_A = registerSound("mark.bomber_a");
    public static final RegistryObject<SoundEvent> BOMBER_B = registerSound("mark.bomber_b");
    public static final RegistryObject<SoundEvent> BOMBER_C = registerSound("mark.bomber_c");
    public static final RegistryObject<SoundEvent> PLANE_A = registerSound("mark.plane_a");
    public static final RegistryObject<SoundEvent> PLANE_B = registerSound("mark.plane_b");
    public static final RegistryObject<SoundEvent> PLANE_C = registerSound("mark.plane_c");
    public static final RegistryObject<SoundEvent> RAIDER_A = registerSound("mark.raider_a");
    public static final RegistryObject<SoundEvent> RAIDER_B = registerSound("mark.raider_b");
    public static final RegistryObject<SoundEvent> RAIDER_C = registerSound("mark.raider_c");
    public static final RegistryObject<SoundEvent> FLAMETHROWER_A = registerSound("mark.flamethrower_a");
    public static final RegistryObject<SoundEvent> FLAMETHROWER_B = registerSound("mark.flamethrower_b");
    public static final RegistryObject<SoundEvent> FLAMETHROWER_C = registerSound("mark.flamethrower_c");
    public static final RegistryObject<SoundEvent> SENTRY_A = registerSound("mark.sentry_a");
    public static final RegistryObject<SoundEvent> SENTRY_B = registerSound("mark.sentry_b");
    public static final RegistryObject<SoundEvent> SENTRY_C = registerSound("mark.sentry_c");
    public static final RegistryObject<SoundEvent> KILL = registerSound("mark.kill");
    public static final RegistryObject<SoundEvent> HEADSHOT_KILL = registerSound("mark.headshot_kill");
    public static final RegistryObject<SoundEvent> NEED_AMMO_A = registerSound("mark.need_ammo_a");
    public static final RegistryObject<SoundEvent> NEED_AMMO_B = registerSound("mark.need_ammo_b");
    public static final RegistryObject<SoundEvent> NEED_AMMO_C = registerSound("mark.need_ammo_c");
    public static final RegistryObject<SoundEvent> NEED_AMMO_D = registerSound("mark.need_ammo_d");
    public static final RegistryObject<SoundEvent> NEED_AMMO_E = registerSound("mark.need_ammo_e");
    public static final RegistryObject<SoundEvent> SUPPORT_EASTER = registerSound("mark.support_easter");
    public static final RegistryObject<SoundEvent> LOW_HEALTH_MEDIC_A = registerSound("mark.low_health_medic_a");
    public static final RegistryObject<SoundEvent> LOW_HEALTH_MEDIC_B = registerSound("mark.low_health_medic_b");
    public static final RegistryObject<SoundEvent> LOW_HEALTH_MEDIC_C = registerSound("mark.low_health_medic_c");
    public static final RegistryObject<SoundEvent> LOW_HEALTH_MEDIC_D = registerSound("mark.low_health_medic_d");
    public static final RegistryObject<SoundEvent> TRENCH_FIGHTER = registerSound("mark.trench_fighter");
    public static final RegistryObject<SoundEvent> INVADER = registerSound("mark.invader");

    private static RegistryObject<SoundEvent> registerSound(String name)
    {
        ResourceLocation location = new ResourceLocation(BF1Q.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(location));
    }

    public static void register(IEventBus eventBus)
    {
        SOUND_EVENTS.register(eventBus);
    }
}
