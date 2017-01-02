package com.nuggetchat.messenger.activities;

public class GamesItem {
    private String gameKey;
    private String gamesName;
    private String gamesImage;
    private String gamesUrl;
    private Boolean portrait;

    public GamesItem(String gameKey, String gamesName, String gamesImage, String gamesUrl, Boolean portrait) {
        this.gameKey = gameKey;
        this.gamesName = gamesName;
        this.gamesImage = gamesImage;
        this.gamesUrl = gamesUrl;
        this.portrait = portrait;
    }

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

    public String getGamesUrl() {
        return gamesUrl;
    }

    public void setGamesUrl(String gamesUrl) {
        this.gamesUrl = gamesUrl;
    }

    public Boolean getPortrait() {
        return portrait;
    }

    public void setPortrait(Boolean portrait) {
        this.portrait = portrait;
    }

}
