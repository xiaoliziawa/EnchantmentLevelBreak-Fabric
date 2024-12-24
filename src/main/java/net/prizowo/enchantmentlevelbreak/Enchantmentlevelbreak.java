package net.prizowo.enchantmentlevelbreak;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class Enchantmentlevelbreak implements ModInitializer {
    public static final String MODID = "enchantmentlevelbreak";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CEnchantCommand.register(dispatcher);
        });
    }
}
