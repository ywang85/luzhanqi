package com.example.wyj.luzhanqi.game.ai;

import com.example.wyj.luzhanqi.game.Board;
import com.example.wyj.luzhanqi.game.Coordinate;
import com.example.wyj.luzhanqi.game.Pieces;

import java.util.Vector;

/**
 * Created by wyj on 2018/1/23.
 */

public class ABSearch {
    protected static final int FLAG_VALUE = 10000000;
    protected Board board;
    protected Board boardClone;
    protected Movement bestMove = null;

    public ABSearch() {
    }

    public ABSearch(Board b) {
        this.board = b;
        this.boardClone = (Board) b.clone();
    }

    /**
     * All possible moves
     * @parameter true = NORTH / AI
     */
    public Vector<Movement> possibleMoves(boolean ai) {
        Vector<Movement> moves = new Vector<Movement>();
        for (int j = 0 ; j < Board.BOARD_HEIGHT ; j++) {
            for (int i = 0; i < Board.BOARD_WIDTH; i++) {
                if (ai && (Pieces.getLocated(boardClone.getBoardArea()[j][i]) == Pieces.AI_TAG) ||
                        !ai	&& (Pieces.getLocated(boardClone.getBoardArea()[j][i]) == Pieces.MAN_TAG)) {
                    for (int tj = Board.BOARD_HEIGHT - 1; tj >=0; tj--) {
                        for (int ti = Board.BOARD_WIDTH -1; ti >=0; ti --){
                            Vector<Coordinate> path = boardClone.pathFinding(i, j, ti, tj);
                            if (path != null && path.size() > 0) {
                                moves.add(new Movement(i, j, ti, tj));
                            }
                        }
                    }
                }
            }
        }
        // Collections.sort(moves);
        // for (int i = 0; i < moves.size(); i++) {
        // logger.debug(moves.get(i));
        // }
        return moves;
    }

    /**
     * Evaluation, @parameter true = NORTH / AI
     */
    public int evaluation(boolean ai) {
        byte[][] stations = boardClone.getStations();
        byte[][] ba = boardClone.getBoardArea();
        int value = 0;
        for (int j = 0; j < Board.BOARD_HEIGHT; j++) {
            for (int i = 0; i < Board.BOARD_WIDTH; i++) {
                // 子力加和分数
                if (ba[j][i] == Pieces.SILING_N) {
                    value += 350;
                } else if (ba[j][i] == Pieces.JUNZHANG_N) {
                    value += 260;
                } else if (ba[j][i] == Pieces.SHIZHANG_N) {
                    value += 170;
                } else if (ba[j][i] == Pieces.LVZHANG_N) {
                    value += 120;
                } else if (ba[j][i] == Pieces.TUANZHANG_N) {
                    value += 90;
                } else if (ba[j][i] == Pieces.YINGZHANG_N) {
                    value += 70;
                } else if (ba[j][i] == Pieces.LIANZHANG_N) {
                    value += 40;
                } else if (ba[j][i] == Pieces.PAIZHANG_N) {
                    value += 20;
                } else if (ba[j][i] == Pieces.GONGBING_N) {
                    value += 60;
                } else if (ba[j][i] == Pieces.BOMB_N) {
                    value += 130;
                } else if (ba[j][i] == Pieces.MINE_N) {
                    value += 39;
                } else if (ba[j][i] == Pieces.FLAG_N) {
                    value += FLAG_VALUE;
                } else if (ba[j][i] == Pieces.SILING_S) {
                    value -= 350;
                } else if (ba[j][i] == Pieces.JUNZHANG_S) {
                    value -= 260;
                } else if (ba[j][i] == Pieces.SHIZHANG_S) {
                    value -= 170;
                } else if (ba[j][i] == Pieces.LVZHANG_S) {
                    value -= 120;
                } else if (ba[j][i] == Pieces.TUANZHANG_S) {
                    value -= 90;
                } else if (ba[j][i] == Pieces.YINGZHANG_S) {
                    value -= 70;
                } else if (ba[j][i] == Pieces.LIANZHANG_S) {
                    value -= 40;
                } else if (ba[j][i] == Pieces.PAIZHANG_S) {
                    value -= 20;
                } else if (ba[j][i] == Pieces.GONGBING_S) {
                    value -= 60;
                } else if (ba[j][i] == Pieces.BOMB_S) {
                    value -= 130;
                } else if (ba[j][i] == Pieces.MINE_S) {
                    value -= 39;
                } else if (ba[j][i] == Pieces.FLAG_S) {
                    value -= FLAG_VALUE;
                }

                // 旗左右的位置是killer招法，这主要弥补搜索深度的不足
                if (ba[j][i] == Pieces.FLAG_S
                        && (Pieces.getLocated(ba[j][i + 1])== Pieces.AI_TAG ||Pieces.getLocated(ba[j][i - 1])== Pieces.AI_TAG)) {
                    value += FLAG_VALUE / 100;
                } else if (ba[j][i] == Pieces.FLAG_N
                        && (Pieces.getLocated(ba[j][i + 1])== Pieces.MAN_TAG ||Pieces.getLocated(ba[j][i - 1])== Pieces.MAN_TAG)) {
                    value -= FLAG_VALUE / 100;
                }

                // 要破三角雷
                if (ba[j][i] == Pieces.FLAG_N
                        && ba[j][i + 1] == Pieces.MINE_N
                        && ba[j][i - 1] == Pieces.MINE_N
                        && ba[j + 1][i] == Pieces.MINE_N) {
                    value += 220;
                } else if (ba[j][i] == Pieces.FLAG_S
                        && ba[j][i + 1] == Pieces.MINE_S
                        && ba[j][i - 1] == Pieces.MINE_S
                        && ba[j - 1][i] == Pieces.MINE_S) {
                    value -= 220;
                }

                // 不要进非旗的大本营
                if (stations[j][i] == Board.HEADQUARTER && Pieces.getLocated(ba[j][i])== Pieces.AI_TAG ) {
                    value -= 100;
                } else if (stations[j][i] == Board.HEADQUARTER && Pieces.getLocated(ba[j][i])== Pieces.MAN_TAG) {
                    value += 100;
                }

                // 占对方旗上的行营，这个位置很重要
                if (ba[j][i] == Pieces.FLAG_S && Pieces.getLocated(ba[j - 2][i])== Pieces.AI_TAG ){
                    value += 25;
                } else if (ba[j][i] == Pieces.FLAG_N && Pieces.getLocated(ba[j + 2][i])== Pieces.MAN_TAG ){
                    value -= 25;
                }

                // 旗上面的位置也很重要
                if (ba[j][i] == Pieces.FLAG_S && Pieces.getLocated(ba[j - 1][i])== Pieces.AI_TAG) {
                    value += 20;
                } else if (ba[j][i] == Pieces.FLAG_N && Pieces.getLocated(ba[j + 1][i])== Pieces.MAN_TAG) {
                    value -= 20;
                }

                // 攻占对方底线加分（鼓励进攻和加强防守）
                if (j == 10 && Pieces.getLocated(ba[j][i])== Pieces.AI_TAG) {
                    value += 8;
                } else if (j == 1 &&  Pieces.getLocated(ba[j][i])== Pieces.MAN_TAG) {
                    value -= 8;
                }

                // 其他行营占分
                if (stations[j][i] == Board.CAMP && Pieces.getLocated(ba[j][i])== Pieces.AI_TAG) {
                    value += 5;
                } else if (stations[j][i] == Board.CAMP && Pieces.getLocated(ba[j][i])== Pieces.MAN_TAG) {
                    value -= 5;
                }
            }
        }

        if (!ai) { // 负值最大搜索。对当前一方而言，如果占优则返回正数，否则返回负数
            value = -value;
        }
        return value;
    }

    /**
     * True if game is over.
     */
    public boolean isGameOver(boolean player) {
        return evaluation(player) > FLAG_VALUE / 2;
    }

    /**
     * Alpha-beta search
     */
    public int alphaBeta(int depth, boolean player, int alpha, int beta) {
        if (depth == 0 || isGameOver(player)) {
            return evaluation(player);
        }
        // Movement best = null;
        Vector<Movement> moves = possibleMoves(player);
        // For each possible move
        for (int i = 0; i < moves.size(); i++) {
            Movement move = moves.get(i);
            byte[][] boardCopy = boardClone.newCopyOfBoard();
            // Make move
            boardClone.makeaMove(move);
            int value = - alphaBeta(depth - 1, !player, -beta, -alpha);
            // Undo the move
            boardClone.recoverBoard(boardCopy);
            if (value >= alpha) {
                alpha = value;
            }
            if (alpha >= beta) {
                break;
            }
        }
        return alpha;
    }
}
