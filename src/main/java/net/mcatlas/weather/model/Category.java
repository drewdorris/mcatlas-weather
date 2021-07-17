package net.mcatlas.weather.model;

/**
 * Using Saffir-Simpson for every storm in the ocean because we're American
 */
public enum Category {
    POST_TROPICAL("Post-Tropical", 1),
    TROPICAL_DEPRESSION("Tropical Depression", 1),
    TROPICAL_STORM("Tropical Storm", 2),
    CAT_1("Category 1", 3),
    CAT_2("Category 2", 3),
    CAT_3("Category 3", 4),
    CAT_4("Category 4", 5),
    CAT_5("Category 5", 6);

    public final String formatted;
    public final int power;

    Category(String formatted, int power) {
        this.formatted = formatted;
        this.power = power;
    }
}
