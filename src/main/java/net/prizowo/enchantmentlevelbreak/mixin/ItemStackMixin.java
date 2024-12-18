package net.prizowo.enchantmentlevelbreak.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "addEnchantment", at = @At("HEAD"), cancellable = true, order = -999)
    private void onAddEnchantment(Enchantment enchantment, int level, CallbackInfo ci) {
        ItemStack stack = (ItemStack)(Object)this;
        NbtList enchantments = stack.getEnchantments();
        enchantments.add(EnchantmentHelper.createNbt(
            EnchantmentHelper.getEnchantmentId(enchantment), 
            level
        ));
        stack.getOrCreateNbt().put("Enchantments", enchantments);
        ci.cancel();
    }
}

