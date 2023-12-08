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

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Register extends AppCompatActivity {

    private static final String SHARED_NAME_CREDENTIALS = "Credentials";
    EditText editTextEmail, editTextPassword;
    Button buttonReg;
    TextView loginNowTextView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextEmail = findViewById(R.id.username);
        editTextPassword = findViewById(R.id.password);
        buttonReg = findViewById(R.id.btn_register);
        loginNowTextView = findViewById(R.id.loginNow);

        //goes to login page
        loginNowTextView.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish(); // finishes current activity
        });


        //when register button is clicked
        buttonReg.setOnClickListener(view -> {
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
            //checks password requirements
            if (!validatePassword(password)){
                Toast.makeText(Register.this, "Password to weak!", Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] salt1 = Utility.generateSalt();
            saveSaltForUser(hashedEmail, salt1);

            hashedPassword = Utility.hashCredential(password, salt1);

            saveNewUser(hashedEmail, hashedPassword);

            Toast.makeText(Register.this, "Created account with email: " + email, Toast.LENGTH_SHORT).show();
            editTextEmail.setText("");
            editTextPassword.setText("");
        });
    }



    private boolean checkIfUserExists(String hashedEmail){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        return sharedPreferences.contains("user_" + hashedEmail);
    }

    private boolean validateEmail(String email){
        final String EMAIL_PATTERN = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(email);

        return matcher.matches();
    }

    private boolean validatePassword(String password){
        final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[A-Z])(.{8,})$";
        Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
        Matcher matcher = pattern.matcher(password);

        return matcher.matches();
    }

    private void saveSaltForUser(String hashedEmail, byte[] salt1){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String salt1String = Base64.getEncoder().encodeToString(salt1);
        editor.putString("salt_" + hashedEmail, salt1String);

        editor.apply();
    }

    private void saveNewUser(String hashedEmail, String password){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_" + hashedEmail, password);
        editor.apply();
    }
}