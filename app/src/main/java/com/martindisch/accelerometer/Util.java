package com.martindisch.accelerometer;

import java.util.List;

/**
 * Class with helper functions.
 */
public class Util {

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Returns a String containing the hexadecimal representation of the given byte array.
     *
     * @param bytes the byte array to convert
     * @return String containing the hexadecimal representation
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Converts the raw accelerometer data into G units.
     *
     * @param value the raw sensor data
     * @return an array with the G unit values for the x, y and z axes
     */
    public static double[] convertAccel(byte[] value) {
        // ±8 G range
        final float SCALE = 32768 / 8;

        int x = (value[7] << 8) + value[6];
        int y = (value[9] << 8) + value[8];
        int z = (value[11] << 8) + value[10];
        return new double[]{((x / SCALE) * -1), y / SCALE, ((z / SCALE) * -1)};
    }

    /**
     * Returns a String containing the content of a CSV file representing
     * the list of {@link Measurement} given.
     *
     * @param recording the list of measurements
     * @return the CSV String representing the measurements
     */
    public static String recordingToCSV(List<Measurement> recording) {
        String csv = "time,x,y,z,combined\n";
        for (Measurement measurement : recording) {
            csv += measurement.getTime() + "," + measurement.getX()
                    + "," + measurement.getY() + "," + measurement.getZ()
                    + "," + measurement.getCombined() + "\n";
        }
        return csv;
    }
}
