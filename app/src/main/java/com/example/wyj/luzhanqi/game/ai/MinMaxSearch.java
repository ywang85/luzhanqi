package com.example.wyj.luzhanqi.game.ai;

import com.example.wyj.luzhanqi.game.Board;
import com.example.wyj.luzhanqi.game.Chess;
import com.example.wyj.luzhanqi.game.Utils.Point;

import java.util.Vector;

/**
 * Created by wyj on 2018/1/23.
 */

public class MinMaxSearch {
    protected static final int FLAG_VALUE = 10000000;
    protected Board board;
    protected Board cloneBoard;

    public MinMaxSearch(Board b) {
        this.board = b;
        this.cloneBoard = (Board) b.clone();
    }

    public Vector<Movement> possibleMoves(boolean ai) {
        Vector<Movement> moves = new Vector<>();
        for (int y = 0; y < Board.BOARD_ROW; y++) {
            for (int x = 0; x < Board.BOARD_COLUMN; x++) {
                if (!isAjacent(x, y, ai, cloneBoard)) {
                    continue;
                }
                for (int j = Board.BOARD_ROW - 1; j >= 0; j--) {
                    for (int i = Board.BOARD_COLUMN -1; i >= 0; i --){
                        Vector<Point> path = cloneBoard.getPath(x, y, i, j);
                        if (path != null && path.size() > 0) {
                            moves.add(new Movement(x, y, i, j));
                        }
                    }
                }
            }
        }
        return moves;
    }

    private boolean isAjacent(int x, int y, boolean AI, Board board) {
        if ((AI && Chess.getChessLocation(board.getCheckerboard()[y][x]) == Chess.AI) ||
                (!AI && Chess.getChessLocation(board.getCheckerboard()[y][x]) == Chess.PLAYER)) {
            return true;
        }
        return  false;
    }

    public int evaluation(boolean ai) {
        byte[][] stations = cloneBoard.getStations();
        byte[][] ba = cloneBoard.getCheckerboard();
        int value = 0;
        for (int j = 0; j < Board.BOARD_ROW; j++) {
            for (int i = 0; i < Board.BOARD_COLUMN; i++) {
                if (ba[j][i] == Chess.AIFieldM) {
                    value += 350;
                } else if (ba[j][i] == Chess.AIGeneral) {
                    value += 260;
                } else if (ba[j][i] == Chess.AIMajorG) {
                    value += 170;
                } else if (ba[j][i] == Chess.AIBrigadierG) {
                    value += 120;
                } else if (ba[j][i] == Chess.AIColonel) {
                    value += 90;
                } else if (ba[j][i] == Chess.AIMajor) {
                    value += 70;
                } else if (ba[j][i] == Chess.AICaptain) {
                    value += 40;
                } else if (ba[j][i] == Chess.AILieutenant) {
                    value += 20;
                } else if (ba[j][i] == Chess.AIEngineer) {
                    value += 60;
                } else if (ba[j][i] == Chess.AIBomb) {
                    value += 130;
                } else if (ba[j][i] == Chess.AIMine) {
                    value += 39;
                } else if (ba[j][i] == Chess.AIFlag) {
                    value += FLAG_VALUE;
                } else if (ba[j][i] == Chess.playerFieldM) {
                    value -= 350;
                } else if (ba[j][i] == Chess.playerGeneral) {
                    value -= 260;
                } else if (ba[j][i] == Chess.playerMajorG) {
                    value -= 170;
                } else if (ba[j][i] == Chess.playerBrigadierG) {
                    value -= 120;
                } else if (ba[j][i] == Chess.playerColonel) {
                    value -= 90;
                } else if (ba[j][i] == Chess.playerMajor) {
                    value -= 70;
                } else if (ba[j][i] == Chess.playerCaptain) {
                    value -= 40;
                } else if (ba[j][i] == Chess.playerLieutenant) {
                    value -= 20;
                } else if (ba[j][i] == Chess.playerEngineer) {
                    value -= 60;
                } else if (ba[j][i] == Chess.playerBomb) {
                    value -= 130;
                } else if (ba[j][i] == Chess.playerMine) {
                    value -= 39;
                } else if (ba[j][i] == Chess.playerFlag) {
                    value -= FLAG_VALUE;
                }

                // if chess is on the left or right of player's flag
                if (ba[j][i] == Chess.playerFlag && (Chess.getChessLocation(ba[j][i + 1]) == Chess.AI
                                                    || Chess.getChessLocation(ba[j][i - 1]) == Chess.AI)) {
                        value += 100000;
                } else if (ba[j][i] == Chess.AIFlag && (Chess.getChessLocation(ba[j][i + 1]) == Chess.PLAYER
                                                    || Chess.getChessLocation(ba[j][i - 1]) == Chess.PLAYER)) {
                    value -= 100000;
                }

                // if player's flag is surround by landmines
                if (ba[j][i] == Chess.AIFlag && ba[j][i + 1] == Chess.AIMine &&
                        ba[j][i - 1] == Chess.AIMine && ba[j + 1][i] == Chess.AIMine) {
                    value += 220;
                } else if (ba[j][i] == Chess.playerFlag && ba[j][i + 1] == Chess.playerMine
                        && ba[j][i - 1] == Chess.playerMine && ba[j - 1][i] == Chess.playerMine) {
                    value -= 220;
                }

                // if the headquarter is not flag, not recommend
                if (stations[j][i] == Board.HEADQUARTER && Chess.getChessLocation(ba[j][i])== Chess.AI) {
                    value -= 100;
                } else if (stations[j][i] == Board.HEADQUARTER && Chess.getChessLocation(ba[j][i])== Chess.PLAYER) {
                    value += 100;
                }

                // The camp on the top of player's flag
                if (ba[j][i] == Chess.playerFlag && Chess.getChessLocation(ba[j - 2][i])== Chess.AI){
                    value += 150;
                } else if (ba[j][i] == Chess.AIFlag && Chess.getChessLocation(ba[j + 2][i])== Chess.PLAYER){
                    value -= 150;
                }

                // The position on the top of player's flag
                if (ba[j][i] == Chess.playerFlag && Chess.getChessLocation(ba[j - 1][i])== Chess.AI) {
                    value += 200;
                } else if (ba[j][i] == Chess.AIFlag && Chess.getChessLocation(ba[j + 1][i])== Chess.PLAYER) {
                    value -= 200;
                }

                // try to reach the end of the board to attack the flag
                if (j == 10 && Chess.getChessLocation(ba[j][i])== Chess.AI) {
                    value += 8;
                } else if (j == 1 &&  Chess.getChessLocation(ba[j][i])== Chess.PLAYER) {
                    value -= 8;
                }

                // try to occupy the camp
                if (stations[j][i] == Board.CAMP && Chess.getChessLocation(ba[j][i])== Chess.AI) {
                    value += 50;
                } else if (stations[j][i] == Board.CAMP && Chess.getChessLocation(ba[j][i])== Chess.PLAYER) {
                    value -= 50;
                }
            }
        }

        if (!ai) {
            value = -value;
        }
        return value;
    }


    public int minimax(int depth, boolean player, int alpha, int beta) {
        if (depth == 0 ) {
            return evaluation(player);
        }
        Vector<Movement> moves = possibleMoves(player);
        if (player) {
            int bestval = Integer.MIN_VALUE;
            for (int i = 0; i < moves.size(); i++) {
                Movement move = moves.get(i);
                byte[][] boardCopy = cloneBoard.CloneBoard();
                cloneBoard.takeAction(move);
                int val = -minimax(depth - 1, false, alpha, beta);
                cloneBoard.undo(boardCopy);
                bestval = Math.max(bestval, val);
                alpha = Math.max(alpha, bestval);
                if (beta <= alpha) {
                    break;
                }
            }
            return bestval;
        } else {
            int bestval = Integer.MAX_VALUE;
            for (int i = 0; i < moves.size(); i++) {
                Movement move = moves.get(i);
                byte[][] boardCopy = cloneBoard.CloneBoard();
                cloneBoard.takeAction(move);
                int val = minimax(depth - 1, true, alpha, beta);
                cloneBoard.undo(boardCopy);
                bestval = Math.min(bestval, val);
                beta = Math.min(beta, bestval);
                if (beta <= alpha) {
                    break;
                }
            }
            return bestval;
        }
    }
}
