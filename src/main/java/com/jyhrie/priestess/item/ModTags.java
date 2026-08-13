package com.jyhrie.priestess.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Item tag keys this mod writes into. The key is only the tag's <em>name</em>; what is in it
 * comes from {@link com.jyhrie.priestess.datagen.ModItemTagsProvider}.
 *
 * <p>Block tags are not here — those hang off the thing they describe, on
 * {@code DecorativePipeBlock} and {@code Dungeon}, because in both cases the tag and the
 * behaviour reading it are the same idea. A Curios slot tag has no such home: the behaviour
 * reading it belongs to another mod entirely.
 */
public final class ModTags {

    public static final class Items {

        /**
         * The Module slot's contents.
         *
         * <p><b>The namespace is {@code curios}, not {@code priestess}</b> — this is a tag
         * another mod owns and we are adding entries to it. That holds even though we define
         * the Module slot ourselves: the slot definition lives in our datapack, but the tag
         * its validator reads is always {@code curios:<slot id>}. Getting this wrong produces
         * an item that is simply not equippable, with no error anywhere.
         */
        public static final TagKey<Item> CURIOS_MODULE = curios("module");

        private static TagKey<Item> curios(String slot) {
            return TagKey.create(Registries.ITEM, new ResourceLocation("curios", slot));
        }

        private Items() {}
    }

    private ModTags() {}
}
