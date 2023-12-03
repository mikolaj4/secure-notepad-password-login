package com.example.bsm_notatnik;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


public class MainActivity extends AppCompatActivity {

    Button buttonLogout, buttonChangePassword;

    private static final String SHARED_NAME_CREDENTIALS = "Credentials";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String current_username_hashed = intent.getStringExtra("CURRENT_USER_EMAIL_HASH");

        buttonLogout = findViewById(R.id.btn_logout);
        buttonChangePassword = findViewById(R.id.btn_change_password);


        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logOut();
            }
        });


        buttonChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPasswordChangeDialog(current_username_hashed);
            }
        });

    }

    private void logOut(){
        Toast.makeText(getApplicationContext(), "Logout Successful!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(getApplicationContext(), Login.class);
        startActivity(intent);
        finish();
    }




    private void showPasswordChangeDialog(String hashedEmail){
        // Inflate the dialog layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.password_change_dialog, null);

        // Create the AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle("Change Password");

        // Set up the positive (OK) button
        builder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Handle password change logic here
                EditText editTextOldPassword = dialogView.findViewById(R.id.editTextOldPassword);
                EditText editTextNewPassword = dialogView.findViewById(R.id.editTextNewPassword);
                EditText editTextConfirmPassword = dialogView.findViewById(R.id.editTextConfirmPassword);

                String oldPassword = editTextOldPassword.getText().toString();
                String newPassword = editTextNewPassword.getText().toString();
                String confirmPassword = editTextConfirmPassword.getText().toString();

                if (TextUtils.isEmpty(oldPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
                    Toast.makeText(MainActivity.this, "Fill out all 3 fields!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!validatePassword(newPassword)){
                    Toast.makeText(MainActivity.this, "Wrong format of new password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!validateOldPassword(hashedEmail, oldPassword)){
                    Toast.makeText(MainActivity.this, "Old password not correct!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Perform password change validation and logic
                if (newPassword.equals(confirmPassword)) {
                    updatePassword(hashedEmail, newPassword);
                    Toast.makeText(MainActivity.this, "Password Changed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "New passwords don't match!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

        // Set up the negative (Cancel) button
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private boolean validatePassword(String password){
        final String PASSWORD_PATTERN = "^.{6,}$";
        Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
        Matcher matcher = pattern.matcher(password);

        return matcher.matches();
    }

    private void updatePassword(String hashedEmail, String newPassword){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        byte[] newSalt = generateSalt();
        String newSaltString = Base64.getEncoder().encodeToString(newSalt);
        editor.putString("salt_" + hashedEmail, newSaltString);

        String hashedNewPassword = hashCredential(newPassword, newSalt);
        editor.putString("user_" + hashedEmail, hashedNewPassword);
        editor.apply();
    }

    private boolean validateOldPassword(String hashedEmail, String oldPassword){
        byte[] salt = getSaltForUser(hashedEmail);
        String hashedOldPassword = hashCredential(oldPassword, salt);
        String hashedCorrectPassword = getPasswordFromShared(hashedEmail);

        assert hashedOldPassword != null;
        if (hashedOldPassword.equals(hashedCorrectPassword)){
            return true;
        } else {
            return false;
        }
    }

    private byte[] getSaltForUser(String hashedEmail){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        String saltFromData = sharedPreferences.getString("salt_" + hashedEmail, "err");
        return Base64.getDecoder().decode(saltFromData);
    }

    private String getPasswordFromShared(String hashedEmail){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        return sharedPreferences.getString("user_" + hashedEmail, "err");
    }

    private static byte[] generateSalt(){
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
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

}

