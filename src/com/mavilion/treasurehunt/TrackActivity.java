package com.mavilion.treasurehunt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by Mav on 11/09/13.
 */
public class TrackActivity extends Activity{
    private static final int REQUEST_EXIT = 0;

    private TrackView track_image;
    private Button track_check;

    private Chest target;

    private SensorService sensorService;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        target = (Chest)getIntent().getParcelableExtra("Chest");
        if(target == null){
            Location loc = new Location("Dummy Location");
            loc.setLatitude(-6.890706853863619);
            loc.setLongitude(107.60949573940245);
            target = new Chest("1f65628f83d046aa3ca490b5bc66b1ac",loc,"68:7f:74:70:11:1c");
        }

        sensorService = new SensorService(this);

        track_image = (TrackView) findViewById(R.id.track_image);
        track_check = (Button) findViewById(R.id.track_check);

        track_image.setService(sensorService);
        track_image.setTarget(target);
        track_check.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                if(track_image.getDistance() <= 20){
                    Intent intent = new Intent(TrackActivity.this, CameraActivity.class);
                    intent.putExtra("Chest", (Parcelable) target);
                    startActivityForResult(intent, REQUEST_EXIT);
                    Toast.makeText(TrackActivity.this, "Camera ready!", Toast.LENGTH_SHORT).show();
                    finish();
                }else{
                    Toast.makeText(TrackActivity.this, "Distance too far...", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void onResume(){
        sensorService.setGPSEnabled(true);
        sensorService.setCompassEnabled(true);
        super.onResume();
    }

    public void onPause(){
        super.onPause();
        sensorService.removeAllEnabled();
    }

    public void onDestroy(){
        super.onDestroy();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_EXIT && resultCode == RESULT_OK) {
            this.finish();
        }
    }
}

