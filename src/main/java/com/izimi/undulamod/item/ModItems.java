package com.izimi.undulamod.item;

import com.izimi.undulamod.UndulaMod;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.item.ToolMaterials;

public class ModItems {

    public static final Item JANUS_UNDAE = register(
        "janus_undae",
        new JanusUndaeItem(
            ToolMaterials.IRON,
            new Item.Settings()
                .maxCount(1)
                .rarity(Rarity.RARE)
        )
    );

    private static Item register(String id, Item item) {
        return Registry.register(
            Registries.ITEM,
            Identifier.of(UndulaMod.MOD_ID, id),
            item
        );
    }

    public static void initialize() {
        UndulaMod.LOGGER.info("Registering Undula items");
        ModEnchantedBooks.initialize();
        ModItemGroups.initialize();
    }
}