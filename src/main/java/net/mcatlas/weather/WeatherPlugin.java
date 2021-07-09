package net.mcatlas.weather;

import net.mcatlas.weather.handlers.*;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles elements of the MCAtlas world itself.
 */
public class WeatherPlugin extends JavaPlugin {

    private static WeatherPlugin plugin;

    private String apiKey;
    private double scaling;
    private boolean apiOffline = false;

    private DynmapHandler dynmapHandler;
    private JsonHandler jsonHandler;

    private TornadoHandler tornadoHandler;
    private TropicalCycloneHandler tropicalCycloneHandler;
    private WeatherStatusHandler weatherStatusHandler;

    public static WeatherPlugin get() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;

        getServer().getPluginManager().registerEvents(new WorldListener(), this);

        saveDefaultConfig();
        this.apiKey = getConfig().getString("apiKey", null);
        this.scaling = getConfig().getDouble("scaling");

        boolean enableWeatherStatus = getConfig().getBoolean("enableWeatherStatus", true);
        boolean enableTornadoes = getConfig().getBoolean("enableTornadoes", true);
        boolean enableTropicalCyclones = getConfig().getBoolean("enableTropicalCyclones", true);

        if (enableWeatherStatus || enableTornadoes || enableTropicalCyclones) {
            this.jsonHandler = new JsonHandler(plugin);

            if (enableTornadoes || enableTropicalCyclones) {
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    String stormInfoWindow = getConfig().getString("storminfowindow");
                    String forecastInfoWindow = getConfig().getString("forecastinfowindow");
                    this.dynmapHandler = new DynmapHandler(plugin, stormInfoWindow, forecastInfoWindow);
                }, 20 * 10);
            }
        }

        if (enableWeatherStatus && apiKey != null) {
            getCommand("weatherhere").setExecutor(new WeatherHereCommand());
            this.weatherStatusHandler = new WeatherStatusHandler(this);
        }

        if (enableTornadoes) {
            this.tornadoHandler = new TornadoHandler(this, getConfig().getInt("minutesBetweenTornadoAlerts", 10));
        }

        if (enableTropicalCyclones) {
            this.tropicalCycloneHandler = new TropicalCycloneHandler(this);
        }
    }

    @Override
    public void onDisable() {
        if (dynmapHandler != null) {
            dynmapHandler.disable();
        }
        plugin = null;
    }

    public double scaling() {
        return scaling;
    }

    public String getAPIKey() {
        return apiKey;
    }

    public boolean isApiOffline() { return apiOffline; }

    public void setApiOffline() { this.apiOffline = true; }

    public void setApiOnline() { this.apiOffline = false; }

    public JsonHandler getJsonHandler() {
        return jsonHandler;
    }

    public DynmapHandler getDynmapHandler() {
        return dynmapHandler;
    }

    public TornadoHandler getTornadoHandler() {
        return tornadoHandler;
    }

    public WeatherStatusHandler getWeatherStatusHandler() {
        return weatherStatusHandler;
    }
}
