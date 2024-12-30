package net.prizowo.enchantmentlevelbreak.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.*;
import net.prizowo.enchantmentlevelbreak.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilMenuMixin extends ForgingScreenHandler {
    @Shadow private int repairItemUsage;
    @Shadow private final Property levelCost = Property.create();

    protected AnvilMenuMixin(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    @Unique
    private boolean canCombineEnchantments(Enchantment newEnchant, Map<Enchantment, Integer> existingEnchants) {
        if (ModConfig.getInstance().allowAllEnchantmentsCombine) {
            return true;
        }
        return existingEnchants.keySet().stream()
                .allMatch(existing -> newEnchant == existing || newEnchant.canCombine(existing));
    }

    @Unique
    private boolean canAcceptEnchantment(ItemStack stack, Enchantment enchantment) {
        return ModConfig.getInstance().allowEnchantAllItems || enchantment.isAcceptableItem(stack);
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void onUpdateResult(CallbackInfo ci) {
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);

        if (left.isEmpty() || right.isEmpty() ||
                (left.isOf(Items.ENCHANTED_BOOK) && !right.isOf(Items.ENCHANTED_BOOK))) {
            return;
        }

        Map<Enchantment, Integer> leftEnchants = EnchantmentHelper.get(left);
        Map<Enchantment, Integer> rightEnchants = EnchantmentHelper.get(right);

        if (!rightEnchants.isEmpty() || right.isOf(Items.ENCHANTED_BOOK)) {
            ItemStack result = left.copy();
            Map<Enchantment, Integer> resultEnchants = EnchantmentHelper.get(result);

            boolean canApplyAll = rightEnchants.entrySet().stream().allMatch(entry ->
                    canAcceptEnchantment(result, entry.getKey()) &&
                            canCombineEnchantments(entry.getKey(), leftEnchants)
            );

            if (!canApplyAll) {
                return;
            }

            rightEnchants.forEach((enchantment, rightLevel) -> {
                int leftLevel = leftEnchants.getOrDefault(enchantment, 0);
                resultEnchants.put(enchantment, leftLevel > 0 ? leftLevel + rightLevel : rightLevel);
            });

            if (!resultEnchants.isEmpty()) {
                EnchantmentHelper.set(resultEnchants, result);
                this.output.setStack(0, result);

                int totalCost = rightEnchants.values().stream().mapToInt(Integer::intValue).sum();
                this.levelCost.set(Math.min(totalCost, 50));
                this.repairItemUsage = 1;

                ci.cancel();
            }
        }
    }
}