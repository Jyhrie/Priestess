package com.jyhrie.priestess.entity;

import com.jyhrie.priestess.entity.bosses.DvAwaken;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * The parts of being a boss that are the same for every boss: a bar at the top of the
 * screen, and not being something the world can quietly take away from you.
 *
 * <p>Everything here is <em>mechanical</em> — true of any boss by definition. What is not here
 * is the fight: there is no {@code tickBoss()} template and no phase machinery, because the
 * bosses' tick loops share only a silhouette and a shared shape forced over them would grow
 * flags to opt out of itself. Subclasses override {@link #customServerAiStep} and call
 * {@code super}; that is the whole contract.
 *
 * <p>The bar is named from the entity type, so it follows the language file. Progress is
 * health by default; a boss whose bar should count something else overrides
 * {@link #barProgress()} rather than writing to {@code bossEvent} from its own tick.
 *
 * <p>Adding a boss:
 * <ol>
 *   <li>extend this, passing a bar colour and overlay to the constructor,</li>
 *   <li>write a {@code static AttributeSupplier.Builder attributes()} and register it in
 *       {@link ModEntities#registerAttributes},</li>
 *   <li>register the {@link EntityType}, a renderer, a name and a spawn egg — the same five
 *       steps any mob needs, listed on {@link ModEntities}.</li>
 * </ol>
 * Nothing here has to be touched to add one.
 */
public abstract class BossMonster extends Monster {

    /** Protected because subclasses recolour it on a phase change. */
    protected final ServerBossEvent bossEvent;

    protected BossMonster(EntityType<? extends BossMonster> type, Level level,
                          BossEvent.BossBarColor colour, BossEvent.BossBarOverlay overlay) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(
                Component.translatable(type.getDescriptionId()), colour, overlay);
    }

    /**
     * What the bar shows, in [0,1]. Called once per tick from {@link #customServerAiStep}, so
     * an override is the single place the bar is decided.
     */
    protected float barProgress() {
        return this.getHealth() / this.getMaxHealth();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(barProgress());
    }

    // The bar is not automatic: vanilla calls these when a player starts and stops tracking
    // the entity, and without them it never appears for anyone.

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    /** Otherwise a name tag renames the mob and leaves the bar saying the old thing. */
    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossEvent.setName(this.getDisplayName());
    }

    /** A boss that despawns because nobody stood near it is a boss you can lose. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
