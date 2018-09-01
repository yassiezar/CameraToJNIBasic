package com.upwork.jaycee.cameratojnibasic;

import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;

public class JNIWrapper
{
    static
    {
        System.loadLibrary("camera");
        Log.e("LibLoader", "Loaded library");
    }

    public static native boolean YUV2Greyscale(int width, int height, ByteBuffer buffer, Surface surface);
}
