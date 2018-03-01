package com.example.wyj.luzhanqi;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class LuZhanQiActivity extends AppCompatActivity {
    private static final int SAVED_LINEUP_MAX_INDEX = 20;
    private static final String INTERNAL_LINEUP_FILENAME = "INTERNAL_LINEUP_FILENAME";
    private LuZhanQiView lzqView;
    private LuZhanQiView.GameThread gameThread;
    private ListView lineupListview;
    private boolean[] internalFileSaved = new boolean[SAVED_LINEUP_MAX_INDEX];
    private ArrayList<String> lineupListData = new  ArrayList<String>();
    private int saveShow = 0 ;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // get handles to the LunarView from XML, and its LunarThread
        lzqView = findViewById(R.id.luzhanqi_view);
        gameThread = lzqView.getGameThread();
        lineupListview = findViewById(R.id.lineup_listview);
        lineupListview.setVisibility(View.INVISIBLE);
        // getLineupListviewData();
        lineupListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                onItemClickP (position);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.start_game_item:
                gameThread.startGame();
                return true;
            case R.id.callin_lineup_item:
                getLineupListviewData(1);
                return true;
            case R.id.save_lineup_item:
                getLineupListviewData(0);
                return true;
            case R.id.restart_item:
                gameThread.restartGame();
                return true;
            case android.R.id.home:
                gameThread.setSurfaceReady(false);
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Show line up files
     * @param saveShow  0 : save ;  1: show
     */
    private void getLineupListviewData(int saveShow) {
        if (gameThread.getMode() != LuZhanQiView.GameThread.STATE_LINEUP){
            return;
        }

        setSaveShow(saveShow);
        lineupListview.setVisibility(View.VISIBLE);
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i< SAVED_LINEUP_MAX_INDEX; i++){
            String internalFileName = INTERNAL_LINEUP_FILENAME + i;
            internalFileSaved[i] = fileExistance(internalFileName);

            sb = new StringBuffer();
            sb.append(getText(R.string.listview_lineup));
            sb.append(String.valueOf(i+1));

            if (!internalFileSaved[i]) {
                sb.append( getText(R.string.listview_lineup_null));
            }

            if (lineupListData.size() < SAVED_LINEUP_MAX_INDEX){
                lineupListData.add(sb.toString());
            } else {
                lineupListData.remove(i);
                lineupListData.add(i, sb.toString());
            }
        }
        lineupListview.setAdapter(new ArrayAdapter<String>(this,R.layout.listview_array, lineupListData));
    }


    /**
     * Item click for the internal saved Line-up files
     * @param pos
     */
    private void onItemClickP (int pos ){
        if (pos < 0 || pos >= SAVED_LINEUP_MAX_INDEX) return;

        String file = INTERNAL_LINEUP_FILENAME + pos;
        if (this.getSaveShow() == 0) {
            saveLineup (file);
        } else if (this.getSaveShow() == 1) {
            if (internalFileSaved[pos]){
                callInLineup(file);
            } else {
                Toast.makeText(LuZhanQiActivity.this, getText(R.string.err_null_lineup), Toast.LENGTH_SHORT)
                        .show();
            }
        }
        lineupListview.setVisibility(View.INVISIBLE);
    }

    public void gameEnd() {
        if (gameThread.getMode() == LuZhanQiView.GameThread.STATE_WIN) {
            Toast.makeText(this, "You win", Toast.LENGTH_LONG).show();
        } else if (gameThread.getMode() == LuZhanQiView.GameThread.STATE_LOSE){
            Toast.makeText(this, "You lose", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 调入布局
     */
    private void callInLineup(String file) {
        FileInputStream fis = null;
        try {
            fis = openFileInput(file);
            lzqView.getBoard().loadPieces(0, fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        StringBuffer sb = new StringBuffer();
        sb.append(getText(R.string.callin_lineup));
        sb.append(getText(R.string.completed));
        Toast.makeText(LuZhanQiActivity.this, sb.toString(), Toast.LENGTH_SHORT)
                .show();
    }

    /**
     * 保存布局
     */
    private void saveLineup(String file) {
        byte[] bytes = new byte[50];
        // The first 16 bytes in ASCII are "Generated by MC" & 0x00
        // MC - Military Chess, my Java SiGuoJunQi Game
        bytes[0] = 0x47;
        bytes[1] = 0x65;
        bytes[2] = 0x6E;
        bytes[3] = 0x65;
        bytes[4] = 0x72;
        bytes[5] = 0x61;
        bytes[6] = 0x74;
        bytes[7] = 0x65;
        bytes[8] = 0x64;
        bytes[9] = 0x20;
        bytes[10] = 0x62;
        bytes[11] = 0x79;
        bytes[12] = 0x20;
        bytes[13] = 0x4D;
        bytes[14] = 0x43;
        bytes[15] = 0x00;
        // Other 4 bytes
        bytes[16] = 0x57; // W
        bytes[17] = 0x04; // EOT - End of transmission
        bytes[18] = 0x00;
        bytes[19] = 0x00;

        byte[][] boardArea = lzqView.getBoard().getBoardArea();
        int index = 20;
        for (int j = 6; j < 6 + 6; j++) {
            for (int i = 0; i < 5; i++) {
                bytes[index++] = boardArea[j][i];
            }
        }
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(file, Context.MODE_PRIVATE);
            fos.write(bytes);
            fos.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        StringBuffer sb = new StringBuffer();
        sb.append(getText(R.string.save_lineup));
        sb.append(getText(R.string.completed));
        Toast.makeText(LuZhanQiActivity.this, sb.toString(), Toast.LENGTH_SHORT)
                .show();
    }
    /**
     * Whether the file save in internal storage exists
     * @param fname
     * @return
     */
    public boolean fileExistance(String fname) {
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }

    public int getSaveShow() {
        return saveShow;
    }

    public void setSaveShow(int saveShow) {
        this.saveShow = saveShow;
    }

    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     *
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
    }

}