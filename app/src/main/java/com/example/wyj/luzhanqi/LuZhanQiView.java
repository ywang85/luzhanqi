package com.example.wyj.luzhanqi;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.example.wyj.luzhanqi.game.Board;
import com.example.wyj.luzhanqi.game.Coordinate;
import com.example.wyj.luzhanqi.game.Pieces;
import com.example.wyj.luzhanqi.game.ai.ABSearchRunnable;

import java.io.InputStream;
import java.util.Random;

/**
 * Created by wyj on 2018/1/23.
 */

public class LuZhanQiView extends SurfaceView implements SurfaceHolder.Callback {
    protected class GameThread extends Thread {
        public static final int STATE_LINEUP = 1;
        //		public static final int STATE_READY = 2;
        public static final int STATE_RUNNING = 3;
        //		public static final int STATE_PAUSE = 4;
        public static final int STATE_LOSE = 5;
        public static final int STATE_WIN = 6;
        // interval to refresh board
        private static final int MOVE_ELAPSED_TIME = 100;
        // Indicate whether the surface has been created & is ready to draw
        private boolean surfaceReady = false;
        private int mode;		// State of the game
        private SurfaceHolder mSurfaceHolder;// Handle to the surface manager object we interact with
        private Handler mHandler;
        private Context mContext;
        private Paint paint;
        private Paint fontPaint;
        private float mFontHeight;
        private InputStream is0 = null, is1= null; // Line Up Input Stream
        // Used to figure out elapsed time between frames
        private long mLastTime;
        private boolean showTitle = false;
        private boolean showFlag = false;

        public GameThread(SurfaceHolder surfaceHolder, Context context,
                          Handler handler) {
            // get handles to some important objects
            mSurfaceHolder = surfaceHolder;
            mContext = context;
            mHandler = handler;
            paint = new Paint();

            fontPaint = new Paint();
            String familyName = "宋体";
            Typeface font = Typeface.create(familyName,Typeface.BOLD);
            fontPaint.setColor(Color.BLACK);
            fontPaint.setTypeface(font);
            fontPaint.setAntiAlias(true);

            Paint.FontMetrics fm = fontPaint.getFontMetrics();
            mFontHeight = fm.descent - fm.top - 4.0f;
            //Log.d(this.getClass().getName(), String.valueOf(fm.descent));
            //Log.d(this.getClass().getName(), String.valueOf(fm.top));


        }

        /**
         * Play sound for fun
         */
        private void playSound(final int soundID) {
            new Thread() {
                public void run() {
                    mediaPlayer = MediaPlayer.create(mContext, soundID);
                    mediaPlayer.start();
                }
            }.start();
        }

        /**
         * Init the game
         */
        private void initGame() {
            // initiate the board
            board.initBoard();
            Resources res = mContext.getResources();
            is0 = res.openRawResource(R.raw.lineup_1);
            is0 = randomLineup();
            is1 = randomLineup();

            if (is0 != null){
                board.loadPieces(Pieces.MAN_TAG, is0);
            }
            if( is1!= null){
                board.loadPieces(Pieces.AI_TAG, is1);
            }
            setWhosTurn(Pieces.MAN_TAG);
            setShowFlag(false);
            setMode(STATE_LINEUP);
            //setMode(STATE_RUNNING);
            //setShowTitle(true);
        }

        /**
         * Start a fresh game
         */
        public void startGame(){
            if (mode == STATE_LINEUP) {
                setMode(STATE_RUNNING);
                playSound(R.raw.gamestart);
            }
        }

        /**
         * ReStart a fresh game
         */
        public void restartGame(){
            initGame();
            //playSound(R.raw.gamerestart0);
        }


        /**
         * Figures the state and sets the UI to the next state.
         */
        private void updatePhysics() {
            long now = System.currentTimeMillis();
            // This allows the game-start to delay the start of the physics
            // by 100ms or whatever.
            if (mLastTime > now) {
                return;
            }
            long elapsed = now - mLastTime;

            if (elapsed > MOVE_ELAPSED_TIME) {
                if(board.hasNextStep()) {
                    boolean stillHasNext = board.nextStep();
                    if (!stillHasNext) { // the last Step
                        int moveAndAttackResult = board.getMoveAndAttackResult();
                        if (moveAndAttackResult == Board.AI_LOST) {
                            playSound(R.raw.win);
                            setMode(STATE_WIN);
                            return;
                        } else if (moveAndAttackResult == Board.MAN_LOST) {
                            playSound(R.raw.gameover);
                            setMode(STATE_LOSE);
                            return;
                        } else if (moveAndAttackResult == Board.KILL ) {
                            checkShowFlag();
                            playSound(R.raw.kill);
                        } else if (moveAndAttackResult == Board.KILLED ) {
                            checkShowFlag();
                            playSound(R.raw.killed);
                        } else if (moveAndAttackResult == Board.EQUAL ) {
                            checkShowFlag();
                            playSound(R.raw.equal);
                        } else if (moveAndAttackResult == Board.MOVE ) {
                            playSound(R.raw.move);
                        }
                        changeWhosTurn();
                    }
                }
                mLastTime = now;
            }
        }

        /**
         * check the board whether the FLAG_N needs to be shown
         */
        private void checkShowFlag(){
            if (!showFlag){
                byte[][] ba = board.getBoardArea();
                for (int j = 0 ; j< Board.BOARD_HEIGHT; j++){
                    for (int i = 0; i< Board.BOARD_WIDTH; i++){
                        if (ba[j][i] == Pieces.SILING_N){
                            return;
                        }
                    }
                }
                showFlag = true;
            }
        }

        // Draw on the canvas
        private void doDraw(Canvas c) {
            // Draw the board
            drawBoard(c);
            drawPieces(c);
            drawFromTo(c);
        }

        private void drawFromTo(Canvas c) {
            Coordinate from = board.getFrom();
            Coordinate from0 = board.getFrom0();
            Coordinate to = board.getTo();
            Coordinate to0 = board.getTo0();

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);

            paint.setColor(Color.BLUE);
            if (from0!=null) {
                int tmpy = from0.y < 6 ? (from0.y+1) : (from0.y+2);
                c.drawRect(X_OFFSET + RECT_WIDTH / 2 + from0.x* RECT_WIDTH - PIECE_WIDTH / 2 -1, Y_OFFSET + tmpy * RECT_HEIGHT - PIECE_HEIGHT / 2 -1 ,
                        X_OFFSET + RECT_WIDTH / 2 + from0.x* RECT_WIDTH + PIECE_WIDTH / 2 + 2, Y_OFFSET + tmpy * RECT_HEIGHT + PIECE_HEIGHT / 2 +2 , paint);
            }
            if (to0!=null) {
                int tmpy = to0.y < 6 ? (to0.y+1) : (to0.y+2);
                c.drawRect(X_OFFSET + RECT_WIDTH / 2 + to0.x* RECT_WIDTH - PIECE_WIDTH / 2 -1, Y_OFFSET + tmpy * RECT_HEIGHT - PIECE_HEIGHT / 2 -1 ,
                        X_OFFSET + RECT_WIDTH / 2 + to0.x* RECT_WIDTH + PIECE_WIDTH / 2 +2, Y_OFFSET + tmpy * RECT_HEIGHT + PIECE_HEIGHT / 2 +2 , paint);
            }

            paint.setColor(Color.RED);
            if (from!=null) {
                int tmpy = from.y < 6 ? (from.y+1) : (from.y+2);
                c.drawRect(X_OFFSET + RECT_WIDTH / 2 + from.x* RECT_WIDTH - PIECE_WIDTH / 2 -1, Y_OFFSET + tmpy * RECT_HEIGHT - PIECE_HEIGHT / 2 -1,
                        X_OFFSET + RECT_WIDTH / 2 + from.x* RECT_WIDTH + PIECE_WIDTH / 2 +2, Y_OFFSET + tmpy * RECT_HEIGHT + PIECE_HEIGHT / 2 +2 , paint);
            }
            if (to!=null) {
                int tmpy = to.y < 6 ? (to.y+1) : (to.y+2);
                c.drawRect(X_OFFSET + RECT_WIDTH / 2 + to.x* RECT_WIDTH - PIECE_WIDTH / 2 -1, Y_OFFSET + tmpy * RECT_HEIGHT - PIECE_HEIGHT / 2 -1 ,
                        X_OFFSET + RECT_WIDTH / 2 + to.x* RECT_WIDTH + PIECE_WIDTH / 2 +2, Y_OFFSET + tmpy * RECT_HEIGHT + PIECE_HEIGHT / 2 + 2 , paint);
            }
            paint.setStrokeWidth(1);
        }

        private void drawPieces(Canvas c) {
            byte[][] ba = board.getBoardArea();

            for (int y = 0; y < Board.BOARD_HEIGHT; y++) {
                for (int x = 0; x < Board.BOARD_WIDTH; x++) {
                    int tmpy = y < 6 ? (y+1) : (y+2);
                    if (ba[y][x] != Board.INVALID_BOARD_TAG) {
//						mFace = Typeface.defaultFromStyle(0);
                        String title = Pieces.pieceTitle(ba[y][x]);
                        int located = Pieces.getLocated(ba[y][x]);

                        paint.setStyle(Paint.Style.FILL);
                        if (located == Pieces.MAN_TAG ) { // color = green
                            paint.setColor(0xFF10F020);
                        } else { // color = orange
                            paint.setColor(0xFFF08010);
                        }
                        // For awareness (set the Flag position Blue)
                        if (ba[y][x] == Pieces.FLAG_S || ba[y][x] == Pieces.FLAG_N && (showTitle || showFlag ) ){
                            paint.setColor(0xFFB15BFF);
                        }
                        // Draw the Piece
                        c.drawRect(X_OFFSET + RECT_WIDTH / 2 + x* RECT_WIDTH - PIECE_WIDTH / 2, Y_OFFSET + tmpy * RECT_HEIGHT - PIECE_HEIGHT / 2,
                                X_OFFSET + RECT_WIDTH / 2 + x* RECT_WIDTH + PIECE_WIDTH / 2, Y_OFFSET + tmpy * RECT_HEIGHT + PIECE_HEIGHT / 2, paint);

                        // Draw the Border of the piece
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setColor(Color.BLACK);
                        c.drawRect(X_OFFSET + RECT_WIDTH / 2 + x* RECT_WIDTH - PIECE_WIDTH / 2, Y_OFFSET + tmpy * RECT_HEIGHT - PIECE_HEIGHT / 2,
                                X_OFFSET + RECT_WIDTH / 2 + x* RECT_WIDTH + PIECE_WIDTH / 2, Y_OFFSET + tmpy * RECT_HEIGHT + PIECE_HEIGHT / 2, paint);

                        // Draw the text
                        if (located == Pieces.MAN_TAG || located == Pieces.AI_TAG && showTitle
                                || located == Pieces.AI_TAG && showFlag && ba[y][x] == Pieces.FLAG_N ) {
                            float mTextPosx = X_OFFSET + RECT_WIDTH / 2 + x* RECT_WIDTH - PIECE_WIDTH / 2 + PIECE_WIDTH / 4;
                            float locy = Y_OFFSET + tmpy * RECT_HEIGHT - PIECE_HEIGHT / 2 ;
                            float mTextPosy = locy +  PIECE_HEIGHT - (PIECE_HEIGHT - mFontHeight) / 2.0f ;
                            //						float mTextPosy = locy +  PIECE_HEIGHT * 3 /4.0f;
                            c.drawText(title, mTextPosx, mTextPosy, fontPaint);
                        }
                    }
                }
            }
        }

        private void drawBoard(Canvas c) {
            // Draw the background, RGB
            // paint.setColor(0xFFD0EFCC);
            paint.setColor(Color.LTGRAY);
            paint.setStyle(Paint.Style.FILL);
            c.drawRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, paint);

            // Roads
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 12; y++) {
                    if (y != 5 && y != 6)
                        c.drawRect(X_OFFSET + RECT_WIDTH / 2 + x * RECT_WIDTH, Y_OFFSET + RECT_HEIGHT + y * RECT_HEIGHT,
                                X_OFFSET + RECT_WIDTH / 2 + (x +1)* RECT_WIDTH, Y_OFFSET + (y+2) * RECT_HEIGHT, paint);
                }
            }
            // '\' AND '/' Roads
            c.drawLine(X_OFFSET + RECT_WIDTH / 2, Y_OFFSET + 2 * RECT_HEIGHT,X_OFFSET + RECT_WIDTH / 2 + 4 * RECT_WIDTH, Y_OFFSET + 6* RECT_HEIGHT, paint);
            c.drawLine(X_OFFSET + RECT_WIDTH / 2, Y_OFFSET + 4 * RECT_HEIGHT,X_OFFSET + RECT_WIDTH / 2 + 2 * RECT_WIDTH, Y_OFFSET + 2* RECT_HEIGHT, paint);
            c.drawLine(X_OFFSET + RECT_WIDTH / 2, Y_OFFSET + 4 * RECT_HEIGHT,X_OFFSET + RECT_WIDTH / 2 + 2 * RECT_WIDTH, Y_OFFSET + 6* RECT_HEIGHT, paint);
            c.drawLine(X_OFFSET + RECT_WIDTH / 2 + 2 * RECT_WIDTH, Y_OFFSET + 2* RECT_HEIGHT,X_OFFSET + RECT_WIDTH / 2 + 4 * RECT_WIDTH, Y_OFFSET + 4* RECT_HEIGHT, paint);
            c.drawLine(X_OFFSET + RECT_WIDTH / 2 + 2 * RECT_WIDTH, Y_OFFSET + 6* RECT_HEIGHT,X_OFFSET + RECT_WIDTH / 2 + 4 * RECT_WIDTH, Y_OFFSET + 4* RECT_HEIGHT, paint);
            c.drawLine(X_OFFSET + RECT_WIDTH / 2, Y_OFFSET + 6* RECT_HEIGHT,X_OFFSET + RECT_WIDTH / 2 + 4 * RECT_WIDTH, Y_OFFSET + 2* RECT_HEIGHT, paint);

            c.drawLine(X_OFFSET + RECT_WIDTH / 2, Y_OFFSET + 8 * RECT_HEIGHT,X_OFFSET + RECT_WIDTH / 2 + 4 * RECT_WIDTH, Y_OFFSET + 12* RECT_HEIGHT, paint);
            c.drawLine(X_OFFSET + RECT_WIDTH / 2, Y_OFFSET + 10 * RECT_HEIGHT,X_OFFSET + RECT_WIDTH / 2 + 2 * RECT_WIDTH, Y_OFFSET + 8* RECT_HEIGHT, paint);
            c.drawLine(X_OFFSET + RECT_WIDTH / 2, Y_OFFSET + 10 * RECT_HEIGHT,X_OFFSET + RECT_WIDTH / 2 + 2 * RECT_WIDTH, Y_OFFSET + 12* RECT_HEIGHT, paint);
            c.drawLine(X_OFFSET + RECT_WIDTH / 2 + 2 * RECT_WIDTH, Y_OFFSET + 8* RECT_HEIGHT,X_OFFSET + RECT_WIDTH / 2 + 4 * RECT_WIDTH, Y_OFFSET + 10* RECT_HEIGHT, paint);
            c.drawLine(X_OFFSET + RECT_WIDTH / 2 + 2 * RECT_WIDTH, Y_OFFSET + 12* RECT_HEIGHT,X_OFFSET + RECT_WIDTH / 2 + 4 * RECT_WIDTH, Y_OFFSET + 10* RECT_HEIGHT, paint);
            c.drawLine(X_OFFSET + RECT_WIDTH / 2, Y_OFFSET + 12* RECT_HEIGHT,X_OFFSET + RECT_WIDTH / 2 + 4 * RECT_WIDTH, Y_OFFSET + 8* RECT_HEIGHT, paint);

            // Railways
            c.drawRect(X_OFFSET + RECT_WIDTH / 2 - 1, Y_OFFSET + RECT_HEIGHT* 2 - 1, X_OFFSET + RECT_WIDTH / 2 + 4 * RECT_WIDTH + 1,Y_OFFSET + 12 * RECT_HEIGHT + 1, paint);
            c.drawRect(X_OFFSET + RECT_WIDTH / 2 + 1, Y_OFFSET + RECT_HEIGHT* 2 + 1, X_OFFSET + RECT_WIDTH / 2 + 4 * RECT_WIDTH - 1,Y_OFFSET + 12 * RECT_HEIGHT - 1, paint);
            c.drawRect(X_OFFSET + RECT_WIDTH / 2 - 1, Y_OFFSET + RECT_HEIGHT* 6 - 1, X_OFFSET + RECT_WIDTH / 2 + 2 * RECT_WIDTH + 1,Y_OFFSET + 8 * RECT_HEIGHT + 1, paint);
            c.drawRect(X_OFFSET + RECT_WIDTH / 2 + 1, Y_OFFSET + RECT_HEIGHT* 6 + 1, X_OFFSET + RECT_WIDTH / 2 + 2 * RECT_WIDTH - 1,Y_OFFSET + 8 * RECT_HEIGHT - 1, paint);
            c.drawRect(X_OFFSET + RECT_WIDTH / 2 + 2 * RECT_WIDTH - 1, Y_OFFSET	+ RECT_HEIGHT * 6 - 1, X_OFFSET + RECT_WIDTH / 2 + 4* RECT_WIDTH + 1, Y_OFFSET + 8 * RECT_HEIGHT+ 1, paint);
            c.drawRect(X_OFFSET + RECT_WIDTH / 2 + 2 * RECT_WIDTH + 1, Y_OFFSET	+ RECT_HEIGHT * 6 + 1, X_OFFSET + RECT_WIDTH / 2 + 4* RECT_WIDTH - 1, Y_OFFSET + 8 * RECT_HEIGHT - 1, paint);
            paint.setColor(Color.GREEN);
            c.drawRect(X_OFFSET + RECT_WIDTH / 2, Y_OFFSET + RECT_HEIGHT * 2,X_OFFSET + RECT_WIDTH / 2 + 4 * RECT_WIDTH, Y_OFFSET + 12* RECT_HEIGHT, paint);
            c.drawRect(X_OFFSET + RECT_WIDTH / 2, Y_OFFSET + RECT_HEIGHT * 6,X_OFFSET + RECT_WIDTH / 2 + 2 * RECT_WIDTH, Y_OFFSET+ 8 * RECT_HEIGHT, paint);
            c.drawRect(X_OFFSET + RECT_WIDTH / 2 + 2 * RECT_WIDTH, Y_OFFSET	+ RECT_HEIGHT * 6, X_OFFSET + RECT_WIDTH / 2 + 4* RECT_WIDTH, Y_OFFSET + 8 * RECT_HEIGHT, paint);

            //Board.STATIONS
            for (int y = 0; y < Board.BOARD_HEIGHT; y++) {
                for (int x = 0; x < Board.BOARD_WIDTH; x++) {
                    int tmpy = y < 6 ? (y+1) : (y+2); //

                    if (board.getStations()[y][x] == Board.HEADQUARTER) {
                        float w = RECT_WIDTH / 2.0f;
                        float h = RECT_HEIGHT / 2.0f;
                        paint.setStyle(Paint.Style.FILL);
                        paint.setColor(Color.BLUE);
                        c.drawRect(X_OFFSET + RECT_WIDTH / 2 + x* RECT_WIDTH - w / 2, Y_OFFSET + tmpy * RECT_HEIGHT - h / 2,
                                X_OFFSET + RECT_WIDTH / 2 + x* RECT_WIDTH + w / 2, Y_OFFSET + tmpy * RECT_HEIGHT + h / 2, paint);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setColor(Color.BLACK);
                        c.drawRect(X_OFFSET + RECT_WIDTH / 2 + x* RECT_WIDTH - w / 2, Y_OFFSET + tmpy * RECT_HEIGHT - h / 2,
                                X_OFFSET + RECT_WIDTH / 2 + x* RECT_WIDTH + w / 2, Y_OFFSET + tmpy * RECT_HEIGHT + h / 2, paint);
                    } else if (board.getStations()[y][x] == Board.CAMP) {
                        float r = RECT_HEIGHT * 3 / 8.0f;
                        paint.setStyle(Paint.Style.FILL);
                        paint.setColor(Color.GRAY);
                        c.drawCircle(X_OFFSET + RECT_WIDTH / 2 + x* RECT_WIDTH, Y_OFFSET + tmpy * RECT_HEIGHT, r, paint);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setColor(Color.BLACK);
                        c.drawCircle(X_OFFSET + RECT_WIDTH / 2 + x* RECT_WIDTH, Y_OFFSET + tmpy * RECT_HEIGHT, r, paint);
                    } else{
                        float w = RECT_WIDTH / 2.0f;
                        float h = RECT_HEIGHT / 2.0f;
                        paint.setStyle(Paint.Style.FILL);
                        paint.setColor(Color.WHITE);
                        c.drawRect(X_OFFSET + RECT_WIDTH / 2 + x* RECT_WIDTH - w / 2, Y_OFFSET + tmpy * RECT_HEIGHT - h / 2,
                                X_OFFSET + RECT_WIDTH / 2 + x* RECT_WIDTH + w / 2, Y_OFFSET + tmpy * RECT_HEIGHT + h / 2, paint);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setColor(Color.BLACK);
                        c.drawRect(X_OFFSET + RECT_WIDTH / 2 + x* RECT_WIDTH - w / 2, Y_OFFSET + tmpy * RECT_HEIGHT - h / 2,
                                X_OFFSET + RECT_WIDTH / 2 + x* RECT_WIDTH + w / 2, Y_OFFSET + tmpy * RECT_HEIGHT + h / 2, paint);
                    }
                }
            }
        }

        @Override
        public void run() {
            while (surfaceReady) {
                Canvas canvas = null;
                try {
                    canvas = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        if (mode == STATE_LINEUP|| mode == STATE_RUNNING ) {
                            updatePhysics();
                        }
                        doDraw(canvas);
                    }
                } finally {
                    if (canvas != null) {
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }

        public void setSurfaceReady(boolean b) {
            this.surfaceReady = b;
        }

        /**
         * Set the game state
         *
         * @param state
         */
        public void setMode(int state) {
            synchronized (mSurfaceHolder) {
                setMode(state, null);
            }
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         *
         * @param mode
         *            one of the STATE_* constants
         * @param message
         *            string to add to screen or null
         */
        public void setMode(int state, CharSequence message) {
			/*
			 * This method optionally can cause a text message to be displayed
			 * to the user when the mode changes. Since the View that actually
			 * renders that text is part of the main View hierarchy and not
			 * owned by this thread, we can't touch the state of that View.
			 * Instead we use a Message + Handler to relay commands to the main
			 * thread, which updates the user-text View.
			 */
            synchronized (mSurfaceHolder) {
                this.mode = state;

                if (state == STATE_RUNNING) {
                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", "");
                    b.putInt("viz", View.INVISIBLE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                } else {
                    Resources res = mContext.getResources();
                    CharSequence str = "";
                    // if (state == STATE_READY)
                    // str = res.getText(R.string.mode_ready);
                    // else if (state == STATE_PAUSE)
                    // str = res.getText(R.string.mode_pause);
                    // else if (state == STATE_LOSE)
                    // str = res.getText(R.string.mode_lose);
                    // else if (state == STATE_LEVELUPDATED)
                    // str = res.getText(R.string.mode_levelupdated);

                    if (state == STATE_LOSE){
                        str = res.getText(R.string.victory);
                    } else if (state == STATE_LOSE){
                        str = res.getText(R.string.failed);
                    }

                    if (message != null) {
                        str = message + "\n" + str;
                    }

                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", str.toString());
                    b.putInt("viz", View.VISIBLE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                }
            }
        }

        public int getMode() {
            synchronized (mSurfaceHolder) {
                return mode;
            }
        }


        public void changeShowTitle(){
            showTitle = !showTitle;
        }

        public boolean isShowTitle() {
            return showTitle;
        }

        public void setShowTitle(boolean showTitle) {
            this.showTitle = showTitle;
        }

        public boolean isShowFlag() {
            return showFlag;
        }

        public void setShowFlag(boolean showFlag) {
            this.showFlag = showFlag;
        }

        /**
         * Random line up from the raw files
         * @return
         */
        private InputStream randomLineup(){
            Resources res = mContext.getResources();
            int r = getRand(1, SAMPLE_LINEUP_MAX_INDEX);
            switch (r) {
                case 1:  return res.openRawResource(R.raw.lineup_1);
                case 2:  return res.openRawResource(R.raw.lineup_2);
                case 3:  return res.openRawResource(R.raw.lineup_3);
                case 4:  return res.openRawResource(R.raw.lineup_4);
                case 5:  return res.openRawResource(R.raw.lineup_5);
                case 6:  return res.openRawResource(R.raw.lineup_6);
                case 7:  return res.openRawResource(R.raw.lineup_7);
                case 8:  return res.openRawResource(R.raw.lineup_8);
                case 9:  return res.openRawResource(R.raw.lineup_9);
                case 10:  return res.openRawResource(R.raw.lineup_10);
                case 11:  return res.openRawResource(R.raw.lineup_11);
                case 12:  return res.openRawResource(R.raw.lineup_12);
                case 13:  return res.openRawResource(R.raw.lineup_13);
                case 14:  return res.openRawResource(R.raw.lineup_14);
                case 15:  return res.openRawResource(R.raw.lineup_15);
                case 16:  return res.openRawResource(R.raw.lineup_16);
                case 17:  return res.openRawResource(R.raw.lineup_17);
                case 18:  return res.openRawResource(R.raw.lineup_18);
                case 19:  return res.openRawResource(R.raw.lineup_19);
                case 20:  return res.openRawResource(R.raw.lineup_20);
                case 21:  return res.openRawResource(R.raw.lineup_21);
                case 22:  return res.openRawResource(R.raw.lineup_22);
                case 23:  return res.openRawResource(R.raw.lineup_23);
                case 24:  return res.openRawResource(R.raw.lineup_24);
                case 25:  return res.openRawResource(R.raw.lineup_25);
                case 26:  return res.openRawResource(R.raw.lineup_26);
                case 27:  return res.openRawResource(R.raw.lineup_27);
                case 28:  return res.openRawResource(R.raw.lineup_28);
                case 29:  return res.openRawResource(R.raw.lineup_29);
                case 30:  return res.openRawResource(R.raw.lineup_30);
                case 31:  return res.openRawResource(R.raw.lineup_31);
                case 32:  return res.openRawResource(R.raw.lineup_32);
                case 33:  return res.openRawResource(R.raw.lineup_33);
                case 34:  return res.openRawResource(R.raw.lineup_34);
                case 35:  return res.openRawResource(R.raw.lineup_35);
                case 36:  return res.openRawResource(R.raw.lineup_36);
                case 37:  return res.openRawResource(R.raw.lineup_37);
                case 38:  return res.openRawResource(R.raw.lineup_38);
                case 39:  return res.openRawResource(R.raw.lineup_39);
                case 40:  return res.openRawResource(R.raw.lineup_40);
                case 41:  return res.openRawResource(R.raw.lineup_41);
                case 42:  return res.openRawResource(R.raw.lineup_42);
                case 43:  return res.openRawResource(R.raw.lineup_43);
                case 44:  return res.openRawResource(R.raw.lineup_44);
                case 45:  return res.openRawResource(R.raw.lineup_45);
                case 46:  return res.openRawResource(R.raw.lineup_46);
                case 47:  return res.openRawResource(R.raw.lineup_47);
                case 48:  return res.openRawResource(R.raw.lineup_48);
                case 49:  return res.openRawResource(R.raw.lineup_49);
                case 50:  return res.openRawResource(R.raw.lineup_50);
                case 51:  return res.openRawResource(R.raw.lineup_51);
                case 52:  return res.openRawResource(R.raw.lineup_52);
                case 53:  return res.openRawResource(R.raw.lineup_53);
                case 54:  return res.openRawResource(R.raw.lineup_54);
                case 55:  return res.openRawResource(R.raw.lineup_55);
                case 56:  return res.openRawResource(R.raw.lineup_56);
                case 57:  return res.openRawResource(R.raw.lineup_57);
                case 58:  return res.openRawResource(R.raw.lineup_58);
                case 59:  return res.openRawResource(R.raw.lineup_59);
                case 60:  return res.openRawResource(R.raw.lineup_60);
                default: return res.openRawResource(R.raw.lineup_1);
            }
        }
    }

    // The width & height of the screen
    private final int SCREEN_WIDTH, SCREEN_HEIGHT;
    // Ratio - RECT_WIDTH/RECT_HEIGHT of the little rectangle.
    // To draw the board fitting to the screen
    private final double RATIO_RECT_W_H = 1.9d;
    private final int RECT_WIDTH, RECT_HEIGHT;
    // Border of the board on the screen
    private final int X_OFFSET, Y_OFFSET;
    private final int PIECE_WIDTH, PIECE_HEIGHT;
    private static final int SAMPLE_LINEUP_MAX_INDEX = 60 ;
    private GameThread gameThread;
    private Board board = new Board();
    private int whosTurn = Pieces.MAN_TAG; // Check who's turn to move
    private MediaPlayer mediaPlayer;

    public LuZhanQiView(Context context, AttributeSet attrs) {
        super(context, attrs);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        // get the size of the screen
        DisplayMetrics dm = context.getApplicationContext().getResources()
                .getDisplayMetrics();
        SCREEN_WIDTH = dm.widthPixels;
        SCREEN_HEIGHT = dm.heightPixels;

        // To fit the screen.
        int littleRectWidth = SCREEN_WIDTH / 5;
        int littleRectHeight = SCREEN_HEIGHT / 14;

        if (littleRectWidth > littleRectHeight * RATIO_RECT_W_H) {
            RECT_WIDTH = (int) (littleRectHeight * RATIO_RECT_W_H);
            RECT_HEIGHT = littleRectHeight;
        } else {
            RECT_WIDTH = littleRectWidth;
            RECT_HEIGHT = (int) (littleRectWidth / RATIO_RECT_W_H);
        }
        // Set the board in the center of the screen
        X_OFFSET = (SCREEN_WIDTH - RECT_WIDTH * 5) / 2;
        Y_OFFSET = (SCREEN_HEIGHT - RECT_HEIGHT * 14) / 2;

        PIECE_HEIGHT = RECT_HEIGHT * 3 / 4;
        PIECE_WIDTH = (int) (PIECE_HEIGHT * RATIO_RECT_W_H - 2);
        Log.d(this.getClass().getName(), "PIECE_HEIGHT = " + PIECE_HEIGHT  +  "PIECE_WIDTH = " + PIECE_WIDTH );

        // create thread only; it's started in surfaceCreated()
        gameThread = new GameThread(holder, context, new Handler() {
            public void handleMessage(Message m) {
                // mStatusText.setVisibility(m.getData().getInt("viz"));
                // mStatusText.setText(m.getData().getString("text"));
            }
        });
        // make sure we get key events
        setFocusable(true);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        gameThread.initGame();
        gameThread.setSurfaceReady(true);
        gameThread.start();

    }

    /* Callback invoked when the surface dimensions change. */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // do nothing currently
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        gameThread.setSurfaceReady(false);
        boolean retry = true;
        while (retry) {
            try {
                gameThread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    /**
     *
     */
    public boolean onTouchEvent(MotionEvent event) {
        float fx = event.getX();
        float fy = event.getY();
        int recordX = -1, recordY = -1; // x,y coordinate on the board from motion event

        if (event.getAction() == MotionEvent.ACTION_DOWN){
            if (gameThread.getMode() == GameThread.STATE_RUNNING) {
                // Convert from motionEvent x,y to the board coordinate
                for (int y = 0; y < Board.BOARD_HEIGHT; y++) {
                    for (int x = 0; x < Board.BOARD_WIDTH; x++) {
                        int tmpy = y < 6 ? (y+1) : (y+2);
                        if (fx >=  X_OFFSET + RECT_WIDTH / 2.0f + x* RECT_WIDTH - PIECE_WIDTH / 2.0f
                                && fx <=  X_OFFSET + RECT_WIDTH / 2.0f + x* RECT_WIDTH + PIECE_WIDTH / 2.0f ){
                            recordX = x;
                        }
                        if (fy >= Y_OFFSET + tmpy * RECT_HEIGHT - PIECE_HEIGHT / 2.0f
                                && fy <= Y_OFFSET + tmpy * RECT_HEIGHT + PIECE_HEIGHT / 2.0f ){
                            recordY = y;
                        }
                    }
                }
                // Log.d(this.getClass().getName(), "Motion Event X,Y coordinate: " + String.valueOf(recordX) + "," + String.valueOf(recordY));

                if (whosTurn == Pieces.MAN_TAG){
                    // Valid x,y coordinate on the board
                    if (recordX != -1 && recordY != -1) {
                        if (board.setFromTo(Pieces.MAN_TAG, new Coordinate(recordX, recordY))){
                            // set the mPath and mMoveAndAttack values
                            board.tryToMove();
                        }
                    }
                }
            }

            // DISPLAY/ UNDISPLAY the Pieces
            if (fx >= SCREEN_WIDTH / 2.0f  - PIECE_WIDTH / 2.0f && fx <=  SCREEN_WIDTH / 2.0f  + PIECE_WIDTH / 2.0f
                    && fy >= SCREEN_HEIGHT / 2.0f  - PIECE_HEIGHT / 2.0f && fy <=  SCREEN_HEIGHT / 2.0f  + PIECE_HEIGHT / 2.0f){
                gameThread.changeShowTitle();
            }

        }
        return true;
    }

    public Board getBoard() {
        return board;
    }

    /**
     * Get a random integer between [pA, pB]
     * @param pA
     * @param pB
     * @return
     */
    public int getRand(int pA, int pB) {
        Random r = new Random();
        return r.nextInt(pB - pA + 1) + pA;
    }
    /**
     * Chage the turn who should move
     */
    public void changeWhosTurn (){
        if (whosTurn == Pieces.MAN_TAG) {
            whosTurn = Pieces.AI_TAG;

            new Thread (new ABSearchRunnable(board)).start();
        } else if (whosTurn == Pieces.AI_TAG) {
            whosTurn = Pieces.MAN_TAG;
        }
    }

    public int getWhosTurn() {
        return whosTurn;
    }

    public void setWhosTurn(int whosTurn) {
        this.whosTurn = whosTurn;
    }
    public GameThread getGameThread() {
        return gameThread;
    }

    public void setGameThread(GameThread gameThread) {
        this.gameThread = gameThread;
    }

}
