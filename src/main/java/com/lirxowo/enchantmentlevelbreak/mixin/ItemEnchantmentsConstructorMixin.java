package com.lirxowo.enchantmentlevelbreak.mixin;

import com.lirxowo.enchantmentlevelbreak.config.ModConfig;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ItemEnchantmentsComponent.Builder.class)
public class ItemEnchantmentsConstructorMixin {
    @ModifyConstant(method = "set", constant = @Constant(intValue = 255))
    private int modifySetMaxLevel(int value) {
        return ModConfig.getInstance().getMaxEnchantmentLevel();
    }

    @ModifyConstant(method = "add", constant = @Constant(intValue = 255))
    private int modifyAddMaxLevel(int value) {
        return ModConfig.getInstance().getMaxEnchantmentLevel();
    }
}