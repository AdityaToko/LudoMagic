package com.tokostudios.chat;

public class Friend {
    private User user1;
    private User user2;
    private String token;

    public Friend(User user1, User user2, String token) {
        this.user1 = user1;
        this.user2 = user2;
        this.token = token;
    }

    public User getUser1() {
        return user1;
    }

    public void setUser1(User user1) {
        this.user1 = user1;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }


    public User getUser2() {
        return user2;
    }


}
