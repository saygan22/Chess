package com.saygan;

import static com.saygan.Constants.BOARD_HEIGHT;
import static com.saygan.Constants.BOARD_WIDTH;
import static com.saygan.Constants.CELL_MARK_RADIUS;
import static com.saygan.Constants.CELL_SIZE;
import static com.saygan.Constants.COORDINATE_RULER_SIZE;
import static com.saygan.Constants.FIRST_HORIZONTAL_COORD;
import static com.saygan.Constants.LAST_VERTICAL_COORD;
import static com.saygan.Constants.SINGLE_COL;
import static com.saygan.Constants.SINGLE_ROW;
import static com.saygan.Constants.BOARD_PLACE_WIDTH_PX;

import java.io.InputStream;

import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class UIFactory {

    public static Node whiteCell() {
        Shape cell = new Rectangle(CELL_SIZE, CELL_SIZE);
        cell.setFill(Color.YELLOW);
        return cell;
    }

    public static Node blackCell() {
        Shape cell = new Rectangle(CELL_SIZE, CELL_SIZE);
        cell.setFill(Color.BROWN);
        return cell;
    }

    public static Circle possibleMoveDot() {
        Circle dot = new Circle(CELL_MARK_RADIUS);
        dot.setFill(Color.TRANSPARENT);
        return dot;
    }

    public static Shape target() {
        Shape cell = new Rectangle(CELL_SIZE, CELL_SIZE);
        cell.setFill(Color.color(0, 0, 0, 0));
        cell.setStrokeType(StrokeType.INSIDE);
        cell.setStroke(Color.GREEN);
        cell.setStrokeWidth(4);
        return cell;
    }

    public static Node coordinateSymbol(char symbol) {
        Text text = new Text(String.valueOf(symbol).toUpperCase());
        text.setFill(Color.WHITE);
        text.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 16));

        Label lb = new Label("", text);
        lb.setAlignment(Pos.CENTER);
        lb.setMinWidth(COORDINATE_RULER_SIZE);
        return lb;
    }

    public static Node messageLabel(String message) {
        Text text = new Text(message);
        text.setFill(Color.WHITE);
        text.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 32));

        Label lb = new Label("", text);
        lb.setAlignment(Pos.CENTER);
        lb.setMinWidth(BOARD_PLACE_WIDTH_PX);
        return lb;
    }

    public static GridPane createHorizontalCoordinateRuler() {
        GridPane horizontalCoordinates = new GridPane();
        for (int col = 0; col < BOARD_WIDTH; ++col) {
            char letter = (char) (FIRST_HORIZONTAL_COORD + col);
            horizontalCoordinates.getColumnConstraints().add(horizontalCoordinateConfig());
            horizontalCoordinates.add(coordinateSymbol(letter), col, SINGLE_ROW);
        }
        return horizontalCoordinates;
    }

    public static GridPane createVerticalCoordinateRuler() {
        GridPane verticalCoordinates = new GridPane();
        for (int row = 0; row < BOARD_HEIGHT; ++row) {
            char letter = (char) (LAST_VERTICAL_COORD - row);
            verticalCoordinates.getRowConstraints().add(verticalCoordinateConfig());
            verticalCoordinates.add(coordinateSymbol(letter), SINGLE_COL, row);
        }
        return verticalCoordinates;
    }

    public static ImageView piece(String name) {
        ClassLoader cl = ChessApplication.class.getClassLoader();
        InputStream is = cl.getResourceAsStream("merida/" + name + ".png");
        ImageView piece = new ImageView(new Image(is));
        piece.setFitHeight(CELL_SIZE);
        piece.setFitWidth(CELL_SIZE);
        return piece;
    }

    public static ColumnConstraints horizontalCoordinateConfig() {
        ColumnConstraints colConfig = new ColumnConstraints(CELL_SIZE);
        colConfig.setHalignment(HPos.CENTER);
        return colConfig;
    }

    public static RowConstraints verticalCoordinateConfig() {
        RowConstraints rowConfig = new RowConstraints(CELL_SIZE);
        rowConfig.setValignment(VPos.CENTER);
        return rowConfig;
    }
}
