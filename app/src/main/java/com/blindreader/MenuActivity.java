package com.blindreader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.SoundPool;
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
    boolean single_tap;
    boolean double_tap;
    SoundPool sounds;
    int confirm_sound;
    double tap_time;

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
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        sounds = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();
        confirm_sound = sounds.load(this, R.raw.confirm, 1);
        tap_time = 0;
    }

    private void switchActivities(int chap, int sec) {
        Intent switchActivityIntent = new Intent(this, MainActivity.class);
        switchActivityIntent.putExtra("CHAPTER_ID", chap);
        switchActivityIntent.putExtra("SECTION_ID", sec);
        startActivity(switchActivityIntent);
        finish();
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
            if (progress_bar_pressed) {

            }
        } else if (action == MotionEvent.ACTION_UP) {
            Log.d("gesturedebug", String.valueOf(progress_select));
            if (two_fingers) {
                /*if (progress_bar_pressed) {
                    single_tap = false;
                    double_tap = false;
                    Log.d("progressselect", String.valueOf(progress_select));
                    progress_bar_pressed = false;
                    cur_chapter = progress_select;
                    progress_select = -1;
                    cur_section = 0;
                    tts.speak("第"+(cur_chapter+1)+"章,第"+(cur_section+1)+"节,"+text.title[cur_chapter][cur_section], TextToSpeech.QUEUE_FLUSH, null, null);
                }*/
            } else {
                if (progress_bar_pressed && (progress_select != -1)) {
                    single_tap = false;
                    double_tap = false;
                    progress_bar_pressed = false;
                    getPosition(progress_select);
                    int tmp = get_cur(cur_chapter, cur_section);
                    tts.speak("第"+(tmp-cur_chapter+1)+"章,"+text.title[cur_chapter][cur_section], TextToSpeech.QUEUE_FLUSH, null, null);
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
        int edge = 75;
        int max, cur;
        /*if (two_fingers) {
            max = text.rawtext.length - 1;
            cur = cur_chapter;
        } else {
            max = text.rawtext[cur_chapter].length - 1;
            cur = cur_section;
        }*/
        max = 0;
        for (int i = 0; i < text.chap_title.length; i++) {
            max += text.title[i].length + 1;
        }
        max -= 1;
        cur = get_cur(cur_chapter, cur_section);
        if (!progress_bar_pressed) {
            progress_bar_pressed = true;
            progress_bar_basecor = y;
            //progress_bar_height[0] = (y - edge) / (float) cur;
            //progress_bar_height[1] = (menuview.getHeight()-(y+edge)) / (float) (max - cur);
            vibrator.vibrate(VibrationEffect.createOneShot(20, 60));
            /*if (two_fingers) {
                tts.speak("第"+(cur_chapter+1)+"章,"+text.chap_title[cur_chapter], TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                tts.speak("第"+(cur_chapter+1)+"章,"+"第"+(cur_section+1)+"节,"+text.title[cur_chapter][cur_section], TextToSpeech.QUEUE_FLUSH, null, null);
            }*/
            tts.speak("第"+(cur+1)+"章,"+text.title[cur_chapter][cur_section], TextToSpeech.QUEUE_FLUSH, null, null);
            progress_speak_time = System.currentTimeMillis();
        } else {
            int new_select;
            if (y < progress_bar_basecor - edge) {
                //new_select = (int) (y / progress_bar_height[0]);
                new_select = cur - (int) (((progress_bar_basecor - edge) - y) / (edge * 2)) - 1;
            } else if (y > progress_bar_basecor + edge){
                //new_select = (int) ((y - (progress_bar_basecor + edge)) / progress_bar_height[1]) + cur + 1;
                new_select = cur + (int) ((y - (progress_bar_basecor + edge)) / (edge * 2)) + 1;
                //Log.d("progressbar", "to page" + (int) ((y - progress_bar_basecor) / progress_bar_height[1]));
            } else {
                new_select = cur;
            }
            if (new_select > max) {
                new_select = max;
            }
            if (new_select < 0) {
                new_select = 0;
            }


            if (new_select != progress_select | (System.currentTimeMillis() - progress_speak_time > 2000)) {
                tts.stop();
                progress_speak_time = System.currentTimeMillis();
                if (new_select != progress_select) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, 60));
                }
                int chap = 0;
                int sec = 0;
                int count = 0;
                int prev_count = 0;
                boolean chap_start = false;
                for (int i = 0; i < text.chap_title.length; i++) {
                    prev_count = count;
                    count += text.title[i].length + 1;
                    if (new_select < count) {
                        chap = i;
                        sec = new_select - prev_count - 1;
                        if (sec == -1) {
                            sec = 0;
                            chap_start = true;
                        } else {
                            chap_start = false;
                        }
                        break;
                    }
                }
                if (chap_start) {
                    tts.speak("第"+(chap+1)+"部分,"+text.chap_title[chap],TextToSpeech.QUEUE_FLUSH, null, null);
                } else {
                    int tmp = new_select - chap;
                    tts.speak("第"+tmp+"章,"+text.title[chap][sec],TextToSpeech.QUEUE_FLUSH, null, null);
                }
                /*if (two_fingers) {
                    tts.speak("第"+(new_select+1)+"章,"+text.chap_title[new_select],TextToSpeech.QUEUE_FLUSH, null, null);
                } else {
                    tts.speak("第"+(new_select+1)+"节,"+text.title[cur_chapter][new_select],TextToSpeech.QUEUE_FLUSH, null, null);
                }*/
            }
            progress_select = new_select;
        }
        if (two_fingers) {
            Log.d("menugesture","two finger dragging");
        } else {
            Log.d("menugesture","dragging");
        }
    }

    public int get_cur(int chap, int sec) {
        int result = 0;
        for (int i = 0; i < chap; i++) {
            result += text.title[i].length + 1;
        }
        result = result + sec;
        return result;
    }

    public void getPosition(int select) {
        int count = 0;
        int prev_count = 0;
        for (int i = 0; i < text.chap_title.length; i++) {
            prev_count = count;
            count += text.title[i].length + 1;
            if (select < count) {
                cur_chapter = i;
                cur_section = select - prev_count - 1;
                if (cur_section == -1) {
                    cur_section = 0;
                }
                return;
            }
        }
        cur_chapter = text.chap_title.length - 1;
        cur_section = text.title[cur_chapter].length - 1;
        return;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }


    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        if (!single_tap) {
            single_tap = true;
            double_tap = false;
            tap_time = System.currentTimeMillis();
        } else {
            if (System.currentTimeMillis() - tap_time < 800) {
                sounds.play(confirm_sound, 1, 1, 0, 0, 1);
                double_tap = true;
                single_tap = false;
                switchActivities(cur_chapter, cur_section);
            } else {
                double_tap = false;
                single_tap = false;
            }
        }
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
        /*if (double_tap) {
            switchActivities(cur_chapter, cur_section);
        } else {
        }*/
        finish();
    }
}