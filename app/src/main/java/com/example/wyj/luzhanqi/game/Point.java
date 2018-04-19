package com.example.wyj.luzhanqi.game;

/**
 * Created by wyj on 2018/1/23.
 */

public class Point {
    public int x;
    public int y;
    public int value;
    public Point parent = null;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
        this.value = x + 100 * y;
    }

    public boolean equals(Point c) {
        return this.value == c.value;
    }
}
