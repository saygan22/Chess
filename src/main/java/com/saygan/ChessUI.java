package com.saygan;

import static com.saygan.ChessUtils.coordToXy;
import static com.saygan.ChessUtils.countCursorOffset;
import static com.saygan.ChessUtils.isEven;
import static com.saygan.Constants.BOARD_BORDER_PX;
import static com.saygan.Constants.BOARD_HEIGHT;
import static com.saygan.Constants.BOARD_HEIGHT_PX;
import static com.saygan.Constants.BOARD_PLACE_HEIGHT_PX;
import static com.saygan.Constants.BOARD_PLACE_WIDTH_PX;
import static com.saygan.Constants.BOARD_WIDTH;
import static com.saygan.Constants.BOARD_WIDTH_PX;
import static com.saygan.Constants.BOTTOM;
import static com.saygan.Constants.CELL_SIZE;
import static com.saygan.Constants.H_CENTER;
import static com.saygan.Constants.V_CENTER;
import static com.saygan.Constants.MESSAGE_AREA_SIZE;
import static com.saygan.Constants.COORDINATE_RULER_SIZE;
import static com.saygan.Constants.LEFT;
import static com.saygan.Constants.RIGHT;
import static com.saygan.Constants.UP;
import static com.saygan.UIFactory.blackCell;
import static com.saygan.UIFactory.createHorizontalCoordinateRuler;
import static com.saygan.UIFactory.createVerticalCoordinateRuler;
import static com.saygan.UIFactory.piece;
import static com.saygan.UIFactory.possibleMoveDot;
import static com.saygan.UIFactory.target;
import static com.saygan.UIFactory.whiteCell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ChessUI {

    private final Color LIGHT_GREY = new Color(0, 0, 0, 0.35);
    protected int dX;
    protected int dY;
    private BorderPane topLevelPane;
    protected GridPane boardLayoutGrid;
    protected Pane piecesLayer;
    protected Shape target;
    protected String currentCoord;
    protected String targetCoord;
    protected Map<String, ImageView> pieces;
    protected Set<ImageView> black;
    protected Set<ImageView> white;
    private FlowPane cellsLayer;
    private StackPane board;
    private Map<String, Circle> possibleMovesMarkers;
    private Node whiteMessage;
    private Node blackMessage;

    private ChessEngine engine;
    private Map<String, EventHandler<MouseEvent>> eventHandlers;
    private Stage mainStage;

    public ChessUI(Map<String, EventHandler<MouseEvent>> eventHandlers) {
        pieces = new HashMap<>();
        black = new HashSet<>();
        white = new HashSet<>();
        possibleMovesMarkers = new HashMap<>();
        this.eventHandlers = eventHandlers;
        prapareBoard();
    }

    private void prapareBoard() {
        createBoardLayoutGrid();
        createMenu();
        createCoordinateRulers();
        createBoard();
    }

    private void createBoardLayoutGrid() {
        boardLayoutGrid = new GridPane();
        boardLayoutGrid.setBackground(new Background(new BackgroundFill(Color.BROWN, null, null)));

        boardLayoutGrid.getRowConstraints().add(new RowConstraints(MESSAGE_AREA_SIZE));
        boardLayoutGrid.getRowConstraints().add(new RowConstraints(COORDINATE_RULER_SIZE));
        boardLayoutGrid.getRowConstraints().add(new RowConstraints(BOARD_PLACE_WIDTH_PX));
        boardLayoutGrid.getRowConstraints().add(new RowConstraints(COORDINATE_RULER_SIZE));
        boardLayoutGrid.getRowConstraints().add(new RowConstraints(MESSAGE_AREA_SIZE));

        boardLayoutGrid.getColumnConstraints().add(new ColumnConstraints(COORDINATE_RULER_SIZE));
        boardLayoutGrid.getColumnConstraints().add(new ColumnConstraints(BOARD_PLACE_HEIGHT_PX));
        boardLayoutGrid.getColumnConstraints().add(new ColumnConstraints(COORDINATE_RULER_SIZE));
    }

    private void createMenu() {
        MenuBar menuBar = new MenuBar();

        Menu menu = new Menu("Menu");

        MenuItem restart = new MenuItem("Restart");
        MenuItem exit = new MenuItem("Exit");
        restart.setOnAction(onRestart());
        exit.setOnAction(onExit());
        menu.getItems().addAll(restart, exit);

        Menu file = new Menu("File");
        MenuItem open = new MenuItem("Open");
        MenuItem save = new MenuItem("Save");
        open.setOnAction(onOpenFile());
        save.setOnAction(onSaveFile());
        file.getItems().addAll(open, save);

        menuBar.getMenus().addAll(menu, file);

        topLevelPane = new BorderPane();
        topLevelPane.setTop(menuBar);
        topLevelPane.setCenter(boardLayoutGrid);
    }

    private EventHandler<ActionEvent> onRestart() {
        return e -> engine.startNewGame();
    }

    private EventHandler<ActionEvent> onExit() {
        return e -> System.exit(0);
    }

    private EventHandler<ActionEvent> onOpenFile() {
        return e -> {
            FileChooser fileChooser = getFileChooser("Open saved game");
            engine.loadGame(fileChooser.showOpenDialog(mainStage));
        };
    }

    private EventHandler<ActionEvent> onSaveFile() {
        return e -> {
            FileChooser fileChooser = getFileChooser("Save current game");
            fileChooser.setInitialFileName(engine.suggestSaveGameFileName());
            engine.saveGame(fileChooser.showSaveDialog(mainStage));
        };
    }

    private FileChooser getFileChooser(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialDirectory(engine.resolveInitialDirectory());
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Chess game file", "*.chs"));
        return fileChooser;
    }

    private void createCoordinateRulers() {
        boardLayoutGrid.add(createHorizontalCoordinateRuler(), H_CENTER, UP);
        boardLayoutGrid.add(createHorizontalCoordinateRuler(), H_CENTER, BOTTOM);
        boardLayoutGrid.add(createVerticalCoordinateRuler(), LEFT, V_CENTER);
        boardLayoutGrid.add(createVerticalCoordinateRuler(), RIGHT, V_CENTER);
    }

    private void createBoard() {
        piecesLayer = new Pane();
        cellsLayer = new FlowPane();
        cellsLayer.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                new BorderWidths(BOARD_BORDER_PX))));
        cellsLayer.setMinSize(BOARD_WIDTH_PX, BOARD_HEIGHT_PX);
        for (int col = 0; col < BOARD_HEIGHT; ++col) {
            for (int row = 0; row < BOARD_WIDTH; ++row) {
                Node cell = null;
                if (isEven(row)) {
                    cell = isEven(col) ? whiteCell() : blackCell();
                } else {
                    cell = isEven(col) ? blackCell() : whiteCell();
                }
                Circle dot = possibleMoveDot();
                dot.setCenterX(row * CELL_SIZE + CELL_SIZE / 2);
                dot.setCenterY(col * CELL_SIZE + CELL_SIZE / 2);
                piecesLayer.getChildren().add(dot);
                cellsLayer.getChildren().add(cell);
                String coord = new String(new char[] { (char) ('a' + row), (char) ('1' + (7 - col)) });
                possibleMovesMarkers.put(coord, dot);
            }
        }
        board = new StackPane();
        board.getChildren().add(cellsLayer);
        board.getChildren().add(piecesLayer);
        boardLayoutGrid.add(board, H_CENTER, V_CENTER);
    }

    protected void showPossibleMoves(List<String> possibleMoves) {
        for (String coord : possibleMoves) {
            possibleMovesMarkers.get(coord).setFill(LIGHT_GREY);
        }
    }

    protected void hidePossibleMoves() {
        for (Circle dot : possibleMovesMarkers.values()) {
            dot.setFill(Color.TRANSPARENT);
            dot.toBack();
        }
    }

    protected void calculateCursorOffsets(double x, double y) {
        dX = countCursorOffset(x);
        dY = countCursorOffset(y);
    }

    protected void createMoveTarget() {
        int[] xy = coordToXy(currentCoord);
        targetCoord = currentCoord;
        target = target();
        target.setLayoutX(xy[0] + 2);
        target.setLayoutY(xy[1] + 2);
        piecesLayer.getChildren().add(target);
    }

    protected void replaceMoveTarget(String coord) {
        targetCoord = coord;
        int[] xy = coordToXy(targetCoord);
        target.setLayoutX(xy[0] + 2);
        target.setLayoutY(xy[1] + 2);
    }

    protected void removeMoveTarget() {
        piecesLayer.getChildren().remove(target);
    }

    protected void putNewPieceOnBoard(String coord, String pieceName) {
        ImageView piece = piece(pieceName);
        int[] xy = coordToXy(coord);
        piece.setX(xy[0]);
        piece.setY(xy[1]);
        piecesLayer.getChildren().add(piece);
        pieces.put(coord, piece);
        if (isWhite(pieceName)) {
            white.add(piece);
        } else {
            black.add(piece);
        }
    }

    public Parent getRoot() {
        return topLevelPane;
    }

    public void removePiece(String coordTo) {
        ImageView piece = pieces.remove(coordTo);
        white.remove(piece);
        black.remove(piece);
        piecesLayer.getChildren().remove(piece);
    }

    public void setTurn(Turn turn) {
        if (Turn.WHITE == turn) {
            deactivateSide(black);
            activateSide(white);
        } else if (Turn.BLACK == turn) {
            deactivateSide(white);
            activateSide(black);
        }
    }

    private void activateSide(Set<ImageView> pieces) {
        for (ImageView piece : pieces) {
            activatePiece(piece);
        }
    }

    private void deactivateSide(Set<ImageView> pieces) {
        for (ImageView piece : pieces) {
            deactivatePiece(piece);
        }
    }

    private void activatePiece(ImageView piece) {
        piece.setCursor(Cursor.OPEN_HAND);
        piece.addEventHandler(MouseEvent.MOUSE_PRESSED, eventHandlers.get("START"));
        piece.addEventHandler(MouseEvent.MOUSE_RELEASED, eventHandlers.get("STOP"));
        piece.addEventHandler(MouseEvent.MOUSE_DRAGGED, eventHandlers.get("MOVE"));
    }

    private void deactivatePiece(ImageView piece) {
        piece.setCursor(Cursor.DEFAULT);
        piece.removeEventHandler(MouseEvent.MOUSE_PRESSED, eventHandlers.get("START"));
        piece.removeEventHandler(MouseEvent.MOUSE_RELEASED, eventHandlers.get("STOP"));
        piece.removeEventHandler(MouseEvent.MOUSE_DRAGGED, eventHandlers.get("MOVE"));
    }

    private boolean isWhite(String pieceNmae) {
        return pieceNmae.startsWith("w");
    }

    public void clearMessages() {
        boardLayoutGrid.getChildren().remove(blackMessage);
        boardLayoutGrid.getChildren().remove(whiteMessage);
    }

    public void declareCheck(Turn attackedSide) {
        clearMessages();
        if(Turn.WHITE == attackedSide) {
            whiteMessage = UIFactory.messageLabel("Check");
            boardLayoutGrid.add(whiteMessage, H_CENTER, 4);
        } else if (Turn.BLACK == attackedSide){
            blackMessage = UIFactory.messageLabel("Check");
            boardLayoutGrid.add(blackMessage, H_CENTER, 0);
        }
    }

    public void checkMate(Turn winner) {
        clearMessages();
        if(Turn.WHITE == winner) {
            blackMessage = UIFactory.messageLabel("Checkmate!");
            boardLayoutGrid.add(blackMessage, H_CENTER, 0);
        } else if (Turn.BLACK == winner){
            whiteMessage = UIFactory.messageLabel("Checkmate!");
            boardLayoutGrid.add(whiteMessage, H_CENTER, 4);
        }

        Alert alert = new Alert(AlertType.WARNING, winner + " won!", ButtonType.OK, ButtonType.FINISH);
        alert.setTitle("Game Over!");
        alert.showAndWait()
             .filter(button -> button == ButtonType.FINISH)
             .ifPresent(button -> System.exit(0));
    }

    public String askForPwanPromotion(Turn turn) {

        Stage stage = new Stage();
        FlowPane root = new FlowPane();
        List<String> pieceNameBox = new ArrayList<>();

        for(char pieceName : "QRBN".toCharArray()) {
            char color = turn == Turn.WHITE? 'w' : 'b';
            String pieceFullName = new String(new char[] {color, pieceName});
            Node piece = piece(pieceFullName);
            root.getChildren().add(piece);
            piece.setCursor(Cursor.OPEN_HAND);
            piece.setOnMouseClicked(e -> {
                pieceNameBox.add(pieceFullName);
                stage.close();
            });
        }

        stage.setTitle("Select promotion piece");
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(root, 400, 100));
        stage.showAndWait();

        return pieceNameBox.iterator().next();
    }

    public void move(String from, String to) {
        ImageView piece = pieces.remove(from);
        int xy[] = coordToXy(to);
        piece.setX(xy[0]);
        piece.setY(xy[1]);
        pieces.put(to, piece);
    }

    public void declareDraw(Draw draw) {
        clearMessages();

        blackMessage = UIFactory.messageLabel("Draw! " + draw.name().replace('_', ' '));
        boardLayoutGrid.add(blackMessage, H_CENTER, 0);
        whiteMessage = UIFactory.messageLabel("Draw! " + draw.name().replace('_', ' '));
        boardLayoutGrid.add(whiteMessage, H_CENTER, 4);

        Alert alert = new Alert(AlertType.INFORMATION, "It's a draw!", ButtonType.OK, ButtonType.FINISH);
        alert.setTitle("Game Over!");
        alert.showAndWait()
             .filter(button -> button == ButtonType.FINISH)
             .ifPresent(button -> System.exit(0));
    }

    public void setEngine(ChessEngine engine) {
        this.engine = engine;
    }

    public void setMainStage(Stage stage) {
        mainStage = stage;
    }
}
