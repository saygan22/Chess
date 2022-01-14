package com.saygan.file;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.saygan.Turn;

public class TestPlainTextChessFileManager {

    private static final String FILENAME = "my game.chs";

    private File root;
    private Map<String, String> cache;

    private PlainTextChessFileManager fileManager = new PlainTextChessFileManager();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() {
        root = folder.getRoot();
        cache = new HashMap<>();
    }

    @Test
    public void chessFileManagerLoadFileContract() throws IOException {
        String filePathToLoad = root.getAbsolutePath() + "/" + FILENAME;

        Writer w = new FileWriter(new File(filePathToLoad));
        w.write("turn=WHITE\n");
        w.write("underCheck=BLACK\n");
        w.write("possibleDrawMovesCount=18\n");
        w.write("white=g1:K,f1:R,g2:P\n");
        w.write("black=c8:K,d8:R,c7:P\n");
        w.write("whiteCastleConditions=true,true,true\n");
        w.write("blackCastleConditions=false,false,false\n");
        w.write("snapshots=wQ..wN..bP:2,wK....wR..:1\n");
        w.close();

        GameState gameState = fileManager.load(filePathToLoad);

        assertThat(gameState.getTurn(), equalTo(Turn.WHITE));
        assertThat(gameState.getUnderCheck(), equalTo(Turn.BLACK));
        assertThat(gameState.getPossibleDrawMovesCount(), equalTo(18));
        assertThat(gameState.getWhite(), equalTo(map("g1", "wK", "f1", "wR", "g2", "wP")));
        assertThat(gameState.getBlack(), equalTo(map("c8", "bK", "d8", "bR", "c7", "bP")));
        assertThat(gameState.getWhiteCastleConditions(), equalTo(new boolean[] {true, true, true}));
        assertThat(gameState.getBlackCastleConditions(), equalTo(new boolean[] {false, false, false}));
        assertThat(gameState.getSnapshots(), equalTo(map("wK....wR..", 1, "wQ..wN..bP", 2)));
    }

    @Test
    public void shouldSaveGameState() throws IOException {
        Map<String, String> expectedWhite = new HashMap<>();
        Map<String, String> expectedBlack = new HashMap<>();

        expectedWhite.put("e1", "wK");
        expectedWhite.put("d1", "wQ");
        expectedWhite.put("f1", "wB");

        expectedBlack.put("e8", "bK");
        expectedBlack.put("d8", "bQ");
        expectedBlack.put("f8", "bB");

        boolean[] expectedWhiteCastleConditions = new boolean[] {true, true, false};
        boolean[] expectedBlackCastleConditions = new boolean[] {false, true, true};

        Map<String, Integer> expectedSnapshots = new HashMap<>();
        expectedSnapshots.put("wQ..wB....", 1);
        expectedSnapshots.put("bQ....bB..", 2);

        GameState gameState = new GameState(Turn.WHITE);
        gameState.setUnderCheck(Turn.WHITE);
        gameState.setPossibleDrawMovesCount(17);
        gameState.setWhite(expectedWhite);
        gameState.setBlack(expectedBlack);
        gameState.setWhiteCastleConditions(expectedWhiteCastleConditions);
        gameState.setBlackCastleConditions(expectedBlackCastleConditions);
        gameState.setSnapshots(expectedSnapshots);

        String filePathToSave = root.getAbsolutePath() + "/" + FILENAME;

        fileManager.save(filePathToSave, gameState);

        Turn turn = readTurn(filePathToSave);
        Turn underCheck = readUnderCheck(filePathToSave);
        int possibleDrawMovesCount = readPossibleDrawMovesCount(filePathToSave);
        Map<String, String> actualWhite = readWhite(filePathToSave);
        Map<String, String> actualblack = readBlack(filePathToSave);
        boolean[] actualWhiteCastleConditions = readWhiteCastleConditions(filePathToSave);
        boolean[] actualBlackCastleConditions = readBlackCastleConditions(filePathToSave);
        Map<String, Integer> actualSnapshots = readSnapshots(filePathToSave);

        assertThat(turn, equalTo(Turn.WHITE));
        assertThat(underCheck, equalTo(Turn.WHITE));
        assertThat(possibleDrawMovesCount, equalTo(17));
        assertThat(actualWhite, equalTo(expectedWhite));
        assertThat(actualblack, equalTo(expectedBlack));
        assertThat(actualWhiteCastleConditions, equalTo(expectedWhiteCastleConditions));
        assertThat(actualBlackCastleConditions, equalTo(expectedBlackCastleConditions));
        assertThat(actualSnapshots, equalTo(expectedSnapshots));
    }

    private Map<String, Integer> readSnapshots(String filePathToSave) throws IOException {
        String snapshotsRaw = getFiledValue("snapshots", filePathToSave);
        String[] snapshotsToCount = snapshotsRaw.split(",");
        Map<String, Integer> snapshots = new HashMap<>();
        for(String snapshotToCountRaw : snapshotsToCount) {
            String[] snapshotToCount = snapshotToCountRaw.split(":");
            snapshots.put(snapshotToCount[0], Integer.parseInt(snapshotToCount[1]));
        }
        return snapshots;
    }

    private boolean[] readWhiteCastleConditions(String filePathToSave) throws IOException {
        return deserialize(getFiledValue("whiteCastleConditions", filePathToSave));
    }

    private boolean[] readBlackCastleConditions(String filePathToSave) throws IOException {
        return deserialize(getFiledValue("blackCastleConditions", filePathToSave));
    }

    private boolean[] deserialize(String castleConditionsRawRaw) {
        String[] castleConditionsRaw = castleConditionsRawRaw.split(",");
        return new boolean[] {Boolean.valueOf(castleConditionsRaw[0]),
                              Boolean.valueOf(castleConditionsRaw[1]),
                              Boolean.valueOf(castleConditionsRaw[2])};
    }

    private Map<String, String> readWhite(String filePathToSave) throws IOException {
        return readTeam("white", filePathToSave);
    }

    private Map<String, String> readBlack(String filePathToSave) throws IOException {
        return readTeam("black", filePathToSave);
    }

    private Map<String, String> readTeam(String team, String filePathToSave) throws IOException {
        String rawMap = getFiledValue(team, filePathToSave);
        String[] rawEntires = rawMap.split(",");
        Map<String, String> teamMap = new HashMap<>();
        for(String entry : rawEntires) {
            String[] kv = entry.split(":");
            teamMap.put(kv[0], team.charAt(0) + kv[1]);
        }
        return teamMap;
    }

    private Integer readPossibleDrawMovesCount(String filePathToSave) throws NumberFormatException, IOException {
        return Integer.parseInt(getFiledValue("possibleDrawMovesCount", filePathToSave));
    }

    private Turn readUnderCheck(String filePathToSave) throws IOException {
        return Turn.valueOf(getFiledValue("underCheck", filePathToSave));
    }

    private Turn readTurn(String filePathToSave) throws IOException {
        return Turn.valueOf(getFiledValue("turn", filePathToSave));
    }

    private String getFiledValue(String field, String filePathToSave) throws IOException {
        if(!cache.isEmpty()) {
            return cache.get(field);
        }

        for(String line : Files.readAllLines(Paths.get(filePathToSave))) {
            String[] keyValue = line.split("=");
            cache.put(keyValue[0], keyValue[1]);
        }
        return cache.get(field);
    }

    private Map<String, Object> map(Object ... elements) {
        Map<String, Object> team = new HashMap<>();
        for(int i = 0; i < elements.length; i += 2) {
            team.put(elements[i].toString(), elements[i+1]);
        }
        return team;
    }
}
