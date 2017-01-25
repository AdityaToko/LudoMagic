package com.nuggetchat.lib;

import com.nuggetchat.lib.common.Utils;

public final class Conf {
    public static final String CHAT_SERVER_HOST = "https://server.nuggetchat.com/";
    public static final String GET_FRIENDS_API_URL = CHAT_SERVER_HOST + "getFriends";

    private static final String FIREBASE_DOMAIN_URI = "https://nuggetplay-ceaaf.firebaseio.com/";
    private static final String FIREBASE_STREAM_URI = "streams/"; // stream URI
    private static final String FIREBASE_GAMES_URI = "games/"; // games URI
    private static final String FIREBASE_WINNERS_URI = "winners/"; // games URI
    private static final String FIREBASE_INCENTIVES = "incentives/"; //incentives URI
    private static final String FIREBASE_CURRENT_LEADER_URI = "current_leader/"; //current leader URI
    private static final String FIREBASE_MULTIPLAYER_GAMES_URI = "multiplayer-games/"; //multiplayer-games URI
    private static final String FIREBASE_USERS_URI = "users/"; //users URI
    public static final String CLOUDINARY_PREFIX_URL = "http://res.cloudinary.com/tokoimages1/image/upload/";
    public static final String FB_TO_FIRE_USER_MAP = "fb-to-fire/"; // Map of users from facebook id to firebase
    public static final String FIREBASE_DEVICE_TOKEN = "devices";
    public static final String FACEBOOK_DEVICE_TOKEN = "devices-facebook";
    public static final String GAME_SESSION = "game-session/";
    public static final String CHAT_WEBRTC_SERVER = "https://chat.nuggetkids.com/";


    public static String firebaseDomainUri() {
        return FIREBASE_DOMAIN_URI;
    }

    public static String firebaseGamesUri() {
        return firebaseDomainUri() + FIREBASE_GAMES_URI;
    }

    public static String firebaseWinnersUri() {
        return firebaseDomainUri() + FIREBASE_WINNERS_URI;
    }

    public static String firebaseStreamUri() {
        return firebaseDomainUri() + FIREBASE_STREAM_URI;
    }

    public static String firebaseMultiPlayerGamesUri() {
        return firebaseStreamUri() + FIREBASE_MULTIPLAYER_GAMES_URI;
    }

    public static String firebaseCurrentLeaderUri() {
        return firebaseDomainUri() + FIREBASE_INCENTIVES + FIREBASE_CURRENT_LEADER_URI;
    }

    public static String firebaseUsersUri() {
        return firebaseDomainUri() + FIREBASE_USERS_URI;
    }

    public static String firebaseMultiGamePlayedURI(String userId, String currentDate) {
        return firebaseUsersUri() + userId + "/" + currentDate + "/" + "multiplayer/";
    }

    public static String firebaseSoloGamePlayedURI(String userId, String currentDate) {
        return firebaseUsersUri() + userId + "/" + currentDate + "/" + "solo/";
    }

    public static String firebaseUsersUri(String firebaseId) {
        if (Utils.isNullOrEmpty(firebaseId)) {
            return "";
        }
        return firebaseUsersUri() + firebaseId +"/";
    }

    public static String firebaseUserNameUri(String firebaseId) {
        String uriPrefix = firebaseUsersUri(firebaseId);
        if (Utils.isNullOrEmpty(uriPrefix)) {
            return "";
        }
        return uriPrefix + "name/";
    }


    public static String firebaseFbToFireidUri(String facebookId) {
        if (Utils.isNullOrEmpty(facebookId)) {
            return "";
        }
        return firebaseDomainUri() + FB_TO_FIRE_USER_MAP + facebookId + "/";
    }

    public static String firebaseUserFriends(String userFirebaseId) {
        return firebaseUsersUri() + userFirebaseId + "/friends/";
    }

    public static String firebaseUserFriend(String userFirebaseId, String friendFacebookId) {
        String userFriendsUri = firebaseUserFriends(userFirebaseId);
        if (Utils.isNullOrEmpty(userFriendsUri) || Utils.isNullOrEmpty(friendFacebookId)) {
            return "";
        }
        return userFriendsUri + friendFacebookId + "/";
    }

    public static String firebaseGameSession() {
        return firebaseDomainUri() + GAME_SESSION;
    }

    public static String firebaseDeviceToken(String firebaseId) {
        if (Utils.isNullOrEmpty(firebaseId)) {
            return "";
        }
        return firebaseDomainUri() + FIREBASE_DEVICE_TOKEN + "/" + firebaseId + "/";
    }

    public static String facebookDeviceToken(String firebaseId) {
        if (Utils.isNullOrEmpty(firebaseId)) {
            return "";
        }
        return firebaseDomainUri() + FACEBOOK_DEVICE_TOKEN + "/" + firebaseId + "/";
    }
}
