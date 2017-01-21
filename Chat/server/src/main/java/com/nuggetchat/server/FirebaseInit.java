package com.nuggetchat.server;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.nuggetchat.lib.Conf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
            String accountJson = "{\n"
                    + "  \"type\": \"service_account\",\n"
                    + "  \"project_id\": \"nuggetplay-ceaaf\",\n"
                    + "  \"private_key_id\": \"dff8e7f819a28752d0ab1b4fc45b2fe082dd6849\",\n"
                    + "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCxwZqQ+bo2/G4M\\nKCYkr/dcU39odMlyM1CGqgq9wSsTcJsTNnfvAl9L8aT4RKLFNaOHfSGIWCcbbHoO\\nW5DqjS3oLca3PgVtAav4Dt2SFJF/4sEwyichVu05VhcGAl+ZXngcQwYrct5JXGli\\nlPeovMwftRPa6s+qMidQ2loK1OBszFdJhpV/TuZoTenYzh71bKyesFytDBaUg6Dp\\nYkL+zLdGM5YQpfThkX6vlD4NvQKpppxSf4Za/lYEchdVaTtBB3gCzO/z7jzSPNDB\\nkPmXgAmFMc7qUs4EZpiGVPlSYJIah68eqEc9RuRelL10TMdm78BHoU4BJd/6FQHp\\n+OIXU/nhAgMBAAECggEAGak+XBGBHXEBngWpxAAXMJ/pM7YwYqkfxeEEXfJl+o92\\nA6cIV1u7UCZZT3HxIZElw2iAD8PSGXZmhG4ZlJK+/tak6k0s0Vi9b7kNqYoImzKr\\n+xQj4/89jdpsxnRPQr/NjR7r5BfYGTUbDZ09x4FB4o3siNz/D63f2R88gnXK1EnJ\\nxlMkatJgTaD43C9mIGTJdXtI04S0wJ22JOn83PfXBkuKEH2uiugTjtWeNOJ/7s7Q\\nRuyu0xQFx7etg6eYJBU3tR85sI/Bux7zVrS9Sp3m8kBqkMQxV4okPeGtD1AhDPWl\\ngXnfOBMZofrAZVKLTqiqg6XAs5EfGKyMRZgPHsAi1QKBgQDmUpOUQvW4G3RPvhUT\\nKECQWkrCldll4PxQyRubpmaXJzbiyI673WWD0NY2FCn17CbRyGzRLc02rt3rA1H2\\nh8dLNDLtPfVRyvDFwutTCHnuno9cGAAhiJ2aB3RgM5/kuUqH25LOqo0BiqyNq3sh\\nsTdzWo1kinQSEp31dlsgPq7GZwKBgQDFksf2OVLKq7yOFg/QaeybtIi3KB9IMV1n\\n66u5GMrkLtP+o3lV8/Jw7bNsUjLjk3UL8luAcjHwH9QNRo+hRbOO1KMwQYcaMddv\\nhrHhkENnf8159carn/nr9b9RXIWgOh7cxE4ICdsYPS5WzPzQyAoKFU50/tSVYnSP\\ni8U+RCRAdwKBgQCej+7lvQtpCg2S8GuV2rpIJsEdIQifqLpL64fEKlSqbubMTgJl\\nt0+x5c3zQQJ6OW+oMPEpOPz0ToKczpZYwLUlQvRke6kt/CayHzTe3xECg8pyPluo\\numK1rbgimqfTUPTMfw+ck7GrhAqzJYEJevWPxqFcvCyYXJS+bhLiKgUstQKBgBYX\\nc4R0pG1FBrBchygmr+45IZBZTCYmXutfzrPDsewR0GKIpOiXfmRiL83M4yPPOSc8\\n0j5qhmrzJCLX4PeHc6xk2lpq+VBUgmJWDU3Lc73+7FrWwIAwQyG/OMVAMzDXTtwg\\nlxkPARCUxrmt8hbmRzkZaMxsMhkCCUaDumpb9DHBAoGARZ254sXv9IR20rrpG3az\\nW4ZJBwz7efA7wLnXxV3rU1pcjQcLevQDtYEQ4+l0tFVS+kT7G/VLezkiQajvBlhQ\\nAV6nw4OGj9GJM8tujEIhaX5i7p0kO79rkHCsr44bOFZB5OzbDqMkxAo++5shFizT\\nNTqVNdV/tv2k07aGGvyyCQQ=\\n-----END PRIVATE KEY-----\\n\",\n"
                    + "  \"client_email\": \"nuggetchat-server@nuggetplay-ceaaf.iam.gserviceaccount.com\",\n"
                    + "  \"client_id\": \"105078638301319594179\",\n"
                    + "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n"
                    + "  \"token_uri\": \"https://accounts.google.com/o/oauth2/token\",\n"
                    + "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n"
                    + "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/nuggetchat-server%40nuggetplay-ceaaf.iam.gserviceaccount.com\"\n"
                    + "}";

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setServiceAccount(new ByteArrayInputStream(accountJson.getBytes()))
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