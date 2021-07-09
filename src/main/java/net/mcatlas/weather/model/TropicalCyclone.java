package net.mcatlas.weather.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class TropicalCyclone {

    private String name;
    private Location location;
    private Coordinate irlLocation;
    private String direction;
    private double directionSpeed;
    private double windsMph;
    private double pressure;
    private String dateLastUpdated;
    private Coordinate[] cone;
    private Forecast[] forecasts;

    public TropicalCyclone(String name, double lat, double lon, String direction, double speed,
                           double windSpeed, double pressure, String date, Coordinate[] cone, Forecast[] forecasts) {
        this.name = name;
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
}
