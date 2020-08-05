package com.blindreader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.speech.tts.TextToSpeech;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class MyView extends View{
    private static final String TAG = "order";
    public int num_row, num_col;
    public int cell_height, cell_width;
    private Paint blackLine = new Paint();
    private Paint fill = new Paint();
    public int edge = 100;
    int[] highlighted = {-1,-1};
    //highlighted = {col, row};

    public MyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        blackLine.setStyle(Paint.Style.STROKE);
        fill.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    public void setDimension(int row, int col) {
        num_row = row;
        num_col = col;
        int height = getHeight() - 2 * edge;
        int width = getWidth() - 2 * edge;
        cell_height = height / num_row;
        cell_width = width / num_col;
    }

    public int getEdge() {
        return edge;
    }

    public float getCellWidth() {
        return cell_width;
    }

    public float getCellHeight() {
        return cell_height;
    }

    /*public void highlightCell(float x, float y, ) {
        if (x > edge & x < getWidth()-edge & y > edge & y < getHeight()-edge) {
            int row = (int)(y - (float)edge)/cell_height;
            int col = (int)(x - (float)edge)/cell_width;
            drawRec(col, row, );
        }

    }*/

    public boolean checkTurnNextPage(float x1, float x2, float y1, float y2) {
        //Log.d(TAG, "onFling: cor is "+e.getX()+","+e.getY());
        Log.d(TAG, "onFling: cor status first"+(x1 > edge));
        Log.d(TAG, "onFling: cor status second"+(x1 < getWidth()-edge));
        Log.d(TAG, "onFling: cor status third"+(y1 > edge));
        Log.d(TAG, "onFling: cor status forth"+(y1 < getHeight()-edge));
        if (x1 > edge & x1 < getWidth()-edge & y1 > edge & y1 < getHeight()-edge) {
            int row = (int) (y1 - (float) edge) / cell_height;
            int col = (int) (x1 - (float) edge) / cell_width;
            Log.d(TAG, "checkTurnNextPage: position is "+row+","+col);
            if ((row == (num_row-1)) && (col == (num_col-1))) {
                if (x2 > getWidth()-edge) {
                    Log.d(TAG, "onFling: next page pls");
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkTurnLastPage(float x1, float x2, float y1, float y2) {
        if (x1 > edge & x1 < getWidth()-edge & y1 > edge & y1 < getHeight()-edge) {
            int row = (int) (y1 - (float) edge) / cell_height;
            int col = (int) (x1 - (float) edge) / cell_width;
            Log.d(TAG, "checkTurnNextPage: position is "+row+","+col);
            if ((row == 0) && (col == 0)) {
                if (x2 < edge) {
                    Log.d(TAG, "onFling: last page pls");
                    return true;
                }
            }
        }
        return false;
    }

    public void getHighlightCell(float x, float y, TextToSpeech tts, String[] text, int page, int last_word) {
        Log.d(TAG, "getHighlightCell: first movement cor is "+x+","+y);
        if (x > edge & x < getWidth()-edge & y > edge & y < getHeight()-edge) {
            Log.d(TAG, "getHighlightCell: reached here");
            int row = (int) (y - (float) edge) / cell_height;
            int col = (int) (x - (float) edge) / cell_width;
            highlighted[0] = col;
            highlighted[1] = row;
            int word_index = page * num_row * num_col + row * num_col + col;
            Log.d(TAG, "getHighlightCell: reached here1");
            if (word_index >= new Text().word_text.length) {
                Log.d(TAG, "getHighlightCell: reached here2");
                tts.speak("读完了", TextToSpeech.QUEUE_FLUSH, null, "word-"+word_index);
                return;
            }
            if (!tts.isSpeaking()) {
                tts.speak(text[word_index], TextToSpeech.QUEUE_FLUSH, null, "word-"+word_index);
                //tts.stop();
            } else {
                if (last_word != word_index) {
                    tts.stop();
                    tts.speak(text[word_index], TextToSpeech.QUEUE_FLUSH, null, "word-"+word_index);
                }
            }
            //tts.stop();
            //you may want to change the utteranceid to word_index
            Log.d(TAG, "getHighlightCell: "+row+","+col);
        }

    }

    public void resetHighlight() {
        highlighted[0] = -1;
        highlighted[1] = -1;
    }

    public int wordsPerPage() {
        return  num_row * num_col;
    }

    public void drawRec(int c, int r, Canvas draw_canvas, Paint p) {
        int left = c * cell_width + edge;
        int top = r * cell_height + edge;
        int right = (c+1) * cell_width + edge;
        int bottom = (r+1) * cell_height + edge;
        //Log.d(TAG, "onDraw: " + cell_width +","+cell_height);
        draw_canvas.drawRect(left, top, right, bottom, p);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        //canvas.drawRect(getLeftPaddingOffset(),getTopPaddingOffset(),getRightPaddingOffset(),getBottomPaddingOffset(),blackPaint);
        for (int i = 0; i < num_col; i++) {
            for (int j = 0; j < num_row; j++) {
                drawRec(i, j, canvas, blackLine);
            }
        }
        if (highlighted[0] != -1 & highlighted[1] != -1) {
            drawRec(highlighted[0], highlighted[1], canvas, fill);
        }
        super.onDraw(canvas);
    }



}




















