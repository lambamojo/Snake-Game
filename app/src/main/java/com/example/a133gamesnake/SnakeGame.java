package com.example.a133gamesnake;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import android.graphics.Bitmap;
import android.graphics.Typeface;



public class SnakeGame extends SurfaceView implements Runnable {



    // Objects for the game loop/thread
    private Thread mThread = null;
    // Control pausing between updates
    private long mNextFrameTime;
    // Is the game currently playing and or paused?
    private volatile boolean mPlaying = false;
    private volatile boolean mPaused = true;

    // for playing sound effects
    private SoundPool mSP;
    private int mEat_ID = -1;
    private int mCrashID = -1;

    // The size in segments of the playable area
    private final int NUM_BLOCKS_WIDE = 40;
    private int mNumBlocksHigh;

    // How many points does the player have
    private int mScore;

    // Objects for drawing
    private Canvas mCanvas;
    private SurfaceHolder mSurfaceHolder;
    private Paint mPaint;

    // A snake ssss
    private Snake mSnake;
    // And an apple
    private Apple mApple;

    // Background image
    private Bitmap mBackgroundBitmap;

    // Your names
    private String mYourNames = "Andres and Lamba";

    // Custom font
    private Typeface mCustomFont;

    // Pause button coordinates and size
    private int mPauseButtonX;
    private int mPauseButtonY;
    private int mPauseButtonSize;

    // This is the constructor method that gets called
    // from SnakeActivity
    public SnakeGame(Context context, Point size) {
        super(context);

        // Work out how many pixels each block is
        int blockSize = size.x / NUM_BLOCKS_WIDE;
        // How many blocks of the same size will fit into the height
        mNumBlocksHigh = size.y / blockSize;

        // Initialize the SoundPool
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            mSP = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            mSP = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        }
        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // Prepare the sounds in memory
            descriptor = assetManager.openFd("get_apple.ogg");
            mEat_ID = mSP.load(descriptor, 0);

            descriptor = assetManager.openFd("snake_death.ogg");
            mCrashID = mSP.load(descriptor, 0);

        } catch (IOException e) {
            // Error
        }

        // Initialize the drawing objects
        mSurfaceHolder = getHolder();
        mPaint = new Paint();

        // Load custom font
        mCustomFont = Typeface.createFromAsset(context.getAssets(), "myFont.ttf");

        // Load background image
        mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.greenbck);
        mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap, size.x, size.y, true);

        // Call the constructors of our two game objects
        mApple = new Apple(context,
                new Point(NUM_BLOCKS_WIDE,
                        mNumBlocksHigh),
                blockSize);

        mSnake = new Snake(context,
                new Point(NUM_BLOCKS_WIDE,
                        mNumBlocksHigh),
                blockSize);


        // Set pause button coordinates and size
        mPauseButtonSize = blockSize * 2;
        mPauseButtonX = blockSize / 2;
        mPauseButtonY = size.y - mPauseButtonSize - blockSize / 2;
    }

    // Method to toggle the game's pause state
    public void togglePause() {
        mPaused = !mPaused;
    }
    // Called to start a new game
    public void newGame() {

        // reset the snake
        mSnake.reset(NUM_BLOCKS_WIDE, mNumBlocksHigh);

        // Get the apple ready for dinner
        mApple.spawn();

        // Reset the mScore
        mScore = 0;

        // Setup mNextFrameTime so an update can triggered
        mNextFrameTime = System.currentTimeMillis();
    }


    // Handles the game loop
    @Override
    public void run() {
        while (mPlaying) {
            if(!mPaused) {
                // Update 10 times a second
                if (updateRequired()) {
                    update();
                }
            }

            draw();
        }
    }


    // Check to see if it is time for an update
    public boolean updateRequired() {

        // Run at 10 frames per second
        final long TARGET_FPS = 10;
        // There are 1000 milliseconds in a second
        final long MILLIS_PER_SECOND = 1000;

        // Are we due to update the frame
        if(mNextFrameTime <= System.currentTimeMillis()){
            // Tenth of a second has passed

            // Setup when the next update will be triggered
            mNextFrameTime =System.currentTimeMillis()
                    + MILLIS_PER_SECOND / TARGET_FPS;

            // Return true so that the update and draw
            // methods are executed
            return true;
        }

        return false;
    }


    // Update all the game objects
    public void update() {

        // Move the snake
        mSnake.move();

        // Did the head of the snake eat the apple?
        if(mSnake.checkDinner(mApple.getLocation())){
            // This reminds me of Edge of Tomorrow.
            // One day the apple will be ready!
            mApple.spawn();

            // Add to  mScore
            mScore += 1 ;

            // Play a sound
            mSP.play(mEat_ID, 1, 1, 0, 0, 1);
        }

        // Did the snake die?
        if (mSnake.detectDeath()) {
            // Pause the game ready to start again
            mSP.play(mCrashID, 1, 1, 0, 0, 1);

            mPaused =true;
        }

    }


    // Do all the drawing
    public void draw() {
        // Get a lock on the mCanvas
        if (mSurfaceHolder.getSurface().isValid()) {
            mCanvas = mSurfaceHolder.lockCanvas();

            // Draw background
            mCanvas.drawBitmap(mBackgroundBitmap, 0, 0, null);


            // Draw score
            mPaint.setColor(Color.WHITE);
            mPaint.setTextSize(120);
            mPaint.setTypeface(mCustomFont);
            mCanvas.drawText("" + mScore, 20, 120, mPaint);

            // Draw your names
            mCanvas.drawText(mYourNames, mCanvas.getWidth() - 1085, 120, mPaint);

            // Draw the apple and the snake
            mApple.draw(mCanvas, mPaint);
            mSnake.draw(mCanvas, mPaint);

            // Draw pause button
            if (!mPaused) {
                mPaint.setColor(Color.WHITE);
                mCanvas.drawRect(mPauseButtonX, mPauseButtonY,
                        mPauseButtonX + mPauseButtonSize, mPauseButtonY + mPauseButtonSize, mPaint);
                mPaint.setColor(Color.WHITE);
                mPaint.setTextSize(80);
                mCanvas.drawText("⏸️", mPauseButtonX + 10, mPauseButtonY + mPauseButtonSize - 20, mPaint);
            }

            // Draw some text while paused
            if (mPaused) {
                mPaint.setColor(Color.WHITE);
                mPaint.setTextSize(250);
                mPaint.setTypeface(mCustomFont);
                mCanvas.drawText(getResources().getString(R.string.tap_to_play), 350, 700, mPaint);
            }

            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int touchX = (int) motionEvent.getX();
        int touchY = (int) motionEvent.getY();

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_UP:
                if (mPaused) {
                    // If paused, start a new game and unpause
                    newGame();
                    togglePause();
                } else {
                    // If not paused, check if touch coordinates are within the pause button's area
                    if (touchX >= mPauseButtonX && touchX <= mPauseButtonX + mPauseButtonSize &&
                            touchY >= mPauseButtonY && touchY <= mPauseButtonY + mPauseButtonSize) {
                        // Toggle pause state
                        togglePause();
                    } else {
                        // Control the snake's direction based on touch position
                        mSnake.switchHeading(motionEvent);
                    }
                }
                break;
        }

        return true;
    }


    // Stop the thread
    public void pause() {
        mPlaying = false;
        if (mThread != null) {
            try {
                mThread.join();
            } catch (InterruptedException e) {
                // Error
            }
        }
    }


    // Start the thread
    public void resume() {
        mPlaying = true;
        mThread = new Thread(this);
        mThread.start();
    }
}