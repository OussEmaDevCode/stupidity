package com.waypo.stupidCam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Date;

public class ShareFragment extends Fragment {
    private static final String IMAGE = "image";
    private static final String CAMERA_ORIENTATION = "camera orientation";

    private byte[] imageBytes;
    private int cameraOrientation;

    private ImageView image;
    private FloatingActionButton fab;
    private View progress;

    public ShareFragment() {
    }

    public static ShareFragment newInstance(byte[] image, int cameraOrientation) {
        ShareFragment fragment = new ShareFragment();
        Bundle args = new Bundle();
        args.putByteArray(IMAGE, image);
        args.putInt(CAMERA_ORIENTATION, cameraOrientation);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageBytes = getArguments().getByteArray(IMAGE);
            cameraOrientation = getArguments().getInt(CAMERA_ORIENTATION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_share, container, false);
        image = rootView.findViewById(R.id.image);
        progress = rootView.findViewById(R.id.progress);
        fab = rootView.findViewById(R.id.fabsave);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Matrix matrix = new Matrix();
        matrix.postRotate(cameraOrientation == 0 ? 90 : -90);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        image.setImageBitmap(bitmap);
        final Bitmap finalBitmap = bitmap;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progress.setVisibility(View.VISIBLE);

                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath();
                File dir = new File(path + "/" + getString(R.string.stupid_pictures));
                if (!dir.exists()) {
                    dir.mkdir();
                }

                File picture = new File(dir, getString(R.string.sutpid_picture) + String.valueOf(new Date().getTime()) + ".jpg");
                try (FileOutputStream fout = new FileOutputStream(picture)) {
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fout);
                    fout.flush();
                    fout.close();
                } catch (IOException ignore) {
                }

                MediaScannerConnection.scanFile(
                        getActivity(),
                        new String[]{picture.getAbsolutePath()},
                        new String[]{"image/jpeg"},
                        null);

                progress.setVisibility(View.GONE);

                Toast.makeText(getActivity(), R.string.image_saved, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
