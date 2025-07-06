package com.thefallersgames.crown.managers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.thefallersgames.crown.CrownPlugin;

/**
 * Manages data persistence for crown ownership
 */
public class DataManager {
    private CrownPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    
    /**
     * Creates a new DataManager
     * @param plugin The plugin instance
     */
    public DataManager(CrownPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    /**
     * Saves crown ownership data to disk
     * @param crownOwners Map of player UUIDs to crown owner UUIDs
     */
    public void saveCrownOwners(Map<UUID, UUID> crownOwners) {
        // Clear previous data
        dataConfig.set("crown_owners", null);
        
        // Save each crown owner
        for (Map.Entry<UUID, UUID> entry : crownOwners.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            dataConfig.set("crown_owners." + key, value);
        }
        
        // Save the file
        try {
            dataConfig.save(dataFile);
            plugin.getLogger().info("Saved crown ownership data");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save crown ownership data: " + e.getMessage());
        }
    }
    
    /**
     * Saves crown respawn data to disk
     * @param pendingRespawns Map of player UUIDs to respawn flags
     */
    public void savePendingRespawns(Map<UUID, Boolean> pendingRespawns) {
        // Clear previous data
        dataConfig.set("pending_respawns", null);
        
        // Save each pending respawn
        for (Map.Entry<UUID, Boolean> entry : pendingRespawns.entrySet()) {
            String key = entry.getKey().toString();
            boolean value = entry.getValue();
            dataConfig.set("pending_respawns." + key, value);
        }
        
        // Save the file
        try {
            dataConfig.save(dataFile);
            plugin.getLogger().info("Saved pending respawn data");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save pending respawn data: " + e.getMessage());
        }
    }
    
    /**
     * Loads crown ownership data from disk
     * @return Map of player UUIDs to crown owner UUIDs
     */
    public Map<UUID, UUID> loadCrownOwners() {
        Map<UUID, UUID> crownOwners = new HashMap<>();
        
        // Check if the section exists
        if (dataConfig.isConfigurationSection("crown_owners")) {
            // Load each crown owner
            for (String key : dataConfig.getConfigurationSection("crown_owners").getKeys(false)) {
                String value = dataConfig.getString("crown_owners." + key);
                
                try {
                    UUID playerUUID = UUID.fromString(key);
                    UUID ownerUUID = UUID.fromString(value);
                    crownOwners.put(playerUUID, ownerUUID);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in crown owners data: " + key);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + crownOwners.size() + " crown owners from data file");
        return crownOwners;
    }
    
    /**
     * Loads crown respawn data from disk
     * @return Map of player UUIDs to respawn flags
     */
    public Map<UUID, Boolean> loadPendingRespawns() {
        Map<UUID, Boolean> pendingRespawns = new HashMap<>();
        
        // Check if the section exists
        if (dataConfig.isConfigurationSection("pending_respawns")) {
            // Load each pending respawn
            for (String key : dataConfig.getConfigurationSection("pending_respawns").getKeys(false)) {
                boolean value = dataConfig.getBoolean("pending_respawns." + key);
                
                try {
                    UUID playerUUID = UUID.fromString(key);
                    pendingRespawns.put(playerUUID, value);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in pending respawns data: " + key);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + pendingRespawns.size() + " pending respawns from data file");
        return pendingRespawns;
    }
} 