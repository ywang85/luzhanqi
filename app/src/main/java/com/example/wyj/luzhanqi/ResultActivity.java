package com.example.wyj.luzhanqi;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.example.wyj.luzhanqi.game.Board;

public class ResultActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        TextView winOrLose = findViewById(R.id.winOrLose);
        TextView result = findViewById(R.id.result);
        boolean win = getIntent().getBooleanExtra(LuZhanQiView.RESULTS, false);
        if (win) {
            winOrLose.setText("You Win!");
        } else {
            winOrLose.setText("You Lose!");
        }
        result.setText("AI killed: " + LuZhanQiView.aiKilled + " You killed: " + LuZhanQiView.youKilled);
    }
}
