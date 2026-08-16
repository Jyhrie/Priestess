package com.jyhrie.priestess.client;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.BossSummonerBlock;
import com.jyhrie.priestess.block.entity.BossSummonerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.model.GeoModel;

/**
 * Which geometry and which texture a boss altar is drawn with.
 *
 * <p>One model class covers both altars because everything it needs it reads off the block
 * entity at render time: geometry from {@link BossSummonerBlock#modelName()}, texture from the
 * block's registry name plus its {@code ARMED} value.
 *
 * <pre>
 * assets/priestess/geo/block/boss_summoner.geo.json
 * assets/priestess/textures/block/boss_summoner/jesselton_projector.png
 * assets/priestess/textures/block/boss_summoner/jesselton_projector_spent.png
 * assets/priestess/textures/block/boss_summoner/dorothys_terminal.png
 * assets/priestess/textures/block/boss_summoner/dorothys_terminal_spent.png
 * </pre>
 *
 * <p>The subfolder is not decoration: {@code textures/block/jesselton_projector.png} already
 * exists as a 16x16 tile for the item model and break particles, whereas these are 128x128 UV
 * sheets that mean nothing except against the geo file above.
 *
 * <p>See {@code docs/BOSS_SPAWNERS.md}.
 */
public class BossSummonerModel extends GeoModel<BossSummonerBlockEntity> {

    private static final String GEO_PATH = "geo/block/";
    private static final String TEXTURE_PATH = "textures/block/boss_summoner/";

    @Override
    public ResourceLocation getModelResource(BossSummonerBlockEntity altar) {
        Block block = altar.getBlockState().getBlock();
        String name = block instanceof BossSummonerBlock summoner
                ? summoner.modelName()
                // Unreachable in practice — this renderer is bound to a block entity type whose
                // only valid blocks are summoners — but a render path that throws on a
                // half-broken world is worse than one that draws the default shape.
                : "boss_summoner";
        return new ResourceLocation(Priestess.MOD_ID, GEO_PATH + name + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BossSummonerBlockEntity altar) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(altar.getBlockState().getBlock());
        String name = blockId == null ? "jesselton_projector" : blockId.getPath();
        boolean armed = altar.getBlockState().getValue(BossSummonerBlock.ARMED);
        return new ResourceLocation(Priestess.MOD_ID,
                TEXTURE_PATH + name + (armed ? "" : "_spent") + ".png");
    }

    /**
     * None. The altar's only movement is the core turning, and the renderer writes that bone
     * rotation directly — see {@code BossSummonerBlockEntity.registerControllers} for why, and
     * what adding real clips would involve.
     *
     * <p>Returning null is the supported way to say "static geometry"; GeckoLib resolves the
     * animation file lazily and never asks.
     */
    @Override
    public ResourceLocation getAnimationResource(BossSummonerBlockEntity altar) {
        return null;
    }
}
