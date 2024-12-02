package net.prizowo.enchantmentlevelbreak.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public class HelperMixin {
    @Inject(method = "getLevelFromNbt", at = @At("HEAD"), cancellable = true)
    private static void onGetLevelFromNbt(NbtCompound nbt, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(nbt.getInt("lvl"));
    }

    @Inject(method = "createNbt", at = @At("HEAD"), cancellable = true)
    private static void onCreateNbt(Identifier id, int level, CallbackInfoReturnable<NbtCompound> cir) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("id", id.toString());
        nbt.putInt("lvl", level);
        cir.setReturnValue(nbt);
        cir.cancel();
    }
}
