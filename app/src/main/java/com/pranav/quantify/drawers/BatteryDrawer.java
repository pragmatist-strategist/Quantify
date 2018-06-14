
package com.pranav.quantify.drawers;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.BatteryManager;

import com.pranav.quantify.drawers.Drawer;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

//Created by pranav


public class BatteryDrawer extends Drawer {
    private final String TAG = this.getClass().getSimpleName();

    private float batteryPct;//This is used for reading battery status.
    private float _batteryPct = -1;//Negation of reading battery status

    private double colorTransitionToCharged = 0;
    private double _colorTransitionToCharged = 0;

    private double colorTransitionToCritical = 0;
    private double _colorTransitionToCritical = 0;

    private final Vector2D zero = new Vector2D(0, 0);

    private final double circleSize = 0.7 * 0.5;

    // Colors
    private final int color_battery_background;
    private final int color_background_decharge;
    private final int color_foreground_decharge;

    private final int color_background_charging;
    private final int color_foreground_charging;

    private final int color_background_critical;
    private final int color_foreground_critical;

    private Paint paint = new Paint();

    // Movement of the cursor.
    private Vector2D pos, _pos;
    private Vector2D vel;

    public BatteryDrawer(Context context) {
        super(context);

        // Register a receiver for battery state changes
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(batteryLevelReceiver, ifilter);


        // Read current battery charging/discharging status...
        batteryPct = getBatteryPct(batteryStatus);

        if (getBatteryIsCharging(batteryStatus)) {
            _colorTransitionToCharged = colorTransitionToCharged = 1.0;
        } else {
            _colorTransitionToCharged = colorTransitionToCharged = 0.0;
            if (batteryPct <= 0.15) _colorTransitionToCritical = colorTransitionToCritical = 1.0;
        }


        // Load the colors for different transitions for different changes in the battery levels.

        color_battery_background = context.getResources().getColor(com.pranav.quantify.R.color.battery_background);
        color_background_decharge = context.getResources().getColor(com.pranav.quantify.R.color.battery_circle_discharging_background);
        color_foreground_decharge = context.getResources().getColor(com.pranav.quantify.R.color.battery_circle_discharging);

        color_foreground_critical = context.getResources().getColor(com.pranav.quantify.R.color.battery_circle_critical);
        color_background_critical = context.getResources().getColor(com.pranav.quantify.R.color.battery_circle_critical_background);

        color_background_charging = context.getResources().getColor(com.pranav.quantify.R.color.battery_circle_charging_background);
        color_foreground_charging = context.getResources().getColor(com.pranav.quantify.R.color.battery_circle_charging);

        this.textColor = context.getResources().getColor(com.pranav.quantify.R.color.battery_text);
    }

    /**
     * The battery level change receiver->listens to battery level changes and changes colour correspondingly .
     */
    BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            batteryPct = getBatteryPct(intent);

            // current state kya h->charging/discharging ?
            if (getBatteryIsCharging(intent)) {
                colorTransitionToCharged = 1.0;
                colorTransitionToCritical = 0.0;
            } else {
                colorTransitionToCharged = 0.0;

                if (batteryPct <= 0.20) { //when battery falls below 20%-> critical stage pahuch gyi hai->color should be changed to critical.
                    colorTransitionToCritical = 1.0;
                } else {// 20% se zyada h? Do nothing as of now.
                    colorTransitionToCritical = 0.0;
                }
            }
        }
    };

    /**
     * Function that determines if the background needs to be redrawn or not->Matlab notificationListener ne kisi service mein change record kiya h so change the colour accordingly.
     */
    //All the further part of code is written with the help of stack overflow as a reference and with the help of Apache math library.


    //
    public boolean shouldDraw() {
        boolean redraw = super.shouldDraw();

        if (vel == null) vel = new Vector2D(0, 0);
        if (pos == null) pos = new Vector2D(0, 0);
        if (_pos == null) _pos = new Vector2D(0, 0);

        Vector2D a = new Vector2D(mOrientation[2] * 0.01, -mOrientation[1] * 0.01);
        a = a.add(pos.scalarMultiply(-0.01));

        vel = vel.scalarMultiply(0.9);
        vel = vel.add(a);
        pos = pos.add(vel);


        float dist = (float) pos.distance(zero);
        float maxDist = (float) (circleSize - circleSize * Math.sqrt(batteryPct));
        if (dist > maxDist) {
            Vector2D n = pos.normalize().scalarMultiply(-1);
            Vector2D reflection = vel.subtract(n.scalarMultiply(2 * vel.dotProduct(n)));

            pos = n.scalarMultiply(-maxDist);

            vel = reflection.scalarMultiply(0.5);
        }

        if (_colorTransitionToCharged != colorTransitionToCharged || _colorTransitionToCritical != _colorTransitionToCritical) {
            redraw = true;
        }
        if (_batteryPct != batteryPct) {
            redraw = true;
        }
        if (_textAlpha != textAlpha) {
            redraw = true;
        }
        if (_pos.distance(pos) > 0.001) {
            redraw = true;
        }

        if (redraw) {
            // Circle color
            //Transition part of the circle colour .
            _colorTransitionToCharged = animateValue(_colorTransitionToCharged, colorTransitionToCharged, 0.03);
            _colorTransitionToCritical = animateValue(_colorTransitionToCritical, colorTransitionToCritical, 0.03);
            _pos = pos;

            _batteryPct = batteryPct;
            return true;
        }

        return false;
    }

    /**
     * Draw the circle to the screen of phone...
     */
    public void draw(Canvas c) {
        super.draw(c);


        /**
         * Helper for setFlags(), setting or clearing the ANTI_ALIAS_FLAG bit
         * AntiAliasing smooths out the edges of what is being drawn, but is has
         * no impact on the interior of the shape. See setDither() and
         * setFilterBitmap() to affect how colors are treated.
         *
         * @param aa true to set the antialias bit in the flags, false to clear it
         */
        paint.setAntiAlias(true);

        // Background of the screen -> ek rectangle draw karo and fill it with a bold colours .
        paint.setColor(color_battery_background);
        c.drawRect(0, 0, c.getWidth(), c.getHeight(), paint);
        //To draw the circle in the exact center.
        int x = c.getWidth() / 2;
        int y = c.getHeight() / 2 - (int) (30f * pixelDensity);

        int canvasSize = c.getWidth();
        if (c.getWidth() > c.getHeight()) {
            canvasSize = c.getHeight();
        }
        float _circleSize = (float) (canvasSize * circleSize);
        int textPos = (int) (y + circleSize * canvasSize);

        // Outer circle for battery.
        //the first variable has 3 parameters-> colour @charging,colour @discharging and finally the transition which determines how smooth the color is changeD @ps.
        int bgCircleColor = interpolateColor(color_background_decharge, color_background_charging, (float) lerp(_colorTransitionToCharged));
        //In the next step, change the colour whenever there is a change in the battery level from >20% -> <20% @ps.
        bgCircleColor = interpolateColor(bgCircleColor, color_background_critical, (float) lerp(_colorTransitionToCritical));
        //lastly, you have to paint the circle.
        paint.setColor(bgCircleColor);
        //and finally draw the circle.
        c.drawCircle(x, y, _circleSize, paint);

        // Inner circle for battery .
        //the first variable has 3 parameters-> colour @charging,colour @discharging and finally the transition which determines how smooth the color is changeD @ps.
        int fgCircleColor = interpolateColor(color_foreground_decharge, color_foreground_charging, (float) lerp(_colorTransitionToCharged));
        //In the next step, change the colour whenever there is a change in the battery level from >20% -> <20% @ps.
        fgCircleColor = interpolateColor(fgCircleColor, color_foreground_critical, (float) lerp(_colorTransitionToCritical));
        //lastly, you have to paint the circle.
        paint.setColor(fgCircleColor);
        //and finally draw the circle.
        c.drawCircle(
                (float) (x + canvasSize * pos.getX()),
                (float) (y + canvasSize * pos.getY()),
                (float) (_circleSize * Math.sqrt(batteryPct)), paint);

        // Convert the %age battery into text.
        String label1 = "Battery " + Integer.toString(Math.round(batteryPct * 100)) + "%";
        //Display the parsed text on screen.
        drawText(label1, "", x, textPos, c);
    }


    /**
     * Parse the battery percentage from the battery change intent
     */
    public float getBatteryPct(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return level / (float) scale;
    }

    /**
     * Set the battery charging status from the battery change intent
     */
    public boolean getBatteryIsCharging(Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
    }
}
