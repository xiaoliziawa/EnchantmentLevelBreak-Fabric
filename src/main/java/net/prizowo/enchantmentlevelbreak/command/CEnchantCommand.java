package net.prizowo.enchantmentlevelbreak.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class CEnchantCommand {
    private static final String TRANSLATION_PREFIX = "command.enchantmentlevelbreak.cenchant.";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("cenchant")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("enchantment", StringArgumentType.greedyString())
                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                Registries.ENCHANTMENT.getIds().stream()
                                        .map(Identifier::toString),
                                builder))
                        .executes(context -> enchantItem(context, 1))
                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1))
                                .executes(context -> enchantItem(context,
                                        IntegerArgumentType.getInteger(context, "level"))))));
    }

    private static int enchantItem(CommandContext<ServerCommandSource> context, int level) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ItemStack itemStack = player.getMainHandStack();

        if (itemStack.isEmpty()) {
            context.getSource().sendError(Text.translatable(TRANSLATION_PREFIX + "no_item"));
            return 0;
        }

        String enchantmentInput = StringArgumentType.getString(context, "enchantment");
        String[] parts = enchantmentInput.split("\\s+", 2);
        String enchantmentName = parts[0];
        if (parts.length > 1) {
            try {
                level = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {}
        }

        Enchantment enchantment = getEnchantment(enchantmentName);
        if (enchantment == null) {
            context.getSource().sendError(Text.translatable(TRANSLATION_PREFIX + "invalid_enchantment", enchantmentName));
            return 0;
        }

        Map<Enchantment, Integer> enchantments = new HashMap<>(EnchantmentHelper.get(itemStack));
        enchantments.put(enchantment, level);
        EnchantmentHelper.set(enchantments, itemStack);

        int finalLevel = level;
        context.getSource().sendFeedback(
                () -> Text.translatable(TRANSLATION_PREFIX + "success", enchantment.getName(finalLevel)),
                true
        );

        return 1;
    }

    private static Enchantment getEnchantment(String name) {
        Identifier enchantmentId = name.contains(":")
                ? new Identifier(name)
                : new Identifier("minecraft", name);
        return Registries.ENCHANTMENT.get(enchantmentId);
    }
} 