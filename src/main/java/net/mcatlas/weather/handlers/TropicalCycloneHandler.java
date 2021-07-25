package net.mcatlas.weather.handlers;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.mcatlas.weather.WeatherPlugin;
import net.mcatlas.weather.model.Tornado;
import net.mcatlas.weather.model.TropicalCyclone;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
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
    private BossBar bossBar;

    private TropicalCycloneDataHandler dataHandler;

    private boolean loaded = false;

    public TropicalCycloneHandler(WeatherPlugin plugin) {
        this.plugin = plugin;
        this.cyclones = new HashSet<>();
        this.playersCurrentlyInCyclone = new HashSet<>();
        this.bossBar = Bukkit.createBossBar(ChatColor.WHITE + "" + ChatColor.BOLD + "Tropical Cyclone Warning", BarColor.RED, BarStyle.SOLID, BarFlag.DARKEN_SKY, BarFlag.CREATE_FOG);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            bossBar.setTitle(ChatColor.WHITE + "" + ChatColor.BOLD + "Tropical Cyclone Warning");
        }, 0L, 40L);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            bossBar.setTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Tropical Cyclone Warning");
        }, 20L, 40L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getScheduler().runTaskTimerAsynchronously(WeatherPlugin.get(), () -> {
                updateTropicalCyclones();
            }, 0L, 20 * 60 * 30L); // 30 sec

            Bukkit.getScheduler().runTaskTimer(WeatherPlugin.get(), () -> {
                launchEntitiesInCyclone();
            }, 0L, 21L); // every 21 ticks

            dataHandler = new TropicalCycloneDataHandler(plugin);
        }, 20 * 20L);
    }

    public boolean isLoaded() {
        return loaded;
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
        this.loaded = true;
    }

    public void launchEntitiesInCyclone() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                removeFromBossBar(player);
                continue;
            }

            Location location = player.getLocation();
            int y = location.getWorld().getHighestBlockYAt(location);
            int playerY = location.getBlockY();

            boolean inBossBarZone = false;

            for (TropicalCyclone cyclone : cyclones) {
                final Location cycloneLoc = cyclone.getLocation();

                cycloneLoc.setY(y);
                location.setY(y);

                double dist = location.distance(cycloneLoc);
                if (dist < 0.1) dist = .1; // if its 0 or near 0 it will be an issue for the vector
                // within 35 + (more depending on wind) blocks of eye, not within eye, less than y=200, not currently in cyclone
                if (!playersCurrentlyInCyclone.contains(player.getUniqueId()) &&
                        !isInEyeArea(cyclone, location, dist) &&
                        playerY < cyclone.getWindsHeight() &&
                        dist < cyclone.getWindsWidth() &&
                        !wearingProtectedArmor(player, cyclone.getWindsMph())) {
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
                            player.setVelocity(vector.rotateAroundY(Math.PI / (4 + Math.sqrt(newDist * 13))).multiply(1.06));
                        }, iter * 3L);
                    }
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> playersCurrentlyInCyclone.remove(player.getUniqueId()), iter);

                    if (isInCoreEye(cyclone, location, dist)) {
                        // give armor and stuff
                    }
                    //if (!tornado.playerHasAlreadyReceivedNamedItem(player.getUniqueId())) {
                        //nameItemInHand(player, tornado);
                    //}

                    // elytra randomly falls off entering tornado
                    if (cyclone.getWindsMph() > 110 && chance(1 + ((cyclone.getWindsMph() - 110) / 8))) {
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
                } else if (dist < 90) { // if less than 90 blocks from cyclone, move around entities that are near the cyclone
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
                } else if (dist < 140) {
                    inBossBarZone = true;
                }
            }
            if (inBossBarZone) {
                addToBossBar(player);
            } else {
                removeFromBossBar(player);
            }
        }
    }

    // within the eye from top to bottom
    // xyDistFromEye is distance ignoring y
    public boolean isInEyeArea(TropicalCyclone cyclone, Location player, double xyDistFromEye) {
        double yDiff = player.getY() - cyclone.getLocation().getY();

        if (yDiff < -10 || yDiff > 150) return false;

        if (xyDistFromEye > cyclone.getEyeRadius() + (.04 * yDiff)) return false;

        return true;
    }

    // surface level area of the eye
    public boolean isInCoreEye(TropicalCyclone cyclone, Location player, double xyDistFromEye) {
        double yDiff = player.getY() - cyclone.getLocation().getY();
        return isInEyeArea(cyclone, player, xyDistFromEye) && Math.abs(yDiff) < 10;
    }

    public boolean wearingProtectedArmor(Player player, double winds) {
        for (ItemStack )
    }

    public double getCycloneProtectionRating(ItemStack item) {
        switch (item.getType()) {
            case LEATHER_BOOTS:
            case LEATHER_CHESTPLATE:
            case LEATHER_HELMET:
            case LEATHER_LEGGINGS:
                break;
            default:
                return -1;
        }

        if (!item.getItemMeta().hasLore()) return -1;

        List<Component> lore = item.getItemMeta().lore();
        for (Component lorePiece : lore) {
            if (lorePiece == null || lorePiece.toString() == null) continue;

            String loreString = lorePiece.toString();
            if (loreString.contains("Rated for ")) {
                int indexStart = loreString.indexOf("Rated for ") + 10;
                String mph = loreString.substring(indexStart + loreString.indexOf(" ", indexStart));
                // untested
                System.out.println(mph);
                // parse double and return value
            }
        }
        return -1;
    }

    public void addToBossBar(Player player) {
        bossBar.addPlayer(player);
    }

    public void removeFromBossBar(Player player) {
        bossBar.removePlayer(player);
    }

}
