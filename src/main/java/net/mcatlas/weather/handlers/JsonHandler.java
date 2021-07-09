package net.mcatlas.weather.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mcatlas.weather.WeatherPlugin;
import net.mcatlas.weather.model.*;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class JsonHandler {

    public JsonElement getJsonFromURL(String urlString) {
        URL url = null;
        HttpURLConnection connection = null;

        try {
            url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
        } catch (Exception e) { // any issue with the connection, like an error code
            if (connection == null) {
                Bukkit.getLogger().warning(urlString + " down?");
                return null;
            }
            int code = 0;
            try {
                code = connection.getResponseCode();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            if (code == 500 && WeatherPlugin.get().isApiOffline()) { // api is known to be offline
                return null;
            } else if (code == 500 && !WeatherPlugin.get().isApiOffline()) { // api is not known to be offline
                Bukkit.getLogger().warning(urlString + " down?");
                WeatherPlugin.get().setApiOffline();
                return null;
            } else if (code == 400) { // bad request (happens occasionally)
                return null;
            } else { // anything else
                e.printStackTrace();
                return null;
            }
        }

        JsonParser jp = new JsonParser();
        JsonElement root;
        try {
            root = jp.parse(new InputStreamReader((InputStream) connection.getContent()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return root;
    }

    // THIS IS FOR TESTING ONLY
    // example input "plugins/mcatlas-environment/tropicalstorms.json"
    public JsonElement getJsonFromLocal(String jsonLocation) {
        JsonElement json = null;

        try (Reader reader = new InputStreamReader(new FileInputStream(jsonLocation), "UTF-8")) {
            json = new JsonParser().parse(reader);
        } catch (Exception e) {
            // do something
            WeatherPlugin.get().getLogger().warning("FILE NOT FOUND");
            e.printStackTrace();
            return null;
        }

        return json;
    }

    @Nullable
    public WeatherData extractWeatherData(JsonElement json, Location location) {
        // used for weather obj
        int x = location.getBlockX();
        int z = location.getBlockZ();

        if (json == null || json.isJsonNull()) {
            return null;
        }

        JsonObject rootobj = json.getAsJsonObject();

        JsonArray weatherArray = rootobj.getAsJsonArray("weather");
        JsonObject weather = weatherArray.get(0).getAsJsonObject();
        // description used for deciding weather conditions ingame
        String weatherDesc = weather.get("main").getAsString();
        String weatherFullDesc = "";
        try { // full desc sometimes not listed
            weatherFullDesc = weather.get("description").getAsString();
        } catch (Exception e) { }
        weatherFullDesc = WordUtils.capitalizeFully(weatherFullDesc);

        JsonObject mainObject = rootobj.getAsJsonObject("main");
        double temp = mainObject.get("temp").getAsDouble();
        double pressure = mainObject.get("pressure").getAsDouble();
        double humidity = mainObject.get("humidity").getAsDouble();

        JsonObject windObject = rootobj.getAsJsonObject("wind");
        double windSpeed = windObject.get("speed").getAsDouble();
        double windDirection = windObject.get("deg").getAsDouble();
        double windGust = 0;
        try { // wind gust occasionally not listed
            windGust = windObject.get("gust").getAsDouble();
        } catch (Exception e) { }

        JsonObject cloudObject = rootobj.getAsJsonObject("clouds");
        double cloudy = cloudObject.get("all").getAsDouble();

        double visibility = rootobj.get("visibility").getAsDouble();

        String name = rootobj.get("name").getAsString();

        WeatherData data = new WeatherData(x, z, weatherDesc, weatherFullDesc, temp, pressure, humidity, windSpeed,
                windDirection, windGust, cloudy, visibility, name);

        if (WeatherPlugin.get().isApiOffline()) {
            Bukkit.getLogger().info("OpenWeatherMap is back online.");
            WeatherPlugin.get().setApiOnline();
        }

        return data;
    }

    public Set<TropicalCyclone> extractTropicalCycloneData(JsonElement data) {
        Set<TropicalCyclone> tropicalCyclones = new HashSet<>();

        if (data == null || data.isJsonNull()) return tropicalCyclones;

        JsonObject rootobj = data.getAsJsonObject();

        JsonObject alerts = rootobj.getAsJsonObject("data");
        for (Map.Entry<String, JsonElement> entry : alerts.entrySet()) {
            JsonElement stormElement = entry.getValue();
            JsonObject stormObj = stormElement.getAsJsonObject();
            if (stormElement == null || stormElement.isJsonNull()) {
                WeatherPlugin.get().getLogger().warning("Storm Null");
                continue;
            }
            boolean active = stormObj.get("active").getAsBoolean();
            if (!active) continue;

            JsonArray properties = stormObj.get("history").getAsJsonArray();
            JsonElement lastHistory = properties.get(properties.size() - 1);
            if (lastHistory == null || lastHistory.isJsonNull()) {
                WeatherPlugin.get().getLogger().warning("Event Null");
                continue;
            }
            JsonObject lastHistoryObj = lastHistory.getAsJsonObject();
            String name = lastHistoryObj.get("name").getAsString();
            String date = lastHistoryObj.get("date").getAsString();
            double lat = lastHistoryObj.get("lat").getAsDouble();
            double lon = lastHistoryObj.get("lon").getAsDouble();
            String direction = lastHistoryObj.get("dir").getAsString();
            double directionSpeed = lastHistoryObj.get("speed").getAsDouble();
            double windSpeed = lastHistoryObj.get("winds").getAsDouble();
            double pressure = lastHistoryObj.get("pressure").getAsDouble();

            JsonArray forecasts = stormObj.get("forecast").getAsJsonArray();
            List<Forecast> stormForecasts = new ArrayList<>();
            for (JsonElement forecast : forecasts) {
                JsonObject forecastObj = forecast.getAsJsonObject();
                String fDate = forecastObj.get("time").getAsString();
                double fLat = forecastObj.get("lat").getAsDouble();
                double fLon = forecastObj.get("lon").getAsDouble();
                double fWindSpeed = forecastObj.get("winds_mph").getAsDouble();
                Forecast stormForecast = new Forecast(fDate, fLat, fLon, fWindSpeed);
                stormForecasts.add(stormForecast);
            }
            Forecast[] forecastsArray = stormForecasts.toArray(new Forecast[0]);

            JsonArray coneCoordsArr = stormObj.get("cone").getAsJsonArray();
            List<Coordinate> coordinates = new ArrayList<>();
            for (JsonElement coneCoordElement : coneCoordsArr) {
                JsonArray coneCoord = coneCoordElement.getAsJsonArray();
                Coordinate coord = new Coordinate(coneCoord.get(0).getAsDouble(), coneCoord.get(1).getAsDouble());
                coordinates.add(coord);
            }
            Coordinate[] coneCoordinates = coordinates.toArray(new Coordinate[0]);

            TropicalCyclone storm = new TropicalCyclone(name, lat, lon, direction,
                    directionSpeed, windSpeed, pressure, date, coneCoordinates, forecastsArray);
            tropicalCyclones.add(storm);
        }

        return tropicalCyclones;
    }

    public Set<Tornado> extractTornadoData(JsonElement data) {
        Set<Tornado> tornadoes = new HashSet<>();

        if (data == null || data.isJsonNull()) return tornadoes;

        JsonObject rootobj = data.getAsJsonObject();

        JsonArray alerts = rootobj.getAsJsonArray("features");
        for (JsonElement alert : alerts) {
            JsonObject alertObj = alert.getAsJsonObject();
            if (alertObj == null || alertObj.isJsonNull()) {
                WeatherPlugin.get().getLogger().warning("Alert Null");
                continue;
            }
            JsonObject properties = alertObj.get("properties").getAsJsonObject();
            JsonElement eventObj = properties.get("event");
            if (eventObj == null || eventObj.isJsonNull()) {
                WeatherPlugin.get().getLogger().warning("Event Null");
                continue;
            }
            String event = eventObj.getAsString();
            if (!event.equals("Tornado Warning")) continue;

            JsonElement areaObj = properties.get("areaDesc");
            String area = "";
            if (areaObj == null || areaObj.isJsonNull()) {
                WeatherPlugin.get().getLogger().warning("Area Null");
                area = "Unknown";
            } else {
                area = areaObj.getAsString();
            }

            JsonObject parameters = properties.get("parameters").getAsJsonObject();

            JsonArray eventMotion = parameters.getAsJsonArray("eventMotionDescription");
            if (eventMotion == null || eventMotion.isJsonNull()) {
                WeatherPlugin.get().getLogger().warning("Event Motion Null");
                continue;
            }

            JsonElement descElement = eventMotion.get(0);
            if (descElement == null || descElement.isJsonNull()) {
                WeatherPlugin.get().getLogger().warning("Event Desc Null");
                continue;
            }
            String desc = descElement.getAsString();
            String coords = desc.substring(desc.lastIndexOf("...") + 3)
                    .replace("...", "");

            String latStr = coords.substring(0, coords.indexOf(","));
            double lat = Double.valueOf(latStr);
            String lonStr = coords.substring(coords.lastIndexOf(",") + 1);
            double lon = Double.valueOf(lonStr);

            Coordinate coord = Coordinate.getMCFromLife(lat, lon);

            Location location = new Location(Bukkit.getWorlds().get(0), coord.getX(), 64, coord.getY());
            Tornado tornado = new Tornado(location, area);
            tornadoes.add(tornado);
        }

        return tornadoes;
    }

}
