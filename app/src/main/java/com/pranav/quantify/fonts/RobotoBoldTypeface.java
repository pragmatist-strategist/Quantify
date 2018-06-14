package com.pranav.quantify.fonts;

import android.content.Context;
import android.graphics.Typeface;

/**
 * Singleton for Roboto Light Typeface
 */
public class RobotoBoldTypeface
{

    private static Typeface instance;

    public static Typeface getInstance(Context context){
        if(instance == null){
            instance = Typeface.createFromAsset(context.getAssets(), "Roboto-Bold.ttf");
        }

        return instance;
    }
}
