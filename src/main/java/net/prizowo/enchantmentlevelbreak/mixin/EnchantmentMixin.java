package net.prizowo.enchantmentlevelbreak.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.prizowo.enchantmentlevelbreak.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentMixin {
    @Unique
    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    @Unique
    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    @Unique
    private static String toRoman(int number) {
        if (number <= 0) return "0";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < ROMAN_VALUES.length && number > 0; i++) {
            while (number >= ROMAN_VALUES[i]) {
                result.append(ROMAN_SYMBOLS[i]);
                number -= ROMAN_VALUES[i];
            }
        }
        return result.toString();
    }

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void onGetName(int level, CallbackInfoReturnable<Text> cir) {
        Enchantment enchantment = (Enchantment) (Object) this;
        ModConfig config = ModConfig.getInstance();

        MutableText name = Text.translatable(enchantment.getTranslationKey());
        
        // Set style based on whether it's a curse
        if (((Enchantment) (Object) this).isCursed()) {
            name.setStyle(Style.EMPTY.withColor(Formatting.RED));
        } else {
            name.setStyle(Style.EMPTY.withColor(Formatting.GRAY));
        }

        if (level != 1) {
            name.append(" ");
            if (config.isUseRomanNumerals() && level <= config.getRomanNumeralsThreshold()) {
                name.append(toRoman(level));
            } else {
                name.append(String.valueOf(level));
            }
        }

        cir.setReturnValue(name);
        cir.cancel();
    }

    @Inject(method = "isAcceptableItem", at = @At("HEAD"), cancellable = true)
    private void onIsAcceptableItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (ModConfig.getInstance().isAllowAnyEnchantment()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canCombine", at = @At("HEAD"), cancellable = true)
    private void onCanCombine(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
        if (ModConfig.getInstance().isAllowAnyEnchantment()) {
            cir.setReturnValue(true);
        }
    }
}