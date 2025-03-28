package net.prizowo.enchantmentlevelbreak.mixin;

import net.minecraft.component.type.ItemEnchantmentsComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEnchantmentsComponent.class)
public class ItemEnchantmentsMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 255))
    private int modifyConstructorMaxLevel(int value) {
        return Integer.MAX_VALUE;
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 255))
    private static int modifyCodecMaxLevel(int value) {
        return Integer.MAX_VALUE;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
    }
}