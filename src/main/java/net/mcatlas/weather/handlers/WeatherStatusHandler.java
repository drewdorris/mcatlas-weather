package net.mcatlas.weather.handlers;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.mcatlas.weather.WeatherPlugin;
import net.mcatlas.weather.WeatherUtil;
import net.mcatlas.weather.model.Coordinate;
import net.mcatlas.weather.model.WeatherData;
import net.mcatlas.weather.model.WeatherPlayer;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.Color;
import java.time.Duration;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class WeatherStatusHandler {

    private WeatherPlugin plugin;

    private Set<WeatherPlayer> allWeatherPlayers;
    private PriorityQueue<WeatherPlayer> playerQueue;

    public WeatherStatusHandler(WeatherPlugin plugin) {
        this.plugin = plugin;
        playerQueue = new PriorityQueue<>();
        allWeatherPlayers = new HashSet<>();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            addPlayerToQueue(player, WeatherPriority.ONLINE);
        }

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            handleWeatherCycle();
        }, 20L, 60L); // pull a new player to set weather every 3 seconds
        // speed of setting weather individually depends on how many people are on

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            checkPlayerLocations();
        }, 20 * 30L, 20 * 20L);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            sendPlayerWeatherMessages();
        }, 20 * 20L, 20 * 30L);
    }

    public void checkPlayerLocations() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }

            for (Player player : world.getPlayers()) {
                if (System.currentTimeMillis() - player.getLastLogin() > Duration.ofSeconds(60).toMillis()) {
                    WeatherPlayer weatherPlayer = getWeatherPlayer(player.getUniqueId());
                    if (weatherPlayer == null) {
                        addPlayerToQueue(player, WeatherPriority.JOIN);
                        continue;
                    }
                    Location prevLocation = weatherPlayer.getLastLocation();
                    if (prevLocation == null) {
                        addPlayerToQueue(player, WeatherPriority.JOIN);
                        continue;
                    }
                    Location currentLocation = player.getLocation();
                    currentLocation.setY(64);
                    if (prevLocation.distance(currentLocation) > 250) {
                        addPlayerToQueue(player, WeatherPriority.MOVE);
                    }
                }
            }
        }
    }

    public void sendPlayerWeatherMessages() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }

            for (Player player : world.getPlayers()) {
                WeatherPlayer weatherPlayer = getWeatherPlayer(player.getUniqueId());
                if (weatherPlayer == null) continue;

                WeatherData weatherData = weatherPlayer.getWeatherData();
                if (weatherData == null) continue;

                int fahrenheit = (int) WeatherUtil.kelvinToFahrenheit(weatherData.getTemperature());
                int celsius = (int) WeatherUtil.kelvinToCelsius(weatherData.getTemperature());
                Color color = WeatherUtil.getColorFromTemperature(fahrenheit);
                TextColor tempColor = TextColor.color(color.getRed(), color.getGreen(), color.getBlue());
                String temperature = tempColor + "" + fahrenheit + "F" + ChatColor.GRAY + "/" + tempColor + "" + celsius + "C";

                net.kyori.adventure.text.Component text = net.kyori.adventure.text.Component.text(fahrenheit + "F").color(tempColor)
                        .append(net.kyori.adventure.text.Component.text("/").color(NamedTextColor.GRAY)
                                .append(net.kyori.adventure.text.Component.text(celsius + "C").color(tempColor)
                                        .append(Component.text(" - " + (int) weatherData.getWindSpeed() +
                                                "mph Wind" + " - " + weatherData.getWeatherFullDesc())
                                                .color(NamedTextColor.GRAY)
                                        )));
                for (int tick = 0; tick <= 25; tick += 5) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.sendActionBar(text);
                    }, tick);
                }
            }
        }
    }

    // Gets the new player to set weather for and handles it for them until they get their weather updated again
    public void handleWeatherCycle() {
        WeatherPlayer weatherPlayer = null;
        Player player = null;
        while (true) {
            weatherPlayer = playerQueue.poll();
            if (weatherPlayer == null || weatherPlayer.getUUID() == null || Bukkit.getPlayer(weatherPlayer.getUUID()) == null) {
                if (playerQueue.isEmpty()) return;
                continue;
            }
            player = Bukkit.getPlayer(weatherPlayer.getUUID());
            if (player == null) continue;
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) continue;

            break;
        }

        weatherPlayer.update();

        addPlayerToQueue(player, WeatherPriority.ONLINE);

        Player thePlayer = player;

        // async!!!
        CompletableFuture.runAsync(() -> {
            setWeatherStatus(thePlayer);
        });
    }

    // RUN ASYNC or lag
    // actually does the work of setting weather characteristics for the player
    public void setWeatherStatus(Player player) {
        Location location = player.getLocation();

        Coordinate coord = Coordinate.getLifeFromMC(location.getBlockX(), location.getBlockZ());

        String urlString = "http://api.openweathermap.org/data/2.5/weather?lat="
                + coord.getY() + "&lon=" + coord.getX() + "&appid="
                + WeatherPlugin.get().getAPIKey();

        JsonElement data = plugin.getJsonHandler().getJsonFromURL(urlString);
        WeatherData weatherData = plugin.getJsonHandler().extractWeatherData(data, location);
        WeatherPlayer weatherPlayer = getWeatherPlayer(player.getUniqueId());

        weatherPlayer.setWeatherData(weatherData);

        Condition condition = Condition.CLEAR;
        if (weatherData != null) {
            condition = getCondition(weatherData.getWeatherDesc());
        }

        final long lastUpdated = weatherPlayer.getLastUpdated();

        switch (condition) {
            case CLEAR: {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.setPlayerWeather(WeatherType.CLEAR);
                });
                break;
            }
            case STORM: // maybe one day add thunderstorm-per-user implementation? idk
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.setPlayerWeather(WeatherType.DOWNFALL);
                });

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) {
                            this.cancel();
                            return;
                        }

                        if (lastUpdated != weatherPlayer.getLastUpdated()) {
                            this.cancel();
                            return;
                        }

                        if (WeatherUtil.RANDOM.nextInt(4) < 3) {
                            return;
                        }

                        double radian = WeatherUtil.RANDOM.nextDouble() * (Math.PI * 2);

                        player.playSound(player.getLocation().clone().add(Math.sin(radian) * 25, 0, Math.cos(radian) * 25),
                                Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 2F, .9F);
                    }
                }.runTaskTimer(plugin, 0L, 5 * 20L);
                break;
            case SAND: {
                BlockData sandDustData = Material.SAND.createBlockData();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) {
                            this.cancel();
                            return;
                        }

                        if (lastUpdated != weatherPlayer.getLastUpdated()) {
                            this.cancel();
                            return;
                        }

                        player.spawnParticle(Particle.FALLING_DUST,
                                location.clone().add((WeatherUtil.RANDOM.nextDouble() - .5) * 30, (WeatherUtil.RANDOM.nextDouble() - .25) * 10, (WeatherUtil.RANDOM.nextDouble() - .5) * 30),
                                1, sandDustData);
                    }
                }.runTaskTimer(plugin, 0L, 1);
                break;
            }
            case SMOKE: {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) {
                            this.cancel();
                            return;
                        }

                        if (lastUpdated != weatherPlayer.getLastUpdated()) {
                            this.cancel();
                            return;
                        }

                        Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 128, 128), 2);

                        player.spawnParticle(Particle.SMOKE_LARGE,
                                location.clone().add((WeatherUtil.RANDOM.nextDouble() - .5) * 30, (WeatherUtil.RANDOM.nextDouble() - .25) * 10, (WeatherUtil.RANDOM.nextDouble() - .5) * 30),
                                0);
                        player.spawnParticle(Particle.SMOKE_NORMAL,
                                location.clone().add((WeatherUtil.RANDOM.nextDouble() - .5) * 30, (WeatherUtil.RANDOM.nextDouble() - .25) * 10, (WeatherUtil.RANDOM.nextDouble() - .5) * 30),
                                0);
                        player.spawnParticle(Particle.REDSTONE,
                                location.clone().add((WeatherUtil.RANDOM.nextDouble() - .5) * 30, (WeatherUtil.RANDOM.nextDouble() - .25) * 10, (WeatherUtil.RANDOM.nextDouble() - .5) * 30),
                                1, dustOptions);
                    }
                }.runTaskTimer(plugin, 0L, 3);
                break;
            }
            case MIST: {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) {
                            this.cancel();
                            return;
                        }

                        if (lastUpdated != weatherPlayer.getLastUpdated()) {
                            this.cancel();
                            return;
                        }

                        Particle.DustOptions lightBlue = new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 200, 255), 1);
                        Particle.DustOptions gray = new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 128, 128), 2);

                        player.spawnParticle(Particle.REDSTONE,
                                location.clone().add((WeatherUtil.RANDOM.nextDouble() - .5) * 30,
                                        (WeatherUtil.RANDOM.nextDouble() - .25) * 10,
                                        (WeatherUtil.RANDOM.nextDouble() - .5) * 30),
                                1, gray);
                        player.spawnParticle(Particle.REDSTONE,
                                location.clone().add((WeatherUtil.RANDOM.nextDouble() - .5) * 30,
                                        (WeatherUtil.RANDOM.nextDouble() - .25) * 10,
                                        (WeatherUtil.RANDOM.nextDouble() - .5) * 30),
                                1, lightBlue);
                    }
                }.runTaskTimer(plugin, 0L, 4);
                break;
            }
            case RAIN: {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.setPlayerWeather(WeatherType.DOWNFALL);
                });
                break;
            }
        }

        if (weatherData.getWindSpeed() > 10 || condition == Condition.STORM ||
                condition == Condition.SAND || condition == Condition.SMOKE) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cancel();
                        return;
                    }

                    if (lastUpdated != weatherPlayer.getLastUpdated()) {
                        cancel();
                        return;
                    }

                    player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, SoundCategory.WEATHER,
                            (float) (0.15F + ((weatherData.getWindSpeed() - 10) / 80)), (float) 0.8);
                }
            }.runTaskTimer(plugin, 10L, 8 * 20L);
        }
    }

    // adds player to the queue for their weather to be updated
    public void addPlayerToQueue(Player player, WeatherPriority priority) {
        WeatherPlayer weatherPlayer = null;
        for (WeatherPlayer otherPlayer : allWeatherPlayers) {
            if (otherPlayer.getUUID().equals(player.getUniqueId())) {
                weatherPlayer = otherPlayer;
                weatherPlayer.setScore(priority.score);
                break;
            }
        }

        if (player == null) {
            return;
        }

        if (weatherPlayer == null) {
            weatherPlayer = new WeatherPlayer(player.getUniqueId(), priority.score, player.getLocation());
            allWeatherPlayers.add(weatherPlayer);
        }

        Location location = player.getLocation();
        location.setY(64);
        weatherPlayer.setLastLocation(location);

        playerQueue.remove(weatherPlayer);
        playerQueue.add(weatherPlayer);
    }

    @NotNull
    public Condition getCondition(String weatherDesc) {
        switch (weatherDesc) {
            case "Drizzle":
            case "Snow":
            case "Rain":
                return Condition.RAIN;
            case "Squall":
            case "Thunderstorm":
            case "Tornado":
                return Condition.STORM;
            case "Haze":
            case "Fog":
            case "Mist":
                return Condition.MIST;
            case "Sand":
                return Condition.SAND;
            case "Smoke":
            case "Dust":
            case "Ash":
                return Condition.SMOKE;
            case "Clear":
            case "Clouds":
            default:
                return Condition.CLEAR;
        }
    }

    @Nullable
    public WeatherPlayer getWeatherPlayer(UUID uuid) {
        for (WeatherPlayer weatherPlayer : this.allWeatherPlayers) {
            if (weatherPlayer.getUUID().equals(uuid)) {
                return weatherPlayer;
            }
        }
        return null;
    }

    public enum Condition {
        CLEAR,
        RAIN,
        MIST,
        SAND,
        SMOKE,
        STORM;
    }

    public enum WeatherPriority {
        JOIN(0),
        MOVE(1),
        ONLINE(2);

        private int score;

        WeatherPriority(int score) {
            this.score = score;
        }
    }

}
