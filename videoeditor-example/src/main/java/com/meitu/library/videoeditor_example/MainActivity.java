package com.meitu.library.videoeditor_example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IjkMediaPlayer ijkMediaPlayer = new IjkMediaPlayer();
    }


}
