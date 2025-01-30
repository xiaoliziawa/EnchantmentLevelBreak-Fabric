package net.prizowo.enchantmentlevelbreak.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.ForgingSlotsManager;
import net.prizowo.enchantmentlevelbreak.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilMenuMixin extends ForgingScreenHandler {
    @Shadow private int repairItemUsage;
    @Shadow private final Property levelCost = Property.create();

    protected AnvilMenuMixin(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context, getForgingSlotsManager());
    }

    @Unique
    private static ForgingSlotsManager getForgingSlotsManager() {
        return ForgingSlotsManager.builder()
                .input(0, 27, 47, stack -> true)
                .input(1, 76, 47, stack -> true)
                .output(2, 134, 47)
                .build();
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void onUpdateResult(CallbackInfo ci) {
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);

        if (!isValidAnvilOperation(left, right)) {
            return;
        }

        if (!getEnchantments(right).isEmpty() || right.isOf(Items.ENCHANTED_BOOK)) {
            if (!canApplyAnyEnchantment(left, right)) {
                return;
            }

            ItemStack result = left.copy();
            ItemEnchantmentsComponent leftEnchants = getEnchantments(left);
            ItemEnchantmentsComponent rightEnchants = getEnchantments(right);
            ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(leftEnchants);

            // 保留左边物品的所有附魔
            for (var entry : leftEnchants.getEnchantmentEntries()) {
                builder.set(entry.getKey(), entry.getIntValue());
            }

            // 合并右边物品的附魔
            boolean anyEnchantmentApplied = false;
            for (var entry : rightEnchants.getEnchantmentEntries()) {
                RegistryEntry<Enchantment> enchantment = entry.getKey();
                if (!canEnchant(left, enchantment) || !isEnchantmentCompatible(enchantment, leftEnchants)) {
                    continue;
                }

                int newLevel = calculateNewLevel(leftEnchants.getLevel(enchantment), entry.getIntValue());
                builder.set(enchantment, newLevel);
                anyEnchantmentApplied = true;
            }

            if (anyEnchantmentApplied) {
                ItemEnchantmentsComponent newEnchants = builder.build();
                setEnchantments(result, newEnchants);
                this.output.setStack(0, result);

                // 计算经验消耗
                int totalCost = 0;
                for (var entry : newEnchants.getEnchantmentEntries()) {
                    totalCost += entry.getIntValue();
                }

                this.levelCost.set(Math.min(totalCost, 50));
                this.repairItemUsage = 1;
                ci.cancel();
            }
        }
    }

    @Unique
    private ItemEnchantmentsComponent getEnchantments(ItemStack stack) {
        if (stack.isOf(Items.ENCHANTED_BOOK)) {
            return stack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        }
        return stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
    }

    @Unique
    private void setEnchantments(ItemStack stack, ItemEnchantmentsComponent enchantments) {
        if (stack.isOf(Items.ENCHANTED_BOOK)) {
            stack.set(DataComponentTypes.STORED_ENCHANTMENTS, enchantments);
        } else {
            stack.set(DataComponentTypes.ENCHANTMENTS, enchantments);
        }
    }

    @Unique
    private boolean canEnchant(ItemStack item, RegistryEntry<Enchantment> enchantment) {
        return item.isOf(Items.ENCHANTED_BOOK) ||
                ModConfig.getInstance().isAllowAnyEnchantment() ||
                enchantment.value().isAcceptableItem(item);
    }

    @Unique
    private boolean isValidAnvilOperation(ItemStack left, ItemStack right) {
        return !left.isEmpty() && !right.isEmpty() &&
                !(left.isOf(Items.ENCHANTED_BOOK) && !right.isOf(Items.ENCHANTED_BOOK));
    }

    @Unique
    private boolean canApplyAnyEnchantment(ItemStack target, ItemStack source) {
        if (source.isOf(Items.ENCHANTED_BOOK)) {
            return true;
        }

        ItemEnchantmentsComponent sourceEnchants = getEnchantments(source);
        for (var entry : sourceEnchants.getEnchantmentEntries()) {
            if (canEnchant(target, entry.getKey())) {
                return true;
            }
        }
        return false;
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

    @Unique
    private boolean isEnchantmentCompatible(RegistryEntry<Enchantment> newEnchant, ItemEnchantmentsComponent existingEnchants) {
        for (var existingEntry : existingEnchants.getEnchantmentEntries()) {
            if (!newEnchant.equals(existingEntry.getKey()) &&
                    !Enchantment.canBeCombined(newEnchant, existingEntry.getKey())) {
                return false;
            }
        }
        return true;
    }
}

