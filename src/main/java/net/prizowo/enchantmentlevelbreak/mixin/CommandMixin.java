package net.prizowo.enchantmentlevelbreak.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.EnchantCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
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

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void execute(ServerCommandSource source, Collection<? extends Entity> targets,
                              RegistryEntry<Enchantment> enchantment, int level,
                              CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        int i = 0;
        Enchantment enchantment2 = enchantment.value();

        for(Entity entity : targets) {
            if (entity instanceof LivingEntity livingEntity) {
                ItemStack itemStack = livingEntity.getMainHandStack();
                if (!itemStack.isEmpty()) {
                    if (enchantment2.isAcceptableItem(itemStack)) {
                        // 直接通过 NBT 添加附魔
                        NbtCompound nbt = itemStack.getOrCreateNbt();
                        NbtList enchantments = nbt.getList("Enchantments", 10);
                        if (enchantments == null) {
                            enchantments = new NbtList();
                        }

                        // 创建新的附魔 NBT
                        NbtCompound enchantmentNbt = new NbtCompound();
                        enchantmentNbt.putString("id", Registries.ENCHANTMENT.getId(enchantment2).toString());
                        enchantmentNbt.putInt("lvl", level);

                        // 移除已存在的相同附魔
                        enchantments.removeIf(nbtElement -> {
                            NbtCompound compound = (NbtCompound) nbtElement;
                            return compound.getString("id").equals(Registries.ENCHANTMENT.getId(enchantment2).toString());
                        });

                        enchantments.add(enchantmentNbt);
                        nbt.put("Enchantments", enchantments);
                        ++i;
                    } else if (targets.size() == 1) {
                        throw FAILED_INCOMPATIBLE_EXCEPTION.create(itemStack.getItem().getName().getString());
                    }
                } else if (targets.size() == 1) {
                    throw FAILED_ITEMLESS_EXCEPTION.create(livingEntity.getName().getString());
                }
            } else if (targets.size() == 1) {
                throw FAILED_ENTITY_EXCEPTION.create(entity.getName().getString());
            }
        }

        if (i == 0) {
            throw FAILED_EXCEPTION.create();
        }

        if (targets.size() == 1) {
            source.sendFeedback(() -> Text.translatable("commands.enchant.success.single",
                    enchantment2.getName(level),
                    targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendFeedback(() -> Text.translatable("commands.enchant.success.multiple",
                    enchantment2.getName(level),
                    targets.size()), true);
        }

        cir.setReturnValue(i);
        cir.cancel();
    }
}
