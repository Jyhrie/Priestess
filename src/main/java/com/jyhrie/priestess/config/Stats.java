package com.jyhrie.priestess.config;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.function.BooleanSupplier;

/**
 * The shared vocabulary of the four balance configs in this package.
 *
 * <p>{@link BossStats}, {@link MinibossStats}, {@link MobStats} and {@link WeaponStats} each own
 * a {@link ForgeConfigSpec} and a file under {@code config/priestess/}. What they have in common
 * — the shape of a mob's six attributes, the shape of a weapon's two numbers, the bounds, and
 * the act of writing them onto a live entity — is here, so the four files stay a list of numbers
 * and nothing else.
 *
 * <h2>Why four files</h2>
 * They are read at different times by different people. Boss health is tuned while designing a
 * fight; mob health is tuned while pacing a dungeon; weapon damage is tuned against both. One
 * six-hundred-line file made every one of those edits a scroll through the other two. The split
 * costs four {@code registerConfig} calls in {@link com.jyhrie.priestess.Priestess} and nothing
 * else — Forge tracks configs by filename, so a mod may register as many as it likes, and a
 * filename containing a directory is created for it ({@code ConfigFileTypeHandler.setupConfigFile}
 * calls {@code Files.createDirectories} on the parent).
 *
 * <h2>Bounds</h2>
 * Vanilla's own limits for each attribute, so nothing configured can be silently clamped later
 * by the attribute itself — {@code AttributeInstance.calculateValue} ends by passing the total
 * through {@code Attribute.sanitizeValue}, which is a clamp, so a config value past the ceiling
 * is not a bigger number but an invisible one. Movement speed is the single exception: vanilla
 * allows 1024, which is not a speed so much as a teleport.
 */
public final class Stats {

    static final double MAX_HEALTH_LIMIT = 1024.0;
    static final double SPEED_LIMIT = 2.0;
    static final double DAMAGE_LIMIT = 2048.0;
    static final double RANGE_LIMIT = 2048.0;
    static final double ARMOUR_LIMIT = 30.0;
    static final double FRACTION_LIMIT = 16.0;

    /**
     * The header every one of the three creature files carries, so the six keys are explained
     * once per file rather than once per mob. Sixteen mobs times six identical comments is a
     * config nobody reads.
     */
    static final String[] ATTRIBUTE_KEYS = {
            "Each table below takes the same six keys:",
            "",
            "  maxHealth            in half-hearts. A player is 20. Vanilla caps this at 1024,",
            "                       and the cap is real — a larger number is silently clamped.",
            "  movementSpeed        0.23 is a zombie, 0.25 a skeleton, 0.3 is brisk, and a",
            "                       sprinting player outruns about 0.33. Zero means it does not",
            "                       move at all.",
            "  attackDamage         melee damage per hit, in half-hearts, before the target's",
            "                       armour. Zero on anything with no melee goal.",
            "  followRange          how far away it notices you, in blocks.",
            "  armour               0-30, subtracted the way a player's armour is.",
            "  knockbackResistance  0 is shoved freely, 1 is immovable.",
            "",
            "Ranged and special attacks carry their own damage and do not read attackDamage;",
            "where something has one it appears as an extra key in its own table.",
            "",
            "CHANGES APPLY AS ENTITIES JOIN THE WORLD, so anything already standing in a loaded",
            "chunk keeps its numbers until that chunk unloads and comes back. A wounded mob stays",
            "wounded: it is restored to the same fraction of the new maximum, not healed to full.",
    };

    /** One file's opening comment: its own headline, then the shared six-key explanation. */
    static String[] header(String headline) {
        String[] lines = new String[ATTRIBUTE_KEYS.length + 2];
        lines[0] = headline;
        lines[1] = "";
        System.arraycopy(ATTRIBUTE_KEYS, 0, lines, 2, ATTRIBUTE_KEYS.length);
        return lines;
    }

    /**
     * The six attributes, in one table. The caller has already pushed the table and pops it
     * afterwards, so ability keys declared around this call land beside these.
     */
    static Block attributes(ForgeConfigSpec.Builder builder, BooleanSupplier loaded,
                            double health, double speed, double damage,
                            double range, double armour, double knockbackResistance) {
        return new Block(loaded,
                builder.defineInRange("maxHealth", health, 1.0, MAX_HEALTH_LIMIT),
                builder.defineInRange("movementSpeed", speed, 0.0, SPEED_LIMIT),
                builder.defineInRange("attackDamage", damage, 0.0, DAMAGE_LIMIT),
                builder.defineInRange("followRange", range, 0.0, RANGE_LIMIT),
                builder.defineInRange("armour", armour, 0.0, ARMOUR_LIMIT),
                builder.defineInRange("knockbackResistance", knockbackResistance, 0.0, 1.0));
    }

    static Weapon weapon(ForgeConfigSpec.Builder builder, BooleanSupplier loaded,
                         double damage, double speed) {
        return new Weapon(loaded, damage, speed,
                builder.defineInRange("attackDamage", damage, 0.0, DAMAGE_LIMIT),
                // Negative, and allowed positive for anyone who wants a weapon that swings
                // faster than a bare fist. The floor is where attack speed reaches zero and the
                // swing never recovers.
                builder.defineInRange("attackSpeed", speed, -3.9, 16.0));
    }

    /**
     * One creature's six attributes, and the ability to write them onto an entity.
     *
     * <p>Base values rather than modifiers: this is what the thing <em>is</em>, so there is
     * nothing to remove later and nothing that should compose with a second source. Runtime
     * effects that change a stat — the miniboss enraging, say — use modifiers on top, which is
     * what keeps them from being erased the next time this runs.
     */
    public static final class Block {

        /**
         * Whether the owning file has been read. Each of the four configs loads independently,
         * so a {@code Block} has to be able to answer for its own — and
         * {@code ForgeConfigSpec.ConfigValue.get()} throws rather than returning a default if it
         * is asked too early.
         */
        private final BooleanSupplier loaded;

        private final ForgeConfigSpec.DoubleValue maxHealth;
        private final ForgeConfigSpec.DoubleValue movementSpeed;
        private final ForgeConfigSpec.DoubleValue attackDamage;
        private final ForgeConfigSpec.DoubleValue followRange;
        private final ForgeConfigSpec.DoubleValue armour;
        private final ForgeConfigSpec.DoubleValue knockbackResistance;

        private Block(BooleanSupplier loaded,
                      ForgeConfigSpec.DoubleValue maxHealth,
                      ForgeConfigSpec.DoubleValue movementSpeed,
                      ForgeConfigSpec.DoubleValue attackDamage,
                      ForgeConfigSpec.DoubleValue followRange,
                      ForgeConfigSpec.DoubleValue armour,
                      ForgeConfigSpec.DoubleValue knockbackResistance) {
            this.loaded = loaded;
            this.maxHealth = maxHealth;
            this.movementSpeed = movementSpeed;
            this.attackDamage = attackDamage;
            this.followRange = followRange;
            this.armour = armour;
            this.knockbackResistance = knockbackResistance;
        }

        /**
         * Overwrites the entity's base attributes with the configured ones. A no-op if the
         * owning file has not been read, which leaves the compiled attributes standing — a
         * working creature, and the right answer in the absence of a config.
         *
         * <p>Health is carried across as a <em>fraction</em>. A boss reloaded at a third of its
         * health comes back at a third of whatever the new maximum is — not healed to full,
         * which would make unloading a chunk a way to reset a fight, and not left holding a
         * number the new maximum cannot contain.
         */
        public void applyTo(LivingEntity entity) {
            if (!loaded.getAsBoolean()) {
                return;
            }

            float previousMax = entity.getMaxHealth();
            float fraction = previousMax > 0.0F
                    ? Mth.clamp(entity.getHealth() / previousMax, 0.0F, 1.0F)
                    : 1.0F;

            setBase(entity, Attributes.MAX_HEALTH, maxHealth.get());
            setBase(entity, Attributes.MOVEMENT_SPEED, movementSpeed.get());
            setBase(entity, Attributes.ATTACK_DAMAGE, attackDamage.get());
            setBase(entity, Attributes.FOLLOW_RANGE, followRange.get());
            setBase(entity, Attributes.ARMOR, armour.get());
            setBase(entity, Attributes.KNOCKBACK_RESISTANCE, knockbackResistance.get());

            entity.setHealth(entity.getMaxHealth() * fraction);
        }

        /**
         * Null-checked because not every attribute exists on every living thing — {@code
         * ATTACK_DAMAGE} and {@code FOLLOW_RANGE} come from {@code Monster} and {@code Mob}
         * rather than from {@code LivingEntity}, so a creature that stopped being a Monster
         * would otherwise take this down with it.
         */
        private static void setBase(LivingEntity entity, Attribute attribute, double value) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null) {
                instance.setBaseValue(value);
            }
        }
    }

    /**
     * One weapon's damage and swing speed.
     *
     * <p>Both are readable two ways. {@link #defaultAttackDamage()} and
     * {@link #defaultAttackSpeed()} are the compiled-in numbers, needed by the item constructor,
     * which runs during registration and may well beat the config file to it.
     * {@link #attackDamage()} and {@link #attackSpeed()} are the configured ones, and fall back
     * to the compiled numbers if they are asked before the file has been read — which is exactly
     * that registration window, so the fallback is not theoretical.
     */
    public static final class Weapon {

        private final BooleanSupplier loaded;
        private final double defaultAttackDamage;
        private final double defaultAttackSpeed;
        private final ForgeConfigSpec.DoubleValue attackDamage;
        private final ForgeConfigSpec.DoubleValue attackSpeed;

        private Weapon(BooleanSupplier loaded, double defaultAttackDamage, double defaultAttackSpeed,
                       ForgeConfigSpec.DoubleValue attackDamage,
                       ForgeConfigSpec.DoubleValue attackSpeed) {
            this.loaded = loaded;
            this.defaultAttackDamage = defaultAttackDamage;
            this.defaultAttackSpeed = defaultAttackSpeed;
            this.attackDamage = attackDamage;
            this.attackSpeed = attackSpeed;
        }

        /** Truncated, because {@code SwordItem}'s constructor takes an int. */
        public int defaultAttackDamage() {
            return (int) defaultAttackDamage;
        }

        public float defaultAttackSpeed() {
            return (float) defaultAttackSpeed;
        }

        public double attackDamage() {
            return loaded.getAsBoolean() ? attackDamage.get() : defaultAttackDamage;
        }

        public double attackSpeed() {
            return loaded.getAsBoolean() ? attackSpeed.get() : defaultAttackSpeed;
        }
    }

    private Stats() {
    }
}
