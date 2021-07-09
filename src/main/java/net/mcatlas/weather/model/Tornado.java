package net.mcatlas.weather.model;

import net.mcatlas.weather.WeatherPlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static net.mcatlas.weather.WeatherUtil.*;

public class Tornado {

    private Location location;
    private String area;
    private Set<UUID> playersAlreadyReceivedNamedItem;
    private int radiusWarning; //unused currently
    private boolean cancelled = false;

    public static final Particle.DustOptions DUST_OPTIONS =
            new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 128, 128), 7);

    public Tornado(Location location, String area) {
        this.location = location;
        this.area = area;
        this.playersAlreadyReceivedNamedItem = new HashSet<>();
        this.radiusWarning = 80;
    }

    public Location getLocation() {
        return this.location;
    }

    public String getArea() {
        return this.area;
    }

    public String getShortenedArea() {
        int firstIndex = getArea().indexOf(";");
        if (firstIndex == -1) firstIndex = getArea().length();
        return getArea().substring(0, firstIndex);
    }

    public boolean playerHasAlreadyReceivedNamedItem(UUID uuid) {
        return this.playersAlreadyReceivedNamedItem.contains(uuid);
    }

    public void addPlayerReceivedNamedItem(UUID uuid) {
        this.playersAlreadyReceivedNamedItem.add(uuid);
    }

    public void update(Tornado other) {
        Location newLocation = other.getLocation().clone();
        this.area = other.getArea();
        double xDiff = newLocation.getX() - location.getX();
        double zDiff = newLocation.getZ() - location.getZ();

        final int endMovingLocation = WeatherPlugin.get().getServer().getCurrentTick() + (4 * 100);
        new BukkitRunnable() {
            @Override
            public void run() {
                location = location.add(xDiff / 100, 0, zDiff / 100);
                if (endMovingLocation < WeatherPlugin.get().getServer().getCurrentTick() || cancelled) {
                    cancel();
                }
            }
        }.runTaskTimer(WeatherPlugin.get(), 0L, 4L);
    }

    public void spawn() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (cancelled) {
                    cancel();
                }

                Bukkit.getScheduler().runTask(WeatherPlugin.get(), () -> { // just to be safe
                    location.setY(getHighestSolidBlockYAt(location) + 1.5);
                });

                Collection<Player> nearbyPlayers = Bukkit.getWorlds().get(0).getNearbyPlayers(location, 150);

                for (Player player : nearbyPlayers) {
                    Bukkit.getScheduler().runTaskAsynchronously(WeatherPlugin.get(), () -> {
                        player.spawnParticle(Particle.EXPLOSION_NORMAL, location.clone().add((RANDOM.nextDouble() - .5) * 3, 0, (RANDOM.nextDouble() - .5) * 4), 1);
                        player.spawnParticle(Particle.EXPLOSION_NORMAL, location.clone().add((RANDOM.nextDouble() - .5) * 3, 0, (RANDOM.nextDouble() - .5) * 4), 1);
                        player.spawnParticle(Particle.EXPLOSION_LARGE, location.clone().add((RANDOM.nextDouble() - .5) * 3, 0, (RANDOM.nextDouble() - .5) * 4), 1);
                    });
                }

                for (double height = .75; height < 20; height += .9) {
                    double width = 1.25 + ((height * height) / 40);
                    double radian = RANDOM.nextDouble() * (Math.PI * 2);
                    Location spawnParticle = location.clone().add(Math.sin(radian) * width, height,  Math.cos(radian) * width);
                    for (Player player : nearbyPlayers) {
                        Bukkit.getScheduler().runTaskAsynchronously(WeatherPlugin.get(), () -> {
                            if (chance(2)) {
                                player.playSound(spawnParticle.clone(), Sound.ITEM_ELYTRA_FLYING, SoundCategory.WEATHER, 1.5F, 0.5F);
                            }
                            player.spawnParticle(Particle.REDSTONE, spawnParticle, 1, DUST_OPTIONS);
                        });
                    }
                }
            }
        }.runTaskTimer(WeatherPlugin.get(), 10L, 2L);
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean similar(Tornado tornado) {
        if (getLocation().equals(tornado.getLocation())) {
            return true;
        }
        if (tornado.getLocation() == null) return false;

        Location orig = getLocation().clone();
        orig.setY(64);
        Location newLoc = tornado.getLocation().clone();
        newLoc.setY(64);

        return orig.distance(newLoc) < 10;
    }

}
