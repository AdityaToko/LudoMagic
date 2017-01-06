package com.nuggetchat.server;

import com.google.appengine.api.ThreadManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.internal.NonNull;
import com.google.firebase.tasks.OnCompleteListener;
import com.google.firebase.tasks.Task;
import com.nuggetchat.lib.Conf;
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
import java.util.UUID;

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
        final String requestUid = UUID.randomUUID().toString();
        myLog(requestUid, "GetFriend request");
        final String facebookAccessToken = req.getParameter(RequestParams.FACEBOOK_ACCESS_TOKEN);
        final String firebaseIdToken = req.getParameter(RequestParams.FIREBASE_ID_TOKEN);
        if (facebookAccessToken == null || facebookAccessToken.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Facebook access token required. Request id: " + requestUid);
            myLog(requestUid, "Facebook access token not provided.");
            return;
        }
        if (firebaseIdToken == null || firebaseIdToken.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Firebase id token required. Request id: " + requestUid);
            myLog(requestUid, "Firebase id token not provided.");
            return;
        }

        Thread thread = ThreadManager.createBackgroundThread(
                new Runnable() {
                    @Override
                    public void run() {
                        FirebaseAuth.getInstance().verifyIdToken(firebaseIdToken)
                                .addOnCompleteListener(
                                        new OnCompleteListener<FirebaseToken>() {
                                            @Override
                                            public void onComplete(
                                                    @NonNull Task<FirebaseToken> task) {
                                                writeToFirebase(
                                                        task, facebookAccessToken, requestUid);
                                            }
                                        });
                    }
                });
        thread.start();

        resp.setContentType("text/plain");
        resp.getWriter().println("Started. Request id: " + requestUid);
    }

    private void writeToFirebase(Task<FirebaseToken> task, String facebookAccessToken,
            final String requestUid) {
        if (!task.isSuccessful()) {
            myLog(requestUid, "Invalid firebase id token.", task.getException());
            return;
        }

        FirebaseToken firebaseToken = task.getResult();
        if (firebaseToken == null) {
            log(requestUid + ": Unable to verify firebase id token.");
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
        myLog(requestUid, "Friend count:" + friendsInfo.size());


        DatabaseReference dbReference = FirebaseDatabase.getInstance().getReferenceFromUrl(
                Conf.firebaseUsersUri() + userId);
        dbReference.setValue(userInfo).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    myLog(requestUid, " Update successful.");
                } else {
                    myLog(requestUid, ": Unable to update friends.", task.getException());
                }
            }
        });
    }

    private void myLog(String reqId, String mesg) {
        log(reqId + ":" + mesg);
    }

    private void myLog(String reqId, String mesg, Exception e) {
        log(reqId + ":" + mesg, e);
    }
}
