package com.example.bsm_notatnik;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Base64;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Register extends AppCompatActivity {

    private static final String SHARED_NAME_CREDENTIALS = "Credentials";

    EditText editTextEmail, editTextPassword;
    Button buttonReg;
    ProgressBar progressBar;
    TextView loginNowTextView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextEmail = findViewById(R.id.username);
        editTextPassword = findViewById(R.id.password);
        buttonReg = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progressBar);
        loginNowTextView = findViewById(R.id.loginNow);


        //goes to login page
        loginNowTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish(); // finishes current activity
            }
        });


        //when register button is clicked
        buttonReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //progressBar.setVisibility(View.VISIBLE);
                String email, hashedEmail, password, hashedPassword;

                email = String.valueOf(editTextEmail.getText());
                password = String.valueOf(editTextPassword.getText());

                //checks if email field is not empty
                if (TextUtils.isEmpty(email)){
                    Toast.makeText(Register.this, "Enter email!", Toast.LENGTH_SHORT).show();
                    return;
                }
                //checks if password field is not empty
                if (TextUtils.isEmpty(password)){
                    Toast.makeText(Register.this, "Enter password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                hashedEmail = Utility.hashEmail(email);

                //checks if given username is already registered in database
                if (checkIfUserExists(hashedEmail)){
                    editTextEmail.setText("");
                    editTextPassword.setText("");
                    Toast.makeText(Register.this, "Account with this username already exists!", Toast.LENGTH_SHORT).show();
                    return;
                }
                //checks if email has correct format
                if (!validateEmail(email)){
                    editTextPassword.setText("");
                    Toast.makeText(Register.this, "Email format not correct!", Toast.LENGTH_SHORT).show();
                    return;
                }
                //checks password wymagania
                if (!validatePassword(password)){
                    Toast.makeText(Register.this, "Password to weak!", Toast.LENGTH_SHORT).show();
                    return;
                }



                byte[] salt1 = Utility.generateSalt();
                byte[] salt2 = Utility.generateSalt();
                saveSaltsForUser(hashedEmail, salt1, salt2);


                hashedPassword = Utility.hashCredential(password, salt1, 1000);

                saveNewUser(hashedEmail, hashedPassword);

                Toast.makeText(Register.this, "Konto utworzone z email: " + email + " oraz hasłem: " + password, Toast.LENGTH_SHORT).show();
                editTextEmail.setText("");
                editTextPassword.setText("");
            }
        });
    }



    private boolean checkIfUserExists(String hashedemail){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        return sharedPreferences.contains("user_" + hashedemail);
    }

    private boolean validateEmail(String email){
        final String EMAIL_PATTERN = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(email);

        return matcher.matches();
    }

    private boolean validatePassword(String password){
        final String PASSWORD_PATTERN = "^.{6,}$";
        Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
        Matcher matcher = pattern.matcher(password);

        return matcher.matches();
    }

    private void saveSaltsForUser(String hashedemail, byte[] salt1, byte[] salt2){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String salt1String = Base64.getEncoder().encodeToString(salt1);
        String salt2String = Base64.getEncoder().encodeToString(salt2);

        editor.putString("salt_" + hashedemail, salt1String);
        editor.putString("salt_2_" + hashedemail, salt2String);


        editor.apply();
    }

    private void saveNewUser(String hashedemail, String password){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_" + hashedemail, password);
        editor.apply();
    }
}