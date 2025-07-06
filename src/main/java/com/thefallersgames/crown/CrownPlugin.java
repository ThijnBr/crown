package com.thefallersgames.crown;

import org.bukkit.plugin.java.JavaPlugin;

import com.thefallersgames.crown.commands.CrownCommand;
import com.thefallersgames.crown.listeners.CrownEventListener;
import com.thefallersgames.crown.managers.CrownManager;
import com.thefallersgames.crown.managers.ConfigManager;
import com.thefallersgames.crown.managers.DataManager;

public class CrownPlugin extends JavaPlugin {
    
    private ConfigManager configManager;
    private CrownManager crownManager;
    private DataManager dataManager;
    
    @Override
    public void onEnable() {
        // Initialize configuration
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        
        // Initialize data manager
        dataManager = new DataManager(this);
        
        // Initialize crown manager
        crownManager = new CrownManager(this, configManager);
        
        // Load saved data
        loadData();
        
        // Register commands
        getCommand("crown").setExecutor(new CrownCommand(this, crownManager, configManager));
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new CrownEventListener(this, crownManager, configManager), this);
        
        getLogger().info("Crown plugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save data before shutdown
        saveData();
        
        getLogger().info("Crown plugin has been disabled!");
    }
    
    /**
     * Save crown ownership data to disk
     */
    private void saveData() {
        if (crownManager != null && dataManager != null) {
            dataManager.saveCrownOwners(crownManager.getCrownOwners());
            dataManager.savePendingRespawns(crownManager.getPendingCrownRespawn());
            getLogger().info("Crown data saved successfully");
        }
    }
    
    /**
     * Load crown ownership data from disk
     */
    private void loadData() {
        if (crownManager != null && dataManager != null) {
            crownManager.setCrownOwners(dataManager.loadCrownOwners());
            crownManager.setPendingCrownRespawn(dataManager.loadPendingRespawns());
            getLogger().info("Crown data loaded successfully");
        }
    }
    
    /**
     * Gets the crown manager
     * @return The crown manager
     */
    public CrownManager getCrownManager() {
        return crownManager;
    }
    
    /**
     * Gets the configuration manager
     * @return The configuration manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Gets the data manager
     * @return The data manager
     */
    public DataManager getDataManager() {
        return dataManager;
    }
} 