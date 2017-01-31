package com.nuggetchat.messenger.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nuggetchat.lib.Conf;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.utils.GlideUtils;
import com.nuggetchat.messenger.utils.MyLog;

import java.util.ArrayList;

public class CustomGridAdapter extends BaseAdapter {
    private Context context;
    private  final ArrayList<GamesItem> gameItemList;

    public CustomGridAdapter(Context context, ArrayList<GamesItem> gameItemList) {
        this.context = context;
        this.gameItemList = gameItemList;
    }
    @Override
    public int getCount() {
        return gameItemList.size();
    }

    @Override
    public Object getItem(int i) {
        return gameItemList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.grid_item, parent, false);
        }
        TextView textView = (TextView) convertView.findViewById(R.id.grid_text);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.grid_image);
        ImageView lockIcon = (ImageView) convertView.findViewById(R.id.lock_icon);
        ImageView stars = (ImageView) convertView.findViewById(R.id.stars);
        ImageView featured = (ImageView) convertView.findViewById(R.id.featured);

        textView.setText(gameItemList.get(position).getGamesName());

        int value = gameItemList.get(position).getValue();
        if(value >=5) {
            stars.setBackgroundResource(R.drawable.five_stars);
        } else if(value==4) {
            stars.setBackgroundResource(R.drawable.four_stars);
        } else if(value==3) {
            stars.setBackgroundResource(R.drawable.three_stars);
        } else if(value==2) {
            stars.setBackgroundResource(R.drawable.two_stars);
        } else if(value==1) {
            stars.setBackgroundResource(R.drawable.one_stars);
        }

        if(value==6) {
            featured.setVisibility(View.VISIBLE);
        } else {
            featured.setVisibility(View.INVISIBLE);
        }

        String imageURl = Conf.CLOUDINARY_PREFIX_URL + gameItemList.get(position).getGamesImage();
        MyLog.d("The image uri " , imageURl);
        GlideUtils.loadImage(context, imageView, null, imageURl);

        return convertView;
    }
}
