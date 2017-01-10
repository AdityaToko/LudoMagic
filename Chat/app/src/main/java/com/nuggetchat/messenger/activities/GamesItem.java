package com.nuggetchat.messenger.activities;

public class GamesItem {
    private String gameKey;
    private String gamesName;
    private String gamesImage;
    private String gamesUrl;
    private Boolean portrait;
    private Boolean locked;
    private int value;
    private Boolean newlyUnlocked = false;

    public Boolean getNewlyUnlocked() {
        return newlyUnlocked;
    }

    public void setNewlyUnlocked(Boolean newlyUnlocked) {
        this.newlyUnlocked = newlyUnlocked;
    }

    public GamesItem(String gameKey, String gamesName, String gamesImage, String gamesUrl, Boolean portrait) {
        this.gameKey = gameKey;
        this.gamesName = gamesName;
        this.gamesImage = gamesImage;
        this.gamesUrl = gamesUrl;
        this.portrait = portrait;
    }

    public GamesItem(String gameKey, String gamesName, String gamesImage, String gamesUrl, Boolean portrait, Boolean locked) {
        this(gameKey, gamesName,gamesImage,gamesUrl,portrait);
        this.locked = locked;
    }

    public GamesItem(String gameKey, String gamesName, String gamesImage, String gamesUrl, Boolean portrait, Boolean locked, Boolean newlyUnlocked) {
        this(gameKey, gamesName,gamesImage,gamesUrl,portrait);
        this.locked = locked;
        this.newlyUnlocked = newlyUnlocked;
    }

    public GamesItem(String gameKey, String gamesName, String gamesImage, String gamesUrl, Boolean portrait, Boolean locked, Boolean newlyUnlocked, int value) {
        this(gameKey, gamesName,gamesImage,gamesUrl,portrait);
        this.locked = locked;
        this.newlyUnlocked = newlyUnlocked;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
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

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

}
