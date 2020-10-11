package com.blindreader;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Toast;

import static com.blindreader.MainActivity.mTwoFingersFling;
import static com.blindreader.MainActivity.mTwoFingersFlingConfirmed;

public class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

    public String TAG = "Gesture";
    public static boolean ifScroll = false;
    @Override
    public boolean onDown(MotionEvent motionEvent) {
//            Log.d("Gesture", "onDown: single tap seperate");
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
//        Log.d("Gesture", "showPress");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
//        Log.d("Gesture", "onSingleTapUp:removed");
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
//        Log.d("Gesture", "onScroll");

//        if(mTwoFingersFlingConfirmed)
//        {
//            Log.d(TAG, " double fingers onScroll");
//        }else if(!mTwoFingersFling){
//            Log.d(TAG, "onScroll");
//        }



        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        Log.d(TAG, "onLongPress");
//      Toast.makeText(MainActivity.this, "last sentence", Toast.LENGTH_SHORT).show();
    }


    public boolean onSingleTapConfirmed(MotionEvent event){
        Log.d(TAG, " onSingleTapConfirmed");
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event){
        Log.d(TAG, " onDoubleTap");
        return true;
    }


    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
//            Log.d(TAG, "onFling: detected");
//            //code modified from https://stackoverflow.com/questions/4139288/android-how-to-handle-right-to-left-swipe-gestures
//            boolean result = false;
//            float diffY = motionEvent1.getY() - motionEvent.getY();
//            float diffX = motionEvent1.getX() - motionEvent.getX();
//            Log.d(TAG, "onFling: listening status is" + listening);
//            if (!listening) {
//                float x1 = motionEvent.getX();
//                float x2 = motionEvent1.getX();
//                float y1 = motionEvent.getY() - contentViewTop - statusBarHeight;
//                float y2 = motionEvent1.getY() - contentViewTop - statusBarHeight;
//                if (myview.checkTurnNextPage(x1, x2, y1, y2)) {
//                    int page_count = word_text.length / (myview.num_col * myview.num_col);
//                    if ((cur_page + 1) * myview.num_col * myview.num_row >= word_text.length) {
//                        tts.speak("已经在尾页", TextToSpeech.QUEUE_FLUSH, null, null);
//                    } else {
//                        cur_page += 1;
//                        if (tts.isSpeaking()) {
//                            tts.stop();
//                        }
//                        tts.speak("下一页", TextToSpeech.QUEUE_FLUSH, null, null);
//                        Log.d(TAG, "onFling: turned to page " + cur_page + " of total " + page_count);
//                    }
//                    return true;
//                }
//                if (myview.checkTurnLastPage(x1, x2, y1, y2)) {
//                    if (cur_page > 0) {
//                        cur_page -= 1;
//                        if (tts.isSpeaking()) {
//                            tts.stop();
//                        }
//                        tts.speak("上一页", TextToSpeech.QUEUE_FLUSH, null, null);
//                        Log.d(TAG, "onFling: turned to page " + cur_page);
//                    } else {
//                        tts.speak("已经在首页", TextToSpeech.QUEUE_FLUSH, null, null);
//                    }
//                    return true;
//                }
//            }
//            if (Math.abs(diffX) > Math.abs(diffY)) {
//                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(v) > SWIPE_VELOCITY_THRESHOLD) {
//                    if (diffX > 0) {
//                        onSwipeRight();
//                    } else {
//                        onSwipeLeft();
//                    }
//                    result = true;
//                }
//            } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(v1) > SWIPE_VELOCITY_THRESHOLD) {
//                if (diffY > 0) {
//                    onSwipeDown();
//                } else {
//                    onSwipeUp();
//                }
//                result = true;
//            }
        if (mTwoFingersFlingConfirmed) {
            Log.d(TAG, " double fingers onFling");
        } else {
            Log.d(TAG, "onFling");
        }
        twoFingersReset();


        return true;
    }

//        private void onSwipeLeft() {
//            if (listening) {
//                tts.stop();
//                if (cur_sentence > 0) {
//                    cur_sentence -= 1;
//                }
//                startPlay(cur_sentence);
//            } else {
//                if (tts.isSpeaking()) {
//                    tts.stop();
//                }
//                if (cur_sentence > 0) {
//                    cur_sentence -= 1;
//                }
//            }
//            Toast.makeText(MainActivity.this, "last sentence", Toast.LENGTH_SHORT).show();
//        }
//
//        private void onSwipeRight() {
//            Log.d(TAG, "onSwipeRight: listening status" + listening);
//            Log.d(TAG, "onSwipeRight: speaking status" + tts.isSpeaking());
//            if (listening) {
//                tts.stop();
//                if (cur_sentence < (text.sentence_text.length - 1)) {
//                    cur_sentence += 1;
//                }
//                startPlay(cur_sentence);
//            } else {
//                if (tts.isSpeaking()) {
//                    tts.stop();
//                }
//                Log.d(TAG, "onSwipeRight: not speaking");
//                if (cur_sentence < (text.sentence_text.length - 1)) {
//                    cur_sentence += 1;
//                }
//            }
//            Toast.makeText(MainActivity.this, "next sentence", Toast.LENGTH_SHORT).show();
//        }
//
//        private void onSwipeUp() {
//            Toast.makeText(MainActivity.this, "swiped up", Toast.LENGTH_SHORT).show();
//        }
//
//        private void onSwipeDown() {
//            Toast.makeText(MainActivity.this, "swiped down", Toast.LENGTH_SHORT).show();
//        }

    public void twoFingersReset(){
        mTwoFingersFlingConfirmed = false;
        mTwoFingersFling = false;
    }
}
