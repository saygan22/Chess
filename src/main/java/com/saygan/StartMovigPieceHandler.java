package com.saygan;

import static com.saygan.ChessUtils.xyToCoord;

import java.util.List;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

public class StartMovigPieceHandler implements EventHandler<MouseEvent> {

    private ChessUI ui;
    private ChessEngine engine;

    StartMovigPieceHandler(ChessUI ui, ChessEngine engine) {
        this.ui = ui;
        this.engine = engine;
    }

    @Override
    public void handle(MouseEvent e) {
        ImageView piece = (ImageView) e.getTarget();

        String startBoardCoord = xyToCoord(e.getX(), e.getY());

        engine.setFromCoord(startBoardCoord);

        ui.currentCoord = startBoardCoord;
        ui.calculateCursorOffsets(e.getX(), e.getY());
        ui.createMoveTarget();

        List<String> possibleMoves = engine.getPossibleMoves();

        ui.showPossibleMoves(possibleMoves);

        piece.setCursor(Cursor.CLOSED_HAND);
        piece.toFront();
    }
}
