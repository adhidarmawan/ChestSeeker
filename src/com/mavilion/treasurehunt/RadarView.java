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
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mav on 08/09/13.
 */
public class RadarView extends SurfaceView implements SurfaceHolder.Callback{

    class RadarThread extends Thread {
        private boolean run = false;

        private double teta = 0;

        private double latitude = 0;
        private double longitude = 0;

        private Paint holo;

        public RadarThread() {
            super();
            holo = new Paint();
            holo.setARGB(255, 51, 181, 229);
            holo.setAntiAlias(true);
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
            teta -= 3;
            //Log.v("Location", "(" + latitude + "," +longitude + ")");
        }

        public void drawRadar(Canvas canvas) {
            canvas.translate(getWidth()/2-200,getHeight()/2-200);

            canvas.save();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawBitmap(radar_bg, 0, 0, null);
            canvas.translate(200,200);
            canvas.rotate((float)teta);
            canvas.drawBitmap(radar_sweep, 0, 0, null);
            canvas.restore();

            for(int i=0;i<points.size();i++){
                canvas.drawBitmap(radar_beep, 200 + (float) points.get(i).x - radar_beep.getWidth()/2, 200 + (float) points.get(i).y - radar_beep.getHeight()/2, null);
            }
        }

        public void setSurfaceSize(int width, int height) {
            synchronized (getHolder()) {
                //
            }
        }
    }

    private RadarThread thread;

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

    private Bitmap radar_bg, radar_sweep, radar_beep;

    public RadarView(Context context,AttributeSet attrs) {
        super(context, attrs);

        Resources resources = context.getResources();
        radar_bg = BitmapFactory.decodeResource(resources, R.drawable.radar_bg);
        radar_sweep = BitmapFactory.decodeResource(resources, R.drawable.radar_sweep);
        radar_beep = BitmapFactory.decodeResource(resources, R.drawable.radar_beep);

        setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSPARENT);
        getHolder().addCallback(this);

        setFocusable(true);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        thread.setSurfaceSize(width, height);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        thread = new RadarThread();
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

    public void setChests(List<Chest> chests, Location location) throws JSONException {
        synchronized (getHolder()) {
            points.clear();
            for(int i = 0;i<chests.size();i++){
                Chest chest = chests.get(i);
                double distance = location.distanceTo(chest.location);
                double degree = location.bearingTo(chest.location);
                double distanceX = distance * Math.min(Math.sin(Math.PI - degree * Math.PI / 180),100) * 200/100;
                double distanceY = distance * Math.min(Math.cos(Math.PI - degree * Math.PI / 180),100) * 200/100;
                points.add(new Point(distanceX, distanceY));
            }
        }
    }
}
