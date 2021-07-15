package com.example.threaddemo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * This is a thread demo
 * When the user touches the image, a red bar will roll down.
 * If the user lifts before it is done, then it will pause the thread.
 */

public class MainActivity extends AppCompatActivity {

    TextView log;
    ImageView theboardfield;
    Bitmap theboard;
    Canvas theboardc;
    final int boardsize = 480;
    boolean isAnimation = false, pause = false;
    protected Handler handler;
    //for drawing
    Rect myRec;
    Paint myColor;
    //for the thread
    Thread myThread;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        log = findViewById(R.id.log);
        //get the imageview and create a bitmap to put in the imageview.
        //also create the canvas to draw on.
        theboardfield = findViewById(R.id.boardfield);
        theboard = Bitmap.createBitmap(boardsize, boardsize, Bitmap.Config.ARGB_8888);
        theboardc = new Canvas(theboard);
        theboardc.drawColor(Color.WHITE);  //background color for the board.
        theboardfield.setImageBitmap(theboard);
        theboardfield.setOnTouchListener(new myTouchListener());
        //For drawing
        myRec = new Rect(0, 0, 10, 10);
        myColor = new Paint();  //default black
        myColor.setStyle(Paint.Style.FILL);

        //message handler for the animation.
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == 0) { //redraw image
                    if (theboard != null && theboardfield != null) {
                        drawBmp();
                    }
                } else {  //get the data package out of the msg
                    Bundle stuff = msg.getData();
                    logthis("Thread: " + stuff.getString("logthis"));
                }
                return true;
            }
        });

    }

    /**
     * attempts to start or restart a the thread
     */
    public void go() {
        if (pause) {  //The thread is paused, so attempt to restart it.
            logthis("About to notify!");
            pause = false;
            synchronized (myThread) {
                myThread.notify(); //in theory, this should wakeup the thread.  OR myThread.notifyAll()
            }
            logthis("waiting threads should be notified.");
        } else { //the thread is not running, so just start it.
            isAnimation = true;
            if (myThread != null)
                myThread = null;
            myThread = new Thread(new animator());
            myThread.start();
        }
    }

    /**
     * attempt to stop/suspend a thread
     */
    public void stop() {
        if (!pause) { //So it is running, now to stop it.
            if (myThread.isAlive()) {//and still active
                //myThread.suspend();  //suspend the thread, except This method is deprecated!  stupid andriod...
                pause = true;
            }
        }
    }

    /**
     * touch listener
     */
    class myTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            //This toggles back and forth the thread.  Starting or stopping it, based on up/down events.    		
            int action = event.getAction();
            switch (action) {
                //case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_DOWN:
                    logthis("onTouch called, DOWN");
                    go();
                    return true;
                case MotionEvent.ACTION_UP:
                    logthis("onTouch called, UP");
                    stop();
                    return true;
            }

            return false;
        }
    }


    public void drawBmp() {
        theboardfield.setImageBitmap(theboard);
        theboardfield.invalidate();
    }

    public void sendmessage(String logthis) {
        Bundle b = new Bundle();
        b.putString("logthis", logthis);
        Message msg = handler.obtainMessage();
        msg.setData(b);
        msg.arg1 = 1;

        msg.what = 1;  //so the empty message is not used!
        System.out.println("About to Send message" + logthis);
        handler.sendMessage(msg);
        System.out.println("Sent message" + logthis);
    }

    class animator implements Runnable {
        @Override
        public void run() {
            boolean done = false;
            int x = 0;
            //first draw a red down the board
            myColor.setColor(Color.RED);
            while (!done) {
                if (!isAnimation) {
                    return;
                }  //To stop the thread, if the system is paused or killed.

                if (pause) {  //We should wait now, until unpaused.
                    System.out.println("Attempting to send message");
                    sendmessage("Waiting!");
                    System.out.println("Sent message:  waiting");
                    //logthis("Thread: Waiting!");
                    try {
                        synchronized (myThread) {
                            myThread.wait();
                        }
                    } catch (InterruptedException e) {
                        sendmessage("Can't Wait!!!");
                        //logthis("Thread: Can't WAIT!!!!");
                    }
                    sendmessage("Resumed!");
                    //logthis("Thread: Resumed!");
                }

                theboardc.drawLine(0, x, boardsize, x, myColor);
                x++;

                //send message to redraw
                handler.sendEmptyMessage(0);
                //now wait a little bit.
                try {
                    Thread.sleep(10);  //change to 100 for a commented out block, instead of just a line.
                } catch (InterruptedException e) {
                    //don't care
                }
                //determine if we are done or move the x?
                if (x >= boardsize) done = true;
            }
            theboardc.drawColor(Color.WHITE);
            myColor.setColor(Color.BLACK);
            isAnimation = false;

        }
    }

    /**
     * simple method to add the log TextView.
     */
    public void logthis(String newinfo) {
        if (newinfo.compareTo("") != 0) {
            log.setText(log.getText() + "\n" + newinfo);
        }
    }
}