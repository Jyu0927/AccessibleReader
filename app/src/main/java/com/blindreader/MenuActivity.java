package com.blindreader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Toast;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

public class MenuActivity extends AppCompatActivity implements GestureDetector.OnGestureListener{
    GestureDetectorCompat gesture_detector_menu;
    Menu menuview;
    boolean two_fingers;
    double first_time;
    float first_x;
    float first_y;
    boolean progress_bar_pressed;
    float progress_bar_basecor;
    float[] progress_bar_height;
    int cur_chapter;
    int cur_section;
    Text text;
    Vibrator vibrator;
    TextToSpeech tts;
    double progress_speak_time;
    int progress_select;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        menuview = new Menu(this);
        setContentView(menuview);
        gesture_detector_menu = new GestureDetectorCompat(this, this);
        text = new Text();
        two_fingers = false;
        first_time = 0;
        first_x = 0;
        first_y = 0;
        progress_bar_pressed = false;
        progress_bar_basecor = 0;
        progress_bar_height = new float[2];
        progress_bar_height[0] = 0;
        progress_bar_height[1] = 0;
        cur_chapter = MainActivity.chapter;
        cur_section = MainActivity.section;
        vibrator = MainActivity.vibrator;
        tts = MainActivity.tts;
        progress_speak_time = 0;
        progress_select = -1;
    }

    private void switchActivities(int chap, int sec) {
        Intent switchActivityIntent = new Intent(this, MainActivity.class);
        switchActivityIntent.putExtra("CHAPTER_ID", chap);
        switchActivityIntent.putExtra("SECTION_ID", sec);
        startActivity(switchActivityIntent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getAction() & MotionEvent.ACTION_MASK;
        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            two_fingers = true;
            Log.d("gesture", "two_fingers changed to true");
        } else if (action == MotionEvent.ACTION_DOWN) {
            Log.d("menu", "downed");
            two_fingers = false;
            first_time = System.currentTimeMillis();
            first_x = e.getX();
            first_y = e.getY();
        } else if (action == MotionEvent.ACTION_MOVE) {
            double time_diff = System.currentTimeMillis() - first_time;
            if (time_diff > 200) {
                progress_bar(e.getY());
            }
        } else if (action == MotionEvent.ACTION_UP) {
            Log.d("gesturedebug", String.valueOf(progress_select));
            if (two_fingers) {
                if (progress_bar_pressed) {
                    Log.d("progressselect", String.valueOf(progress_select));
                    progress_bar_pressed = false;
                    cur_chapter = progress_select;
                    progress_select = -1;
                    cur_section = 0;
                    tts.speak("第"+(cur_chapter+1)+"章,第"+(cur_section+1)+"节,"+text.title[cur_chapter][cur_section], TextToSpeech.QUEUE_FLUSH, null, null);
                }
            } else {
                if (progress_bar_pressed && (progress_select != -1)) {
                    progress_bar_pressed = false;
                    cur_section = progress_select;
                    tts.speak("第"+(cur_chapter+1)+"章,第"+(cur_section+1)+"节,"+text.title[cur_chapter][cur_section], TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
            two_fingers = false;
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            //two_fingers = false;
            /*if (progress_bar_pressed) {
                Log.d("progressselect", String.valueOf(progress_select));
                progress_bar_pressed = false;
                cur_chapter = progress_select;
                progress_select = -1;
                cur_section = 0;
                tts.speak("第"+(cur_chapter+1)+"章,第"+(cur_section+1)+"节", TextToSpeech.QUEUE_FLUSH, null, null);
            }*/
        }
        this.gesture_detector_menu.onTouchEvent(e);
        return super.onTouchEvent(e);
    }

    public void progress_bar(float y) {
        int edge = 50;
        int max, cur;
        if (two_fingers) {
            max = text.rawtext.length - 1;
            cur = cur_chapter;
        } else {
            max = text.rawtext[cur_chapter].length - 1;
            cur = cur_section;
        }
        if (!progress_bar_pressed) {
            progress_bar_pressed = true;
            progress_bar_basecor = y;
            progress_bar_height[0] = (y - edge) / (float) cur;
            progress_bar_height[1] = (menuview.getHeight()-(y+edge)) / (float) (max - cur);
            vibrator.vibrate(VibrationEffect.createOneShot(20, 60));
            if (two_fingers) {
                tts.speak("第"+(cur_chapter+1)+"章,"+text.chap_title[cur_chapter], TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                tts.speak("第"+(cur_section+1)+"节,"+text.title[cur_chapter][cur_section], TextToSpeech.QUEUE_FLUSH, null, null);
            }
            progress_speak_time = System.currentTimeMillis();
        } else {
            int new_select;
            if (y < progress_bar_basecor - edge) {
                new_select = (int) (y / progress_bar_height[0]);
            } else if (y > progress_bar_basecor + edge){
                new_select = (int) ((y - (progress_bar_basecor + edge)) / progress_bar_height[1]) + cur + 1;
                //Log.d("progressbar", "to page" + (int) ((y - progress_bar_basecor) / progress_bar_height[1]));
            } else {
                new_select = cur;
            }
            if (new_select > max) {
                new_select = max;
            }
            if (new_select != progress_select | (System.currentTimeMillis() - progress_speak_time > 2000)) {
                tts.stop();
                progress_speak_time = System.currentTimeMillis();
                if (new_select != progress_select) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, 60));
                }
                if (two_fingers) {
                    tts.speak("第"+(new_select+1)+"章,"+text.chap_title[new_select],TextToSpeech.QUEUE_FLUSH, null, null);
                } else {
                    tts.speak("第"+(new_select+1)+"节,"+text.title[cur_chapter][new_select],TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
            progress_select = new_select;
        }
        if (two_fingers) {
            Log.d("menugesture","two finger dragging");
        } else {
            Log.d("menugesture","dragging");
        }
    }

    /*int edge = 10;
        if (!progress_bar_pressed) {
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
        tts.speak("百分之"+(int) ((cur_page+1) * 100/page_count) + ",第"+(cur_page+1)+"页", TextToSpeech.QUEUE_FLUSH, null, null);
        Log.d("progressbar", "height is "+ progress_bar_height[0]+ progress_bar_height[1]);
        progress_speak_time = System.currentTimeMillis();
    } else {
        int new_page;
        if (y < progress_bar_basecor - edge) {
            new_page = (int) (y / progress_bar_height[0]);
        } else if (y > progress_bar_basecor + edge){
            new_page = (int) ((y - progress_bar_basecor) / progress_bar_height[1]) + cur_page + 1;
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
    }*/

    /*public boolean onTouchEvent(MotionEvent e) {
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
            } else if (time_diff > 200 && !swiping && !two_fingers && (System.currentTimeMillis() - stop_lastTime > 300)) {
                Toast.makeText(MainActivity.this, "dragging", Toast.LENGTH_SHORT).show();
                if (!progress_bar_pressed && !listening && (e.getX() < (myview.edge+myview.cell_width*myview.num_col))) {
                    touch_read_cell(e);
                } else if (first_x >= (myview.edge+myview.cell_width*myview.num_col)) {
                    progress_bar(e.getY());
                    vibrate_on_touch(e.getX(), e.getY()-contentViewTop-statusBarHeight);//check if this is right, only vibrate don't read
                }
                //avoid touch cell when flinging (swiping)
                //move to touch read is much slower -> velocity
                //time_diff controlled otherwise swipe triggers touch read (when starts to swipe velocity low)
            }
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            Log.d("timediff", String.valueOf(Math.hypot(e.getX() - first_x, e.getY() - first_y)));
            if (two_fingers && (Math.hypot(e.getX() - first_x, e.getY() - first_y) < 100)){
                Toast.makeText(MainActivity.this, "two finger single click", Toast.LENGTH_SHORT).show();
                Log.d("gesture", "TWO FINGERS SINGLE_CLICK !");
                Log.d("progressbar", "listening status is "+ listening);
                if (listening) {
                    local_lis = listen_to_touch();
                } else {
                    local_lis = touch_to_listen();
                }
                swiping = false;
            }
            two_fingers = false;
        } else if (action == MotionEvent.ACTION_UP) {
            //touch_release_cell();
            swiping = false;
            if (progress_bar_pressed) {
                progress_bar_pressed = false;
                //local_lis = true;
                if (e.getX() > (myview.edge+myview.cell_width*myview.num_col)) {
                    cur_page = progress_cur_page;
                    if (cur_page > page_count - 1) {
                        cur_page = page_count - 1;
                    }
                }
                //retract the action on progress bar, go back to current page
                progress_to_page(cur_page);
                Log.d("progressbar","to page "+ cur_page);
            }
        }
        listening = local_lis;
        this.gesture_detector.onTouchEvent(e);
        return super.onTouchEvent(e);
    }*/

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        /*Log.d("menu", "downed");
        switchActivities();*/
        return false;
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
        Log.d("switchactivity", motionEvent.getX()+","+motionEvent1.getX());
        Log.d("gesture", "onFling: just fling," + two_fingers);
        //code modified from https://stackoverflow.com/questions/4139288/android-how-to-handle-right-to-left-swipe-gestures
        boolean result = false;
        float diffY = motionEvent1.getY() - motionEvent.getY();
        float diffX = motionEvent1.getX() - motionEvent.getX();
        if (Math.abs(diffX) > Math.abs(diffY)) {
            if (Math.abs(diffX) > MainActivity.SWIPE_THRESHOLD && Math.abs(v) > MainActivity.SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    if (motionEvent.getX() < 15) {
                        //onSwipeRight();
                    }
                } else {
                    tts.stop();
                    onSwipeLeft();
                }
                result = true;
            }
        } else if (Math.abs(diffY) > MainActivity.SWIPE_THRESHOLD && Math.abs(v1) > MainActivity.SWIPE_VELOCITY_THRESHOLD) {
            if (diffY > 0) {
            }
            result = true;
        }
        two_fingers = false;
        return result;
    }

    public void onSwipeLeft() {
        tts.speak("关闭目录", QUEUE_FLUSH, null, null);
        switchActivities(cur_chapter, cur_section);
    }
}