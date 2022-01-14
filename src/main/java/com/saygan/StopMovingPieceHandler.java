package com.saygan;

import static com.saygan.ChessUtils.coordToXy;
import static com.saygan.ChessUtils.isOutOfBorder;
import static com.saygan.ChessUtils.xyToCoord;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

public class StopMovingPieceHandler implements EventHandler<MouseEvent> {

    private ChessUI ui;
    private ChessEngine engine;

    StopMovingPieceHandler(ChessUI ui, ChessEngine engine) {
        this.ui = ui;
        this.engine = engine;
    }

    @Override
    public void handle(MouseEvent e) {
        ImageView piece = (ImageView) e.getTarget();

        String newCoord = xyToCoord(e.getX(), e.getY());
        piece.setCursor(Cursor.OPEN_HAND);
        piece.toBack();

        String coord;
        if (isOutOfBorder(e.getX(), e.getY()) || !engine.getPossibleMoves().contains(newCoord)) {
            coord = ui.currentCoord;
            engine.clearCaches();
        } else {
            coord = newCoord;
            engine.moveCompleted(coord);
            if(ui.pieces.remove(ui.currentCoord) == piece) {
                ui.pieces.put(coord, piece);
            }
        }

        int xy[] = coordToXy(coord);
        piece.setX(xy[0]);
        piece.setY(xy[1]);

        ui.hidePossibleMoves();
        ui.removeMoveTarget();

        if (engine.isInCheckMateState()) {
            ui.checkMate(engine.getWinner());
            engine.startNewGame();
        } else if (engine.isInCheckState()) {
            ui.declareCheck(engine.getAttackedSide());
        } else if (engine.isDraw()) {
            ui.declareDraw(engine.getDraw());
            engine.startNewGame();
        }
    }
}
