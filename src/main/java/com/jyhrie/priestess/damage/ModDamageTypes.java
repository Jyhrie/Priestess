package com.jyhrie.priestess.damage;

import com.jyhrie.priestess.Priestess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * Damage types are datapack JSON in 1.20.1, so like the worldgen registries these are
 * declared in Java and written out by {@code gradlew runData} — see README.md.
 */
public class ModDamageTypes {

    public static final ResourceKey<DamageType> ORIPATHY = key("oripathy");

    /** Jesselton's phase one: ordinary kinetic damage, which armour is exactly what it is for. */
    public static final ResourceKey<DamageType> SPECTRAL_ARTS = key("spectral_arts");

    /**
     * Jesselton's phase two, and the Failed Vision's lasers. Its membership of
     * {@code minecraft:bypasses_armor} is the whole difference between the two phases.
     */
    public static final ResourceKey<DamageType> VOID_ARTS = key("void_arts");

    /** A dying Originium Slug bursting. Corrosive, and it infects. */
    public static final ResourceKey<DamageType> ORIGINIUM_ACID = key("originium_acid");

    /** A Rhine security drone's beam. Eats armour durability rather than piercing it. */
    public static final ResourceKey<DamageType> RHINE_LASER = key("rhine_laser");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(Priestess.MOD_ID, name));
    }

    public static void bootstrap(BootstapContext<DamageType> context) {
        // NEVER scaling: the infection does not care what difficulty you picked. The message
        // id drives the death message key, death.attack.oripathy.
        context.register(ORIPATHY, new DamageType("oripathy", DamageScaling.NEVER, 0.0F, DamageEffects.HURT));

        // The rest scale like every vanilla mob attack, so Hard hurts more than Easy.
        context.register(SPECTRAL_ARTS,
                new DamageType("spectral_arts", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
        context.register(VOID_ARTS,
                new DamageType("void_arts", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
        context.register(ORIGINIUM_ACID,
                new DamageType("originium_acid", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
        context.register(RHINE_LASER,
                new DamageType("rhine_laser", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
    }

    /**
     * A source attributed to the mob that caused it, so kills are credited and death
     * messages read "…was crystallised by an Originium Slug" rather than naming nobody.
     */
    public static DamageSource source(Level level, ResourceKey<DamageType> type,
                                      net.minecraft.world.entity.Entity attacker) {
        return new DamageSource(level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type), attacker);
    }

    /**
     * Damage types live in the dynamic registries, so a source can only be built once a
     * level is available — there is no static constant to hold.
     */
    public static DamageSource source(Level level, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type));
    }
}
