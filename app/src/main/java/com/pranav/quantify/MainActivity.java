package com.pranav.quantify;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.pranav.quantify.fonts.RobotoBoldTypeface;
import com.pranav.quantify.fonts.RobotoLightTypeface;


/**
 * The Main app activity, describes the wallpaper and directs user towards notification settings
 */
public class MainActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    protected SharedPreferences mSettings;

    protected ToggleButton mWifiEnabled;
    protected ToggleButton mBatteryEnabled;
    protected ToggleButton mNotificationsEnabled;
    protected Button mSetWallpaperBtn;


    /**
     * the click listener for all drawers buttons
     */
    protected View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //if none of the buttons are on, this one must stay on
            if (!anyChecked()) {
                ((ToggleButton) v).setChecked(true);
            }
        }

    };

    /**
     * are any of the ToggleButtons currently checked? yes, by default i have checked the battery one.
     */
    protected boolean anyChecked() {
        ToggleButton[] btns = {mWifiEnabled, mBatteryEnabled, mNotificationsEnabled};

        for (ToggleButton btn : btns) {
            if (btn.isChecked()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //grab button references
        mWifiEnabled = (ToggleButton) findViewById(R.id.wifiEnableButton);
        mBatteryEnabled = (ToggleButton) findViewById(R.id.batteryEnableButton);
        mNotificationsEnabled = (ToggleButton) findViewById(R.id.notificationsEnableButton);
        mSetWallpaperBtn = (Button) findViewById(R.id.choseWallpaperButton);

        Typeface robotoLight = RobotoLightTypeface.getInstance(this);
        Typeface robotoBold = RobotoBoldTypeface.getInstance(this);
        mSetWallpaperBtn.setTypeface(robotoBold);

        //grab shared preferences
        mSettings = getSharedPreferences(WallpaperPreferences.PREFERENCES, MODE_PRIVATE);

        ((TextView) findViewById(R.id.descriptionTextView)).setTypeface(robotoLight);

        //set listeners
        mWifiEnabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //use the basic one as well
                mOnClickListener.onClick(v);
                checkLocationPermission();
            }
        });

        mBatteryEnabled.setOnClickListener(mOnClickListener);
        mNotificationsEnabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //use the basic one as well
                mOnClickListener.onClick(v);

                if (mNotificationsEnabled.isChecked() && !NotificationService.permissionsGranted) {
                    showNotificationPermissionAlert();
                }
            }
        });

        mSetWallpaperBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                startActivity(intent);
            }
        });


    }

    //onResume
    @Override
    public void onResume() {
        super.onResume();
        updateGUI();
        if (!isNotificationServiceRunning()) {
            mNotificationsEnabled.setChecked(false);
        }

        this.checkLocationPermission();

        //in the case where notifications was the only one selected
        //and its permissions were revoked, turn back on WiFi
        if (!anyChecked()) {
            mBatteryEnabled.setChecked(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.updateSettings();
    }

    private void updateSettings() {
        //update the shared preferences
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean("wifi", mWifiEnabled.isChecked());
        editor.putBoolean("battery", mBatteryEnabled.isChecked());
        editor.putBoolean("notifications", mNotificationsEnabled.isChecked());
        editor.apply();
    }

    private void checkLocationPermission() {
        if (mWifiEnabled.isChecked()) {
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
            }
        }
    }

    //check if the required permissions are granted by the user or not:
    //For wifi->check location access by the user.
    //similarly check if the user ha granted permission to allow the app to read notifications or not.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions[0].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                mWifiEnabled.setChecked(false);
                this.updateSettings();
            }
        }
    }

    //Alert dialog ki form me permission request karo app ke liye from the user.
    private void showNotificationPermissionAlert() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setMessage(getString(R.string.notification_permission));

        alertBuilder
                //deliberately i have not set it as a cancellable alertDialog-> permissions zaroori hain app ko kaam krne ke liye !
                .setCancelable(false)
                .setPositiveButton("YES",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                moveToNotificationListenerSettings();
                            }
                        })
                .setNegativeButton("NO",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mNotificationsEnabled.setChecked(false);
                            }
                        });


        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();
    }

    //Check if the given permissions service provided by the user are working correctly or not.
    private boolean isNotificationServiceRunning() {

        ContentResolver resolver = getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(resolver, "enabled_notification_listeners");

        String packageName = getPackageName();

        return enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName);
    }

    //this is step when the user checks for the components he wants to see and finally the wallpaper preferences are changed by the app after taking permission from the user.
    private void updateGUI() {
        mWifiEnabled.setChecked(mSettings.getBoolean(WallpaperPreferences.WIFI_CELLULAR, false));
        mBatteryEnabled.setChecked(mSettings.getBoolean(WallpaperPreferences.BATTERY, false));
        mNotificationsEnabled.setChecked(mSettings.getBoolean(WallpaperPreferences.NOTIFICATIONS, false));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //This is the options available in the menu tab.
    //Basically selection of menu part.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.open_settings:
                moveToNotificationListenerSettings();
                break;

            case R.id.about:
                moveToAbout();
                break;
            case R.id.licenses:
            default:
                moveToLicenses();
        }

        return true;
    }

    /**
     * go to the OS-level notification listener settings
     */
    private void moveToNotificationListenerSettings() {
        Intent intent = null;
        //This is an ever true statement. It will always remain true in whatever case. Basically used if someone deliberately wants to have a look into this app.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        }
    }

    /**
     * go to the about section
     */
    private void moveToAbout() {
        Intent intent = new Intent(this, LocalWebActivity.class);
        intent.putExtra(LocalWebActivity.EXTRA_HTML_URI, "html/about.html");
        startActivity(intent);
    }

    /**
     * go to the licenses section
     */
    private void moveToLicenses() {
        //go to Licenses section here
        Intent intent = new Intent(this, LocalWebActivity.class);
        intent.putExtra(LocalWebActivity.EXTRA_HTML_URI, "html/licenses.html");
        startActivity(intent);
    }

}
