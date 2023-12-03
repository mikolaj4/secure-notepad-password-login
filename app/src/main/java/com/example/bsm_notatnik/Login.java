package com.example.bsm_notatnik;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Login extends AppCompatActivity {

    private static final String SHARED_NAME_CREDENTIALS = "Credentials";
    EditText editTextEmail, editTextPassword;
    Button buttonLogin;
    ProgressBar progressBar;
    TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.username);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.registerNow);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Register.class);
                startActivity(intent);
                finish(); // finishes current activity
            }
        });



        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //progressBar.setVisibility(View.VISIBLE);
                String email, password;
                email = String.valueOf(editTextEmail.getText());
                password = String.valueOf(editTextPassword.getText());

                if (TextUtils.isEmpty(email)){
                    Toast.makeText(Login.this, "Enter email!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)){
                    Toast.makeText(Login.this, "Enter password!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!checkIfUserExists(email)){
                    Toast.makeText(Login.this, "No such username in database!", Toast.LENGTH_SHORT).show();
                    editTextPassword.setText("");
                    return;
                }

                login(email, password);
                //progressBar.setVisibility(View.GONE);
            }
        });
    }


    private boolean checkIfUserExists(String email){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        return sharedPreferences.contains("user_" + email);
    }

    private void login(String email, String password){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        String passwordHashFromData = sharedPreferences.getString("user_" + email, "err");

        byte[] salt = getSaltForUser(email);

        String inputPasswordHash = hashPassword(password, salt);

        assert inputPasswordHash != null;

        if (inputPasswordHash.equals(passwordHashFromData)){
            Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("CURRENT_USER_EMAIL", email);
            startActivity(intent);
            finish();

        }else {
            Toast.makeText(getApplicationContext(), "Wrong credentials!", Toast.LENGTH_SHORT).show();
            editTextPassword.setText("");
        }
    }

    private byte[] getSaltForUser(String email){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        String saltFromData = sharedPreferences.getString("salt_" + email, "err");
        return Base64.getDecoder().decode(saltFromData);
    }

    private static String hashPassword(String password, byte[] salt){
        int iteratiions = 1000;
        int keyLen = 256;

        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iteratiions, keyLen);
        try{
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
            byte[] hashedPassword = secretKey.getEncoded();
            return Base64.getEncoder().encodeToString(hashedPassword);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }


}