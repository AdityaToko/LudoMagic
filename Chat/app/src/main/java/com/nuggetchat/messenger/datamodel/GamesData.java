package com.nuggetchat.messenger.datamodel;

import java.util.HashMap;

public class GamesData {
    private int ageBestMax;
    private int ageBestMin;
    private int ageLimitMax;
    private int ageLimitMin;
    private int valueScore;
    private String emotion;
    private boolean isFun;
    private String dataId;
    private String dataType;
    private String url;
    private String featuredImage;
    private String lang;
    private boolean webOnly;
    private Boolean portrait;
    private String title;
    private double modifiedTime;
    private HashMap<String, Integer> genderBiasPercent;
    private HashMap<String, Boolean> categories;
    private HashMap<String, Boolean> indexHash;

    public GamesData() {
        genderBiasPercent = new HashMap<String, Integer>();
        categories = new HashMap<String, Boolean>();
        indexHash = new HashMap<String, Boolean>();
    }


    public int getAgeBestMax() {
        return ageBestMax;
    }

    public void setAgeBestMax(int ageBestMax) {
        this.ageBestMax = ageBestMax;
    }

    public int getAgeBestMin() {
        return ageBestMin;
    }

    public void setAgeBestMin(int ageBestMin) {
        this.ageBestMin = ageBestMin;
    }

    public int getAgeLimitMax() {
        return ageLimitMax;
    }

    public void setAgeLimitMax(int ageLimitMax) {
        this.ageLimitMax = ageLimitMax;
    }

    public int getAgeLimitMin() {
        return ageLimitMin;
    }

    public void setAgeLimitMin(int ageLimitMin) {
        this.ageLimitMin = ageLimitMin;
    }

    public int getValueScore() {
        return valueScore;
    }

    public void setValueScore(int valueScore) {
        this.valueScore = valueScore;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public boolean isFun() {
        return isFun;
    }

    public void setFun(boolean fun) {
        isFun = fun;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFeaturedImage() {
        return featuredImage;
    }

    public void setFeaturedImage(String featuredImage) {
        this.featuredImage = featuredImage;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public boolean isWebOnly() {
        return webOnly;
    }

    public void setWebOnly(boolean webOnly) {
        this.webOnly = webOnly;
    }

    public Boolean getPortrait() {
        return portrait;
    }

    public void setPortrait(Boolean portrait) {
        this.portrait = portrait;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(double modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public HashMap<String, Integer> getGenderBiasPercent() {
        return genderBiasPercent;
    }

    public void setGenderBiasPercent(HashMap<String, Integer> genderBiasPercent) {
        this.genderBiasPercent = genderBiasPercent;
    }

    public HashMap<String, Boolean> getCategories() {
        return categories;
    }

    public void setCategories(HashMap<String, Boolean> categories) {
        this.categories = categories;
    }

    public HashMap<String, Boolean> getIndexHash() {
        return indexHash;
    }

    public void setIndexHash(HashMap<String, Boolean> indexHash) {
        this.indexHash = indexHash;
    }
}
