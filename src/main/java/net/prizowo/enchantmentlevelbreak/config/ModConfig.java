package net.prizowo.enchantmentlevelbreak.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("enchantmentlevelbreak.json");
    private static ModConfig INSTANCE;

    private boolean useRomanNumerals = true;
    private boolean allowAnyEnchantment = false;
    private boolean allowLevelStacking = false;
    private int romanNumeralsThreshold = 5000;

    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public boolean isUseRomanNumerals() {
        return useRomanNumerals;
    }

    public boolean isAllowAnyEnchantment() {
        return allowAnyEnchantment;
    }

    public boolean isAllowLevelStacking() {
        return allowLevelStacking;
    }

    public int getRomanNumeralsThreshold() {
        return romanNumeralsThreshold;
    }

    public void setUseRomanNumerals(boolean useRomanNumerals) {
        this.useRomanNumerals = useRomanNumerals;
        save();
    }

    public void setAllowAnyEnchantment(boolean allowAnyEnchantment) {
        this.allowAnyEnchantment = allowAnyEnchantment;
        save();
    }

    public void setAllowLevelStacking(boolean allowLevelStacking) {
        this.allowLevelStacking = allowLevelStacking;
        save();
    }

    public void setRomanNumeralsThreshold(int threshold) {
        this.romanNumeralsThreshold = threshold;
        save();
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    INSTANCE = GSON.fromJson(reader, ModConfig.class);
                }
            } else {
                INSTANCE = new ModConfig();
                INSTANCE.save();
            }
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
            INSTANCE = new ModConfig();
        }
    }

    private void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }
}