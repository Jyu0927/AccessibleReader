package com.blindreader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.speech.tts.TextToSpeech;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

public class Menu extends View implements GestureDetector.OnGestureListener {
    private static final String TAG = "order";
    public int num_row, num_col;
    private Paint blackLine = new Paint();
    private Paint fill = new Paint();
    public int edge = 100;
    int[] highlighted = {-1,-1};
    GestureDetectorCompat gesture_detector_menu;

    public Menu(Context context) {
        super(context);
        blackLine.setStyle(Paint.Style.STROKE);
        fill.setStyle(Paint.Style.FILL_AND_STROKE);
        gesture_detector_menu = new GestureDetectorCompat(context, this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        super.onDraw(canvas);
    }


    @Override
    public boolean onDown(MotionEvent motionEvent) {
        Log.d("menu", "touched down");
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }
}




















