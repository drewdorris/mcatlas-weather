package net.mcatlas.weather.handlers;

import com.google.gson.JsonElement;
import net.mcatlas.weather.WeatherPlugin;
import net.mcatlas.weather.model.TropicalCyclone;
import org.bukkit.Bukkit;

import java.util.Set;

public class TropicalCycloneHandler {

    private WeatherPlugin plugin;

    public TropicalCycloneHandler(WeatherPlugin plugin) {
        this.plugin = plugin;

        Bukkit.getScheduler().runTaskTimerAsynchronously(WeatherPlugin.get(), () -> {
            updateTropicalCyclones();
        }, 20 * 20L, 20 * 60 * 30);
    }

    public void updateTropicalCyclones() {
        plugin.getDynmapHandler().resetTropicalCycloneMarkers();
        JsonElement data = plugin.getJsonHandler().getJsonFromURL("https://api.weatherusa.net/v1.2/tropical?storm=active");
        Set<TropicalCyclone> storms = plugin.getJsonHandler().extractTropicalCycloneData(data);
        plugin.getDynmapHandler().createTropicalCycloneMarkers(storms);
    }
}
