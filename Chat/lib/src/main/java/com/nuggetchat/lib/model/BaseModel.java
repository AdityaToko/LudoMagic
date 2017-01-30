package com.nuggetchat.lib.model;

import java.io.Serializable;

public class BaseModel implements BaseModelInterface, Serializable {

    private Long modifiedTime;
    private String title;

    public BaseModel() {
    }

    public BaseModel(String title, Long modifiedTime) {
        this.title = title;
        this.modifiedTime = modifiedTime;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        if (DataFormat.isNotNullNorEmpty(title)) {
            this.title = title;
        }
    }

    @Override
    public Long getModifiedTime() {
        return modifiedTime;
    }

    @Override
    public void setModifiedTime(Long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }
}
