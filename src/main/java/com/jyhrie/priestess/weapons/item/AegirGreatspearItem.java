package com.jyhrie.priestess.weapons.item;

import com.jyhrie.priestess.config.WeaponStats;
import com.jyhrie.priestess.weapons.ModWeapons;
import com.jyhrie.priestess.weapons.WeaponPhysics;
import com.jyhrie.priestess.weapons.WeaponText;
import com.jyhrie.priestess.weapons.WeaponTiers;
import com.jyhrie.priestess.weapons.entity.AegirTide;
import com.jyhrie.priestess.weapons.entity.AegirWhirlpool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Aegir Greatspear. Ægir's polearm — three abilities, and every one of them pulls.
 *
 * <table border="1">
 *   <caption>inputs</caption>
 *   <tr><th>Input</th><th>Name</th><th>Effect</th><th>Cooldown</th></tr>
 *   <tr><td>left click</td><td>Tide-Piercer</td><td>throws a lance of white water; whatever it
 *       hits is dragged toward you</td><td>the spear's own swing rate, 0.83 s</td></tr>
 *   <tr><td>right click</td><td>Undertow</td><td>a 5×5×5 box in front, all of it dragged
 *       toward you at once</td><td>10 s</td></tr>
 *   <tr><td>shift + right click</td><td>Maelstrom</td><td>a whirlpool where you clicked, 8 s,
 *       sucking everything in and grinding it for 5 a second</td><td>30 s</td></tr>
 * </table>
 *
 * <p>Cooldowns split the same way {@link LaevatainItem}'s do: {@code ItemCooldowns} is keyed
 * by <em>item</em> and holds one timer, so a 30-second Maelstrom would otherwise lock out the
 * throw. The throw takes the vanilla cooldown; the two right-click abilities keep their
 * ready-times in stack NBT as game-time stamps.
 *
 * <p><b>Main-hand only</b>: {@link #thrust} reads the main hand and no other. A both-hands
 * scan combined with a main-hand-only client check is what lets one swing fire a
 * <em>different</em> weapon held in the off hand.
 */
public class AegirGreatspearItem extends ConfiguredSwordItem {

    // Damage and swing speed live in config/priestess/weapon.toml. What is here is geometry
    // and timing, which are shape rather than balance.

    private static final float TIDE_SPEED = 2.2F;

    /** Spread in degrees. Zero, so the lance lands where the crosshair was. */
    private static final float TIDE_INACCURACY = 0.0F;

    private static final String TAG_UNDERTOW_READY = "AegirUndertowReady";
    private static final int UNDERTOW_COOLDOWN_TICKS = 200;      // 10 s

    /**
     * A 5 × 5 × 5 box laid along the crosshair. <em>Oriented</em>, not an {@link AABB} — an
     * axis-aligned box would swell to its diagonal on any heading but due north, making the
     * ability half again as wide in some directions. See {@link #undertowTargets}.
     */
    private static final double UNDERTOW_LENGTH = 5.0;
    private static final double UNDERTOW_HALF_WIDTH = 2.5;
    private static final double UNDERTOW_HALF_HEIGHT = 2.5;

    /** Stronger than the throw's: this is the ability whose whole job is repositioning a group. */
    private static final double UNDERTOW_PULL_STRENGTH = 0.75;

    private static final String TAG_MAELSTROM_READY = "AegirMaelstromReady";
    private static final int MAELSTROM_COOLDOWN_TICKS = 600;     // 30 s

    /**
     * How far away one can be opened. Beyond this the ray stops and the vortex forms at the end
     * of it, so a shot at open sky still costs the cooldown and still does something.
     */
    private static final double MAELSTROM_PLACE_RANGE = 20.0;

    public AegirGreatspearItem() {
        super(WeaponTiers.DEMONIC, WeaponStats.AEGIR_GREATSPEAR, new Properties());
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.EPIC;
    }

    /** Foam down through open water into the deep trench and back. */
    @Override
    public Component getName(ItemStack stack) {
        return WeaponText.gradient(Component.translatable(this.getDescriptionId(stack)), 0.3F, 2.0F,
                new int[]{232, 250, 255},   // foam
                new int[]{176, 236, 255},
                new int[]{104, 208, 246},   // shallow
                new int[]{48, 168, 226},
                new int[]{20, 122, 196},    // open water
                new int[]{16, 82, 160},
                new int[]{18, 52, 122},     // deep
                new int[]{22, 36, 92},
                new int[]{18, 52, 122},
                new int[]{20, 122, 196},
                new int[]{104, 208, 246},
                new int[]{232, 250, 255}    // back to foam, closing the loop
        ).withStyle(ChatFormatting.BOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.translatable("tooltip.priestess.aegir_greatspear.flavour"));
        tooltip.add(Component.literal(" "));

        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.priestess.aegir_greatspear.left"));
            tooltip.add(Component.translatable("tooltip.priestess.aegir_greatspear.left_detail"));
            tooltip.add(Component.literal(" "));
            tooltip.add(Component.translatable("tooltip.priestess.aegir_greatspear.right"));
            tooltip.add(Component.translatable("tooltip.priestess.aegir_greatspear.right_detail"));
            tooltip.add(Component.literal(" "));
            tooltip.add(Component.translatable("tooltip.priestess.aegir_greatspear.shift_right"));
            tooltip.add(Component.translatable("tooltip.priestess.aegir_greatspear.shift_right_detail"));
        } else {
            tooltip.add(Component.translatable("tooltip.priestess.hold_shift"));
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }

    /**
     * Throws the lance. Called on the server from the swing packet, never directly — a swing is
     * a client-side event and only the server may spawn entities, so
     * {@code WeaponSwingEvents} → {@code SwingSlashC2S} → here is the whole chain. Main hand
     * only; see the class note.
     */
    public static void thrust(Level level, Player user) {
        ItemStack stack = user.getMainHandItem();
        if (stack.getItem() != ModWeapons.AEGIR_GREATSPEAR.get()) {
            return;
        }
        if (user.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }

        // One swing's worth of ticks, so the throw lands at the rate the spear swings rather
        // than as fast as the player can click.
        AttributeInstance attribute = user.getAttribute(Attributes.ATTACK_SPEED);
        float attackSpeed = attribute != null ? (float) attribute.getValue() : 4.0F;
        user.getCooldowns().addCooldown(stack.getItem(), (int) (20.0F / attackSpeed));

        if (!level.isClientSide()) {
            AegirTide tide = new AegirTide(level, user, WeaponText.itemAttackDamage(stack)
                    * WeaponStats.AEGIR_TIDE_FRACTION.get().floatValue());
            tide.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0F,
                    TIDE_SPEED, TIDE_INACCURACY);
            level.addFreshEntity(tide);
        }

        playSound(level, user, SoundEvents.TRIDENT_THROW, 1.1F, 0.8F, 1.0F);
    }

    /** Neither ability charges, so this is the whole right-click path. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Shift first, or the Undertow branch swallows it.
        if (player.isShiftKeyDown()) {
            return castMaelstrom(level, player, stack, maelstromTarget(level, player));
        }
        return castUndertow(level, player, stack);
    }

    /**
     * Shift-right-clicking a <em>block</em> never reaches {@link #use} — holding shift is what
     * suppresses block interaction, so the click arrives here instead. Maelstrom is aimed at a
     * spot on the ground, so without this it would be unusable.
     *
     * <p>The clicked location is used directly rather than re-raycast: the player picked a
     * point on a face, and that beats anything derived afterwards.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        return castMaelstrom(context.getLevel(), player, context.getItemInHand(),
                context.getClickLocation()).getResult();
    }

    private static InteractionResultHolder<ItemStack> castUndertow(Level level, Player player,
                                                                   ItemStack stack) {
        if (!ready(stack, level, TAG_UNDERTOW_READY)) {
            return InteractionResultHolder.fail(stack);
        }
        startCooldown(stack, level, TAG_UNDERTOW_READY, UNDERTOW_COOLDOWN_TICKS);

        if (!level.isClientSide()) {
            Vec3 origin = player.getEyePosition();
            Vec3 forward = player.getLookAngle();
            double length = reachBeforeTerrain(level, player, origin, forward, UNDERTOW_LENGTH);

            float damage = WeaponText.itemAttackDamage(stack)
                    * WeaponStats.AEGIR_UNDERTOW_FRACTION.get().floatValue();
            for (LivingEntity caught : undertowTargets(level, player, origin, forward, length)) {
                caught.hurt(player.damageSources().playerAttack(player), damage);
                WeaponPhysics.pullTowards(caught, player.position(), UNDERTOW_PULL_STRENGTH);
            }

            drawUndertow(level, origin, forward, length);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(p.getUsedItemHand()));
        }

        playSound(level, player, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.3F, 0.6F, 0.8F);
        return InteractionResultHolder.success(stack);
    }

    /**
     * Candidates are rewritten into the player's own frame and kept only if they land inside
     * the box on all three axes, which is what makes the volume oriented rather than
     * axis-aligned.
     */
    private static List<LivingEntity> undertowTargets(Level level, Player user, Vec3 origin,
                                                      Vec3 forward, double length) {
        Vec3 right = rightOf(forward);
        Vec3 up = right.cross(forward).normalize();

        // Broad phase: big enough to contain the oriented box on any heading. The exact test
        // below throws the corners back out.
        AABB search = new AABB(origin, origin.add(forward.scale(length)))
                .inflate(Math.max(UNDERTOW_HALF_WIDTH, UNDERTOW_HALF_HEIGHT));

        return level.getEntitiesOfClass(LivingEntity.class, search, candidate -> {
            if (candidate == user || !candidate.isAlive() || candidate.isSpectator()) {
                return false;
            }
            Vec3 offset = candidate.getBoundingBox().getCenter().subtract(origin);
            double alongForward = offset.dot(forward);
            return alongForward >= 0.0 && alongForward <= length
                    && Math.abs(offset.dot(right)) <= UNDERTOW_HALF_WIDTH
                    && Math.abs(offset.dot(up)) <= UNDERTOW_HALF_HEIGHT;
        });
    }

    /** The same white trail the thrown lance leaves, so the box reads as a volley of it. */
    private static void drawUndertow(Level level, Vec3 origin, Vec3 forward, double length) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 right = rightOf(forward);
        Vec3 up = right.cross(forward).normalize();

        for (int across = -1; across <= 1; across++) {
            for (int high = -1; high <= 1; high++) {
                Vec3 lane = origin
                        .add(right.scale(across * UNDERTOW_HALF_WIDTH * 0.7))
                        .add(up.scale(high * UNDERTOW_HALF_HEIGHT * 0.7));
                for (double along = 0.5; along <= length; along += 0.5) {
                    Vec3 at = lane.add(forward.scale(along));
                    serverLevel.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z,
                            1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    /**
     * Opens the vortex at {@code at}. {@link AegirWhirlpool} owns everything that happens
     * next, so this method is just the cooldown.
     */
    private static InteractionResultHolder<ItemStack> castMaelstrom(Level level, Player player,
                                                                    ItemStack stack, Vec3 at) {
        if (!ready(stack, level, TAG_MAELSTROM_READY)) {
            return InteractionResultHolder.fail(stack);
        }
        startCooldown(stack, level, TAG_MAELSTROM_READY, MAELSTROM_COOLDOWN_TICKS);

        if (!level.isClientSide()) {
            AegirWhirlpool.spawn(level, at, player);
            stack.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(p.getUsedItemHand()));
        }

        return InteractionResultHolder.success(stack);
    }

    /**
     * Where a shift-click in open air puts the vortex: the first solid thing under the
     * crosshair, or the end of the ray if there is nothing out there.
     */
    private static Vec3 maelstromTarget(Level level, Player player) {
        Vec3 origin = player.getEyePosition();
        Vec3 far = origin.add(player.getLookAngle().scale(MAELSTROM_PLACE_RANGE));
        // Fluid.NONE so aiming at the sea opens the vortex on the seabed, not the surface.
        return level.clip(new ClipContext(origin, far,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
    }

    /**
     * The horizontal perpendicular to a look direction. Degenerate only at the exact zenith or
     * nadir, where any perpendicular will do.
     *
     * <p>A method rather than inlined because the result has to be <em>effectively final</em>
     * to be read inside {@link #undertowTargets}' predicate lambda, and the degenerate guard
     * cannot be written as a single assignment.
     */
    private static Vec3 rightOf(Vec3 forward) {
        Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
        return right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
    }

    /**
     * How far an ability reaches before terrain stops it. Clipping down the centre line only,
     * so a mob tucked behind a corner but inside the box still gets caught.
     */
    private static double reachBeforeTerrain(Level level, Player user, Vec3 origin,
                                             Vec3 forward, double maximum) {
        HitResult hit = level.clip(new ClipContext(origin, origin.add(forward.scale(maximum)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
        return hit.getType() == HitResult.Type.MISS
                ? maximum
                : hit.getLocation().subtract(origin).length();
    }

    private static boolean ready(ItemStack stack, Level level, String tag) {
        return level.getGameTime() >= stack.getOrCreateTag().getLong(tag);
    }

    private static void startCooldown(ItemStack stack, Level level, String tag, int ticks) {
        stack.getOrCreateTag().putLong(tag, level.getGameTime() + ticks);
    }

    private static void playSound(Level level, Player user, SoundEvent sound,
                                  float volume, float minPitch, float maxPitch) {
        float pitch = Mth.nextFloat(level.getRandom(), minPitch, maxPitch);
        if (!level.isClientSide()) {
            level.playSound(null, user.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
        } else {
            level.playLocalSound(user.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch, false);
        }
    }
}
