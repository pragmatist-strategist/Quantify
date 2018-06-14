package com.pranav.quantify;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import com.pranav.quantify.drawers.BatteryDrawer;
import com.pranav.quantify.drawers.CombinedWifiCellularDrawer;
import com.pranav.quantify.drawers.Drawer;
import com.pranav.quantify.drawers.NotificationsDrawer;

import java.util.ArrayList;
//this part of documentation is to be continued @ps.
/**
 * The Live Wallpaper-> basically changes the colour of the wallpaper whenever there is a change recorded.
 */

/**
 * A wallpaper service is responsible for showing a live wallpaper behind
 * applications that would like to sit on top of it.  This service object
 * itself does very little -- its only purpose is to generate instances of
 * {@link Engine} as needed.  Implementing a wallpaper thus
 * involves subclassing from this, subclassing an Engine implementation,
 * and implementing {@link #onCreateEngine()} to return a new instance of
 * your engine.
 */
public class MeterWallpaper extends WallpaperService {

    private final String TAG = this.getClass().getSimpleName();

    private Drawer mDrawer;

    // Variable containing the index of the drawer last shown
    private int mDrawerIndex = -1;
    private long mHideTimestamp = -1;

    @Override
    public Engine onCreateEngine() {
        WallpaperEngine engine = new WallpaperEngine(this);

        return engine;
    }


    /**
     * The wallpaper engine that will handle the rendering
     */
    private class WallpaperEngine extends WallpaperService.Engine {

        public Context mContext;

        private boolean mVisible = false;
        private final Handler mHandler = new Handler();

        public WallpaperEngine(Context context) {
            this.mContext = context;
        }

        /**
         * Handle tap commands
         */
        public Bundle onCommand(String action, int x, int y, int z, Bundle extras, boolean resultRequested) {
            //taps work on Nexus devices but not all, for example Samsung
            if (action.equals("android.wallpaper.tap")) {
                if (mDrawer != null) {
                    mDrawer.tap(x, y);
                }
            }
            return super.onCommand(action, x, y, z, extras, resultRequested);
        }

        /**
         * Draw runloop
         */
        private final Runnable mUpdateDisplay = new Runnable() {
            @Override
            public void run() {
                draw();
            }
        };

        /**
         * Draw function doing the context locking and rendering
         */
        private void draw() {
            if (mDrawer == null) return;

            // Ask the drawer if wants to draw in this frame
            if (mDrawer.shouldDraw()) {
                SurfaceHolder holder = getSurfaceHolder();
                Canvas c = null;
                try {
                    // Lock the drawing canvas
                    c = holder.lockCanvas();
                    if (c != null) {
                        // Let the drawer render to the canvas
                        mDrawer.draw(c);
                    }
                } finally {
                    if (c != null) holder.unlockCanvasAndPost(c);
                }
            }
            mHandler.removeCallbacks(mUpdateDisplay);
            if (mVisible) {
                // Wait one frame, and redraw
                mHandler.postDelayed(mUpdateDisplay, 33);
            }
        }


        /**
         * Toggle visibility of the wallpaper
         * In this function we create new drawers every time the wallpaper
         * is visible again, cycling through the available ones
         *
         * @param visible whether the wallpaper is currently visible
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {

                SharedPreferences prefs = getSharedPreferences(WallpaperPreferences.PREFERENCES, MODE_PRIVATE);

                ArrayList<Class> drawerClasses = new ArrayList<Class>();

                if (prefs.getBoolean(WallpaperPreferences.WIFI_CELLULAR, true)) {
                    drawerClasses.add(CombinedWifiCellularDrawer.class);
                }
                if (prefs.getBoolean(WallpaperPreferences.BATTERY, true)) {
                    drawerClasses.add(BatteryDrawer.class);
                }
                if (prefs.getBoolean(WallpaperPreferences.NOTIFICATIONS, true)) {
                    drawerClasses.add(NotificationsDrawer.class);
                }

                if (System.currentTimeMillis() - mHideTimestamp > 500 || mHideTimestamp == -1) {
                    mDrawerIndex++;
                    if (mDrawerIndex >= drawerClasses.size()) {
                        mDrawerIndex = 0;
                    }
                }
                Class cls = drawerClasses.get(mDrawerIndex);
                if (cls == NotificationsDrawer.class) {
                    mDrawer = new NotificationsDrawer(mContext);
                } else if (cls == BatteryDrawer.class) {
                    mDrawer = new BatteryDrawer(mContext);
                } else {
                    mDrawer = new CombinedWifiCellularDrawer(mContext);
                }

                mDrawer.start();
                // Start the drawing loop
                draw();

            } else {
                mHideTimestamp = System.currentTimeMillis();
                if (mDrawer != null) {
                    mDrawer.destroy();
                    mDrawer = null;
                }
                mHandler.removeCallbacks(mUpdateDisplay);
            }
        }

        //whenever there is a change in the gesture of the phone.
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            draw();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mUpdateDisplay);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mVisible = false;
            mHandler.removeCallbacks(mUpdateDisplay);
        }
    }

}


