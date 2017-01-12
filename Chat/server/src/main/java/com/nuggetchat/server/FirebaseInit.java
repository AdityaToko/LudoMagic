package com.nuggetchat.server;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.nuggetchat.lib.Conf;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class FirebaseInit extends HttpServlet {
    private static final Logger log = Logger.getLogger(FirebaseInit.class.getSimpleName());

    private static FirebaseDatabase firebaseInstance =  null;

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (firebaseInstance == null) {
            log.info("Firebase Init: Start");
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setServiceAccount(new FileInputStream(new File("WEB-INF/nuggetplay-ceaaf-firebase-adminsdk.json")))
                    .setDatabaseUrl(Conf.firebaseDomainUri())
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase Init: Done.");
            firebaseInstance = FirebaseDatabase.getInstance();
        }

        // Its important to return response with 200 OK for _ah/start request
        resp.setContentType("text/plain");
        resp.getWriter().println("Success firebase init");
    }
}
