package net.mcatlas.weather;

import net.mcatlas.weather.model.Category;
import org.bukkit.*;
import org.bukkit.block.Block;

import java.awt.Color;
import java.util.Random;

public class WeatherUtil {

    public static Random RANDOM = new Random();

    public static String[] directions = { "N", "NE", "E", "SE", "S", "SW", "W", "NW", "N" };

    public static double kelvinToFahrenheit(double kelvin) {
        double celsius = kelvinToCelsius(kelvin);
        return (celsius * 9/5) + 32;
    }

    public static double kelvinToCelsius(double kelvin) {
        return kelvin - 273.15;
    }

    public static Color getColorFromTemperature(double fahrenheit) {
        double r = 0;
        double g = 0;
        double b = 0;

        // RED
        if (fahrenheit > 75) {
            r = 255;
        } else if (fahrenheit > 55) {
            r = 255 - (75 - fahrenheit);
        } else if (fahrenheit > -65) {
            // -65F to 55F colors
            // -65F is 0, 55F is 235
            r = (((fahrenheit + 65) / 120) * 235);
        } else {
            r = 0;
        }

        // GREEN
        if (fahrenheit > 125) {
            g = 0;
        } else if (fahrenheit > 55) {
            // 125F is 0, 55F is 255
            g = 255 - (((fahrenheit - 55) / 70) * 255);
        } else if (fahrenheit >= -65) {
            // -65F to 35F
            // -65F is 55, 55F is 255
            g = (((fahrenheit + 65) / 120) * 200) + 55;
        } else {
            g = 55;
        }

        // BLUE
        if (fahrenheit > 75) {
            // b is 0 >= 75F
            b = 0;
        } else if (fahrenheit > 55) {
            // b is 200 at 55F
            // 200 at 55F to 0 at 75F
            b = 255 - ((((fahrenheit - 55) / 20) * 200) + 55);
        } else if (fahrenheit > 35) {
            // b is 255 at 35F
            // 255 at 35F to 200 at 55F
            b = 255 - (((fahrenheit - 35) / 20) * 55);
        } else {
            // b is 255 below 35
            b = 255;
        }

        Color color = new Color((int) r, (int) g, (int) b);

        return color;
    }

    public static String degreesToCardinal(double degrees) {
        return directions[(int) Math.round(((double) degrees % 360) / 45)];
    }

    public static Category getCategory(String name, double mph) {
        if (name != null && name.contains("Post-Tropical")) return Category.POST_TROPICAL;
        if (mph <= 38) return Category.TROPICAL_DEPRESSION;
        if (mph > 38 && mph < 74) return Category.TROPICAL_STORM;
        if (mph >= 74 && mph < 96) return Category.CAT_1;
        if (mph >= 96 && mph < 111) return Category.CAT_2;
        if (mph >= 111 && mph < 130) return Category.CAT_3;
        if (mph >= 130 && mph < 157) return Category.CAT_4;
        if (mph >= 157) return Category.CAT_5;
        return null;
    }

    public static int getHighestSolidBlockYAt(Location location) {
        location = location.clone();
        location.setY(255);
        return getHighestSolidBlockY(location);
    }

    private static int getHighestSolidBlockY(Location location) {
        int highestY = location.getWorld().getHighestBlockYAt(location);
        Block highest = location.getWorld().getBlockAt(location);
        if (highestY > location.getBlockY() && !isNotSolid(highest.getType())) {
            return location.getBlockY();
        }
        location.setY(location.getBlockY() - 1);
        return getHighestSolidBlockY(location);
    }

    private static boolean isNotSolid(Material material) {
        switch (material) {
            case OAK_LEAVES:
            case BIRCH_LEAVES:
            case ACACIA_LEAVES:
            case DARK_OAK_LEAVES:
            case JUNGLE_LEAVES:
            case SPRUCE_LEAVES:
            case FERN:
            case TALL_GRASS:
            case GRASS:
            case LARGE_FERN:
            case PEONY:
            case ROSE_BUSH:
            case LILAC:
            case SUGAR_CANE:
            case SUNFLOWER:
            case AIR:
            // it's solid but tornadoes look better like this
            case OAK_LOG:
            case BIRCH_LOG:
            case SPRUCE_LOG:
            case JUNGLE_LOG:
            case ACACIA_LOG:
            case DARK_OAK_LOG:
                return true;
        }
        return false;
    }

    /**
     * Returns true number% of the time.
     * Example: (40) would give you a 40% chance to return true.
     * @param number double out of 100
     * @return true number% of the time, false (100 - number)% of the time
     */
    public static boolean chance(double number) {
        return chanceOutOf(number, 100);
    }

    /**
     * Returns true number% of the time.
     * Example: (40) would give you a 40% chance to return true.
     * @param number integer out of 100
     * @return true number% of the time, false (100 - number)% of the time
     */
    public static boolean chance(int number) {
        return chanceOutOf(number, 100);
    }

    /**
     * Returns true number out of outOf times.
     * Example: (50, 250) is a 20% chance.
     * @param number double smaller than
     * @param outOf any value
     * @return true depending on your chance for it to be true, false if not
     */
    public static boolean chanceOutOf(double number, int outOf) {
        return chanceOutOf((int) number, outOf);
    }

    /**
     * Returns true number out of outOf times.
     * Example: (50, 250) is a 20% chance.
     * @param number integer smaller than outOf
     * @param outOf any value
     * @return true depending on your chance for it to be true, false if not
     */
    public static boolean chanceOutOf(int number, int outOf) {
        number--;
        if (number < 0) return false;

        if (number >= outOf) return true;

        int nextInt = RANDOM.nextInt(outOf);

        return number >= nextInt;
    }

}
