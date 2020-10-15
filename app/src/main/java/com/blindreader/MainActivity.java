package com.blindreader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.style.TtsSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Locale;

import static android.speech.tts.TextToSpeech.*;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener, OnInitListener{

    TextToSpeech tts;
    public static Text text;
    String textContent;
    String[] sentence_text;
    String[] word_text;
    String[] word_text_original;
    GestureDetectorCompat gesture_detector;
    public static int cur_sentence;
    private static final int SWIPE_THRESHOLD = 250;
    private static final int SWIPE_VELOCITY_THRESHOLD = 800;
    public static MyView myview;
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
    double[] word_last_touched_time;
    Vibrator vibrator;
    SoundPool sounds;
    int clickSound;
    double stop_lastTime;
    public static int[] last_highlighted;
    public static boolean no_lift_since_high_light;
    double last_turn_page_time;
    long[] row_last_vibrate_time;
    long[] col_last_vibrate_time;
    int[] sen_start_on_page;
    int[] sen_end_on_page;
    int[] cumulative_words_count;
    public static boolean swiping;

    //public static boolean mTwoFingersFlingConfirmed = false;
    //public static boolean mTwoFingersFling = false;
    public static boolean two_fingers = false;
    public float first_x;
    public float first_y;
    public double first_time;
    //public float timeDown;
    public String TAGG = "Gesture";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myview = new MyView(this, null);
        setContentView(myview);
        gesture_detector = new GestureDetectorCompat(this, this);
        tts = new TextToSpeech(this,this);
        tts.setPitch(0.9f);
        text = new Text();
        textContent = text.rawtext;
        sentence_text = text.sentence_text;
        new TtsSpan.VerbatimBuilder().setVerbatim(sentence_text[0]);
        word_text = text.word_text;
        word_text_original = new Text().word_text;
        text.punc_word_text(word_text);
        words_in_page = text.num_words_in_sentence;
        word_last_touched_time = new double[word_text.length + 1];
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        sen_start_on_page = new int[text.sentence_text.length];
        sen_end_on_page = new int[text.sentence_text.length];
        cumulative_words_count = new int[text.sentence_text.length];

    }

    @Override
    public void onInit(int status) {
        // Calculate ActionBar height and StatusBar height
        myview.setDimension(8, 6);
        int count = 0;
        for (int i = 0; i < text.sentence_text.length; i++) {
            sen_start_on_page[i] = count / (myview.num_row * myview.num_col);
            count += text.num_words_in_sentence[i];
            sen_end_on_page[i] = (count - 1) / (myview.num_row * myview.num_col);
            cumulative_words_count[i] = count;
        }

        row_last_vibrate_time = new long[myview.num_row+1];
        col_last_vibrate_time = new long[myview.num_col+1];

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
        listening = false;
        word_index = -1;
        words_per_page = myview.num_col * myview.num_row;
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        sounds = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();
        clickSound = sounds.load(this, R.raw.click, 1);
        stop_lastTime = 0;
        last_highlighted = new int[2];
        last_highlighted[0] = -1;
        last_highlighted[1] = -1;
        no_lift_since_high_light = false;
        swiping = false;


        if (status == SUCCESS) {
            int result = tts.setLanguage(Locale.CHINESE);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String s) {
                    Log.d("messageis", s);
                    try {
                        cur_sentence = Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        String[] parts = s.split("-");
                        if (parts[0].equals("word")) {
                            word_index = Integer.parseInt(parts[1]);
                            return;
                        }
                    }
                    last_highlighted[0] = -1;
                    last_highlighted[1] = -1;
                    touch_release_cell();
                    Log.d("messageis", "reached here");
                }

                @Override
                public void onDone(String s) {
                    Log.d("ondoneis", "onDone: s is " +s);
                    //speaking stops
                    String[] parts = s.split("-");
                    if (parts[0].equals("single")) {
                        s = parts[1];
                        listening = false;
                    }
                    sentence_finished = Integer.parseInt(s);
                    //cur_sentence = sentence_finished + 1;


                    //vibrator.vibrate(VibrationEffect.createOneShot(50, 10));
                }

                @Override
                public void onError(String s) {
                    //there's an error
                }
            });
        } else {
            Toast.makeText(MainActivity.this, "TextToSpeech failed", Toast.LENGTH_SHORT).show();
        }
        //start reading only at double finger tap (initial listening set to false)
        //startPlay(cur_sentence);
    }

    private void switchActivities() {
        Intent switchActivityIntent = new Intent(this, MenuActivity.class);
        startActivity(switchActivityIntent);
    }

    private void startPlay(int cur_num, int word_index) {
        if (tts.isSpeaking()) {
            tts.stop();
        }
        for (int c = cur_num; c < sentence_text.length; c++) {
            if (c == cur_num) {
                if (word_index == -1) {
                    tts.speak(sentence_text[c], QUEUE_FLUSH, null, String.valueOf(c));
                } else {
                    String[] parts = Arrays.copyOfRange(word_text_original, word_index, cumulative_words_count[cur_num]);
                    //String[] parts = sentence_text[c].split(word);
                    Log.d("string is", String.join("", parts));
                    tts.speak(String.join("", parts), QUEUE_FLUSH, null, String.valueOf(c));
                }
            } else {
                tts.speak(sentence_text[c], QUEUE_ADD, null, String.valueOf(c));
            }
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean local_lis = listening;
        long time = System.currentTimeMillis();
        int action = e.getAction() & MotionEvent.ACTION_MASK;
        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            two_fingers = true;
            Log.d("gesture", "two_fingers changed to true");
        } else if (action == MotionEvent.ACTION_DOWN) {
            Log.d("gesture", "this is single tap");
            two_fingers = false;
            first_x = e.getX();
            first_y = e.getY();
            first_time = System.currentTimeMillis();
        } else if (action == MotionEvent.ACTION_MOVE) {
            double time_diff = System.currentTimeMillis() - first_time;
            float distance = e.getX() - first_x;
            float y_distance = e.getY() - first_y;
            double velocity = distance / time_diff;
            double y_velocity = y_distance / time_diff;
            if (two_fingers && Math.hypot(e.getX() - first_x, e.getY() - first_y) > 10){
                Log.d("gesture", "this is two finger fling");
                //two_fingers = false;
                //return myDetector.onTouchEvent(event);
            } else if (time_diff > 200 && !swiping && !two_fingers && !listening && (System.currentTimeMillis() - stop_lastTime > 300)) {
                Toast.makeText(MainActivity.this, "dragging", Toast.LENGTH_SHORT).show();
                //touch_read_cell(e);
                //avoid touch cell when flinging (swiping)
                //move to touch read is much slower -> velocity
                //time_diff controlled otherwise swipe triggers touch read (when starts to swipe velocity low)
            }
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            Log.d("timediff", String.valueOf(Math.hypot(e.getX() - first_x, e.getY() - first_y)));
            if (two_fingers && (Math.hypot(e.getX() - first_x, e.getY() - first_y) < 100)){
                Toast.makeText(MainActivity.this, "two finger single click", Toast.LENGTH_SHORT).show();
                Log.d("gesture", "TWO FINGERS SINGLE_CLICK !");
                if (listening) {
                    //local_lis = listen_to_touch();
                } else {
                    //local_lis = touch_to_listen();
                }
                two_fingers = false;
                swiping = false;
            }
        } else if (action == MotionEvent.ACTION_UP) {
            //touch_release_cell();
            swiping = false;
        }
        listening = local_lis;
        this.gesture_detector.onTouchEvent(e);
        return super.onTouchEvent(e);
    }

    public void restart_cur_sentence() {
        if (tts.isSpeaking()) {
            tts.stop();
        }
        cur_sentence -= 1;
        next_sentence();
        //tts.speak(sentence_text[cur_sentence], QUEUE_FLUSH, null, String.valueOf(cur_sentence));
    }

    public boolean listen_to_touch() {
        stop_lastTime = System.currentTimeMillis();
        tts.stop();
        cur_page = sen_start_on_page[cur_sentence];
        Log.d(TAG, "onTouchEvent: page is "+sen_start_on_page[cur_sentence]);
        Toast.makeText(MainActivity.this, "stopped at sentence " + (cur_sentence), Toast.LENGTH_SHORT).show();
        //local_lis = false;
        //listening = false;
        if (tts.isSpeaking()) {
            tts.stop();
        }
        sounds.play(clickSound, 1, 1, 0, 0, 1);
        return false;
    }

    public boolean touch_to_listen() {
        sounds.play(clickSound, 1, 1, 0, 0, 1);
        if (last_highlighted[0] == -1) {
            if (cur_sentence == sentence_finished) {
                startPlay(cur_sentence + 1, -1);
            } else {
                startPlay(cur_sentence, -1);
            }
            Toast.makeText(MainActivity.this, "start at sentence " + (cur_sentence), Toast.LENGTH_SHORT).show();
        } else {
            int index = cur_page * myview.num_col * myview.num_row + last_highlighted[1] * myview.num_col + last_highlighted[0];
            int sen = which_sentence(index);
            if (index >= text.word_text.length) {
                sen = text.sentence_text.length - 1;
                startPlay(sen, -1);
                return true;
            }
            startPlay(sen, index);
        }
        return true;
    }

    public static int which_sentence(int i) {
        int count = 0;
        int sen = -1;
        while ((i + 1 >= count - 1) && (sen < text.sentence_text.length - 1)) {
            sen += 1;
            count += text.num_words_in_sentence[sen];
        }
        return sen;
    }

    public void touch_read_cell(MotionEvent e) {
        myview.getHighlightCell(e.getX(), e.getY()-contentViewTop-statusBarHeight,
                tts, word_text, cur_page, word_index, word_last_touched_time);
        myview.invalidate();
        vibrate_on_touch(e.getX(), e.getY()-contentViewTop-statusBarHeight);
    }

    public void touch_release_cell() {
        Log.d(TAG, "onTouchEvent: action up");
        myview.resetHighlight();
        myview.invalidate();
        no_lift_since_high_light = false;
    }

    public void vibrate_on_touch(float x, float y) {
        int col = (int) (x - (float) myview.edge) / myview.cell_width;
        int row = (int) (y - (float) myview.edge) / myview.cell_height;
        long cur_time = System.currentTimeMillis();
        Log.d(TAG, "vibrate_on_touch: cor is "+row+","+col);

        //check if this 1500 gap is ok

        if (Math.abs(col * myview.cell_width + myview.edge-x) <= 20 &&
                y > myview.edge && y < myview.getHeight()-myview.edge) {
            Log.d(TAG, "vibrate_on_touch: supposed to vibrate on col "+col);
            if (cur_time - col_last_vibrate_time[col] >= 800) {
                //Log.d(TAG, "vibrate_on_touch: supposed to vibrate on col "+col);
                vibrator.vibrate(VibrationEffect.createOneShot(30, 20));
                col_last_vibrate_time[col] = cur_time;
            }
        }
        if (Math.abs(row * myview.cell_height + myview.edge-y) <= 20 &&
            x > myview.edge && x < (myview.edge+myview.cell_width*myview.num_col)) {
            if (cur_time - row_last_vibrate_time[row] >= 800) {
                Log.d(TAG, "vibrate_on_touch: supposed to vibrate");
                vibrator.vibrate(VibrationEffect.createOneShot(30, 20));
                row_last_vibrate_time[row] = cur_time;
            }
        }
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
        Toast.makeText(MainActivity.this, "one finger single click", Toast.LENGTH_SHORT).show();
        if (!listening && (System.currentTimeMillis() - stop_lastTime > 300)) {
            //touch_read_cell(motionEvent);
        }
        Log.d("gesture", "onSingleTapUp:comfirmed");
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        Log.d("gesture", "onScroll: detected scroll");

        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    public void turn_next_page() {
        last_highlighted[0] = -1;
        last_highlighted[1] = -1;
        touch_release_cell();
        last_turn_page_time = System.currentTimeMillis();
        int page_count = word_text.length/(myview.num_col * myview.num_col);
        if ((cur_page+1)*myview.num_col*myview.num_row >= word_text.length) {
            tts.speak("已经在尾页",TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            cur_page += 1;
            if (tts.isSpeaking()) {
                tts.stop();
            }
            tts.speak("到第"+ (cur_page+1)+"页",TextToSpeech.QUEUE_FLUSH, null, null);
            Log.d(TAG, "onFling: turned to page "+cur_page+" of total "+ page_count);
        }
        int sen = 0;
        int i = 0;
        while (i < sen_start_on_page.length) {
            if (sen_start_on_page[i] == cur_page) {
                sen = i;
                break;
            }
            i++;
        }
        cur_sentence = sen;
    }

    public void turn_last_page() {
        last_highlighted[0] = -1;
        last_highlighted[1] = -1;
        touch_release_cell();
        last_turn_page_time = System.currentTimeMillis();
        if (cur_page > 0) {
            cur_page -= 1;
            if (tts.isSpeaking()) {
                tts.stop();
            }
            tts.speak("到第"+ (cur_page+1)+"页",TextToSpeech.QUEUE_FLUSH, null, null);
            Log.d(TAG, "onFling: turned to page "+cur_page);
        } else {
            tts.speak("已经在首页",TextToSpeech.QUEUE_FLUSH, null, null);
        }
        int sen = 0;
        int i = 0;
        while (i < sen_start_on_page.length) {
            if (sen_start_on_page[i] == cur_page) {
                sen = i;
                break;
            }
            i++;
        }
        cur_sentence = sen;
    }

    public void listen_turn_next_page() {
        last_turn_page_time = System.currentTimeMillis();
        if (tts.isSpeaking()) {
            tts.stop();
        }
        int page = sen_start_on_page[cur_sentence];
        page += 1;
        if ((page+1)*myview.num_col*myview.num_row >= word_text.length) {
            page -= 1;
            //tts.speak("已经在尾页",TextToSpeech.QUEUE_FLUSH, null, null);
        }
        int sen = 0;
        int i = 0;
        while (i < sen_start_on_page.length) {
            if (sen_start_on_page[i] == page) {
                sen = i;
                break;
            }
            i++;
        }
        startPlay(sen, -1);
    }

    public void listen_turn_last_page() {
        last_turn_page_time = System.currentTimeMillis();
        if (tts.isSpeaking()) {
            tts.stop();
        }
        int page = sen_start_on_page[cur_sentence];
        page -= 1;
        if (page < 0) {
            page += 1;
        }
        int sen = 0;
        int i = 0;
        while (i < sen_start_on_page.length) {
            if (sen_start_on_page[i] == page) {
                sen = i;
                break;
            }
            i++;
        }
        startPlay(sen, -1);
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        Log.d("switchactivity", motionEvent.getX()+","+motionEvent1.getX());
        Log.d("gesture", "onFling: just fling," + two_fingers);
        //code modified from https://stackoverflow.com/questions/4139288/android-how-to-handle-right-to-left-swipe-gestures
        boolean result = false;
        float diffY = motionEvent1.getY() - motionEvent.getY();
        float diffX = motionEvent1.getX() - motionEvent.getX();
        Log.d(TAG, "onFling: listening status is"+listening);
        if (Math.abs(diffX) > Math.abs(diffY)) {
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(v) > SWIPE_VELOCITY_THRESHOLD) {
                Log.d("swiping", "wrong place");
                swiping = true;
                if (diffX > 0) {
                    if (motionEvent.getX() < 15) {
                        Toast.makeText(MainActivity.this, "swipe right from left edge", Toast.LENGTH_SHORT).show();
                        //switchActivities();
                    } else {
                        onSwipeRight();
                    }
                } else {
                    onSwipeLeft();
                }
                result = true;
            }
        } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(v1) > SWIPE_VELOCITY_THRESHOLD) {
            swiping = true;
            if (diffY > 0) {
                onSwipeDown();
            } else {
                onSwipeUp();
            }
            result = true;
        }
        two_fingers = false;
        return result;
    }

    private void last_sentence() {
        tts.stop();
        if (cur_sentence > 0) {
            cur_sentence -= 1;
        } else {
            tts.speak("已经在首句", QUEUE_FLUSH, null, null);
            while (tts.isSpeaking()) {
            }
        }
        //startPlay(cur_sentence);
        tts.speak(sentence_text[cur_sentence], QUEUE_FLUSH, null, "single-"+String.valueOf(cur_sentence));
        if (!listening) {
            listening = true;
        }

        Toast.makeText(MainActivity.this, "last sentence", Toast.LENGTH_SHORT).show();
    }

    private void onSwipeLeft() {
        if (two_fingers) {
            Toast.makeText(MainActivity.this, "two finger swipe left", Toast.LENGTH_SHORT).show();
            if (listening) {
                //listen_turn_last_page();
            } else {
                //turn_last_page();
            }
        } else {
            Toast.makeText(MainActivity.this, "one finger swipe left", Toast.LENGTH_SHORT).show();
            //last_sentence();
        }
    }

    private void next_sentence() {
        //摸读后的next sentence怎么算
        tts.stop();
        if (cur_sentence < (text.sentence_text.length-1)) {
            cur_sentence += 1;
        } else {
            tts.speak("已经在尾句", QUEUE_FLUSH, null, null);
            while (tts.isSpeaking()) {
            }
        }
        //startPlay(cur_sentence);
        tts.speak(sentence_text[cur_sentence], QUEUE_FLUSH, null, "single-"+String.valueOf(cur_sentence));
        if (!listening) {
            listening = true;
        }
        Toast.makeText(MainActivity.this, "next sentence", Toast.LENGTH_SHORT).show();
    }

    private void onSwipeRight() {
        if (two_fingers) {
            Toast.makeText(MainActivity.this, "two finger swipe right", Toast.LENGTH_SHORT).show();
            if (listening) {
                //listen_turn_next_page();
            } else {
                //turn_next_page();
            }
        } else {
            Toast.makeText(MainActivity.this, "one finger swipe right", Toast.LENGTH_SHORT).show();
            //next_sentence();
        }
    }

    private void tell_page_loca() {
        if (tts.isSpeaking()) {
            tts.stop();
        }
        int page_count = (word_text.length-1)/(myview.num_col * myview.num_col);
        if (listening) {
            cur_page = sen_start_on_page[cur_sentence];
        }
        tts.speak("当前第"+(cur_page+1)+"页，一共"+(page_count+1)+"页", QUEUE_FLUSH, null, null);
        listening = false;
    }


    private void onSwipeUp() {
        //switchActivities();
        if (two_fingers) {
            Toast.makeText(MainActivity.this, "two finger swipe up", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "one finger swipe up", Toast.LENGTH_SHORT).show();
            //tell_page_loca();
        }
    }

    private void to_page_start() {
        if (!listening) return;
        if (tts.isSpeaking()) {
            tts.stop();
        }
        int page = sen_start_on_page[cur_sentence];
        int sen = 0;
        int i = 0;
        while (i < sen_start_on_page.length) {
            if (sen_start_on_page[i] == page) {
                sen = i;
                break;
            }
            i++;
        }
        startPlay(sen, -1);
    }

    private void onSwipeDown() {
        if (two_fingers) {
            Toast.makeText(MainActivity.this, "two finger swipe down", Toast.LENGTH_SHORT).show();
            //to_page_start();
        } else {
            Toast.makeText(MainActivity.this, "one finger swipe down", Toast.LENGTH_SHORT).show();
            //restart_cur_sentence();
        }
    }

    @Override
    public void onDestroy() {
        sounds.release();
        sounds = null;
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}