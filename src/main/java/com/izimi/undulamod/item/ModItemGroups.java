package com.izimi.undulamod.item;

import com.izimi.undulamod.UndulaMod;
import com.izimi.undulamod.enchantment.ModEnchantments;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {

    public static final ItemGroup UNDULA_GROUP = Registry.register(
        Registries.ITEM_GROUP,
        Identifier.of(UndulaMod.MOD_ID, "undula_group"),
        ItemGroup.create(ItemGroup.Row.TOP, 0)
            .displayName(Text.translatable("itemGroup.undulamod.undula_group"))
            .icon(() -> new ItemStack(ModItems.JANUS_UNDAE))
            .entries((displayContext, entries) -> {
                entries.add(ModItems.JANUS_UNDAE);

                var enchantmentRegistry = displayContext.lookup()
                    .getWrapperOrThrow(RegistryKeys.ENCHANTMENT);

                for (int level = 1; level <= 3; level++) {
                    entries.add(createEnchantedBookStack(
                        enchantmentRegistry, ModEnchantments.SHATTER_WHIRL, level));
                }

                for (int level = 1; level <= 3; level++) {
                    entries.add(createEnchantedBookStack(
                        enchantmentRegistry, ModEnchantments.PENETRATE, level));
                }

                for (int level = 1; level <= 5; level++) {
                    entries.add(createEnchantedBookStack(
                        enchantmentRegistry, ModEnchantments.LETHALITY, level));
                }
            })
            .build()
    );

    private static ItemStack createEnchantedBookStack(
            net.minecraft.registry.RegistryWrapper<Enchantment> registry,
            RegistryKey<Enchantment> enchantmentKey,
            int level) {

        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        RegistryEntry<Enchantment> entry = registry.getOrThrow(enchantmentKey);

        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(
            ItemEnchantmentsComponent.DEFAULT
        );
        builder.add(entry, level);
        ItemEnchantmentsComponent component = builder.build();

        stack.set(DataComponentTypes.STORED_ENCHANTMENTS, component);

        return stack;
    }

    public static void initialize() {
        UndulaMod.LOGGER.info("Registering item groups");
    }
}