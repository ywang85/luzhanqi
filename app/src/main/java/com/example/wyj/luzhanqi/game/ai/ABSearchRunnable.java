package com.example.wyj.luzhanqi.game.ai;

import android.util.Log;

import com.example.wyj.luzhanqi.game.Board;
import com.example.wyj.luzhanqi.game.Pieces;

import java.util.Collections;
import java.util.Random;
import java.util.Vector;

/**
 * Created by wyj on 2018/1/23.
 */

public class ABSearchRunnable extends ABSearch implements Runnable{
    // 计算机计算时间，由于是递归调用，因此有时会超出这个时间
    private static final int COMPUTER_THINKING_TIME = 3000;
    // 计算机set from & to 的时间间隔
    private static final int COMPUTER_SELECTION_TIME = 240;
    private Random rdm = new Random();

    public ABSearchRunnable(){
        super();
    }
    public ABSearchRunnable(Board b){
        super(b);
    }

    @Override
    public void run() {
        int rint = rdm.nextInt(100);
        int depth = 0;
        if (rint < 35) {
            depth = 0;	// 35%
        } else if (rint < 80) {
            depth = 1;	// 45%
        } else {
            depth = 2;	// 20%
        }
        Log.d(this.getClass().getName(), "Search depth:" + String.valueOf(depth));

        long startThinking = System.currentTimeMillis();
        byte[][] boardCopy = boardClone.newCopyOfBoard();
        Vector<Movement> moves = possibleMoves(true);
        int[] values = new int[moves.size()];

        // 先做一下局面评估
        for (int i = 0; i < moves.size(); i++) {
            Movement mm = moves.get(i);
            values[i] = Integer.MAX_VALUE;
            boardClone.makeaMove(mm);
            mm.setValue(evaluation(true));
            boardClone.recoverBoard(boardCopy);
        }

        // 给Possible Move按value从大到小排序
        Collections.sort(moves);
        for (int i = 0; i < moves.size(); i++) {
            boardClone.makeaMove(moves.get(i));
            values[i] = alphaBeta(depth, false, Integer.MIN_VALUE,
                    Integer.MAX_VALUE);
            boardClone.recoverBoard(boardCopy);

            // Check the search time. The computer can think 2 seconds at most
            long endThinking = System.currentTimeMillis();
            if (endThinking - startThinking > COMPUTER_THINKING_TIME) {
                break;
            }
        }

        int chosen = 0;	// 电脑选择的步骤
        int value = Integer.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (values[i] < value) {
                chosen = i; // 电脑选择对Human来说估值最小的move
                value = values[i];
            }
        }

        // 电脑采取行动
        board.clearFromTo ();
        board.setFromTo(Pieces.AI_TAG, moves.get(chosen).getStart());
        try {
            Thread.sleep(COMPUTER_SELECTION_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (board.setFromTo(Pieces.AI_TAG, moves.get(chosen).getEnd())){
            board.tryToMove();
        }
    }
}
