
package com.pranav.quantify.drawers;

import android.app.Service;
import android.content.Context;
import android.provider.Settings;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.pranav.quantify.R;
import java.util.List;

/**
 * this is basically for the Network connections.
 * Tells me about the connection which the user is having currently and also handle's the network connectivity if it is changed.
 */

public class CellularDrawer extends TriangleFillDrawer {
    private final String TAG = this.getClass().getSimpleName();
    private boolean firstRead = true;

    //check for airplane mode->on state h ya off state h....
    private static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private TelephonyManager tManager;

    public CellularDrawer(final Context context) {
        super(
                context,
                context.getResources().getColor(R.color.cellular_background),
                context.getResources().getColor(R.color.cellular_triangle_background),
                context.getResources().getColor(R.color.cellular_triangle_foreground),
                context.getResources().getColor(R.color.cellular_triangle_critical)
        );

        this.label1 = "Cellular";

        tManager = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
        setLabel2();
//Listens to the changes in the signal strength-> kab zyada h and kab kam h
        tManager.listen(new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);

                int level = 0;
                String tech = "";
//airplane mode means no connection is there.
                if (isAirplaneModeOn(context)) {
                    percent = 0f;
                    connected = false;
                    label1 = "No connection";
                    label2 = "Airplane Mode Enabled";
                    return;
                }

                List<CellInfo> infos = null;

                try {
                    infos = tManager.getAllCellInfo();
                } catch (SecurityException e) {
                    Log.e(TAG, e.toString());
                }

                if (infos == null) {
                    connected = false;
                    return;
                }
//Basically it checks if the given signal is LTE(Long term evolution) or not.
                for (final CellInfo info : infos) {
                    if (info instanceof CellInfoLte) {
                        final CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();

                        if (level < lte.getLevel()) {
                            level = lte.getLevel();
                            tech = "lte";
                        }

                    }
                }

                connected = true;
                label1 = "Cellular";
                percent = (float) (level / 4.0);

                if (firstRead) {
                    firstRead = false;
                    _percent = (float) (percent - 0.001);
                }
            }

            //Listens to the change in the state of network connection-> boundary conditions mein->jab WiFi ON/OFF hua h etc.
            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                super.onServiceStateChanged(serviceState);
                setLabel2();
                Log.d(TAG, "STATE " + String.valueOf(serviceState) + "   " + serviceState.getState());
            }
        }, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_SERVICE_STATE);

    }

    //IF the network connection is unknown to me...
    private void setLabel2() {
        String type = getNetworkType();
        label2 = tManager.getNetworkOperatorName();
        if (!type.equals("Unknown")) {
            label2 += " " + type;
        }
    }

    // since only 2 types of networks are available and majority of them are 4G->thanks to Jio and in all other cases are 3G.
    private String getNetworkType() {
        switch (tManager.getNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            default:
                return "3G";
        }
    }

}
