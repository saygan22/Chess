package com.saygan.file;

public interface ChessFileManager {

    void save(String filePathToSave, GameState gameState);

    GameState load(String filePathToLoad);

}
