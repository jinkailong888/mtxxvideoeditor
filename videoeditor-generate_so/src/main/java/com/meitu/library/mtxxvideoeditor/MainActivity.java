package com.meitu.library.mtxxvideoeditor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
    }


    static{
        System.loadLibrary("ffmpeg");
        System.loadLibrary("ijkplayer");
        System.loadLibrary("ijksdl");
    }


}
