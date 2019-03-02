package com.sih.android.accenttranslatorvoip.feature;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

public class AccentDetection extends AppCompatActivity {


    private Button accentDecectionMic;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accent_detection);

        accentDecectionMic = (Button)findViewById(R.id.acccent_detection_mic);
    }
}
