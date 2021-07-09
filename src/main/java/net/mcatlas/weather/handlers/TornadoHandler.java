package net.mcatlas.weather.handlers;

import com.google.gson.JsonElement;
import net.mcatlas.weather.WeatherPlugin;
import net.mcatlas.weather.model.Tornado;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.mcatlas.weather.WeatherUtil.RANDOM;
import static net.mcatlas.weather.WeatherUtil.chance;

public class TornadoHandler {

    private WeatherPlugin plugin;

    private BossBar tornadoBossBar;
    private int minutesBetweenTornadoAlerts;

    private Set<Tornado> tornadoes;

    public TornadoHandler(WeatherPlugin plugin, int minutesBetweenTornadoAlerts) {
        this.plugin = plugin;
        this.minutesBetweenTornadoAlerts = minutesBetweenTornadoAlerts;
        this.tornadoes = new HashSet<>();

        tornadoBossBar = Bukkit.createBossBar(ChatColor.WHITE + "" + ChatColor.BOLD + "Tornado Warning", BarColor.RED, BarStyle.SOLID, BarFlag.DARKEN_SKY);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tornadoBossBar.setTitle(ChatColor.WHITE + "" + ChatColor.BOLD + "Tornado Warning");
        }, 0L, 40L);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tornadoBossBar.setTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Tornado Warning");
        }, 20L, 40L);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            launchEntitiesInTornado();
        }, 10 * 20L, 20L);
    }

    public int getMinutesBetweenTornadoAlerts() {
        return minutesBetweenTornadoAlerts;
    }

    public void launchEntitiesInTornado() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                removeFromBossBar(player);
                continue;
            }

            Location location = player.getLocation();
            int x = location.getBlockX();
            int z = location.getBlockZ();
            if (x > 0 || z > 0) {
                removeFromBossBar(player);
                continue; // southern hemisphere
            }
            // now only players in north and west hemisphere. USA general area
            int y = location.getWorld().getHighestBlockYAt(location);
            int playerY = location.getBlockY();

            boolean inBossBarZone = false;

            for (Tornado tornado : tornadoes) {
                Location tornadoLoc = tornado.getLocation();

                tornadoLoc.setY(y);
                location.setY(y);

                double dist = location.distance(tornadoLoc);
                if (dist < 0.1) dist = .1; // if its 0 or near 0 it will be an issue for the vector
                if (dist < 5 && playerY - y < 20 && playerY - y > -5) { // 5 blocks from middle of tornado; 20 blocks above or 5 blocks below bottom of tornado
                    inBossBarZone = true;

                    // send them flying
                    org.bukkit.util.Vector dir = location.getDirection().multiply(2);
                    org.bukkit.util.Vector vector = new org.bukkit.util.Vector(dir.getX(), 20 + (1 * (5 / dist)), dir.getZ());
                    player.setVelocity(vector);

                    for (int i = 1; i < RANDOM.nextInt(20); i++) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.setVelocity(vector.rotateAroundY(Math.PI / 6).multiply(1.1));
                        }, i * 2L);
                    }

                    if (!tornado.playerHasAlreadyReceivedNamedItem(player.getUniqueId())) {
                        nameItemInHand(player, tornado);
                    }

                    // elytra randomly falls off entering tornado
                    if (chance(5)) {
                        ItemStack chestplate = player.getInventory().getChestplate();
                        if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
                            player.getInventory().setChestplate(null);
                            player.sendMessage(ChatColor.RED + "" + ChatColor.ITALIC + "The winds tore your elytra into pieces!");
                        }
                    }
                    // random parts of inventory get blown away when entering tornado
                    if (chance(25)) {
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
                } else if (dist < 30) { // if less than 30 blocks from tornado, move around entities that are near the tornado
                    inBossBarZone = true;
                    for (Entity entity : player.getNearbyEntities(30, 30, 30)) {
                        Location entityLocation = entity.getLocation();
                        double entDist = entityLocation.distance(tornadoLoc);
                        if (entDist < 7) {
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
                addToBossBar(player);
            } else {
                removeFromBossBar(player);
            }
        }
    }

    public void addToBossBar(Player player) {
        tornadoBossBar.addPlayer(player);
    }

    public void removeFromBossBar(Player player) {
        tornadoBossBar.removePlayer(player);
    }

    public void nameItemInHand(Player player, Tornado tornado) {
        ItemStack mainItem = player.getInventory().getItemInMainHand();
        if (mainItem.getType() == Material.AIR) {
            return;
        }

        if (mainItem.getAmount() > 1) {
            return;
        }

        ItemMeta meta = mainItem.getItemMeta();

        if (meta.hasLore()) {
            return;
        }

        String firstArea = tornado.getShortenedArea();
        String location = "From the tornado of " + firstArea;
        String time = LocalDate.now().toString().replace("-", "/");
        List<String> lore = new ArrayList<>();
        lore.add(location);
        lore.add(time);
        meta.setLore(lore);

        mainItem.setItemMeta(meta);
        tornado.addPlayerReceivedNamedItem(player.getUniqueId());
    }

    // ASYNC
    public void updateTornadoes() {
        plugin.getDynmapHandler().resetTornadoMarkers();
        JsonElement data = plugin.getJsonHandler().getJsonFromURL("https://api.weather.gov/alerts/active");
        Set<Tornado> updatedTornadoes = plugin.getJsonHandler().extractTornadoData(data);

        // this.tornadoes = updatedTornadoes.stream().filter(t -> this.tornadoes.contains(t)).collect(Collectors.toSet());
        Set<Tornado> deadTornadoes = new HashSet<>();
        Set<Tornado> newTornadoes = new HashSet<>(updatedTornadoes);
        for (Tornado tornado : this.tornadoes) {
            double minDist = 999999;
            Tornado newTornadoIsEqualToOld = null;
            for (Tornado updatedTornado : updatedTornadoes) {
                double distance = updatedTornado.getLocation().distance(tornado.getLocation());
                if (updatedTornado.similar(tornado) && distance < minDist) {
                    minDist = distance;
                    newTornadoIsEqualToOld = updatedTornado;
                }
            }
            if (newTornadoIsEqualToOld != null && minDist != 999999) {
                tornado.update(newTornadoIsEqualToOld);
                newTornadoes.remove(newTornadoIsEqualToOld);
                updatedTornadoes.remove(newTornadoIsEqualToOld);
            } else {
                plugin.getLogger().info(tornado.getArea() + " tornado dissipated");
                deadTornadoes.add(tornado);
            }
        }
        for (Tornado dead : deadTornadoes) {
            dead.cancel();
        }
        this.tornadoes.removeAll(deadTornadoes);
        this.tornadoes.addAll(newTornadoes);

        for (Tornado tornado : newTornadoes) {
            plugin.getLogger().info("Formed tornado at " + tornado.getLocation().getBlockX() + " " + tornado.getLocation().getBlockZ());
            tornado.spawn();
        }
        plugin.getLogger().info(tornadoes.size() + " tornadoes total");

        if (tornadoes.size() <= 0) return;

        /*
        Bukkit.getScheduler().runTask(this, () -> {
            for (Tornado tornado : this.tornadoes) {
                tornado.getLocation().getWorld().playSound(tornado.getLocation(), Sound.ENTITY_GHAST_SCREAM, SoundCategory.WEATHER, 10F, 0.65F);
            }
        });
         */

        String locations = "";
        for (Tornado tornado : this.tornadoes) {
            locations += tornado.getShortenedArea() + "; ";
        }
        locations = locations.substring(0, locations.length() - 2);
        Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD +
                "Tornado Warning in effect for: " + ChatColor.RESET + "" + ChatColor.RED + locations);

        plugin.getDynmapHandler().createTornadoMarkers(tornadoes);
        // TODO get alerts and alert things, create polygons, find towns in alert zones etc
    }

}
