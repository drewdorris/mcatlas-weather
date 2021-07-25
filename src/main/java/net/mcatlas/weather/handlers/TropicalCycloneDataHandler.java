package net.mcatlas.weather.handlers;

import net.mcatlas.weather.WeatherPlugin;
import net.mcatlas.weather.model.TropicalCyclone;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TropicalCycloneDataHandler {

    private WeatherPlugin plugin;

    private TropicalCycloneHandler cycloneHandler;
    private FileConfiguration dataYaml;

    public static final String DATA_FILE_NAME = "cycloneplayerdata.yml";

    public TropicalCycloneDataHandler(WeatherPlugin plugin) {
        this.plugin = plugin;
        this.cycloneHandler = plugin.getTropicalCycloneHandler();
        if (!loadTropicalCycloneData()) {
            plugin.getLogger().warning("Issue loading tropical cyclone data!");
            return;
        }

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (!cycloneHandler.isLoaded()) return;
            refreshPlayers(cycloneHandler.getCyclones());
        }, 0L, 20 * 15L); // every 15sec
    }

    private boolean loadTropicalCycloneData() {
        File configFile = new File(plugin.getDataFolder(), DATA_FILE_NAME);
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                plugin.getLogger().warning("Issue creating tropical cyclone data file!");
                return false;
            }
        }
        dataYaml = YamlConfiguration.loadConfiguration(configFile);
        return dataYaml != null;
    }

    public void refreshPlayers(Collection<TropicalCyclone> cyclones) {
        if (dataYaml.getConfigurationSection("cyclones") == null) return;
        for (String cycloneId : dataYaml.getConfigurationSection("cyclones").getKeys(false)) {
            String cycloneSection = "cyclones." + cycloneId;
            Set<String> players = dataYaml.getStringList(cycloneSection).stream().collect(Collectors.toSet());

            Optional<TropicalCyclone> matchingCycloneOpt = cyclones.stream().findFirst().filter(c -> c.getId().equals(cycloneId));

            if (matchingCycloneOpt.isPresent()) {
                TropicalCyclone cyclone = matchingCycloneOpt.get();
                boolean changed = players.addAll(cyclone.getPlayersWhoReceivedReward()
                        .stream()
                        .map(UUID::toString)
                        .collect(Collectors.toSet()));
                if (changed) {
                    save(cycloneSection, players.stream().collect(Collectors.toList()));
                }
                cyclone.setPlayersWhoReceivedReward(players.stream().map(UUID::fromString).collect(Collectors.toSet()));
            } else {
                save(cycloneSection, null);
                continue;
            }
        }
    }

    // run async
    public void addPlayer(String cycloneId, UUID playerUUID) {
        String section = "cyclones." + cycloneId;
        List<String> players = dataYaml.getStringList(section);
        if (players == null) players = new ArrayList<>();

        players.add(playerUUID.toString());
        save(section, players);
    }

    private void save(String section, List<String> players) {
        dataYaml.set(section, players);
        try {
            dataYaml.save(DATA_FILE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
