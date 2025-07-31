package com.thefallersgames.crown.managers;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.thefallersgames.crown.CrownPlugin;

/**
 * Manages particle effects for the crown plugin
 */
public class ParticleManager {
    private CrownPlugin plugin;
    private ConfigManager configManager;
    
    /**
     * Creates a new ParticleManager
     * @param plugin The plugin instance
     * @param configManager The configuration manager
     */
    public ParticleManager(CrownPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    /**
     * Plays a particle effect when a player becomes leader
     * @param player The player who became leader
     */
    public void playLeaderParticleEffect(Player player) {
        if (!configManager.areParticlesEnabled()) {
            return;
        }
        
        final String particleTypeStr = configManager.getParticleType();
        final Particle particleType = getParticleType(particleTypeStr);
        
        final double offsetX = configManager.getParticleOffsetX();
        final double offsetY = configManager.getParticleOffsetY();
        final double offsetZ = configManager.getParticleOffsetZ();
        final double speed = configManager.getParticleSpeed();
        final int count = configManager.getParticleCount();
        final int duration = configManager.getParticleDuration();
        
        // Create a repeating task for the particle effect
        new BukkitRunnable() {
            private int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                // Get the player's current location (so particles follow them)
                Location playerLocation = player.getLocation();
                
                // Calculate position around the player's current location
                Location particleLocation = playerLocation.clone().add(
                    (Math.random() - 0.5) * offsetX,
                    offsetY + (Math.random() - 0.5) * 0.5,
                    (Math.random() - 0.5) * offsetZ
                );
                
                // Spawn particles based on type
                if (particleType == Particle.REDSTONE) {
                    // Handle colored particles
                    Color color = Color.fromRGB(
                        configManager.getParticleColorRed(),
                        configManager.getParticleColorGreen(),
                        configManager.getParticleColorBlue()
                    );
                    player.getWorld().spawnParticle(
                        particleType,
                        particleLocation,
                        count,
                        offsetX, offsetY, offsetZ,
                        new Particle.DustOptions(color, 1.0f)
                    );
                } else {
                    // Standard particles
                    player.getWorld().spawnParticle(
                        particleType,
                        particleLocation,
                        count,
                        offsetX, offsetY, offsetZ,
                        speed
                    );
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L); // Run every 2 ticks (10 times per second)
    }
    
    /**
     * Gets the particle type from string, with fallback to FLAME
     * @param particleTypeStr The particle type string
     * @return The particle type
     */
    private Particle getParticleType(String particleTypeStr) {
        try {
            return Particle.valueOf(particleTypeStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type in config: " + particleTypeStr + ". Using FLAME instead.");
            return Particle.FLAME;
        }
    }
} 