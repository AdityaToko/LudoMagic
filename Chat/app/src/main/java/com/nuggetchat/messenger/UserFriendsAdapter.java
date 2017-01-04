package com.nuggetchat.messenger;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.nuggetchat.lib.model.FriendInfo;

import java.util.List;

public class UserFriendsAdapter extends BaseAdapter {

    public List<FriendInfo> userDetails;
    Context context;

    public UserFriendsAdapter(List<FriendInfo> selectUsers, Context context) {
        userDetails = selectUsers;
        this.context = context;
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
            LayoutInflater li = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = li.inflate(R.layout.list_item, null);
        } else {
            view = convertView;
        }

        final ViewHolder viewHolder = new ViewHolder();

        viewHolder.title = (TextView) view.findViewById(R.id.name);
        viewHolder.imageView = (ImageView) view.findViewById(R.id.profile_image);

        final FriendInfo data = userDetails.get(i);
        viewHolder.title.setText(data.getName());

        // Set image if exists
        try {
            String profilePicUrl;
            if (data.getFacebookId() != null) {
                profilePicUrl = getProfilePicUrl(data.getFacebookId());
                Glide.with(context).load(profilePicUrl).asBitmap().centerCrop().into(new BitmapImageViewTarget(viewHolder.imageView) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circularBitmapDrawable =
                                RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                        circularBitmapDrawable.setCircular(true);
                        viewHolder.imageView.setImageDrawable(circularBitmapDrawable);
                    }
                });

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

    private static class ViewHolder {
        ImageView imageView;
        TextView title;
    }

    private String getProfilePicUrl(String facebookUserId) {
        return "https://graph.facebook.com/" + facebookUserId + "/picture?width=200&height=150";
    }
}
