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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

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
                String email, hashedEmail, password;
                email = String.valueOf(editTextEmail.getText());

                hashedEmail = hashEmail(email);
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
                //progressBar.setVisibility(View.GONE);
            }
        });
    }


    private boolean checkIfUserExists(String hashedemail){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        return sharedPreferences.contains("user_" + hashedemail);
    }

    private String hashEmail(String email){
        byte[] emailSalt = new byte[16];
        emailSalt = getFirst16BytesOfHash(email);

        return hashCredential(email, emailSalt);
    }

    private void login(String hashedemail, String password){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        String passwordHashFromData = sharedPreferences.getString("user_" + hashedemail, "err");


        byte[] salt = getSaltForUser(hashedemail);

        String inputPasswordHash = hashCredential(password, salt);

        assert inputPasswordHash != null;

        if (inputPasswordHash.equals(passwordHashFromData)){
            Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("CURRENT_USER_EMAIL_HASH", hashedemail);
            startActivity(intent);
            finish();

        }else {
            Toast.makeText(getApplicationContext(), "Wrong credentials!", Toast.LENGTH_SHORT).show();
            editTextPassword.setText("");
        }
    }

    private byte[] getSaltForUser(String hashedemail){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        String saltFromData = sharedPreferences.getString("salt_" + hashedemail, "err");
        return Base64.getDecoder().decode(saltFromData);
    }

    private static String hashCredential(String credential, byte[] salt){
        int iteratiions = 1000;
        int keyLen = 256;

        KeySpec keySpec = new PBEKeySpec(credential.toCharArray(), salt, iteratiions, keyLen);
        try{
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
            byte[] hashedCredential = secretKey.getEncoded();
            return Base64.getEncoder().encodeToString(hashedCredential);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }


    private byte[] getFirst16BytesOfHash(String input){
        try {
            // Create MessageDigest instance for SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Get the hash value by updating the digest with the input bytes
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Truncate the hash to the first 16 bytes
            byte[] truncatedHash = new byte[16];
            System.arraycopy(hashBytes, 0, truncatedHash, 0, 16);

            return truncatedHash;
        } catch (NoSuchAlgorithmException e) {
            // Handle the exception (e.g., print an error message)
            e.printStackTrace();
            return null;
        }
    }


}