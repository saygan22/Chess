package com.saygan;

import static com.saygan.Constants.BOARD_HEIGHT_PX;
import static com.saygan.Constants.BOARD_WIDTH_PX;
import static com.saygan.Constants.CELL_SIZE;
import static com.saygan.Constants.FIRST_HORIZONTAL_COORD;
import static com.saygan.Constants.LAST_VERTICAL_COORD;

public class ChessUtils {

    public static boolean isEven(int digit) {
        return digit % 2 == 0;
    }

    public static boolean isWithinXBorder(double x) {
        return x >= 0 && x < BOARD_WIDTH_PX;
    }

    public static boolean isWithinYBorder(double y) {
        return y >= 0 && y < BOARD_HEIGHT_PX;
    }

    public static boolean isOutOfXBorder(double x) {
        return x < 0 || x >= BOARD_WIDTH_PX;
    }

    public static boolean isOutOfYBorder(double y) {
        return y < 0 || y >= BOARD_HEIGHT_PX;
    }

    public static boolean isOutOfBorder(double x, double y) {
        return isOutOfXBorder(x) || isOutOfYBorder(y);
    }

    public static int countCursorOffset(double d) {
        return (int) (d % CELL_SIZE);
    }

    public static int[] coordToXy(String coord) {
        return new int[] { (coord.charAt(0) - FIRST_HORIZONTAL_COORD) * CELL_SIZE, (LAST_VERTICAL_COORD - coord.charAt(1)) * CELL_SIZE};
    }

    public static String xyToCoord(double x, double y) {
        char hCoord = (char)(FIRST_HORIZONTAL_COORD + (int)(x / CELL_SIZE));
        char vCoord = (char)(LAST_VERTICAL_COORD - (int)(y / CELL_SIZE));
        return new String(new char[] {hCoord, vCoord});
    }
}
