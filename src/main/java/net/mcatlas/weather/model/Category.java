package net.mcatlas.weather.model;

/**
 * Using Saffir-Simpson for every storm in the ocean because we're American
 */
public enum Category {
    POST_TROPICAL(1),
    TROPICAL_DEPRESSION(1),
    TROPICAL_STORM(2),
    CAT_1(3),
    CAT_2(3),
    CAT_3(4),
    CAT_4(5),
    CAT_5(6);

    private int power;

    Category(int power) {
        this.power = power;
    }
}
