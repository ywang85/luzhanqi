package com.example.wyj.luzhanqi.game.ai;

import com.example.wyj.luzhanqi.game.Board;
import com.example.wyj.luzhanqi.game.Chess;

import java.util.Collections;
import java.util.Vector;

/**
 * Created by wyj on 2018/1/23.
 */

public class MinMaxThread extends MinMaxSearch implements Runnable{
    private static final int MAX_RECURSION_TIME = 3000;
    private static final int MAX_DECISION_TIME = 240;

    public MinMaxThread(Board board){
        super(board);
    }

    @Override
    public void run() {
        int depth = 0;
        long startThinking = System.currentTimeMillis();
        byte[][] boardCopy = cloneBoard.CloneBoard();
        Vector<Movement> moves = possibleMoves(true);
        int[] values = new int[moves.size()];

        computerJudge(moves, values, boardCopy);
        Collections.sort(moves);

        computerThinking(moves, values, depth, startThinking, boardCopy);
        int selection = computerSelection(values);
        computerAction(moves, selection);
    }

    private void computerJudge(Vector<Movement> movements, int[] values, byte[][] boardCopy) {
        for (int i = 0; i < movements.size(); i++) {
            Movement movement = movements.get(i);
            values[i] = Integer.MAX_VALUE;
            cloneBoard.takeAction(movement);
            movement.setValue(evaluation(true));
            cloneBoard.undo(boardCopy);
        }
    }

    private void computerThinking(Vector<Movement> movements, int[] values, int depth, long startThinking, byte[][] boardCopy) {
        for (int i = 0; i < movements.size(); i++) {
            // dfs
            cloneBoard.takeAction(movements.get(i));
            values[i] = minimax(depth, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
            cloneBoard.undo(boardCopy);

            long endThinking = System.currentTimeMillis();
            if (endThinking - startThinking > MAX_RECURSION_TIME) {
                break;
            }
        }
    }

    private int computerSelection(int[] values) {
        int selection = 0;
        int value = Integer.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (values[i] < value) {
                value = values[i];
                selection = i;
            }
        }
        return selection;
    }

    private void computerAction(Vector<Movement> movements, int selection) {
        board.resetTouchStartEnd();
        board.clickAndMove(Chess.AI, movements.get(selection).getStart());
        try {
            Thread.sleep(MAX_DECISION_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (board.clickAndMove(Chess.AI, movements.get(selection).getEnd())){
            board.nextActionType();
        }
    }
}
