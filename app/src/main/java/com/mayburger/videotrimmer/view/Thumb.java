/*
 * MIT License
 *
 * Copyright (c) 2016 Knowledge, education for life.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.mayburger.videotrimmer.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.mayburger.videotrimmer.R;

import java.util.List;
import java.util.Vector;

public class Thumb {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    private int mIndex;
    private float mVal;
    private float mPos;
    private Bitmap mBitmap;
    private int mWidthBitmap;
    private int mHeightBitmap;

    private float mLastTouchX;

    private Thumb() {
        mVal = 0;
        mPos = 0;
    }

    public int getIndex() {
        return mIndex;
    }

    private void setIndex(int index) {
        mIndex = index;
    }

    public float getVal() {
        return mVal;
    }

    public void setVal(float val) {
        mVal = val;
    }

    public float getPos() {
        return mPos;
    }

    public void setPos(float pos) {
        mPos = pos;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(@NonNull Bitmap bitmap) {
        mBitmap = bitmap;
        mWidthBitmap = bitmap.getWidth();
        mHeightBitmap = bitmap.getHeight();
    }

    @NonNull
    public static List<Thumb> initThumbs(Context context,Resources resources) {

        List<Thumb> thumbs = new Vector<>();

        for (int i = 0; i < 2; i++) {
            Thumb th = new Thumb();
            th.setIndex(i);
            if (i == 0) {
                int resImageLeft = R.drawable.ic_thumb_left;
                th.setBitmap(getThumb(context,getBitmapFromVectorDrawable(context,resImageLeft)));
            } else {
                int resImageRight = R.drawable.ic_thumb_right;
                th.setBitmap(getThumb(context,getBitmapFromVectorDrawable(context,resImageRight)));
            }

            thumbs.add(th);
        }
        return thumbs;
    }

     static Bitmap getThumb(Context context,Bitmap thumbBitmap){
        View thumbView = LayoutInflater.from(context).inflate(R.layout.thumb, null,false);
        TextView text = thumbView.findViewById(R.id.text);
        ImageView image = thumbView.findViewById(R.id.thumb);
        image.setImageBitmap(thumbBitmap);
        text.setText("0");
        text.setVisibility(View.INVISIBLE);
        thumbView.measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);
        Bitmap bitmap = Bitmap.createBitmap(thumbView.getMeasuredWidth(),thumbView.getMeasuredHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        thumbView.layout(0,0,thumbView.getMeasuredWidth(),thumbView.getMeasuredHeight());
        thumbView.draw(canvas);
        return bitmap;
    }


    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static int getWidthBitmap(@NonNull List<Thumb> thumbs) {
        return thumbs.get(0).getWidthBitmap();
    }

    public static int getHeightBitmap(@NonNull List<Thumb> thumbs) {
        return thumbs.get(0).getHeightBitmap();
    }

    public float getLastTouchX() {
        return mLastTouchX;
    }

    public void setLastTouchX(float lastTouchX) {
        mLastTouchX = lastTouchX;
    }

    public int getWidthBitmap() {
        return mWidthBitmap;
    }

    private int getHeightBitmap() {
        return mHeightBitmap;
    }
}
