package com.nuggetchat.messenger.activities;

public class GamesItem {
    private String gameKey;
    private String gamesName;
    private String gamesImage;

    public String getGameKey() {
        return gameKey;
    }

    public void setGameKey(String gameKey) {
        this.gameKey = gameKey;
    }

    public String getGamesName() {
        return gamesName;
    }

    public void setGamesName(String gamesName) {
        this.gamesName = gamesName;
    }

    public String getGamesImage() {
        return gamesImage;
    }

    public void setGamesImage(String gamesImage) {
        this.gamesImage = gamesImage;
    }

    public GamesItem(String gameKey, String gamesName, String gamesImage) {
        this.gameKey = gameKey;
        this.gamesName = gamesName;
        this.gamesImage = gamesImage;
    }

}
