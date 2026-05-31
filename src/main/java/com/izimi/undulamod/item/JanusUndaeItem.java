package com.izimi.undulamod.item;

import com.izimi.undulamod.config.UndulaConfig;
import com.izimi.undulamod.enchantment.ModEnchantments;
import com.izimi.undulamod.mechanic.UndulaHandler;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class JanusUndaeItem extends SwordItem {

    private static final float BASE_CRIT_RATE = 0.10f;

    public JanusUndaeItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.undulamod.janus_undae.lore1").formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("item.undulamod.janus_undae.lore2").formatted(Formatting.DARK_AQUA));

        // 附魔效果说明
        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("item.undulamod.janus_undae.enchant.lethality").formatted(Formatting.RED));
        tooltip.add(Text.translatable("item.undulamod.janus_undae.enchant.shatter").formatted(Formatting.BLUE));
        tooltip.add(Text.translatable("item.undulamod.janus_undae.enchant.penetrate").formatted(Formatting.DARK_PURPLE));
    }
    
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld().isClient()) return true;
        if (!(attacker instanceof PlayerEntity player)) return true;

        ServerWorld world = (ServerWorld) attacker.getWorld();
        var enchantmentRegistry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);

        RegistryEntry<Enchantment> lethality = enchantmentRegistry.getEntry(ModEnchantments.LETHALITY).orElseThrow();
        RegistryEntry<Enchantment> shatterWhirl = enchantmentRegistry.getEntry(ModEnchantments.SHATTER_WHIRL).orElseThrow();
        RegistryEntry<Enchantment> penetrate = enchantmentRegistry.getEntry(ModEnchantments.PENETRATE).orElseThrow();

        int lethalityLevel = EnchantmentHelper.getLevel(lethality, stack);
        int shatterLevel = EnchantmentHelper.getLevel(shatterWhirl, stack);
        int penetrateLevel = EnchantmentHelper.getLevel(penetrate, stack);

        if (shatterLevel > 0 && penetrateLevel > 0) {
            shatterLevel = 0;
        }

        float critRate = BASE_CRIT_RATE + UndulaConfig.LETHALITY_CRIT_RATE[lethalityLevel];

        UndulaHandler.onJanusHit(player, target, stack, critRate,
            shatterLevel, penetrateLevel, world);

        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantability() {
        return 22;
    }
}