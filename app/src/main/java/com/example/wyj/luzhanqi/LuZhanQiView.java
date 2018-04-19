package com.example.wyj.luzhanqi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.example.wyj.luzhanqi.game.Board;
import com.example.wyj.luzhanqi.game.Chess;
import com.example.wyj.luzhanqi.game.Utils.Point;
import com.example.wyj.luzhanqi.game.Utils.Pair;
import com.example.wyj.luzhanqi.game.ai.MinMaxThread;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by wyj on 2018/1/23.
 */

public class LuZhanQiView extends SurfaceView implements SurfaceHolder.Callback, GestureDetector.OnGestureListener {

    public static final int XPOS = 0;
    public static final int YPOS = 100;
    public static Map<String, Pair> order = new HashMap<>();
    private GestureDetectorCompat mDetector;

    public static int youKilled = 0;
    public static int aiKilled = 0;
    public static final String RESULTS = "RESULTS";
    public boolean win = false;

    // screen width, height
    private int SCREEN_WIDTH, SCREEN_HEIGHT;

    private final double GRID_RATIO = 1.9d;
    private final int GRID_WIDTH, GRID_HEIGHT;
    // Border of the board on the screen
    private final int X_OFFSET, Y_OFFSET;
    private final int CHESS_WIDTH, CHESS_HEIGHT;
    public static final int MAXCOUNT = 15;
    private GameThread gameThread;
    private Board board = new Board();
    public static int turn = Chess.PLAYER;
    private MediaPlayer mediaPlayer;

    @SuppressLint("HandlerLeak")
    public LuZhanQiView(final Context context, AttributeSet attrs) {
        super(context, attrs);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        setScreenSize(context);

        GRID_WIDTH = SCREEN_WIDTH / 5;
        GRID_HEIGHT = (int) (SCREEN_WIDTH / 5 / GRID_RATIO);

        // Set the board in the center of the screen
        X_OFFSET = (SCREEN_WIDTH - GRID_WIDTH * 5) / 2;
        Y_OFFSET = (SCREEN_HEIGHT - GRID_HEIGHT * 14) / 2;

        CHESS_HEIGHT = GRID_HEIGHT * 3 / 4;
        CHESS_WIDTH = (int) (CHESS_HEIGHT * GRID_RATIO - 2);

        gameThread = new GameThread(this, holder, context, new Handler() {
            public void handleMessage(Message m) {
                switch (m.what){
                    case GameThread.STATE_WIN:
                        win = true;
                        gameThread.finishGame();
                        break;
                    case GameThread.STATE_LOSE:
                        win = false;
                        gameThread.finishGame();
                        break;
                    default:
                        break;
                }
            }
        });
        setFocusable(true);
    }

    private void setScreenSize(Context context) {
        DisplayMetrics dm = context.getApplicationContext().getResources()
                .getDisplayMetrics();
        SCREEN_WIDTH = dm.widthPixels;
        SCREEN_HEIGHT = dm.heightPixels;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        gameThread.init();
        gameThread.setReady(true);
        gameThread.start();
        mDetector = new GestureDetectorCompat(getContext(), this);
        order.put("Engin", new Pair("Engineer", 1));
        order.put("Lieut", new Pair("Lieutenant", 2));
        order.put("Capta", new Pair("Captain", 3));
        order.put("Major", new Pair("Major", 4));
        order.put("Colon", new Pair("Colonel", 5));
        order.put("Briga", new Pair("Brigadier", 6));
        order.put("M Gen", new Pair("Major General", 7));
        order.put("Gener", new Pair("General", 8));
        order.put("F Mar", new Pair("Field Marshal", 9));
        order.put("Bomb", new Pair("Bomb", -1));
        order.put("LandM", new Pair("LandMine", -1));
        order.put("Flag", new Pair("Flag", -1));
    }

    /* Callback invoked when the surface dimensions change. */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        gameThread.setReady(false);
        boolean flag = true;
        while (flag) {
            try {
                gameThread.join();
                flag = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    @Override
    public boolean onDown(MotionEvent event) {
        float fx = event.getX();
        float fy = event.getY();

        if (gameThread.getMode() == GameThread.RUNNING) {
            int[] xy = getXyCoordinate(fx, fy);
            if (turn == Chess.PLAYER) {
                if (xy[0] != -1 && xy[1] != -1) {
                    if (board.clickAndMove(Chess.PLAYER, new Point(xy[0], xy[1]))){
                        board.nextActionType();
                    }
                }
            }
        }
        return true;
    }

    private int[] getXyCoordinate(float fx, float fy) {
        int lastX = -1, lastY = -1;
        for (int y = 0; y < Board.BOARD_ROW; y++) {
            for (int x = 0; x < Board.BOARD_COLUMN; x++) {
                int tmp;
                if (y < 6) {
                    tmp = y + 1;
                } else {
                    tmp = y + 2;
                }
                if (fx >=  X_OFFSET + GRID_WIDTH / 2.0 + x * GRID_WIDTH - CHESS_WIDTH / 2.0
                        && fx <= X_OFFSET + GRID_WIDTH / 2.0 + x * GRID_WIDTH + CHESS_WIDTH / 2.0) {
                    lastX = x;
                }
                if (fy >= Y_OFFSET + tmp * GRID_HEIGHT - CHESS_HEIGHT / 2.0
                        && fy <= Y_OFFSET + tmp * GRID_HEIGHT + CHESS_HEIGHT / 2.0){
                    lastY = y;
                }
            }
        }
        return new int[]{lastX, lastY};
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
    public void onLongPress(MotionEvent event) {
        float fx = event.getX();
        float fy = event.getY();

        int[] xy = getXyCoordinate(fx, fy);

        if (turn == Chess.PLAYER) {
            if (xy[0] != -1 && xy[1] != -1) {
                byte currChess = board.getCheckerboard()[xy[1]][xy[0]];
                String name = Chess.pieceTitle(currChess);
                if (!name.substring(0,2).equals("AI")) {
                    Pair p = order.get(name);
                    String title = p.name;
                    int level = p.order;
                    Toast toast = Toast.makeText(getContext(), title + " " + level, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, XPOS, YPOS);
                    toast.show();
                }
            }
        }
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    public int getRand(int a) {
        Random r = new Random();
        return r.nextInt( a + 1);
    }

    public void changeTurn(){
        if (turn == Chess.PLAYER) {
            turn = Chess.AI;
            new Thread (new MinMaxThread(board)).start();
        } else if (turn == Chess.AI) {
            turn = Chess.PLAYER;
        }
    }

    public static int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public GameThread getGameThread() {
        return gameThread;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public Board getBoard() {
        return board;
    }

    public int getX_OFFSET() {
        return X_OFFSET;
    }

    public int getY_OFFSET() {
        return Y_OFFSET;
    }

    public int getGRID_WIDTH() {
        return GRID_WIDTH;
    }

    public int getGRID_HEIGHT() {
        return GRID_HEIGHT;
    }

    public int getCHESS_WIDTH() {
        return CHESS_WIDTH;
    }

    public int getCHESS_HEIGHT() {
        return CHESS_HEIGHT;
    }

    public int getSCREEN_WIDTH() {
        return SCREEN_WIDTH;
    }

    public int getSCREEN_HEIGHT() {
        return SCREEN_HEIGHT;
    }

}



