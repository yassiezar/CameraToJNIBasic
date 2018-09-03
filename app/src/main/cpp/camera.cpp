#include <camera.hpp>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT bool JNICALL
Java_com_upwork_jaycee_cameratojnibasic_JNIWrapper_YUV2Greyscale(JNIEnv* env, jobject obj, jint width, jint height, jobject srcBuffer, jint stride, jobject surface)
{
    uint8_t *srcPtr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(srcBuffer));
    if (srcPtr == nullptr)
    {
        __android_log_write(ANDROID_LOG_ERROR, "JNI", "Buffer null error");
        return false;
    }

    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    ANativeWindow_acquire(window);

    ANativeWindow_Buffer buffer;
    ANativeWindow_setBuffersGeometry(window, width, height, 0);

    if(ANativeWindow_lock(window, &buffer, NULL) != 0)
    {
        __android_log_write(ANDROID_LOG_ERROR, "JNI", "Window acquisition error");
        ANativeWindow_release(window);
        return false;
    }

    /*
    uint32_t* dstBuffer = reinterpret_cast<uint32_t *>(buffer.bits);
    uint32_t* dstBuffer_orig = dstBuffer;

    // Buffer
    int32_t buferWidth = buffer.width;
    int32_t bufferHeight = buffer.height;
    int32_t bufferStride = buffer.stride;

    // Window
    int32_t winWidth = ANativeWindow_getWidth(window);
    int32_t winHeight = ANativeWindow_getHeight(window);
    int32_t winFormat = ANativeWindow_getFormat(window);

    for (int srcCol = 0; srcCol < width; srcCol++) {
        for (int srcRow = height - 1; srcRow >= 0; srcRow--) {

            // packRGBA() just converts YUV to RGB.
            *dstBuffer = YUV2RGB(srcBuf[srcRow * width + srcCol], 0, 0);
            dstBuffer++;

            // We cannot simple write to destination pixels sequentially because of the
            // stride. Stride is the actual memory buffer width, while the image width is only
            // the wdith of valid pixels.
            // If we reach the end of a source row, we need to advance our destination
            // pointer to skip the padding cells.
            if (srcRow == 0)
                dstBuffer += (bufferStride - winWidth);

        }
    }*/

    //memcpy(buffer.bits, srcPtr, width * height * 4);
    uint8_t *outPtr = reinterpret_cast<uint8_t *>(buffer.bits);
    for (size_t y = 0; y < height; y++)
    {
       uint8_t* rowPtr = srcPtr + y * width;
       for (size_t x = 0; x < width; x++)
       {
            // packRGBA() just converts YUV to RGB.
            *(outPtr++) = *rowPtr;
            *(outPtr++) = *rowPtr;
            *(outPtr++) = *rowPtr;
            *(outPtr++) = 255; // gamma for RGBA_8888
            ++rowPtr;
        }
    }

    ANativeWindow_unlockAndPost(window);
    ANativeWindow_release(window);

    return true;
}

#ifdef __cplusplus
}
#endif