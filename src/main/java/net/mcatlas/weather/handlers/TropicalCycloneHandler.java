package net.mcatlas.weather.handlers;

import com.google.gson.JsonElement;
import net.mcatlas.weather.WeatherPlugin;
import net.mcatlas.weather.model.Tornado;
import net.mcatlas.weather.model.TropicalCyclone;
import org.bukkit.*;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

import static net.mcatlas.weather.WeatherUtil.RANDOM;
import static net.mcatlas.weather.WeatherUtil.chance;

public class TropicalCycloneHandler {

    private WeatherPlugin plugin;

    private Set<TropicalCyclone> cyclones;

    private Set<UUID> playersCurrentlyInCyclone;

    private Set<BossBar> bossBars;

    public TropicalCycloneHandler(WeatherPlugin plugin) {
        this.plugin = plugin;
        this.cyclones = new HashSet<>();
        this.playersCurrentlyInCyclone = new HashSet<>();
        this.bossBars = new HashSet<>();

        Bukkit.getScheduler().runTaskTimerAsynchronously(WeatherPlugin.get(), () -> {
            updateTropicalCyclones();
        }, 20 * 20L, 20 * 60 * 30);

        Bukkit.getScheduler().runTaskTimer(WeatherPlugin.get(), () -> {
            launchEntitiesInCyclone();
        }, 10 * 20L, 21L);
    }

    public Set<TropicalCyclone> getCyclones() {
        return this.cyclones;
    }

    public void updateTropicalCyclones() {
        for (TropicalCyclone cyclone : cyclones) {
            cyclone.cancel();
        }
        plugin.getDynmapHandler().resetTropicalCycloneMarkers();
        JsonElement data = plugin.getJsonHandler().getJsonFromURL("https://api.weatherusa.net/v1.2/tropical?storm=active");
        this.cyclones = plugin.getJsonHandler().extractTropicalCycloneData(data);
        plugin.getDynmapHandler().createTropicalCycloneMarkers(cyclones);

        for (TropicalCyclone cyclone : cyclones) {
            cyclone.spawn();
        }
        plugin.getLogger().info(cyclones.size() + " cyclones total");
    }

    public void launchEntitiesInCyclone() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                //removeFromBossBar(player);
                continue;
            }

            Location location = player.getLocation();
            int x = location.getBlockX();
            int z = location.getBlockZ();
            if (x > 0 || z > 0) {
                //removeFromBossBar(player);
                continue; // southern hemisphere
            }
            // now only players in north and west hemisphere. USA general area
            int y = location.getWorld().getHighestBlockYAt(location);
            int playerY = location.getBlockY();

            boolean inBossBarZone = false;

            for (TropicalCyclone cyclone : cyclones) {
                final Location cycloneLoc = cyclone.getLocation();

                cycloneLoc.setY(y);
                location.setY(y);

                double dist = location.distance(cycloneLoc);
                if (dist < 0.1) dist = .1; // if its 0 or near 0 it will be an issue for the vector
                // within 50 blocks of eye, not within eye, less than y=200, not currently in cyclone
                if (dist < 35 + (cyclone.getWindsMph() / 8) && dist > cyclone.getEyeRadius() && playerY < 140 + (cyclone.getCategory().power * 6) &&
                        !playersCurrentlyInCyclone.contains(player.getUniqueId())) {
                    inBossBarZone = true;

                    // send them flying
                    org.bukkit.util.Vector dir = cycloneLoc.toVector().subtract(location.toVector()).normalize();//.multiply(30 / dist);
                    dir = dir.getCrossProduct(new Vector(0, 1, 0));
                    org.bukkit.util.Vector vector = new org.bukkit.util.Vector(dir.getX(), 10 / dist, dir.getZ());
                    
                    player.setVelocity(vector);
                    playersCurrentlyInCyclone.add(player.getUniqueId());

                    int iter;
                    for (iter = 1; iter < (7 * 3) + (RANDOM.nextInt(5) * 7); iter++) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            Location currentPlayerLoc = player.getLocation();
                            currentPlayerLoc.setY(y);
                            double newDist = currentPlayerLoc.distance(cycloneLoc);
                            player.setVelocity(vector.rotateAroundY(Math.PI / (4 + Math.sqrt(newDist * (13 - (cyclone.getCategory().power * 2))))).multiply(1.06));
                        }, iter * 3L);
                    }
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> playersCurrentlyInCyclone.remove(player.getUniqueId()), iter);

                    //if (!tornado.playerHasAlreadyReceivedNamedItem(player.getUniqueId())) {
                        //nameItemInHand(player, tornado);
                    //}

                    // elytra randomly falls off entering tornado
                    if (chance(1)) {
                        ItemStack chestplate = player.getInventory().getChestplate();
                        if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
                            player.getInventory().setChestplate(null);
                            player.sendMessage(ChatColor.RED + "" + ChatColor.ITALIC + "The winds tore your elytra into pieces!");
                        }
                    }
                    // random parts of inventory get blown away when entering tornado
                    if (chance(15)) {
                        int invSize = player.getInventory().getSize();
                        boolean someBlownAway = false;
                        for (int i = 0; i < RANDOM.nextInt(5); i++) {
                            int slot = RANDOM.nextInt(invSize);
                            ItemStack stack = player.getInventory().getItem(slot);
                            player.getInventory().setItem(slot, null);
                            if (stack != null) {
                                someBlownAway = true;
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    Item item = player.getWorld().dropItem(player.getLocation(), stack);
                                    item.setVelocity(player.getVelocity());
                                }, 10L * RANDOM.nextInt(4));
                            }
                        }
                        if (someBlownAway) {
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                player.sendMessage(ChatColor.RED + "" + ChatColor.ITALIC + "The winds blew away some of your inventory!");
                            }, 20L * 2);
                        }
                    }
                } else if (dist < 50) { // if less than 30 blocks from tornado, move around entities that are near the tornado
                    inBossBarZone = true;
                    for (Entity entity : player.getNearbyEntities(50, 50, 50)) {
                        Location entityLocation = entity.getLocation();
                        double entDist = entityLocation.distance(cycloneLoc);
                        if (entDist < 25) {
                            org.bukkit.util.Vector dir = entity.getLocation().getDirection().multiply(2);
                            org.bukkit.util.Vector vector = new org.bukkit.util.Vector(dir.getX(), 5 + (1 * (5 / entDist)), dir.getZ());
                            vector.normalize().multiply(2);
                            for (int i = 1; i < RANDOM.nextInt(20); i++) {
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    entity.setVelocity(vector.rotateAroundY(Math.PI / 6).multiply(1.1));
                                }, i * 2L);
                            }
                        }
                    }
                } else if (dist < 50) {
                    inBossBarZone = true;
                }
            }
            if (inBossBarZone) {
                //addToBossBar(player);
            } else {
                //removeFromBossBar(player);
            }
        }
    }

}
