package com.nuggetchat.messenger.datamodel;

public class LastLeader {
    String id;
    String name;
    int score;
    long scoreTime;

    public LastLeader() {

    }

    public LastLeader(String id, String name, int score, long scoreTime) {
        this.id = id;
        this.name = name;
        this.score = score;
        this.scoreTime = scoreTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public long getScoreTime() {
        return scoreTime;
    }

    public void setScoreTime(long scoreTime) {
        this.scoreTime = scoreTime;
    }
}
