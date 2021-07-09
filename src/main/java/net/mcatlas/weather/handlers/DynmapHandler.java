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

    private final String stormInfoWindow;
    private final String forecastInfoWindow;

    private WeatherPlugin plugin;

    public DynmapHandler(WeatherPlugin plugin, String stormInfoWindow, String forecastInfoWindow) {
        this.plugin = plugin;

        this.stormInfoWindow = stormInfoWindow;
        this.forecastInfoWindow = forecastInfoWindow;

        Plugin dynmap = plugin.getServer().getPluginManager().getPlugin("dynmap");
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
            if (storm.getName().contains("Tropical Storm")) {
                cycloneIcon = "wx_ts";
            } else if (storm.getName().contains("Hurricane")) {
                cycloneIcon = "wx_hu";
            }
            Marker marker = stormMarkerSet.createMarker(storm.getName(), storm.getName(), storm.getLocation().getWorld().getName(),
                    storm.getLocation().getX(), 64, storm.getLocation().getZ(), markerapi.getMarkerIcon(cycloneIcon), false);
            String stormDesc = stormInfoWindow;
            stormDesc = stormDesc.replace("%stormname%", storm.getName());
            stormDesc = stormDesc.replace("%directionandspeed%", "Going " + storm.getDirection() + " at " + (int) storm.getDirectionSpeed() + " mph");
            stormDesc = stormDesc.replace("%windspeed%", (int) storm.getWindsMph() + " mph");
            stormDesc = stormDesc.replace("%pressure%", "" + (int) storm.getPressure() + " mbar");
            stormDesc = stormDesc.replace("%date%", storm.getDateLastUpdated());
            marker.setDescription(stormDesc);
            stormMarkers.add(marker);

            int i = 0;
            double[] xx = new double[storm.getForecasts().length + 1];
            double[] yy = new double[storm.getForecasts().length + 1];
            double[] zz = new double[storm.getForecasts().length + 1];
            xx[i] = storm.getLocation().getBlockX();
            yy[i] = 64;
            zz[i] = storm.getLocation().getBlockZ();
            for (Forecast forecast : storm.getForecasts()) {
                String forecastIcon = "wx_f_td";
                if (forecast.getWindsMph() < 74 && forecast.getWindsMph() > 38) {
                    forecastIcon = "wx_f_ts";
                } else if (forecast.getWindsMph() > 73 && forecast.getWindsMph() < 111) {
                    forecastIcon = "wx_f_hu";
                } else if (forecast.getWindsMph() > 110) {
                    forecastIcon = "wx_f_mh";
                }
                i++;
                Marker forecastMarker = stormMarkerSet.createMarker(storm.getName() + i, storm.getName() + " " + forecast.getTime(),
                        forecast.getLocation().getWorld().getName(),
                        forecast.getLocation().getX(), 64, forecast.getLocation().getZ(),
                        markerapi.getMarkerIcon(forecastIcon), false);
                String forecastDesc = forecastInfoWindow;
                forecastDesc = forecastDesc.replace("%stormname%", storm.getName());
                forecastDesc = forecastDesc.replace("%date%", forecast.getTime());
                forecastDesc = forecastDesc.replace("%windspeed%", (int) forecast.getWindsMph() + " mph");
                forecastMarker.setDescription(forecastDesc);
                stormMarkers.add(forecastMarker);

                xx[i] = forecast.getLocation().getBlockX();
                yy[i] = 64;
                zz[i] = forecast.getLocation().getBlockZ();
            }

            PolyLineMarker forecastLine = stormMarkerSet.createPolyLineMarker(storm.getName() + "_forecastline",
                    "", false, storm.getLocation().getWorld().getName(), xx, yy, zz, false);
            int color = 0xAAAAAA;
            forecastLine.setLineStyle(3, .5, color);
            stormMarkers.add(forecastLine);

            double[] conex = new double[storm.getCone().length];
            double[] coney = new double[storm.getCone().length];
            double[] conez = new double[storm.getCone().length];
            int j = 0;
            for (Coordinate coord : storm.getCone()) {
                Coordinate mc = Coordinate.getMCFromLife(coord.getY(), coord.getX());
                conex[j] = mc.getX();
                coney[j] = 64;
                conez[j] = mc.getY();
                j++;
            }

            PolyLineMarker cone = stormMarkerSet.createPolyLineMarker(storm.getName() + "_cone",
                    "", false, storm.getLocation().getWorld().getName(), conex, coney, conez, false);
            cone.setLineStyle(3, .7, 0x444444);
            stormMarkers.add(cone);
        }
    }

}
