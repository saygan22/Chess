package com.saygan.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

import com.saygan.Turn;

public class PlainTextChessFileManager implements ChessFileManager {

    private Writer writer;

    @Override
    public void save(String filePathToSave, GameState gameState) {
        createAndOpenFile(filePathToSave);
        write("turn", gameState.getTurn());
        write("underCheck", gameState.getUnderCheck());
        write("possibleDrawMovesCount", gameState.getPossibleDrawMovesCount());
        write("white", serialize(gameState.getWhite()));
        write("black", serialize(gameState.getBlack()));
        write("whiteCastleConditions", serialize(gameState.getWhiteCastleConditions()));
        write("blackCastleConditions", serialize(gameState.getBlackCastleConditions()));
        write("snapshots", serializeSnapshots(gameState.getSnapshots()));
        doClose(writer);
    }

    private String serializeSnapshots(Map<String, Integer> snapshots) {

        if(snapshots.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for(Entry<String, Integer> entry : snapshots.entrySet()) {
            sb.append(entry.getKey());
            sb.append(":");
            sb.append(entry.getValue());
            sb.append(",");
        }

        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    private String serialize(boolean[] castleConditions) {
        StringJoiner joiner = new StringJoiner(",");
        for(boolean castleCondition : castleConditions) {
            joiner.add(String.valueOf(castleCondition));
        }
        return joiner.toString();
    }

    private String serialize(Map<String, String> team) {
        StringBuilder sb = new StringBuilder();

        for(Entry<String, String> entry : team.entrySet()) {
            sb.append(entry.getKey());
            sb.append(":");
            sb.append(entry.getValue().charAt(1));
            sb.append(",");
        }

        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    private void write(String key, Object value) {
        try {
            writer.write(key + "=" + (value == null? "null" : value.toString()) + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void doClose(Writer writer) {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GameState load(String filePathToLoad) {

        BufferedReader reader = doOpen(filePathToLoad);

        Map<String, String> cache = new HashMap<>();
        while(true) {
            String rawLine = doReadLine(reader);

            if(rawLine == null) {
                break;
            }

            String[] kv = rawLine.split("=", 2);

            cache.put(kv[0], kv[1]);
        }

        GameState state = new GameState(deserializeTurn(cache.get("turn")));

        state.setUnderCheck(deserializeTurn(cache.get("underCheck")));
        state.setPossibleDrawMovesCount(deserializeInt(cache.get("possibleDrawMovesCount")));
        state.setWhite(deserializeTeam('w', cache.get("white")));
        state.setBlack(deserializeTeam('b', cache.get("black")));
        state.setWhiteCastleConditions(deserializeCastleCondition(cache.get("whiteCastleConditions")));
        state.setBlackCastleConditions(deserializeCastleCondition(cache.get("blackCastleConditions")));
        state.setSnapshots(deserializeSnapShots(cache.get("snapshots")));

        return state;
    }

    private Map<String, Integer> deserializeSnapShots(String snapshotsRaw) {
        String[] snapshotsToCount = snapshotsRaw.split(",");
        Map<String, Integer> snapshots = new HashMap<>();
        for(String snapshotToCountRaw : snapshotsToCount) {
            String[] snapshotToCount = snapshotToCountRaw.split(":");
            snapshots.put(snapshotToCount[0], Integer.parseInt(snapshotToCount[1]));
        }
        return snapshots;
    }

    private boolean[] deserializeCastleCondition(String castleConditionsRawRaw) {
        String[] castleConditionsRaw = castleConditionsRawRaw.split(",");
        return new boolean[] {Boolean.valueOf(castleConditionsRaw[0]),
                              Boolean.valueOf(castleConditionsRaw[1]),
                              Boolean.valueOf(castleConditionsRaw[2])};
    }

    private Map<String, String> deserializeTeam(char team, String rawTeam) {
        String[] rawEntires = rawTeam.split(",");
        Map<String, String> teamMap = new HashMap<>();
        for(String entry : rawEntires) {
            String[] kv = entry.split(":");
            teamMap.put(kv[0], team + kv[1]);
        }
        return teamMap;
    }

    private int deserializeInt(String rawTurn) {
        return Integer.parseInt(rawTurn);
    }

    private Turn deserializeTurn(String rawTurn) {
        return rawTurn.equals("null")? null : Turn.valueOf(rawTurn);
    }

    private String doReadLine(BufferedReader reader) {
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private BufferedReader doOpen(String filePathToLoad) {
        try {
            return new BufferedReader(new FileReader(new File(filePathToLoad)));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void createAndOpenFile(String filePathToSave) {
        try {
            new File(filePathToSave).createNewFile();
            writer = new FileWriter(filePathToSave);
        } catch (IOException e) {
           throw new RuntimeException(e);
        }
    }
}
