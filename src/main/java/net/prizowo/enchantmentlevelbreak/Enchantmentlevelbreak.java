package net.prizowo.enchantmentlevelbreak;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.prizowo.enchantmentlevelbreak.command.CEnchantCommand;
import net.prizowo.enchantmentlevelbreak.config.ModConfig;

public class Enchantmentlevelbreak implements ModInitializer {
    @Override
    public void onInitialize() {
        ModConfig.load();
        
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> CEnchantCommand.register(dispatcher));
    }
}
