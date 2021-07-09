package net.mcatlas.weather.handlers;

import net.mcatlas.weather.WeatherPlugin;
import net.mcatlas.weather.model.Coordinate;
import net.mcatlas.weather.model.Forecast;
import net.mcatlas.weather.model.Tornado;
import net.mcatlas.weather.model.TropicalCyclone;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.*;

import java.util.HashSet;
import java.util.Set;

public class DynmapHandler {
    private DynmapAPI api;
    private MarkerAPI markerapi;
    private MarkerSet stormMarkerSet;
    private MarkerSet tornadoMarkerSet;

    private Set<MarkerDescription> tornadoMarkers;
    private Set<MarkerDescription> stormMarkers;

    public DynmapHandler() {
        Plugin dynmap = WeatherPlugin.get().getServer().getPluginManager().getPlugin("dynmap");
        if (dynmap == null) {
            Bukkit.getLogger().warning("Dynmap not found!!");
            return;
        }

        this.api = (DynmapAPI) dynmap; /* Get API */

        this.markerapi = api.getMarkerAPI();
        if (this.markerapi == null) {
            Bukkit.getLogger().warning("Error loading dynmap marker API!");
            return;
        }

        tornadoMarkers = new HashSet<>();
        stormMarkers = new HashSet<>();

        // Create new countries markerset
        tornadoMarkerSet = markerapi.getMarkerSet("tornadoes.markerset");
        if(tornadoMarkerSet == null) {
            tornadoMarkerSet = markerapi.createMarkerSet("tornadoes.markerset",
                    "Tornadoes", null, false);
        } else {
            tornadoMarkerSet.setMarkerSetLabel("Tornadoes");
        }
        if (tornadoMarkerSet == null) {
            Bukkit.getLogger().warning("Error creating marker set");
            return;
        }
        tornadoMarkerSet.setLayerPriority(90);
        tornadoMarkerSet.setHideByDefault(false);

        // Create new countries markerset
        stormMarkerSet = markerapi.getMarkerSet("tropicalcyclones.markerset");
        if(stormMarkerSet == null) {
            stormMarkerSet = markerapi.createMarkerSet("tropicalcyclones.markerset",
                    "Tropical Cyclones", null, false);
        } else {
            stormMarkerSet.setMarkerSetLabel("Tropical Cyclones");
        }
        if (stormMarkerSet == null) {
            Bukkit.getLogger().warning("Error creating marker set");
            return;
        }
        stormMarkerSet.setLayerPriority(100);
        stormMarkerSet.setHideByDefault(false);
    }

    public void disable() {
        for (MarkerDescription marker : tornadoMarkers) {
            marker.deleteMarker();
        }
        for (MarkerDescription marker : stormMarkers) {
            marker.deleteMarker();
        }
    }

    public void createTornadoMarkers(Set<Tornado> tornadoes) {
        for (MarkerDescription marker : tornadoMarkers) {
            marker.deleteMarker();
        }
        int i = 0;
        for (Tornado tornado : tornadoes) {
            i++;
            tornadoMarkers.add(tornadoMarkerSet.createMarker(tornado.getArea() + i, "Tornado in " + tornado.getShortenedArea(), tornado.getLocation().getWorld().getName(),
                    tornado.getLocation().getX(), 64, tornado.getLocation().getZ(), markerapi.getMarkerIcon("tornado"), false));
        }
    }

    public void createTropicalCycloneMarkers(Set<TropicalCyclone> storms) {
        for (MarkerDescription marker : stormMarkers) {
            marker.deleteMarker();
        }
        for (TropicalCyclone storm : storms) {
            String cycloneIcon = "wx_td";
            if (storm.name.contains("Tropical Storm")) {
                cycloneIcon = "wx_ts";
            } else if (storm.name.contains("Hurricane")) {
                cycloneIcon = "wx_hu";
            }
            Marker marker = stormMarkerSet.createMarker(storm.name, storm.name, storm.location.getWorld().getName(),
                    storm.location.getX(), 64, storm.location.getZ(), markerapi.getMarkerIcon(cycloneIcon), false);
            String stormDesc = WeatherPlugin.get().getConfig().getString("storminfowindow");
            stormDesc = stormDesc.replace("%stormname%", storm.name);
            stormDesc = stormDesc.replace("%directionandspeed%", "Going " + storm.direction + " at " + (int) storm.directionSpeed + " mph");
            stormDesc = stormDesc.replace("%windspeed%", (int) storm.windsMph + " mph");
            stormDesc = stormDesc.replace("%pressure%", "" + (int) storm.pressure + " mbar");
            stormDesc = stormDesc.replace("%date%", storm.dateLastUpdated);
            marker.setDescription(stormDesc);
            stormMarkers.add(marker);

            int i = 0;
            double[] xx = new double[storm.forecasts.length + 1];
            double[] yy = new double[storm.forecasts.length + 1];
            double[] zz = new double[storm.forecasts.length + 1];
            xx[i] = storm.location.getBlockX();
            yy[i] = 64;
            zz[i] = storm.location.getBlockZ();
            for (Forecast forecast : storm.forecasts) {
                String forecastIcon = "wx_f_td";
                if (forecast.windsMph < 74 && forecast.windsMph > 38) {
                    forecastIcon = "wx_f_ts";
                } else if (forecast.windsMph > 73 && forecast.windsMph < 111) {
                    forecastIcon = "wx_f_hu";
                } else if (forecast.windsMph > 110) {
                    forecastIcon = "wx_f_mh";
                }
                i++;
                Marker forecastMarker = stormMarkerSet.createMarker(storm.name + i, storm.name + " " + forecast.time, forecast.location.getWorld().getName(),
                        forecast.location.getX(), 64, forecast.location.getZ(), markerapi.getMarkerIcon(forecastIcon), false);
                String forecastDesc = WeatherPlugin.get().getConfig().getString("forecastinfowindow");
                forecastDesc = forecastDesc.replace("%stormname%", storm.name);
                forecastDesc = forecastDesc.replace("%date%", forecast.time);
                forecastDesc = forecastDesc.replace("%windspeed%", (int) forecast.windsMph + " mph");
                forecastMarker.setDescription(forecastDesc);
                stormMarkers.add(forecastMarker);

                xx[i] = forecast.location.getBlockX();
                yy[i] = 64;
                zz[i] = forecast.location.getBlockZ();
            }

            PolyLineMarker forecastLine = stormMarkerSet.createPolyLineMarker(storm.name + "_forecastline",
                    "", false, storm.location.getWorld().getName(), xx, yy, zz, false);
            int color = 0xAAAAAA;
            forecastLine.setLineStyle(3, .5, color);
            stormMarkers.add(forecastLine);

            double[] conex = new double[storm.cone.length];
            double[] coney = new double[storm.cone.length];
            double[] conez = new double[storm.cone.length];
            int j = 0;
            for (Coordinate coord : storm.cone) {
                Coordinate mc = Coordinate.getMCFromLife(coord.y, coord.x);
                conex[j] = mc.x;
                coney[j] = 64;
                conez[j] = mc.y;
                j++;
            }

            PolyLineMarker cone = stormMarkerSet.createPolyLineMarker(storm.name + "_cone",
                    "", false, storm.location.getWorld().getName(), conex, coney, conez, false);
            cone.setLineStyle(3, .7, 0x444444);
            stormMarkers.add(cone);
        }
    }

}
