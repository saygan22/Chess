package com.saygan;

import static com.saygan.ChessUtils.isOutOfBorder;
import static com.saygan.ChessUtils.isWithinXBorder;
import static com.saygan.ChessUtils.isWithinYBorder;
import static com.saygan.ChessUtils.xyToCoord;

import javafx.event.EventHandler;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

public class PieceMoveHandler implements EventHandler<MouseEvent> {

    private ChessUI ui;
    private ChessEngine engine;

    PieceMoveHandler(ChessUI ui, ChessEngine engine) {
        this.ui = ui;
        this.engine = engine;
    }

    @Override
    public void handle(MouseEvent e) {

        ImageView piece = (ImageView) e.getTarget();

        if (isWithinXBorder(e.getX())) {
            piece.setX(e.getX() - ui.dX);
        }

        if (isWithinYBorder(e.getY())) {
            piece.setY(e.getY() - ui.dY);
        }

        if (isOutOfBorder(e.getX(), e.getY())) {
            ui.replaceMoveTarget(ui.currentCoord);
            return;
        }

        String newCoord = xyToCoord(e.getX(), e.getY());

        if (!engine.getPossibleMoves().contains(newCoord)) {
            ui.replaceMoveTarget(ui.currentCoord);
            return;
        }

        if (!ui.targetCoord.equals(newCoord)) {
            ui.replaceMoveTarget(newCoord);
        }
    }
}
