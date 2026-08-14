package com.jyhrie.priestess.weapons.item;

import com.jyhrie.priestess.weapons.ModWeapons;
import com.jyhrie.priestess.weapons.WeaponText;
import com.jyhrie.priestess.weapons.WeaponTiers;
import com.jyhrie.priestess.weapons.entity.WeaponVfx;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Laevatain. Surtr's greatsword — three fire abilities on the three click inputs.
 *
 * <p>Unlike everything else in this package this weapon is <b>not ported from anywhere</b>. It
 * is original content built to {@code docs/WEAPONS.md}, and it lives here because the
 * scaffolding a weapon needs — {@link WeaponTiers}, {@link WeaponText}, the swing packet — is
 * all here. See {@link ModWeapons} for what that costs the package's "delete it and nothing
 * breaks" contract.
 *
 * <h2>The three abilities</h2>
 * <table border="1">
 *   <caption>inputs</caption>
 *   <tr><th>Input</th><th>Name</th><th>Effect</th><th>Cooldown</th></tr>
 *   <tr><td>left click</td><td>Laevatain</td><td>wide sweep in front</td>
 *       <td>the sword's own swing rate, 1.2 s</td></tr>
 *   <tr><td>right click</td><td>Molten Giant</td><td>charge, then a 2×2×5 box in front, all at
 *       once</td><td>3 s</td></tr>
 *   <tr><td>shift + right click</td><td>Twilight</td><td>charge, then a 60° cone, fire erupts
 *       underfoot</td><td>10 s</td></tr>
 * </table>
 *
 * <h2>Why the cooldowns are not all vanilla cooldowns</h2>
 * {@code ItemCooldowns} is keyed by <em>item</em>, so it holds exactly one timer and these need
 * three independent ones — a 10 s Twilight must not lock out a 1.2 s swing. Only the sweep uses
 * the vanilla cooldown, which is also the one the hotbar sweep animation is honestly about;
 * the two abilities keep their ready-times in the stack's NBT as game-time stamps. Stack tags
 * sync to the client on their own, so both sides can answer "is it ready" without a packet.
 */
public class LaevatainItem extends SwordItem {

    // ── The sword ─────────────────────────────────────────────────────────────

    private static final int ATTACK_DAMAGE = 18;

    /**
     * Offset from the player's base attack speed of 4.0, so the final value is 0.8333 attacks a
     * second — one swing per <b>1.2 seconds</b>. Slower than any vanilla sword; the damage and
     * the sweep are what pay for it.
     */
    private static final float ATTACK_SPEED = -1.6F;

    // ── Laevatain, the sweep ──────────────────────────────────────────────────

    private static final double SWEEP_RANGE = 5.0;
    private static final double SWEEP_ARC_DEGREES = 120.0;

    /**
     * Fraction of the sword's damage each caught mob takes.
     *
     * <p>Kept at or below 1.0 on purpose. A mob the player actually connected with is inside
     * its hurt-immunity window when the sweep resolves, and vanilla {@code hurt} ignores a
     * second hit that is not <em>larger</em> than the one still on the clock — so the aimed
     * target takes the melee hit and the sweep passes over it, while everything else around it
     * takes the sweep. That is vanilla's own sweeping-edge rule, borrowed rather than
     * reimplemented, and it is why this class does not clear {@code invulnerableTime} the way
     * {@link DevilsDevastationItem} deliberately does.
     */
    private static final float SWEEP_DAMAGE_FRACTION = 0.75F;

    private static final int SWEEP_BURN_SECONDS = 4;

    // ── Molten Giant, the line ────────────────────────────────────────────────

    /**
     * Ticks of drawing for a full-strength cast, shared by both abilities.
     *
     * <p>One number rather than two <b>because the item model can only have one</b>: the
     * {@code pull} predicate that drives the drawing animation is a single function of the
     * stack and cannot know which ability is being charged. Giving the two different charge
     * times would make the visible draw lie about one of them. Public for that predicate, which
     * lives in {@code WeaponsClient}.
     */
    public static final int CHARGE_TICKS = 20;

    /**
     * Which ability is being drawn, written when the draw starts and read back on release.
     *
     * <p>Both abilities are on the same button and both draw like a bow, so release has to be
     * told which one it is finishing. It cannot work it out: the shift key may have been let go
     * at any point during the draw, and checking it again at release would fire Molten Giant
     * from a Twilight charge.
     */
    private static final String TAG_CHARGING = "LaevatainCharging";
    private static final int CHARGING_NONE = 0;
    private static final int CHARGING_MOLTEN_GIANT = 1;
    private static final int CHARGING_TWILIGHT = 2;

    private static final String TAG_MOLTEN_GIANT_READY = "LaevatainMoltenGiantReady";
    private static final int MOLTEN_GIANT_COOLDOWN_TICKS = 60;      // 3 s

    /** Below this fraction of a full charge the release does nothing and costs no cooldown. */
    private static final float MOLTEN_GIANT_MIN_CHARGE = 0.25F;

    /**
     * The struck volume: a <b>2 × 2 × 5</b> box laid along the crosshair, so 5 blocks of reach
     * with a 2-block square cross-section centred on the aim line.
     *
     * <p>It is an <em>oriented</em> box, not an {@link AABB} — a plain bounding box is
     * axis-aligned and would swell to the diagonal whenever the player faced anything but due
     * north, quietly making the ability much wider on some headings than others. See
     * {@link #fireLine} for how the test is done instead.
     *
     * <p>The whole volume resolves in the tick it is cast: nothing travels down it and there is
     * nothing to dodge once it is out, which is what the 3-second cooldown pays for.
     */
    private static final double MOLTEN_GIANT_LENGTH = 5.0;
    private static final double MOLTEN_GIANT_HALF_WIDTH = 1.0;
    private static final double MOLTEN_GIANT_HALF_HEIGHT = 1.0;

    private static final float MOLTEN_GIANT_DAMAGE_FRACTION = 1.5F;
    private static final int MOLTEN_GIANT_BURN_SECONDS = 8;

    // ── Twilight, the charged cone ────────────────────────────────────────────

    private static final String TAG_TWILIGHT_READY = "LaevatainTwilightReady";
    private static final int TWILIGHT_COOLDOWN_TICKS = 200;         // 10 s

    /** Below this fraction of a full charge the release does nothing and costs no cooldown. */
    private static final float TWILIGHT_MIN_CHARGE = 0.25F;

    private static final double TWILIGHT_RANGE = 10.0;
    private static final double TWILIGHT_ARC_DEGREES = 60.0;
    private static final float TWILIGHT_DAMAGE_FRACTION = 1.25F;
    private static final int TWILIGHT_BURN_SECONDS = 10;

    // ── VFX lifetimes ─────────────────────────────────────────────────────────
    // Ticks, and each one must equal the animation_length of the matching
    // assets/priestess/animations/*.animation.json, which is authored in seconds at 20 ticks
    // to the second. Change one and change the other: too short and the mesh disappears
    // mid-swing, too long and it hangs in the air on its final frame.

    /** laevatain_slash.animation.json — 0.4 s. */
    private static final int SLASH_VFX_TICKS = 8;

    /** laevatain_stab.animation.json — 0.5 s. */
    private static final int STAB_VFX_TICKS = 10;

    /** laevatain_eruption.animation.json — 0.6 s. */
    private static final int ERUPTION_VFX_TICKS = 12;

    public LaevatainItem() {
        super(WeaponTiers.DEMONIC, ATTACK_DAMAGE, ATTACK_SPEED, new Properties());
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.EPIC;
    }

    /**
     * The animated name: white-hot at the edge through gold and orange into deep ember and back.
     * Fewer stops than Devil's Devastation's palette and all of them fire — this is one metal at
     * one temperature, not a whole spectrum. See {@link WeaponText#gradient}.
     */
    @Override
    public Component getName(ItemStack stack) {
        return WeaponText.gradient(Component.translatable(this.getDescriptionId(stack)), 0.3F, 2.0F,
                new int[]{255, 250, 232},   // white hot
                new int[]{255, 236, 170},
                new int[]{255, 210, 92},    // gold
                new int[]{255, 170, 40},
                new int[]{255, 122, 20},    // orange
                new int[]{240, 78, 16},
                new int[]{214, 44, 22},     // red
                new int[]{160, 26, 24},
                new int[]{112, 20, 26},     // deep ember
                new int[]{160, 26, 24},
                new int[]{214, 44, 22},
                new int[]{255, 122, 20},
                new int[]{255, 210, 92},
                new int[]{255, 250, 232}    // back to white, closing the loop
        ).withStyle(ChatFormatting.BOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.translatable("tooltip.priestess.laevatain.flavour"));
        tooltip.add(Component.literal(" "));

        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.priestess.laevatain.left"));
            tooltip.add(Component.translatable("tooltip.priestess.laevatain.left_detail"));
            tooltip.add(Component.literal(" "));
            tooltip.add(Component.translatable("tooltip.priestess.laevatain.right"));
            tooltip.add(Component.translatable("tooltip.priestess.laevatain.right_detail"));
            tooltip.add(Component.literal(" "));
            tooltip.add(Component.translatable("tooltip.priestess.laevatain.shift_right"));
            tooltip.add(Component.translatable("tooltip.priestess.laevatain.shift_right_detail"));
        } else {
            tooltip.add(Component.translatable("tooltip.priestess.hold_shift"));
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.setSecondsOnFire(SWEEP_BURN_SECONDS);
        // Note the absence of `target.invulnerableTime = 0` here — see SWEEP_DAMAGE_FRACTION.
        return super.hurtEnemy(stack, target, attacker);
    }

    // ── Left click: Laevatain ─────────────────────────────────────────────────

    /**
     * The sweep. Called on the server from the swing packet, never directly — a swing is a
     * client-side event and only the server may deal damage, so {@code WeaponSwingEvents} →
     * {@code SwingSlashC2S} → here is the whole chain.
     *
     * <p>Checks both hands because the sword works off-hand, and no-ops for any hand not
     * holding one.
     */
    public static void sweep(Level level, Player user) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = user.getItemInHand(hand);
            if (stack.getItem() != ModWeapons.LAEVATAIN.get()) {
                continue;
            }
            if (user.getCooldowns().isOnCooldown(stack.getItem())) {
                return;
            }

            // One swing's worth of ticks, so the sweep lands at exactly the rate the sword
            // swings rather than as fast as the player can click. At this weapon's attack
            // speed that is 24 ticks — the 1.2 seconds the sword advertises.
            AttributeInstance attribute = user.getAttribute(Attributes.ATTACK_SPEED);
            float attackSpeed = attribute != null ? (float) attribute.getValue() : 4.0F;
            user.getCooldowns().addCooldown(stack.getItem(), (int) (20.0F / attackSpeed));

            if (!level.isClientSide()) {
                float damage = WeaponText.itemAttackDamage(stack) * SWEEP_DAMAGE_FRACTION;
                for (LivingEntity target : targetsInCone(level, user, SWEEP_RANGE, SWEEP_ARC_DEGREES)) {
                    target.hurt(user.damageSources().playerAttack(user), damage);
                    target.setSecondsOnFire(SWEEP_BURN_SECONDS);
                }

                // The slash: a flat plane, always parallel to the ground.
                //
                // Pitch is pinned to zero and the offset is taken from a *flattened* look
                // vector, so neither the plane's angle nor its height answers to the
                // crosshair. Looking at the sky and looking at your boots put the same
                // horizontal disc in the same place, which is honest — the sweep this draws
                // tests a flat 120° fan and never cared about pitch either.
                Vec3 flatForward = user.getLookAngle().multiply(1.0, 0.0, 1.0);
                flatForward = flatForward.lengthSqr() < 1.0E-6
                        ? new Vec3(0.0, 0.0, 1.0)       // looking straight up or down
                        : flatForward.normalize();

                WeaponVfx.spawn(level, ModWeapons.LAEVATAIN_SLASH.get(),
                        user.position().add(flatForward.scale(1.2)).add(0.0, 1.1, 0.0),
                        user.getYRot(), 0.0F, SLASH_VFX_TICKS);
            }

            playSound(level, user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.7F, 0.9F);
        }
    }

    // ── Right click: Molten Giant, and Twilight (shift) ───────────────────────

    /**
     * Both abilities draw like a bow, so this only ever <em>starts</em> a charge. Which one is
     * decided here and written to the stack, because {@link #releaseUsing} cannot ask: the
     * player may well have let go of shift during the draw, and reading the key again at
     * release would fire the wrong ability.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Shift first, or the Molten Giant branch swallows it.
        if (player.isShiftKeyDown()) {
            return beginCharge(level, player, hand, stack, CHARGING_TWILIGHT, TAG_TWILIGHT_READY);
        }
        return beginCharge(level, player, hand, stack, CHARGING_MOLTEN_GIANT, TAG_MOLTEN_GIANT_READY);
    }

    /**
     * Shift-right-clicking a <em>block</em> never reaches {@link #use} — holding shift is
     * exactly what suppresses block interaction, so the click arrives here instead. Twilight is
     * a ground-level ability and would be unusable while looking at terrain without this.
     *
     * <p>Everything else returns PASS and falls through to {@code use}.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        return beginCharge(context.getLevel(), player, context.getHand(), context.getItemInHand(),
                CHARGING_TWILIGHT, TAG_TWILIGHT_READY).getResult();
    }

    /**
     * Starts a draw. Nothing is spent here — the cooldown is only taken in
     * {@link #releaseUsing}, and only if the charge got far enough to fire.
     */
    private static InteractionResultHolder<ItemStack> beginCharge(Level level, Player player,
                                                                  InteractionHand hand, ItemStack stack,
                                                                  int which, String readyTag) {
        if (!ready(stack, level, readyTag)) {
            return InteractionResultHolder.fail(stack);
        }
        stack.getOrCreateTag().putInt(TAG_CHARGING, which);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;   // effectively "until released"
    }

    /**
     * Whichever ability was drawn fires here. {@code remaining} counts <em>down</em>, which is
     * why the charge is duration minus remaining.
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!(entity instanceof Player player)) {
            return;
        }

        // Cleared whatever happens next, so a draw that fires nothing cannot leave a stale
        // value behind for the next release to act on.
        int which = stack.getOrCreateTag().getInt(TAG_CHARGING);
        stack.getOrCreateTag().putInt(TAG_CHARGING, CHARGING_NONE);

        int heldTicks = getUseDuration(stack) - remaining;

        switch (which) {
            case CHARGING_MOLTEN_GIANT -> {
                float charge = Math.min((float) heldTicks / CHARGE_TICKS, 1.0F);
                if (charge >= MOLTEN_GIANT_MIN_CHARGE) {
                    castMoltenGiant(level, player, stack, charge);
                }
            }
            case CHARGING_TWILIGHT -> {
                float charge = Math.min((float) heldTicks / CHARGE_TICKS, 1.0F);
                if (charge >= TWILIGHT_MIN_CHARGE) {
                    castTwilight(level, player, stack, charge);
                }
            }
            default -> {
                // Nothing was drawn — a stack that changed hands mid-draw, or a reload.
            }
        }
    }

    /**
     * The line, on release. The box itself resolves in one go — the whole 2 × 2 × 5 lands in the
     * tick it is cast rather than a projectile travelling out and hitting things in the order it
     * reaches them. Charge buys damage, not reach: the box is the same size at any draw, so a
     * rushed cast covers the ground the player aimed at and simply hits for less.
     */
    private static void castMoltenGiant(Level level, Player player, ItemStack stack, float charge) {
        if (!ready(stack, level, TAG_MOLTEN_GIANT_READY)) {
            return;
        }
        startCooldown(stack, level, TAG_MOLTEN_GIANT_READY, MOLTEN_GIANT_COOLDOWN_TICKS);

        if (!level.isClientSide()) {
            fireLine(level, player, stack, charge);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(p.getUsedItemHand()));
        }

        playSound(level, player, SoundEvents.BLAZE_SHOOT, 1.4F, 0.6F, 0.8F);
    }

    /**
     * The 2 × 2 × 5 box in front of the player, resolved in one go.
     *
     * <p>Every candidate is rewritten into the player's own frame — forward along the crosshair,
     * right and up across it — and kept if it lands inside the box on all three axes. That is
     * what makes the volume an oriented box rather than an axis-aligned one, so it is the same
     * shape on every heading.
     *
     * <p>The box is shortened at the first solid block, so it does not reach through a wall.
     */
    private static void fireLine(Level level, Player user, ItemStack stack, float charge) {
        Vec3 origin = user.getEyePosition();
        Vec3 forward = user.getLookAngle();

        // Right is the horizontal perpendicular; taken against world up, which is degenerate
        // only when looking exactly at the zenith or nadir — there, any perpendicular will do.
        Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        Vec3 up = right.cross(forward).normalize();

        // Stop at terrain. Clipping down the centre line is an approximation — a mob tucked
        // behind a corner but inside the box still gets hit — and the right trade for a 5-block
        // ability, since the alternative is a per-target line-of-sight check.
        double length = MOLTEN_GIANT_LENGTH;
        Vec3 far = origin.add(forward.scale(length));
        HitResult hit = level.clip(new ClipContext(origin, far,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
        if (hit.getType() != HitResult.Type.MISS) {
            length = hit.getLocation().subtract(origin).length();
        }

        float damage = WeaponText.itemAttackDamage(stack) * MOLTEN_GIANT_DAMAGE_FRACTION * charge;

        // Broad phase: an axis-aligned box big enough to contain the oriented one whatever the
        // heading. Cheap, and the exact test below throws the corners back out.
        AABB search = new AABB(origin, origin.add(forward.scale(length)))
                .inflate(Math.max(MOLTEN_GIANT_HALF_WIDTH, MOLTEN_GIANT_HALF_HEIGHT));

        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, search,
                other -> other != user && other.isAlive() && !other.isSpectator())) {

            Vec3 offset = candidate.getBoundingBox().getCenter().subtract(origin);
            double alongForward = offset.dot(forward);
            if (alongForward < 0.0 || alongForward > length) {
                continue;
            }
            if (Math.abs(offset.dot(right)) > MOLTEN_GIANT_HALF_WIDTH) {
                continue;
            }
            if (Math.abs(offset.dot(up)) > MOLTEN_GIANT_HALF_HEIGHT) {
                continue;
            }

            candidate.hurt(user.damageSources().playerAttack(user), damage);
            candidate.setSecondsOnFire(MOLTEN_GIANT_BURN_SECONDS);
        }

        // The stab, laid along the crosshair from the eye. The mesh is built five blocks long
        // in +Z, which is the box's full reach, so the two agree without a scale factor — a
        // wall-shortened cast still draws the whole spike and lets it sink into the wall,
        // which reads better than a spike that stops politely short.
        WeaponVfx.spawn(level, ModWeapons.LAEVATAIN_STAB.get(), origin,
                user.getYRot(), user.getXRot(), STAB_VFX_TICKS);
    }

    // ── Shift + right click: Twilight ─────────────────────────────────────────

    /**
     * The cone. Everything inside 10 blocks and 60° takes a hit, catches fire, and has an
     * eruption of flame thrown up underneath it.
     *
     * <p><b>The eruption places no blocks.</b> It is particles plus {@code setSecondsOnFire} on
     * the mobs themselves, so the ability burns what it catches and nothing else — no spread,
     * no scorched terrain, and nothing left behind once the mob is dead.
     */
    private static void castTwilight(Level level, Player player, ItemStack stack, float charge) {
        if (!ready(stack, level, TAG_TWILIGHT_READY)) {
            return;
        }
        startCooldown(stack, level, TAG_TWILIGHT_READY, TWILIGHT_COOLDOWN_TICKS);

        if (!level.isClientSide()) {
            // The cone's reach and angle are fixed; only the damage answers to the charge, so a
            // half-charged cast covers the same ground and hits for less rather than becoming a
            // different, smaller ability the player has to re-aim.
            float damage = WeaponText.itemAttackDamage(stack) * TWILIGHT_DAMAGE_FRACTION * charge;
            for (LivingEntity target : targetsInCone(level, player, TWILIGHT_RANGE, TWILIGHT_ARC_DEGREES)) {
                target.hurt(player.damageSources().playerAttack(player), damage);
                target.setSecondsOnFire(TWILIGHT_BURN_SECONDS);
                eruptUnder(level, target);
            }
            drawArc(level, player, TWILIGHT_RANGE, TWILIGHT_ARC_DEGREES);
            stack.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(p.getUsedItemHand()));
        }

        playSound(level, player, SoundEvents.FIRECHARGE_USE, 1.5F, 0.5F, 0.7F);
    }

    /**
     * The floor erupting under a caught mob: a column of flame standing up out of the ground
     * where it is, plus a handful of lava flecks for weight the mesh cannot give.
     *
     * <p>Spawned at the mob's feet rather than parented to it, so the eruption stays where the
     * ground opened even if the mob is knocked away from it — this is the floor doing
     * something, not a status effect riding the target.
     *
     * <p>The yaw is the mob's own, which is as good as random and stops a row of them all
     * erupting in identical alignment.
     */
    private static void eruptUnder(Level level, LivingEntity target) {
        WeaponVfx.spawn(level, ModWeapons.LAEVATAIN_ERUPTION.get(), target.position(),
                target.getYRot(), 0.0F, ERUPTION_VFX_TICKS);

        if (level instanceof ServerLevel serverLevel) {
            Vec3 feet = target.position();
            serverLevel.sendParticles(ParticleTypes.LAVA, feet.x, feet.y + 0.1, feet.z,
                    4, 0.15, 0.1, 0.15, 0.0);
        }
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    /**
     * Every living thing inside {@code range} of the user and within {@code arcDegrees} of where
     * they are looking. The arc is the full width, so 60 means 30 either side of the crosshair.
     *
     * <p>Compares against the cosine of the half-angle rather than taking an {@code acos} per
     * candidate: the dot product of two unit vectors already <em>is</em> that cosine.
     */
    private static List<LivingEntity> targetsInCone(Level level, Player user,
                                                    double range, double arcDegrees) {
        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        double minimumCosine = Math.cos(Math.toRadians(arcDegrees * 0.5));

        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                user.getBoundingBox().inflate(range),
                other -> other != user && other.isAlive() && !other.isSpectator())) {

            Vec3 toTarget = candidate.getBoundingBox().getCenter().subtract(eye);
            double distance = toTarget.length();
            if (distance > range || distance < 1.0E-4) {
                continue;
            }
            if (toTarget.scale(1.0 / distance).dot(look) < minimumCosine) {
                continue;
            }
            found.add(candidate);
        }
        return found;
    }

    /** Flame particles along the arc, so an AOE the player cannot see still reads as one. */
    private static void drawArc(Level level, Player user, double range, double arcDegrees) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 eye = user.getEyePosition();
        float yaw = user.getYRot();
        int steps = 24;

        for (int i = 0; i <= steps; i++) {
            double offset = -arcDegrees * 0.5 + (arcDegrees * i / steps);
            double radians = Math.toRadians(yaw + offset + 90.0);
            // Drawn at chest height and at the arc's outer edge, which is the part of the
            // shape a player needs to judge — the inside of a cone tells them nothing.
            double x = eye.x + Math.cos(radians) * range * 0.8;
            double z = eye.z + Math.sin(radians) * range * 0.8;
            serverLevel.sendParticles(flame(), x, eye.y - 0.6, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static ParticleOptions flame() {
        return ParticleTypes.FLAME;
    }

    private static boolean ready(ItemStack stack, Level level, String tag) {
        return level.getGameTime() >= stack.getOrCreateTag().getLong(tag);
    }

    private static void startCooldown(ItemStack stack, Level level, String tag, int ticks) {
        stack.getOrCreateTag().putLong(tag, level.getGameTime() + ticks);
    }

    private static void playSound(Level level, Player user, net.minecraft.sounds.SoundEvent sound,
                                  float volume, float minPitch, float maxPitch) {
        float pitch = Mth.nextFloat(level.getRandom(), minPitch, maxPitch);
        if (!level.isClientSide()) {
            level.playSound(null, user.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
        } else {
            level.playLocalSound(user.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch, false);
        }
    }
}
