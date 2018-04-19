package com.example.wyj.luzhanqi.game;

import com.example.wyj.luzhanqi.LuZhanQiView;
import com.example.wyj.luzhanqi.game.ai.Movement;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Vector;

/**
 * Created by wyj on 2018/1/23.
 */

public class Board implements Cloneable{
    public static String chessName = "";
    private byte killed = 0;

    public static final int BOARD_COLUMN = 5;
    public static final int BOARD_ROW = 12;
    public static final byte EMPTY = 0x00;

    public static final byte HEADQUARTER = 0x71;
    public static final byte CAMP = 0x72;
    public static final byte ROAD_STATION = 0x73;
    public static final byte RAILWAY_STATION = 0x74;

    public static final int TotalChessNum = 50;

    public static final int INVALID_MOVE = 1000;
    public static final int MOVE = 1001;
    public static final int RANK_HIGHER = 1002;
    public static final int RANK_SAME = 1003;
    public static final int RANK_LOWER = 1004;
    public static final int PLAYER_LOSE = 1005;
    public static final int AI_LOSE = 1006;



    private byte[][] checkerboard = new byte[BOARD_ROW][BOARD_COLUMN];

    private final byte[][] stations = new byte[BOARD_ROW][BOARD_COLUMN];

    private PriorityQueue<Point> openList;
    private ArrayList<Point> closedList;

    private Vector<Point> paths;
    private Point touch_start;
    private Point touch_end; 	// on touch event
    private Point start;
    private Point end; // last touch_start and touch_end, touch_end draw the red tag
    private int curStep = 0;
    private int actionType;
    private byte currChess;


    public void initBoard() {
        for (int x = 0; x < BOARD_ROW; x++) {
            for (int y = 0; y < BOARD_COLUMN; y++) {
                checkerboard[x][y] = EMPTY;
                if ((x == 0 || x == 11) && (y == 1 || y == 3)) {
                    stations[x][y] = HEADQUARTER;
                } else if ((x == 2 || x == 4 || x == 7 || x == 9)
                        && (y == 1 || y == 3) || (x == 3 || x == 8) && y == 2) {
                    stations[x][y] = CAMP;
                } else if (x == 1 || x == 5 || x == 6 || x == 10 || y == 0
                        && x != 0 && x != 11 || y == 4 && x != 0 && x != 11) {
                    stations[x][y] = RAILWAY_STATION;
                } else {
                    stations[x][y] = ROAD_STATION;
                }
            }
        }

        paths = null;
        touch_start = null;
        touch_end = null;
        start = null;
        end = null;
        curStep = 0;
        currChess = EMPTY;
        actionType = INVALID_MOVE;
    }


    public boolean loadChess(int location, InputStream inputstream) {
        if (inputstream == null) {
            return false;
        }

        byte[] bytes = new byte[TotalChessNum];
        int x = 0 , y = 0 ;
        try {
            inputstream.read(bytes, 0, TotalChessNum);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 20; i < bytes.length; i++) {
            if (bytes[i] != EMPTY) {
                if (location == Chess.PLAYER) {
                    x = (i - 20) % 5; // 0, 1, 2, 3, 4
                    y = 6 + (i - 20) / 5; // 6 -> 7 -> 8 -> 9 -> 10
                    checkerboard[y][x] = bytes[i];
                } else if (location == Chess.AI) {
                    x = 4 - (i - 20) % 5;
                    y = 5 - (i - 20) / 5;
                    checkerboard[y][x] = (byte) ((int) bytes[i] + 0x10);
                }
            }
        }
        return true;
    }



    public byte[][] CloneBoard() {
        byte[][] copy = new byte[BOARD_ROW][BOARD_COLUMN];
        for (int x = 0; x < BOARD_ROW; x++) {
            for (int y = 0; y < BOARD_COLUMN; y++) {
                copy[x][y] = checkerboard[x][y] ;
            }
        }
        return copy;
    }


    public void undo(byte[][] copy) {
        for (int x = 0; x < BOARD_ROW; x++) {
            for (int y = 0; y < BOARD_COLUMN; y++) {
                if (checkerboard[x][y] != copy[x][y]) {
                    checkerboard[x][y] = copy[x][y];
                }
            }
        }
    }

    public boolean validXY (int x, int y) {
        if (x < 0 || x >= BOARD_COLUMN || y < 0 || y >= BOARD_ROW) {
            return false;
        }
        return true;
    }

    public byte[][] getStations() {
        return stations;
    }

    public byte[][] getCheckerboard() {
        return checkerboard;
    }


    public void resetTouchStartEnd(){
        touch_start = null;
        touch_end = null;
    }

    public boolean clickAndMove(int player, Point point) {
        byte lastPos = checkerboard[point.y][point.x];
        if (touch_start == null && player == Chess.getChessLocation(lastPos)){
            touch_start = point;
            touch_end = null;
            return true;
        }
        if (touch_start == null && player != Chess.getChessLocation(lastPos)){
            touch_end = null;
            return false;
        }
        if (touch_end == null) {
            byte currPos = checkerboard[touch_start.y][touch_start.x];
            if (Chess.sameLocation(lastPos, currPos)){
                touch_start = point;
            } else {
                touch_end = point;
            }
            return true;
        }
        return false;
    }

    public void nextActionType() {
        if (touch_start == null || touch_end == null) {
            return;
        }
        paths = getPath(touch_start.x, touch_start.y, touch_end.x, touch_end.y);
        if (paths == null || paths.size() < 1) {
            start = touch_start;
            end = touch_end;
            touch_start = null;
            touch_end = null;
            paths = null;
            curStep = 0;
            return;
        }

        actionType = getActionType(touch_start.x, touch_start.y, touch_end.x, touch_end.y);
    }


    private int getActionType(int startX, int startY, int endX, int endY) {
        // chess can not move if it's in the headquarter, if its path is surrounded by other chess, or move to the same location
        if (stations[startY][startX] == EMPTY ||
                stations[endY][endX] == EMPTY ||
                checkerboard[startY][startX] == EMPTY ||
                (checkerboard[endY][endX] != EMPTY &&
                        Chess.sameLocation(checkerboard[startY][startX], checkerboard[endY][endX]))) {
            return INVALID_MOVE;
        }
        // Move
        if (checkerboard[endY][endX] == EMPTY) {
            return MOVE;
        }
        // Attack
        if (Chess.getType(checkerboard[endY][endX]) == Chess.playerFlag) { // check whose flag is
            int losePosition = Chess.getChessLocation(checkerboard[endY][endX]);
            return losePosition > 0 ? AI_LOSE : PLAYER_LOSE;
        } else if (Chess.getType(checkerboard[startY][startX]) == Chess.playerBomb
                || Chess.getType(checkerboard[endY][endX]) == Chess.playerBomb) {
            return RANK_SAME;
        } else if (Chess.getType(checkerboard[endY][endX]) == Chess.playerMine) {
            if (Chess.getType(checkerboard[startY][startX]) == Chess.playerEngineer) {
                chessName = Chess.pieceTitle(Chess.playerMine);
                return RANK_HIGHER;
            } else {
                chessName = Chess.pieceTitle(Chess.playerEngineer);
                return RANK_LOWER;
            }
        } else {
            byte chessStart = Chess.getType(checkerboard[startY][startX]);
            byte chessEnd = Chess.getType(checkerboard[endY][endX]);
            if (chessStart < chessEnd) {
                if (LuZhanQiView.getTurn() == Chess.AI) {
                    killed = chessEnd;
                    chessName = Chess.pieceTitle(killed);
                }
                return RANK_HIGHER;
            } else if (chessStart == chessEnd) {
                return RANK_SAME;
            } else {
                if (LuZhanQiView.getTurn() == Chess.PLAYER) {
                    killed = chessStart;
                    chessName = Chess.pieceTitle(killed);
                }
                return RANK_LOWER;
            }
        }
    }


    public void takeAction(Movement move) {
        if (move == null){
            return ;
        }
        Point prev = move.getStart();
        Point curr = move.getEnd();

        int action = getActionType(prev.x, prev.y, curr.x, curr.y);

        switch (action) {
            case MOVE:
            case RANK_HIGHER:
            case AI_LOSE:
            case PLAYER_LOSE:
                checkerboard[curr.y][curr.x] = checkerboard[prev.y][prev.x];
                checkerboard[prev.y][prev.x] = EMPTY;
                break;
            case RANK_SAME:
                checkerboard[curr.y][curr.x] = EMPTY;
                checkerboard[prev.y][prev.x] = EMPTY;
                break;
            case RANK_LOWER:
                checkerboard[prev.y][prev.x] = EMPTY;
                break;
        }
    }



    public boolean hasNextStep(){
        if (paths == null || paths.size() == 0) {
            curStep = 0;
            return false;
        }
        if (curStep <= paths.size() - 1) {
            return true;
        }
        return false;
    }

    private boolean isLastStep(){
        return curStep + 1 == paths.size();
    }


    public boolean nextStep(){
        if (curStep <= 0) {
            currChess = checkerboard[touch_start.y][touch_start.x];
            checkerboard[touch_start.y][touch_start.x] = EMPTY;
        } else {
            Point last = paths.get(curStep - 1);
            checkerboard[last.y][last.x] = EMPTY;
        }

        if (!isLastStep()) {
            Point next = paths.get(curStep);
            checkerboard[next.y][next.x] = currChess;
            curStep++;
            return true;
        } else {
            switch (actionType) {
                case MOVE:
                case RANK_HIGHER:
                case AI_LOSE:
                case PLAYER_LOSE:
                    checkerboard[touch_end.y][touch_end.x] = currChess;
                    break;
                case RANK_SAME:
                    checkerboard[touch_end.y][touch_end.x] = EMPTY;
                    break;
                case RANK_LOWER:
                    break;
            }

            start = touch_start;
            end = touch_end;
            touch_start = null;
            touch_end = null;
            paths = null;
            curStep = 0;
            return false;
        }
    }

    public Vector<Point> getPath(int startX, int startY, int endX, int endY) {
        // No piece in x0,y0 or The pieces in HEADQUARTER and the mines can NOT move
        if (checkerboard[startY][startX]== EMPTY
                || stations[startY][startX] == HEADQUARTER
                || Chess.getType(checkerboard[startY][startX]) == Chess.playerMine) {
            return null;
        }
        // Can not move to a camp which is occupied
        if (stations[endY][endX] == CAMP && checkerboard[endY][endX] != EMPTY) {
            return null;
        }
        // move to the same location
        if (Chess.sameLocation(checkerboard[startY][startX], checkerboard[endY][endX])) {
            return null;
        }

        Vector<Point> path = new Vector<>();


        // if chess is on last row, or want to move to last row
        // if chess is in camp or want to move to camp
        // if chess wants to move to headquarter(chess can not move once it's in the headquarter)
        // if only one step, add to path
        if (stations[startY][startX] == ROAD_STATION
                || stations[endY][endX] == ROAD_STATION
                || stations[startY][startX] == CAMP
                || stations[endY][endX] == CAMP
                || stations[endY][endX] == HEADQUARTER) {
            if (roadAdjacent(startX, startY, endX, endY)) {
                path.add(new Point(endX, endY));
                return path;
            }
        }

        // camp to camp, one step
        if (stations[startY][startX] == CAMP
                || stations[endY][endX] == CAMP) {
            if (campAdjacent(startX, startY, endX, endY)) {
                path.add(new Point(endX, endY));
                return path;
            }
        }
        // A*
        AStar(startX, startY, endX, endY, path);

        Collections.reverse(path);
        return path;
    }


    private void AStar(int startX, int startY, int endX, int endY, Vector<Point> path) {

        Point startNode = new Point(startX, startY);
        final Point end = new Point(endX, endY);
        openList = new PriorityQueue<>(1000, new Comparator<Point>() {
            @Override
            public int compare(Point point, Point t1) {
                return gScore(point.parent, point) + heuristics(point, end) - gScore(t1.parent, t1) + heuristics(t1, end);
            }
        });
        closedList = new ArrayList<>();

        if (stations[startY][startX] == RAILWAY_STATION && stations[endY][endX] == RAILWAY_STATION &&
                (validRailwayRoad(startX, startY, endX, endY) || Chess.getType(checkerboard[startY][startX]) == Chess.playerEngineer)) {
            ArrayList<Point> adjacent;
            boolean engineer = Chess.getType(checkerboard[startY][startX]) == Chess.playerEngineer;
            Point current = null;
            openList.add(startNode);

            while (!openListContains(end) && openList.size() > 0) {
                current = openList.poll();
                closedList.add(current);

                adjacent = getNeighbors(current, end, engineer);
                for (Point adj : adjacent) {
                    if (closedListContains(adj) || (checkerboard[adj.y][adj.x] != Chess.INVALID && !adj.equals(end))) {
                        continue;
                    }
                    if (!openListContains(adj)) {
                        adj.parent = current;
                        openList.add(adj);
                    } else {
                        if (gScore(current.parent, current) + gScore(current, adj) < gScore(current.parent, adj)) {
                            adj.parent = current;
                        }
                    }
                }
            }
            end.parent = current;

            if (openListContains(end)) { // Find the path
                Point tmp = end;
                while (tmp != startNode) {
                    path.add(tmp);
                    tmp = tmp.parent;
                }
            }
        }
    }

    private boolean isCamp(Point p) {
        if (((p.y == 5) && (p.x == 1) ||
                (p.y == 5) && (p.x == 3) ||
                (p.y == 6) && (p.x == 1) ||
                (p.y == 6) && (p.x == 3))) {
            return true;
        }
        return false;
    }

    private ArrayList<Point> getNeighbors(Point current, Point end, boolean engineer) {
        ArrayList<Point> neighbors = new ArrayList<>();

        if (engineer) {
            // get four direction of the chess
            for (int i = -1; i <= 1; i += 2) {
                // check for column
                if (validXY(current.x, current.y + i) &&
                        stations[current.y + i][current.x] == RAILWAY_STATION) {
                    if (!isCamp(current)) {
                        neighbors.add(new Point(current.x, current.y + i));
                    }
                }
                // check for row
                if (validXY(current.x + i, current.y) &&
                        stations[current.y][current.x + i] == RAILWAY_STATION) {
                    neighbors.add(new Point(current.x + i, current.y));
                }
            }
        } else {
            for (int i = -1; i <= 1; i += 2) {
                // same row
                if (current.x == end.x && validXY(current.x, current.y + i)
                        && stations[current.y + i][current.x] == RAILWAY_STATION) {
                    if (!isCamp(current)){
                        neighbors.add(new Point(current.x, current.y + i));
                    }
                }
                // same column
                if (current.y == end.y && validXY(current.x + i, current.y) &&
                        stations[current.y][current.x + i] == RAILWAY_STATION) {
                    neighbors.add(new Point(current.x + i, current.y));
                }
            }
        }
        return neighbors;
    }

    private boolean openListContains(Point target) {
        for (Point p : openList) {
            if (p.equals(target))
                return true;
        }
        return false;
    }

    private boolean closedListContains(Point target) {
        for (Point p : closedList) {
            if (p.equals(target))
                return true;
        }
        return false;
    }


    // g(n)
    private int gScore(Point curr, Point goal) {
        if (curr == null || goal == null) {
            return 0;
        }

        // Validation
        if (stations[curr.y][curr.x] == Chess.INVALID
                || stations[goal.y][goal.x] == Chess.INVALID) {
            return Integer.MAX_VALUE;
        }

        if (curr.x == goal.x || curr.y == goal.y) { // move horizontal or move vertical, cost 10
            return Math.abs(curr.x - goal.x) * 10 + Math.abs(curr.y - goal.y) * 10;
        } else if (Math.abs(curr.x - goal.x) == 1 && Math.abs(curr.y - goal.y) == 1) { // move diagonal, cost sqrt(10^2 + 10^2) = 14
            return 14;
        } else { // invalid move
            return Integer.MAX_VALUE;
        }
    }


    //  h(n) Manhattan distance
    private int heuristics(Point curr, Point goal) {
        return (Math.abs(curr.x - goal.x) + Math.abs(curr.y - goal.y)) * 10;
    }

    private boolean roadAdjacent(int currX, int currY, int nextX, int nextY) {
        return currX == nextX && Math.abs(currY - nextY) == 1 || currY == nextY && Math.abs(currX - nextX) == 1;
    }

    private boolean campAdjacent(int currX, int currY, int nextX, int nextY) {
        return Math.abs(currX - nextX) == 1 && Math.abs(currY - nextY) == 1;
    }

    private boolean validRailwayRoad(int startX, int startY, int endX, int endY) {
        return startX == endX || startY == endY;
    }


    public Point getTouch_start() {
        return touch_start;
    }


    public Point getTouch_end() {
        return touch_end;
    }


    public Point getStart() {
        return start;
    }


    public Point getEnd() {
        return end;
    }


    public int getActionType() {
        return actionType;
    }

    public Object clone() {
        Board bclone = null;
        try {
            bclone = (Board) super.clone();
            bclone.checkerboard = this.CloneBoard();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return bclone;
    }
}



