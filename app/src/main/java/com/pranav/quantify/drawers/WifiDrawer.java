package com.pranav.quantify.drawers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.pranav.quantify.R;


public class WifiDrawer extends TriangleFillDrawer {
    private final String TAG = this.getClass().getSimpleName();

    private boolean firstRead = true;


    public WifiDrawer(Context context) {
        super(
                context,
                context.getResources().getColor(R.color.wifi_background),
                context.getResources().getColor(R.color.wifi_triangle_background),
                context.getResources().getColor(R.color.wifi_triangle_foreground),
                context.getResources().getColor(R.color.wifi_triangle_critical)
        );

        this.label1 = "WIFI";

        // Register for Wifi state change notifications
        IntentFilter ifilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        ifilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        context.registerReceiver(wifiReceiver, ifilter);
    }

    /**
     * Receive WIFI state changes
     */
    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Read wifi signal strength
            if (intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION)) {

                percent = getWifiStrength(intent);
                //draw this below the top label
                label2 = getWifiNetworkName(context);

                if (firstRead) {
                    firstRead = false;
                    _percent = (float) (percent - 0.001);
                }
            }

            // Read wifi status
            else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                connected = getWifiConnected(intent);
            }
        }
    };

    /**
     * Convert wifi strength from intent into an integer lying b/w 0 & 1.
     */
    public float getWifiStrength(Intent intent) {
        float level = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -1);
        level = WifiManager.calculateSignalLevel((int) level, 100);
        level /= 100.0;
        return level;
    }

    /**
     * Check the current wifi connection from intent-> connected aor disconnected.
     */
    public boolean getWifiConnected(Intent intent) {
        NetworkInfo info = (NetworkInfo) intent.getExtras().get(WifiManager.EXTRA_NETWORK_INFO);

        if (info == null) return false;
        return info.getState().equals(NetworkInfo.State.CONNECTED);
    }

    /**
     * Parse wifi for network name to which it is connected and display it on screen.
     */
    public String getWifiNetworkName(Context context) {
        WifiManager mgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = mgr.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        //for some reason SSID comes wrapped in double-quotes
        if (ssid == null) {
            ssid = "";
        }
        return ssid.replace("\"", "");
    }

}
