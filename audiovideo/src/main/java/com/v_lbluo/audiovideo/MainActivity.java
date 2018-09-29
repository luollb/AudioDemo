package com.v_lbluo.audiovideo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button start;
    private Button stop;

    private static final File parentPath = Environment.getExternalStorageDirectory();

    private AudioEncoder ae;

    private AudioRecordFunc instance;

    private AudioDemo demo;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
//            instance = AudioRecordFunc.getInstance();
//            ae = new AudioEncoder();
//            ae.setmSavePath(parentPath + "/" + "palyer.aac");
//            try {
//                ae.prepare();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            demo = AudioDemo.getInstance();
            demo.setmSavePath(parentPath + "/" + "demo.aac");
            try {
                demo.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        start.setOnClickListener(this);
        stop.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start: {
//                instance.startRecordAndFile();
//                try {
//                    ae.start();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                try {
                    demo.start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            break;
            case R.id.stop: {
//                instance.stopRecordAndFile();
//                ae.stop();
                try {
                    demo.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            break;
        }
    }
}
