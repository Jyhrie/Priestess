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
 * <p>Original content, not ported, and it lives here for the same reason {@link LaevatainItem}
 * does: the scaffolding a weapon needs — {@link WeaponTiers}, {@link WeaponText},
 * {@link WeaponPhysics}, the swing packet — is all in this package. See {@link ModWeapons} for
 * what that costs the "delete the folder" contract.
 *
 * <h2>The three abilities</h2>
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
 * <h2>Where the cooldowns live</h2>
 * The same split {@link LaevatainItem} uses and for the same reason: {@code ItemCooldowns} is
 * keyed by <em>item</em> and holds exactly one timer, so a 30-second Maelstrom would otherwise
 * lock out the throw. The throw takes the vanilla cooldown — it is the one that should track
 * the swing rate and drive the hotbar sweep — and the two right-click abilities keep their
 * ready-times in the stack's NBT as game-time stamps. Stack tags sync to the client on their
 * own, so both sides can answer "is it ready" without a packet.
 *
 * <h2>This weapon is main-hand only</h2>
 * Unlike the two weapons written before it, {@link #thrust} reads the main hand and no other.
 * Off-hand ability firing is not wanted, and a both-hands scan combined with a main-hand-only
 * client check is what lets one swing fire a <em>different</em> weapon held in the off hand.
 */
public class AegirGreatspearItem extends ConfiguredSwordItem {

    // ── The spear ─────────────────────────────────────────────────────────────
    //
    // Damage, swing speed and all three ability damages live in
    // config/priestess/weapon.toml — see WeaponStats and ConfiguredSwordItem. What is left here
    // is the geometry and the timings, which are shape rather than balance.
    //
    // At the default -2.8 offset from the player's base 4.0 the spear swings 1.2 times a
    // second — one swing per 0.83 seconds, and the 16-tick cooldown that `20 / attackSpeed`
    // works out to below. Slower than a sword and faster than Laevatain: it is a reach weapon,
    // and the throw is what it is really swinging.

    // ── Left click: Tide-Piercer ──────────────────────────────────────────────

    private static final float TIDE_SPEED = 2.2F;

    /**
     * Spread in degrees. Zero: this is a spear thrown down the crosshair, and a lance that lands
     * next to what you aimed at would make the pull feel arbitrary.
     */
    private static final float TIDE_INACCURACY = 0.0F;

    // ── Right click: Undertow ─────────────────────────────────────────────────

    private static final String TAG_UNDERTOW_READY = "AegirUndertowReady";
    private static final int UNDERTOW_COOLDOWN_TICKS = 200;      // 10 s

    /**
     * The struck volume: <b>5 × 5 × 5</b>, laid along the crosshair — five blocks of reach on a
     * five-block square cross-section centred on the aim line.
     *
     * <p>An <em>oriented</em> box, not an {@link AABB}. A plain bounding box is axis-aligned and
     * would swell to its own diagonal whenever the player faced anything but due north, quietly
     * making the ability half again as wide on some headings. See {@link #undertowTargets}.
     */
    private static final double UNDERTOW_LENGTH = 5.0;
    private static final double UNDERTOW_HALF_WIDTH = 2.5;
    private static final double UNDERTOW_HALF_HEIGHT = 2.5;

    /** Stronger than the throw's: this is the ability whose whole job is repositioning a group. */
    private static final double UNDERTOW_PULL_STRENGTH = 0.75;

    // ── Shift + right click: Maelstrom ────────────────────────────────────────

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

    /**
     * The animated name: deep trench blue up through tidal green and cyan into white foam, and
     * back down. The mirror image of {@link LaevatainItem}'s palette in structure and its
     * opposite in temperature — one body of water at one depth, not a spectrum.
     */
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

    // ── Left click: Tide-Piercer ──────────────────────────────────────────────

    /**
     * Throws the lance. Called on the server from the swing packet, never directly — a swing is
     * a client-side event and only the server may spawn entities, so
     * {@code WeaponSwingEvents} → {@code SwingSlashC2S} → here is the whole chain.
     *
     * <p><b>Main hand only</b>, deliberately; see the class note.
     */
    public static void thrust(Level level, Player user) {
        ItemStack stack = user.getMainHandItem();
        if (stack.getItem() != ModWeapons.AEGIR_GREATSPEAR.get()) {
            return;
        }
        if (user.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }

        // One swing's worth of ticks, so the throw lands at exactly the rate the spear swings
        // rather than as fast as the player can click. At this weapon's attack speed that is
        // 16 ticks — the 0.83 seconds the spear advertises.
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

    // ── Right click: Undertow, and Maelstrom (shift) ──────────────────────────

    /**
     * Neither ability charges, so this is the whole of the right-click path: pick one, fire it,
     * take its cooldown.
     */
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
     * Shift-right-clicking a <em>block</em> never reaches {@link #use} — holding shift is
     * exactly what suppresses block interaction, so the click arrives here instead. Maelstrom is
     * aimed at a spot on the ground and would be unusable while looking at terrain without this,
     * which is to say unusable.
     *
     * <p>The clicked location is used directly rather than re-raycast: the player picked a point
     * on a face, and that point is a better answer than anything derived afterwards.
     *
     * <p>Everything else returns PASS and falls through to {@code use}.
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

    /**
     * The box, resolved in the tick it is cast. Everything caught takes a hit and is dragged
     * toward the player; nothing travels, so there is nothing to dodge once it is out, which is
     * what the ten-second cooldown pays for.
     */
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
     * Everything inside the oriented 5 × 5 × 5 box.
     *
     * <p>Every candidate is rewritten into the player's own frame — forward along the crosshair,
     * right and up across it — and kept only if it lands inside the box on all three axes. That
     * is what makes the volume oriented rather than axis-aligned, so it is the same shape on
     * every heading.
     */
    private static List<LivingEntity> undertowTargets(Level level, Player user, Vec3 origin,
                                                      Vec3 forward, double length) {
        Vec3 right = rightOf(forward);
        Vec3 up = right.cross(forward).normalize();

        // Broad phase: an axis-aligned box big enough to contain the oriented one whatever the
        // heading. Cheap, and the exact test below throws the corners back out.
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

    /**
     * The same white trail the thrown lance leaves, drawn as a volley filling the box, so an
     * area effect the player cannot otherwise see still reads as one.
     */
    private static void drawUndertow(Level level, Vec3 origin, Vec3 forward, double length) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 right = rightOf(forward);
        Vec3 up = right.cross(forward).normalize();

        // A 3 × 3 grid of lances across the box's face, each drawn as a run of points down its
        // length — the same END_ROD white AegirTide trails, so the ability looks like a volley
        // of the left click rather than like a separate effect.
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

    // ── Shift + right click: Maelstrom ────────────────────────────────────────

    /**
     * Opens the vortex at {@code at}. The whirlpool is an entity and owns everything that
     * happens next — see {@link AegirWhirlpool} — so this method is the cooldown and the sound.
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
        // ClipContext.Fluid.NONE so aiming at the sea opens the vortex on the seabed rather
        // than on the surface, which is the reading that matches what it is.
        return level.clip(new ClipContext(origin, far,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    /**
     * The horizontal perpendicular to a look direction — the "right" axis of the player's own
     * frame, which with {@code forward} and their cross product spans the oriented box.
     *
     * <p>Taken against world up, which is degenerate only when looking exactly at the zenith or
     * nadir; there, any perpendicular will do and world +X is as good as another.
     *
     * <p>A method rather than two lines inlined at each site because the result has to be
     * <em>effectively final</em> to be read inside {@link #undertowTargets}' predicate lambda,
     * and the guard above cannot be written as a single assignment without one.
     */
    private static Vec3 rightOf(Vec3 forward) {
        Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
        return right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
    }

    /**
     * How far an ability reaches before terrain stops it.
     *
     * <p>Clipping down the centre line is an approximation — a mob tucked behind a corner but
     * inside the box still gets caught — and the right trade for a five-block ability, since the
     * alternative is a per-target line-of-sight check.
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
