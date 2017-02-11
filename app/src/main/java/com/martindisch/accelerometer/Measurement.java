package com.martindisch.accelerometer;

/**
 * Represents one measurement of x, y and z acceleration
 */
public class Measurement {

    private double x, y, z;
    private String time;

    /**
     * Constructor for a basic measurement.
     *
     * @param x    the x axis acceleration
     * @param y    the y axis acceleration
     * @param z    the z axis acceleration
     * @param time the time the measurement was taken
     */
    public Measurement(double x, double y, double z, String time) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.time = time;
    }

    /**
     * Returns the combined acceleration sqrt(x^2 + y^2 + z^2).
     *
     * @return the acceleration of all three axes combined
     */
    public double getCombined() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getTime() {
        return time;
    }
}
