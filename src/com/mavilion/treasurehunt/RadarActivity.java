package com.mavilion.treasurehunt;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Mav on 08/09/13.
 */
public class RadarActivity extends Activity implements LocationListener{

    private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1; // in Meters
    private static final long MINIMUM_TIME_BETWEEN_UPDATES = 10000; // in Milliseconds

    private RadarView radar_image;
    private ListView radar_list;

    private Location location;

    private List<Chest> chests = new ArrayList<Chest>();
    private List<String> chestlist = new ArrayList<String>();

    private SensorService sensorService;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radar);

        sensorService = new SensorService(this);

        radar_image = (RadarView) findViewById(R.id.radar_image);
        radar_list = (ListView) findViewById(R.id.radar_list);
    }

    public void onPause(){
        sensorService.removeAllEnabled();
        super.onPause();
    }

    public void onResume(){
        sensorService.setGPSEnabled(true);
        new GetChestTask().execute(getString(R.string.group_id));
        super.onResume();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 0 , 0, "Reset");
        menu.add(0, 1 , 0, "Refresh");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                new ResetChestTask().execute(getString(R.string.group_id));
                return true;
            case 1:
                new GetChestTask().execute(getString(R.string.group_id));
                return true;
        }
        return false;
    }

    private class GetChestTask extends AsyncTask<String, Integer, JSONObject> {
        ProgressDialog progdialog;

        protected void onPreExecute() {
            progdialog = ProgressDialog.show(RadarActivity.this, "Please Wait", "Loading radar...");
        }
        protected JSONObject doInBackground(String...params) {
            publishProgress(0);
            while(location == null){
                location = sensorService.getLocation();
            }

            publishProgress(1);
            JSONObject object = null;

            try {
                StringBuilder builder = new StringBuilder();
                HttpClient client = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet("http://milestone.if.itb.ac.id/pbd/index.php"
                        +"?group_id="+params[0]
                        +"&latitude="+location.getLatitude()
                        +"&longitude="+location.getLongitude()
                        +"&action="+"retrieve"
                );
                HttpResponse response = client.execute(httpGet);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    object = new JSONObject(builder.toString());
                } else {
                    Log.e("Parse JSON", "Failed to download file");
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return object;
        }

        protected void onProgressUpdate(Integer...progress) {
            switch(progress[0]){
                case 0:
                    progdialog.setMessage("Locating GPS...");
                    break;
                case 1:
                    progdialog.setMessage("Locating Chest...");
                    break;
                default:
                    break;
            }
        }

        protected void onPostExecute(JSONObject result) {
            try {
                if(result.getString("status").equals("success") && !result.isNull("data")){
                    JSONArray JSONchests = result.getJSONArray("data");
                    chests = new ArrayList<Chest>();
                    chestlist = new ArrayList<String>();
                    for (int i = 0; i < JSONchests.length(); ++i) {
                        chests.add(new Chest(JSONchests.getJSONObject(i), location));
                        String loctext = "Location : "+chests.get(i).location.getLatitude()+", "+chests.get(i).location.getLongitude();
                        chestlist.add(loctext);
                    }
                    radar_image.setChests(chests, location);
                    MyArrayAdapter adapter = new MyArrayAdapter(RadarActivity.this,
                            android.R.layout.simple_list_item_1, chestlist);
                    radar_list.setAdapter(adapter);
                    radar_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> parent, final View view,int position, long id) {
                            Intent intent = new Intent(RadarActivity.this, TrackActivity.class);
                            intent.putExtra("Chest", (Parcelable) chests.get((int)id));
                            startActivity(intent);
                        }

                    });
                    progdialog.dismiss();
                    Toast.makeText(RadarActivity.this, ""+chests.size()+" chest(s) found!", Toast.LENGTH_SHORT).show();
                }else{
                    progdialog.dismiss();
                    Toast.makeText(RadarActivity.this, "No Chest found...", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                progdialog.dismiss();
                Toast.makeText(RadarActivity.this, "JSON invalid.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        protected void onCancelled() {
            progdialog.dismiss();
        }
    }

    private class ResetChestTask extends AsyncTask<String, Integer, JSONObject> {
        ProgressDialog progdialog;

        protected void onPreExecute() {
            progdialog = ProgressDialog.show(RadarActivity.this, "Resetting", "Resetting...");
        }
        protected JSONObject doInBackground(String...params) {
            publishProgress(0);
            JSONObject object = null;
            try {
                StringBuilder builder = new StringBuilder();
                HttpClient client = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://milestone.if.itb.ac.id/pbd/index.php");
                MultipartEntity postentity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                postentity.addPart("group_id", new StringBody(params[0]));
                postentity.addPart("action",new StringBody("reset"));
                httpPost.setEntity(postentity);
                Log.d("Post",httpPost.getEntity().toString());
                HttpResponse response = client.execute(httpPost);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    object = new JSONObject(builder.toString());
                } else {
                    Log.e("Parse JSON", "Failed to download file");
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return object;
        }

        protected void onProgressUpdate(Integer...progress) {
            switch(progress[0]){
                case 0:
                    progdialog.setMessage("Resetting chests status...");
                    break;
                default:
                    break;
            }
        }

        protected void onPostExecute(JSONObject result) {
            try {
                if(result.getString("status").equals("success")){
                    progdialog.dismiss();
                    Toast.makeText(RadarActivity.this, "Chests locations reset!", Toast.LENGTH_SHORT).show();
                }else{
                    progdialog.dismiss();
                    Toast.makeText(RadarActivity.this, "Chests locations not reset...", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                progdialog.dismiss();
                Toast.makeText(RadarActivity.this, "JSON invalid.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        protected void onCancelled() {
            progdialog.dismiss();
        }
    }

    public void onBackPressed(){
        super.onBackPressed();
    }

    public void onLocationChanged(Location location) {
    }

    public void onStatusChanged(String s, int i, Bundle b) {
    }

    public void onProviderDisabled(String s) {
    }

    public void onProviderEnabled(String s) {
    }

    private class MyArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public MyArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        public boolean hasStableIds() {
            return true;
        }

    }
}