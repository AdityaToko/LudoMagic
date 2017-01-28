package com.nuggetchat.messenger.datamodel;

public class CallMadeHistory {
    private long playedTimestamp;
    private String playedWithID;
    private long duration;
    private boolean isAccepted;

    public CallMadeHistory(long playedTimestamp, String playedWithID, long duration, boolean isAccepted) {
        this.playedTimestamp = playedTimestamp;
        this.playedWithID = playedWithID;
        this.duration = duration;
        this.isAccepted = isAccepted;
    }

    public long getPlayedTimestamp() {
        return playedTimestamp;
    }

    public void setPlayedTimestamp(long playedTimestamp) {
        this.playedTimestamp = playedTimestamp;
    }

    public String getPlayedWithID() {
        return playedWithID;
    }

    public void setPlayedWithID(String playedWithID) {
        this.playedWithID = playedWithID;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isAccepted() {
        return isAccepted;
    }

    public void setAccepted(boolean accepted) {
        isAccepted = accepted;
    }
}
