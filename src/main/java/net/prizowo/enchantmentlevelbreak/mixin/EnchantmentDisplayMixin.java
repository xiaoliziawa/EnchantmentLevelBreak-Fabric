package net.prizowo.enchantmentlevelbreak.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.registry.tag.EnchantmentTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentDisplayMixin {
    @Inject(method = "getName(Lnet/minecraft/registry/entry/RegistryEntry;I)Lnet/minecraft/text/Text;", at = @At("HEAD"), cancellable = true)
    private static void onGetName(RegistryEntry<Enchantment> enchantment, int level, CallbackInfoReturnable<Text> cir) {
        MutableText name = enchantment.value().description().copy();
        if (enchantment.isIn(EnchantmentTags.CURSE)) {
            Texts.setStyleIfAbsent(name, Style.EMPTY.withColor(Formatting.RED));
        } else {
            Texts.setStyleIfAbsent(name, Style.EMPTY.withColor(Formatting.GRAY));
        }
        
        if (level != 1) {
            name.append(" ").append(String.valueOf(level));
        }
        cir.setReturnValue(name);
    }
} 