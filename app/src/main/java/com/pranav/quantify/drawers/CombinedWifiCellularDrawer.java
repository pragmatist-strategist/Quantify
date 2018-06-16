
package com.pranav.quantify.drawers;
import android.content.Context;
import android.graphics.Canvas;

import com.pranav.quantify.drawers.Drawer;
import com.pranav.quantify.drawers.WifiDrawer;

/**
 * Combine the two Drawers into one which toggles between them
 * depending on if you are on wifi or cellular data.
 */
public class CombinedWifiCellularDrawer extends Drawer {

    protected CellularDrawer mCellularDrawer;
    protected WifiDrawer mWifiDrawer;
    protected Drawer mActiveDrawer;


    public CombinedWifiCellularDrawer(Context context) {
        super(context);
        mCellularDrawer = new CellularDrawer(context);
        mWifiDrawer = new WifiDrawer(context);
        selectActiveDrawer();
    }


    @Override
    public void tap(int x, int y) {
        mActiveDrawer.tap(x, y);
    }

    //OnDestroy
    @Override
    public void destroy() {
        super.destroy();
        mCellularDrawer.destroy();
        mWifiDrawer.destroy();
    }

    //onStart
    @Override
    public void start() {
        mWifiDrawer.start();
        mCellularDrawer.start();
    }

    protected void selectActiveDrawer() {
        mActiveDrawer = mWifiDrawer.isCritical() ? mCellularDrawer : mWifiDrawer;
    }

    @Override
    public boolean shouldDraw() {
        Drawer activeDrawer = mActiveDrawer;
        selectActiveDrawer();
        //if the drawer switched then definitely draw
        if (activeDrawer != mActiveDrawer) {
            //no matter what call should Draw, cause there might be setup code in it
            mActiveDrawer.shouldDraw();
            return true;
        }

        return mActiveDrawer.shouldDraw();
    }


    @Override
    public void draw(Canvas c) {
        mActiveDrawer.draw(c);
    }

}
