package com.mavilion.treasurehunt;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.location.Location;
import android.location.LocationManager;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

/**
 * Created by Mav on 12/09/13.
 */
public class TrackView extends SurfaceView implements SurfaceHolder.Callback{

    private double locteta = 0;
    private double comteta = 0;
    private double teta = 0;
    public double distance = Double.MAX_VALUE;
    private int blipspeed = 0;

    SensorService sensorService;
    Location target;

    public void setService(SensorService sensorservice) {
        sensorService = sensorservice;
    }

    public void setTarget(Chest chest) {
        target = chest.location;
    }

    public double getTeta(){
        return teta;
    }

    public double getDistance(){
        return distance;
    }

    class TrackThread extends Thread {
        private boolean run = false;

        private int alpha = 0;

        private Paint holo, palpha;

        public TrackThread() {
            super();
            holo = new Paint();
            holo.setARGB(255, 51, 181, 229);
            holo.setTextSize(30);
            holo.setAntiAlias(true);
            palpha = new Paint();
            palpha.setAntiAlias(true);
        }

        public void setRunning(boolean run) {
            this.run = run;
        }

        public void run() {
            while (run) {
                Canvas c = null;
                try {
                    c = getHolder().lockCanvas(null);
                    if (c != null) {
                        synchronized (getHolder()) {
                            updateRadar();
                            drawRadar(c);
                        }
                    }
                } finally {
                    if (c != null) {
                        getHolder().unlockCanvasAndPost(c);
                    }
                }
            }
        }

        public void updateRadar() {
            setLocationAndCompass(sensorService.getLocation(),target,sensorService.getCompass());
            if(distance > 100) {
                blipspeed = 1;
            } else if(distance > 20) {
                blipspeed = (int)(26-(distance/4));
            } else {
                blipspeed = 50;
            }
            alpha = alpha <= 0 ? 255 : alpha - blipspeed;
            palpha.setAlpha(alpha);
        }

        public void drawRadar(Canvas canvas) {
            canvas.translate(getWidth()/2-200,getHeight()/2-200);

            canvas.save();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawBitmap(track_bg, 0, 0, palpha);
            canvas.translate(200, 200);
            canvas.rotate((float) teta);
            canvas.translate(-track_arrow.getWidth() / 2, -track_arrow.getHeight() / 2);
            canvas.drawBitmap(track_arrow, 0, 0, null);
            canvas.restore();
        }

        public void setSurfaceSize(int width, int height) {
            synchronized (getHolder()) {
                //
            }
        }
    }

    private TrackThread thread;

    public class Point {
        public double x = 0;
        public double y = 0;
        //constructor
        public Point(double a, double b) {
            x = a;
            y = b;
        }
    }
    private ArrayList<Point> points = new ArrayList<Point>();

    private Bitmap track_bg, track_arrow;

    public TrackView(Context context,AttributeSet attrs) {
        super(context, attrs);

        Resources resources = context.getResources();
        track_bg = BitmapFactory.decodeResource(resources, R.drawable.track_bg);
        track_arrow = BitmapFactory.decodeResource(resources, R.drawable.track_arrow);

        setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSPARENT);
        getHolder().addCallback(this);

        setFocusable(true);
    }

    public void setLocationAndCompass(Location location, Location target, double dir) {
        if(location != null && target != null){
            locteta = location.bearingTo(target);
            distance = location.distanceTo(target);
        }
        comteta = dir;
        teta = locteta + comteta;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        thread.setSurfaceSize(width, height);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        thread = new TrackThread();
        thread.setRunning(true);
        thread.start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }
}