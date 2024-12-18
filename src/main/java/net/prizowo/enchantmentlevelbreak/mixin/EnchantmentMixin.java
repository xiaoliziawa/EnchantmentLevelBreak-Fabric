package net.prizowo.enchantmentlevelbreak.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.prizowo.enchantmentlevelbreak.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentMixin {
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true, order = -999)
    private void onGetName(int level, CallbackInfoReturnable<Text> cir) {
        Text originalName = cir.getReturnValue();
        String modifiedName = originalName.getString().replaceAll("enchantment\\.level\\.\\d+", String.valueOf(level));
        cir.setReturnValue(Text.literal(modifiedName));
    }

    @Inject(method = "isAcceptableItem", at = @At("HEAD"), cancellable = true, order = -999)
    private void onIsAcceptableItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (ModConfig.getInstance().allowEnchantAllItems) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "canCombine", at = @At("HEAD"), cancellable = true, order = -999)
    private void onCanCombine(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
        if (ModConfig.getInstance().allowAllEnchantmentsCombine) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
