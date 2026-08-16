package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.damage.ModDamageTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Which vanilla damage-type tags this mod's damage types join.
 *
 * <p>These tags are the only reason a damage type is more than a death message: membership of
 * {@code bypasses_armor} <em>is</em> the mechanic that makes the prison's riot gear stop
 * mattering halfway through the boss it was looted from.
 *
 * <p>Tags are additive across datapacks, so writing to a vanilla tag appends rather than
 * replaces.
 */
public class ModDamageTypeTagsProvider extends TagsProvider<DamageType> {

    public ModDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                     @Nullable ExistingFileHelper existingFileHelper) {
        super(output, Registries.DAMAGE_TYPE, registries, Priestess.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Spectral Arts is deliberately absent: phase one is the half your gear answers.
        tag(DamageTypeTags.BYPASSES_ARMOR)
                .add(ModDamageTypes.VOID_ARTS);

        // Every ranged attack in the chapter is hitscan, so there is no travel time in which
        // to raise a shield. Leaving them blockable would make a shield a hard counter to
        // three of the four dungeon threats.
        tag(DamageTypeTags.BYPASSES_SHIELD)
                .add(ModDamageTypes.VOID_ARTS)
                .add(ModDamageTypes.SPECTRAL_ARTS)
                .add(ModDamageTypes.RHINE_LASER);

        // A cloud, not a blow: nothing to face and nothing to be thrown by.
        tag(DamageTypeTags.BYPASSES_SHIELD)
                .add(ModDamageTypes.ORIGINIUM_ACID);
        tag(DamageTypeTags.NO_IMPACT)
                .add(ModDamageTypes.ORIGINIUM_ACID);
    }

    @Override
    public String getName() {
        return "Damage Type Tags: " + Priestess.MOD_ID;
    }
}
