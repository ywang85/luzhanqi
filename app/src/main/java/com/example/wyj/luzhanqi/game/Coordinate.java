package com.example.wyj.luzhanqi.game;

/**
 * Created by wyj on 2018/1/23.
 */

public class Coordinate {
    public int x;
    public int y;
    public int value;
    public Coordinate parent = null; // 为A*寻路使用

    public Coordinate(int coordinate) {
        this.x = coordinate % 100;
        this.y = coordinate / 100;
        this.value = coordinate;
    }

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
        this.value = 100 * y + x;
    }

    public boolean equals(Coordinate c) {
        return this.value == c.value;
    }
}
