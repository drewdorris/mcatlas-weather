package net.mcatlas.weather;

import net.mcatlas.weather.handlers.WeatherStatusHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.ItemStack;

import static net.mcatlas.weather.WeatherUtil.*;

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
