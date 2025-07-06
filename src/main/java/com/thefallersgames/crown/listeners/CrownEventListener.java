package com.thefallersgames.crown.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
            // Mark player for crown respawn
            crownManager.markForRespawn(playerUUID);
            
            // Remove crown from drops
            event.getDrops().removeIf(item -> crownManager.isCrownItem(item));
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
        
        // Case 1: Prevent other players from picking up crown items
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
     * Prevents players from dropping crown items
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        
        if (crownManager.isCrownItem(droppedItem)) {
            // Cancel the drop event
            event.setCancelled(true);
            
            // Notify the player
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.RED + "The crown cannot be dropped!");
        }
    }
} 