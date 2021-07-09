package net.mcatlas.weather.model;

import net.mcatlas.weather.WeatherPlugin;

public class Coordinate {
    private double x; // long
    private double y; // lat

    public Coordinate(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    // returns real life coordinate!
    public static Coordinate getLifeFromMC(int mcX, int mcY) {
        double x = (mcX / WeatherPlugin.get().scaling());
        double y = (mcY / WeatherPlugin.get().scaling()) * -1;
        return new Coordinate(x, y);
    }

    public static Coordinate getMCFromLife(double lat, double lon) {
        double x = (lon * WeatherPlugin.get().scaling());
        double z = (lat * WeatherPlugin.get().scaling()) * -1;
        return new Coordinate(x, z);
    }

}
