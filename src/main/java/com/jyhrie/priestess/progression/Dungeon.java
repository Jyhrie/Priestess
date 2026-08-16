package com.jyhrie.priestess.progression;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.entity.ModEntities;
import com.jyhrie.priestess.item.ModItems;
import com.jyhrie.priestess.world.dimension.ModBiomes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Supplier;

/**
 * The dungeons, declared once, with everything the two progression mechanics need.
 *
 * <p><b>Adding one:</b> add a constant, and nothing else changes. {@link DungeonLockdown} and
 * {@link FlightRestriction} iterate these, and {@link DungeonProgress} keys off
 * {@link #getSerializedName()}. The {@linkplain #sealedBlocks() tag} is derived from the
 * constant's own name, so a new dungeon has one the moment it exists and cannot accidentally
 * point at another dungeon's blocks.
 *
 * <p>Cleared by killing a boss, or — for a dungeon that ends in a chest rather than a fight —
 * by picking up an item. A dungeon with neither can never seal anything; see
 * {@link #hasClearCondition()}.
 *
 * <p>A dungeon has no <em>physical extent</em> here: the lockdown gates a declared set of
 * blocks rather than an area, so a structure's pipes, doors and decoration stay mineable.
 */
public enum Dungeon {

    /**
     * Mansfield State Prison. Cleared by Jesselton Williams, who drops the Master Key —
     * so the lockdown lifts at exactly the moment the chapter says it should.
     */
    MANSFIELD_BREAK(() -> ModEntities.MB_JESSELTON_WILLIAMS.get(), null, Set.of()),

    /** Dorothy's Vision. Cleared by "Awaken", which the terminal inside summons. */
    DOROTHYS_VISION(() -> ModEntities.DV_AWAKEN.get(), null, Set.of()),

    /**
     * Rhine Lab HQ. No boss, so the Blueprint in the Director's Office clears it. It is also
     * where Movement I ends, which is why Columbia's sky hangs off it — clearing the movement
     * is what earns flight.
     */
    RHINE_LAB(null, () -> ModItems.BLUEPRINT_ORIGINIUM_REFINEMENT.get(), Set.of(ModBiomes.COLUMBIA)),

    /**
     * Under Tides. Bishop Quintus clears it; it unlocks no biome because Ægir has none painted
     * on the map yet, and it gates no blocks because nothing has been tagged to it.
     */
    UNDER_TIDES(() -> ModEntities.SV_BISHOP_QUINTUS.get(), null, Set.of());

    @Nullable
    private final Supplier<EntityType<?>> boss;
    @Nullable
    private final Supplier<Item> clearedByPickingUp;
    private final Set<ResourceKey<Biome>> unlocksFlightIn;
    private final TagKey<Block> sealedBlocks;

    Dungeon(@Nullable Supplier<EntityType<?>> boss,
            @Nullable Supplier<Item> clearedByPickingUp,
            Set<ResourceKey<Biome>> unlocksFlightIn) {
        this.boss = boss;
        this.clearedByPickingUp = clearedByPickingUp;
        this.unlocksFlightIn = unlocksFlightIn;
        this.sealedBlocks = TagKey.create(Registries.BLOCK,
                new ResourceLocation(Priestess.MOD_ID, "sealed_by/" + getSerializedName()));
    }

    /** Stable across renames of the constant — this is what ends up in save data. */
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Whether anything in the game can clear this dungeon. One that nothing clears must never
     * seal anything — that is a locked door with no key, and it is the one failure mode of this
     * system a player cannot recover from. {@link DungeonProgress#isCleared} fails open on it
     * for exactly that reason.
     */
    public boolean hasClearCondition() {
        return boss != null || clearedByPickingUp != null;
    }

    /**
     * The blocks this dungeon seals wherever they stand, as {@code priestess:sealed_by/<name>}.
     *
     * <p>Membership is the whole rule: a block in here cannot be broken by a player who has
     * not cleared this dungeon, in or out of the structure, and drops nothing because the
     * break never happens. Populated in {@code ModBlockTagsProvider}.
     */
    public TagKey<Block> sealedBlocks() {
        return sealedBlocks;
    }

    /** Whether {@code state} is one of this dungeon's gated blocks. */
    public boolean seals(BlockState state) {
        return state.is(sealedBlocks);
    }

    public boolean isClearedBy(EntityType<?> type) {
        return boss != null && boss.get() == type;
    }

    public boolean isClearedByPickingUp(Item item) {
        return clearedByPickingUp != null && clearedByPickingUp.get() == item;
    }

    public Set<ResourceKey<Biome>> unlocksFlightIn() {
        return unlocksFlightIn;
    }
}
