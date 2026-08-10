package com.jyhrie.priestess.progression;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.entity.ModEntities;
import com.jyhrie.priestess.item.ModItems;
import com.jyhrie.priestess.world.dimension.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The dungeons, declared once, with everything the two progression mechanics need to know
 * about them.
 *
 * <p>This is the only place the three otherwise-unrelated facts about a dungeon meet: which
 * <em>structure</em> is its physical extent, what <em>clears</em> it, and which
 * <em>biomes</em> it unlocks flight in. Keeping them together is the point — a dungeon whose
 * structure and boss were declared in different files would very easily end up sealing an
 * area nothing can open.
 *
 * <h2>Adding one</h2>
 * Add a constant. Nothing else has to change: {@link DungeonLockdown} iterates these to find
 * which dungeon a position is inside, {@link FlightRestriction} iterates them to find which
 * gate a biome sits behind, and {@link DungeonProgress} keys its records off
 * {@link #getSerializedName()}.
 *
 * <h2>The two ways a dungeon is cleared</h2>
 * Most are cleared by killing a boss. Rhine Lab has none — it ends in a chest rather than a
 * fight — so it is cleared by <em>picking up</em> the item that chest holds. Both are
 * declared here and handled in {@link DungeonProgress}; a dungeon may use either, and one
 * with neither can never be sealed at all (see {@link #canBeSealed()}).
 */
public enum Dungeon {

    /**
     * Mansfield State Prison. Cleared by Jesselton Williams, who drops the Master Key —
     * so the lockdown lifts at exactly the moment the chapter says it should.
     */
    MANSFIELD_BREAK("mansfield_state_prison",
            () -> ModEntities.MB_JESSELTON_WILLIAMS.get(), null, Set.of()),

    /** Dorothy's Vision. Cleared by "Awaken", which the terminal inside summons. */
    DOROTHYS_VISION("dorothys_vision",
            () -> ModEntities.DV_AWAKEN.get(), null, Set.of()),

    /**
     * Rhine Lab HQ. The only dungeon with no boss in it, so the Blueprint in the Director's
     * Office is what clears it — which also makes it the last of the three, and therefore
     * the right one to hang Columbia's sky on.
     *
     * <p>Note the ordering that creates: Columbia is flight-locked until Rhine Lab is done,
     * and Rhine Lab is where Movement I ends. Clearing the movement is what earns the sky.
     */
    RHINE_LAB("rhine_lab_hq",
            null, () -> ModItems.BLUEPRINT_ORIGINIUM_REFINEMENT.get(), Set.of(ModBiomes.COLUMBIA)),

    /**
     * Under Tides. <b>Has no structure yet</b>, so it has no physical extent and nothing is
     * sealed — the lockdown skips it silently until one is declared in {@code ModStructures}.
     * Bishop Quintus already clears it, and it unlocks no biome because Ægir has none of its
     * own painted on the map yet; both are one-line changes here when they exist.
     */
    UNDER_TIDES(null,
            () -> ModEntities.SV_BISHOP_QUINTUS.get(), null, Set.of());

    @Nullable
    private final ResourceKey<Structure> structure;
    @Nullable
    private final Supplier<EntityType<?>> boss;
    @Nullable
    private final Supplier<Item> clearedByPickingUp;
    private final Set<ResourceKey<Biome>> unlocksFlightIn;

    Dungeon(@Nullable String structureId,
            @Nullable Supplier<EntityType<?>> boss,
            @Nullable Supplier<Item> clearedByPickingUp,
            Set<ResourceKey<Biome>> unlocksFlightIn) {
        this.structure = structureId == null ? null
                : ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(Priestess.MOD_ID, structureId));
        this.boss = boss;
        this.clearedByPickingUp = clearedByPickingUp;
        this.unlocksFlightIn = unlocksFlightIn;
    }

    /** Stable across renames of the constant — this is what ends up in save data. */
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Whether this dungeon has any way of being cleared. One that has none is never sealed. */
    public boolean canBeSealed() {
        return structure != null && (boss != null || clearedByPickingUp != null);
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

    /**
     * Whether {@code pos} is inside this dungeon's built rooms.
     *
     * <p>Piece-accurate rather than whole-bounding-box: {@code getStructureWithPieceAt} tests
     * the individual jigsaw pieces, so an empty stretch of world that merely falls between
     * two wings of a structure is not sealed. Every dungeon here is a single {@code .nbt}
     * today, which makes the two identical — but they stop being identical the moment a
     * dungeon grows a second piece, and the piece-accurate answer is the one that stays
     * right.
     */
    public boolean contains(ServerLevel level, BlockPos pos) {
        if (structure == null) {
            return false;
        }
        return level.structureManager().getStructureWithPieceAt(pos, structure).isValid();
    }

    /** Every dungeon that could seal something, in declaration order. */
    public static List<Dungeon> sealable() {
        return SEALABLE;
    }

    private static final List<Dungeon> SEALABLE =
            List.of(values()).stream().filter(Dungeon::canBeSealed).toList();
}
