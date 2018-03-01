package com.example.wyj.luzhanqi.game;


/**
 * Created by wyj on 2018/1/23.
 */

public class Pieces {
    /*
     * Soldier type 对于棋子而言 － 无棋子: 0x00 ; South的棋子0x0-, North 0x1-
     */
    public static final byte FLAG_S = 0x02;
    public static final byte MINE_S = 0x03;
    public static final byte BOMB_S = 0x04;
    public static final byte SILING_S = 0x05;
    public static final byte JUNZHANG_S = 0x06;
    public static final byte SHIZHANG_S = 0x07;
    public static final byte LVZHANG_S = 0x08;
    public static final byte TUANZHANG_S = 0x09;
    public static final byte YINGZHANG_S = 0x0A;
    public static final byte LIANZHANG_S = 0x0B;
    public static final byte PAIZHANG_S = 0x0C;
    public static final byte GONGBING_S = 0x0D;
    public static final byte FLAG_N = 0x12;
    public static final byte MINE_N = 0x13;
    public static final byte BOMB_N = 0x14;
    public static final byte SILING_N = 0x15;
    public static final byte JUNZHANG_N = 0x16;
    public static final byte SHIZHANG_N = 0x17;
    public static final byte LVZHANG_N = 0x18;
    public static final byte TUANZHANG_N = 0x19;
    public static final byte YINGZHANG_N = 0x1A;
    public static final byte LIANZHANG_N = 0x1B;
    public static final byte PAIZHANG_N = 0x1C;
    public static final byte GONGBING_N = 0x1D;
    public static final byte INVALID = 0x00;
    // tag for man or AI
    public static final int MAN_TAG = 0;
    public static final int AI_TAG = 1;
    public static final int UNKNOWN_TAG = -1;

    /**
     * Solider type <--> caption
     */
    public static String pieceTitle(byte type) {
        String title = "n/a";
        switch (type) {
            case FLAG_S:
                //军棋
                title = "Flag";
                break;
            case FLAG_N:
                title = "AIFlag";
                break;
            case MINE_S:
                //地雷
                title = "LandM";
                break;
            case MINE_N:
                title = "AILandM";
                break;
            case BOMB_S:
                //炸弹
                title = "Bomb";
                break;
            case BOMB_N:
                //炸弹
                title = "AIBomb";
                break;
            case SILING_S:
                //司令
                title = "F Mar";
                break;
            case SILING_N:
                //司令
                title = "AIF Mar";
                break;
            case JUNZHANG_S:
                //军长
                title = "Gener";
                break;
            case JUNZHANG_N:
                //军长
                title = "AIGener";
                break;
            case SHIZHANG_S:
                //师长
                title = "M Gen";
                break;
            case SHIZHANG_N:
                //师长
                title = "AIM Gen";
                break;
            case LVZHANG_S:
                //旅长
                title = "Briga";
                break;
            case LVZHANG_N:
                //旅长
                title = "AIBriga";
                break;
            case TUANZHANG_S:
                //团长
                title = "Colon";
                break;
            case TUANZHANG_N:
                //团长
                title = "AIColon";
                break;
            case YINGZHANG_S:
                //营长
                title = "Major";
                break;
            case YINGZHANG_N:
                //营长
                title = "AIMajor";
                break;
            case LIANZHANG_S:
                //连长
                title = "Capta";
                break;
            case LIANZHANG_N:
                //连长
                title = "AICapta";
                break;
            case PAIZHANG_S:
                //排长
                title = "Lieut";
                break;
            case PAIZHANG_N:
                //排长
                title = "AILieut";
                break;
            case GONGBING_S:
                //工兵
                title = "Engin";
                break;
            case GONGBING_N:
                //工兵
                title = "AIEngin";
                break;
        }
        return title;
    }

    /**
     * Pieces are the same player
     * @param p0
     * @param p1
     * @return
     */
    public static boolean sameLocation (byte p0, byte p1){
        if (p0 == Board.INVALID_BOARD_TAG || p1 == Board.INVALID_BOARD_TAG) {
            return false;
        }
        return getLocated(p0) == getLocated(p1);
    }

    /**
     * get located from the piece
     * 0 : man
     * 1 : AI
     */
    public static int getLocated(byte p) {
        if (p > 0x40) {
            return UNKNOWN_TAG;
        } else if (p > 0x30) {
            return UNKNOWN_TAG;
        } else if (p > 0x20) {
            return UNKNOWN_TAG;
        } else if (p > 0x10) {
            return AI_TAG;
        } else if (p > 0x00){
            return MAN_TAG;
        } else {
            return UNKNOWN_TAG;
        }
    }


    /**
     * Convert the 0x3a, 0x2a, 0x1a to 0x0a
     *
     * @param p
     * @return
     */
    public static byte getPureType(byte p) {
        if (p > 0x40) {
            return INVALID;
        } else if (p > 0x30) {
            return (byte) (p - 0x30);
        } else if (p > 0x20) {
            return (byte) (p - 0x20);
        } else if (p > 0x10) {
            return (byte) (p - 0x10);
        } else {
            return p;
        }
    }



}
