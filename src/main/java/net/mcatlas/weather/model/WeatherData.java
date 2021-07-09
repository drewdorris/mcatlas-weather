package net.mcatlas.weather.model;

public class WeatherData {
    final int x;
    final int z;

    final String weatherDesc;
    final String weatherFullDesc;
    final double temperature;
    final double pressure;
    final double humidity;

    final double windSpeed;
    final double windDirection;
    final double windGust;

    final double cloudiness;

    final double visibility;

    final String name;

    public WeatherData(int x, int z, String weatherDesc, String weatherFullDesc, double temperature, double pressure,
                       double humidity, double windSpeed, double windDirection, double windGust, double cloudiness,
                       double visibility, String name) {
        this.x = x;
        this.z = z;
        this.weatherDesc = weatherDesc;
        this.weatherFullDesc = weatherFullDesc;
        this.temperature = temperature;
        this.pressure = pressure;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.windGust = windGust;
        this.cloudiness = cloudiness;
        this.visibility = visibility;
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public String getWeatherDesc() {
        return weatherDesc;
    }

    public String getWeatherFullDesc() {
        return weatherFullDesc;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getPressure() {
        return pressure;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public double getWindDirection() {
        return windDirection;
    }

    public double getWindGust() {
        return windGust;
    }

    public double getCloudiness() {
        return cloudiness;
    }

    public double getVisibility() {
        return visibility;
    }

    public String getName() {
        return name;
    }

}
