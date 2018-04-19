package com.example.wyj.luzhanqi.game.ai;

import com.example.wyj.luzhanqi.game.Point;

/**
 * Created by wyj on 2018/1/23.
 */

public class Movement implements Comparable<Movement> {
    private Point start;
    private Point end;
    private int value;

    public Movement(int xStart, int yStart, int xEnd, int yEnd) {
        this.start = new Point(xStart, yStart);
        this.end = new Point(xEnd, yEnd);
    }

    public Point getStart() {
        return start;
    }

    public Point getEnd() {
        return end;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public int compareTo(Movement movement) {
        Movement m = movement;
        return m.getValue() - this.value;
    }
}
