package com.example.wyj.luzhanqi;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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
                ImageView board = findViewById(R.id.board);
                board.setVisibility(View.INVISIBLE);
                WebView webView = findViewById(R.id.web_view);
                webView.setVisibility(View.INVISIBLE);
                return true;
            case R.id.show_board:
                board = findViewById(R.id.board);
                if (board.getVisibility() == View.INVISIBLE) {
                    board.setVisibility(View.VISIBLE);
                } else {
                    board.setVisibility(View.INVISIBLE);
                }
                return true;
            case R.id.explain:
                webView = findViewById(R.id.web_view);
                if (webView.getVisibility() == View.INVISIBLE) {
                    webView.setVisibility(View.VISIBLE);
                    webView.getSettings().setJavaScriptEnabled(true);
                    webView.setWebViewClient(new WebViewClient());
                    webView.loadUrl("http://ancientchess.com/page/play-luzhanqi.htm");
                } else {
                    webView.setVisibility(View.INVISIBLE);
                }
                return true;
            case R.id.restart_item:
                gameThread.restartGame();
                board = findViewById(R.id.board);
                board.setVisibility(View.INVISIBLE);
                webView = findViewById(R.id.web_view);
                webView.setVisibility(View.INVISIBLE);
                return true;
            case android.R.id.home:
                gameThread.setSurfaceReady(false);
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
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