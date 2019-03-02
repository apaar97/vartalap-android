package com.sih.android.accenttranslatorvoip.feature;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.w3c.dom.Text;

public class LoginActivity extends AppCompatActivity {


    private TextView loginHeading;
    private EditText loginPassword;
    private EditText loginUsername;
    private Button loginSubmit;
    private Button loginSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginHeading = (TextView)findViewById(R.id.login_heading);
        loginPassword = (EditText)findViewById(R.id.login_password);
        loginUsername = (EditText)findViewById(R.id.login_username);
        loginSubmit = (Button)findViewById(R.id.login_submit);
        loginSignup = (Button)findViewById(R.id.login_signup);


    }
}
