package net.mcatlas.weather;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.mcatlas.weather.handlers.*;
import net.mcatlas.weather.model.*;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.mcatlas.weather.WeatherUtil.*;

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

        boolean enableWeather = getConfig().getBoolean("enableWeather", true);
        boolean enableWeatherStatus = true;
        boolean enableTornadoes = true;
        boolean enableTropicalCyclones = true;

        if (enableWeatherStatus || enableTornadoes || enableTropicalCyclones) {
            this.jsonHandler = new JsonHandler();

            if (enableTornadoes || enableTropicalCyclones) {
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    this.dynmapHandler = new DynmapHandler();
                }, 20 * 10);
            }
        }

        if (enableWeatherStatus && apiKey != null) {
            getCommand("weatherhere").setExecutor(new WeatherHereCommand());
            this.weatherStatusHandler = new WeatherStatusHandler(this);
        }

        if (enableTornadoes) {
            this.tornadoHandler = new TornadoHandler(this, getConfig().getInt("minutesBetweenTornadoAlerts"));
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

}
