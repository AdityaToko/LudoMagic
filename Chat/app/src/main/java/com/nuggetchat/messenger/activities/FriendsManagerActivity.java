package com.nuggetchat.messenger.activities;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.share.model.ShareLinkContent;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.UserFriendsAdapter;
import com.nuggetchat.messenger.datamodel.UserDetails;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;
import com.tokostudios.chat.ChatActivity;
import com.tokostudios.chat.webRtcClient.RtcListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;

import java.util.ArrayList;
import java.util.List;

public class FriendsManagerActivity extends AppCompatActivity{
    private static final String LOG_TAG = FriendsManagerActivity.class.getSimpleName();
    ArrayList<UserDetails> selectUsers;
    List<UserDetails> temp;
    // Contact List
    ListView listView;
    // Cursor to load contacts list
    Cursor phones, email;

    // Pop up
    ContentResolver resolver;
    UserFriendsAdapter adapter;
    CallbackManager callbackManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_friendsmanager);
        getUserFriends(SharedPreferenceUtility.getFacebookUserId(FriendsManagerActivity.this));
        callbackManager = CallbackManager.Factory.create();

        selectUsers = new ArrayList<>();
        resolver = this.getContentResolver();
        listView = (ListView) findViewById(R.id.contacts_list);
        ShareLinkContent linkContent = new ShareLinkContent.Builder()
                .setContentTitle("....")
                .build();
    }

    private void getUserFriends(String id) {
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/friends",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        Log.d(LOG_TAG, response.toString());
                        JSONObject object = response.getJSONObject();
                        try {
                            for (int i = 0; i < object.getJSONArray("data").length(); i++) {
                                JSONObject dataObject = (object.getJSONArray("data")).getJSONObject(i);
                                Log.d(LOG_TAG, dataObject.toString());
                                String name = dataObject.getString("name");
                                String userId = dataObject.getString("id");
                                UserDetails userData = new UserDetails();
                                userData.setUserId(userId);
                                userData.setName(name);
                                selectUsers.add(userData);
                                Log.d(LOG_TAG, "Values " + name + "  " + userId);
                            }
                            adapter = new UserFriendsAdapter(selectUsers, FriendsManagerActivity.this);
                            listView.setAdapter(adapter);
                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                    UserDetails userDetails = (UserDetails) adapterView.getAdapter().getItem(i);
                                    String userId = userDetails.getUserId();
                                    Log.e(LOG_TAG, "onItemClick: " + userId + " " + userDetails.getName());
                                    Intent intent = new Intent(FriendsManagerActivity.this, ChatActivity.class);
                                    intent.putExtra("userId", userId);
                                    startActivity(intent);
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }


    public void sendMessagetoFriends(View v) {
        Log.d(LOG_TAG, "Message to friends called");

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setPackage("com.facebook.orca");
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "Hey! How are you? I just found this awesome app where we can chat and play simultaneously. Lets play YO!");
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "You do not have Facebook Messenger installed", Toast.LENGTH_LONG).show();
        }
    }

    public void sendShareIntent(View v) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "Hey! How are you? I just found this awesome app where we can chat and play simultaneously. Lets play YO!");
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar actions click
        switch (item.getItemId()) {
            case R.id.action_logout:
                LoginManager.getInstance().logOut();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
}
