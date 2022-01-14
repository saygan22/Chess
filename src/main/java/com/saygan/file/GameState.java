package com.saygan.file;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.saygan.Turn;

public class GameState {

    public static final GameState INITIAL_STATE = new GameState(Turn.WHITE);

    static {
        INITIAL_STATE.white = new HashMap<>();
        INITIAL_STATE.white.put("a2", "wP");
        INITIAL_STATE.white.put("b2", "wP");
        INITIAL_STATE.white.put("c2", "wP");
        INITIAL_STATE.white.put("d2", "wP");
        INITIAL_STATE.white.put("e2", "wP");
        INITIAL_STATE.white.put("f2", "wP");
        INITIAL_STATE.white.put("g2", "wP");
        INITIAL_STATE.white.put("h2", "wP");
        INITIAL_STATE.white.put("a1", "wR");
        INITIAL_STATE.white.put("b1", "wN");
        INITIAL_STATE.white.put("c1", "wB");
        INITIAL_STATE.white.put("d1", "wQ");
        INITIAL_STATE.white.put("e1", "wK");
        INITIAL_STATE.white.put("f1", "wB");
        INITIAL_STATE.white.put("g1", "wN");
        INITIAL_STATE.white.put("h1", "wR");
        INITIAL_STATE.white = Collections.unmodifiableMap(INITIAL_STATE.white);

        INITIAL_STATE.black = new HashMap<>();
        INITIAL_STATE.black.put("a7", "bP");
        INITIAL_STATE.black.put("b7", "bP");
        INITIAL_STATE.black.put("c7", "bP");
        INITIAL_STATE.black.put("d7", "bP");
        INITIAL_STATE.black.put("e7", "bP");
        INITIAL_STATE.black.put("f7", "bP");
        INITIAL_STATE.black.put("g7", "bP");
        INITIAL_STATE.black.put("h7", "bP");
        INITIAL_STATE.black.put("a8", "bR");
        INITIAL_STATE.black.put("b8", "bN");
        INITIAL_STATE.black.put("c8", "bB");
        INITIAL_STATE.black.put("d8", "bQ");
        INITIAL_STATE.black.put("e8", "bK");
        INITIAL_STATE.black.put("f8", "bB");
        INITIAL_STATE.black.put("g8", "bN");
        INITIAL_STATE.black.put("h8", "bR");
        INITIAL_STATE.black = Collections.unmodifiableMap(INITIAL_STATE.black);
        INITIAL_STATE.snapshots = Collections.emptyMap();

        INITIAL_STATE.whiteCastleConditions = new boolean[3];
        INITIAL_STATE.blackCastleConditions = new boolean[3];
    }

    private final Turn turn;
    private Turn underCheck;
    private int possibleDrawMovesCount;
    private Map<String, String> white;
    private Map<String, String> black;
    private boolean[] whiteCastleConditions;
    private boolean[] blackCastleConditions;
    private Map<String, Integer> snapshots;

    public GameState(Turn turn) {
        this.turn = turn;
    }

    public Turn getTurn() {
        return turn;
    }

    public void setUnderCheck(Turn underCheck) {
        this.underCheck = underCheck;
    }

    public Turn getUnderCheck() {
        return underCheck;
    }

    public void setPossibleDrawMovesCount(int possibleDrawMovesCount) {
        this.possibleDrawMovesCount = possibleDrawMovesCount;
    }

    public int getPossibleDrawMovesCount() {
        return possibleDrawMovesCount;
    }

    public void setWhite(Map<String, String> white) {
        this.white = white;
    }

    public Map<String, String> getWhite() {
        return white;
    }

    public void setBlack(Map<String, String> black) {
        this.black = black;
    }

    public Map<String, String> getBlack() {
        return black;
    }

    public void setWhiteCastleConditions(boolean[] whiteCastleConditions) {
        this.whiteCastleConditions = whiteCastleConditions;
    }

    public boolean[] getWhiteCastleConditions() {
        return whiteCastleConditions;
    }

    public void setBlackCastleConditions(boolean[] blackCastleConditions) {
        this.blackCastleConditions = blackCastleConditions;
    }

    public boolean[] getBlackCastleConditions() {
        return blackCastleConditions;
    }

    public void setSnapshots(Map<String, Integer> snapshots) {
        this.snapshots = snapshots;
    }

    public Map<String, Integer> getSnapshots() {
        return snapshots;
    }
}
