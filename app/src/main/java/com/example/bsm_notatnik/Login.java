package com.example.bsm_notatnik;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Base64;


public class Login extends AppCompatActivity {

    private static final String SHARED_NAME_CREDENTIALS = "Credentials";
    EditText editTextEmail, editTextPassword;
    Button buttonLogin;
    TextView textView;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_LOCKOUT_TIME = 60000;
    private int loginAttempts = 0;
    private long lastLoginAttemptTime = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.username);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.btn_login);
        textView = findViewById(R.id.registerNow);

        textView.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), Register.class);
            startActivity(intent);
            finish(); // finishes current activity
        });



        buttonLogin.setOnClickListener(view -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastLoginAttemptTime >= LOGIN_LOCKOUT_TIME) {
                loginAttempts = 0;
            }
            loginAttempts++;
            lastLoginAttemptTime = currentTime;
            if (loginAttempts > MAX_LOGIN_ATTEMPTS) {
                Toast.makeText(Login.this, "Too many login attempts.", Toast.LENGTH_SHORT).show();
                return;
            }


            String email, hashedEmail, password;

            email = String.valueOf(editTextEmail.getText());
            hashedEmail = Utility.hashEmail(email);

            password = String.valueOf(editTextPassword.getText());


            if (TextUtils.isEmpty(email)){
                Toast.makeText(Login.this, "Enter email!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)){
                Toast.makeText(Login.this, "Enter password!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!checkIfUserExists(hashedEmail)){
                Toast.makeText(Login.this, "No such username in database!", Toast.LENGTH_SHORT).show();
                editTextPassword.setText("");
                return;
            }

            login(hashedEmail, password);
        });
    }


    private boolean checkIfUserExists(String hashedEmail){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        return sharedPreferences.contains("user_" + hashedEmail);
    }

    private void login(String hashedEmail, String password) {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        String passwordHashFromData = sharedPreferences.getString("user_" + hashedEmail, "err");

        byte[] salt = getSaltForUser(hashedEmail);

        String inputPasswordHash = Utility.hashCredential(password, salt);

        assert inputPasswordHash != null;

        if (inputPasswordHash.equals(passwordHashFromData)){
            Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("CURRENT_USER_EMAIL_HASH", hashedEmail);
            intent.putExtra("PAS", password);
            startActivity(intent);
            finish();

        }else {
            Toast.makeText(getApplicationContext(), "Wrong credentials!", Toast.LENGTH_SHORT).show();
            editTextPassword.setText("");
        }
    }

    private byte[] getSaltForUser(String hashedEmail){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        String saltFromData = sharedPreferences.getString("salt_" + hashedEmail, "err");
        return Base64.getDecoder().decode(saltFromData);
    }

}