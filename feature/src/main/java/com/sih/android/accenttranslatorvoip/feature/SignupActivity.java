package com.sih.android.accenttranslatorvoip.feature;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class SignupActivity extends AppCompatActivity {


    private TextView signupHeading;
    private EditText signupUsername;
    private EditText signupEmail;
    private EditText signupPassword;
    private Button signupSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signupHeading = (TextView)findViewById(R.id.signup_heading);
        signupUsername = (EditText)findViewById(R.id.signup_username);
        signupEmail = (EditText)findViewById(R.id.signup_email);
        signupPassword = (EditText)findViewById(R.id.signup_password);
        signupSubmit = (Button)findViewById(R.id.signup_submit);

    }
}
