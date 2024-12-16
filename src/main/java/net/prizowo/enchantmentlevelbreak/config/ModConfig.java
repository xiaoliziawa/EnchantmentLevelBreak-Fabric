package net.prizowo.enchantmentlevelbreak.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ModConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("enchantmentlevelbreak.properties");
    private static ModConfig INSTANCE;

    public boolean allowEnchantAllItems = false;
    public boolean allowAllEnchantmentsCombine = false;

    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        INSTANCE = new ModConfig();
        
        try {
            if (Files.exists(CONFIG_PATH)) {
                Properties props = new Properties();
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    props.load(reader);
                }
                
                INSTANCE.allowEnchantAllItems = Boolean.parseBoolean(
                    props.getProperty("allowEnchantAllItems", "false")
                );
                INSTANCE.allowAllEnchantmentsCombine = Boolean.parseBoolean(
                    props.getProperty("allowAllEnchantmentsCombine", "false")
                );
            } else {
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            
            Properties props = new Properties() {
                @Override
                public synchronized void store(Writer writer, String comments) throws IOException {
                    writer.write("#" + comments.replace("\n", "\n#") + "\n");
                    for (String key : stringPropertyNames()) {
                        writer.write(key + "=" + getProperty(key) + "\n");
                    }
                }
            };
            
            props.setProperty("allowEnchantAllItems", String.valueOf(INSTANCE.allowEnchantAllItems));
            props.setProperty("allowAllEnchantmentsCombine", String.valueOf(INSTANCE.allowAllEnchantmentsCombine));
            
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                props.store(writer, """
                    EnchantmentLevelBreak Configuration
                    allowEnchantAllItems: 是否允许对任意物品进行附魔
                    allowAllEnchantmentsCombine: 是否允许所有附魔互相组合""");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 