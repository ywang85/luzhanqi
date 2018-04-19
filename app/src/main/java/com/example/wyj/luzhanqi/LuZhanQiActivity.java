package com.example.wyj.luzhanqi;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

public class LuZhanQiActivity extends AppCompatActivity {
    private LuZhanQiView lzqView;
    private GameThread gameThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
            case R.id.show_hide:
                gameThread.flipTitle();
                return true;
            case android.R.id.home:
                gameThread.setReady(false);
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


}