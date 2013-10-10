package com.mavilion.treasurehunt;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Mav on 19/09/13.
 */
public class Chest implements Parcelable{
    public String ID;
    public Location location;
    public String bssID;

    public Chest (String id, Location loc, String bss){
        ID = id;
        location = loc;
        bssID = bss;
    }

    public Chest (JSONObject jsonObject, Location loc){
        try {
            ID = jsonObject.getString("id");
            bssID = jsonObject.getString("bssid");
            double distance = jsonObject.getDouble("distance");
            double degree = jsonObject.getDouble("degree");
            location = new Location(ID);
            double distanceX = distance * Math.sin(Math.PI - degree * Math.PI / 180);
            double distanceY = distance * Math.cos(Math.PI - degree * Math.PI / 180);
            double latitude = (180/Math.PI)*Math.asin( Math.sin(loc.getLatitude()*Math.PI/180)*Math.cos(distance/6378137) +
                    Math.cos(loc.getLatitude()*Math.PI/180)*Math.sin(distance/6378137)*Math.cos(degree*Math.PI/180));
            double longitude = loc.getLongitude() + (180/Math.PI)*Math.atan2(Math.sin(degree*Math.PI/180)*Math.sin(distance/6378137)*Math.cos(loc.getLatitude()*Math.PI/180),
                    Math.cos(distance/6378137)-Math.sin(loc.getLatitude()*Math.PI/180)*Math.sin(latitude*Math.PI/180));
            location.setLatitude(latitude);
            location.setLongitude(longitude);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int i) {
        out.writeString(ID);
        out.writeString(location.getProvider());
        out.writeDouble(location.getLatitude());
        out.writeDouble(location.getLongitude());
        out.writeString(bssID);
    }

    public static final Parcelable.Creator<Chest> CREATOR = new Parcelable.Creator<Chest>() {
        public Chest createFromParcel(Parcel in) {
            return new Chest(in);
        }

        public Chest[] newArray(int size) {
            return new Chest[size];
        }
    };

    private Chest(Parcel in) {
        ID = in.readString();
        location = new Location(in.readString());
        location.setLatitude(in.readDouble());
        location.setLongitude(in.readDouble());
        bssID = in.readString();
    }
}
