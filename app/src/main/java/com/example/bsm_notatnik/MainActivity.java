package com.example.bsm_notatnik;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity {
    Button buttonLogout, buttonChangePassword, buttonAddNewNote;
    private static final String SHARED_NAME_CREDENTIALS = "Credentials";
    private static final String SHARED_NAME_NOTES = "Notes";
    private static String HASHED_EMAIL = "";
    private static String KEY = "";
    private List<Note> noteList;
    private LinearLayout notesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String current_username_hashed = intent.getStringExtra("CURRENT_USER_EMAIL_HASH");
        HASHED_EMAIL = current_username_hashed;
        KEY = intent.getStringExtra("KEY");

        notesContainer = findViewById(R.id.notesContainer);
        noteList = new ArrayList<>();
        try {
            loadNotesFromPreferencesToList();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        displayNotes();

        buttonLogout = findViewById(R.id.btn_logout);
        buttonChangePassword = findViewById(R.id.btn_change_password);
        buttonAddNewNote = findViewById(R.id.btn_add_note);

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
                try {
                    updatePassword(hashedEmail, newPassword);
                } catch (InvalidAlgorithmParameterException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchPaddingException e) {
                    throw new RuntimeException(e);
                } catch (IllegalBlockSizeException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                } catch (BadPaddingException e) {
                    throw new RuntimeException(e);
                } catch (InvalidKeyException e) {
                    throw new RuntimeException(e);
                }

                Toast.makeText(MainActivity.this, "Password Changed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "New passwords don't match!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
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

                try {
                    saveNotesToPreferences("add");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                } catch (InvalidAlgorithmParameterException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchPaddingException e) {
                    throw new RuntimeException(e);
                } catch (IllegalBlockSizeException e) {
                    throw new RuntimeException(e);
                } catch (BadPaddingException e) {
                    throw new RuntimeException(e);
                } catch (InvalidKeyException e) {
                    throw new RuntimeException(e);
                }
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
                try {
                    deleteNoteAndRefresh(note);
                } catch (InvalidAlgorithmParameterException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchPaddingException e) {
                    throw new RuntimeException(e);
                } catch (IllegalBlockSizeException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                } catch (BadPaddingException e) {
                    throw new RuntimeException(e);
                } catch (InvalidKeyException e) {
                    throw new RuntimeException(e);
                }

                note.setTitle(title);
                note.setContent(content);

                noteList.add(note);

                try {
                    saveNotesToPreferences("add");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                } catch (InvalidAlgorithmParameterException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchPaddingException e) {
                    throw new RuntimeException(e);
                } catch (IllegalBlockSizeException e) {
                    throw new RuntimeException(e);
                } catch (BadPaddingException e) {
                    throw new RuntimeException(e);
                } catch (InvalidKeyException e) {
                    throw new RuntimeException(e);
                }
                createNoteView(note);
            }else {
                Toast.makeText(MainActivity.this, "Enter title and content!", Toast.LENGTH_SHORT).show();
            }


        });

        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void showDeleteDialog(final Note note){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete this note");
        builder.setMessage("Are you sure you want to delete it?");
        builder.setPositiveButton("Delete", (dialogInterface, i) -> {
            try {
                deleteNoteAndRefresh(note);
            } catch (InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e);
            } catch (NoSuchPaddingException e) {
                throw new RuntimeException(e);
            } catch (IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeySpecException e) {
                throw new RuntimeException(e);
            } catch (BadPaddingException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }





    private boolean validatePassword(String password){
        final String PASSWORD_PATTERN = "^.{6,}$";
        Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
        Matcher matcher = pattern.matcher(password);

        return matcher.matches();
    }

    private void updatePassword(String hashedEmail, String newPassword) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        byte[] newSalt = Utility.generateSalt();
        String newSaltString = Base64.getEncoder().encodeToString(newSalt);
        editor.putString("salt_" + hashedEmail, newSaltString);

        String hashedNewPassword = Utility.hashCredential(newPassword, newSalt, 1000);
        editor.putString("user_" + hashedEmail, hashedNewPassword);
        editor.apply();

        KEY = newPassword;
        saveNotesToPreferences("");
    }

    private boolean validateOldPassword(String hashedEmail, String oldPassword){
        byte[] salt = getSaltForUser(hashedEmail, false);
        String hashedOldPassword = Utility.hashCredential(oldPassword, salt, 1000);
        String hashedCorrectPassword = gerPasswrodHashFromShared(hashedEmail);

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

    private String gerPasswrodHashFromShared(String hashedEmail){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_CREDENTIALS, MODE_PRIVATE);
        return sharedPreferences.getString("user_" + hashedEmail, "err");
    }




    private void saveNotesToPreferences(String mode) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_NOTES, MODE_PRIVATE);
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
            editor.putString(i + "_title_" + HASHED_EMAIL, encrypt("AES/CBC/PKCS5Padding", note.getTitle(), getKeyFromPassword(KEY, getSaltForUser(HASHED_EMAIL, true)), generateIv()));
            editor.putString(i + "_content_" + HASHED_EMAIL, note.getContent());

        }

        editor.apply();
    }

    public static SecretKey getKeyFromPassword(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

        return secret;
    }


    private void loadNotesFromPreferencesToList() throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_NOTES, MODE_PRIVATE);
        int noteCount = sharedPreferences.getInt("notecount_" + HASHED_EMAIL, 0);

        for(int i=0; i<noteCount; i++){
            String title = sharedPreferences.getString(i + "_title_" + HASHED_EMAIL, "");
            String content = sharedPreferences.getString(i + "_content_" + HASHED_EMAIL, "");

            Note note = new Note();
            note.setTitle(decrypt("AES/CBC/PKCS5Padding", title, getKeyFromPassword(KEY, getSaltForUser(HASHED_EMAIL, true)), generateIv()) );
            note.setContent(content);

            noteList.add(note);
        }
    }

    private static final byte[] FIXED_IV = {
            0x01, 0x02, 0x03, 0x04,
            0x05, 0x06, 0x07, 0x08,
            0x09, 0x0A, 0x0B, 0x0C,
            0x0D, 0x0E, 0x0F, 0x10
    };

    public static IvParameterSpec generateIv() {
        return new IvParameterSpec(Arrays.copyOf(FIXED_IV, FIXED_IV.length));
    }

    public static String encrypt(String algorithm, String input, SecretKey key, IvParameterSpec iv) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return Base64.getEncoder().encodeToString(cipherText);
    }

    public static String decrypt(String algorithm, String cipherText, SecretKey key, IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException{

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
        return new String(plainText);
    }


    private void createNoteView(final Note note){
        View noteView = getLayoutInflater().inflate(R.layout.note_item, null);
        TextView noteTitleTextView = noteView.findViewById(R.id.noteTitleTextView);
        TextView noteContentTextView = noteView.findViewById(R.id.noteContentTextView);
        Button deleteNoteDutton = noteView.findViewById(R.id.btnDeleteNote);

        noteTitleTextView.setText(note.getTitle());
        noteContentTextView.setText(note.getContent());

        deleteNoteDutton.setOnClickListener(view -> showDeleteDialog(note));

        noteView.setOnLongClickListener(view -> {
            showEditNoteDialog(note);
            return true;
        });

        notesContainer.addView(noteView);
    }



    private void deleteNoteAndRefresh(Note note) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
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




}

