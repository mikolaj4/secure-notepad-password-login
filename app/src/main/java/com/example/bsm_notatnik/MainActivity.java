package com.example.bsm_notatnik;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity {
    private CryptoManager cryptoManager;

    Button buttonLogout, buttonChangePassword, buttonAddNewNote;
    private static final String SHARED_NAME_CREDENTIALS = "Credentials";
    private static final String SHARED_NOTES_NAME = "Notes";
    private static String HASHED_EMAIL = "";
    private static String KEY_HASH = "";
    private List<Note> noteList;
    private LinearLayout notesContainer;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            cryptoManager = new CryptoManager();
        } catch (Exception e) {
            e.printStackTrace(); // Handle exceptions according to your needs
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String current_username_hashed = intent.getStringExtra("CURRENT_USER_EMAIL_HASH");
        HASHED_EMAIL = current_username_hashed;
        KEY_HASH = intent.getStringExtra("KEY_HASH");

        notesContainer = findViewById(R.id.notesContainer);
        noteList = new ArrayList<>();
        loadNotesFromPreferencesToList();
        displayNotes();

        buttonLogout = findViewById(R.id.btn_logout);
        buttonChangePassword = findViewById(R.id.btn_change_password);
        buttonAddNewNote = findViewById(R.id.btn_add_note);

        Log.i("KURWAAAAAAAAAAAAAAAAA", KEY_HASH);

        buttonLogout.setOnClickListener(view -> logOut());

        buttonChangePassword.setOnClickListener(view -> showPasswordChangeDialog(current_username_hashed));

        buttonAddNewNote.setOnClickListener(view -> showAddNewNoteDialog());

    }




    private void logOut(){
        Toast.makeText(getApplicationContext(), "Logout Successful!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(getApplicationContext(), Login.class);
        startActivity(intent);
        finish();
    }


    private void showPasswordChangeDialog(String hashedEmail){
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.password_change_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle("Change Password");

        builder.setPositiveButton("Change", (dialogInterface, i) -> {
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

            if (newPassword.equals(confirmPassword)) {
                updatePassword(hashedEmail, newPassword);
                Toast.makeText(MainActivity.this, "Password Changed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "New passwords don't match!", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up the negative (Cancel) button
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());

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

        byte[] newSalt = Utility.generateSalt();
        String newSaltString = Base64.getEncoder().encodeToString(newSalt);
        editor.putString("salt_" + hashedEmail, newSaltString);

        String hashedNewPassword = Utility.hashCredential(newPassword, newSalt, 1000);
        editor.putString("user_" + hashedEmail, hashedNewPassword);
        editor.apply();
    }

    private boolean validateOldPassword(String hashedEmail, String oldPassword){
        byte[] salt = getSaltForUser(hashedEmail, false);
        String hashedOldPassword = Utility.hashCredential(oldPassword, salt, 1000);
        String hashedCorrectPassword = getPasswordFromShared(hashedEmail);

        assert hashedOldPassword != null;
        return hashedOldPassword.equals(hashedCorrectPassword);
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

    private String getPasswordFromShared(String hashedEmail){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        return sharedPreferences.getString("user_" + hashedEmail, "err");
    }









    private void showAddNewNoteDialog(){
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.create_note_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle("Create new note");

        builder.setPositiveButton("Save", (dialogInterface, i) -> {
            EditText noteTitleEditText = dialogView.findViewById(R.id.noteTitleEditText);
            EditText noteContentEditText = dialogView.findViewById(R.id.noteContentEditText);

            String title = noteTitleEditText.getText().toString();
            String content = noteContentEditText.getText().toString();

            if (!title.isEmpty() && !content.isEmpty()){
                Note note = new Note();
                note.setTitle(title);
                note.setContent(content);

                noteList.add(note);

                saveNotesToPreferences("add");
                createNoteView(note);
            }

            Toast.makeText(MainActivity.this, "Note saved!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void showEditNoteDialog(Note note){
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.create_note_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle("Edit note");

        EditText noteTitleEditText = dialogView.findViewById(R.id.noteTitleEditText);
        EditText noteContentEditText = dialogView.findViewById(R.id.noteContentEditText);
        noteTitleEditText.setText(note.getTitle());
        noteContentEditText.setText(note.getContent());

        builder.setPositiveButton("Save", (dialogInterface, i) -> {
            String title = noteTitleEditText.getText().toString();
            String content = noteContentEditText.getText().toString();

            if (!title.isEmpty() && !content.isEmpty()){
                deleteNoteAndRefresh(note);

                note.setTitle(title);
                note.setContent(content);

                noteList.add(note);

                saveNotesToPreferences("add");
                createNoteView(note);
            }else {
                Toast.makeText(MainActivity.this, "Enter title and content!", Toast.LENGTH_SHORT).show();
            }


        });

        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void saveNotesToPreferences(String mode){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NOTES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (mode.equals("del")){
            int noteCount = sharedPreferences.getInt("notecount_"+HASHED_EMAIL, 0);
            for(int i=0; i<noteCount; i++){
                editor.remove(i + "_title_" + HASHED_EMAIL);
                editor.remove(i + "_content_" + HASHED_EMAIL);
            }
        }

        editor.putInt("notecount_" + HASHED_EMAIL, noteList.size());
        for(int i=0; i<noteList.size(); i++){
            Note note = noteList.get(i);
            editor.putString(i + "_title_" + HASHED_EMAIL, encryptCesar(note.getTitle(), 2));
            editor.putString(i + "_content_" + HASHED_EMAIL, note.getContent());

        }

        editor.apply();
    }


    private void loadNotesFromPreferencesToList(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NOTES_NAME, MODE_PRIVATE);
        int noteCount = sharedPreferences.getInt("notecount_" + HASHED_EMAIL, 0);

        for(int i=0; i<noteCount; i++){
            String title = sharedPreferences.getString(i + "_title_" + HASHED_EMAIL, "");
            String content = sharedPreferences.getString(i + "_content_" + HASHED_EMAIL, "");

            Note note = new Note();
            note.setTitle(decryptCesar(title, 2));
            note.setContent(content);

            noteList.add(note);
        }
    }


    private void createNoteView(final Note note){
        View noteView = getLayoutInflater().inflate(R.layout.note_item, null);
        TextView noteTitleTextView = noteView.findViewById(R.id.noteTitleTextView);
        TextView noteContentTextView = noteView.findViewById(R.id.noteContentTextView);
        Button deleteNoteDutton = noteView.findViewById(R.id.btnDeleteNote);

        noteTitleTextView.setText(note.getTitle());
        noteContentTextView.setText(note.getContent());

        deleteNoteDutton.setOnClickListener(view -> {
            showDeleteDialog(note);
        });

        noteView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showEditNoteDialog(note);
                return true;
            }
        });

        notesContainer.addView(noteView);
    }

    private void showDeleteDialog(final Note note){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete this note");
        builder.setMessage("Are you sure you want to delete it?");
        builder.setPositiveButton("Delete", (dialogInterface, i) -> deleteNoteAndRefresh(note));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteNoteAndRefresh(Note note){
        noteList.remove(note);
        saveNotesToPreferences("del");
        refreshNotesView();
    }

    private void refreshNotesView(){
        notesContainer.removeAllViews();
        displayNotes();
    }

    private void displayNotes(){
        for(Note note : noteList){
            createNoteView(note);
        }
    }

    private static final byte[] CONSTANT_IV = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };

    // Function to encrypt a message using AES in CBC mode with a constant IV
    public static String encrypt(String data, String key) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // Use the constant IV
            IvParameterSpec ivParameterSpec = new IvParameterSpec(CONSTANT_IV);

            // Create a SecretKeySpec using the provided key
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), ALGORITHM);

            // Initialize the cipher for encryption
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            // Encrypt the data
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());

            // Combine IV and encrypted data for later decryption
            byte[] combined = new byte[CONSTANT_IV.length + encryptedBytes.length];
            System.arraycopy(CONSTANT_IV, 0, combined, 0, CONSTANT_IV.length);
            System.arraycopy(encryptedBytes, 0, combined, CONSTANT_IV.length, encryptedBytes.length);

            // Base64 encode the result for easy storage and transmission
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Function to decrypt a message using AES in CBC mode with a constant IV
    public static String decrypt(String encryptedData, String key) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // Decode the Base64-encoded input
            byte[] combined = Base64.getDecoder().decode(encryptedData);

            // Extract IV from the combined data
            byte[] iv = new byte[CONSTANT_IV.length];
            System.arraycopy(combined, 0, iv, 0, CONSTANT_IV.length);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            // Create a SecretKeySpec using the provided key
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), ALGORITHM);

            // Initialize the cipher for decryption
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            // Decrypt the data
            byte[] decryptedBytes = cipher.doFinal(combined, CONSTANT_IV.length, combined.length - CONSTANT_IV.length);

            return new String(decryptedBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Function to encrypt a message using Caesar Cipher
    public static String encryptCesar(String message, int key) {
        StringBuilder encryptedMessage = new StringBuilder();

        for (char character : message.toCharArray()) {
            if (Character.isLetter(character)) {
                char base = Character.isUpperCase(character) ? 'A' : 'a';
                char encryptedChar = (char) ((character - base + key) % 26 + base);
                encryptedMessage.append(encryptedChar);
            } else {
                // Keep non-alphabetic characters unchanged
                encryptedMessage.append(character);
            }
        }

        return encryptedMessage.toString();
    }

    // Function to decrypt a message using Caesar Cipher with a key
    public static String decryptCesar(String encryptedMessage, int key) {
        return encryptCesar(encryptedMessage, 26 - (key % 26)); // Decryption is equivalent to shifting in the opposite direction
    }






}

