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
     * Checks if the crown should have binding curse
     * @return true if binding curse is enabled
     */
    public boolean hasBindingCurse() {
        return config.getBoolean("crown.binding_curse", true);
    }
    
    /**
     * Checks if the crown should have vanishing curse
     * @return true if vanishing curse is enabled
     */
    public boolean hasVanishingCurse() {
        return config.getBoolean("crown.vanishing_curse", false);
    }
    
    /**
     * Checks if auto-equip on give is enabled
     * @return true if auto-equip on give is enabled
     */
    public boolean isAutoEquipOnGiveEnabled() {
        return config.getBoolean("behavior.auto_equip_on_give", true);
    }
    
    /**
     * Checks if crown transfer on kill is enabled
     * @return true if crown transfer on kill is enabled
     */
    public boolean isCrownTransferOnKillEnabled() {
        return config.getBoolean("behavior.transfer_on_kill", true);
    }
    
    /**
     * Checks if transfer to killer should be prioritized over dropping
     * @return true if transfer to killer should be prioritized
     */
    public boolean isPrioritizeTransferEnabled() {
        return config.getBoolean("behavior.prioritize_transfer", true);
    }
    
    /**
     * Checks if dropping the crown is allowed
     * @return true if dropping the crown is allowed
     */
    public boolean isDroppingAllowed() {
        return config.getBoolean("behavior.allow_dropping", false);
    }
    
    /**
     * Checks if crown should be removed from player data on death
     * @return true if crown should be removed from player data on death
     */
    public boolean shouldRemoveCrownOnDeath() {
        return config.getBoolean("behavior.remove_crown_on_death", true);
    }
    
    /**
     * Applies configured enchantments to the crown
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
    
    // Particle effect configuration methods
    
    /**
     * Checks if particle effects are enabled
     * @return true if particle effects are enabled
     */
    public boolean areParticlesEnabled() {
        return config.getBoolean("particles.enabled", true);
    }
    
    /**
     * Gets the particle type to use
     * @return The particle type as a string
     */
    public String getParticleType() {
        return config.getString("particles.type", "FLAME");
    }
    
    /**
     * Gets the number of particles to spawn
     * @return The particle count
     */
    public int getParticleCount() {
        return config.getInt("particles.count", 30);
    }
    
    /**
     * Gets the duration of the particle effect in ticks
     * @return The duration in ticks
     */
    public int getParticleDuration() {
        return config.getInt("particles.duration", 60);
    }
    
    /**
     * Gets the X offset for particles
     * @return The X offset
     */
    public double getParticleOffsetX() {
        return config.getDouble("particles.offset.x", 0.3);
    }
    
    /**
     * Gets the Y offset for particles
     * @return The Y offset
     */
    public double getParticleOffsetY() {
        return config.getDouble("particles.offset.y", 1.8);
    }
    
    /**
     * Gets the Z offset for particles
     * @return The Z offset
     */
    public double getParticleOffsetZ() {
        return config.getDouble("particles.offset.z", 0.3);
    }
    
    /**
     * Gets the particle speed
     * @return The particle speed
     */
    public double getParticleSpeed() {
        return config.getDouble("particles.speed", 0.2);
    }
    
    /**
     * Gets the red component of the particle color
     * @return The red value (0-255)
     */
    public int getParticleColorRed() {
        return config.getInt("particles.color.red", 255);
    }
    
    /**
     * Gets the green component of the particle color
     * @return The green value (0-255)
     */
    public int getParticleColorGreen() {
        return config.getInt("particles.color.green", 215);
    }
    
    /**
     * Gets the blue component of the particle color
     * @return The blue value (0-255)
     */
    public int getParticleColorBlue() {
        return config.getInt("particles.color.blue", 0);
    }
} 