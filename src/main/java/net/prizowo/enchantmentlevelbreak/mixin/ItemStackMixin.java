package net.prizowo.enchantmentlevelbreak.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Unique
    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);

    @Inject(method = "addEnchantment", at = @At("HEAD"), cancellable = true)
    private void onAddEnchantment(RegistryEntry<Enchantment> enchantment, int level, CallbackInfo ci) {
        if (IS_PROCESSING.get()) {
            return;
        }

        try {
            IS_PROCESSING.set(true);
            ItemStack stack = (ItemStack)(Object)this;
            
            if (!stack.isEmpty() && level > 0) {
                stack.addEnchantment(enchantment, level);
                ci.cancel();
            }
        } finally {
            IS_PROCESSING.set(false);
        }
    }
}
