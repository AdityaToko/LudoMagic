package com.nuggetchat.lib;

public final class Conf {
    public static final String CHAT_SERVER_HOST = "http://server.nuggetchat.com:8080/";
    public static final String GET_FRIENDS_API_URL = CHAT_SERVER_HOST + "getFriends";

    private static final String FIREBASE_DOMAIN_URI = "https://nuggetplay-ceaaf.firebaseio.com/";
    private static final String FIREBASE_STREAM_URI = "streams/"; // stream URI
    private static final String FIREBASE_GAMES_URI = "games/"; // games URI
    private static final String FIREBASE_MULTIPLAYER_GAMES_URI = "multiplayer-games/"; //multiplayer-games URI
    private static final String FIREBASE_USERS_URI = "users/"; //users URI
    public static final String CLOUDINARY_PREFIX_URL = "http://res.cloudinary.com/tokoimages1/image/upload/";
    public static final String FB_TO_FIRE_USER_MAP = "fb-to-fire/"; // Map of users from facebook id to firebase
    public static final String GAME_SESSION = "game-session/";


    public static String firebaseDomainUri() {
        return FIREBASE_DOMAIN_URI;
    }

    public static String firebaseGamesUri() {
        return firebaseDomainUri() + FIREBASE_GAMES_URI;
    }

    public static String firebaseStreamUri() {
        return firebaseDomainUri() + FIREBASE_STREAM_URI;
    }

    public static String firebaseMultiPlayerGamesUri() {
        return firebaseStreamUri() + FIREBASE_MULTIPLAYER_GAMES_URI;
    }

    public static String firebaseUsersUri() {
        return firebaseDomainUri() + FIREBASE_USERS_URI;
    }

    public static String firebaseFbToFireidUri(String facebookId) {
        return firebaseDomainUri() + FB_TO_FIRE_USER_MAP + facebookId + "/";
    }

    public static String firebaseUserFriends(String userFirebaseId) {
        return firebaseUsersUri() + userFirebaseId + "/friends";
    }

    public static String firebaseGameSession() {
        return firebaseDomainUri() + GAME_SESSION;
    }
}
