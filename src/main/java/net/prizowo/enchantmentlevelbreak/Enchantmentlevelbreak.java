package net.prizowo.enchantmentlevelbreak;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.prizowo.enchantmentlevelbreak.command.CEnchantCommand;

public class Enchantmentlevelbreak implements ModInitializer {
    @Override
    public void onInitialize() {
        // 注册命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CEnchantCommand.register(dispatcher);
        });
    }
}
