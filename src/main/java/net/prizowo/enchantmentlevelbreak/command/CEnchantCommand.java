package net.prizowo.enchantmentlevelbreak.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CEnchantCommand {
    private static final SuggestionProvider<ServerCommandSource> SUGGEST_ENCHANTMENTS = (context, builder) -> {
        DynamicRegistryManager registryManager = context.getSource().getWorld().getRegistryManager();
        Registry<Enchantment> registry = registryManager.getOrThrow(RegistryKeys.ENCHANTMENT);
        return CommandSource.suggestIdentifiers(registry.getIds(), builder);
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("cenchant")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("enchantment", StringArgumentType.greedyString())
                        .suggests(SUGGEST_ENCHANTMENTS)
                        .executes(context -> enchantItem(context.getSource(),
                                StringArgumentType.getString(context, "enchantment"),
                                1))
                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1))
                                .executes(context -> enchantItem(context.getSource(),
                                        StringArgumentType.getString(context, "enchantment"),
                                        IntegerArgumentType.getInteger(context, "level"))))));
    }

    private static int enchantItem(ServerCommandSource source, String enchantmentInput, int level) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            ItemStack itemStack = player.getMainHandStack();

            if (itemStack.isEmpty()) {
                source.sendError(Text.translatable("command.enchantmentlevelbreak.cenchant.no_item"));
                return 0;
            }

            String[] parts = enchantmentInput.split("\\s+", 2);
            String enchantmentName = parts[0];
            if (parts.length > 1) {
                try {
                    long longLevel = Long.parseLong(parts[1]);
                    if (longLevel > Integer.MAX_VALUE) {
                        source.sendError(Text.translatable("command.enchantmentlevelbreak.cenchant.level_too_high", Integer.MAX_VALUE));
                        return 0;
                    }
                    level = (int) longLevel;
                } catch (NumberFormatException ignored) {
                }
            }

            // Check if level exceeds int max value
            if (level > Integer.MAX_VALUE || level < 1) {
                source.sendError(Text.translatable("command.enchantmentlevelbreak.cenchant.level_too_high", Integer.MAX_VALUE));
                return 0;
            }

            String fullName = enchantmentName.contains(":") ? enchantmentName : "minecraft:" + enchantmentName;
            Identifier enchantmentId = Identifier.tryParse(fullName);
            if (enchantmentId == null) {
                source.sendError(Text.translatable("command.enchantmentlevelbreak.cenchant.invalid_enchantment", fullName));
                return 0;
            }

            DynamicRegistryManager registryManager = source.getWorld().getRegistryManager();
            Registry<Enchantment> registry = registryManager.getOrThrow(RegistryKeys.ENCHANTMENT);
            RegistryEntry<Enchantment> enchantmentEntry = registry.getEntry(enchantmentId).orElse(null);

            if (enchantmentEntry == null) {
                source.sendError(Text.translatable("command.enchantmentlevelbreak.cenchant.invalid_enchantment", enchantmentName));
                return 0;
            }

            itemStack.addEnchantment(enchantmentEntry, level);
            int finalLevel = level;
            source.sendFeedback(() -> Text.translatable("command.enchantmentlevelbreak.cenchant.success", Enchantment.getName(enchantmentEntry, finalLevel)), true);

            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("An error occurred while executing the command: " + e.getMessage()));
            return 0;
        }
    }
}
