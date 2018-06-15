
package com.pranav.quantify.drawers;


import android.animation.ArgbEvaluator;
import android.app.Service;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.service.wallpaper.WallpaperService;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.util.Calendar;

import com.pranav.quantify.fonts.RobotoLightTypeface;


/**
 * This is used for setting layout in other apps.
 * Class inherited by the other drawers
 */
public class Drawer implements SensorEventListener {
    private Display mDisplay;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    //The sensor manager is called and 2 sensors are used by app->accelerometer & magnetometer. Further these are used in 3 different orientations i.e. an arraylist is called for the same.
//Along x, y and z axis respectively.
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;

    private float[] mR = new float[9];
    protected float[] mOrientation = new float[3];


    private long prevTapTime = 0;
    //value of colour lies frm 0-255. so i will further convert it to a scale of 0-1.
    protected double textAlpha = 255;
    protected double _textAlpha = 255;
    protected int textFadeCount = 0;

    protected int textColor = Color.WHITE;

    protected float pixelDensity;
    //ArgbEvaluator-> my function to decide the colour of the app according to change in state.
    private final ArgbEvaluator ev = new ArgbEvaluator();
    private Paint textPaint;

    public Context context;

    public Drawer(Context context) {
        this.context = context;
        this.pixelDensity = context.getResources().getDisplayMetrics().density;
    }

    public void start() {
//This is the code for my sensor manager part.
        //This  plays a pivotal role in the
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mLastAccelerometerSet = false;
        mLastMagnetometerSet = false;
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    //OnDestroy->unregister the sensorListener Service
    public void destroy() {
        if (mSensorManager == null) {
            return;
        }
        mSensorManager.unregisterListener(this);
    }

    public void draw(Canvas c) {


    }

    public boolean shouldDraw() {
        boolean draw = false;

        return draw;
    }


    public void tap(int x, int y) {
        long thistime = Calendar.getInstance().getTimeInMillis();
        if (prevTapTime < thistime) {
            if (thistime - prevTapTime <= 1000) {
                doubleTap(x, y);
            }
        }
        prevTapTime = thistime;
    }

    protected void doubleTap(int x, int y) {
        textFadeCount = 50;
        textAlpha = (float) 1.0;
    }

    /**
     * Draw the text under the graphics
     */
    protected void drawText(String text1, String text2, int x, int y, Canvas c) {
        if (textPaint == null) {
            Paint p = new Paint();
            p.setTypeface(RobotoLightTypeface.getInstance(context));
            p.setColor(textColor);
            p.setTextSize(14 * pixelDensity);
            this.textPaint = p;
        }

        float w = textPaint.measureText(text1, 0, text1.length());
        int offset = (int) w / 2;
        c.drawText(text1, x - offset, y + (18f * pixelDensity), textPaint);

        w = textPaint.measureText(text2, 0, text2.length());
        offset = (int) w / 2;
        c.drawText(text2, x - offset, y + (36f * pixelDensity), textPaint);

    }

    /**
     * Lerp->. Lerp (in android), a quasi-acronym for linear interpolation(extension) in computing and mathematics.
     * Produce a nice lerp between 0...1
     */
    protected double lerp(double val) {
        return val * val * val * (val * (6f * val - 15f) + 10f);
    }

    /**
     * Do a simple animation towards a value with a given speed.
     * Basically this incorporates the animation taught in the last class.
     */
    protected double animateValue(double curVal, double goalVal, double speed) {
        double ret = curVal;
        if (ret < goalVal) {
            ret += speed;
        } else if (ret > goalVal) {
            ret -= speed;
        }

        if (Math.abs(ret - goalVal) < speed) {
            ret = goalVal;
        }
        return ret;
    }


    /**
     * Interpolate between two HEX colors
     *
     * @param c1 First color
     * @param c2 Second color
     * @param v  interpolation value (c1 * (1-v) + c2 * v)
     * @return Interpolated color
     */
    protected int interpolateColor(int c1, int c2, float v) {
        return (int) ev.evaluate(v, c1, c2);
    }

    //This part of code is basically used for sensors state and their changes.
    //So basically onSensorChanged is called and it records the values which are further parsed and used my me in showing the real-time values of various things.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);

            try {
                mDisplay = ((WindowManager) ((WallpaperService) context).getApplication().getSystemService(Service.WINDOW_SERVICE))
                        .getDefaultDisplay();
            } catch (Exception ignored) {
            }


            int rotation = Surface.ROTATION_0;
            if (mDisplay != null) {
                rotation = mDisplay.getRotation();
            }
//As taught in class -> using a camera:
            //I have applied that logic in this section which can define my current axis and hence i can get my current orientation is in which axis which has
            // further helped me to draw the 2D axis again.....
//90->2rd quad, 270->4th quad, 180->3rd quad and 0->1st quad obviously.
            float[] mRremap = mR.clone();
            if (rotation == Surface.ROTATION_90) {
                SensorManager.remapCoordinateSystem(mR, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, mRremap);
            }
            if (rotation == Surface.ROTATION_270) {
                SensorManager.remapCoordinateSystem(mR, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, mRremap);
            }
            if (rotation == Surface.ROTATION_180) {
                SensorManager.remapCoordinateSystem(mR, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, mRremap);
            }
//finally get my final orientation
            SensorManager.getOrientation(mRremap, mOrientation);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
