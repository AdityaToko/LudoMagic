package com.nuggetchat.messenger.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nuggetchat.messenger.R;

public class CustomGridAdapter extends BaseAdapter {
    private Context context;
    private  final String[] gamesName;
    private final int[] gamesImage;

    public CustomGridAdapter(Context context, String[] gamesName, int[] gamesImage) {
        this.context = context;
        this.gamesName = gamesName;
        this.gamesImage = gamesImage;
    }
    @Override
    public int getCount() {
        return gamesName.length;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View gridView;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null){
            gridView = new View(context);
            gridView = inflater.inflate(R.layout.grid_item, null);
            TextView textView = (TextView) gridView.findViewById(R.id.grid_text);
            ImageView imageView = (ImageView) gridView.findViewById(R.id.grid_image);
            textView.setText(gamesName[position]);
            imageView.setImageResource(gamesImage[position]);
        } else {
            gridView = (View) convertView;
        }

        return gridView;
    }
}
