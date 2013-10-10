package com.mavilion.treasurehunt;

import android.app.Activity;
import android.app.ProgressDialog;
import android.hardware.Camera;
import android.location.Location;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Mav on 18/09/13.
 */
public class CameraActivity extends Activity {
    private Camera camera;
    private CameraView camera_preview;

    private Chest target;

    private SensorService sensorService;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        target = (Chest)getIntent().getParcelableExtra("Chest");
        if(target == null){
            Location loc = new Location("Dummy Location");
            loc.setLatitude(-6.890706853863619);
            loc.setLongitude(107.60949573940245);
            target = new Chest("1f65628f83d046aa3ca490b5bc66b1ac",loc,"68:7f:74:70:11:1c");
        }

        sensorService = new SensorService(this);

        camera_preview = (CameraView) findViewById(R.id.camera_preview);
        openCamera();

        Button captureButton = (Button) findViewById(R.id.camera_button);
        captureButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                camera.takePicture(null, null, takePicture);
            }
        });
    }

    private void openCamera() {
        try {
            camera = Camera.open();
            camera_preview.setCamera(camera);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onPause(){
        sensorService.removeAllEnabled();
        super.onPause();
    }

    public void onResume(){
        sensorService.setGPSEnabled(true);
        sensorService.setWifiEnabled(true);
        super.onResume();
    }

    Camera.PictureCallback takePicture = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera cam) {
            new AcquireChestTask(data,cam).execute(getString(R.string.group_id));
        }
    };

    private class AcquireChestTask extends AsyncTask<String, Integer, JSONObject> {
        ProgressDialog progdialog;
        byte[] data;
        Camera cam;

        public AcquireChestTask(byte[] dat, Camera cm) {
            data = dat;
            cam = cm;
        }

        protected void onPreExecute() {
            progdialog = ProgressDialog.show(CameraActivity.this, "Acquiring chest", "Acquiring chest...");
        }
        protected JSONObject doInBackground(String...params) {
            JSONObject object = null;
            publishProgress(0);

            File pictureFile = getOutputMediaFile();
            if (pictureFile == null) {
                return null;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();

                ExifInterface ef = new ExifInterface(pictureFile.getCanonicalPath());
                double latitude = sensorService.getLocation().getLatitude();
                double longitude = sensorService.getLocation().getLongitude();
                ef.setAttribute(ExifInterface.TAG_GPS_LATITUDE, dec2DMS(latitude));
                ef.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,dec2DMS(longitude));
                if (latitude > 0)
                    ef.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
                else
                    ef.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
                if (longitude > 0)
                    ef.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
                else
                    ef.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
                ef.saveAttributes();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            publishProgress(1);
            try {
                StringBuilder builder = new StringBuilder();
                HttpClient client = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://milestone.if.itb.ac.id/pbd/index.php");
                MultipartEntity postentity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                postentity.addPart("group_id", new StringBody(params[0]));
                postentity.addPart("chest_id", new StringBody(target.ID));
                postentity.addPart("file", new FileBody(pictureFile));
                postentity.addPart("bssid", new StringBody(target.bssID));
                postentity.addPart("wifi", new StringBody(""+sensorService.getWifiSignal(target.bssID)));
                postentity.addPart("action",new StringBody("acquire"));
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
                    progdialog.setMessage("Capturing picture...");
                    break;
                case 1:
                    progdialog.setMessage("Acquiring chest...");
                    break;
                default:
                    break;
            }
        }

        protected void onPostExecute(JSONObject result) {
            try {
                if(result.getString("status").equals("success")){
                    progdialog.dismiss();
                    Toast.makeText(CameraActivity.this, "Chest acquired!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK, null);
                    finish();
                }else{
                    progdialog.dismiss();
                    Toast.makeText(CameraActivity.this, "Failed : "+result.getString("description"), Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                progdialog.dismiss();
                Toast.makeText(CameraActivity.this, "JSON invalid.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        protected void onCancelled() {
            progdialog.dismiss();
        }
    }

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"TreasureHunt");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        return mediaFile;
    }

    private static String dec2DMS(double coord) {
        coord = coord > 0 ? coord : -coord;
        String sOut = Integer.toString((int)coord) + "/1,";
        coord = (coord % 1) * 60;
        sOut = sOut + Integer.toString((int)coord) + "/1,";
        coord = (coord % 1) * 60000;
        sOut = sOut + Integer.toString((int)coord) + "/1000";
        return sOut;
    }
}