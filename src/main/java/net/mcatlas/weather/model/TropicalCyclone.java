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

public class TropicalCyclone {

    private String id; // EP062021
    private String name; // Hurricane Felicia
    private String shortName; // Felicia
    private Location location;
    private Coordinate irlLocation;
    private String direction; // W
    private double directionSpeed; // 8 mph
    private Category category; // Category 4
    private double windsMph; // 145mph
    private double pressure; // 947mb
    private String dateLastUpdated; // 2021-07-15 15:00
    private Coordinate[] cone; // cone coordinates
    private Forecast[] forecasts; // forecasted locations

    private Set<UUID> playersWhoReceivedReward;

    private boolean cancelled = false;

    public static final Particle.DustOptions DUST_OPTIONS =
            new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 128, 128), 10);

    public TropicalCyclone(String id, String name, String shortName, double lat, double lon,
                           String direction, double speed, Category category, double windSpeed,
                           double pressure, String date, Coordinate[] cone, Forecast[] forecasts) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.irlLocation = new Coordinate(lat, lon);
        Coordinate ingame = Coordinate.getMCFromLife(lat, lon);
        this.location = new Location(Bukkit.getWorlds().get(0), ingame.getX(), 64, ingame.getY());
        this.direction = direction;
        this.directionSpeed = speed;
        this.category = category;
        this.windsMph = windSpeed;
        this.pressure = pressure;
        this.dateLastUpdated = date;
        this.cone = cone;
        this.forecasts = forecasts;

        this.playersWhoReceivedReward = new HashSet<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public Location getLocation() {
        return location;
    }

    public Coordinate getIrlLocation() {
        return irlLocation;
    }

    public String getDirection() {
        return direction;
    }

    public double getDirectionSpeed() {
        return directionSpeed;
    }

    public Category getCategory() {
        return category;
    }

    public double getWindsMph() {
        return windsMph;
    }

    public double getPressure() {
        return pressure;
    }

    public String getDateLastUpdated() {
        return dateLastUpdated;
    }

    public Coordinate[] getCone() {
        return cone;
    }

    public Forecast[] getForecasts() {
        return forecasts;
    }

    public Set<UUID> getPlayersWhoReceivedReward() {
        return playersWhoReceivedReward;
    }

    public void addPlayerReceivedReward(UUID uuid) {
        playersWhoReceivedReward.add(uuid);
    }

    public void setPlayersWhoReceivedReward(Set<UUID> players) {
        this.playersWhoReceivedReward = players;
    }

    public boolean hasReceivedReward(UUID player) {
        return playersWhoReceivedReward.contains(player);
    }

    public double getEyeRadius() {
        return 6 - (.7 * category.power);
    }

    public double getWindsHeight() {
        return 95 + (getCategory().power * 7);
    }

    public double getWindsWidth() {
        return 35 + (getWindsMph() / 7);
    }

    public void cancel() {
        this.cancelled = true;
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

                Collection<Player> nearbyPlayers = Bukkit.getWorlds().get(0).getNearbyPlayers(location, 175 - (3 * category.power));

                /*
                for (Player player : nearbyPlayers) {
                    Bukkit.getScheduler().runTaskAsynchronously(WeatherPlugin.get(), () -> {
                        player.spawnParticle(Particle.EXPLOSION_NORMAL, location.clone().add((RANDOM.nextDouble() - .5) * 3, 0, (RANDOM.nextDouble() - .5) * 4), 1);
                        player.spawnParticle(Particle.EXPLOSION_NORMAL, location.clone().add((RANDOM.nextDouble() - .5) * 3, 0, (RANDOM.nextDouble() - .5) * 4), 1);
                        player.spawnParticle(Particle.EXPLOSION_LARGE, location.clone().add((RANDOM.nextDouble() - .5) * 3, 0, (RANDOM.nextDouble() - .5) * 4), 1);
                    });
                }
                 */

                for (int j = 0; j < 3; j++) {
                    double randomHeight = RANDOM.nextDouble() * 2;
                    for (double height = .5 + randomHeight; height < 20 + category.power; height += Math.sqrt(height) / 2) {
                        double radian = RANDOM.nextDouble() * (Math.PI * 2);
                        // outsideOfEye makes the particles go a little out of the eye but still stay near the eye
                        double scale = 32 - category.power;
                        int outsideOfEye = (int) (RANDOM.nextDouble() * scale); // sqrt(1000)
                        outsideOfEye *= outsideOfEye;
                        outsideOfEye = (int) (scale * scale) / (outsideOfEye + 1);
                        Location spawnParticle = location.clone().add(
                                Math.sin(radian) * getEyeRadius() * outsideOfEye,
                                height,
                                Math.cos(radian) * getEyeRadius() * outsideOfEye);
                        for (Player player : nearbyPlayers) {
                            Bukkit.getScheduler().runTaskAsynchronously(WeatherPlugin.get(), () -> {
                                if (chance(2)) {
                                    player.playSound(spawnParticle.clone(), Sound.ITEM_ELYTRA_FLYING, SoundCategory.WEATHER, 1.5F, 0.5F);
                                }
                                player.spawnParticle(Particle.EXPLOSION_NORMAL, spawnParticle, 0, Math.sin(radian + (Math.PI / 2)), 0,  Math.cos(radian + (Math.PI / 2)));
                                player.spawnParticle(Particle.REDSTONE, spawnParticle, 1, DUST_OPTIONS);
                            });
                        }
                    }
                }
            }
        }.runTaskTimer(WeatherPlugin.get(), 10L, 3L);
    }

}
