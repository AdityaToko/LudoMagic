package com.nuggetchat.messenger.activities;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nuggetchat.lib.Conf;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.utils.GlideUtils;

import java.util.ArrayList;

public class CustomGridAdapter extends BaseAdapter {
    private Context context;
    private  final ArrayList<String> gamesName;
    private final ArrayList<String> gamesImage;

    public CustomGridAdapter(Context context, ArrayList<String> gamesName, ArrayList<String> gamesImage) {
        this.context = context;
        this.gamesName = gamesName;
        this.gamesImage = gamesImage;
    }
    @Override
    public int getCount() {
        return gamesName.size();
    }

    @Override
    public Object getItem(int i) {
        return gamesName.get(i);
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
        textView.setText(gamesName.get(position));
        String imageURl = Conf.CLOUDINARY_PREFIX_URL + gamesImage.get(position);
        Log.d("The image uri " , imageURl);
        GlideUtils.loadImage(context, imageView, null, imageURl);

        return convertView;
    }
}
