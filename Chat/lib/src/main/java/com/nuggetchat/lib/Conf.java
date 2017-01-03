package com.nuggetchat.lib;

public final class Conf {
    private static final String FIREBASE_DOMAIN_URI = "https://nuggetplay-ceaaf.firebaseio.com/";
    private static final String FIREBASE_STREAM_URI = "streams/"; // stream URI
    private static final String FIREBASE_GAMES_URI = "games/"; // games URI
    private static final String FIREBASE_MULTIPLAYER_GAMES_URI = "multiplayer-games/"; //multiplayer-games URI
    private static final String FIREBASE_USERS_URI = "users/"; //users URI
    public static final String CLOUDINARY_PREFIX_URL = "http://res.cloudinary.com/tokoimages1/image/upload/";


    public static String _firebaseDomainURI() {
        return FIREBASE_DOMAIN_URI;
    }

    public static String firebaseGamesURI() {
        return _firebaseDomainURI() + FIREBASE_GAMES_URI;
    }

    public static String firebaseStreamUri() {
        return _firebaseDomainURI() + FIREBASE_STREAM_URI;
    }

    public static String firebaseMultiPlayerGamesUri() {
        return firebaseStreamUri() + FIREBASE_MULTIPLAYER_GAMES_URI;
    }

    public static String firebaseUsersURI() {
        return _firebaseDomainURI() + FIREBASE_USERS_URI;
    }
}
