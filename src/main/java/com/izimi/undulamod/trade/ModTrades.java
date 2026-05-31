package com.izimi.undulamod.trade;

import com.izimi.undulamod.enchantment.ModEnchantments;

import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;

public class ModTrades {

    public static void initialize() {
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, 1, factories -> {
            for (int lv = 1; lv <= 5; lv++) {
                final int level = lv;
                factories.add((entity, random) -> {
                    var registry = entity.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
                    RegistryEntry<Enchantment> e = registry.getEntry(ModEnchantments.LETHALITY).orElseThrow();
                    return new TradeOffer(
                        new TradedItem(Items.EMERALD, 10 + level * 5),
                        EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(e, level)),
                        3, 20, 0.1f
                    );
                });
            }

            for (int lv = 1; lv <= 3; lv++) {
                final int level = lv;
                factories.add((entity, random) -> {
                    var registry = entity.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
                    RegistryEntry<Enchantment> e = registry.getEntry(ModEnchantments.SHATTER_WHIRL).orElseThrow();
                    return new TradeOffer(
                        new TradedItem(Items.EMERALD, 15 + level * 5),
                        EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(e, level)),
                        3, 20, 0.1f
                    );
                });
            }

            for (int lv = 1; lv <= 3; lv++) {
                final int level = lv;
                factories.add((entity, random) -> {
                    var registry = entity.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
                    RegistryEntry<Enchantment> e = registry.getEntry(ModEnchantments.PENETRATE).orElseThrow();
                    return new TradeOffer(
                        new TradedItem(Items.EMERALD, 15 + level * 5),
                        EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(e, level)),
                        3, 20, 0.1f
                    );
                });
            }
        });
    }
}