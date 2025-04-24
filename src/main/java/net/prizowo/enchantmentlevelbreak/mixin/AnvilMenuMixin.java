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
    private boolean canEnchant(ItemStack item, Enchantment enchantment) {
        return item.isOf(Items.ENCHANTED_BOOK) ||
                ModConfig.getInstance().isAllowAnyEnchantment() ||
                enchantment.isAcceptableItem(item);
    }

    @Unique
    private boolean isEnchantmentCompatible(Enchantment newEnchant, Map<Enchantment, Integer> existingEnchants) {
        if (ModConfig.getInstance().isAllowAnyEnchantment()) {
            return true;
        }
        return existingEnchants.keySet().stream()
                .allMatch(existing -> newEnchant == existing || newEnchant.canCombine(existing));
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void onUpdateResult(CallbackInfo ci) {
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);

        if (!isValidAnvilOperation(left, right)) {
            return;
        }

        Map<Enchantment, Integer> leftEnchants = EnchantmentHelper.get(left);
        Map<Enchantment, Integer> rightEnchants = EnchantmentHelper.get(right);

        if (!rightEnchants.isEmpty() || right.isOf(Items.ENCHANTED_BOOK)) {
            ItemStack result = left.copy();

            boolean anyEnchantmentApplied = false;
            for (Map.Entry<Enchantment, Integer> entry : rightEnchants.entrySet()) {
                Enchantment enchantment = entry.getKey();
                int rightLevel = entry.getValue();

                if (!canEnchant(result, enchantment) || !isEnchantmentCompatible(enchantment, leftEnchants)) {
                    continue;
                }

                int leftLevel = leftEnchants.getOrDefault(enchantment, 0);
                int newLevel = calculateNewLevel(leftLevel, rightLevel);
                
                leftEnchants.put(enchantment, newLevel);
                anyEnchantmentApplied = true;
            }

            if (anyEnchantmentApplied) {
                EnchantmentHelper.set(leftEnchants, result);
                this.output.setStack(0, result);

                // Calculate experience cost
                int totalCost = leftEnchants.values().stream().mapToInt(Integer::intValue).sum();
                this.levelCost.set(Math.min(totalCost, 50));
                this.repairItemUsage = 1;
                ci.cancel();
            }
        }
    }

    @Unique
    private boolean isValidAnvilOperation(ItemStack left, ItemStack right) {
        return !left.isEmpty() && !right.isEmpty() &&
                !(left.isOf(Items.ENCHANTED_BOOK) && !right.isOf(Items.ENCHANTED_BOOK));
    }

    @Unique
    private int calculateNewLevel(int leftLevel, int rightLevel) {
        if (leftLevel <= 0) {
            return rightLevel;
        }

        if (ModConfig.getInstance().isAllowLevelStacking()) {
            return leftLevel + rightLevel;
        }

        return leftLevel == rightLevel ? leftLevel + 1 : Math.max(leftLevel, rightLevel);
    }
}