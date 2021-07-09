package net.mcatlas.weather;

import net.mcatlas.weather.model.WeatherData;
import net.mcatlas.weather.model.WeatherPlayer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;

/**
 *  This exists just to check if the API is working, and to check accuracy
 *
 */
public class WeatherHereCommand implements CommandExecutor {

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) return false;
		Player player = (Player) sender;

		WeatherPlayer weatherPlayer = WeatherPlugin.get().getWeatherPlayer(player.getUniqueId());
		if (weatherPlayer == null) {
			player.sendMessage("Error");
			return false;
		}
		WeatherData weatherData = weatherPlayer.getWeatherData();

		player.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE +
				weatherData.weatherDesc + "; " + weatherData.weatherFullDesc);

		int fahrenheit = (int) WeatherUtil.kelvinToFahrenheit(weatherData.temperature);
		int celsius = (int) WeatherUtil.kelvinToCelsius(weatherData.temperature);
		Color colorTemp = WeatherUtil.getColorFromTemperature(fahrenheit);
		ChatColor chatColor = net.md_5.bungee.api.ChatColor.of(colorTemp);
		player.sendMessage(ChatColor.YELLOW + "Temperature: " + chatColor +
				fahrenheit + "F" + ChatColor.WHITE + "/" + chatColor + celsius + "C");

		Location playerLocation = player.getLocation();
		playerLocation.setY(64);
		Location weatherLocation = new Location(playerLocation.getWorld(), weatherData.x, 64, weatherData.z);
		int distance = (int) playerLocation.distance(weatherLocation);
		player.sendMessage(ChatColor.YELLOW + "Coords: " + ChatColor.WHITE +
				weatherLocation.getBlockX() + ", " + weatherLocation.getBlockZ() + " " +
				ChatColor.GRAY + "(" + distance + " blocks away)");

		player.sendMessage(ChatColor.YELLOW + "Pressure: " + ChatColor.WHITE + (int) weatherData.pressure + "mb");
		player.sendMessage(ChatColor.YELLOW + "Humidity: " + ChatColor.WHITE + (int) weatherData.humidity + "%");
		player.sendMessage(ChatColor.YELLOW + "Cloudiness: " + ChatColor.WHITE + (int) weatherData.cloudiness + "%");
		player.sendMessage(ChatColor.YELLOW + "Visibility: " + ChatColor.WHITE + (int) weatherData.visibility + " meters");

		String direction = WeatherUtil.degreesToCardinal(weatherData.windDirection);
		player.sendMessage(ChatColor.YELLOW + "Wind: " + ChatColor.WHITE + direction + " at " +
				(int) weatherData.windSpeed + "mph; " + (int) weatherData.windGust + "mph gust");

		if (weatherData.name != null && !weatherData.name.isEmpty()) {
			player.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + "\"" + weatherData.name + "\"");
		}

		if (args.length > 0) {
			String arg = args[0];
			int temp = 0;
			try {
				temp = Integer.parseInt(arg);
			} catch (Exception e) {}
			Color colorTemp2 = WeatherUtil.getColorFromTemperature(temp);
			ChatColor chatColor2 = net.md_5.bungee.api.ChatColor.of(colorTemp);
			player.sendMessage(chatColor2 + "This is the color of " + temp + " Fahrenheit");
			player.sendMessage(chatColor2 + "" + colorTemp2.getRed() + "R " + colorTemp2.getGreen() + "G " + colorTemp2.getBlue() + "B");
		}

		return true;
	}

}
