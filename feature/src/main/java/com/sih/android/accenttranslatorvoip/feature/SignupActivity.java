package com.sih.android.accenttranslatorvoip.feature;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.View;
import android.widget.Toast;
import android.content.Intent;


public class SignupActivity extends AppCompatActivity {


    private TextView signupHeading;
    private EditText signupUsername;
    private EditText signupEmail;
    private EditText signupPassword;
    private Button signupSubmit;
    SharedPreferences sharedPreferences;
    Editor editor;
    UserSession session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signupHeading = (TextView)findViewById(R.id.signup_heading);
        signupUsername = (EditText)findViewById(R.id.signup_username);
        signupEmail = (EditText)findViewById(R.id.signup_email);
        signupPassword = (EditText)findViewById(R.id.signup_password);
        signupSubmit = (Button)findViewById(R.id.signup_submit);

        sharedPreferences = getApplicationContext().getSharedPreferences("Reg", 0);
// get editor to edit in file
        editor = sharedPreferences.edit();

        signupSubmit.setOnClickListener(new View.OnClickListener() {

            public void onClick (View v) {
                String name = signupUsername.getText().toString();
                String email = signupEmail.getText().toString();
                String pass = signupPassword.getText().toString();

                if(signupUsername.getText().length()<=0){
                    Toast.makeText(SignupActivity.this, "Enter name", Toast.LENGTH_SHORT).show();
                }
                else if( signupEmail.getText().length()<=0){
                    Toast.makeText(SignupActivity.this, "Enter email", Toast.LENGTH_SHORT).show();
                }
                else if( signupPassword.getText().length()<=0){
                    Toast.makeText(SignupActivity.this, "Enter password", Toast.LENGTH_SHORT).show();
                }
                else{

                    // as now we have information in string. Lets stored them with the help of editor
                    editor.putString("Name", name);
                    editor.putString("Email",email);
                    editor.putString("txtPassword",pass);
                    //editor.commit();
                }   // commit the values

                // after saving the value open next activity
                Intent ob = new Intent(SignupActivity.this, Dashboard.class);
                startActivity(ob);

            }
        });

    }
}
