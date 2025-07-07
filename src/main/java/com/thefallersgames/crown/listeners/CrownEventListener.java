package com.thefallersgames.crown.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.thefallersgames.crown.CrownPlugin;
import com.thefallersgames.crown.managers.ConfigManager;
import com.thefallersgames.crown.managers.CrownManager;

/**
 * Handles all crown-related events
 */
public class CrownEventListener implements Listener {
    
    private CrownPlugin plugin;
    private CrownManager crownManager;
    private ConfigManager configManager;
    
    /**
     * Creates a new CrownEventListener
     * @param plugin The plugin instance
     * @param crownManager The crown manager
     * @param configManager The configuration manager
     */
    public CrownEventListener(CrownPlugin plugin, CrownManager crownManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.crownManager = crownManager;
        this.configManager = configManager;
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();
        
        // Check if player is a crown owner
        if (crownManager.isPlayerCrownOwner(playerUUID)) {
            // Get the killer if it exists
            Player killer = player.getKiller();
            
            // Check if transfer on kill is enabled, there is a killer, and either
            // prioritize_transfer is enabled or dropping is disabled
            boolean shouldTransfer = configManager.isCrownTransferOnKillEnabled() && 
                                    killer != null && 
                                    (configManager.isPrioritizeTransferEnabled() || !configManager.isDroppingAllowed());
            
            if (shouldTransfer) {
                // Transfer crown to killer with transfer flag
                crownManager.giveCrown(killer, true);
                killer.sendMessage(configManager.getMessage("crown_transferred"));
                
                // Remove the crown from the list of items to restore on respawn
                crownManager.setCrownRespawnFlag(playerUUID, false);
                
                // Remove crown from drops
                event.getDrops().removeIf(item -> crownManager.isCrownItem(item));
            } 
            // If we shouldn't transfer and dropping is allowed, let the crown drop naturally
            else if (configManager.isDroppingAllowed()) {
                // Check if crown should be removed from player data on death
                if (configManager.shouldRemoveCrownOnDeath()) {
                    // Remove player from crown owners list since the crown is now dropped
                    crownManager.removePlayerFromCrownOwners(playerUUID);
                }
                // Don't remove crown from drops - let it drop naturally
            } 
            // Neither transfer nor dropping is enabled, use original respawn behavior
            else {
                // Mark player for crown respawn
                crownManager.markForRespawn(playerUUID);
                
                // Remove crown from drops
                event.getDrops().removeIf(item -> crownManager.isCrownItem(item));
            }
        }
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        // Check if player should receive a crown on respawn
        if (crownManager.shouldReceiveCrownOnRespawn(playerUUID)) {
            // Schedule task to give crown after respawn
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                crownManager.giveCrown(player);
                crownManager.setCrownRespawnFlag(playerUUID, false);
            }, 20L); // 1 second delay to ensure player is fully respawned
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        // Get the clicked inventory and item
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        // Check if a crown is involved in this click
        boolean currentIsCrown = currentItem != null && crownManager.isCrownItem(currentItem);
        boolean cursorIsCrown = cursorItem != null && crownManager.isCrownItem(cursorItem);
        
        if (!currentIsCrown && !cursorIsCrown) {
            return; // No crown involved, do nothing
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();
        
        // If dropping is allowed in config, check if the player is trying to pick up a crown
        if (configManager.isDroppingAllowed() && currentIsCrown && !cursorIsCrown) {
            // Check if player already has a crown (other than the one being clicked)
            if (crownManager.isPlayerCrownOwner(playerUUID) && hasOtherCrown(player, currentItem)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You already have a crown!");
                return;
            }
            
            // For picked up crowns, update the owner
            crownManager.updateCrownOwner(currentItem, player);
            
            // Register the player as a crown owner
            crownManager.registerPlayerAsCrownOwner(playerUUID);
            return;
        }
        
        // Case 1: Prevent other players from picking up crown items if dropping is not allowed
        if (currentIsCrown) {
            UUID ownerUUID = crownManager.getCrownOwner(currentItem);
            if (ownerUUID != null && !ownerUUID.equals(playerUUID)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "This crown doesn't belong to you!");
                
                // If they somehow got it in their inventory, schedule removal
                if (event.getSlotType() != SlotType.ARMOR) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.getInventory().remove(currentItem);
                    }, 1L);
                }
                return;
            }
        }
        
        // Case 2: Prevent putting crown in containers
        if (cursorIsCrown && clickedInventory != null && clickedInventory.getType() != InventoryType.PLAYER) {
            // Player is trying to put a crown in a container
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "The crown cannot be stored in containers!");
            return;
        }
    }
    
    /**
     * Checks if a player has a crown other than the specified one
     * @param player The player to check
     * @param excludedCrown The crown to exclude from the check
     * @return true if the player has a crown other than the specified one
     */
    private boolean hasOtherCrown(Player player, ItemStack excludedCrown) {
        // Check helmet slot
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null && crownManager.isCrownItem(helmet) && !helmet.equals(excludedCrown)) {
            return true;
        }
        
        // Check entire inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && crownManager.isCrownItem(item) && !item.equals(excludedCrown)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Handle crown pickup event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;  // Only care about players picking up items
        }
        
        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        
        // Check if the picked up item is a crown
        if (crownManager.isCrownItem(item)) {
            UUID playerUUID = player.getUniqueId();
            
            // Check if player already has a crown
            if (crownManager.isPlayerCrownOwner(playerUUID) && playerHasCrown(player)) {
                // Player already has a crown, prevent pickup
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You already have a crown!");
                return;
            }
            
            if (configManager.isDroppingAllowed()) {
                // Update the crown's owner to this player
                crownManager.updateCrownOwner(item, player);
                
                // Register the player as a crown owner
                crownManager.registerPlayerAsCrownOwner(player.getUniqueId());
                
                // Notify the player
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendMessage(configManager.getMessage("crown_given"));
                }, 1L);
            } else {
                // If dropping is not allowed, check if this player is the owner
                UUID ownerUUID = crownManager.getCrownOwner(item);
                
                if (ownerUUID != null && !ownerUUID.equals(playerUUID)) {
                    // Cancel the pickup
                    event.setCancelled(true);
                    
                    // Notify the player
                    player.sendMessage(ChatColor.RED + "This crown doesn't belong to you!");
                }
            }
        }
    }
    
    /**
     * Checks if a player has a crown in their inventory
     * @param player The player to check
     * @return true if the player has a crown
     */
    private boolean playerHasCrown(Player player) {
        // Check helmet slot
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null && crownManager.isCrownItem(helmet)) {
            return true;
        }
        
        // Check entire inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && crownManager.isCrownItem(item)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Prevent dragging crown items into containers
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack draggedItem = event.getOldCursor();
        
        if (crownManager.isCrownItem(draggedItem)) {
            // Check if any of the slots are in a non-player inventory
            boolean nonPlayerSlot = event.getRawSlots().stream()
                .anyMatch(slot -> slot < event.getView().getTopInventory().getSize());
            
            if (nonPlayerSlot) {
                event.setCancelled(true);
                Player player = (Player) event.getWhoClicked();
                player.sendMessage(ChatColor.RED + "The crown cannot be stored in containers!");
            }
        }
    }
    
    /**
     * Prevent moving crown items between inventories
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (crownManager.isCrownItem(event.getItem())) {
            // Cancel any automated movement of crown items
            event.setCancelled(true);
        }
    }
    
    /**
     * Prevents players from dropping crown items if configured
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        
        if (crownManager.isCrownItem(droppedItem)) {
            // Check if dropping is allowed in config
            if (!configManager.isDroppingAllowed()) {
                // Cancel the drop event
                event.setCancelled(true);
                
                // Notify the player
                Player player = event.getPlayer();
                player.sendMessage(ChatColor.RED + "The crown cannot be dropped!");
            } else {
                // If dropping is allowed, remove the player from crown owners
                Player player = event.getPlayer();
                crownManager.removePlayerFromCrownOwners(player.getUniqueId());
            }
        }
    }
} 