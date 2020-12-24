package com.blindreader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static android.speech.tts.TextToSpeech.*;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener, OnInitListener{

    static TextToSpeech tts;
    public static Text text;
    String textContent;
    static String[] sentence_text;
    static String[] word_text;
    String[] word_text_original;
    GestureDetectorCompat gesture_detector;
    public static int cur_sentence;
    public static final int SWIPE_THRESHOLD = 250;
    public static final int SWIPE_VELOCITY_THRESHOLD = 800;
    public static int[][] book_num_page;
    public static MyView myview;
    int contentViewTop;
    int statusBarHeight;
    static int cur_page;
    static int[] words_in_page;
    int word_count_when_stop;
    int sentence_finished;
    int words_per_page;
    private static final String TAG = "order";
    static boolean listening;
    int word_index;
    double[] word_last_touched_time;
    static Vibrator vibrator;
    SoundPool sounds;
    int clickSound;
    int page_flip_sound;
    double stop_lastTime;
    public static int[] last_highlighted;
    public static boolean no_lift_since_high_light;
    double last_turn_page_time;
    long[] row_last_vibrate_time;
    long[] col_last_vibrate_time;
    static int[] sen_start_on_page;
    int[] sen_end_on_page;
    int[] cumulative_words_count;
    public static boolean swiping;

    //public static boolean mTwoFingersFlingConfirmed = false;
    //public static boolean mTwoFingersFling = false;
    public static boolean two_fingers = false;
    public float first_x;
    public float first_y;
    public double first_time;
    public double last_time;
    public float last_x, last_y;
    //public float timeDown;
    public String TAGG = "Gesture";
    public static int page_count;
    public static boolean progress_bar_pressed;
    public static boolean progress_cancel;
    public static float progress_bar_basecor;
    public static float[] progress_bar_height;
    public static int progress_cur_page;
    static long progress_speak_time;
    public static int chapter;
    public static int section;
    int total_page_in_book;
    public String file_name;

    //for counting things unrelated to the APP itself
    public static Map <String, Integer> dictionary;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myview = new MyView(this, null);
        setContentView(myview);
        gesture_detector = new GestureDetectorCompat(this, this);
        tts = new TextToSpeech(this,this);
        tts.setPitch(0.9f);
        text = new Text();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        cur_sentence = 0;
        cur_page = 0;
        word_count_when_stop = 0;
        sentence_finished = -1;
        word_index = -1;
        last_highlighted = new int[2];
        last_highlighted[0] = -1;
        last_highlighted[1] = -1;
    }

    @Override
    public void onInit(int status) {
        //writeFile(myview);
        Instant instant = Instant.now();
        myview.invalidate();
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            chapter = 0;
            section = 0;
        } else {
            chapter = extras.getInt("CHAPTER_ID");
            section = extras.getInt("SECTION_ID");
        }
        file_name = instant +"_"+chapter+"_"+section+".txt";
        Log.d("filenamechanged",file_name);
        textContent = text.rawtext[chapter][section];
        if (textContent.equals("")) {
            sentence_text = new String[] {};
        } else {
            sentence_text = textContent.split("[。？！；\n]");
        }
        Log.d("checkcrash", "sentence text length is "+textContent.length());
        //new TtsSpan.VerbatimBuilder().setVerbatim(sentence_text[0]);
        word_text = new Text().word_text[chapter][section];
        for (int i = 0; i < word_text.length; i++) {
            Log.d("checkwordtext", word_text[i]);
        }
        word_text_original = new Text().word_text[chapter][section];
        text.punc_word_text(word_text);
        words_in_page = text.num_words_in_sentence[chapter][section];
        word_last_touched_time = new double[word_text.length + 1];
        sen_start_on_page = new int[sentence_text.length];
        sen_end_on_page = new int[sentence_text.length];
        cumulative_words_count = new int[sentence_text.length];
        progress_bar_height = new float[2];
        // Calculate ActionBar height and StatusBar height
        myview.setDimension(10, 6);
        Log.d("checkcrash", "reach here");
        int count = 0;
        for (int i = 0; i < sentence_text.length; i++) {
            sen_start_on_page[i] = count / (myview.num_row * myview.num_col);
            count += words_in_page[i];
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

        /*cur_sentence = 0;
        cur_page = 0;
        word_count_when_stop = 0;
        sentence_finished = -1;
        word_index = -1;*/
        listening = false;
        words_per_page = myview.num_col * myview.num_row;
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        sounds = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();
        clickSound = sounds.load(this, R.raw.click, 1);
        page_flip_sound = sounds.load(this, R.raw.page_flip,1);
        stop_lastTime = 0;
        /*last_highlighted = new int[2];
        last_highlighted[0] = -1;
        last_highlighted[1] = -1;*/
        no_lift_since_high_light = false;
        swiping = false;
        page_count = ((word_text.length-1)/ (myview.num_row * myview.num_col)) +1;
        progress_bar_pressed = false;
        progress_cancel = false;
        progress_bar_basecor = 0;
        progress_bar_height[0] = 0;
        progress_bar_height[1] = 0;
        progress_cur_page = 0;
        progress_speak_time = 0;
        last_time = 0;
        book_num_page = text.num_page();
        total_page_in_book = 0;
        last_x = 0;
        last_y = 0;
        Log.d("crashcheck", String.valueOf(book_num_page));
        for (int i = 0; i < text.rawtext.length; i++) {
            for (int j = 0; j < text.num_words_in_sentence[i].length; j++) {
                Log.d("crashcheck", i+","+j);
                total_page_in_book += book_num_page[i][j];
            }
        }
        Log.d("crashcheck", "reached here1");

        dictionary = new HashMap<String, Integer>();
        dictionary.put("stop", 0);
        dictionary.put("start", 0);
        dictionary.put("last_sen_l", 0);
        dictionary.put("last_sen_t", 0);
        dictionary.put("next_sen_l", 0);
        dictionary.put("next_sen_t", 0);
        dictionary.put("last_page_l", 0);
        dictionary.put("last_page_t", 0);
        dictionary.put("next_page_l", 0);
        dictionary.put("next_page_t", 0);
        dictionary.put("back_sen_l", 0);
        dictionary.put("back_sen_t", 0);
        dictionary.put("back_page_l", 0);
        dictionary.put("progress_bar", 0);
        dictionary.put("cancel_bar", 0);
        dictionary.put("chap_status_l", 0);
        dictionary.put("chap_status_t", 0);
        dictionary.put("book_status_l", 0);
        dictionary.put("book_status_t", 0);


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
                        if (parts[0].equals("single")) {
                            cur_page = sen_start_on_page[cur_sentence];
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
        generateNoteOnSD(file_name, "results");
        Set<String> keys = dictionary.keySet();
        for (String key: keys) {
            generateNoteOnSD(file_name, key +":"+dictionary.get(key));
        }
        Intent switchActivityIntent = new Intent(this, MenuActivity.class);
        startActivity(switchActivityIntent);
    }

    private void startPlay(int cur_num, int word_index) {
        if (tts.isSpeaking()) {
            tts.stop();
        }
        if (cur_num > sentence_text.length - 1) {
            return;
        }
        for (int c = cur_num; c < sentence_text.length; c++) {
            if (c == cur_num) {
                if (word_index == -1) {
                    tts.speak(sentence_text[c], QUEUE_FLUSH, null, String.valueOf(c));
                    tts.speak("，", QUEUE_ADD, null, String.valueOf(c));
                } else {
                    String[] parts = Arrays.copyOfRange(word_text_original, word_index, cumulative_words_count[cur_num]);
                    //String[] parts = sentence_text[c].split(word);
                    Log.d("string is", String.join("", parts));
                    tts.speak(String.join("", parts), QUEUE_FLUSH, null, String.valueOf(c));
                }
            } else {
                tts.speak(sentence_text[c], QUEUE_ADD, null, String.valueOf(c));
                tts.speak("，", QUEUE_ADD, null, String.valueOf(c));
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
            progress_cancel = false;
            first_x = e.getX();
            first_y = e.getY();
            first_time = System.currentTimeMillis();
            last_x = first_x;
            last_y = first_y;
        } else if (action == MotionEvent.ACTION_MOVE) {
            double time_diff = System.currentTimeMillis() - first_time;
            double last_time_diff = System.currentTimeMillis() - last_time;

            //record last x and get the velocity
            
            float distance = e.getX() - last_x;
            //float y_distance = e.getY() - first_y;
            double velocity = distance / last_time_diff;
            //double y_velocity = y_distance / time_diff;
            double y_distance = e.getY() - last_y;
            //Log.d("debugging", "time difference is "+time_diff);
            //Log.d("debugging", "swiping is " + swiping);
            //Log.d("debugging", "swiping is " + two_fingers);
            //Log.d("debugging", "last time diff is " + (System.currentTimeMillis() - stop_lastTime));
            if (two_fingers && Math.hypot(e.getX() - first_x, e.getY() - first_y) > 10){
                Log.d("gesture", "this is two finger fling");
                //two_fingers = false;
                //return myDetector.onTouchEvent(event);
            } else if (time_diff > 200 && !swiping && !two_fingers && (System.currentTimeMillis() - stop_lastTime > 300)) {
                //Toast.makeText(MainActivity.this, "dragging", Toast.LENGTH_SHORT).show();
                Log.d("debugging", "truth is  "+(listening));
                if (!progress_bar_pressed && !listening) {
                    vibrate_on_touch(e.getX(), e.getY()-contentViewTop-statusBarHeight);
                }
                if (!progress_bar_pressed && !listening && (e.getX() < (myview.edge+myview.cell_width*myview.num_col))) {
                    // add velocity > -0.7 && y_distance>=0 if want to control nextline don't read the middle part
                    Log.d("checkvelocity", "velocity is " + y_distance);
                    touch_read_cell(e);
                } else if (first_x >= (10+myview.edge+myview.cell_width*myview.num_col)) {
                    progress_bar(e.getY());
                }
                //avoid touch cell when flinging (swiping)
                //move to touch read is much slower -> velocity
                //time_diff controlled otherwise swipe triggers touch read (when starts to swipe velocity low)
            }
            last_time = System.currentTimeMillis();
            last_x = e.getX();
            last_y = e.getY();
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            if (two_fingers && (Math.hypot(e.getX() - first_x, e.getY() - first_y) < 100)){
                //Toast.makeText(MainActivity.this, "two finger single click", Toast.LENGTH_SHORT).show();
                Log.d("gesture", "TWO FINGERS SINGLE_CLICK !");
                Log.d("progressbar", "listening status is "+ listening);
                if (listening) {
                    local_lis = listen_to_touch();
                } else {
                    local_lis = touch_to_listen();
                }
                swiping = false;
                two_fingers = false;
            }
        } else if (action == MotionEvent.ACTION_UP) {
            last_time = 0;
            last_x = 0;
            last_y = 0;
            swiping = false;
            if (last_highlighted[0] != -1) {
                int word_index = cur_page * myview.num_row * myview.num_col + last_highlighted[1] * myview.num_col + last_highlighted[0];
                if (word_index < word_text.length) {
                    word_last_touched_time[word_index] = 0;
                }
            }
            if (progress_bar_pressed) {
                progress_bar_pressed = false;
                //local_lis = true;
                if (e.getX() > (myview.edge+myview.cell_width*myview.num_col)) {
                    generateNoteOnSD(file_name, "progress_bar " + System.currentTimeMillis());
                    dictionary.put("progress_bar", dictionary.get("progress_bar")+1);
                    cur_page = progress_cur_page;
                    if (cur_page > page_count - 1) {
                        cur_page = page_count - 1;
                    }
                } else {
                    progress_cancel = true;
                    generateNoteOnSD(file_name, "cancel_bar " + System.currentTimeMillis());
                    dictionary.put("cancel_bar", dictionary.get("cancel_bar")+1);
                    tts.speak("，回到第"+(cur_page+1)+"页", QUEUE_FLUSH, null, null);
                }
                    //retract the action on progress bar, go back to current page
                    progress_to_page(cur_page);
                    myview.invalidate();
                Log.d("progressbar","to page "+ cur_page);
            }
        }
        listening = local_lis;
        this.gesture_detector.onTouchEvent(e);
        myview.invalidate();
        return super.onTouchEvent(e);
    }

    public void progress_to_page(int page) {
        int sen = 0;
        int i = 0;
        while (i < sen_start_on_page.length) {
            if (sen_start_on_page[i] == page) {
                sen = i;
                break;
            }
            i++;
        }
        cur_sentence = sen;
        if (listening) {
            while (tts.isSpeaking()) {

            }
            startPlay(sen, -1);
        }
    }

    public void restart_cur_sentence() {
        if (tts.isSpeaking()) {
            tts.stop();
        }
        cur_sentence -= 1;
        if (listening) {
            generateNoteOnSD(file_name, "back_sen_l " + System.currentTimeMillis());
            dictionary.put("back_sen_l", dictionary.get("back_sen_l")+1);
        } else {
            generateNoteOnSD(file_name, "back_sen_t " + System.currentTimeMillis());
            dictionary.put("back_sen_t", dictionary.get("back_sen_t")+1);
        }
        next_sentence();
        //tts.speak(sentence_text[cur_sentence], QUEUE_FLUSH, null, String.valueOf(cur_sentence));
    }

    public boolean listen_to_touch() {
        stop_lastTime = System.currentTimeMillis();
        tts.stop();
        cur_page = sen_start_on_page[cur_sentence];
        Log.d(TAG, "onTouchEvent: page is "+sen_start_on_page[cur_sentence]);
        //Toast.makeText(MainActivity.this, "stopped at sentence " + (cur_sentence), Toast.LENGTH_SHORT).show();
        //local_lis = false;
        //listening = false;
        if (tts.isSpeaking()) {
            tts.stop();
        }
        sounds.play(clickSound, 1, 1, 0, 0, 1);
        generateNoteOnSD(file_name, "stop " + System.currentTimeMillis());
        dictionary.put("stop", dictionary.get("stop")+1);
        return false;
    }

    public boolean touch_to_listen() {
        sounds.play(clickSound, 0.5f, 0.5f, 0, 0, 1);
        //check if the volume is right
        if (last_highlighted[0] == -1) {
            if (cur_sentence == sentence_finished) {
                startPlay(cur_sentence + 1, -1);
            } else {
                startPlay(cur_sentence, -1);
            }
            //Toast.makeText(MainActivity.this, "start at sentence " + (cur_sentence), Toast.LENGTH_SHORT).show();
        } else {
            int index = cur_page * myview.num_col * myview.num_row + last_highlighted[1] * myview.num_col + last_highlighted[0];
            int sen = which_sentence(index);
            if (index >= word_text.length) {
                sen = sentence_text.length - 1;
                startPlay(sen, -1);
                return true;
            }
            startPlay(sen, index);
        }
        generateNoteOnSD(file_name, "start " + System.currentTimeMillis());
        dictionary.put("start", dictionary.get("start")+1);
        return true;
    }

    public static int which_sentence(int i) {
        int count = 0;
        int sen = -1;
        while ((i + 1 >= count - 1) && (sen < sentence_text.length - 1)) {
            sen += 1;
            count += words_in_page[sen];
        }
        return sen;
    }

    public void touch_read_cell(MotionEvent e) {
        //vibrate_on_touch(e.getX(), e.getY()-contentViewTop-statusBarHeight);
        myview.getHighlightCell(e.getX(), e.getY()-contentViewTop-statusBarHeight,
                tts, word_text, cur_page, word_index, word_last_touched_time);
        myview.invalidate();
        //vibrate_on_touch(e.getX(), e.getY()-contentViewTop-statusBarHeight);
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
        Log.d("vibrateontouch", "vibrate_on_touch: cor is "+row+","+col);

        //check if this 1500 gap is ok

        if (Math.abs(col * myview.cell_width + myview.edge-x) <= 20 &&
                y > myview.edge && y < myview.getHeight()-myview.edge) {
            Log.d(TAG, "vibrate_on_touch: supposed to vibrate on col "+col);
            if (cur_time - col_last_vibrate_time[col] >= 800) {
                //Log.d(TAG, "vibrate_on_touch: supposed to vibrate on col "+col);
                vibrator.vibrate(VibrationEffect.createOneShot(30, 60));
                col_last_vibrate_time[col] = cur_time;
            }
        }
        if (Math.abs(row * myview.cell_height + myview.edge-y) <= 20 &&
            x > myview.edge && x < (myview.edge+myview.cell_width*myview.num_col)) {
            if (cur_time - row_last_vibrate_time[row] >= 800) {
                Log.d(TAG, "vibrate_on_touch: supposed to vibrate");
                vibrator.vibrate(VibrationEffect.createOneShot(30, 60));
                row_last_vibrate_time[row] = cur_time;
            }
        }
    }

    public static void progress_bar(float y) {
        //Log.d("progressbar","cor is "+y);
        int edge = 10;
        if (!progress_bar_pressed) {
            last_highlighted[0] = -1;
            last_highlighted[1] = -1;
            tts.stop();
            progress_bar_pressed = true;
            progress_bar_basecor = y;
            if (listening) {
                cur_page = sen_start_on_page[cur_sentence];
            }
            progress_bar_height[0] = (y - edge) / (float) cur_page;
            progress_bar_height[1] = (myview.getHeight()-(y+edge)) / (float) (page_count - 1 - cur_page);
            progress_cur_page = cur_page;
            vibrator.vibrate(VibrationEffect.createOneShot(20, 60));
            tts.speak("百分之"+(int) ((cur_page+1) * 100/page_count) + ",第"+(cur_page+1)+"页",TextToSpeech.QUEUE_FLUSH, null, null);
            Log.d("progressbar", "height is "+ progress_bar_height[0]+ progress_bar_height[1]);
            progress_speak_time = System.currentTimeMillis();
        } else {
            int new_page;
            if (y < progress_bar_basecor - edge) {
                new_page = (int) (y / progress_bar_height[0]);
            } else if (y > progress_bar_basecor + edge){
                new_page = (int) ((y - (progress_bar_basecor + edge)) / progress_bar_height[1]) + cur_page + 1;
                //Log.d("progressbar", "to page" + (int) ((y - progress_bar_basecor) / progress_bar_height[1]));
            } else {
                new_page = cur_page;
            }
            if (new_page > page_count -1) {
                new_page = page_count-1;
            }
            if (new_page != progress_cur_page | (System.currentTimeMillis() - progress_speak_time > 2000)) {
                tts.stop();
                progress_speak_time = System.currentTimeMillis();
                if (new_page != progress_cur_page) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, 60));
                }
                Log.d("testingtesting",String.valueOf((int)(new_page+1) * 100/page_count));
                tts.speak((int) ((new_page+1) * 100/page_count) + ",第"+(new_page+1)+"页",TextToSpeech.QUEUE_FLUSH, null, null);
            }
            progress_cur_page = new_page;
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
        //Toast.makeText(MainActivity.this, "one finger single click", Toast.LENGTH_SHORT).show();
        if (!listening && (System.currentTimeMillis() - stop_lastTime > 300)) {
            vibrate_on_touch(motionEvent.getX(), motionEvent.getY()-contentViewTop-statusBarHeight);
            touch_read_cell(motionEvent);
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
            tts.speak("第"+ (cur_page+1)+"页",TextToSpeech.QUEUE_FLUSH, null, null);
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
        generateNoteOnSD(file_name, "next_page_t " + System.currentTimeMillis());
        dictionary.put("next_page_t", dictionary.get("next_page_t")+1);
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
            tts.speak("第"+ (cur_page+1)+"页",TextToSpeech.QUEUE_FLUSH, null, null);
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
        generateNoteOnSD(file_name, "last_page_t " + System.currentTimeMillis());
        dictionary.put("last_page_t", dictionary.get("last_page_t")+1);
    }

    public void listen_turn_next_page() {
        sounds.play(page_flip_sound, 1, 1, 0, 0, 1.5f);
        last_turn_page_time = System.currentTimeMillis();
        if (tts.isSpeaking()) {
            tts.stop();
        }
        int page = sen_start_on_page[cur_sentence];
        page += 1;
        if (page >= page_count) {
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
        generateNoteOnSD(file_name, "next_page_l " + System.currentTimeMillis());
        dictionary.put("next_page_l", dictionary.get("next_page_l")+1);
        startPlay(sen, -1);
    }

    public void listen_turn_last_page() {
        sounds.play(page_flip_sound, 1, 1, 0, 0, 1.5f);
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
        generateNoteOnSD(file_name, "last_page_l " + System.currentTimeMillis());
        dictionary.put("last_page_l", dictionary.get("last_page_l")+1);
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
                        //Toast.makeText(MainActivity.this, "swipe right from left edge", Toast.LENGTH_SHORT).show();
                        tts.stop();
                        tts.speak("打开目录", QUEUE_FLUSH, null, null);
                        listening = false;
                        switchActivities();
                    } else {
                        Log.d("timediffis", String.valueOf(System.currentTimeMillis()-first_time));
                        onSwipeRight();
                    }
                } else {
                    if (!progress_cancel) {
                        onSwipeLeft();
                    }
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
        swiping = false;
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
            generateNoteOnSD(file_name, "last_sen_t " + System.currentTimeMillis());
            dictionary.put("last_sen_t", dictionary.get("last_sen_t")+1);
        } else {
            generateNoteOnSD(file_name, "last_sen_l " + System.currentTimeMillis());
            dictionary.put("last_sen_l", dictionary.get("last_sen_l")+1);
        }

        //Toast.makeText(MainActivity.this, "last sentence", Toast.LENGTH_SHORT).show();
    }

    private void onSwipeLeft() {
        if (two_fingers) {
            //Toast.makeText(MainActivity.this, "two finger swipe left", Toast.LENGTH_SHORT).show();
            if (listening) {
                listen_turn_next_page();
            } else {
                turn_next_page();
            }
        } else {
            //Toast.makeText(MainActivity.this, "one finger swipe left", Toast.LENGTH_SHORT).show();
            last_sentence();
        }
    }

    private void next_sentence() {
        tts.stop();
        if (cur_sentence < (sentence_text.length-1)) {
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
        //Toast.makeText(MainActivity.this, "next sentence", Toast.LENGTH_SHORT).show();
    }

    private void onSwipeRight() {
        if (two_fingers) {
            //Toast.makeText(MainActivity.this, "two finger swipe right", Toast.LENGTH_SHORT).show();
            if (listening) {
                listen_turn_last_page();
            } else {
                turn_last_page();
            }
        } else {
            //Toast.makeText(MainActivity.this, "one finger swipe right", Toast.LENGTH_SHORT).show();
            if (!listening) {
                generateNoteOnSD(file_name, "next_sen_t " + System.currentTimeMillis());
                dictionary.put("next_sen_t", dictionary.get("next_sen_t")+1);
            } else {
                generateNoteOnSD(file_name, "next_sen_l " + System.currentTimeMillis());
                dictionary.put("next_sen_l", dictionary.get("next_sen_l")+1);
            }
            next_sentence();
        }
    }

    private void tell_page_loca() {
        if (tts.isSpeaking()) {
            tts.stop();
        }
        //int page_count = (word_text.length-1)/(myview.num_col * myview.num_col);
        if (listening) {
            cur_page = sen_start_on_page[cur_sentence];
            generateNoteOnSD(file_name, "chap_status_l " + System.currentTimeMillis());
            dictionary.put("chap_status_l", dictionary.get("chap_status_l")+1);
        } else {
            generateNoteOnSD(file_name, "chap_status_t " + System.currentTimeMillis());
            dictionary.put("chap_status_t", dictionary.get("chap_status_t")+1);
        }
        tts.speak("当前第"+(cur_page+1)+"页，一共"+page_count+"页", QUEUE_FLUSH, null, null);
        listening = false;
    }

    private void tell_book_loca() {
        if (tts.isSpeaking()) {
            tts.stop();
        }
        if (listening) {
            cur_page = sen_start_on_page[cur_sentence];
            generateNoteOnSD(file_name, "book_status_l " + System.currentTimeMillis());
            dictionary.put("book_status_l", dictionary.get("book_status_l")+1);
        } else {
            generateNoteOnSD(file_name, "book_status_t " + System.currentTimeMillis());
            dictionary.put("book_status_t", dictionary.get("book_status_t")+1);
        }
        int pre_count = 0;
        for (int i = 0; i < chapter; i++) {
            for (int j = 0; j < section; j++) {
                pre_count += book_num_page[i][j];
            }
        }
        int entire_sec = 0;
        for (int i = 0; i < chapter; i++) {
            entire_sec += text.title[i].length;
        }
        entire_sec += section;
        pre_count += cur_page;
        tts.speak("百分之"+(int) ((pre_count+1) * 100/total_page_in_book) + ",第"+(chapter+1)+"部分，第"+(entire_sec+1)+"章", QUEUE_FLUSH, null, null);
        listening = false;
    }


    private void onSwipeDown() {
        if (two_fingers) {
            //Toast.makeText(MainActivity.this, "two finger swipe down", Toast.LENGTH_SHORT).show();
            tell_book_loca();
        } else {
            //Toast.makeText(MainActivity.this, "one finger swipe down", Toast.LENGTH_SHORT).show();
            tell_page_loca();
        }
    }

    private void to_page_start() {
        Log.d("debugging","listening status is "+listening);
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
        generateNoteOnSD(file_name, "back_page_l " + System.currentTimeMillis());
        dictionary.put("back_page_l", dictionary.get("back_page_l")+1);
        Log.d("debugging","reached here");
        startPlay(sen, -1);
    }

    private void onSwipeUp() {
        if (two_fingers) {
            //Toast.makeText(MainActivity.this, "two finger swipe up", Toast.LENGTH_SHORT).show();
            to_page_start();
        } else {
            //Toast.makeText(MainActivity.this, "one finger swipe up", Toast.LENGTH_SHORT).show();
            restart_cur_sentence();
        }
    }

    private boolean isExternalStorageWritable() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    public void generateNoteOnSD(String sFileName, String sBody) {
        FileOutputStream fos ;
        Log.d("writingexternalfile", sFileName);
        //Toast.makeText(MainActivity.this, isExternalStorageAvailable(), Toast.LENGTH_SHORT).show();
        try {
            fos = new FileOutputStream(new File(getExternalFilesDir("android"), sFileName), true);

            FileWriter fWriter;

            try {
                fWriter = new FileWriter(fos.getFD());
                fWriter.write(sBody);
                fWriter.write("\n");
                fWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                fos.getFD().sync();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeFile(View v) {
        if (isExternalStorageWritable()) {
            Toast.makeText(MainActivity.this, Environment.getExternalStorageState(), Toast.LENGTH_SHORT).show();
            try {
                File root = Environment.getExternalStorageDirectory();
                File dir = new File(root.getAbsolutePath()+"/download");
                Toast.makeText(MainActivity.this, root.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                dir.mkdirs();
                File textFile = new File(dir, "mydata.txt");
                FileWriter writer = new FileWriter(textFile);
                writer.append("bla");
                writer.flush();
                writer.close();
                Log.d("writingexternalfile", String.valueOf(textFile.getAbsolutePath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
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