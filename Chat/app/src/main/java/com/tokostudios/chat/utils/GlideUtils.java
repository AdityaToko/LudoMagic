package com.tokostudios.chat.utils;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.tokostudios.chat.BuildConfig;

public class GlideUtils {
    private static final String LOG_TAG = GlideUtils.class.getSimpleName();

    /**
     * Loads image into image view. Can support both Gif and static images sent in imageUrl.
     * <p>
     * Use diskCacheStrategy {@link DiskCacheStrategy#SOURCE} for GIFs Since using 3.x of Glide
     * https://github.com/bumptech/glide/issues/358
     *
     * @param context Context to load image (should be nearest context, avoid application context
     * @param imageView {@link ImageView} reference on which image has to be loaded
     * @param thumbnailUrl Url to be displayed till the image is fully loaded
     * @param imageUrl Actual url to image
     */
    public static void loadImage(Context context, ImageView imageView, String thumbnailUrl,
            String imageUrl) {
        loadImage(context, imageView, thumbnailUrl, imageUrl, null);
    }

    public static void loadImage(Context context, ImageView imageView, String thumbnailUrl,
            final String imageUrl, final ProgressBar progressBar) {
        if (imageView == null) {
            return;
        }

        if (imageUrl != null && !imageUrl.equals("")) {
            if (imageUrl.startsWith("https://i.ytimg.com/")) {
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
            DrawableRequestBuilder<String> thumbnailRequest = null;
            if (thumbnailUrl != null && !thumbnailUrl.equals("")) {
                thumbnailRequest = Glide.with(context)
                        .load(thumbnailUrl)
                        .diskCacheStrategy(DiskCacheStrategy.RESULT);
            }

            DiskCacheStrategy cacheStrategy = DiskCacheStrategy.RESULT;

            DrawableRequestBuilder<String> drawableRequestBuilder = Glide.with(context)
                    .load(imageUrl)
                    .thumbnail(thumbnailRequest)
                    .diskCacheStrategy(cacheStrategy);
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
                drawableRequestBuilder = drawableRequestBuilder
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model,
                                    Target<GlideDrawable> target, boolean isFirstResource) {
                                Log.e(LOG_TAG, "Error in downloading Image. "
                                        + (BuildConfig.DEBUG ? imageUrl : ""), e);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model,
                                    Target<GlideDrawable> target, boolean isFromMemoryCache,
                                    boolean isFirstResource) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }
                        });

            }
            drawableRequestBuilder.into(imageView);
        } else {
            Glide.clear(imageView);
            imageView.setImageDrawable(null);
        }
    }
}
