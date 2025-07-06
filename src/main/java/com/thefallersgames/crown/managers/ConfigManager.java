package com.thefallersgames.crown.managers;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import com.thefallersgames.crown.CrownPlugin;

/**
 * Handles plugin configuration settings
 */
public class ConfigManager {
    private CrownPlugin plugin;
    private FileConfiguration config;
    
    /**
     * Creates a new ConfigManager
     * @param plugin The plugin instance
     */
    public ConfigManager(CrownPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }
    
    /**
     * Reloads the configuration
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    /**
     * Gets the material for the crown
     * @return The material name as a string
     */
    public String getCrownMaterial() {
        return config.getString("crown.material", "GOLDEN_HELMET");
    }
    
    /**
     * Gets the display name for the crown
     * @return The formatted display name
     */
    public String getCrownName() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("crown.name", "&6The Crown"));
    }
    
    /**
     * Gets the lore for the crown
     * @return List of formatted lore strings
     */
    public java.util.List<String> getCrownLore() {
        java.util.List<String> loreConfig = config.getStringList("crown.lore");
        java.util.List<String> formattedLore = new java.util.ArrayList<>();
        
        for (String line : loreConfig) {
            formattedLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        
        return formattedLore;
    }
    
    /**
     * Gets the health bonus for the crown
     * @return The health bonus amount
     */
    public double getHealthBonus() {
        return config.getDouble("crown.health_bonus", 4.0);
    }
    
    /**
     * Gets the armor bonus for the crown
     * @return The armor bonus amount
     */
    public double getArmorBonus() {
        return config.getDouble("crown.armor", 3.0);
    }
    
    /**
     * Checks if binding curse should be applied
     * @return true if binding curse should be applied
     */
    public boolean hasBindingCurse() {
        return config.getBoolean("crown.binding_curse", true);
    }
    
    /**
     * Checks if vanishing curse should be applied
     * @return true if vanishing curse should be applied
     */
    public boolean hasVanishingCurse() {
        return config.getBoolean("crown.vanishing_curse", false);
    }
    
    /**
     * Applies custom enchantments from config to the crown
     * @param crown The crown item to enchant
     */
    public void applyEnchantments(ItemStack crown) {
        // Apply curses if configured
        if (hasBindingCurse()) {
            crown.addUnsafeEnchantment(Enchantment.BINDING_CURSE, 1);
        }
        
        if (hasVanishingCurse()) {
            crown.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
        }
        
        // Apply custom enchantments from config
        ConfigurationSection enchantmentsSection = config.getConfigurationSection("crown.enchantments");
        if (enchantmentsSection != null) {
            for (String enchName : enchantmentsSection.getKeys(false)) {
                try {
                    Enchantment enchantment = Enchantment.getByName(enchName);
                    if (enchantment != null) {
                        int level = enchantmentsSection.getInt(enchName, 1);
                        crown.addUnsafeEnchantment(enchantment, level);
                    } else {
                        plugin.getLogger().warning("Unknown enchantment in config: " + enchName);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error applying enchantment " + enchName + ": " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Gets a formatted message from the config
     * @param key The message key
     * @return The formatted message with color codes
     */
    public String getMessage(String key) {
        String prefix = ChatColor.translateAlternateColorCodes('&', config.getString("messages.prefix", "&6[Crown] &r"));
        String message = ChatColor.translateAlternateColorCodes('&', config.getString("messages." + key, ""));
        return prefix + message;
    }
} 