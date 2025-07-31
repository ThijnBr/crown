package com.thefallersgames.crown.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.thefallersgames.crown.CrownPlugin;

/**
 * Manages crown items and crown ownership
 */
public class CrownManager {
    private CrownPlugin plugin;
    private ConfigManager configManager;
    private NamespacedKey crownKey;
    private NamespacedKey ownerKey;
    private Map<UUID, UUID> crownOwners; // Player UUID -> Crown Owner UUID
    private Map<UUID, Boolean> pendingCrownRespawn; // Player UUID -> Should receive crown on respawn
    
    /**
     * Creates a new CrownManager
     * @param plugin The plugin instance
     * @param configManager The configuration manager
     */
    public CrownManager(CrownPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.crownKey = new NamespacedKey(plugin, "crown_item");
        this.ownerKey = new NamespacedKey(plugin, "crown_owner");
        this.crownOwners = new HashMap<>();
        this.pendingCrownRespawn = new HashMap<>();
    }
    
    /**
     * Creates a crown item with the configured properties
     * @param player The player who will receive the crown
     * @return The created crown item
     */
    public ItemStack createCrown(Player player) {
        Material crownMaterial = Material.valueOf(configManager.getCrownMaterial());
        ItemStack crown = new ItemStack(crownMaterial, 1);
        ItemMeta meta = crown.getItemMeta();
        
        // Set name and lore
        meta.setDisplayName(configManager.getCrownName());
        
        // Get base lore from config
        List<String> lore = new ArrayList<>(configManager.getCrownLore());
        
        // Set attributes
        double healthBonus = configManager.getHealthBonus();
        AttributeModifier healthModifier = new AttributeModifier(
                UUID.randomUUID(), 
                "crown.health", 
                healthBonus, 
                AttributeModifier.Operation.ADD_NUMBER, 
                EquipmentSlot.HEAD
        );
        meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, healthModifier);
        
        double armorBonus = configManager.getArmorBonus();
        AttributeModifier armorModifier = new AttributeModifier(
                UUID.randomUUID(),
                "crown.armor",
                armorBonus,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HEAD
        );
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, armorModifier);
        
        // Add flags
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        
        // Add custom tag to identify this as a crown item
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(crownKey, PersistentDataType.STRING, "crown");
        container.set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        
        // Set the updated lore before we add enchantments
        meta.setLore(lore);
        crown.setItemMeta(meta);
        
        // Apply custom enchantments from config
        configManager.applyEnchantments(crown);
        
        // Update meta after enchantments applied
        meta = crown.getItemMeta();
        
        // Add stats and enchantments to lore
        lore.add("");
        lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Stats:");
        
        // Add health bonus info
        double hearts = healthBonus / 2.0;
        lore.add(ChatColor.RED + "• +" + hearts + " Hearts");
        
        // Add armor stats
        lore.add(ChatColor.WHITE + "• " + armorBonus + " Armor");
        
        // Add enchantments section if there are any
        Map<Enchantment, Integer> enchants = crown.getEnchantments();
        if (!enchants.isEmpty()) {
            lore.add("");
            lore.add(ChatColor.AQUA + "" + ChatColor.BOLD + "Enchantments:");
            
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                String enchantName = formatEnchantmentName(entry.getKey().getKey().getKey());
                int level = entry.getValue();
                
                // Format special enchants differently
                if (entry.getKey().equals(Enchantment.BINDING_CURSE)) {
                    lore.add(ChatColor.RED + "• Curse of Binding");
                } else if (entry.getKey().equals(Enchantment.VANISHING_CURSE)) {
                    lore.add(ChatColor.RED + "• Curse of Vanishing");
                } else {
                    // Convert Roman numerals for levels
                    String levelStr = level == 1 ? "" : " " + toRoman(level);
                    lore.add(ChatColor.GRAY + "• " + enchantName + levelStr);
                }
            }
        }
        
        // Add owner info
        lore.add("");
        lore.add(ChatColor.YELLOW + "Owner: " + ChatColor.WHITE + player.getName());
        
        meta.setLore(lore);
        
        // Hide enchantments and attributes as requested
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        crown.setItemMeta(meta);
        
        return crown;
    }
    
    /**
     * Format enchantment name from key to readable text
     * @param key The enchantment key (e.g., "protection")
     * @return Formatted name (e.g., "Protection")
     */
    private String formatEnchantmentName(String key) {
        String[] parts = key.split("_");
        StringBuilder result = new StringBuilder();
        
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(part.substring(0, 1).toUpperCase());
                result.append(part.substring(1).toLowerCase());
                result.append(" ");
            }
        }
        
        return result.toString().trim();
    }
    
    /**
     * Convert a number to Roman numerals
     * @param num The number to convert
     * @return The Roman numeral string
     */
    private String toRoman(int num) {
        if (num <= 0 || num > 10) {
            return String.valueOf(num);
        }
        
        final String[] romans = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return romans[num];
    }
    
    /**
     * Gives a crown to the specified player
     * @param player The player to give the crown to
     */
    public void giveCrown(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Remove any existing crown they might have
        removeCrown(player);
        
        // Remove any crowns from other players in their inventory
        removeOtherCrownsFromInventory(player);
        
        // Create and give the new crown
        ItemStack crown = createCrown(player);
        
        // Store the player as a crown owner
        crownOwners.put(playerUUID, playerUUID);
        
        // Check if auto-equip is enabled
        boolean autoEquip = configManager.isAutoEquipOnGiveEnabled();
        
        // Give the crown to the player
        if (autoEquip) {
            // Handle any existing helmet
            handleExistingHelmet(player);
            
            // Force equip the crown
            player.getInventory().setHelmet(crown);
            player.sendMessage(configManager.getMessage("crown_given"));
        } else {
            player.getInventory().addItem(crown);
            player.sendMessage(configManager.getMessage("crown_given"));
        }
        
        // Play particle effect for becoming leader
        playLeaderParticleEffect(player);
    }
    
    /**
     * Gives a crown to the specified player after a transfer (e.g., from kill)
     * @param player The player to give the crown to
     * @param isTransfer Whether this is a transfer event (e.g., from kill)
     */
    public void giveCrown(Player player, boolean isTransfer) {
        UUID playerUUID = player.getUniqueId();
        
        // Remove any existing crown they might have
        removeCrown(player);
        
        // Remove any crowns from other players in their inventory
        removeOtherCrownsFromInventory(player);
        
        // Create and give the new crown
        ItemStack crown = createCrown(player);
        
        // Store the player as a crown owner
        crownOwners.put(playerUUID, playerUUID);
        
        // Check if auto-equip is enabled (always use the auto_equip_on_give setting)
        boolean autoEquip = configManager.isAutoEquipOnGiveEnabled();
        
        // Give the crown to the player
        if (autoEquip) {
            // Handle any existing helmet
            handleExistingHelmet(player);
            
            // Force equip the crown
            player.getInventory().setHelmet(crown);
        } else {
            player.getInventory().addItem(crown);
        }
        
        // Play particle effect for becoming leader (even on transfer)
        playLeaderParticleEffect(player);
    }
    
    /**
     * Handles an existing helmet by moving it to inventory or dropping it
     * @param player The player whose helmet needs to be handled
     */
    private void handleExistingHelmet(Player player) {
        ItemStack currentHelmet = player.getInventory().getHelmet();
        if (currentHelmet != null && !isCrownItem(currentHelmet)) {
            // Try to add to inventory
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(currentHelmet);
            
            // If inventory is full, drop the item
            if (!leftover.isEmpty()) {
                for (ItemStack item : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
        }
    }
    
    /**
     * Removes the crown from the specified player
     * @param player The player to remove the crown from
     */
    public void removeCrown(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Check if player is wearing a crown
        ItemStack helmet = player.getInventory().getHelmet();
        if (isCrownItem(helmet)) {
            player.getInventory().setHelmet(null);
            player.sendMessage(configManager.getMessage("crown_removed"));
        }
        
        // Remove all crown items from player inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (isCrownItem(item)) {
                player.getInventory().remove(item);
            }
        }
        
        // Remove from crown owners list
        crownOwners.remove(playerUUID);
        pendingCrownRespawn.remove(playerUUID);
    }
    
    /**
     * Checks if an item is a crown
     * @param item The item to check
     * @return true if the item is a crown, false otherwise
     */
    public boolean isCrownItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(crownKey, PersistentDataType.STRING);
    }
    
    /**
     * Gets the UUID of the owner of the crown
     * @param item The crown item
     * @return The UUID of the owner, or null if not a crown or no owner
     */
    public UUID getCrownOwner(ItemStack item) {
        if (!isCrownItem(item)) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String ownerString = container.get(ownerKey, PersistentDataType.STRING);
        
        if (ownerString == null) {
            return null;
        }
        
        return UUID.fromString(ownerString);
    }
    
    /**
     * Updates the owner of the crown item
     * @param item The crown item to update
     * @param player The new owner
     */
    public void updateCrownOwner(ItemStack item, Player player) {
        if (!isCrownItem(item)) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        item.setItemMeta(meta);
    }
    
    /**
     * Registers a player as a crown owner
     * @param playerUUID The UUID of the player to register
     */
    public void registerPlayerAsCrownOwner(UUID playerUUID) {
        crownOwners.put(playerUUID, playerUUID);
    }
    
    /**
     * Removes a player from the crown owners list
     * @param playerUUID The UUID of the player to remove
     */
    public void removePlayerFromCrownOwners(UUID playerUUID) {
        crownOwners.remove(playerUUID);
    }
    
    /**
     * Checks if a player is a crown owner
     * @param playerUUID The player's UUID
     * @return true if the player is a crown owner
     */
    public boolean isPlayerCrownOwner(UUID playerUUID) {
        return crownOwners.containsKey(playerUUID);
    }
    
    /**
     * Marks a player for crown respawn
     * @param playerUUID The player's UUID
     */
    public void markForRespawn(UUID playerUUID) {
        pendingCrownRespawn.put(playerUUID, true);
    }
    
    /**
     * Checks if a player should receive a crown on respawn
     * @param playerUUID The player's UUID
     * @return true if the player should receive a crown
     */
    public boolean shouldReceiveCrownOnRespawn(UUID playerUUID) {
        return pendingCrownRespawn.containsKey(playerUUID) && pendingCrownRespawn.get(playerUUID);
    }
    
    /**
     * Sets the crown respawn flag for a player
     * @param playerUUID The player's UUID
     * @param value The flag value
     */
    public void setCrownRespawnFlag(UUID playerUUID, boolean value) {
        pendingCrownRespawn.put(playerUUID, value);
    }
    
    /**
     * Gets the map of crown owners
     * @return Map of player UUIDs to crown owner UUIDs
     */
    public Map<UUID, UUID> getCrownOwners() {
        return crownOwners;
    }
    
    /**
     * Sets the map of crown owners
     * @param crownOwners Map of player UUIDs to crown owner UUIDs
     */
    public void setCrownOwners(Map<UUID, UUID> crownOwners) {
        this.crownOwners = crownOwners;
    }
    
    /**
     * Gets the map of pending crown respawns
     * @return Map of player UUIDs to respawn flags
     */
    public Map<UUID, Boolean> getPendingCrownRespawn() {
        return pendingCrownRespawn;
    }
    
    /**
     * Sets the map of pending crown respawns
     * @param pendingCrownRespawn Map of player UUIDs to respawn flags
     */
    public void setPendingCrownRespawn(Map<UUID, Boolean> pendingCrownRespawn) {
        this.pendingCrownRespawn = pendingCrownRespawn;
    }
    
    /**
     * Removes any crowns belonging to other players from the player's inventory
     * @param player The player whose inventory to check
     */
    private void removeOtherCrownsFromInventory(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (isCrownItem(item)) {
                UUID ownerUUID = getCrownOwner(item);
                // Remove crowns that belong to other players
                if (ownerUUID != null && !ownerUUID.equals(playerUUID)) {
                    player.getInventory().remove(item);
                }
            }
        }
    }
    
    /**
     * Plays the leader particle effect for a player
     * @param player The player who became leader
     */
    private void playLeaderParticleEffect(Player player) {
        if (plugin.getParticleManager() != null) {
            plugin.getParticleManager().playLeaderParticleEffect(player);
        }
    }
} 