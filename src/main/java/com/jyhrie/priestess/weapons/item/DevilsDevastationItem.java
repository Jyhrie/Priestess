package com.jyhrie.priestess.weapons.item;

import com.jyhrie.priestess.config.WeaponStats;
import com.jyhrie.priestess.weapons.ModWeapons;
import com.jyhrie.priestess.weapons.WeaponRarities;
import com.jyhrie.priestess.weapons.WeaponText;
import com.jyhrie.priestess.weapons.WeaponTiers;
import com.jyhrie.priestess.weapons.entity.DevilsPitchforkEntity;
import com.jyhrie.priestess.weapons.entity.DevilsScytheEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Devil's Devastation. A greatsword that throws a fan of five projectiles on every swing.
 *
 * <p>Ported from Lethality. <b>{@code docs/LETHALITY WEAPONS.md} records what changed on the
 * way in</b> — several behaviours here are stubbed rather than absent, and editing this file
 * without reading it will look like fixing bugs that are actually decisions.
 *
 * <p>Swinging is a client-side event and only the server may spawn entities, so the chain is
 * {@code WeaponSwingEvents} → {@code SwingSlashC2S} → {@link #fireFan}. That indirection is
 * not optional; a weapon that spawns projectiles client-side spawns them nowhere.
 *
 * <p>The compiled numbers are defaults — see {@link ConfiguredSwordItem} and
 * {@code config/priestess/weapon.toml}. The tier adds nothing to damage; see
 * {@link WeaponTiers}.
 */
public class DevilsDevastationItem extends ConfiguredSwordItem {

    /** Muzzle velocity of every projectile in the fan. */
    private static final float PROJECTILE_SPEED = 1.75F;

    /** Random spread, in the same units {@code shootFromRotation} takes. */
    private static final float PROJECTILE_INACCURACY = 0.5F;

    /** Degrees off-centre for the two outer scythes. */
    private static final float SCYTHE_ANGLE = 25.0F;

    /** Degrees off-centre for the two pitchforks — inside the scythes. */
    private static final float PITCHFORK_ANGLE = 12.5F;

    /** Height above the player's feet the fan leaves from. */
    private static final double SPAWN_HEIGHT = 0.25;

    /**
     * Terramity is not a dependency, so this resolves to null without it — which is what
     * crashes Lethality, since it passes the null straight to {@code playSound}. Kept as a
     * runtime lookup so the sound comes back for free if Terramity is ever added, with the
     * null check and vanilla fallback in {@link #playSwingSound} so it does not crash.
     */
    private static final ResourceLocation PREFERRED_SWING_SOUND =
            new ResourceLocation("terramity", "crescent_moonblade_wave");

    public DevilsDevastationItem() {
        super(WeaponTiers.DEMONIC, WeaponStats.DEVILS_DEVASTATION, new Properties());
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return WeaponRarities.CALAMITOUS;
    }

    /** Warm white through fire, blood and violet to magenta and back. Verbatim from Lethality. */
    @Override
    public Component getName(ItemStack stack) {
        return WeaponText.gradient(Component.translatable(this.getDescriptionId(stack)), 0.25F, 2.0F,
                new int[]{255, 254, 251},   // warm white
                new int[]{255, 242, 203},   // golden cream
                new int[]{255, 227, 192},
                new int[]{255, 221, 177},   // soft peach
                new int[]{255, 202, 153},
                new int[]{255, 172, 131},   // pastel orange
                new int[]{255, 156, 119},
                new int[]{255, 139, 107},   // coral
                new int[]{252, 114, 94},
                new int[]{251, 101, 87},    // salmon red
                new int[]{235, 75, 76},
                new int[]{232, 72, 68},
                new int[]{255, 64, 0},      // fire
                new int[]{255, 0, 0},       // pure red
                new int[]{255, 0, 0},
                new int[]{197, 32, 57},     // blood
                new int[]{150, 0, 100},
                new int[]{127, 0, 175},     // dark violet
                new int[]{170, 0, 210},
                new int[]{210, 0, 240},
                new int[]{255, 0, 255},     // pure magenta
                new int[]{255, 75, 165},
                new int[]{255, 98, 176},
                new int[]{255, 130, 192},
                new int[]{255, 158, 189},
                new int[]{255, 170, 197},
                new int[]{255, 191, 201},
                new int[]{255, 221, 203},
                new int[]{255, 227, 203},
                new int[]{255, 242, 203},
                new int[]{255, 254, 251}    // back to warm white, closing the loop
        ).withStyle(ChatFormatting.BOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        // Lethality renders this in a custom font ("lethality:homicide") that did not come
        // across. Same palette, default glyphs.
        tooltip.add(WeaponText.gradient(Component.literal("Calamitous"), 0.25F, 5.0F,
                new int[]{255, 254, 251},
                new int[]{255, 242, 203},
                new int[]{255, 221, 177},
                new int[]{255, 202, 153},
                new int[]{255, 172, 131},
                new int[]{255, 156, 119},
                new int[]{255, 139, 107},
                new int[]{252, 114, 94},
                new int[]{251, 101, 87},
                new int[]{235, 75, 76},
                new int[]{197, 32, 57},
                new int[]{235, 75, 76},
                new int[]{251, 101, 87},
                new int[]{252, 114, 94},
                new int[]{255, 139, 107},
                new int[]{255, 156, 119},
                new int[]{255, 172, 131},
                new int[]{255, 202, 153},
                new int[]{255, 221, 177},
                new int[]{255, 254, 251}
        ));

        tooltip.add(Component.literal(" "));
        tooltip.add(Component.translatable("tooltip.priestess.devils_devastation.flavour"));
        tooltip.add(Component.literal(" "));

        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.priestess.devils_devastation.on_swing"));
            tooltip.add(Component.translatable("tooltip.priestess.devils_devastation.on_swing_detail"));
            tooltip.add(Component.literal(" "));
            tooltip.add(Component.translatable("tooltip.priestess.devils_devastation.on_hit"));
            tooltip.add(Component.translatable("tooltip.priestess.devils_devastation.on_hit_detail"));
        } else {
            // Lethality's key says "hold_ctrl" while the check above is hasShiftDown, so its
            // prompt asks for the wrong key. Renamed to match what works.
            tooltip.add(Component.translatable("tooltip.priestess.hold_shift"));
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Lethality stacks Terramity's Nyxium Fire here, which is the weapon's real damage
        // curve. Terramity is not a dependency; see docs/LETHALITY WEAPONS.md, "Terramity".
        target.setSecondsOnFire(10);

        // Clearing hurt-immunity is what lets the projectile fan land on a target the melee
        // swing just hit, rather than the burst being eaten by invulnerability frames. It is
        // why this weapon bursts as hard as it does — do not tidy it away.
        target.invulnerableTime = 0;

        return super.hurtEnemy(stack, target, attacker);
    }

    /**
     * Spawns the fan. Called on the server from the swing packet, never directly. Checks both
     * hands because the weapon works off-hand.
     */
    public static void fireFan(Level level, Player user) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = user.getItemInHand(hand);
            if (stack.getItem() != ModWeapons.DEVILS_DEVASTATION.get()) {
                continue;
            }

            float scytheDamage = WeaponText.itemAttackDamage(stack)
                    * WeaponStats.DEVILS_PROJECTILE_FRACTION.get().floatValue();

            // Lethality skips this cooldown when Better Combat is installed and lets it own
            // the rate limit. Not integrated; to restore, guard with
            // if (!ModList.get().isLoaded("bettercombat")).
            if (user.getCooldowns().isOnCooldown(stack.getItem())) {
                return;
            }
            AttributeInstance attribute = user.getAttribute(Attributes.ATTACK_SPEED);
            float attackSpeed = attribute != null ? (float) attribute.getValue() : 4.0F;
            // One swing's worth of ticks, so the fan fires at the rate the sword swings
            // rather than as fast as the player can click.
            user.getCooldowns().addCooldown(stack.getItem(), (int) (20.0F / attackSpeed));

            if (!level.isClientSide()) {
                spawnFan(level, user, scytheDamage);
            }

            playSwingSound(level, user);
        }
    }

    /** Five projectiles: scythes at 0° and ±25°, pitchforks at ±12.5° and +2 damage. */
    private static void spawnFan(Level level, Player user, float scytheDamage) {
        shoot(level, new DevilsScytheEntity(level, user.getX(), user.getY() + SPAWN_HEIGHT,
                user.getZ(), scytheDamage), user, 0.0F);
        shoot(level, new DevilsScytheEntity(level, user.getX(), user.getY() + SPAWN_HEIGHT,
                user.getZ(), scytheDamage), user, -SCYTHE_ANGLE);
        shoot(level, new DevilsScytheEntity(level, user.getX(), user.getY() + SPAWN_HEIGHT,
                user.getZ(), scytheDamage), user, SCYTHE_ANGLE);

        float pitchforkDamage = scytheDamage
                + WeaponStats.DEVILS_PITCHFORK_BONUS.get().floatValue();
        shoot(level, new DevilsPitchforkEntity(level, user.getX(), user.getY() + SPAWN_HEIGHT,
                user.getZ(), pitchforkDamage), user, -PITCHFORK_ANGLE);
        shoot(level, new DevilsPitchforkEntity(level, user.getX(), user.getY() + SPAWN_HEIGHT,
                user.getZ(), pitchforkDamage), user, PITCHFORK_ANGLE);
    }

    /**
     * The {@code setDeltaMovement} before {@code shootFromRotation} looks redundant but is
     * not: the entity's first {@code tick} can run before the aimed velocity is applied, and
     * without a delta already set it steps by zero and renders on the player's feet.
     */
    private static <T extends net.minecraft.world.entity.projectile.AbstractHurtingProjectile>
    void shoot(Level level, T projectile, Player user, float yawOffset) {
        projectile.setDeltaMovement(user.getLookAngle());
        projectile.shootFromRotation(user, user.getXRot(), user.getYRot() + yawOffset,
                0.0F, PROJECTILE_SPEED, PROJECTILE_INACCURACY);
        projectile.setOwner(user);
        level.addFreshEntity(projectile);
    }

    private static void playSwingSound(Level level, Player user) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(PREFERRED_SWING_SOUND);
        if (sound == null) {
            sound = SoundEvents.PLAYER_ATTACK_SWEEP;
        }

        float pitch = Mth.nextFloat(level.getRandom(), 1.1F, 1.3F);
        float volume = 1.5F;

        if (!level.isClientSide()) {
            level.playSound(null, user.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
        } else {
            level.playLocalSound(user.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch, false);
        }
    }
}
