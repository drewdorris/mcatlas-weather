package net.mcatlas.weather.model;

import org.bukkit.Location;

import java.util.UUID;

public class WeatherPlayer implements Comparable<WeatherPlayer> {
    private UUID uuid;
    private int score;
    private WeatherData weatherData;
    private Location lastLocation;
    private long lastUpdated;

    public WeatherPlayer(UUID uuid, int score, Location location) {
        this.uuid = uuid;
        this.score = score;
        this.weatherData = null;
        this.lastLocation = location;
        this.lastUpdated = System.currentTimeMillis();
    }

    public WeatherPlayer(UUID uuid, int score, WeatherData weatherData, Location location) {
        this.uuid = uuid;
        this.score = score;
        this.weatherData = weatherData;
        this.lastLocation = location;
        this.lastUpdated = System.currentTimeMillis();
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public UUID getUUID() {
        return uuid;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void update() {
        this.lastUpdated = System.currentTimeMillis();
    }

    public WeatherData getWeatherData() {
        return weatherData;
    }

    public void setWeatherData(WeatherData data) {
        this.weatherData = data;
    }

    public Location getLastLocation() {
        return this.lastLocation;
    }

    public void setLastLocation(Location location) {
        this.lastLocation = location;
    }

    @Override
    public int compareTo(WeatherPlayer o) {
        return Integer.compare(score, o.getScore());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UUID) {
            return obj.equals(getUUID());
        } else if (obj instanceof WeatherPlayer) {
            WeatherPlayer weatherObj = (WeatherPlayer) obj;
            return weatherObj.getUUID().equals(getUUID());
        }
        return false;
    }
}