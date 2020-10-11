package com.blindreader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.*;

import static android.speech.tts.TextToSpeech.*;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener, OnInitListener{

    TextToSpeech tts;
    Text text;
    String textContent;
    String[] sentence_text;
    String[] word_text;
    GestureDetectorCompat gesture_detector;
    int cur_sentence;
    private static final int SWIPE_THRESHOLD = 400;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    MyView myview;
    int contentViewTop;
    int statusBarHeight;
    int cur_page;
    int[] words_in_page;
    int word_count_when_stop;
    int sentence_finished;
    int words_per_page;
    private static final String TAG = "order";
    boolean listening;
    int word_index;
    //ViewGroup.MarginLayoutParams margin_params;

    public MyGestureListener myGestureListener;
    public GestureDetector myDetector;
    public static boolean mTwoFingersFlingConfirmed = false;
    public static boolean mTwoFingersFling = false;
    public float clickDownX;
    public float clickDownY;
    public float timeDown;
    public String TAGG = "Gesture";
    public float currentTime;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        myview = new MyView(this, null);
        setContentView(myview);
        gesture_detector = new GestureDetectorCompat(this, this);
        tts = new TextToSpeech(this,this);
        text = new Text();
        textContent = text.rawtext;
        sentence_text = text.sentence_text;
        word_text = text.word_text;
        words_in_page = text.num_words_in_sentence;
        //myview.setDimension(8, 6);
        /*words_in_page = new int[sentence_text.length];
        for (int a = 0; a < sentence_text.length; a++) {
            words_in_page[a] = sentence_text[a].length();
        }*/
        //margin_params = (ViewGroup.MarginLayoutParams) R.layout.activity_main.

        myGestureListener = new MyGestureListener();
        myDetector = new GestureDetector(this, myGestureListener);
    }

    @Override
    public void onInit(int status) {
        // Calculate ActionBar height and StatusBar height
        myview.setDimension(8, 6);
        Rect rectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        statusBarHeight = rectangle.top;
        contentViewTop =
                window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        //int titleBarHeight= contentViewTop - statusBarHeight;
        TypedValue tv = new TypedValue();

        cur_sentence = 0;
        cur_page = 0;
        word_count_when_stop = 0;
        sentence_finished = -1;
        listening = true;
        word_index = -1;
        words_per_page = myview.num_col * myview.num_row;


        if (status == SUCCESS) {
            int result = tts.setLanguage(Locale.CHINESE);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String s) {
                    String[] parts = s.split("-");
                    if (parts[0].equals("word")) {
                        word_index = Integer.parseInt(parts[1]);
                    }
                }

                @Override
                public void onDone(String s) {
                    Log.d(TAG, "onDone: s is " +s);
                    //speaking stops
                    sentence_finished = Integer.parseInt(s);
                    cur_sentence = sentence_finished + 1;
                }

                @Override
                public void onError(String s) {
                    //there's an error
                }
            });
        } else {
            Toast.makeText(MainActivity.this, "TextToSpeech failed", Toast.LENGTH_SHORT).show();
        }
        startPlay(cur_sentence);
    }

    private void startPlay(int cur_num) {
        for (int c = cur_num; c < sentence_text.length; c++) {
            if (c == cur_num) {
                tts.speak(sentence_text[c], QUEUE_FLUSH, null, String.valueOf(c));
            } else {
                tts.speak(sentence_text[c], QUEUE_ADD, null, String.valueOf(c));
            }
        }
    }


//    @Override
//    public boolean onTouchEvent(MotionEvent e) {
//        boolean local_lis = listening;
//        long time = System.currentTimeMillis();
//        int action = e.getAction() & MotionEvent.ACTION_MASK;
//        if (action == MotionEvent.ACTION_POINTER_DOWN) {
//            //Toast.makeText(MainActivity.this, "双击", Toast.LENGTH_SHORT).show();
//            Log.e(TAG, "onTouchEvent: listening is "+listening);
//            Log.e(TAG, "onTouchEvent: speaking is "+tts.isSpeaking());
//            if (listening) {
//                tts.stop();
//                word_count_when_stop = 0;
//                for (int i = 0; i < cur_sentence;i++) {
//                    word_count_when_stop += words_in_page[i];
//                }
//                Log.d(TAG, "onTouchEvent: word count is "+word_count_when_stop);
//                if (word_count_when_stop == 0) {
//                    cur_page = 0;
//                } else {
//                    Log.d(TAG, "onTouchEvent: reached here "+words_per_page);
//                    cur_page = (word_count_when_stop+words_per_page)/words_per_page;
//                    cur_page -= 1;
//                    Log.d(TAG, "onTouchEvent: cur_page is "+cur_page);
//                    //cur_page = roundUp(word_count_when_stop+1, words_per_page);
//                }
//                Log.d(TAG, "onTouchEvent: page is "+cur_page);
//                Toast.makeText(MainActivity.this, "stopped at sentence " + cur_sentence, Toast.LENGTH_SHORT).show();
//                local_lis = false;
//                if (tts.isSpeaking()) {
//                    tts.stop();
//                }
//                tts.speak("暂停", QUEUE_FLUSH, null, null);
//            } else {
//                startPlay(cur_sentence);
//                Toast.makeText(MainActivity.this, "start at sentence " + cur_sentence, Toast.LENGTH_SHORT).show();
//                local_lis = true;
//            }
//        } else if (action == MotionEvent.ACTION_DOWN | action == MotionEvent.ACTION_MOVE) {
//            //Log.d(TAG, "onTouchEvent: single time is"+System.currentTimeMillis());
//            if (!listening) {
//                Log.d(TAG, "onTouchEvent: 摸读了");
//                myview.getHighlightCell(e.getX(), e.getY()-contentViewTop-statusBarHeight, tts, word_text, cur_page, word_index);
//                //Toast.makeText(MainActivity.this, "location at" + (e.getY()-contentViewTop-statusBarHeight), Toast.LENGTH_SHORT).show();
//                myview.invalidate();
//            } else {
//                //Toast.makeText(MainActivity.this, "听读中，勿扰", Toast.LENGTH_SHORT).show();
//            }
//        } else if (action == MotionEvent.ACTION_UP) {
//            Log.d(TAG, "onTouchEvent: action up");
//            myview.resetHighlight();
//            myview.invalidate();
//        }
//        listening = local_lis;
//        this.gesture_detector.onTouchEvent(e);
//        return super.onTouchEvent(e);
//    }
    public void twoFingersReset(){
        mTwoFingersFlingConfirmed = false;
        mTwoFingersFling = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        switch(event.getAction() & MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_DOWN:
                mTwoFingersFling = false;
                mTwoFingersFlingConfirmed = false;
                clickDownX = event.getRawX();
                clickDownY = event.getRawY();
                timeDown = System.currentTimeMillis();
                Toast.makeText(MainActivity.this, "down", Toast.LENGTH_SHORT).show();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mTwoFingersFling = true;
                break;
            case MotionEvent.ACTION_MOVE:

                if(mTwoFingersFling && Math.hypot(event.getRawX() - clickDownX, event.getRawY() - clickDownY) > 10){
                    mTwoFingersFlingConfirmed = true;
                    return myDetector.onTouchEvent(event);
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:

                if (mTwoFingersFling && Math.hypot(event.getRawX() - clickDownX, event.getRawY() - clickDownY) < 10){
                    Log.d(TAGG, "TWO FINGERS SINGLE_CLICK !");
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return myDetector.onTouchEvent(event);

    }

    public static int roundUp(int num, int divisor) {
        return (num + divisor - 1) / divisor;
    }



        @Override
        public boolean onDown(MotionEvent motionEvent) {
            Log.d(TAG, "onDown: single tap seperate");
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            return true;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            Log.d(TAG, "onSingleTapUp:removed");
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {

            return true;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {
            Log.d(TAG, "changanshoushi");
            Toast.makeText(MainActivity.this, "last sentence", Toast.LENGTH_SHORT).show();

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            Log.d(TAG, "onFling: detected");
            //code modified from https://stackoverflow.com/questions/4139288/android-how-to-handle-right-to-left-swipe-gestures
            boolean result = false;
            float diffY = motionEvent1.getY() - motionEvent.getY();
            float diffX = motionEvent1.getX() - motionEvent.getX();
            Log.d(TAG, "onFling: listening status is" + listening);
            if (!listening) {
                float x1 = motionEvent.getX();
                float x2 = motionEvent1.getX();
                float y1 = motionEvent.getY() - contentViewTop - statusBarHeight;
                float y2 = motionEvent1.getY() - contentViewTop - statusBarHeight;
                if (myview.checkTurnNextPage(x1, x2, y1, y2)) {
                    int page_count = word_text.length / (myview.num_col * myview.num_col);
                    if ((cur_page + 1) * myview.num_col * myview.num_row >= word_text.length) {
                        tts.speak("已经在尾页", TextToSpeech.QUEUE_FLUSH, null, null);
                    } else {
                        cur_page += 1;
                        if (tts.isSpeaking()) {
                            tts.stop();
                        }
                        tts.speak("下一页", TextToSpeech.QUEUE_FLUSH, null, null);
                        Log.d(TAG, "onFling: turned to page " + cur_page + " of total " + page_count);
                    }
                    return true;
                }
                if (myview.checkTurnLastPage(x1, x2, y1, y2)) {
                    if (cur_page > 0) {
                        cur_page -= 1;
                        if (tts.isSpeaking()) {
                            tts.stop();
                        }
                        tts.speak("上一页", TextToSpeech.QUEUE_FLUSH, null, null);
                        Log.d(TAG, "onFling: turned to page " + cur_page);
                    } else {
                        tts.speak("已经在首页", TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                    return true;
                }
            }
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(v) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight();
                    } else {
                        onSwipeLeft();
                    }
                    result = true;
                }
            } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(v1) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    onSwipeDown();
                } else {
                    onSwipeUp();
                }
                result = true;
            }
            return result;
        }

        private void onSwipeLeft() {
            if (listening) {
                tts.stop();
                if (cur_sentence > 0) {
                    cur_sentence -= 1;
                }
                startPlay(cur_sentence);
            } else {
                if (tts.isSpeaking()) {
                    tts.stop();
                }
                if (cur_sentence > 0) {
                    cur_sentence -= 1;
                }
            }
            Toast.makeText(MainActivity.this, "last sentence", Toast.LENGTH_SHORT).show();
        }

        private void onSwipeRight() {
            Log.d(TAG, "onSwipeRight: listening status" + listening);
            Log.d(TAG, "onSwipeRight: speaking status" + tts.isSpeaking());
            if (listening) {
                tts.stop();
                if (cur_sentence < (text.sentence_text.length - 1)) {
                    cur_sentence += 1;
                }
                startPlay(cur_sentence);
            } else {
                if (tts.isSpeaking()) {
                    tts.stop();
                }
                Log.d(TAG, "onSwipeRight: not speaking");
                if (cur_sentence < (text.sentence_text.length - 1)) {
                    cur_sentence += 1;
                }
            }
            Toast.makeText(MainActivity.this, "next sentence", Toast.LENGTH_SHORT).show();
        }

        private void onSwipeUp() {
            Toast.makeText(MainActivity.this, "swiped up", Toast.LENGTH_SHORT).show();
        }

        private void onSwipeDown() {
            Toast.makeText(MainActivity.this, "swiped down", Toast.LENGTH_SHORT).show();
        }


}