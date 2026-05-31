package com.izimi.undulamod.item;

import com.izimi.undulamod.UndulaMod;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEnchantedBooks {

    public static final Item SHATTER_WHIRL_BOOK = createEnchantedBook(
        "shatter_whirl_book"
    );

    public static final Item PENETRATE_BOOK = createEnchantedBook(
        "penetrate_book"
    );

    public static final Item LETHALITY_BOOK = createEnchantedBook(
        "lethality_book"
    );

    private static Item createEnchantedBook(String id) {
        Item.Settings settings = new Item.Settings()
            .maxCount(1);

        return Registry.register(
            Registries.ITEM,
            Identifier.of(UndulaMod.MOD_ID, id),
            new Item(settings)
        );
    }

    public static void initialize() {
        UndulaMod.LOGGER.info("Registering enchanted books");
    }
}