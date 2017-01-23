package com.nuggetchat.messenger.datamodel;

public class GamePlayedHistory {
    private String gameID;
    private long playedTimestamp;
    private boolean isMultiplayer;
    private String playedWithID;

    public GamePlayedHistory(String gameID, long playedTimestamp, boolean isMultiplayer, String playedWithID) {
        this.gameID = gameID;
        this.playedTimestamp = playedTimestamp;
        this.isMultiplayer = isMultiplayer;
        this.playedWithID = playedWithID;
    }

    public String getGameID() {
        return gameID;
    }

    public void setGameID(String gameID) {
        this.gameID = gameID;
    }

    public long getPlayedTimestamp() {
        return playedTimestamp;
    }

    public void setPlayedTimestamp(long playedTimestamp) {
        this.playedTimestamp = playedTimestamp;
    }

    public boolean isMultiplayer() {
        return isMultiplayer;
    }

    public void setMultiplayer(boolean multiplayer) {
        isMultiplayer = multiplayer;
    }

    public String getPlayedWithID() {
        return playedWithID;
    }

    public void setPlayedWithID(String playedWithID) {
        this.playedWithID = playedWithID;
    }
}
