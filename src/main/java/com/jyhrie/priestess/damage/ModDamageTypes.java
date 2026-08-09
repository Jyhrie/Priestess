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

    public static final ResourceKey<DamageType> ORIPATHY = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(Priestess.MOD_ID, "oripathy"));

    public static void bootstrap(BootstapContext<DamageType> context) {
        // NEVER scaling: the infection does not care what difficulty you picked. No
        // exhaustion either — it is lethal in one hit, there is no hunger cost to model.
        // The message id drives the death message key: death.attack.oripathy.
        context.register(ORIPATHY, new DamageType("oripathy", DamageScaling.NEVER, 0.0F, DamageEffects.HURT));
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
