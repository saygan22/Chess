package com.saygan;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.saygan.file.ChessFileManager;
import com.saygan.file.GameState;
import com.saygan.file.PlainTextChessFileManager;

public class ChessEngine {

    private static final String SAVES_DIRECTORY = "saves";

    private static final String SNAPSHOT_EMPTY_CELL = "..";
    private static final int MAX_REPEAT_POSITION = 3;
    private static final int LONG_GAME_MOVES_COUNT = 50 * 2;
    private static final char BLACK_IN_PASSANT_TARGET_CELL = '6';
    private static final char WHITE_IN_PASSANT_TARGET_CELL = '3';
    private static final int START_WHITE_PAWN_RANK = 1;
    private static final int START_BLACK_PAWN_RANK = 6;

    private static final int LEFT_ROOK = 0;
    private static final int KING = 1;
    private static final int RIGHT_ROOK = 2;

    private final int[] LONG_PART_OF_KNIGHT_MOVES = new int[] { 2, -2 };
    private final int[] SHORT_PART_OF_KNIGHT_MOVES = new int[] { 1, -1 };

    private static final int MAX = 7;
    private Turn turn;
    private String coordFrom;
    private String currentPieceMove;
    private String inPassantTargetCell;
    private String inPassantPossibleVictim;
    private Turn winner;
    private Turn underCheck;
    private int possibleDrawMovesCount;

    private ChessUI ui;

    private Map<Turn, Direction> pawnMoveDirections = new HashMap<>();
    private Map<Turn, List<Direction>> pawnFightDirections = new HashMap<>();
    private Map<String, String> white;
    private Map<String, String> black;
    private List<String> possibleMovesCache;
    private Map<String, Integer> boardSnapshotsCount;

    private boolean[] whiteCastleConditions;
    private boolean[] blackCastleConditions;

    private String[][] boardNavigator;

    private Draw draw;

    private ChessFileManager fileManager;

    public ChessEngine(ChessUI ui) {

        pawnMoveDirections.put(Turn.WHITE, Direction.UP);
        pawnMoveDirections.put(Turn.BLACK, Direction.DOWN);

        pawnFightDirections.put(Turn.WHITE, Arrays.asList(Direction.UP_LEFT, Direction.UP_RIGHT));
        pawnFightDirections.put(Turn.BLACK, Arrays.asList(Direction.DOWN_LEFT, Direction.DOWN_RIGHT));

        this.ui = ui;
        ui.setEngine(this);

        white = new LinkedHashMap<>();
        black = new LinkedHashMap<>();
        possibleMovesCache = new ArrayList<>();
        boardSnapshotsCount = new HashMap<>();

        boardNavigator = new String[8][];
        for (int col = 0; col < boardNavigator.length; ++col) {
            boardNavigator[col] = new String[8];
            for (int row = 0; row < boardNavigator[col].length; ++row) {
                boardNavigator[col][row] = new String(new char[] { (char) ('a' + col), (char) ('1' + row) });
            }
        }

        fileManager = new PlainTextChessFileManager();
    }

    public void startNewGame() {
        doStartGame(GameState.INITIAL_STATE);
    }

    private void doStartGame(GameState state) {

        possibleMovesCache.clear();
        ui.clearMessages();
        draw = null;
        winner = null;
        underCheck = state.getUnderCheck();
        possibleDrawMovesCount = state.getPossibleDrawMovesCount();

        removePieces(white);
        removePieces(black);
        boardSnapshotsCount.clear();
        boardSnapshotsCount.putAll(state.getSnapshots());

        whiteCastleConditions = state.getWhiteCastleConditions();
        blackCastleConditions = state.getBlackCastleConditions();

        white.clear();
        black.clear();

        white.putAll(state.getWhite());
        black.putAll(state.getBlack());

        for (Map.Entry<String, String> entry : white.entrySet()) {
            preparePiece(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, String> entry : black.entrySet()) {
            preparePiece(entry.getKey(), entry.getValue());
        }

        turn = state.getTurn();
        ui.setTurn(turn);

        if(underCheck != null) {
            ui.declareCheck(underCheck);
        }
    }

    private void preparePiece(String coord, String pieceName) {
        ui.putNewPieceOnBoard(coord, pieceName);
    }

    public void setFromCoord(String startBoardCoord) {
        coordFrom = startBoardCoord;
        currentPieceMove = resolveTurning().get(coordFrom);
    }

    public List<String> getPossibleMoves() {
        if (!possibleMovesCache.isEmpty()) {
            return possibleMovesCache;
        }

        boolean[] castleConditions = resolveTurningCastleConditions(turn);
        Map<String, String> turning = resolveTurning();
        Map<String, String> waiting = resolveWaiting();

        List<String> possibleMoves = doGetPossibleMoves(turn, coordFrom, turning, waiting);
        List<String> cellsToRemove = new ArrayList<>();

        for(Iterator<String> it = possibleMoves.iterator(); it.hasNext();) {
            String possibleMove = it.next();
            Map<String, String> turningCopy = new LinkedHashMap<>(turning);
            Map<String, String> waitingCopy = new LinkedHashMap<>(waiting);
            if (waitingCopy.containsKey(possibleMove)) {
                waitingCopy.remove(possibleMove);
            }
            turningCopy.put(possibleMove, turningCopy.remove(coordFrom));

            if(checkForCheck(invertTurn(turn), waitingCopy, turningCopy)) {
                it.remove();
                if(isKing(currentPieceMove) && !castleConditions[KING]) {
                    if(leftCellForKingUnderAttack(turn, possibleMove)) {
                        cellsToRemove.add(leftCastleCellForKing(turn));
                    }
                    if(rightCellForKingUnderAttack(turn, possibleMove)) {
                        cellsToRemove.add(rightCastleCellForKing(turn));
                    }
                }
            }
        }

        possibleMoves.removeAll(cellsToRemove);
        possibleMovesCache.addAll(possibleMoves);
        return possibleMovesCache;
    }

    private String rightCastleCellForKing(Turn turn) {
        return Turn.WHITE == turn? "g1" : "g8";
    }

    private String leftCastleCellForKing(Turn turn) {
        return Turn.WHITE == turn? "c1" : "c8";
    }

    private String rightCellForKing(Turn turn) {
        return Turn.WHITE == turn? "f1" : "f8";
    }

    private String leftCellForKing(Turn turn) {
        return Turn.WHITE == turn? "d1" : "d8";
    }

    private String rightRookStartCell(Turn turn) {
        return Turn.WHITE == turn? "h1" : "h8";
    }

    private String leftRookStartCell(Turn turn) {
        return Turn.WHITE == turn? "a1" : "a8";
    }

    private String waitingRightRookStartCell(Turn turn) {
        return Turn.WHITE == turn? "h8" : "h1";
    }

    private String waitingLeftRookStartCell(Turn turn) {
        return Turn.WHITE == turn? "a8" : "a1";
    }

    private boolean rightCellForKingUnderAttack(Turn turn, String possibleMove) {
        return possibleMove.equals(rightCellForKing(turn));
    }

    private boolean leftCellForKingUnderAttack(Turn turn, String possibleMove) {
        return possibleMove.equals(leftCellForKing(turn));
    }

    private boolean[] resolveTurningCastleConditions(Turn turn) {
        return Turn.WHITE == turn? whiteCastleConditions : blackCastleConditions;
    }

    private boolean[] resolveWaitingCastleConditions(Turn turn) {
        return Turn.WHITE == turn? blackCastleConditions : whiteCastleConditions;
    }

    private List<String> doGetPossibleMoves(Turn turn, String coordFrom, Map<String, String> turning, Map<String, String> waiting) {
        String piece = resolveCurrentPiece(coordFrom, turning);
        List<String> possibleMoves = new ArrayList<>();
        if (isPawn(piece)) {
            possibleMoves.addAll(pawnMovesSet(turn, coordFrom, turning, waiting));
        } else if (isKnight(piece)) {
            possibleMoves.addAll(knightMovesSet(coordFrom, turning, waiting));
        } else if (isRook(piece)) {
            possibleMoves.addAll(rookMovesSet(coordFrom, turning, waiting));
        } else if (isBishop(piece)) {
            possibleMoves.addAll(bishopMovesSet(coordFrom, turning, waiting));
        } else if (isQueen(piece)) {
            possibleMoves.addAll(queenMovesSet(coordFrom, turning, waiting));
        } else if (isKing(piece)) {
            possibleMoves.addAll(kingMovesSet(turn, coordFrom, turning, waiting));
        }

        return possibleMoves;
    }

    private List<String> pawnMovesSet(Turn turn, String coordFrom, Map<String, String> turning, Map<String, String> waiting) {
        List<String> possibleMoves = new ArrayList<>();
        possibleMoves.addAll(moveForPawn(resolvePawnMoveDirection(turn), coordFrom, turning, waiting));
        possibleMoves.addAll(fightForPawn(resolvePawnFightDirections(turn), coordFrom, waiting));
        return possibleMoves;
    }

    private List<String> rookMovesSet(String coordFrom, Map<String, String> turning, Map<String, String> waiting) {
        List<String> possibleMoves = new ArrayList<>();
        possibleMoves.addAll(moves(Direction.UP, coordFrom, MAX, turning, waiting));
        possibleMoves.addAll(moves(Direction.DOWN, coordFrom, MAX, turning, waiting));
        possibleMoves.addAll(moves(Direction.LEFT, coordFrom, MAX, turning, waiting));
        possibleMoves.addAll(moves(Direction.RIGHT, coordFrom, MAX, turning, waiting));
        return possibleMoves;
    }

    private List<String> knightMovesSet(String coordFrom, Map<String, String> turning, Map<String, String> waiting) {
        List<String> possibleMoves = new ArrayList<>();
        int letter = coordLetterPosition(coordFrom);
        int digit = coordDigitPosition(coordFrom);

        for (int longPart : LONG_PART_OF_KNIGHT_MOVES) {
            int newDigit = digit + longPart;
            for (int shortPart : SHORT_PART_OF_KNIGHT_MOVES) {
                int newLetter = letter + shortPart;
                if (isWithinBoardNavigator(newLetter) && isWithinBoardNavigator(newDigit)) {
                    String cellCoord = boardNavigator[newLetter][newDigit];
                    if(hasPieceCollision(cellCoord, possibleMoves, turning, waiting)) {
                        continue;
                    }
                    possibleMoves.add(cellCoord);
                }
            }
        }

        for (int longPart : LONG_PART_OF_KNIGHT_MOVES) {
            int newLetter = letter + longPart;
            for (int shortPart : SHORT_PART_OF_KNIGHT_MOVES) {
                int newDigit = digit + shortPart;
                if (isWithinBoardNavigator(newLetter) && isWithinBoardNavigator(newDigit)) {
                    String cellCoord = boardNavigator[newLetter][newDigit];
                    if(hasPieceCollision(cellCoord, possibleMoves, turning, waiting)) {
                        continue;
                    }
                    possibleMoves.add(cellCoord);
                }
            }
        }

        return possibleMoves;
    }

    private List<String> bishopMovesSet(String coordFrom, Map<String, String> turning, Map<String, String> waiting) {
        List<String> possibleMoves = new ArrayList<>();
        possibleMoves.addAll(moves(Direction.UP_LEFT, coordFrom, MAX, turning, waiting));
        possibleMoves.addAll(moves(Direction.UP_RIGHT, coordFrom, MAX, turning, waiting));
        possibleMoves.addAll(moves(Direction.DOWN_LEFT, coordFrom, MAX, turning, waiting));
        possibleMoves.addAll(moves(Direction.DOWN_RIGHT, coordFrom, MAX, turning, waiting));
        return possibleMoves;
    }

    private List<String> queenMovesSet(String coordFrom, Map<String, String> turning, Map<String, String> waiting) {
        List<String> possibleMoves = new ArrayList<>();
        possibleMoves.addAll(moves(Direction.UP, coordFrom, MAX, turning, waiting));
        possibleMoves.addAll(moves(Direction.DOWN, coordFrom, MAX, turning, waiting));
        possibleMoves.addAll(moves(Direction.LEFT, coordFrom, MAX, turning, waiting));
        possibleMoves.addAll(moves(Direction.RIGHT, coordFrom, MAX, turning, waiting));
        possibleMoves.addAll(moves(Direction.UP_LEFT, coordFrom, MAX, turning, waiting));
        possibleMoves.addAll(moves(Direction.UP_RIGHT, coordFrom, MAX, turning, waiting));
        possibleMoves.addAll(moves(Direction.DOWN_LEFT, coordFrom, MAX, turning, waiting));
        possibleMoves.addAll(moves(Direction.DOWN_RIGHT, coordFrom, MAX, turning, waiting));
        return possibleMoves;
    }

    private List<String> kingMovesSet(Turn turn, String coordFrom, Map<String, String> turning, Map<String, String> waiting) {
        List<String> possibleMoves = new ArrayList<>();
        possibleMoves.addAll(moves(Direction.UP, coordFrom, 1, turning, waiting));
        possibleMoves.addAll(moves(Direction.DOWN, coordFrom, 1, turning, waiting));
        possibleMoves.addAll(moves(Direction.UP_LEFT, coordFrom, 1, turning, waiting));
        possibleMoves.addAll(moves(Direction.UP_RIGHT, coordFrom, 1, turning, waiting));
        possibleMoves.addAll(moves(Direction.DOWN_LEFT, coordFrom, 1, turning, waiting));
        possibleMoves.addAll(moves(Direction.DOWN_RIGHT, coordFrom, 1, turning, waiting));

        boolean[] castleCondition = resolveTurningCastleConditions(turn);
        int leftMoves = !castleCondition[KING] && !castleCondition[LEFT_ROOK] && wayToLeftRookIsClear(turn, turning, waiting) && turn != underCheck? 2 : 1;
        int rightMoves = !castleCondition[KING] && !castleCondition[RIGHT_ROOK] && wayToRightRookIsClear(turn, turning, waiting) && turn != underCheck? 2 : 1;
        possibleMoves.addAll(moves(Direction.LEFT, coordFrom, leftMoves, turning, waiting));
        possibleMoves.addAll(moves(Direction.RIGHT, coordFrom, rightMoves, turning, waiting));
        return possibleMoves;
    }

    private boolean wayToRightRookIsClear(Turn turn, Map<String, String> turning, Map<String, String> waiting) {
        return areEmpty(turning, waiting, wayToRightCastle(turn));
    }

    private boolean wayToLeftRookIsClear(Turn turn, Map<String, String> turning, Map<String, String> waiting) {
        return areEmpty(turning, waiting, wayToLeftCastle(turn));
    }

    private boolean areEmpty(Map<String, String> turning, Map<String, String> waiting, String ... cells) {
        for(String cell : cells) {
            if(turning.containsKey(cell) || waiting.containsKey(cell)) {
                return false;
            }
        }
        return true;
    }

    private String[] wayToLeftCastle(Turn turn) {
        return Turn.WHITE == turn? new String[] {"b1", "c1", "d1"} : new String[] {"b8", "c8", "d8"};
    }

    private String[] wayToRightCastle(Turn turn) {
        return Turn.WHITE == turn? new String[] {"f1", "g1"} : new String[] {"f8", "g8"};
    }

    // -------------------------------------------------- //

    private List<String> moves(Direction direction, String coordFrom, int cellsCount, Map<String, String> turning, Map<String, String> waiting) {
        List<String> possibleMoves = new ArrayList<>();
        int letter = coordLetterPosition(coordFrom);
        int digit = coordDigitPosition(coordFrom);
        for (int i = 0; i < cellsCount; ++i) {
            letter = resolveHorizontalDelta(direction, letter);
            digit = resolveVerticalDelta(direction, digit);
            if (isWithinBoardNavigator(letter) && isWithinBoardNavigator(digit)) {
                String cellCoord = boardNavigator[letter][digit];
                if (hasPieceCollision(cellCoord, possibleMoves, turning, waiting)) {
                    break;
                }
                possibleMoves.add(cellCoord);
            }
        }
        return possibleMoves;
    }

    private boolean hasPieceCollision(String cellCoord, List<String> possibleMoves, Map<String, String> turning, Map<String, String> waiting) {

        if (turning.containsKey(cellCoord)) {
            return true;
        }

        if (waiting.containsKey(cellCoord)) {
            possibleMoves.add(cellCoord);
            return true;
        }

        return false;
    }

    private List<String> moveForPawn(Direction direction, String coordFrom, Map<String, String> white, Map<String, String> black) {
        List<String> possibleMoves = new ArrayList<>();

        int letter = coordLetterPosition(coordFrom);
        int digit = coordDigitPosition(coordFrom);

        int cellsCount = pawnsMoveCells(digit, direction);

        for (int i = 0; i < cellsCount; ++i) {
            digit = resolveVerticalDelta(direction, digit);
            if (isWithinBoardNavigator(digit)) {
                String cellCoord = boardNavigator[letter][digit];
                if (white.containsKey(cellCoord) || black.containsKey(cellCoord)) {
                    break;
                }
                possibleMoves.add(cellCoord);
            }
        }
        return possibleMoves;
    }

    private List<String> fightForPawn(List<Direction> directions, String coordFrom, Map<String, String> thatSide) {
        List<String> possibleMoves = new ArrayList<>();
        for(Direction direction : directions) {
            int digit = resolveVerticalDelta(direction, coordDigitPosition(coordFrom));
            int letter = resolveHorizontalDelta(direction, coordLetterPosition(coordFrom));
            if (isWithinBoardNavigator(letter) && isWithinBoardNavigator(digit)) {
                String cellCoord = boardNavigator[letter][digit];
                if (thatSide.containsKey(cellCoord) || cellCoord.equals(inPassantTargetCell)) {
                    possibleMoves.add(cellCoord);
                }
            }
        }
        return possibleMoves;
    }

    private boolean isWithinBoardNavigator(int digit) {
        return digit >= 0 && digit <= 7;
    }

    private int coordLetterPosition(String coord) {
        return coord.charAt(0) - 'a';
    }

    private int coordDigitPosition(String coord) {
        return coord.charAt(1) - '1';
    }

    public void moveCompleted(String coordTo) {

        Map<String, String> turning = resolveTurning();
        Map<String, String> waiting = resolveWaiting();
        boolean[] turningCastleCondition = resolveTurningCastleConditions(turn);
        boolean[] waitingCastleCondition = resolveWaitingCastleConditions(turn);

        if (coordTo.equals(inPassantTargetCell) && isPawn(currentPieceMove)) {
            white.remove(inPassantPossibleVictim);
            black.remove(inPassantPossibleVictim);
            ui.removePiece(inPassantPossibleVictim);
        }

        inPassantTargetCell = null;
        inPassantPossibleVictim = null;

        if (waiting.containsKey(coordTo)) {
            waiting.remove(coordTo);
            ui.removePiece(coordTo);
            possibleDrawMovesCount = 0;
        }
        turning.put(coordTo, turning.remove(coordFrom));

        if(isPawn(currentPieceMove)) {
            possibleDrawMovesCount = 0;

            if (pawnMadeDoubleMove(turn, coordFrom, coordTo)) {
                inPassantTargetCell = formInPassantTargetCellCoord(turn, coordFrom);
                inPassantPossibleVictim = coordTo;
            }

            if(isLastHorizontal(turn, coordTo)) {

                String pieceName = ui.askForPwanPromotion(turn);

                turning.put(coordTo, pieceName);
                ui.removePiece(coordFrom);
                ui.putNewPieceOnBoard(coordTo, pieceName);
            }
        }

        if(isKing(currentPieceMove)) {
            if(!turningCastleCondition[KING]) {
                turningCastleCondition[KING] = true;
                if(rightCastleCellForKing(turn).equals(coordTo)) {
                    turning.put(rightCellForKing(turn), turning.remove(rightRookStartCell(turn)));
                    ui.move(rightRookStartCell(turn), rightCellForKing(turn));
                }
                if(leftCastleCellForKing(turn).equals(coordTo)) {
                    turning.put(leftCellForKing(turn), turning.remove(leftRookStartCell(turn)));
                    ui.move(leftRookStartCell(turn), leftCellForKing(turn));
                }
            }
        }

        if(isRook(currentPieceMove)) {
            if(!turningCastleCondition[LEFT_ROOK] && coordFrom.equals(leftRookStartCell(turn))) {
                turningCastleCondition[LEFT_ROOK] = true;
            }
            if(!turningCastleCondition[RIGHT_ROOK] && coordFrom.equals(rightRookStartCell(turn))) {
                turningCastleCondition[RIGHT_ROOK] = true;
            }
        }

        if(waitingLeftRookStartCell(turn).contains(coordTo) && !waitingCastleCondition[LEFT_ROOK]) {
            waitingCastleCondition[LEFT_ROOK] = true;
        }

        if(waitingRightRookStartCell(turn).contains(coordTo) && !waitingCastleCondition[RIGHT_ROOK]) {
            waitingCastleCondition[RIGHT_ROOK] = true;
        }

        if(checkForCheck(turn, turning, waiting)) {
            underCheck = invertTurn(turn);
            winner = turn;
            for(String waitingPieceCoord : waiting.keySet()) {
                List<String> possibleMoves = doGetPossibleMoves(underCheck, waitingPieceCoord, waiting, turning);
                for(String possibleMove : possibleMoves) {
                    Map<String, String> turningCopy = new LinkedHashMap<>(turning);
                    Map<String, String> waitingCopy = new LinkedHashMap<>(waiting);
                    if (turningCopy.containsKey(possibleMove)) {
                        turningCopy.remove(possibleMove);
                    }
                    waitingCopy.put(possibleMove, waitingCopy.remove(waitingPieceCoord));
                    if(!checkForCheck(turn, turningCopy, waitingCopy)) {
                        winner = null;
                    }
                }
            }
        } else {
            underCheck = null;
            ui.clearMessages();
        }

        checkIfItsALongGame();
        checkIfThereIsStalemate(turning, waiting);
        checkIfThereIsPositionRepeats();
        checkIfThereIsNotEnoughMaterial();

        changeTurn();
        possibleMovesCache.clear();
    }

    private void checkIfItsALongGame() {
        if(possibleDrawMovesCount++ == LONG_GAME_MOVES_COUNT) {
            draw = Draw.LONG_GAME;
        }
    }

    private void checkIfThereIsStalemate(Map<String, String> turning, Map<String, String> waiting) {
        if(noValidMoves(invertTurn(turn), waiting, turning)) {
            draw = Draw.STALEMATE;
        }
    }

    private void checkIfThereIsPositionRepeats() {
        String boardSnapshot = createBoardSnapshot();

        if(boardSnapshotsCount.containsKey(boardSnapshot)) {
            int newCount = boardSnapshotsCount.get(boardSnapshot) + 1;
            if(newCount == MAX_REPEAT_POSITION) {
                draw = Draw.REPEAT_POSITION;
            } else {
                boardSnapshotsCount.put(boardSnapshot, newCount);
            }
        } else {
            boardSnapshotsCount.put(boardSnapshot, 1);
        }
    }

    private void checkIfThereIsNotEnoughMaterial() {
        if(noPawns() &&
           noQueens() &&
           noRooks() &&
           noBothBishops() &&
           noBothKnights() &&
           noOneBishopAndOneKnight()) {
            draw = Draw.NO_ENOUGH_MATERIAL;
        }
    }

    private String formInPassantTargetCellCoord(Turn turn, String coordFrom) {
        return new String(new char[] { coordFrom.charAt(0), resolveInPassantTargetCell(turn) });
    }

    private char resolveInPassantTargetCell(Turn turn) {
        return Turn.WHITE == turn? WHITE_IN_PASSANT_TARGET_CELL : BLACK_IN_PASSANT_TARGET_CELL;
    }

    private boolean pawnMadeDoubleMove(Turn turn, String coordFrom, String coordTo) {
        if(coordDigitPosition(coordFrom) != resolveStartPawnRank(turn)) {
            return false;
        }

        if(Math.abs(coordDigitPosition(coordTo) - coordDigitPosition(coordFrom)) != 2) {
            return false;
        }

        return true;
    }

    private int resolveStartPawnRank(Turn turn) {
        return Turn.WHITE == turn? START_WHITE_PAWN_RANK : START_BLACK_PAWN_RANK;
    }

    private boolean noOneBishopAndOneKnight() {
        return !((hasPiecesIn(white, this::isBishop, 1) && hasPiecesIn(white, this::isKnight, 1)) ||
                 (hasPiecesIn(black, this::isBishop, 1) && hasPiecesIn(black, this::isKnight, 1)));
    }

    private boolean noBothKnights() {
        return !(hasPiecesIn(white, this::isKnight, 2) || hasPiecesIn(black, this::isKnight, 2));
    }

    private boolean noBothBishops() {
        return !(hasPiecesIn(white, this::isBishop, 2) || hasPiecesIn(black, this::isBishop, 2));
    }

    private boolean noRooks() {
        return hasPiecesIn(white, this::isRook, 0) && hasPiecesIn(black, this::isRook, 0);
    }

    private boolean noQueens() {
        return hasPiecesIn(white, this::isQueen, 0) && hasPiecesIn(black, this::isQueen, 0);
    }

    private boolean noPawns() {
        return hasPiecesIn(white, this::isPawn, 0) && hasPiecesIn(black, this::isPawn, 0);
    }

    private boolean hasPiecesIn(Map<String, String> side, Predicate<String> pieceType, int targetCount) {
        int count = 0;
        for(String pieceName : side.values()) {
            if(pieceType.test(pieceName)) {
                count++;
            }
        }
        return targetCount == count;
    }

    private String createBoardSnapshot() {

        StringBuilder sb = new StringBuilder();
        for (int col = 0; col < Constants.BOARD_WIDTH; ++col) {
            for (int row = 0; row < Constants.BOARD_HEIGHT; ++row) {
                String coord = new String(new char[] { (char) ('a' + col), (char) ('1' + row) });

                String pieceName = white.get(coord);
                if(pieceName == null) {
                    pieceName = black.get(coord);
                }

                sb.append(pieceName == null? SNAPSHOT_EMPTY_CELL : pieceName);
            }
        }

        return sb.toString();
    }

    private boolean noValidMoves(Turn turn, Map<String, String> thisSide, Map<String, String> thatSide) {

        for(String pieceCoord : thisSide.keySet()) {
            List<String> possibleMoves = doGetPossibleMoves(turn, pieceCoord, thisSide, thatSide);

            for(String possibleMove : possibleMoves) {
                Map<String, String> thisSideCopy = new LinkedHashMap<>(thisSide);
                Map<String, String> thatSideCopy = new LinkedHashMap<>(thatSide);

                if (thatSideCopy.containsKey(possibleMove)) {
                    thatSideCopy.remove(possibleMove);
                }
                thisSideCopy.put(possibleMove, thisSideCopy.remove(pieceCoord));

                if (!checkForCheck(turn, thatSideCopy, thisSideCopy)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isLastHorizontal(Turn turn, String coordTo) {
        char lastHorizontal = (Turn.WHITE == turn)? '8' : '1';
        return coordTo.charAt(1) == lastHorizontal;
    }

    private boolean checkForCheck(Turn turn, Map<String, String> attackingSide, Map<String, String> defendingSide) {
        String defendingKingCoord = null;
        for (String defendingPieceCoord : defendingSide.keySet()) {
            String defendingPiece = defendingSide.get(defendingPieceCoord);
            if (isKing(defendingPiece)) {
                defendingKingCoord = defendingPieceCoord;
                break;
            }
        }

        return isCellIsUnderAttack(turn, attackingSide, defendingSide, defendingKingCoord);
    }

    private boolean isCellIsUnderAttack(Turn turn, Map<String, String> attackingSide, Map<String, String> defendingSide, String coordToCheck) {
        for (String attackingPiece : attackingSide.keySet()) {
            List<String> possibleMoves = doGetPossibleMoves(turn, attackingPiece, attackingSide, defendingSide);
            if (possibleMoves.contains(coordToCheck)) {
                return true;
            }
        }

        return false;
    }

    private Direction resolvePawnMoveDirection(Turn turn) {
        return pawnMoveDirections.get(turn);
    }

    private List<Direction> resolvePawnFightDirections(Turn turn) {
        return pawnFightDirections.get(turn);
    }

    private int pawnsMoveCells(int digit, Direction direction) {
        if(Direction.UP == direction && digit == START_WHITE_PAWN_RANK) {
            return 2;
        }

        if(Direction.DOWN == direction && digit == START_BLACK_PAWN_RANK) {
            return 2;
        }

        return 1;
    }

    private int resolveVerticalDelta(Direction direction, int digit) {
        return direction.getDy() + digit;
    }

    private int resolveHorizontalDelta(Direction direction, int letter) {
        return direction.getDx() + letter;
    }

    private Map<String, String> resolveTurning() {
        return Turn.WHITE == turn? white : black;
    }

    private Map<String, String> resolveWaiting() {
        return Turn.WHITE == turn? black : white;
    }

    private String resolveCurrentPiece(String coordFrom, Map<String, String> turning) {
        return turning.get(coordFrom);
    }

    private void changeTurn() {
        turn = invertTurn(turn);
        ui.setTurn(turn);
    }

    private Turn invertTurn(Turn turn) {
        return  (Turn.WHITE == turn)? Turn.BLACK : Turn.WHITE;
    }

    public void clearCaches() {
        possibleMovesCache.clear();
    }

    public boolean isDraw() {
        return draw != null;
    }

    public Draw getDraw() {
        return draw;
    }

    public boolean isInCheckMateState() {
        return winner != null;
    }

    public Turn getWinner() {
        return winner;
    }

    private boolean isPawn(String piece) {
        return piece.endsWith("P");
    }

    private boolean isRook(String piece) {
        return piece.endsWith("R");
    }

    private boolean isKnight(String piece) {
        return piece.endsWith("N");
    }

    private boolean isBishop(String piece) {
        return piece.endsWith("B");
    }

    private boolean isQueen(String piece) {
        return piece.endsWith("Q");
    }

    private boolean isKing(String piece) {
        return piece.endsWith("K");
    }

    public boolean isInCheckState() {
        return underCheck != null;
    }

    private void removePieces(Map<String, String> team) {
        for(String pieceCoord : team.keySet()) {
            ui.removePiece(pieceCoord);
        }
        team.clear();
    }

    public Turn getAttackedSide() {
        return underCheck;
    }

    public File resolveInitialDirectory() {
        File thisDir = new File(System.getProperty("user.dir"));
        Optional<File> savesOpt = Arrays.stream(thisDir.listFiles())
                                        .filter(f -> f.isDirectory())
                                        .filter(f -> SAVES_DIRECTORY.equalsIgnoreCase(f.getName()))
                                        .findFirst();

        if(savesOpt.isPresent()) {
            return savesOpt.get();
        }

        thisDir = new File(thisDir, SAVES_DIRECTORY);
        thisDir.mkdir();

        return thisDir;
    }

    public String suggestSaveGameFileName() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
    }

    public void loadGame(File selectedFile) {
        GameState state = fileManager.load(selectedFile.getAbsolutePath());
        doStartGame(state);
    }

    public void saveGame(File selectedFile) {
        fileManager.save(selectedFile.getAbsolutePath(), getGameState());
    }

    private GameState getGameState() {
        GameState state = new GameState(turn);
        state.setUnderCheck(underCheck);
        state.setPossibleDrawMovesCount(possibleDrawMovesCount);
        state.setWhite(white);
        state.setBlack(black);
        state.setWhiteCastleConditions(whiteCastleConditions);
        state.setBlackCastleConditions(blackCastleConditions);
        state.setSnapshots(boardSnapshotsCount);
        return state;
    }
}
