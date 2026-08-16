package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.world.terra.TerraMapPreview;
import com.mojang.logging.LogUtils;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Renders {@code docs/terra_world_preview.png} during datagen.
 *
 * <p>Unusual in two deliberate ways. It writes outside {@code src/generated/resources},
 * because the preview is documentation rather than a datapack file. And it ignores
 * {@link CachedOutput}: its real inputs are the map PNGs and the warp constants in the Java,
 * neither of which the cache tracks, so being told it is up to date when it is not would be
 * worse than re-rendering a small image every run.
 *
 * <p>It runs inside datagen because touching {@code TerraRegion} initialises
 * {@code Registries}, which throws unless the game has bootstrapped.
 */
public class ModTerraPreviewProvider implements DataProvider {

    private static final Logger LOG = LogUtils.getLogger();

    private final PackOutput output;

    public ModTerraPreviewProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.runAsync(() -> {
            try {
                LOG.info("{}", TerraMapPreview.render(previewFile()));
            } catch (Exception e) {
                // A broken preview must not fail the build; the datapack is still valid.
                LOG.error("Could not render the Terra preview", e);
            }
        });
    }

    /**
     * {@code src/generated/resources} -> the project root -> {@code docs/}.
     * Derived from the pack output rather than the working directory, which during
     * {@code runData} is {@code run-data/} and not the project root.
     */
    private File previewFile() {
        Path generatedResources = output.getOutputFolder().toAbsolutePath().normalize();
        Path projectRoot = generatedResources.getParent().getParent().getParent();
        return projectRoot.resolve("docs").resolve("terra_world_preview.png").toFile();
    }

    @Override
    public String getName() {
        return "Terra map preview";
    }
}
