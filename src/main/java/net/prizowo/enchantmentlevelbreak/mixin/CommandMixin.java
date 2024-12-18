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
import net.minecraft.text.Text;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
    private static void execute(ServerCommandSource source, Collection<? extends Entity> targets, Enchantment enchantment, int level, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        int i = 0;

        for(Entity entity : targets) {
            if (entity instanceof LivingEntity livingEntity) {
                ItemStack itemStack = livingEntity.getMainHandStack();
                if (!itemStack.isEmpty()) {
                    if (enchantment.isAcceptableItem(itemStack)) {
                        NbtCompound nbt = itemStack.getOrCreateNbt();
                        NbtList enchantments = nbt.getList("Enchantments", 10);
                        if (enchantments == null) {
                            enchantments = new NbtList();
                        }

                        NbtCompound enchantmentNbt = new NbtCompound();
                        enchantmentNbt.putString("id", Registry.ENCHANTMENT.getId(enchantment).toString());
                        enchantmentNbt.putInt("lvl", level);

                        String enchantmentId = Registry.ENCHANTMENT.getId(enchantment).toString();
                        for (int j = 0; j < enchantments.size(); j++) {
                            NbtCompound existingEnchant = enchantments.getCompound(j);
                            if (existingEnchant.getString("id").equals(enchantmentId)) {
                                enchantments.remove(j);
                                break;
                            }
                        }

                        enchantments.add(enchantmentNbt);
                        nbt.put("Enchantments", enchantments);
                        ++i;
                    } else if (targets.size() == 1) {
                        throw FAILED_INCOMPATIBLE_EXCEPTION.create(itemStack.getItem().getName());
                    }
                } else if (targets.size() == 1) {
                    throw FAILED_ITEMLESS_EXCEPTION.create(livingEntity.getName());
                }
            } else if (targets.size() == 1) {
                throw FAILED_ENTITY_EXCEPTION.create(entity.getName());
            }
        }

        if (i == 0) {
            throw FAILED_EXCEPTION.create();
        }

        if (targets.size() == 1) {
            source.sendFeedback(Text.translatable("commands.enchant.success.single",
                    enchantment.getName(level),
                    targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendFeedback(Text.translatable("commands.enchant.success.multiple",
                    enchantment.getName(level),
                    targets.size()), true);
        }

        cir.setReturnValue(i);
        cir.cancel();
    }
}
