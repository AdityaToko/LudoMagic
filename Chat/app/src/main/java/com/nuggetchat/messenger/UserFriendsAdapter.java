package com.nuggetchat.messenger;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nuggetchat.messenger.datamodel.UserDetails;
import com.nuggetchat.messenger.utils.GlideUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserFriendsAdapter extends BaseAdapter {

    public List<UserDetails> userDetails;
    private ArrayList<UserDetails> userDetailsList;
    Context context;
    ViewHolder viewHolder;

    public UserFriendsAdapter(List<UserDetails> selectUsers, Context context) {
        userDetails = selectUsers;
        this.context = context;
        this.userDetailsList = new ArrayList<>();
        this.userDetailsList.addAll(userDetails);
    }

    @Override
    public int getCount() {
        return userDetails.size();
    }

    @Override
    public Object getItem(int i) {
        return userDetails.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        if (view == null) {
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = li.inflate(R.layout.list_item, null);
        } else {
            view = convertView;
        }

        viewHolder = new ViewHolder();

        viewHolder.title = (TextView) view.findViewById(R.id.name);
        viewHolder.imageView = (ImageView) view.findViewById(R.id.profile_image);

        final UserDetails data = userDetails.get(i);
        viewHolder.title.setText(data.getName());

        // Set image if exists
        try {
            String profilePicUrl;
            if (data.getProfilePicUrl() != null) {
                profilePicUrl = data.getProfilePicUrl();
                GlideUtils.loadImage(context, viewHolder.imageView, null, profilePicUrl);
            } else {
                viewHolder.imageView.setImageResource(R.drawable.nuggeticon);
            }
        } catch (OutOfMemoryError e) {
            // Add default picture
            viewHolder.imageView.setImageDrawable(this.context.getDrawable(R.drawable.nuggeticon));
            e.printStackTrace();
        }

        view.setTag(data);
        return view;
    }

    // Filter Class
    public void filter(String charText) {
        charText = charText.toLowerCase(Locale.getDefault());
        userDetails.clear();
        if (charText.length() == 0) {
            userDetails.addAll(userDetailsList);
        } else {
            for (UserDetails wp : userDetailsList) {
                if (wp.getName().toLowerCase(Locale.getDefault())
                        .contains(charText)) {
                    userDetails.add(wp);
                }
            }
        }
        notifyDataSetChanged();
    }
    static class ViewHolder {
        ImageView imageView;
        TextView title;
    }
}
