package com.example.wyj.luzhanqi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.example.wyj.luzhanqi.game.Board;
import com.example.wyj.luzhanqi.game.Chess;
import com.example.wyj.luzhanqi.game.Point;

import java.io.InputStream;

/**
 * Created by wyj on 2018/4/11.
 */
class GameThread extends Thread {
    public static final int READY = 0;

    public static final int RUNNING = 1;

    public static final int STATE_LOSE = 2;

    public static final int STATE_WIN = 3;

    private static final int TIME_ELAPSED = 100;

    private LuZhanQiView luZhanQiView;

    private boolean isReady;

    private int mode;

    private SurfaceHolder surfaceHolder;

    private Handler mHandler;

    private Context mContext;

    private Paint paint;

    private Paint fontPainter;

    private float mFontHeight;

    private InputStream playerLineup, aiLineup;

    private long mLastTime;

    private boolean isTitleShown = false;

    private boolean isFlagShown = false;

    private Canvas canvas;

    public GameThread(LuZhanQiView luZhanQiView, SurfaceHolder surfaceHolder, Context context, Handler handler) {
        this.luZhanQiView = luZhanQiView;
        this.surfaceHolder = surfaceHolder;
        mContext = context;
        mHandler = handler;
        paint = new Paint();
        initPaint();
    }

    private void initPaint() {
        fontPainter = new Paint();
        Typeface font = Typeface.create("bold", Typeface.BOLD);
        fontPainter.setTypeface(font);
        fontPainter.setColor(Color.WHITE);
        fontPainter.setTextSize(30);

        Paint.FontMetrics fontMetrics = fontPainter.getFontMetrics();
        mFontHeight = fontMetrics.descent - 4 - fontMetrics.top;
    }

    public void finishGame() {
        Activity activity = (Activity) luZhanQiView.getContext();
        Intent intent = new Intent(activity, ResultActivity.class);
        intent.putExtra(LuZhanQiView.RESULTS, luZhanQiView.win);
        activity.startActivity(intent);
    }

    private void playSound(final int soundID) {
        new Thread() {
            public void run() {
                luZhanQiView.setMediaPlayer(MediaPlayer.create(mContext, soundID));
                luZhanQiView.getMediaPlayer().start();
            }
        }.start();
    }

    public void init() {
        luZhanQiView.getBoard().initBoard();
        playerLineup = randomLineup();
        aiLineup = randomLineup();

        if (playerLineup != null) {
            luZhanQiView.getBoard().loadChess(Chess.PLAYER, playerLineup);
        }
        if (aiLineup != null) {
            luZhanQiView.getBoard().loadChess(Chess.AI, aiLineup);
        }
        luZhanQiView.setTurn(Chess.PLAYER);
        setFlagShown(false);
        setMode(READY);
    }

    public void startGame() {
        if (mode == READY) {
            setMode(RUNNING);
            playSound(R.raw.gamestart);
        }
    }


    public void restartGame() {
        init();
        playSound(R.raw.gamestart);
    }

    private void updatePhysics() {
        long now = System.currentTimeMillis();
        if (mLastTime > now) {
            return;
        }
        long elapsed = now - mLastTime;

        if (elapsed > TIME_ELAPSED) {
            if (luZhanQiView.getBoard().hasNextStep()) {
                boolean hasNext = luZhanQiView.getBoard().nextStep();
                if (!hasNext) { // the last Step
                    int result = luZhanQiView.getBoard().getActionType();
                    switch (result) {
                        case Board.AI_LOSE:
                            playSound(R.raw.win);
                            setMode(STATE_WIN);
                            break;
                        case Board.PLAYER_LOSE:
                            playSound(R.raw.lose);
                            setMode(STATE_LOSE);
                            break;
                        case Board.RANK_HIGHER:
                            if (LuZhanQiView.turn == Chess.AI) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast toast = Toast.makeText(luZhanQiView.getContext(),
                                                Board.chessName + " has been killed",
                                                Toast.LENGTH_SHORT);
                                        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                                                luZhanQiView.XPOS, luZhanQiView.YPOS);
                                        toast.show();
                                    }
                                });
                                LuZhanQiView.aiKilled++;
                            } else {
                                LuZhanQiView.youKilled++;
                            }
                            checkShowFlag();
                            playSound(R.raw.rank_higher);
                            break;
                        case Board.RANK_LOWER:
                            if (LuZhanQiView.turn == Chess.AI) {
                                LuZhanQiView.youKilled++;
                            } else {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast toast = Toast.makeText(luZhanQiView.getContext(),
                                                Board.chessName + " has been killed",
                                                Toast.LENGTH_SHORT);
                                        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                                                luZhanQiView.XPOS, luZhanQiView.YPOS);
                                        toast.show();
                                    }
                                });
                                LuZhanQiView.aiKilled++;
                            }
                            checkShowFlag();
                            playSound(R.raw.rank_lower);
                            break;
                        case Board.RANK_SAME:
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast toast = Toast.makeText(luZhanQiView.getContext(),
                                            "bomb or " + Board.chessName,
                                            Toast.LENGTH_SHORT);
                                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                                            luZhanQiView.XPOS, luZhanQiView.YPOS);
                                    toast.show();
                                }
                            });
                            LuZhanQiView.youKilled++;
                            LuZhanQiView.aiKilled++;
                            checkShowFlag();
                            playSound(R.raw.rank_equal);
                            break;
                        case Board.MOVE:
                            playSound(R.raw.move);
                            break;
                    }
                    luZhanQiView.changeTurn();
                }
            }
            mLastTime = now;
        }
    }

    private void checkShowFlag() {
        if (isFlagShown) {
            return;
        }

        byte[][] ba = luZhanQiView.getBoard().getCheckerboard();
        for (int j = 0; j < Board.BOARD_ROW; j++) {
            for (int i = 0; i < Board.BOARD_COLUMN; i++) {
                if (ba[j][i] == Chess.AIFieldM) {
                    return;
                }
            }
        }
        // if Field M is not on the board
        isFlagShown = true;

    }

    private void startDraw(Canvas canvas) {
        drawBoard(canvas);
        drawChess(canvas);
        drawStepMove(canvas);
        drawSelectionBox(canvas);
    }

    // draw two grid to show previous and current position
    private void drawStepMove(Canvas canvas) {
        Point prevStep = luZhanQiView.getBoard().getStart();
        Point currStep = luZhanQiView.getBoard().getEnd();

        float rangeW = luZhanQiView.getCHESS_WIDTH() / 2;
        float rangeH = luZhanQiView.getCHESS_HEIGHT() / 2;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        paint.setColor(Color.RED);

        if (prevStep != null) {
            int tmp = prevStep.y < 6 ? (prevStep.y + 1) : (prevStep.y + 2);
            float x = luZhanQiView.getX_OFFSET() + luZhanQiView.getGRID_WIDTH() / 2 + prevStep.x * luZhanQiView.getGRID_WIDTH();
            float y = luZhanQiView.getY_OFFSET() + tmp * luZhanQiView.getGRID_HEIGHT();
            canvas.drawRect(x - rangeW - 1, y - rangeH - 1, x + rangeW + 2, y + rangeH + 2, paint);
        }

        if (currStep != null) {
            int tmp = currStep.y < 6 ? (currStep.y + 1) : (currStep.y + 2);
            float x = luZhanQiView.getX_OFFSET() + luZhanQiView.getGRID_WIDTH() / 2 + currStep.x * luZhanQiView.getGRID_WIDTH();
            float y = luZhanQiView.getY_OFFSET() + tmp * luZhanQiView.getGRID_HEIGHT();
            canvas.drawRect(x - rangeW - 1,  y - rangeH - 1, x + rangeW + 2, y + rangeH + 2, paint);
        }
    }

    // highlight the chess you select
    private void drawSelectionBox(Canvas canvas) {
        Point touchStart = luZhanQiView.getBoard().getTouch_start();
        Point touchEnd = luZhanQiView.getBoard().getTouch_end();

        float rangeW = luZhanQiView.getCHESS_WIDTH() / 2;
        float rangeH = luZhanQiView.getCHESS_HEIGHT() / 2;

        paint.setColor(Color.WHITE);
        if (touchStart != null) {
            int tmp = touchStart.y < 6 ? (touchStart.y + 1) : (touchStart.y + 2);
            float x = luZhanQiView.getX_OFFSET() + luZhanQiView.getGRID_WIDTH() / 2 + touchStart.x * luZhanQiView.getGRID_WIDTH();
            float y = luZhanQiView.getY_OFFSET() + tmp * luZhanQiView.getGRID_HEIGHT();
            canvas.drawRect(x - rangeW - 1,  y - rangeH - 1,
                    x + rangeW + 2, y + rangeH + 2, paint);
        }
        if (touchEnd != null) {
            int tmp = touchEnd.y < 6 ? (touchEnd.y + 1) : (touchEnd.y + 2);
            float x = luZhanQiView.getX_OFFSET() + luZhanQiView.getGRID_WIDTH() / 2 + touchEnd.x * luZhanQiView.getGRID_WIDTH();
            float y = luZhanQiView.getY_OFFSET() + tmp * luZhanQiView.getGRID_HEIGHT();
            canvas.drawRect(x - rangeW - 1,  y - rangeH - 1,
                    x + rangeW + 2, y + rangeH + 2, paint);
        }
        paint.setStrokeWidth(1);
    }

    private void drawChess(Canvas canvas) {
        byte[][] ba = luZhanQiView.getBoard().getCheckerboard();

        for (int y = 0; y < Board.BOARD_ROW; y++) {
            for (int x = 0; x < Board.BOARD_COLUMN; x++) {
                int tmpy = y < 6 ? (y + 1) : (y + 2);
                if (ba[y][x] != Board.EMPTY) {
                    String title = Chess.pieceTitle(ba[y][x]);
                    int located = Chess.getChessLocation(ba[y][x]);

                    paint.setStyle(Paint.Style.FILL);
                    // player is blue, AI is orange
                    if (located == Chess.PLAYER) {
                        paint.setColor(Color.parseColor("#005B90"));
                    } else {
                        paint.setColor(Color.parseColor("#FE8743"));
                    }
                    // flag is red
                    if (ba[y][x] == Chess.playerFlag || ba[y][x] == Chess.AIFlag && (isTitleShown || isFlagShown)) {
                        paint.setColor(Color.parseColor("#FF0000"));
                    }
                    // Draw the Piece
                    float dx = luZhanQiView.getX_OFFSET() + luZhanQiView.getGRID_WIDTH() / 2 + x * luZhanQiView.getGRID_WIDTH();
                    float dy = luZhanQiView.getY_OFFSET() + tmpy * luZhanQiView.getGRID_HEIGHT();

                    float rangeW = luZhanQiView.getCHESS_WIDTH() / 2;
                    float rangeH = luZhanQiView.getCHESS_HEIGHT() / 2;

                    canvas.drawRect( dx - rangeW,  dy - rangeH,
                            dx + rangeW, dy + rangeH, paint);

                    // Draw the text
                    if (located == Chess.PLAYER || located == Chess.AI && isTitleShown
                            || located == Chess.AI && isFlagShown && ba[y][x] == Chess.AIFlag) {
                        float mTextPosx = dx - rangeW / 2;
                        float locy = dy - rangeH;
                        float mTextPosy = locy + rangeH + mFontHeight / 2.0f;
                        canvas.drawText(title, mTextPosx, mTextPosy, fontPainter);
                    }
                }
            }
        }
    }

    private void drawBoard(Canvas canvas) {
        paint.setColor(Color.parseColor("#F9E79F"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, luZhanQiView.getSCREEN_WIDTH(), luZhanQiView.getSCREEN_HEIGHT(), paint);

        // Roads
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3);

        canvas.drawText("hold chess to get more info", 20, 80, paint);

        paint.setTextSize(40);
        paint.setStyle(Paint.Style.STROKE);

        float xOffSet = luZhanQiView.getX_OFFSET();
        float yOffSet = luZhanQiView.getY_OFFSET();
        float gridW = luZhanQiView.getGRID_WIDTH();
        float gridH = luZhanQiView.getGRID_HEIGHT();

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 12; y++) {
                if (y != 5 && y != 6)
                    canvas.drawRect(xOffSet + gridW / 2 + x * gridW, yOffSet + gridH + y * gridH,
                            xOffSet + gridW / 2 + (x + 1) * gridW, yOffSet + (y + 2) * gridH, paint);
            }
        }

        // \ and /
        canvas.drawLine(xOffSet + gridW / 2, yOffSet + 2 * gridH,
                xOffSet + gridW / 2 + 4 * gridW, yOffSet + 6 * gridH, paint);
        canvas.drawLine(xOffSet + gridW / 2, yOffSet + 4 * gridH,
                xOffSet + gridW / 2 + 2 * gridW, yOffSet + 2 * gridH, paint);
        canvas.drawLine(xOffSet + gridW / 2, yOffSet + 4 * gridH,
                xOffSet + gridW / 2 + 2 * gridW, yOffSet + 6 * gridH, paint);
        canvas.drawLine(xOffSet + gridW / 2 + 2 * gridW, yOffSet + 2 * gridH,
                xOffSet + gridW / 2 + 4 * gridW, yOffSet + 4 * gridH, paint);
        canvas.drawLine(xOffSet + gridW / 2 + 2 * gridW, yOffSet + 6 * gridH ,
                xOffSet + gridW / 2 + 4 * gridW, yOffSet + 4 * gridH, paint);
        canvas.drawLine(xOffSet + gridW / 2, yOffSet + 6 * gridH,
                xOffSet + gridW / 2 + 4 * gridW, yOffSet + 2 * gridH, paint);

        canvas.drawLine(xOffSet + gridW / 2, yOffSet + 2 * gridH + 6 * gridH,
                xOffSet + gridW / 2 + 4 * gridW, yOffSet + 12 * gridH, paint);
        canvas.drawLine(xOffSet + gridW / 2, yOffSet + 2 * gridH + 8 * gridH,
                xOffSet + gridW / 2 + 2 * gridW, yOffSet + 8 * gridH, paint);
        canvas.drawLine(xOffSet + gridW / 2, yOffSet + 2 * gridH + 8 * gridH,
                xOffSet + gridW / 2 + 2 * gridW, yOffSet + 12 * gridH, paint);
        canvas.drawLine(xOffSet + gridW / 2 + 2 * gridW, yOffSet + 2 * gridH + 6 * gridH,
                xOffSet + gridW / 2 + 4 * gridW, yOffSet + 10 * gridH, paint);
        canvas.drawLine(xOffSet + gridW / 2 + 2 * gridW, yOffSet + 2 * gridH + 10 * gridH,
                xOffSet + gridW / 2 + 4 * gridW, yOffSet + 10 * gridH, paint);
        canvas.drawLine(xOffSet + gridW / 2, yOffSet + 2 * gridH + 10 * gridH,
                xOffSet + gridW / 2 + 4 * gridW, yOffSet + 8 * gridH, paint);

        // Railways
        canvas.drawRect(xOffSet + gridW / 2 - 1, yOffSet + gridH * 2 - 1,
                xOffSet + gridW / 2 + 4 * gridW + 1, yOffSet + 12 * gridH + 1, paint);
        canvas.drawRect(xOffSet + gridW / 2 + 1, yOffSet + gridH * 2 + 1,
                xOffSet + gridW / 2 + 4 * gridW - 1, yOffSet + 12 * gridH - 1, paint);
        canvas.drawRect(xOffSet + gridW / 2 - 1, yOffSet + gridH * 6 - 1,
                xOffSet + gridW / 2 + 2 * gridW + 1, yOffSet + 8 * gridH + 1, paint);
        canvas.drawRect(xOffSet + gridW / 2 + 1, yOffSet + gridH * 6 + 1,
                xOffSet + gridW / 2 + 2 * gridW - 1, yOffSet + 8 * gridH - 1, paint);
        canvas.drawRect(xOffSet + gridW / 2 + 2 * gridW - 1, yOffSet + gridH * 6 - 1,
                xOffSet + gridW / 2 + 4 * gridW + 1, yOffSet + 8 * gridH + 1, paint);
        canvas.drawRect(xOffSet + gridW / 2 + 2 * gridW + 1, yOffSet + gridH * 6 + 1,
                xOffSet + gridW / 2 + 4 * gridW - 1, yOffSet + 8 * gridH - 1, paint);

        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(10);
        canvas.drawRect(xOffSet + gridW / 2, yOffSet + gridH * 2,
                xOffSet + gridW / 2 + 4 * gridW, yOffSet + 12 * gridH, paint);
        canvas.drawRect(xOffSet + gridW / 2, yOffSet + gridH * 6,
                xOffSet+ gridW / 2 + 2 * gridW, yOffSet + 8 * gridH, paint);
        canvas.drawRect(xOffSet + gridW / 2 + 2 * gridW, yOffSet + gridH * 6,
                xOffSet + gridW / 2 + 4 * gridW, yOffSet + 8 * gridH, paint);

        //Board.STATIONS
        for (int y = 0; y < Board.BOARD_ROW; y++) {
            for (int x = 0; x < Board.BOARD_COLUMN; x++) {
                int tmpy;
                if (y < 6) {
                    tmpy = y + 1;
                } else {
                    tmpy = y + 2;
                }

                switch (luZhanQiView.getBoard().getStations()[y][x]) {
                    case Board.HEADQUARTER:
                        float w = gridW / 2.0f;
                        float h = gridH / 2.0f;
                        paint.setStyle(Paint.Style.FILL);
                        paint.setColor(Color.BLUE);
                        canvas.drawRect(xOffSet + gridW / 2 + x * gridW - w / 2, yOffSet + tmpy * gridH - h / 2,
                                xOffSet + gridW / 2 + x * gridW + w / 2, yOffSet + tmpy * gridH + h / 2, paint);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setColor(Color.BLACK);
                        canvas.drawRect(xOffSet + gridW / 2 + x * gridW - w / 2, yOffSet + tmpy * gridH - h / 2,
                                xOffSet + gridW / 2 + x * gridW + w / 2, yOffSet + tmpy * gridH + h / 2, paint);
                        break;
                    case Board.CAMP:
                        float r = gridH * 3 / 8.0f;
                        paint.setStyle(Paint.Style.FILL);
                        paint.setColor(Color.GRAY);
                        canvas.drawCircle(xOffSet + gridW / 2 + x * gridW, yOffSet + tmpy * gridH, r, paint);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setColor(Color.BLACK);
                        canvas.drawCircle(xOffSet + gridW / 2 + x * gridW, yOffSet + tmpy * gridH, r, paint);
                        break;
                    default:
                        paint.setStyle(Paint.Style.FILL);
                        paint.setColor(Color.WHITE);
                        canvas.drawRect(xOffSet + gridW / 2.0f + x * gridW - gridW / 2.0f / 2, yOffSet + tmpy * gridH - gridH / 2.0f / 2,
                                xOffSet + gridW / 2.0f + x * gridW + gridW / 2.0f / 2, yOffSet + tmpy * gridH + gridH / 2.0f / 2, paint);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setColor(Color.BLACK);
                        canvas.drawRect(xOffSet + gridW / 2.0f + x * gridW - gridW / 2.0f / 2, yOffSet + tmpy * gridH - gridH / 2.0f / 2,
                                xOffSet + gridW / 2.0f + x * gridW + gridW / 2.0f / 2, yOffSet + tmpy * gridH + gridH / 2.0f / 2, paint);
                        break;
                }
            }
        }
    }

    @Override
    public void run() {
        while (isReady) {
            try {
                canvas = surfaceHolder.lockCanvas(null);
                synchronized (surfaceHolder) {
                    if (mode == STATE_WIN || mode == STATE_LOSE) {
                        isReady = false;
                    }

                    if (mode == READY || mode == RUNNING) {
                        updatePhysics();
                    }
                    startDraw(canvas);

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    public void setReady(boolean ready) {
        this.isReady = ready;
    }

    public void setMode(int mode) {
        synchronized (surfaceHolder) {
            setMode(mode, null);
        }
    }

    public void setMode(final int state, final CharSequence message) {
        synchronized (surfaceHolder) {
            this.mode = state;

            if (state == RUNNING) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Message msg = new Message();
                        msg.what = state;
                        mHandler.sendMessage(msg);
                    }
                }).start();
            } else {
                if (state == STATE_WIN) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Message m = new Message();
                            m.what = state;
                            mHandler.sendMessage(m);
                        }
                    }).start();
                } else if (state == STATE_LOSE) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Message m = new Message();
                            m.what = state;
                            mHandler.sendMessage(m);
                        }
                    }).start();
                }
            }
        }
    }

    public int getMode() {
        synchronized (surfaceHolder) {
            return mode;
        }
    }

    public void flipTitle() {
        isTitleShown = !isTitleShown;
    }


    private void setFlagShown(boolean flagShown) {
        this.isFlagShown = flagShown;
    }

    private int[] lineups = new int[]{R.raw.lineup_1, R.raw.lineup_2, R.raw.lineup_3, R.raw.lineup_4,
                                R.raw.lineup_5, R.raw.lineup_6, R.raw.lineup_7, R.raw.lineup_8,
                                R.raw.lineup_9, R.raw.lineup_10, R.raw.lineup_11, R.raw.lineup_12,
                                R.raw.lineup_13, R.raw.lineup_14, R.raw.lineup_15};

    private InputStream randomLineup() {
        Resources res = mContext.getResources();
        int r = luZhanQiView.getRand(LuZhanQiView.MAXCOUNT);
        return res.openRawResource(lineups[r - 1]);
    }
}
