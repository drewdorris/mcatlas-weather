package net.mcatlas.weather;

import net.mcatlas.weather.handlers.WeatherStatusHandler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldInitEvent;

/**
 * Environment listener for various things
 *
 */
public class WorldListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onWorldInit(WorldInitEvent event) {
		World world = event.getWorld();

		if (world.getEnvironment() == World.Environment.NORMAL) {
			// EnvironmentUtil.createTornado(new Location(Bukkit.getWorlds().get(0), 500, 100, 500));
			// every 5(maybe) minutes updates tornado locations
			// async
			Bukkit.getScheduler().runTaskTimerAsynchronously(WeatherPlugin.get(), () -> {
				WeatherPlugin.get().getTornadoHandler().updateTornadoes();
			}, 20 * 20L, 20 * 60 * WeatherPlugin.get().getTornadoHandler().getMinutesBetweenTornadoAlerts());
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		World world = event.getPlayer().getWorld();
		if (world.getEnvironment() != World.Environment.NORMAL) {
			return;
		}
		WeatherPlugin.get().getWeatherStatusHandler().addPlayerToQueue(event.getPlayer(), WeatherStatusHandler.WeatherPriority.JOIN);
	}

	/*
	// When block is placed
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		// have something for preventing placing near tornadoes
	}
	 */

}
