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
        super(type, syncId, playerInventory, context);
    }

    @Override
    public ForgingSlotsManager getForgingSlotsManager() {
        return ForgingSlotsManager.create()
                .input(0, 27, 47, stack -> true)
                .input(1, 76, 47, stack -> true)
                .output(2, 134, 47)
                .build();
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
        boolean sameItem = left.isOf(right.getItem());
        boolean rightIsBook = right.isOf(Items.ENCHANTED_BOOK);

        ItemEnchantmentsComponent leftEnchants = getEnchantments(left);
        ItemEnchantmentsComponent rightEnchants = getEnchantments(right);

        if (sameItem) {
            if (!leftEnchants.isEmpty() || !rightEnchants.isEmpty()) {
                handleEnchantmentMerge(left, leftEnchants, rightEnchants, true, ci);
            }
            return;
        }
        if (!rightEnchants.isEmpty() && (rightIsBook || !getEnchantments(right).isEmpty())) {
            handleEnchantmentMerge(left, leftEnchants, rightEnchants, false, ci);
        }
    }

    @Unique
    private void handleEnchantmentMerge(ItemStack target, ItemEnchantmentsComponent leftEnchants, ItemEnchantmentsComponent rightEnchants, boolean isSameItemMerge, CallbackInfo ci) {
        ItemStack result = target.copy();
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(leftEnchants);
        boolean anyApplied = false;
        int totalCost = 0;

        for (var entry : rightEnchants.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> enchantment = entry.getKey();
            int rightLevel = entry.getIntValue();
            boolean canApply = isSameItemMerge || ModConfig.getInstance().isAllowAnyEnchantment() || enchantment.value().isAcceptableItem(target);

            if (canApply) {
                int leftLevel = leftEnchants.getLevel(enchantment);
                int newLevel = calculateNewLevel(leftLevel, rightLevel);
                newLevel = Math.min(newLevel, ModConfig.getInstance().getMaxEnchantmentLevel());
                builder.set(enchantment, newLevel);
                totalCost += newLevel;
                anyApplied = true;
            }
        }

        if (anyApplied) {
            ItemEnchantmentsComponent newEnchants = builder.build();
            setEnchantments(result, newEnchants);
            this.output.setStack(0, result);
            this.repairItemUsage = Math.min(totalCost, 50);
            this.levelCost.set(this.repairItemUsage);
            ci.cancel();
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
            if (stack.contains(DataComponentTypes.ENCHANTMENTS)) {
                stack.remove(DataComponentTypes.ENCHANTMENTS);
            }
        } else {
            stack.set(DataComponentTypes.ENCHANTMENTS, enchantments);
        }
    }

    @Unique
    private int calculateNewLevel(int leftLevel, int rightLevel) {
        // 优先级: allowLevelStacking > allowVanillaLevelStacking
        if (ModConfig.getInstance().isAllowLevelStacking()) {
            // allowLevelStacking为true时，直接相加 (5+5=10)
            return leftLevel + rightLevel;
        } else if (ModConfig.getInstance().isAllowVanillaLevelStacking() && leftLevel == rightLevel) {
            // allowVanillaLevelStacking为true且相同等级时，+1 (5+5=6)
            return leftLevel + 1;
        } else {
            // 两个都为false时，使用原版机制，取最大值 (5+5=5)
            return Math.max(leftLevel, rightLevel);
        }
    }
}