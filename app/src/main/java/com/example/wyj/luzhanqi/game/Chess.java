package com.example.wyj.luzhanqi.game;


/**
 * Created by wyj on 2018/1/23.
 */

public class Chess {
    /*
     * Soldier type 对于棋子而言 － 无棋子: 0x00 ; South的棋子0x0-, North 0x1-
     */
    public static final byte playerFlag = 0x02;
    public static final byte playerMine = 0x03;
    public static final byte playerBomb = 0x04;
    public static final byte playerFieldM = 0x05;
    public static final byte playerGeneral = 0x06;
    public static final byte playerMajorG = 0x07;
    public static final byte playerBrigadierG = 0x08;
    public static final byte playerColonel = 0x09;
    public static final byte playerMajor = 0x0A;
    public static final byte playerCaptain = 0x0B;
    public static final byte playerLieutenant = 0x0C;
    public static final byte playerEngineer = 0x0D;
    public static final byte AIFlag = 0x12;
    public static final byte AIMine = 0x13;
    public static final byte AIBomb = 0x14;
    public static final byte AIFieldM = 0x15;
    public static final byte AIGeneral = 0x16;
    public static final byte AIMajorG = 0x17;
    public static final byte AIBrigadierG = 0x18;
    public static final byte AIColonel = 0x19;
    public static final byte AIMajor = 0x1A;
    public static final byte AICaptain = 0x1B;
    public static final byte AILieutenant = 0x1C;
    public static final byte AIEngineer = 0x1D;
    public static final byte INVALID = 0x00;

    public static final int PLAYER = 0;
    public static final int AI = 1;
    public static final int UNKNOWN_TAG = -1;


    public static String pieceTitle(byte type) {
        String title = "AI";
        switch (type) {
            case playerFlag:
                //军棋
                title = "Flag";
                break;
            case AIFlag:
                title = "AIFlag";
                break;
            case playerMine:
                //地雷
                title = "LandM";
                break;
            case AIMine:
                title = "AILandM";
                break;
            case playerBomb:
                //炸弹
                title = "Bomb";
                break;
            case AIBomb:
                //炸弹
                title = "AIBomb";
                break;
            case playerFieldM:
                //司令
                title = "F Mar";
                break;
            case AIFieldM:
                //司令
                title = "AIF Mar";
                break;
            case playerGeneral:
                //军长
                title = "Gener";
                break;
            case AIGeneral:
                //军长
                title = "AIGener";
                break;
            case playerMajorG:
                //师长
                title = "M Gen";
                break;
            case AIMajorG:
                //师长
                title = "AIM Gen";
                break;
            case playerBrigadierG:
                //旅长
                title = "Briga";
                break;
            case AIBrigadierG:
                //旅长
                title = "AIBriga";
                break;
            case playerColonel:
                //团长
                title = "Colon";
                break;
            case AIColonel:
                //团长
                title = "AIColon";
                break;
            case playerMajor:
                //营长
                title = "Major";
                break;
            case AIMajor:
                //营长
                title = "AIMajor";
                break;
            case playerCaptain:
                //连长
                title = "Capta";
                break;
            case AICaptain:
                //连长
                title = "AICapta";
                break;
            case playerLieutenant:
                //排长
                title = "Lieut";
                break;
            case AILieutenant:
                //排长
                title = "AILieut";
                break;
            case playerEngineer:
                //工兵
                title = "Engin";
                break;
            case AIEngineer:
                //工兵
                title = "AIEngin";
                break;
        }
        return title;
    }

    public static boolean sameLocation (byte c1, byte c2){
        if (c1 == Board.EMPTY || c2 == Board.EMPTY) {
            return false;
        }
        return getChessLocation(c1) == getChessLocation(c2);
    }


     // 0 : man
     // 1 : AI

    public static int getChessLocation(byte p) {
        if (p > 0x10 && p <= 0x20) {
            return AI;
        }
        if (p > 0x00 && p <= 0x10) {
            return PLAYER;
        }
        return UNKNOWN_TAG;
    }


    public static byte getType(byte p) {
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
