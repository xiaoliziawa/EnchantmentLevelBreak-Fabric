package net.prizowo.enchantmentlevelbreak.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CEnchantCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("cenchant")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("enchantment", StringArgumentType.greedyString())
                        .suggests((context, builder) -> CommandSource.suggestIdentifiers(
                                Registries.ENCHANTMENT.getIds(), builder))
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
                context.getSource().sendError(Text.literal("你必须手持一个物品来附魔"));
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

            Enchantment enchantment = Registries.ENCHANTMENT.get(enchantmentId);

            if (enchantment == null) {
                context.getSource().sendError(Text.literal("无效的附魔: " + enchantmentName));
                return 0;
            }

            itemStack.addEnchantment(enchantment, level);

            int finalLevel = level;
            context.getSource().sendFeedback(() ->
                Text.literal("已将 " + enchantment.getName(finalLevel).getString() + " 附魔应用到物品上"), true);

            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("执行命令时发生错误: " + e.getMessage()));
            return 0;
        }
    }
} 