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

    @Unique
    private ItemEnchantmentsComponent getEnchantments(ItemStack stack) {
        if (stack.isOf(Items.ENCHANTED_BOOK)) {
            // 对于附魔书，使用STORED_ENCHANTMENTS组件
            return stack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        } else {
            // 对于其他物品，使用ENCHANTMENTS组件
            return stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        }
    }

    @Unique
    private void setEnchantments(ItemStack stack, ItemEnchantmentsComponent enchantments) {
        if (stack.isOf(Items.ENCHANTED_BOOK)) {
            // 对于附魔书，总是使用STORED_ENCHANTMENTS
            stack.set(DataComponentTypes.STORED_ENCHANTMENTS, enchantments);
        } else {
            // 对于其他物品，使用ENCHANTMENTS
            stack.set(DataComponentTypes.ENCHANTMENTS, enchantments);
        }
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void onUpdateResult(CallbackInfo ci) {
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);

        if (!left.isEmpty() && !right.isEmpty()) {
            // 如果左边是附魔书，右边不是附魔书，不合并
            if (left.isOf(Items.ENCHANTED_BOOK) && !right.isOf(Items.ENCHANTED_BOOK)) {
                return;
            }

            // 检查是否含有附魔
            boolean hasLeftEnchants = !getEnchantments(left).isEmpty();
            boolean hasRightEnchants = !getEnchantments(right).isEmpty();
            
            if (hasRightEnchants || right.isOf(Items.ENCHANTED_BOOK)) {
                ItemStack result = left.copy();
                
                // 获取铁砧左右两个物品的附魔组件
                ItemEnchantmentsComponent leftEnchants = getEnchantments(left);
                ItemEnchantmentsComponent rightEnchants = getEnchantments(right);
                
                // 创建新附魔组件构造器
                ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(leftEnchants);
                
                // 保留左边物品的所有附魔
                for (var entry : leftEnchants.getEnchantmentEntries()) {
                    builder.set(entry.getKey(), entry.getIntValue());
                }
                
                // 合并右边物品的附魔
                for (var entry : rightEnchants.getEnchantmentEntries()) {
                    RegistryEntry<Enchantment> enchantment = entry.getKey();
                    int rightLevel = entry.getIntValue();
                    int leftLevel = leftEnchants.getLevel(enchantment);
                    
                    // 检查附魔兼容性
                    boolean canApply = true;
                    for (var existingEntry : leftEnchants.getEnchantmentEntries()) {
                        if (!enchantment.equals(existingEntry.getKey()) && // 如果不是同一个附魔
                            !Enchantment.canBeCombined(enchantment, existingEntry.getKey())) { // 且不兼容
                            canApply = false;
                            break;
                        }
                    }
                    
                    if (canApply) {
                        // 相同附魔直接相加
                        if (leftLevel > 0) {
                            builder.set(enchantment, leftLevel + rightLevel);
                        } else {
                            // 新附魔设置等级和属性到新的附魔书上面
                            builder.set(enchantment, rightLevel);
                        }
                    }
                }
                
                // 应用合并附魔
                ItemEnchantmentsComponent newEnchants = builder.build();
                if (!newEnchants.isEmpty()) {
                    setEnchantments(result, newEnchants);
                    this.output.setStack(0, result);
                    
                    // 经验消耗
                    int totalCost = 0;
                    for (var entry : rightEnchants.getEnchantmentEntries()) {
                        totalCost += entry.getIntValue();
                    }
                    
                    this.levelCost.set(Math.min(totalCost, 50));
                    this.repairItemUsage = 1;
                    
                    ci.cancel();
                }
            }
        }
    }
}

