package com.nuggetchat.messenger.datamodel;

import java.util.HashMap;
import java.util.Map;

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
        genderBiasPercent = new HashMap<>();
        categories = new HashMap<>();
        indexHash = new HashMap<>();
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

    public boolean getIsFun() {
        return isFun;
    }

    public void setIsFun(boolean fun) {
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

    public Map<String, Integer> getGenderBiasPercent() {
        return genderBiasPercent;
    }

    public void setGenderBiasPercent(Map<String, Integer> genderBiasPercent) {
        this.genderBiasPercent = (HashMap<String, Integer>) genderBiasPercent;
    }

    public Map<String, Boolean> getCategories() {
        return categories;
    }

    public void setCategories(Map<String, Boolean> categories) {
        this.categories = (HashMap<String, Boolean>) categories;
    }

    public Map<String, Boolean> getTagIndex() {
        return indexHash;
    }

    public void setTagIndex(Map<String, Boolean> indexHash) {
        this.indexHash = (HashMap<String, Boolean>) indexHash;
    }
}
