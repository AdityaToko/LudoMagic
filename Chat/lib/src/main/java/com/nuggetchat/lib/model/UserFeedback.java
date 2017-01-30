package com.nuggetchat.lib.model;

public class UserFeedback implements BaseModelInterface  {
    private BaseModel baseModel;
    private String question;
    private String answer;

    public UserFeedback() {
        baseModel = new BaseModel();
    }

    public UserFeedback(String question, String answer) {
        if (DataFormat.isNotNullNorEmpty(question) && DataFormat.isNotNullNorEmpty(answer)) {
            this.question = question;
            this.answer = answer;
        }
        // Setting title and type to null to reduce the data size overhead.
        baseModel = new BaseModel(null, System.currentTimeMillis());
    }

    @Override
    public String getTitle() {
        return baseModel.getTitle();
    }

    @Override
    public void setTitle(String title) {
        baseModel.setTitle(title);
    }

    @Override
    public Long getModifiedTime() {
        return baseModel.getModifiedTime();
    }

    @Override
    public void setModifiedTime(Long ts) {
        baseModel.setModifiedTime(ts);
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
