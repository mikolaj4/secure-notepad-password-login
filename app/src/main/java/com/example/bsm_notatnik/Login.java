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
import javax.crypto.spec.SecretKeySpec;

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

                try {
                    login(hashedEmail, password);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                }
                //progressBar.setVisibility(View.GONE);
            }
        });
    }


    private boolean checkIfUserExists(String hashedemail){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        return sharedPreferences.contains("user_" + hashedemail);
    }

    private void login(String hashedemail, String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        String passwordHashFromData = sharedPreferences.getString("user_" + hashedemail, "err");


        byte[] salt = getSaltForUser(hashedemail, false);

        String inputPasswordHash = Utility.hashCredential(password, salt, 1000);

        assert inputPasswordHash != null;

        if (inputPasswordHash.equals(passwordHashFromData)){
            Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("CURRENT_USER_EMAIL_HASH", hashedemail);
            //intent.putExtra("KEY", getKeyFromPassword(password, getSalt2(hashedemail)));
            intent.putExtra("PAS", password);
            startActivity(intent);
            finish();

        }else {
            Toast.makeText(getApplicationContext(), "Wrong credentials!", Toast.LENGTH_SHORT).show();
            editTextPassword.setText("");
        }
    }

    public static SecretKey getKeyFromPassword(String password, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

        return secret;
    }

    private String genKeyHash(String hashedemail, String password){
        byte[] salt2 = getSaltForUser(hashedemail, true);
        return Utility.hashCredential(password, salt2, 5000);
    }

    private byte[] getSaltForUser(String hashedEmail, boolean salt2){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        String saltFromData;

        if (salt2){
            saltFromData = sharedPreferences.getString("salt_2_" + hashedEmail, "err");
        }
        else {
            saltFromData = sharedPreferences.getString("salt_" + hashedEmail, "err");
        }
        return Base64.getDecoder().decode(saltFromData);

    }

    private String getSalt2(String hashedEmail){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        return sharedPreferences.getString("salt_2_" + hashedEmail, "err");
    }

}