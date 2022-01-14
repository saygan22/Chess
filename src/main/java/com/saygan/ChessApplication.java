package com.saygan;

import java.util.HashMap;
import java.util.Map;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class ChessApplication extends Application {

    private ChessUI ui;
    private ChessEngine engine;
    private Map<String, EventHandler<MouseEvent>> eventHandlers;

    public ChessApplication() {
        eventHandlers = new HashMap<>();
        ui = new ChessUI(eventHandlers);
        engine = new ChessEngine(ui);
        eventHandlers.put("MOVE", new PieceMoveHandler(ui, engine));
        eventHandlers.put("START", new StartMovigPieceHandler(ui, engine));
        eventHandlers.put("STOP", new StopMovingPieceHandler(ui, engine));

        engine.startNewGame();
    }

    public void start(Stage stage) throws Exception {

        ui.setMainStage(stage);
        Scene scene = new Scene(ui.getRoot());

        stage.setResizable(false);
        stage.setTitle("Chess");
        stage.setScene(scene);
        stage.show();
    }
}
