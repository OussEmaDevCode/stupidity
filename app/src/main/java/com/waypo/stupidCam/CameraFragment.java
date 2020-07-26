package com.waypo.stupidCam;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import static android.content.Context.SENSOR_SERVICE;

public class CameraFragment extends Fragment {

    private Button ok;
    private Button no;
    private Button reverseButton;

    private Button flash;
    private boolean flashIsOn = false;

    private OnFragmentInteractionListener mListener;

    private android.hardware.Camera mCamera;
    private int CAMERA_SIDE = 0;

    private FrameLayout preview;

    private static final float SHAKE_THRESHOLD = 17.5f;
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECS = 1000;
    private long mLastShakeTime;
    private SensorManager mSensorMgr;
    private SensorEventListener sensorEventListener;
    private byte[] picture;

    public CameraFragment() {
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            setUpCamera();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_camera, container, false);
        preview = rootView.findViewById(R.id.camera_preview);
        reverseButton = rootView.findViewById(R.id.reverse);
        flash = rootView.findViewById(R.id.flash);
        no = rootView.findViewById(R.id.no);
        ok = rootView.findViewById(R.id.ok);
        return rootView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                setUpCamera();
            } else {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setUpListener();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCamera != null) {
            mCamera = getCameraInstance(CAMERA_SIDE);
            toggleFlash();
            preview.addView(new CameraPreview(getActivity(), mCamera));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopCameraPreview();
        if (sensorEventListener != null) {
            mSensorMgr.unregisterListener(sensorEventListener);
        }
    }

    public static android.hardware.Camera getCameraInstance(int orientation) {
        android.hardware.Camera c = null;
        try {
            c = android.hardware.Camera.open(orientation);
        } catch (Exception ignore) {
        }
        return c;
    }

    private android.hardware.Camera.PictureCallback mPicture = new android.hardware.Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data, android.hardware.Camera camera) {
            reverseButton.setVisibility(View.GONE);
            flash.setVisibility(View.GONE);
            no.setVisibility(View.VISIBLE);
            ok.setVisibility(View.VISIBLE);
            picture = data;
        }
    };

    private void setUpCamera() {
        mCamera = getCameraInstance(CAMERA_SIDE);
        preview.addView(new CameraPreview(getActivity(), mCamera));
        sensorEventListener = new SensorEventListener() {
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
                            if (mCamera != null) {
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
        setUpListener();

        reverseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CAMERA_SIDE = CAMERA_SIDE == 0 ? 1 : 0;
                stopCameraPreview();
                mCamera = getCameraInstance(CAMERA_SIDE);
                toggleFlash();
                preview.addView(new CameraPreview(getActivity(), mCamera));
            }
        });

        flash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                    flashIsOn = !flashIsOn;
                    toggleFlash();
                }
            }
        });
        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reverseButton.setVisibility(View.VISIBLE);
                flash.setVisibility(View.VISIBLE);
                no.setVisibility(View.GONE);
                ok.setVisibility(View.GONE);
                mCamera.startPreview();
            }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onImageCaptured(picture, CAMERA_SIDE);
            }
        });
    }

    private void setUpListener() {
        mSensorMgr = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null && sensorEventListener != null) {
            mSensorMgr.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void toggleFlash() {
        android.hardware.Camera.Parameters p = mCamera.getParameters();
        flash.setBackground(ContextCompat.getDrawable(getActivity(), flashIsOn ? R.drawable.ic_flash_off_black_24dp : R.drawable.ic_flash_on_black_24dp));
        p.setFlashMode(flashIsOn ? android.hardware.Camera.Parameters.FLASH_MODE_TORCH : android.hardware.Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(p);
    }

    private void stopCameraPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            preview.removeAllViews();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onImageCaptured(byte[] bytes, int cameraSide);
    }
}

