package com.mavilion.treasurehunt;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mav on 18/09/13.
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback{

    private Camera camera;

    public CameraView(Context context,AttributeSet attrs) {
        super(context, attrs);

        //setZOrderOnTop(true);
        //getHolder().setFormat(PixelFormat.TRANSPARENT);
        getHolder().addCallback(this);

        setFocusable(true);
    }

    public void setCamera(Camera cam) {
        camera = cam;
        try {
            Camera.Parameters parameters = camera.getParameters();
            if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                parameters.set("orientation", "portrait");
                camera.setDisplayOrientation(90);
                parameters.setRotation(90);
            } else {
                parameters.set("orientation", "landscape");
                camera.setDisplayOrientation(0);
                parameters.setRotation(0);
            }
            parameters.setPictureSize(640, 480);
            camera.setParameters(parameters);
            camera.setPreviewDisplay(getHolder());
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        if (camera != null) {
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }
}
