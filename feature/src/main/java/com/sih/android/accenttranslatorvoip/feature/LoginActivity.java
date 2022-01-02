package com.sih.android.accenttranslatorvoip.feature;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {


    private TextView loginHeading;
    private EditText loginPassword;
    private EditText loginUsername;
    private Button loginSubmit;
    private Button loginSignup;
    private static final String PREFER_NAME = "Reg";
    SharedPreferences sharedPreferences;
    UserSession session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginHeading = (TextView)findViewById(R.id.login_heading);
        loginPassword = (EditText)findViewById(R.id.login_password);
        loginUsername = (EditText)findViewById(R.id.login_username);
        loginSubmit = (Button)findViewById(R.id.login_submit);
        loginSignup = (Button)findViewById(R.id.login_signup);

        sharedPreferences = getApplicationContext().getSharedPreferences("Reg", 0);

        session = new UserSession(getApplicationContext());

        Toast.makeText(getApplicationContext(),
                "User Login Status: " + session.isUserLoggedIn(),
                Toast.LENGTH_LONG).show();

        sharedPreferences = getSharedPreferences(PREFER_NAME, Context.MODE_PRIVATE);

        loginSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new  Intent(getApplicationContext(),SignupActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                // Add new Flag to start new Activity
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);

                finish();
            }
        });

        loginSubmit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                // Get username, password from EditText
                String username = loginUsername.getText().toString();
                String password = loginPassword.getText().toString();

                // Validate if username, password is filled
                if(username.trim().length() > 0 && password.trim().length() > 0){
                    String uName = null;
                    String uPassword =null;

                    if (sharedPreferences.contains("Name")) {
                        uName = sharedPreferences.getString("Name", "");
                    }
                    if (sharedPreferences.contains("txtPassword")) {
                        uPassword = sharedPreferences.getString("txtPassword", "");
                    }

                    // Object uName = null;
                    // Object uEmail = null;
                    if(username.equals(uName) && password.equals(uPassword)){

                        session.createUserLoginSession(uName,
                                uPassword);

                        // Starting MainActivity
                        Intent i = new  Intent(getApplicationContext(),Dashboard.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        // Add new Flag to start new Activity
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);

                        finish();

                    }else{

                        // username / password doesn't match&
                        Toast.makeText(getApplicationContext(),
                                "Username/Password is incorrect",
                                Toast.LENGTH_LONG).show();

                    }
                }else{

                    // user didn't entered username or password
                    Toast.makeText(getApplicationContext(),
                            "Please enter username and password",
                            Toast.LENGTH_LONG).show();

                }

            }
        });
    }
}
