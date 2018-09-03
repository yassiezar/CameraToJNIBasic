package com.upwork.jaycee.cameratojnibasic;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback
{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    private static final int STATE_PREVIEW = 0;

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener()
    {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
        {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
        {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
        {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
    };


    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback()
    {
        @Override
        public void onOpened(@NonNull CameraDevice camera)
        {
            cameraOpenCloseLock.release();
            MainActivity.this.camera = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera)
        {
            cameraOpenCloseLock.release();
            camera.close();
            MainActivity.this.camera = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error)
        {
            cameraOpenCloseLock.release();
            camera.close();
            MainActivity.this.camera = null;
            MainActivity.this.finish();
        }
    };

    private final ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener()
    {
        @Override
        public void onImageAvailable(ImageReader reader)
        {
            /* TODO: Send image to JNI */
            Image img = reader.acquireLatestImage();
            if(img == null)
            {
                Log.e(TAG, "No image");
                return;
            }

            if(!JNIWrapper.YUV2Greyscale(img.getWidth(), img.getHeight(), img.getPlanes()[0].getBuffer(),img.getPlanes()[0].getPixelStride(), surface))
            {
                Log.e(TAG, "Image display error");
            }
            img.close();
        }
    };

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback()
    {

        private void process(CaptureResult result)
        {
            switch (state) {
                case STATE_PREVIEW:
                {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
            }
        }
    };

    private Semaphore cameraOpenCloseLock = new Semaphore(1);

    private CameraDevice camera;
    private ImageReader imageReader;
    private Size previewSize;
    private CaptureRequest.Builder previewRequestBuilder;
    private CameraCaptureSession captureSession;
    private CaptureRequest previewRequest;

    // Thread and handler to control the camera data
    private Handler cameraHandler;
    private HandlerThread cameraBackgroundThread;

    private AutofitTexture cameraTexture;
    private Surface surface;

    private int sensorOrientation;
    private int state = STATE_PREVIEW;

    private String cameraID;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraTexture = findViewById(R.id.surface);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        startBackgroundThread();

        // Recapture texture
        if(cameraTexture.isAvailable())
        {
            openCamera(cameraTexture.getWidth(), cameraTexture.getHeight());
        }
        else
        {
            cameraTexture.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onPause()
    {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_CAMERA_PERMISSION)
        {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                Log.e(TAG, "Camera permission error");
            }
        }
        else
        {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void requestCameraPermission()
    {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
        {
            Log.e(TAG, "Camera permission error");
        }
        else
        {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void setupCamera(int width, int height)
    {
        Log.e(TAG, "Setup Camera");
        CameraManager cameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try
        {
            for(String cameraID : cameraManager.getCameraIdList())
            {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraID);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT)
                {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if(map == null)
                {
                    continue;
                }

                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)), new CompareSizesByArea());
                imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.YUV_420_888, 2);
                imageReader.setOnImageAvailableListener(imageAvailableListener, cameraHandler);

                int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swapped = false;

                switch (displayRotation)
                {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (sensorOrientation == 90 || sensorOrientation == 270)
                        {
                            swapped = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (sensorOrientation == 0 || sensorOrientation == 180)
                        {
                            swapped = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedWidth = width;
                int rotatedHeight = height;
                int maxWidth = displaySize.x;
                int maxHeight = displaySize.y;

                if(swapped)
                {
                    rotatedHeight = width;
                    rotatedWidth = height;
                    maxWidth = displaySize.y;
                    maxHeight = displaySize.x;
                }

                if(maxWidth > MAX_PREVIEW_WIDTH) maxWidth = MAX_PREVIEW_WIDTH;
                if(maxHeight > MAX_PREVIEW_HEIGHT) maxHeight = MAX_PREVIEW_HEIGHT;

                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedWidth, rotatedHeight, maxWidth,
                        maxHeight, largest);

                int orientation = getResources().getConfiguration().orientation;
                /*if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                {
                    cameraTexture.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                }
                else
                {
                    cameraTexture.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                }*/

                this.cameraID = cameraID;

                return;
            }
        }
        catch(CameraAccessException e)
        {
            Log.e(TAG, "Camera access issue: " + e);
        }
        catch(NullPointerException e)
        {
            Log.e(TAG, "Null pointer, camera not accessable on device: " + e);
        }
    }

    private void openCamera(int width, int height)
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            requestCameraPermission();
            return;
        }

        Log.e(TAG, "Open Camera");
        setupCamera(width, height);
        configureTransform(width, height);
        CameraManager cameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);

        try
        {
            if(!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS))
            {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            cameraManager.openCamera(cameraID, cameraStateCallback, cameraHandler);
        }
        catch(CameraAccessException e)
        {
            Log.e(TAG, "Camera access issue: " + e);
        }
        catch(InterruptedException e)
        {
            Log.e(TAG, "Could not acquire camera lock: " + e);
        }
    }

    private void closeCamera()
    {
        try
        {
            cameraOpenCloseLock.acquire();
            if (null != captureSession)
            {
                captureSession.close();
                captureSession = null;
            }
            if(camera != null)
            {
                camera.close();
                camera = null;
            }
            if(imageReader != null)
            {
                imageReader.close();
                imageReader = null;
            }
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        }
        finally
        {
            cameraOpenCloseLock.release();
        }
    }

    private void startBackgroundThread()
    {
        cameraBackgroundThread = new HandlerThread("CameraBackground");
        cameraBackgroundThread.start();
        cameraHandler = new Handler(cameraBackgroundThread.getLooper());
    }

    private void stopBackgroundThread()
    {
        cameraBackgroundThread.quitSafely();
        try
        {
            cameraBackgroundThread.join();
            cameraBackgroundThread = null;
            cameraHandler = null;
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        }
    }

    private void createCameraPreviewSession()
    {
        Log.e(TAG, "Create preview");
        try
        {
            SurfaceTexture texture = cameraTexture.getSurfaceTexture();
            assert cameraTexture != null;

            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            surface = new Surface(texture);

            previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // previewRequestBuilder.addTarget(surface);
            previewRequestBuilder.addTarget(imageReader.getSurface());

            List<Surface> outputSurfaces = new ArrayList<>();
            outputSurfaces.add(imageReader.getSurface());
            // outputSurfaces.add(surface);
            camera.createCaptureSession(outputSurfaces,
                    new CameraCaptureSession.StateCallback()
                    {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session)
                        {
                            if(camera == null)
                            {
                                return;
                            }
                            captureSession = session;
                            try
                            {
                                // previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                                previewRequest = previewRequestBuilder.build();
                                captureSession.setRepeatingRequest(previewRequest, captureCallback, cameraHandler);
                            }
                            catch (CameraAccessException e)
                            {
                                Log.e(TAG, "Camera access error: " + e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session)
                        {
                            Log.e(TAG, "Preview configure failed");
                        }
                    }, cameraHandler
            );
        }
        catch (CameraAccessException e)
        {
            Log.e(TAG, "Camera access error: " + e);
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio)
    {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();

        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();

        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices)
        {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight && option.getHeight() == option.getWidth() * h / w)
            {
                if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight)
                {
                    bigEnough.add(option);
                }
                else
                {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0)
        {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        else if (notBigEnough.size() > 0)
        {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        }
        else
        {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[2];
        }
    }

    static class CompareSizesByArea implements Comparator<Size>
    {

        @Override
        public int compare(Size lhs, Size rhs)
        {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private void configureTransform(int viewWidth, int viewHeight)
    {
        if (null == cameraTexture || null == previewSize)
        {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        cameraTexture.setTransform(matrix);
    }
}
