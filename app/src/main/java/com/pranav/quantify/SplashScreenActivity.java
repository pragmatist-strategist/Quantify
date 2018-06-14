package com.pranav.quantify;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.pranav.quantify.MainActivity;
import com.pranav.quantify.R;

/**
 * This was the first time I introduced such activity in the app. This was something
 * which i saw in many apps and thought of showcasing it for the first time and
 * introduced in this app.
 * there is nothing special about this except that it gives a delay of 3 seconds before the original app is loaded .
 * <p>
 * The first activity that shows the logo and then moves you to the main app.
 */
public class SplashScreenActivity extends Activity {

    @Override
    public void onCreate(Bundle savedBundleInstance) {
        super.onCreate(savedBundleInstance);
        setContentView(R.layout.activity_splashscreen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                moveToMainActivity();
            }
        }, 3000);

    }


    protected void moveToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        //remove this activity from the stack after 3 seconds have passed.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

}
