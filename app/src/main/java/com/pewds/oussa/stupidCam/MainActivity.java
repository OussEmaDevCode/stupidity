package com.pewds.oussa.stupidCam;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private Camera mCamera;
    int CAMERA_SIDE = 0;
    FrameLayout preview;
    private CameraPreview mPreview;
    private static final float SHAKE_THRESHOLD = 17.5f; // m/S**2
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECS = 1000;
    private long mLastShakeTime;
    private Button ok;
    private Button no;
    private boolean flashIsOn = false;
    private SensorManager mSensorMgr;
    private Button captureButton;
    Button flash;
    SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long curTime = System.currentTimeMillis();
                if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECS) {

                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    double acceleration = Math.sqrt(Math.pow(x, 2) +
                            Math.pow(y, 2) +
                            Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH;


                    if (acceleration > SHAKE_THRESHOLD) {
                        mLastShakeTime = curTime;
                        if(mCamera != null) {
                            mCamera.takePicture(null, null, mPicture);
                        }

                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mCamera = getCameraInstance(CAMERA_SIDE);
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(MainActivity.this, mCamera);
        if(flashIsOn){
            Camera.Parameters p = mCamera.getParameters();
            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(p);
        }
        preview.addView(mPreview);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCamera = getCameraInstance(CAMERA_SIDE);
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},3 );

        }
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        captureButton = findViewById(R.id.button_capture);
        captureButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CAMERA_SIDE = (CAMERA_SIDE%2)==0 ? CAMERA_SIDE+1 : CAMERA_SIDE-1;
                        mCamera.stopPreview();
                        mCamera.release();
                        preview.removeAllViews();
                        mCamera = getCameraInstance(CAMERA_SIDE);
                        // Create our Preview view and set it as the content of our activity.
                        mPreview = new CameraPreview(MainActivity.this, mCamera);
                        if(flashIsOn){
                            Camera.Parameters p = mCamera.getParameters();
                            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            mCamera.setParameters(p);
                        }
                        preview.addView(mPreview);
                    }
                }
        );
        flash = findViewById(R.id.flash);
        flash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( MainActivity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)){
                    if(!flashIsOn) {
                        flashIsOn = true;
                        flash.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_flash_off_black_24dp));
                        Camera.Parameters p = mCamera.getParameters();
                        p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        mCamera.setParameters(p);
                    }else {
                        flashIsOn =false;
                        flash.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_flash_on_black_24dp));
                        Camera.Parameters p = mCamera.getParameters();
                        p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        mCamera.setParameters(p);
                    }
                }
            }
        });
        no = findViewById(R.id.no);
        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureButton.setVisibility(View.VISIBLE);
                flash.setVisibility(View.VISIBLE);
                no.setVisibility(View.GONE);
                ok.setVisibility(View.GONE);
                mCamera.startPreview();
            }
        });
        ok = findViewById(R.id.ok);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 3: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    recreate();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},3 );
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        mSensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        // Listen for shakes
        Sensor accelerometer = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            mSensorMgr.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            preview.removeAllViews();
        }
        mSensorMgr.unregisterListener(sensorEventListener);
    }


    public static Camera getCameraInstance(int orientation){
        Camera c = null;
        try {
            c = Camera.open(orientation);// attempt to get a Camera instance


        }
        catch (Exception e){
            Log.e("shit","shit");
        }
        return c; // returns null if camera is unavailable
    }
    /** A basic Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                if (mCamera != null) {
                    mCamera.stopPreview();
                    try {
                        mCamera.setDisplayOrientation(90);

                    }catch (Exception e){}
                    mCamera.setPreviewDisplay(holder);
                    mCamera.startPreview();
                }
            } catch (IOException e) {
                Log.d("bobs", "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e){
                Log.d("bobs", "Error starting camera preview: " + e.getMessage());
            }
        }
    }
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            captureButton.setVisibility(View.GONE);
            flash.setVisibility(View.GONE);
            no.setVisibility(View.VISIBLE);
            ok.setVisibility(View.VISIBLE);
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Bytes.bytes = data;
                    Intent i = new Intent(MainActivity.this,share.class);
                    startActivity(i);
                    finish();
                }
            });
        }
    };
}
