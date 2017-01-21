package com.nuggetchat.messenger.activities;

import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nuggetchat.lib.Conf;
import com.nuggetchat.lib.common.RequestParams;
import com.nuggetchat.lib.model.FriendInfo;
import com.nuggetchat.messenger.NuggetInjector;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.UserFriendsAdapter;
import com.nuggetchat.messenger.utils.FirebaseAnalyticsConstants;
import com.nuggetchat.messenger.utils.MyLog;
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
    private ArrayList<FriendInfo> usersFriendList;
    // Contact List
    private ListView listView;

    // Pop up
    private ContentResolver resolver;
    private UserFriendsAdapter adapter;
    private CallbackManager callbackManager;
    private Intent intent;
    private Handler mainHandler;
    @BindView(R.id.friends_manager_progress_bar) /* package-local */ ProgressBar
            friendsManagerProgressBar;
    @BindView(R.id.invite_friends_text) /* package-local */ TextView inviteFriendsText;
    @BindView(R.id.swipeContainer) /* package-local */ SwipeRefreshLayout swipeContainer;
    private NuggetInjector nuggetInjector;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friendsmanager);
        ButterKnife.bind(this);
        intent = getIntent();
        nuggetInjector = NuggetInjector.getInstance();
        getUserFriends();
        callbackManager = CallbackManager.Factory.create();
        mainHandler = new Handler(Looper.getMainLooper());
        usersFriendList = new ArrayList<>();
        resolver = this.getContentResolver();
        listView = (ListView) findViewById(R.id.contacts_list);
        adapter = new UserFriendsAdapter(usersFriendList, FriendsManagerActivity.this);
        listView.setAdapter(adapter);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                usersFriendList.clear();
                getUserFriends();
            }
        });

        swipeContainer.setColorSchemeResources(android.R.color.holo_red_light,
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light);
    }

    public void sendMessageToFriends(View v) {
        MyLog.d(LOG_TAG, "Message to friends called");

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setPackage("com.facebook.orca");
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, ViewUtils.getInviteBody());

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.facebook_messenger_not_found, Toast.LENGTH_LONG)
                    .show();
        }
        nuggetInjector.logEvent(FirebaseAnalyticsConstants.ADD_FACEBOOK_FRIENDS_BUTTON_CLICKED,
                null /* bundle */);
    }

    public void sendShareIntent(View v) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, ViewUtils.getInviteSubject());
        intent.putExtra(Intent.EXTRA_TEXT, ViewUtils.getInviteBody());
        startActivity(intent);
        nuggetInjector.logEvent(FirebaseAnalyticsConstants.ADD_OTHER_FRIENDS_BUTTON_CLICKED,
                null /* bundle */);
    }

    @OnClick(R.id.skip_friends_addition)
    /* package-local */ void skipFriendsAddition() {
        if (intent.getStringExtra("user_id") == null) {
            MyLog.i(LOG_TAG, "the skip button is clicked");
            Intent intent = new Intent(FriendsManagerActivity.this, GamesChatActivity.class);
            startActivity(intent);
        }
        nuggetInjector.logEvent(FirebaseAnalyticsConstants.SKIP_FRIENDS_ADDITION_BUTTON_CLICKED,
                null /* bundle */);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        MyLog.i(LOG_TAG, "On resume - friend activity");
        if (nuggetInjector.isOngoingCall()) {
            MyLog.i(LOG_TAG, "On resume - friend activity");
            finish();
            return;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            ViewUtils.showWindowNavigation(getWindow());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
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

    public void getUserFriends() {
        final String facebookToken =
                SharedPreferenceUtility.getFacebookAccessToken(FriendsManagerActivity.this);
        final String firebaseUid =
                SharedPreferenceUtility.getFirebaseUid(FriendsManagerActivity.this);
        FirebaseAuth.getInstance().getCurrentUser().getToken(true)
                .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (task.isSuccessful()) {
                            String firebaseToken = task.getResult().getToken();
                            MyLog.i(LOG_TAG,
                                    "Refreshing - getUserFriends token:" + firebaseToken + " user:"
                                            + firebaseUid);
                            updateFriendsList(firebaseUid, firebaseToken, facebookToken);
                        } else {
                            MyLog.e(LOG_TAG, "Error in updating friends.", task.getException());
                        }
                    }
                });
    }

    private void updateFriendsList(final String firebaseUid, final String firebaseToken,
            final String facebookToken) {
        friendsManagerProgressBar.setVisibility(VISIBLE);
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest sr = new StringRequest(
                Request.Method.POST,
                Conf.GET_FRIENDS_API_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        MyLog.i(LOG_TAG, "Request success " + response);
                        getFriendsFromFirebase(firebaseUid);
                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i,
                                    long l) {
                                Intent resultIntent = GamesChatActivity
                                        .getNewIntentGameChatActivity(FriendsManagerActivity.this);
                                resultIntent.putExtra("user_id",
                                        ((FriendInfo) adapterView.getAdapter().getItem(i))
                                                .getFacebookId());
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
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        MyLog.d(LOG_TAG, "Error in making friends request", error);
                        friendsManagerProgressBar.setVisibility(INVISIBLE);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put(RequestParams.FACEBOOK_ACCESS_TOKEN, facebookToken);
                params.put(RequestParams.FIREBASE_ID_TOKEN, firebaseToken);
                return params;
            }
        };
        queue.add(sr);
    }

    private void getFriendsFromFirebase(String firebaseId) {
        String firebaseUri = Conf.firebaseUserFriends(firebaseId);
        MyLog.i(LOG_TAG, "Fetching user friends : , " + firebaseUri);

        final DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseUri);
        friendsManagerProgressBar.setVisibility(INVISIBLE);
        inviteFriendsText.setVisibility(VISIBLE);
        swipeContainer.setRefreshing(false);
        if (firebaseRef == null) {
            MyLog.e(LOG_TAG, "Unable to get database reference.");
            return;
        }

        firebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<FriendInfo> newFriendList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    FriendInfo friendInfo = snapshot.getValue(FriendInfo.class);
                    newFriendList.add(friendInfo);
                }
                if (!newFriendList.isEmpty()) {
                    usersFriendList.clear();
                    usersFriendList.addAll(newFriendList);
                    MyLog.d(LOG_TAG, String.valueOf(usersFriendList.size()));
                    SharedPreferenceUtility.setNumberOfFriends(usersFriendList.size(),
                            FriendsManagerActivity.this);
                }
                updateAdapterAndHideProgressBar(usersFriendList.isEmpty());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                MyLog.e(LOG_TAG, "Friend request cancelled," + databaseError);
            }
        });
    }

    private void updateAdapterAndHideProgressBar(final boolean friendListEmpty) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (friendListEmpty) {
                    inviteFriendsText.setVisibility(VISIBLE);
                } else {
                    inviteFriendsText.setVisibility(INVISIBLE);
                }
                friendsManagerProgressBar.setVisibility(INVISIBLE);
                adapter.notifyDataSetChanged();
            }
        });
    }
}
