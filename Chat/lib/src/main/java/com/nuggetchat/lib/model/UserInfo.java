package com.nuggetchat.lib.model;

import java.util.Map;

/**
 * Info about a user.
 */
public class UserInfo {
    private String id;
    private String name;
    private String facebookId;
    private Map<String, FriendInfo> friends;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }

    public Map<String, FriendInfo> getFriends() {
        return friends;
    }

    public void setFriends(Map<String, FriendInfo> friends) {
        this.friends = friends;
    }
}
