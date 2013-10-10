package com.mavilion.treasurehunt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * Created by Mav on 08/09/13.
 */
public class SplashActivity extends Activity {

    private static int SPLASH_TIME_OUT = 3000;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {

            public void run() {
                Intent i = new Intent(SplashActivity.this, RadarActivity.class);
                startActivity(i);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}