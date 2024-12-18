package net.prizowo.enchantmentlevelbreak.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class CEnchantCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("cenchant")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("enchantment", StringArgumentType.greedyString())
                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                Registry.ENCHANTMENT.getIds().stream()
                                        .map(Identifier::toString),
                                builder))
                        .executes(context -> enchantItem(context, 1))
                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1))
                                .executes(context -> enchantItem(context,
                                        IntegerArgumentType.getInteger(context, "level"))))));
    }

    private static int enchantItem(CommandContext<ServerCommandSource> context, int level) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayer();
            ItemStack itemStack = player.getMainHandStack();
            String enchantmentInput = StringArgumentType.getString(context, "enchantment");

            if (itemStack.isEmpty()) {
                context.getSource().sendError(Text.of("You must hold an item to enchant"));
                return 0;
            }

            String[] parts = enchantmentInput.split("\\s+", 2);
            String enchantmentName = parts[0];
            if (parts.length > 1) {
                try {
                    level = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    // 忽略无效的数字
                }
            }

            Identifier enchantmentId;
            if (!enchantmentName.contains(":")) {
                enchantmentId = new Identifier("minecraft", enchantmentName);
            } else {
                enchantmentId = new Identifier(enchantmentName);
            }

            Enchantment enchantment = Registry.ENCHANTMENT.get(enchantmentId);

            if (enchantment == null) {
                context.getSource().sendError(Text.of("Invalid enchantment: " + enchantmentName));
                return 0;
            }

            // 使用NBT方式添加附魔
            NbtCompound nbt = itemStack.getOrCreateNbt();
            NbtList enchantments = nbt.getList("Enchantments", 10);
            if (enchantments == null) {
                enchantments = new NbtList();
            }

            // 创建新的附魔NBT
            NbtCompound enchantmentNbt = new NbtCompound();
            enchantmentNbt.putString("id", Registry.ENCHANTMENT.getId(enchantment).toString());
            enchantmentNbt.putInt("lvl", level);

            // 移除已存在的相同附魔
            String enchantmentIdStr = Registry.ENCHANTMENT.getId(enchantment).toString();
            for (int i = 0; i < enchantments.size(); i++) {
                NbtCompound existingEnchant = enchantments.getCompound(i);
                if (existingEnchant.getString("id").equals(enchantmentIdStr)) {
                    enchantments.remove(i);
                    break;
                }
            }

            enchantments.add(enchantmentNbt);
            nbt.put("Enchantments", enchantments);

            context.getSource().sendFeedback(
                Text.of("Applied " + enchantment.getName(level).getString() + " to the item"),
                true
            );

            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.of("Error executing command: " + e.getMessage()));
            return 0;
        }
    }
} 