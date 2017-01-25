package com.nuggetchat.messenger.datamodel;

public class CurrentLeader {
    String id;
    String minName;
    int minimum1;
    int minimum2;
    int score;
    long scoreTime;

    public CurrentLeader(){

    }
    public CurrentLeader(String id, String minName, int minimum1, int minimum2, int score, long scoreTime) {
        this.id = id;
        this.minName = minName;
        this.minimum1 = minimum1;
        this.minimum2 = minimum2;
        this.score = score;
        this.scoreTime = scoreTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMinName() {
        return minName;
    }

    public void setMinName(String minName) {
        this.minName = minName;
    }

    public int getMinimum1() {
        return minimum1;
    }

    public void setMinimum1(int minimum1) {
        this.minimum1 = minimum1;
    }

    public int getMinimum2() {
        return minimum2;
    }

    public void setMinimum2(int minimum2) {
        this.minimum2 = minimum2;
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
