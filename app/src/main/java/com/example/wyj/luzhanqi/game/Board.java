package com.example.wyj.luzhanqi.game;

import android.util.Log;

import com.example.wyj.luzhanqi.game.ai.Movement;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by wyj on 2018/1/23.
 */

public class Board implements Cloneable{
    // Array Size of the board
    public static final int BOARD_WIDTH = 5;
    public static final int BOARD_HEIGHT = 12;
    public static final byte INVALID_BOARD_TAG = 0x00;
    // STATIONS - 大本营，行营，兵站 （分公路上的和铁路上的）
    public static final byte HEADQUARTER = 0x71;
    public static final byte CAMP = 0x72;
    public static final byte STATION_ROAD = 0x73;
    public static final byte STATION_RAILWAY = 0x74;
    // the default line-up file length: 50 bytes
    public static final int LINEUP_FILE_BYTE_LENGTH = 50;
    // move and attack result
    public static final int INVALID_MOTION = 1000;
    public static final int MOVE = 1001;
    public static final int KILL = 1002;
    public static final int EQUAL = 1003;
    public static final int KILLED = 1004;
    public static final int MAN_LOST = 1005;
    public static final int AI_LOST = 1006;
    // public static final int GAME_OVER = 1006;

    // all the stations
    private final byte[][] stations = new byte[BOARD_HEIGHT][BOARD_WIDTH];
    // Array of the board
    private byte[][] boardArea = new byte[BOARD_HEIGHT][BOARD_WIDTH];
    // it's for A* path finding
    private ArrayList<Coordinate> openList, closedList;
    // For update physics (update board)
    private Vector<Coordinate> mPath = null;
    private Coordinate from = null, to = null; 	// on touch event
    private Coordinate from0 = null, to0 = null; // last from and to, to draw the red tag
    private int curStep = 0;
    private int moveAndAttackResult ;
    private byte mSourcePiece;

    // Constructor
    public Board() {

    }

    /**
     * Initiate the board
     */
    public void initBoard() {
        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                // pieces
                boardArea[y][x] = INVALID_BOARD_TAG;
                // stations
                if ((y == 0 || y == 11) && (x == 1 || x == 3)) {
                    stations[y][x] = HEADQUARTER;
                } else if ((y == 2 || y == 4 || y == 7 || y == 9)
                        && (x == 1 || x == 3) || (y == 3 || y == 8) && x == 2) {
                    stations[y][x] = CAMP;
                } else if (y == 1 || y == 5 || y == 6 || y == 10 || x == 0
                        && y != 0 && y != 11 || x == 4 && y != 0 && y != 11) {
                    stations[y][x] = STATION_RAILWAY;
                } else {
                    stations[y][x] = STATION_ROAD;
                }
            }
        }

        mPath = null;
        from = to = from0 = to0 = null;
        curStep = 0;
        mSourcePiece = INVALID_BOARD_TAG;
        moveAndAttackResult = INVALID_MOTION;
    }

    /**
     * Load all the pieces before the game.
     * @param located, 0 - game player, 1: the computer
     * @param is, reads from such as res/raw/lineup_xxxxx.jql
     */
    public boolean loadPieces (int located, InputStream is) {
        if (is == null) {
            return false;
        }

        byte[] bytes = new byte[LINEUP_FILE_BYTE_LENGTH];
        int x = 0 , y = 0 ;
        try {
            is.read(bytes, 0, LINEUP_FILE_BYTE_LENGTH);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(this.getClass().getName(), "Read Lineup File Error!");
        }

        for (int i = 20; i < bytes.length; i++) {	// ignore the first 20 bytes
            if (bytes[i] != INVALID_BOARD_TAG) {
                if (located == Pieces.MAN_TAG) {
                    x = (i - 20) % 5;
                    y = 6 + (i - 20) / 5;
                    boardArea[y][x] = bytes[i];
                }else if (located == Pieces.AI_TAG) {
                    x = 4 - (i - 20) % 5;
                    y = 5 - (i - 20) / 5;
                    boardArea[y][x] = (byte) ((int) bytes[i] + 0x10);
                }
            }
        }
        return true;
    }

    /**
     * Get a copy of board[][]
     */
    public byte[][] newCopyOfBoard() {
        byte[][] b = new byte[BOARD_HEIGHT][BOARD_WIDTH];
        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                b[y][x] =boardArea[y][x] ;
            }
        }
        return b;
    }

    /**
     * Recover the board
     */
    public void recoverBoard(byte[][] b) {
        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                boardArea[y][x] = b[y][x];
            }
        }
    }

    /**
     * Validate the X, Y on the board array
     * @param x
     * @param y
     * @return
     */
    public boolean validXY ( int x, int y) {
        return x >=0 && x < BOARD_WIDTH && y >=0 && y < BOARD_HEIGHT;
    }

    public boolean validYX ( int y, int x) {
        return validXY(x, y);
    }

    public byte[][] getStations() {
        return stations;
    }

    public byte[][] getBoardArea() {
        return boardArea;
    }

    public void setBoardArea(byte[][] boardArea) {
        this.boardArea = boardArea;
    }

    public void clearFromTo(){
        from = null;
        to = null;
    }

    /**
     * Set the from or to from the onTouchEvent
     *
     * @param c
     */
    public boolean setFromTo(int whosTurn, Coordinate c) {
        if (from == null && whosTurn == Pieces.getLocated(boardArea[c.y][c.x])){
            from = c;
            to = null;
            return true;
        } else if (from == null && whosTurn != Pieces.getLocated(boardArea[c.y][c.x])){
            to = null;
            return false;
        } else if (to == null) {
            int x = from.x, y = from.y;
            int x0= c.x, y0 = c.y;
            // Check if user changes the "from" selection
            if (Pieces.sameLocation( boardArea[y0][x0], boardArea[y][x])){
                from = c;
            } else {
                to = c;
            }
            return true;
        } else {
            // both are not null. It's not allowed to come here
            Log.e(this.getClass().getName(), "// both are not null. It's not allowed to come here!");
            return false;
        }
    }

    /**
     * Try to move after path-finding
     */
    public void tryToMove() {
        if (from == null || to == null) {
            return;
        }
        // path finding
        mPath = pathFinding(from.x, from.y, to.x, to.y);
        if (mPath == null || mPath.size()<1) {
//			Log.d(this.getClass().getName(), "Path is null!");
            // reset from,to and mPath
            from0 = from ;
            to0 = to;
            from = null;
            to = null;
            mPath = null;
            curStep = 0;
            return;
        }
//		Log.d(this.getClass().getName(), "Path is found! Length: " + mPath.size());
        // Move and attack
        moveAndAttackResult = moveAndAttack(from.x, from.y, to.x, to.y);
//		Log.d(this.getClass().getName(), "Move result: " + String.valueOf(moveAndAttackResult));
    }

    /**
     * move and/or attack on the board
     *
     * @return
     */
    private int moveAndAttack (int x0, int y0, int x, int y) {
        // Validation
        if (stations[y0][x0] == INVALID_BOARD_TAG || stations[y][x] == INVALID_BOARD_TAG
                || boardArea[y0][x0] == INVALID_BOARD_TAG || boardArea[y][x] != INVALID_BOARD_TAG
                && Pieces.sameLocation(boardArea[y0][x0], boardArea[y][x])) {
            return INVALID_MOTION;
        }
        // Move only
        if (boardArea[y][x] == INVALID_BOARD_TAG) {
            return MOVE;
        }
        // Attack
        if (Pieces.getPureType(boardArea[y][x]) == Pieces.FLAG_S) {
            int locatedLost = Pieces.getLocated(boardArea[y][x]);
            // One lost
            return locatedLost > 0 ? AI_LOST:MAN_LOST;
        } else if (Pieces.getPureType(boardArea[y0][x0]) == Pieces.BOMB_S
                || Pieces.getPureType(boardArea[y][x]) == Pieces.BOMB_S) {
            // Bomb
            return EQUAL;
        } else if (Pieces.getPureType(boardArea[y][x]) == Pieces.MINE_S) {
            if (Pieces.getPureType(boardArea[y0][x0]) == Pieces.GONGBING_S) {
                // Mine
                return KILL;
            } else {
                return KILLED;
            }
        } else {
            // Soldiers
            byte soldier0 = Pieces.getPureType(boardArea[y0][x0]);
            byte soldier1 = Pieces.getPureType(boardArea[y][x]);
            if (soldier0 < soldier1) {
                return KILL;
            } else if (soldier0 == soldier1) {
                return EQUAL;
            } else {
                return KILLED;
            }
        }
    }

    /**
     * One move on the board; it's for AB search
     * @param move
     */
    public void makeaMove (Movement move) {
        if (move == null){
            return ;
        }
        Coordinate from = move.getStart();
        Coordinate to = move.getEnd();

        int result = moveAndAttack(from.x, from.y, to.x, to.y);

        switch (result) {
            case MOVE:
            case KILL:
            case AI_LOST:
            case MAN_LOST:
                boardArea[to.y][to.x] = boardArea[from.y][from.x];
                boardArea[from.y][from.x] = INVALID_BOARD_TAG;
                break;
            case EQUAL:
                boardArea[to.y][to.x] = INVALID_BOARD_TAG;
                boardArea[from.y][from.x] = INVALID_BOARD_TAG;
                break;
            case KILLED:
                boardArea[from.y][from.x] = INVALID_BOARD_TAG;
                break;
        }


    }


    /**
     * Check if there is next step of the path
     * @return
     */
    public boolean hasNextStep(){
        if (mPath == null || mPath.size()<1) {
            curStep = 0;
            return false;
        }
        return curStep + 1 <= mPath.size();
    }

    public boolean isLastStep(){
        return curStep + 1 == mPath.size();
    }

    /**
     * Refresh the board by moving a step
     * @return false: no more nextSteps
     */
    public boolean nextStep(){
        // remove current step's board byte
        if (curStep <= 0) {
            mSourcePiece = boardArea[from.y][from.x];
            boardArea[from.y][from.x] = INVALID_BOARD_TAG;
        } else {
            Coordinate last = mPath.get(curStep-1);
            boardArea[last.y][last.x] = INVALID_BOARD_TAG;
        }

        // move to a new step
        if (!isLastStep()) {
            Coordinate next = mPath.get(curStep);
            boardArea[next.y][next.x] = mSourcePiece;
            curStep++;
            //Log.d(this.getClass().getName(), "Next step(), y & x: " + String.valueOf(next.y)+","+String.valueOf(next.x));
            return true;
        } else {
            //Log.d(this.getClass().getName(), "Last step(), y & x: " + String.valueOf(to.y)+","+String.valueOf(to.x));
            // to determine the target piece's destiny
            switch (moveAndAttackResult) {
                case MOVE:
                case KILL:
                case AI_LOST:
                case MAN_LOST:
                    boardArea[to.y][to.x] = mSourcePiece;
                    break;
                case EQUAL:
                    boardArea[to.y][to.x] = INVALID_BOARD_TAG;
                    break;
                case KILLED:
                    break;
            }
            // reset from,to and mPath
            from0 = from ;
            to0 = to;
            from = null;
            to = null;
            mPath = null;
            curStep = 0;
            return false;
        }
    }

    /**
     * Path finding
     * @param x0
     * @param y0
     * @param x
     * @param y
     * @return Vector<Coordiante>  doesn't include the source
     */
    public Vector<Coordinate> pathFinding(int x0, int y0, int x, int y) {
        // No piece in x0,y0 or The pieces in HEADQUARTER and the mines can NOT move
        if (boardArea[y0][x0]== INVALID_BOARD_TAG || stations[y0][x0] == HEADQUARTER
                || Pieces.getPureType(boardArea[y0][x0]) == Pieces.MINE_S) {
            return null;
        }
        // Can not move to a camp which is occupied
        if (stations[y][x] == CAMP && boardArea[y][x] != INVALID_BOARD_TAG) {
            return null;
        }
        // Piece -> Piece which are belong to the same player
        if (Pieces.sameLocation(boardArea[y0][x0], boardArea[y][x])) {
            return null;
        }

        Coordinate beginning = new Coordinate(x0, y0);
        Coordinate end = new Coordinate(x, y);
        Vector<Coordinate> path = new Vector<Coordinate>();

        // Road station or Camp ( | - ) , move only 1 step
        if (stations[y0][x0] == STATION_ROAD || stations[y][x] == STATION_ROAD
                || stations[y0][x0] == CAMP || stations[y][x] == CAMP
                || stations[y][x] == HEADQUARTER) {
            if (roadAdjacent(x0, y0, x, y)) {
                path.add(new Coordinate(x, y));
                return path;
            }
        }

        // Camp, move only 1 step (/ \)
        if (stations[y0][x0] == CAMP || stations[y][x] == CAMP) {
            if (campAdjacent(x0, y0, x, y)) {
                path.add(new Coordinate(x, y));
                return path;
            }
        }

        // on Railway, A* path finding
        openList = new ArrayList<Coordinate>();
        closedList = new ArrayList<Coordinate>();

        if (stations[y0][x0] == STATION_RAILWAY	&& stations[y][x] == STATION_RAILWAY &&
                (validRailwayRoad(x0, y0, x, y) || Pieces.getPureType(boardArea[y0][x0]) == Pieces.GONGBING_S)) {
            ArrayList<Coordinate> adjacent = new ArrayList<Coordinate>();
            boolean engineer = Pieces.getPureType(boardArea[y0][x0]) == Pieces.GONGBING_S;
            Coordinate current = null;
            openList.add(beginning);

            do {
                // Find the minimum F value Coordinate from the openList
                current = lookForMinF(end);
                openList.remove(current);
                closedList.add(current);
                // Get all adjacent XYs of current
                adjacent = allAdjacents(beginning, current, end, engineer);
                // logger.debug("All adjacent of current(" + current.value +
                // ") = " + adjacent.size());

                for (Coordinate adj : adjacent) { // Traverse all adjacent of
                    // current Coordinate
                    if (!closedListContains(adj)
                            && (boardArea[adj.y][adj.x] == Pieces.INVALID || adj.equals(end))) {
                        if (!openListContains(adj)) {
                            adj.parent = current;
                            openList.add(adj);
                        } else {
                            if (getCostG(current.parent, current)
                                    + getCostG(current, adj) < getCostG(
                                    current.parent, adj)) {
                                adj.parent = current;
                            }
                        }
                    }
                }
            } while (!openListContains(end) && openList.size() > 0);
            end.parent = current;

            if (openListContains(end)) { // Find the path
                Coordinate t = end;
                while (t != beginning) {
                    path.add(t);
                    t = t.parent;
                }
            }
        }

        // Convert the path array
        for (int i = path.size() - 1; i > (path.size() - 1) / 2; i--) {
            Coordinate tmp = path.get(i);
            path.set(i, path.get(path.size() - 1 - i));
            path.set(path.size() - 1 - i, tmp);
        }
        return path;
    }

    /**
     * Get the adjacent points of current
     */
    private ArrayList<Coordinate> allAdjacents(Coordinate beginning,
                                               Coordinate current, Coordinate end, boolean engineer) {
        ArrayList<Coordinate> adjacent = new ArrayList<Coordinate>();

        if (engineer) {
            for (int i = -1; i <= 1; i += 2) {
                if (validYX(current.y + i, current.x) &&
                        stations[current.y + i][current.x] == STATION_RAILWAY) {
                    if(!((current.y ==5) && (current.x == 1)
                            ||(current.y ==5) && (current.x == 3)
                            ||(current.y ==6) && (current.x == 1)
                            ||(current.y ==6) && (current.x == 3))
                            ){
                        adjacent.add(new Coordinate(current.x, current.y + i));
                    }
                }
                if (validYX(current.y , current.x+i) &&
                        stations[current.y][current.x + i] == STATION_RAILWAY) {
                    adjacent.add(new Coordinate(current.x + i, current.y));
                }
            }
        } else { // Not the engineer
            for (int i = -1; i <= 1; i += 2) { // The beginning and end are on
                // the same row/column
                if (current.x == end.x
                        && validYX(current.y + i, current.x)
                        && stations[current.y + i][current.x] == STATION_RAILWAY) {
                    if(!((current.y ==5) && (current.x == 1)
                            ||(current.y ==5) && (current.x == 3)
                            ||(current.y ==6) && (current.x == 1)
                            ||(current.y ==6) && (current.x == 3))
                            ){
                        adjacent.add(new Coordinate(current.x, current.y + i));
                    }
                }
                if (current.y == end.y
                        && validYX(current.y , current.x+i)
                        && stations[current.y][current.x + i] == STATION_RAILWAY) {
                    adjacent.add(new Coordinate(current.x + i, current.y));
                }
            }
        }
        return adjacent;
    }

    /**
     * True if openList contains the target
     */
    private boolean openListContains(Coordinate target) {
        for (Coordinate c : openList) {
            if (c.equals(target))
                return true;
        }
        return false;
    }

    /**
     * True if closedList contains the target
     */
    private boolean closedListContains(Coordinate target) {
        for (Coordinate c : closedList) {
            if (c.equals(target))
                return true;
        }
        return false;
    }

    /**
     * Look for the Coordinate that has the minimum F value from openList list
     */
    private Coordinate lookForMinF(Coordinate target) {
        Coordinate c = openList.get(0);
        for (int i = 1; i < openList.size(); i++) {
            Coordinate tmp = openList.get(i);
            if (getCostG(tmp.parent, tmp) + getDistanceH(tmp, target) < getCostG(
                    c.parent, c) + getDistanceH(c, target)) {
                c = tmp;
            }
        }
        return c;
    }

    /**
     * The G function - cost from c0 to c1
     */
    private int getCostG(Coordinate c0, Coordinate c1) {
        // c.parent compare to c, if c is the beginning, then c.parent is NULL
        if (c0 == null || c1 == null) {
            return 0;
        }

        // Validation
        if (stations[c0.y][c0.x] == Pieces.INVALID
                || stations[c1.y][c1.x] == Pieces.INVALID) {
            return Integer.MAX_VALUE;
        }

        if (c0.x == c1.x || c0.y == c1.y) {
            return Math.abs(c0.x - c1.x) * 10 + Math.abs(c0.y - c1.y) * 10;
        } else if (Math.abs(c0.x - c1.x) == 1 && Math.abs(c0.y - c1.y) == 1) {
            return 14;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * The H function - Manhattan distance from x0,y0 to x,y
     */
    private int getDistanceH(int x0, int y0, int x, int y) {
        return (Math.abs(x0 - x) + Math.abs(y0 - y)) * 10;
    }
    /**
     * The H function - Manhattan distance from c0 to c1
     */
    private int getDistanceH(Coordinate c0, Coordinate c1) {
        return getDistanceH(c0.x, c0.y, c1.x, c1.y);
    }

    /**
     * Road adjacent
     */
    private boolean roadAdjacent(int x0, int y0, int x, int y) {
        return x0 == x && Math.abs(y0 - y) == 1 || y0 == y && Math.abs(x0 - x) == 1;
    }
    /**
     * Camp adjacent
     */
    private boolean campAdjacent(int x0, int y0, int x, int y) {
        return Math.abs(x0 - x) == 1 && Math.abs(y0 - y) == 1;
    }
    /**
     * Valid railway road
     */
    private boolean validRailwayRoad(int x0, int y0, int x, int y) {
        return x0 == x 	|| y0 == y;

		/*
		 return  x0 == x && ( x == 0 || x == 4 || x == 2 && y >= 5 && y <=6)
			||  y0 == y && (y == 1 || y == 5 || y == 6 || y == 10);
		*/
    }


    public Coordinate getFrom() {
        return from;
    }

    public void setFrom(Coordinate from) {
        this.from = from;
    }

    public Coordinate getTo() {
        return to;
    }

    public void setTo(Coordinate to) {
        this.to = to;
    }

    public Coordinate getFrom0() {
        return from0;
    }

    public void setFrom0(Coordinate from0) {
        this.from0 = from0;
    }

    public Coordinate getTo0() {
        return to0;
    }

    public void setTo0(Coordinate to0) {
        this.to0 = to0;
    }

    public int getMoveAndAttackResult() {
        return moveAndAttackResult;
    }

    public void setMoveAndAttackResult(int moveAndAttackResult) {
        this.moveAndAttackResult = moveAndAttackResult;
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        Board b = new Board();
        b.printStations();
    }
    /**
     * test
     */
    private void printStations() {
        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                if (stations[y][x] == HEADQUARTER) {
                    System.out.print("HA");
                } else if (stations[y][x] == CAMP) {
                    System.out.print("()");
                } else if (stations[y][x] == STATION_RAILWAY) {
                    System.out.print("==");
                } else if (stations[y][x] == STATION_ROAD) {
                    System.out.print("..");
                }
                System.out.print("   ");
            }
            System.out.print("\n");
        }
    }

    /**
     * Board object Clone for AB search
     */
    public Object clone() {
        Board bclone = null;
        try {
            bclone = (Board) super.clone();
            bclone.boardArea = this.newCopyOfBoard();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return bclone;
    }
}
