package net.mcatlas.weather.model;

import net.mcatlas.weather.WeatherPlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

import static net.mcatlas.weather.WeatherUtil.*;

public class TropicalCyclone {

    private String name;
    private String shortName;
    private Location location;
    private Coordinate irlLocation;
    private String direction;
    private double directionSpeed;
    private double windsMph;
    private double pressure;
    private String dateLastUpdated;
    private Coordinate[] cone;
    private Forecast[] forecasts;

    private boolean cancelled = false;

    public static final Particle.DustOptions DUST_OPTIONS =
            new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 128, 128), 10);

    public TropicalCyclone(String name, String shortName, double lat, double lon, String direction, double speed,
                           double windSpeed, double pressure, String date, Coordinate[] cone, Forecast[] forecasts) {
        this.name = name;
        this.shortName = shortName;
        this.irlLocation = new Coordinate(lat, lon);
        Coordinate ingame = Coordinate.getMCFromLife(lat, lon);
        this.location = new Location(Bukkit.getWorlds().get(0), ingame.getX(), 64, ingame.getY());
        this.direction = direction;
        this.directionSpeed = speed;
        this.windsMph = windSpeed;
        this.pressure = pressure;
        this.dateLastUpdated = date;
        this.cone = cone;
        this.forecasts = forecasts;
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

                Collection<Player> nearbyPlayers = Bukkit.getWorlds().get(0).getNearbyPlayers(location, 150);

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
                    for (double height = .5 + randomHeight; height < 20; height += Math.sqrt(height) / 2) {
                        double width = 3.75;
                        double radian = RANDOM.nextDouble() * (Math.PI * 2);
                        // outsideOfEye makes the particles go a little out of the eye but still stay near the eye
                        int outsideOfEye = (int) (RANDOM.nextDouble() * 31.62); // sqrt(1000)
                        outsideOfEye *= outsideOfEye;
                        outsideOfEye = 1000 / (outsideOfEye + 1);
                        Location spawnParticle = location.clone().add(Math.sin(radian) * width * outsideOfEye, height,  Math.cos(radian) * width * outsideOfEye);
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
