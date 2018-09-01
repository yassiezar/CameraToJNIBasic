#include <camera.hpp>

#ifdef __cplusplus
extern "C" {
#endif

uint32_t* YUV2RGB(uint8_t y, uint8_t u, uint8_t v)
{
    uint32_t* rgb;
    rgb = (uint32_t*)malloc(4);

    y -= 16;
    u -= 128;
    v -= 128;
    rgb[0] = 1.164 * y             + 1.596 * v;
    rgb[1] = 1.164 * y - 0.392 * u - 0.813 * v;
    rgb[2] = 1.164 * y + 2.017 * u;
    rgb[3] = 255;

    return rgb;
}

JNIEXPORT bool JNICALL
Java_com_upwork_jaycee_cameratojnibasic_JNIWrapper_YUV2Greyscale(JNIEnv* env, jobject obj, jint width, jint height, jobject srcBuffer, jobject surface)
{
    ANativeWindow * window = ANativeWindow_fromSurface(env, surface);
    ANativeWindow_acquire(window);

    ANativeWindow_Buffer buffer;
    ANativeWindow_setBuffersGeometry(window, width, height, 0);
    ANativeWindow_lock(window, &buffer, NULL);

    uint8_t *srcPtr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(srcBuffer));

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

    uint8_t *outPtr = reinterpret_cast<uint8_t *>(buffer.bits);
    for (size_t y = 0; y < height; y++) {
        uint8_t *rowPtr = srcPtr + y * width;
        for (size_t x = 0; x < width; x++)
        {
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

void RGBfromYUV(double& R, double& G, double& B, double Y, double U, double V)
{
    Y -= 16;
    U -= 128;
    V -= 128;
    R = 1.164 * Y             + 1.596 * V;
    G = 1.164 * Y - 0.392 * U - 0.813 * V;
    B = 1.164 * Y + 2.017 * U;
}

#ifdef __cplusplus
}
#endif