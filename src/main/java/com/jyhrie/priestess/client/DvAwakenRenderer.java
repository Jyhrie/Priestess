package com.jyhrie.priestess.client;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.entity.bosses.DvAwaken;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Draws {@link DvAwaken} from its Blockbench model.
 *
 * <p>This is the one mob that does not go through {@link PriestessMobRenderer}, and it
 * cannot: that class extends {@code MobRenderer} and takes a vanilla {@code EntityModel},
 * whereas GeckoLib renders from its own {@code GeoModel} through a parallel hierarchy. The
 * two do not meet, so the odd one out gets its own file — which is the rule
 * {@code PriestessMobRenderer} already states for a mob that grows something the shared
 * renderer cannot express.
 *
 * <h2>Where the files have to live</h2>
 * {@link DefaultedEntityGeoModel} derives all three resource paths from the single name
 * below, so {@code new ResourceLocation(MOD_ID, "dv_awaken")} means exactly:
 * <ul>
 *   <li>{@code assets/priestess/geo/entity/dv_awaken.geo.json}</li>
 *   <li>{@code assets/priestess/textures/entity/dv_awaken.png}</li>
 *   <li>{@code assets/priestess/animations/entity/dv_awaken.animation.json}</li>
 * </ul>
 * The last of those does not exist yet and does not need to: GeckoLib resolves it lazily,
 * only when a controller asks for a clip by name, and {@code DvAwaken} registers none.
 *
 * <p>The one-argument constructor is load-bearing. Its two-argument sibling turns on head
 * tracking, which makes GeckoLib look up a bone literally named {@code head} — and this
 * model's eleven bones are {@code bb_main}, {@code Ring}, {@code Prism} and friends, so
 * asking for head tracking would be asking for a bone that is not there.
 */
public class DvAwakenRenderer extends GeoEntityRenderer<DvAwaken> {

    /**
     * Draw scale. Must be kept in step with the hitbox on {@code ModEntities.DV_AWAKEN}, which
     * is {@code SCALE} times the 1.5-block box the mob was registered with — a model that
     * outgrows its hitbox is a boss with a sword-swing that misses the part you aimed at.
     */
    private static final float SCALE = 3.0F;

    public DvAwakenRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(new ResourceLocation(Priestess.MOD_ID, "dv_awaken")));

        // Scales about the entity origin, which sits at the model's feet (its geometry runs
        // from y=0 upward), so growing it pushes the silhouette up rather than sinking it
        // into the floor.
        withScale(SCALE);

        // 0.8 was sized for the 1.5-block placeholder; the shadow has to grow with the mob
        // or a six-block boss casts a puddle.
        this.shadowRadius = 0.8F * SCALE;
    }
}
