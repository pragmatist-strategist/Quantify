package com.pranav.quantify.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ToggleButton;

import com.pranav.quantify.R;


/**
 * Render the ToggleButton as my drawable checkbox graphics
 * This is basically for checkbox in the app.
 */
public class CustomToggleButton extends ToggleButton
{

    public CustomToggleButton(Context context) {
        super(context);
    }


    public CustomToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();
        setText("");
        updateBackground();
    }

    private void updateBackground(){
        //is the current option from the list selected? ->handling the cases simultaneously.
        int drawable = isChecked() ? R.drawable.menu_checkbox_selected : R.drawable.menu_checkbox_unselected;
        setBackground(getContext().getResources().getDrawable(drawable));
    }


    @Override
    public void setChecked(boolean checked)
    {
        super.setChecked(checked);
        setText("");
        updateBackground();
    }

}
