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
 * <p>Unusual for a provider in two ways, both deliberate:
 * <ul>
 *   <li>It writes outside {@code src/generated/resources}. The preview is documentation,
 *       not a datapack file, and shipping a 200 KB PNG inside the mod jar to look at once
 *       would be silly.</li>
 *   <li>It ignores {@link CachedOutput}, so it is not hash-cached. Its real input is the
 *       Terra map PNGs and the warp constants in the Java, neither of which the cache
 *       tracks; being told the preview is "up to date" when it is not would be worse than
 *       re-rendering a small image every run.</li>
 * </ul>
 *
 * <p>It runs here rather than as a standalone tool because touching {@code TerraRegion}
 * initialises {@code Registries}, which throws unless the game has bootstrapped, and
 * Forge cannot bootstrap outside its own launcher. Datagen is already inside one.
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
                // A broken preview must not fail the build — the datapack it sits
                // alongside is still perfectly valid without it.
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
