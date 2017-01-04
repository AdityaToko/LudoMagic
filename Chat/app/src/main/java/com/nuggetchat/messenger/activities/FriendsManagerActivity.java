package com.nuggetchat.messenger.activities;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.CallbackManager;
import com.facebook.login.LoginManager;
import com.facebook.share.model.ShareLinkContent;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nuggetchat.lib.common.RequestParams;
import com.nuggetchat.lib.model.FriendInfo;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.UserFriendsAdapter;
import com.nuggetchat.messenger.datamodel.UserDetails;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;
import com.nuggetchat.messenger.utils.ViewUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class FriendsManagerActivity extends AppCompatActivity {
    private static final String LOG_TAG = FriendsManagerActivity.class.getSimpleName();
    ArrayList<FriendInfo> selectUsers;
    List<UserDetails> temp;
    // Contact List
    ListView listView;
    // Cursor to load contacts list
    Cursor phones, email;

    // Pop up
    ContentResolver resolver;
    UserFriendsAdapter adapter;
    CallbackManager callbackManager;
    Intent intent;

    @BindView(R.id.friends_manager_progress_bar) /* package-local */ ProgressBar friendsManagerProgressBar;
    @BindView(R.id.invite_friends_text) /* package-local */ TextView inviteFriendsText;
    @BindView(R.id.swipeContainer) /* package-local */ SwipeRefreshLayout swipeContainer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_friendsmanager);
        ButterKnife.bind(this);
        intent = getIntent();
        getUserFriends(SharedPreferenceUtility.getFacebookAccessToken(this),
                SharedPreferenceUtility.getFirebaseIdToken(this),
                SharedPreferenceUtility.getFirebaseUid(this));
        callbackManager = CallbackManager.Factory.create();

        selectUsers = new ArrayList<>();
        resolver = this.getContentResolver();
        listView = (ListView) findViewById(R.id.contacts_list);
        ShareLinkContent linkContent = new ShareLinkContent.Builder()
                .setContentTitle("....")
                .build();
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                selectUsers.clear();
                getUserFriends(SharedPreferenceUtility.getFacebookAccessToken(FriendsManagerActivity.this),
                        SharedPreferenceUtility.getFirebaseIdToken(FriendsManagerActivity.this));
            }
        });

        swipeContainer.setColorSchemeResources(android.R.color.holo_red_light,
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light);
    }

    public void sendMessagetoFriends(View v) {
        Log.d(LOG_TAG, "Message to friends called");

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setPackage("com.facebook.orca");
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "Hey! How are you? I just found this awesome app where we can chat and play simultaneously. Lets play Nugget!");
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "You do not have Facebook Messenger installed", Toast.LENGTH_LONG).show();
        }
    }

    public void sendShareIntent(View v) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "Hey! How are you? I just found this awesome app where we can chat and play simultaneously. Lets play Nugget!");
        startActivity(intent);
    }

    @OnClick(R.id.skip_friends_addition)
    /* package-local */ void skipFriendsAddition() {
        if (intent.getStringExtra("user_id") == null) {
            Intent intent = new Intent(FriendsManagerActivity.this, GamesChatActivity.class);
            startActivity(intent);
        }
        finish();
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            ViewUtils.setWindowImmersive(getWindow());
        }
    }

    public void getUserFriends(final String accessToken, final String idToken, final String firebaseUid) {
        friendsManagerProgressBar.setVisibility(VISIBLE);
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://server.nuggetchat.com:8080/getFriends";
        StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(LOG_TAG, "Request success " + response);
                getFriendsFromFirebase(firebaseUid);
                adapter = new UserFriendsAdapter(selectUsers, FriendsManagerActivity.this);
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Intent resultIntent = new Intent(FriendsManagerActivity.this, GamesChatActivity.class);
                        resultIntent.putExtra("user_id", ((FriendInfo) adapterView.getAdapter().getItem(i)).getFacebookId());
                        if (intent.getStringExtra("user_id") == null) {
                            startActivity(resultIntent);
                            finish();
                            return;
                        }
                        setResult(1234, resultIntent);
                        finish();
                    }
                });
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG_TAG, "Error in making friends request", error);
                friendsManagerProgressBar.setVisibility(INVISIBLE);
            }
        }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put(RequestParams.FACEBOOK_ACCESS_TOKEN, accessToken);
                params.put(RequestParams.FIREBASE_ID_TOKEN, idToken);
                return params;
            }
        };
        queue.add(sr);
    }

    private void getFriendsFromFirebase(String firebaseId) {
        String firebaseUri = "https://nuggetplay-ceaaf.firebaseio.com/users/" + firebaseId + "/friends";
        Log.i(LOG_TAG, "Fetching user friends : , " + firebaseUri);

        final DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseUri);
        friendsManagerProgressBar.setVisibility(INVISIBLE);
        swipeContainer.setRefreshing(false);
        inviteFriendsText.setVisibility(VISIBLE);
        if (firebaseRef == null) {
            Log.e(LOG_TAG, "Unable to get database reference.");
            return;
        }
        final ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    FriendInfo friendInfo = snapshot.getValue(FriendInfo.class);
                    if (!selectUsers.contains(friendInfo)) {
                        selectUsers.add(friendInfo);
                        adapter.notifyDataSetChanged();
                    }
                    friendsManagerProgressBar.setVisibility(INVISIBLE);
                    inviteFriendsText.setVisibility(INVISIBLE);
                }
                firebaseRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        firebaseRef.addValueEventListener(valueEventListener);
    }

    public void getUserFriends(final String accessToken, final String idToken) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://server.nuggetchat.com:8080/getFriends";
        StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i(LOG_TAG, "Facebook login success ");
//                Log.i(LOG_TAG, "Facebook response " + response);
                getUserFriends(SharedPreferenceUtility.getFacebookAccessToken(FriendsManagerActivity.this),
                        SharedPreferenceUtility.getFirebaseIdToken(FriendsManagerActivity.this),
                        SharedPreferenceUtility.getFirebaseUid(FriendsManagerActivity.this));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG_TAG, "Error in making friends request", error);
            }
        }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put(RequestParams.FACEBOOK_ACCESS_TOKEN, accessToken);
                params.put(RequestParams.FIREBASE_ID_TOKEN, idToken);
                return params;
            }
        };
        queue.add(sr);
    }
}
