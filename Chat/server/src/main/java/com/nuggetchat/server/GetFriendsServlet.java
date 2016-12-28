package com.nuggetchat.server;

import com.google.appengine.api.ThreadManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.tasks.Task;
import com.nuggetchat.lib.common.RequestParams;
import com.nuggetchat.lib.model.FriendInfo;
import com.nuggetchat.lib.model.UserInfo;
import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Version;
import com.restfb.types.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GetFriendsServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain");
        resp.getWriter().println("Please use the form to POST to this url");
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String facebookAccessToken = req.getParameter(RequestParams.FACEBOOK_ACCESS_TOKEN);
        String firebaseIdToken = req.getParameter(RequestParams.FIREBASE_ID_TOKEN);
        if (facebookAccessToken == null || facebookAccessToken.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Facebook access token required.");
            return;
        }
        if (firebaseIdToken == null || firebaseIdToken.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Firebase id token required.");
            return;
        }

        Thread thread = ThreadManager.createBackgroundThread(() ->
                FirebaseAuth.getInstance().verifyIdToken(firebaseIdToken).addOnCompleteListener(
                        task -> writeToFirebase(task, facebookAccessToken)));
        thread.start();

        resp.setContentType("text/plain");
        resp.getWriter().println("Started.");
    }

    private void writeToFirebase(Task<FirebaseToken> task, String facebookAccessToken) {
        if (!task.isSuccessful()) {
            log("Invalid firebase id token.", task.getException());
            return;
        }

        FirebaseToken firebaseToken = task.getResult();
        if (firebaseToken == null) {
            log("Unable to verify firebase id token.");
            return;
        }

        String userId = task.getResult().getUid();

        FacebookClient client = new DefaultFacebookClient(facebookAccessToken, Version.VERSION_2_8);
        Connection<User> friends = client.fetchConnection("me/friends", User.class);
        User me = client.fetchObject("me", User.class);

        UserInfo userInfo = new UserInfo();
        userInfo.setId(userId);
        userInfo.setFacebookId(me.getId());
        userInfo.setName(me.getName());

        Map<String, FriendInfo> friendsInfo = new HashMap<>();
        for (User friend : friends.getData()) {
            FriendInfo friendInfo = new FriendInfo();
            friendInfo.setName(friend.getName());
            friendInfo.setFacebookId(friend.getId());
            friendsInfo.put(friend.getId(), friendInfo);
        }
        userInfo.setFriends(friendsInfo);

        DatabaseReference dbReference = FirebaseDatabase.getInstance().getReferenceFromUrl(
                "https://nuggetplay-ceaaf.firebaseio.com/users/" + userId);
        dbReference.setValue(userInfo);
    }
}
