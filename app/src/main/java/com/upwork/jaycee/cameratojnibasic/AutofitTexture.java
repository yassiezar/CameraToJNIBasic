package com.upwork.jaycee.cameratojnibasic;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class AutofitTexture extends TextureView
{
    private int ratioWidth = 0;
    private int ratioHeight = 0;

    public AutofitTexture(Context context)
    {
        this(context, null);
    }

    public AutofitTexture(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public AutofitTexture(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    public void setAspectRatio(int width, int height)
    {
        if (width < 0 || height < 0)
        {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        ratioWidth = width;
        ratioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (0 == ratioWidth || 0 == ratioHeight)
        {
            setMeasuredDimension(width, height);
        }
        else
        {
            if (width < height * ratioWidth / ratioHeight)
            {
                setMeasuredDimension(width, width * ratioHeight / ratioWidth);
            }
            else
            {
                setMeasuredDimension(height * ratioWidth / ratioHeight, height);
            }
        }
    }
}
