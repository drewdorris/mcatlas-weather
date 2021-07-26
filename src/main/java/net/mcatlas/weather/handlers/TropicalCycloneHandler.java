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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.util.Vector;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static net.mcatlas.weather.WeatherUtil.RANDOM;
import static net.mcatlas.weather.WeatherUtil.chance;

public class TropicalCycloneHandler {

    private WeatherPlugin plugin;

    private Set<TropicalCyclone> cyclones;
    private Set<UUID> playersCurrentlyInCyclone;
    private Map<BossBar, String> bossBars;

    private TropicalCycloneDataHandler dataHandler;

    private boolean loaded = false;

    public TropicalCycloneHandler(WeatherPlugin plugin) {
        this.plugin = plugin;
        this.cyclones = new HashSet<>();
        this.playersCurrentlyInCyclone = new HashSet<>();
        this.bossBars = new HashMap<>();
        //this.bossBar = Bukkit.createBossBar(ChatColor.WHITE + "" + ChatColor.BOLD + "Tropical Cyclone Warning", BarColor.RED, BarStyle.SOLID, BarFlag.DARKEN_SKY);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<BossBar, String> entry : bossBars.entrySet()) {
                entry.getKey().setTitle(ChatColor.WHITE + "" + ChatColor.BOLD + entry.getValue());
            }
        }, 0L, 80L);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<BossBar, String> entry : bossBars.entrySet()) {
                entry.getKey().setTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Tropical Cyclone Warning");
            }
        }, 40L, 80L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getScheduler().runTaskTimerAsynchronously(WeatherPlugin.get(), () -> {
                updateTropicalCyclones();
                dataHandler.refreshPlayers(cyclones);
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
        //JsonElement data = plugin.getJsonHandler().getJsonFromURL("https://api.weatherusa.net/v1.2/tropical?storm=active");
        JsonElement data = plugin.getJsonHandler().getJsonFromLocal("plugins/mcatlas-weather/tropicalstorms2.json");
        this.cyclones = plugin.getJsonHandler().extractTropicalCycloneData(data);
        plugin.getDynmapHandler().createTropicalCycloneMarkers(cyclones);

        for (BossBar bossBar : bossBars.keySet()) {
            bossBar.removeAll();
        }
        this.bossBars.clear();
        for (TropicalCyclone cyclone : cyclones) {
            cyclone.spawn();
            BossBar bar = Bukkit.createBossBar(ChatColor.WHITE + "" + ChatColor.BOLD + cyclone.getName(), BarColor.RED, BarStyle.SOLID, BarFlag.DARKEN_SKY);
            String title = cyclone.getName();
            this.bossBars.put(bar, title);
        }
        plugin.getLogger().info(cyclones.size() + " cyclones total");
        this.loaded = true;
    }

    public void launchEntitiesInCyclone() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                removeFromBossBars(player);
                continue;
            }

            if (player.getGameMode() == GameMode.SPECTATOR) continue;

            Location location = player.getLocation();
            int y = location.getWorld().getHighestBlockYAt(location);
            int playerY = location.getBlockY();

            TropicalCyclone bossBarCyclone = null;

            for (TropicalCyclone cyclone : cyclones) {
                final Location cycloneLoc = cyclone.getLocation();

                cycloneLoc.setY(y);
                location.setY(y);

                double dist = location.distance(cycloneLoc);
                if (dist < 0.1) dist = .1; // if its 0 or near 0 it will be an issue for the vector

                if (dist < 150) {
                    bossBarCyclone = cyclone;
                }

                if (isInCoreEye(cyclone, location, dist) && !cyclone.hasReceivedReward(player.getUniqueId())) {
                    giveProtectedArmor(player, cyclone);
                    break;
                }

                // within 35 + (more depending on wind) blocks of eye, not within eye, less than y=200, not currently in cyclone
                if (!playersCurrentlyInCyclone.contains(player.getUniqueId()) &&
                        !isInEyeArea(cyclone, location, dist) &&
                        playerY < cyclone.getWindsHeight() &&
                        dist < cyclone.getWindsWidth() &&
                        !wearingProtectedArmor(player, cyclone.getWindsMph())) {
                    // send them flying
                    org.bukkit.util.Vector dir = cycloneLoc.toVector().subtract(location.toVector()).normalize();//.multiply(30 / dist);
                    dir = dir.getCrossProduct(new Vector(0, 1, 0));
                    org.bukkit.util.Vector vector = new org.bukkit.util.Vector(dir.getX(), 10 / dist, dir.getZ());
                    
                    player.setVelocity(vector);
                    playersCurrentlyInCyclone.add(player.getUniqueId());

                    int iter;
                    for (iter = 1; iter < (7 * 3) + (RANDOM.nextInt(5) * 7); iter++) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (wearingProtectedArmor(player, cyclone.getWindsMph())) return;
                            Location currentPlayerLoc = player.getLocation();
                            currentPlayerLoc.setY(y);
                            double newDist = currentPlayerLoc.distance(cycloneLoc);
                            player.setVelocity(vector.rotateAroundY(Math.PI / (4 + Math.sqrt(newDist * 13))).multiply(1.06));
                        }, iter * 3L);
                    }
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> playersCurrentlyInCyclone.remove(player.getUniqueId()), iter);

                    // elytra randomly falls off entering cyclone
                    if (cyclone.getWindsMph() > 110 && chance(1 + ((cyclone.getWindsMph() - 110) / 8))) {
                        ItemStack chestplate = player.getInventory().getChestplate();
                        if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
                            player.getInventory().setChestplate(null);
                            player.sendMessage(ChatColor.RED + "" + ChatColor.ITALIC + "The winds tore your elytra into pieces!");
                        }
                    }
                    // random parts of inventory get blown away when entering cyclone
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
                }
            }
            if (bossBarCyclone != null) {
                addToBossBar(player, bossBarCyclone);
            } else {
                removeFromBossBars(player);
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

    // gives the reward to the player
    public void giveProtectedArmor(Player player, TropicalCyclone cyclone) {
        // randomly choose armor, put in inventory or drop at feet
        int chooseArmor = RANDOM.nextInt(4);
        Material armorType = Material.LEATHER_HELMET;
        switch (chooseArmor) {
            case 1:
                armorType = Material.LEATHER_CHESTPLATE;
                break;
            case 2:
                armorType = Material.LEATHER_LEGGINGS;
                break;
            case 3:
                armorType = Material.LEATHER_BOOTS;
                break;
        }

        ItemStack armor = new ItemStack(armorType, 1);
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();

        meta.setDisplayName(ChatColor.RED + cyclone.getName() + " - " + ChatColor.GRAY +  "Withstands " + (int) cyclone.getWindsMph() + "mph");
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);

        String stringRated = "Rated for " + (int) cyclone.getWindsMph() + "mph winds";
        String stringBy = "Created by " + player.getName() + " on " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        List<String> lore = Arrays.asList(stringRated, stringBy);
        meta.setLore(lore);

        int speed = (int) cyclone.getWindsMph();
        int redScale = Math.abs(225 - speed);
        meta.setColor(Color.fromRGB(255, redScale, redScale));

        if (meta instanceof Repairable) {
            ((Repairable) meta).setRepairCost(400);
        }

        if (!player.getInventory().addItem(armor).isEmpty()) {
            player.getWorld().dropItem(player.getLocation(), armor);
        }

        cyclone.addPlayerReceivedReward(player.getUniqueId());
        dataHandler.addPlayer(cyclone.getId(), player.getUniqueId());
    }

    public boolean wearingProtectedArmor(Player player, double winds) {
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null) continue;
            if (getCycloneProtectionRating(armor) >= winds) return true;
        }
        return false;
    }

    public double getCycloneProtectionRating(ItemStack item) {
        if (item == null) return -1;
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

        int highestRating = -1;
        List<Component> lore = item.getItemMeta().lore();
        for (Component lorePiece : lore) {
            if (lorePiece == null || lorePiece.toString() == null) continue;

            String loreString = lorePiece.toString();
            if (loreString.contains("Rated for ")) {
                int indexStart = loreString.indexOf("Rated for ") + 10;
                String mphStr = loreString.substring(indexStart, loreString.indexOf("mph", indexStart));
                double mph = Double.valueOf(mphStr);
                if (mph > highestRating) highestRating = (int) mph;
            }
        }
        return highestRating;
    }

    public void addToBossBar(Player player, TropicalCyclone cyclone) {
        for (Map.Entry<BossBar, String> entry : bossBars.entrySet()) {
            if (entry.getValue().equals(cyclone.getName())) {
                entry.getKey().addPlayer(player);
            } else {
                entry.getKey().removePlayer(player);
            }
        }
    }

    public void removeFromBossBars(Player player) {
        for (Map.Entry<BossBar, String> entry : bossBars.entrySet()) {
            entry.getKey().removePlayer(player);
        }
    }

}
