package com.nuggetchat.messenger.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;
import android.widget.TextView;

import com.nuggetchat.messenger.R;

public class CustomFontTextView extends TextView {

    public CustomFontTextView(Context context) {
        super(context);
        setFont(context, null /* attrs */);
    }

    public CustomFontTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFont(context, attrs);
    }

    public CustomFontTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFont(context, attrs);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (type == BufferType.NORMAL && text instanceof String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                text = Html.fromHtml(text.toString(), Html.FROM_HTML_MODE_COMPACT);
            } else {
                @SuppressWarnings("deprecation")
                Spanned newText = Html.fromHtml(text.toString());
                text = newText;
            }
        }

        super.setText(text, type);
    }

    private void setFont(Context context, AttributeSet attrs) {
        String fontName = null;

        if (attrs != null) {
            TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                    attrs, R.styleable.CustomFontTextView, 0, 0);

            try {
                fontName = typedArray.getString(R.styleable.CustomFontTextView_font_name);
            } finally {
                typedArray.recycle();
            }
        }

        if (fontName == null) {
            throw new IllegalArgumentException("fontName not specified.");
        }

        if (isInEditMode()) {
            return;
        }

        Typeface font = Typefaces.get(context, "fonts/" + fontName);
        if (font != null) {
            setTypeface(font);
        }
    }
}
