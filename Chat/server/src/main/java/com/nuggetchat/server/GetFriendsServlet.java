/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Servlet Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloWorld
*/

package com.nuggetchat.server;

import com.nuggetchat.lib.RequestParams;
import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Version;
import com.restfb.types.User;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.*;

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
        String accessToken = req.getParameter(RequestParams.ACCESS_TOKEN);
        if (accessToken == null || accessToken.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Access token required.");
            return;
        }

        resp.setContentType("text/plain");
        resp.getWriter().println("Token " + accessToken);

        FacebookClient client = new DefaultFacebookClient(accessToken, Version.VERSION_2_6);
        Connection<User> friends = client.fetchConnection("me/friends", User.class);
        for (User friend : friends.getData()) {
            resp.getWriter().println(friend.getId() + " : " + friend.getName());
        }
    }

}
