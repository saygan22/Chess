package com.saygan;

public class Constants {
    static final char FIRST_HORIZONTAL_COORD = 'a';
    static final char FIRST_VERTICAL_COORD = '1';
    static final char LAST_VERTICAL_COORD = '8';
    static final int MESSAGE_AREA_SIZE = 50;
    static final int COORDINATE_RULER_SIZE = 25;
    static final int CELL_SIZE = 70;
    static final int CELL_MARK_RADIUS = CELL_SIZE / 4;
    static final int BOARD_WIDTH = 8;
    static final int BOARD_HEIGHT = 8;
    static final int BOARD_BORDER_PX = 2;
    static final int BOARD_WIDTH_PX = BOARD_WIDTH * CELL_SIZE;
    static final int BOARD_HEIGHT_PX = BOARD_HEIGHT * CELL_SIZE;
    static final int BOARD_PLACE_WIDTH_PX = BOARD_WIDTH_PX + 2 * BOARD_BORDER_PX;
    static final int BOARD_PLACE_HEIGHT_PX = BOARD_HEIGHT_PX + 2 * BOARD_BORDER_PX;

    static final int SINGLE_ROW = 0;
    static final int SINGLE_COL = 0;
    static final int UP = 1;
    static final int LEFT = 0;
    static final int H_CENTER = 1;
    static final int V_CENTER = 2;
    static final int BOTTOM = 3;
    static final int RIGHT = 2;
}
