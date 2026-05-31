package com.izimi.undulamod.enchantment;

import com.izimi.undulamod.UndulaMod;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEnchantments {
    
    public static final RegistryKey<Enchantment> SHATTER_WHIRL = 
        RegistryKey.of(RegistryKeys.ENCHANTMENT, 
            Identifier.of(UndulaMod.MOD_ID, "shatter_whirl"));
    
    public static final RegistryKey<Enchantment> PENETRATE = 
        RegistryKey.of(RegistryKeys.ENCHANTMENT, 
            Identifier.of(UndulaMod.MOD_ID, "penetrate"));
    
    public static final RegistryKey<Enchantment> LETHALITY = 
        RegistryKey.of(RegistryKeys.ENCHANTMENT, 
            Identifier.of(UndulaMod.MOD_ID, "lethality"));
    
    public static void initialize() {
        UndulaMod.LOGGER.info("Enchantment keys registered");
    }
}