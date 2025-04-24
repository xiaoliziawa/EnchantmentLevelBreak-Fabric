package net.prizowo.enchantmentlevelbreak.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.EnchantCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(EnchantCommand.class)
public class CommandMixin {
    @Shadow @Final private static DynamicCommandExceptionType FAILED_INCOMPATIBLE_EXCEPTION;
    @Shadow @Final private static DynamicCommandExceptionType FAILED_ITEMLESS_EXCEPTION;
    @Shadow @Final private static DynamicCommandExceptionType FAILED_ENTITY_EXCEPTION;
    @Shadow @Final private static SimpleCommandExceptionType FAILED_EXCEPTION;

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, order = -999)
    private static void execute(ServerCommandSource source, Collection<? extends Entity> targets,
                                Enchantment enchantment, int level,
                                CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        int successCount = enchantTargets(targets, enchantment, level);

        if (successCount == 0) {
            throw FAILED_EXCEPTION.create();
        }

        sendFeedback(source, enchantment, level, targets, successCount);
        cir.setReturnValue(successCount);
        cir.cancel();
    }

    @Unique
    private static int enchantTargets(Collection<? extends Entity> targets,
                                      Enchantment enchantment,
                                      int level) throws CommandSyntaxException {
        int successCount = 0;

        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                if (targets.size() == 1) {
                    throw FAILED_ENTITY_EXCEPTION.create(entity.getName());
                }
                continue;
            }

            ItemStack itemStack = livingEntity.getMainHandStack();
            if (itemStack.isEmpty()) {
                if (targets.size() == 1) {
                    throw FAILED_ITEMLESS_EXCEPTION.create(livingEntity.getName());
                }
                continue;
            }

            if (!enchantment.isAcceptableItem(itemStack)) {
                if (targets.size() == 1) {
                    throw FAILED_INCOMPATIBLE_EXCEPTION.create(itemStack.getItem().getName());
                }
                continue;
            }

            applyEnchantment(itemStack, enchantment, level);
            successCount++;
        }

        return successCount;
    }

    @Unique
    private static void applyEnchantment(ItemStack itemStack, Enchantment enchantment, int level) {
        NbtCompound nbt = itemStack.getOrCreateNbt();
        NbtList enchantments = nbt.getList("Enchantments", 10);
        if (enchantments == null) {
            enchantments = new NbtList();
        }

        NbtCompound enchantmentNbt = new NbtCompound();
        String enchantmentId = Registry.ENCHANTMENT.getId(enchantment).toString();
        enchantmentNbt.putString("id", enchantmentId);
        enchantmentNbt.putInt("lvl", level);

        enchantments.removeIf(element ->
                ((NbtCompound) element).getString("id").equals(enchantmentId)
        );

        enchantments.add(enchantmentNbt);
        nbt.put("Enchantments", enchantments);
    }

    @Unique
    private static void sendFeedback(ServerCommandSource source,
                                     Enchantment enchantment,
                                     int level,
                                     Collection<? extends Entity> targets,
                                     int successCount) {
        if (targets.size() == 1) {
            source.sendFeedback(
                    new TranslatableText("commands.enchant.success.single",
                            enchantment.getName(level),
                            targets.iterator().next().getDisplayName()
                    ),
                    true
            );
        } else {
            source.sendFeedback(
                    new TranslatableText("commands.enchant.success.multiple",
                            enchantment.getName(level),
                            successCount
                    ),
                    true
            );
        }
    }
}
