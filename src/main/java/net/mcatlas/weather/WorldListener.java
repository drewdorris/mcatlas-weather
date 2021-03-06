package net.mcatlas.weather;

import net.mcatlas.weather.handlers.TropicalCycloneHandler;
import net.mcatlas.weather.handlers.WeatherStatusHandler;
import net.mcatlas.weather.model.Tornado;
import net.mcatlas.weather.model.TropicalCyclone;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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

	// When block is placed
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		Location location = event.getBlock().getLocation();
		if (location.getWorld().getEnvironment() != World.Environment.NORMAL) {
			return;
		}
		if (WeatherPlugin.get().getTropicalCycloneHandler() != null) {
			location.setY(64);
			for (TropicalCyclone cyclone : WeatherPlugin.get().getTropicalCycloneHandler().getCyclones()) {
				Location cycloneLoc = cyclone.getLocation();
				cycloneLoc.setY(64);
				double dist = cycloneLoc.distance(location);
				if (dist < 25) {
					event.setCancelled(true);
					event.getPlayer().sendMessage(ChatColor.RED + "You can't place blocks near " + cyclone.getName() + "!");
					return;
				}
			}
		}
		if (WeatherPlugin.get().getTornadoHandler() != null) {
			location.setY(64);
			for (Tornado tornado : WeatherPlugin.get().getTornadoHandler().getTornadoes()) {
				Location tornadoLoc = tornado.getLocation();
				tornadoLoc.setY(64);
				double dist = tornado.getLocation().distance(location);
				if (dist < 13) {
					event.setCancelled(true);
					event.getPlayer().sendMessage(ChatColor.RED + "You can't place near the " + tornado.getArea() + " tornado");
					return;
				}
			}
		}
	}

	// When block is placed
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent event) {
		Location location = event.getBlock().getLocation();
		if (location.getWorld().getEnvironment() != World.Environment.NORMAL) {
			return;
		}
		if (WeatherPlugin.get().getTropicalCycloneHandler() != null) {
			location.setY(64);
			for (TropicalCyclone cyclone : WeatherPlugin.get().getTropicalCycloneHandler().getCyclones()) {
				double dist = cyclone.getLocation().distance(location);
				if (dist < 25) {
					event.setCancelled(true);
					event.getPlayer().sendMessage(ChatColor.RED + "You can't break near " + cyclone.getName() + "!");
					return;
				}
			}
		}
		if (WeatherPlugin.get().getTornadoHandler() != null) {
			location.setY(64);
			for (Tornado tornado : WeatherPlugin.get().getTornadoHandler().getTornadoes()) {
				Location tornadoLoc = tornado.getLocation();
				tornadoLoc.setY(64);
				double dist = tornado.getLocation().distance(location);
				if (dist < 13) {
					event.setCancelled(true);
					event.getPlayer().sendMessage(ChatColor.RED + "You can't break near the " + tornado.getArea() + " tornado!");
					return;
				}
			}
		}
	}

	// handle firework rockets in tropical cyclones
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getItem() != null && event.getItem().getType() == Material.FIREWORK_ROCKET) {
				TropicalCycloneHandler cycloneHandler = WeatherPlugin.get().getTropicalCycloneHandler();
				if (cycloneHandler == null) return;

				Player player = event.getPlayer();
				if (cycloneHandler.isInCycloneWindsUnprotected(player)) {
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED + "Can't use rockets near a tropical cyclone!");
				}
			}
		}
	}

}
