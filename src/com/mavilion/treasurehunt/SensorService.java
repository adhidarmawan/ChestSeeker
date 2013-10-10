package com.mavilion.treasurehunt;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.util.List;

/**
 * Created by Mav on 18/09/13.
 */
public class SensorService implements LocationListener, SensorEventListener {

    private final Context context;

    public SensorService(Context context) {
        this.context = context;
    }

    //Location Service

    private boolean isGPSProviderEnabled = false;
    private boolean isNetworkProviderEnabled = false;
    private boolean isGPSEnabled = false;

    private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1;
    private static final long MINIMUM_TIME_BETWEEN_UPDATES = 10000;

    protected LocationManager locationManager;
    private Location location = null;

    public void setGPSEnabled(boolean value) {
        if(isGPSEnabled == value)return;
        if(value){
            try {
                locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                isGPSProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                isNetworkProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                if (!isGPSProviderEnabled && !isNetworkProviderEnabled) {
                } else {
                    isGPSEnabled = true;
                    if (isNetworkProviderEnabled) {
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MINIMUM_TIME_BETWEEN_UPDATES, MINIMUM_DISTANCE_CHANGE_FOR_UPDATES, this);
                    }
                    if (isGPSProviderEnabled) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINIMUM_TIME_BETWEEN_UPDATES, MINIMUM_DISTANCE_CHANGE_FOR_UPDATES, this);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            if(locationManager != null){
                locationManager.removeUpdates(SensorService.this);
                locationManager = null;
            }
            isGPSEnabled = false;
        }
    }

    public Location getLocation() {
        if(isGPSEnabled) {
            if (isNetworkProviderEnabled) {
                if (locationManager != null) {
                    Location getlocnet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (getlocnet != null) {
                        location = getlocnet;
                    }
                }
            }
            if (isGPSProviderEnabled) {
                if (locationManager != null) {
                    Location getlocgps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (getlocgps != null) {
                        location = getlocgps;
                    }
                }
            }
        }
        return location;
    }

    public boolean isGPSEnabled() {
        return isGPSEnabled;
    }

    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle("GPS is settings");
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(intent);
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    public void onLocationChanged(Location location) {
    }

    public void onProviderDisabled(String provider) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    //Sensor Service
    protected SensorManager sensorManager;
    private double compass;

    private boolean isCompassEnabled = false;

    public void setCompassEnabled(boolean value){
        if(isCompassEnabled == value)return;
        if(value){
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            Sensor magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            Sensor gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            isCompassEnabled = true;
        } else {
            if(sensorManager != null){
                sensorManager.unregisterListener(this);
                sensorManager = null;
            }
            isCompassEnabled = false;
        }
    }

    public double getCompass() {
        return compass;
    }

    private float[] mGravity = null;
    private float[] mGeomagnetic = null;

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                compass = 360 - ((double)orientation[0] * 180/Math.PI);
            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    //Wifi Service
    protected WifiManager wifiManager;

    private boolean isWifiEnabled = false;

    public void setWifiEnabled(boolean value){
        if(isWifiEnabled == value)return;
        if(value){
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        } else {
            if(wifiManager != null){
                wifiManager = null;
            }
            isWifiEnabled = false;
        }
    }

    public int getWifiSignal(String bssID){
        int signal = 0;
        List<ScanResult> results = wifiManager.getScanResults();
        if (results != null) {
            for(int i = 0;i<results.size();i++){
                ScanResult result = results.get(i);
                if(result.BSSID.equals(bssID)){
                    signal = result.level;
                }
            }
        }
        return signal;
    }

    public void removeAllEnabled(){
        setGPSEnabled(false);
        setCompassEnabled(false);
        setWifiEnabled(false);
    }
}