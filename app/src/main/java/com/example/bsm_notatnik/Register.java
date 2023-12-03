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
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
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

                hashedEmail = hashEmail(email);

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



                byte[] salt = generateSalt();
                saveSaltForUser(hashedEmail, salt);

                hashedPassword = hashCredential(password, salt);

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

    private String hashEmail(String email){
        byte[] emailSalt;
        emailSalt = getFirst16BytesOfHash(email);

        return hashCredential(email, emailSalt);
    }

    private static byte[] generateSalt(){
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    private void saveSaltForUser(String hashedemail, byte[] salt){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String saltString = Base64.getEncoder().encodeToString(salt);
        editor.putString("salt_" + hashedemail, saltString);
        editor.apply();
    }

    private void saveNewUser(String hashedemail, String password){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_" + hashedemail, password);
        editor.apply();
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