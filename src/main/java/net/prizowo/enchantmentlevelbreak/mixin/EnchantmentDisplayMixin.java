package net.prizowo.enchantmentlevelbreak.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.registry.tag.EnchantmentTags;
import net.prizowo.enchantmentlevelbreak.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentDisplayMixin {
    @Unique
    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    @Unique
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    @Unique
    private static String intToRoman(int num) {
        StringBuilder roman = new StringBuilder();
        for (int i = 0; i < VALUES.length; i++) {
            while (num >= VALUES[i]) {
                roman.append(SYMBOLS[i]);
                num -= VALUES[i];
            }
        }
        return roman.toString();
    }

    @Inject(method = "getName(Lnet/minecraft/registry/entry/RegistryEntry;I)Lnet/minecraft/text/Text;", at = @At("HEAD"), cancellable = true)
    private static void onGetName(RegistryEntry<Enchantment> enchantment, int level, CallbackInfoReturnable<Text> cir) {
        MutableText name = enchantment.value().description().copy();
        if (enchantment.isIn(EnchantmentTags.CURSE)) {
            Texts.setStyleIfAbsent(name, Style.EMPTY.withColor(Formatting.RED));
        } else {
            Texts.setStyleIfAbsent(name, Style.EMPTY.withColor(Formatting.GRAY));
        }

        if (level != 1) {
            name.append(" ");
            ModConfig config = ModConfig.getInstance();
            if (config.isUseRomanNumerals() && level <= config.getRomanNumeralsThreshold()) {
                name.append(intToRoman(level));
            } else {
                name.append(String.valueOf(level));
            }
        }
        cir.setReturnValue(name);
    }
} 