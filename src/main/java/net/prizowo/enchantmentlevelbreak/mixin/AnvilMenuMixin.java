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
import java.util.HashMap;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilMenuMixin extends ForgingScreenHandler {
    @Shadow private int repairItemUsage;
    @Shadow private final Property levelCost = Property.create();

    protected AnvilMenuMixin(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void onUpdateResult(CallbackInfo ci) {
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);

        if (!left.isEmpty() && !right.isEmpty()) {
            handleAnvilOperation(left, right, ci);
        }
    }

    @Unique
    private void handleAnvilOperation(ItemStack left, ItemStack right, CallbackInfo ci) {
        Map<Enchantment, Integer> leftEnchants = EnchantmentHelper.get(left);
        Map<Enchantment, Integer> rightEnchants = EnchantmentHelper.get(right);

        if (left.getItem() == right.getItem()) {
            if (!leftEnchants.isEmpty() || !rightEnchants.isEmpty()) {
                handleEnchantmentMerge(left, leftEnchants, rightEnchants, true, ci);
            }
            return;
        }

        if (!rightEnchants.isEmpty() && isEnchantedBook(right)) {
            handleEnchantmentMerge(left, leftEnchants, rightEnchants, false, ci);
        }
    }

    @Unique
    private boolean isEnchantedBook(ItemStack stack) {
        return stack.isOf(Items.ENCHANTED_BOOK);
    }

    @Unique
    private void handleEnchantmentMerge(ItemStack target, Map<Enchantment, Integer> leftEnchants, Map<Enchantment, Integer> rightEnchants, boolean isSameItemMerge, CallbackInfo ci) {
        if (!ModConfig.getInstance().isAllowLevelStacking() && !ModConfig.getInstance().isAllowVanillaLevelStacking() && !ModConfig.getInstance().isAllowAnyEnchantment()) {
            return;
        }

        Map<Enchantment, Integer> resultEnchants = new HashMap<>(leftEnchants);
        int totalCost = 0;
        boolean anyEnchantmentApplied = false;

        for (Map.Entry<Enchantment, Integer> entry : rightEnchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int rightLevel = entry.getValue();
            boolean canApply = isSameItemMerge || ModConfig.getInstance().isAllowAnyEnchantment() || enchantment.isAcceptableItem(target);
            if (canApply) {
                int leftLevel = resultEnchants.getOrDefault(enchantment, 0);
                int newLevel = calculateNewLevel(leftLevel, rightLevel);
                newLevel = Math.min(newLevel, ModConfig.getInstance().getMaxEnchantmentLevel());

                resultEnchants.put(enchantment, newLevel);
                totalCost += newLevel;
                anyEnchantmentApplied = true;
            }
        }

        if (anyEnchantmentApplied) {
            applyResult(target, resultEnchants, totalCost);
            ci.cancel();
        }
    }

    @Unique
    private int calculateNewLevel(int leftLevel, int rightLevel) {
        if (ModConfig.getInstance().isAllowLevelStacking()) {
            return leftLevel + rightLevel;
        } else if (ModConfig.getInstance().isAllowVanillaLevelStacking() && leftLevel == rightLevel) {
            return leftLevel + 1;
        } else {
            return Math.max(leftLevel, rightLevel);
        }
    }

    @Unique
    private void applyResult(ItemStack target, Map<Enchantment, Integer> enchantments, int totalCost) {
        ItemStack result = target.copy();
        EnchantmentHelper.set(enchantments, result);
        this.output.setStack(0, result);

        this.repairItemUsage = Math.min(totalCost, 50);
        this.levelCost.set(this.repairItemUsage);
    }
}