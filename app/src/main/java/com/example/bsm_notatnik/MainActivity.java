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

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;


public class MainActivity extends AppCompatActivity {
    Button buttonLogout, buttonChangePassword, buttonAddNewNote;
    private static final String SHARED_NAME_CREDENTIALS = "Credentials";
    private static final String SHARED_NAME_NOTES = "Notes";
    private static String HASHED_EMAIL = "";
    private static String PAS = "";
    private List<Note> noteList;
    private LinearLayout notesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String current_username_hashed = intent.getStringExtra("CURRENT_USER_EMAIL_HASH");
        HASHED_EMAIL = current_username_hashed;
        PAS = intent.getStringExtra("PAS");

        notesContainer = findViewById(R.id.notesContainer);
        noteList = new ArrayList<>();

        try {
            loadNotesFromPreferencesToList();
        } catch (GeneralSecurityException e) {
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
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }
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
                } catch (GeneralSecurityException e) {
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
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }

                note.setTitle(title);
                note.setContent(content);

                noteList.add(note);

                try {
                    saveNotesToPreferences("add");
                } catch (GeneralSecurityException e) {
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
            } catch (GeneralSecurityException e) {
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

        PAS = newPassword;
        saveNotesToPreferences("");
    }

    private boolean validateOldPassword(String hashedEmail, String oldPassword){
        byte[] salt = getSaltForUser(hashedEmail, false);
        String hashedOldPassword = Utility.hashCredential(oldPassword, salt, 1000);
        String hashedCorrectPassword = gerPasswordHashFromShared(hashedEmail);

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

    private String gerPasswordHashFromShared(String hashedEmail){
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

        //tutaj muszę wygenerować randomowy iv. Używam go do enkrypcji i zapisuje do shared jako string
        IvParameterSpec iv = UtilityAES.generateIv();
        String ivString = ivToString(iv);
        saveIvStringToShared(ivString);

        editor.putInt("notecount_" + HASHED_EMAIL, noteList.size());
        for(int i=0; i<noteList.size(); i++){
            Note note = noteList.get(i);
            editor.putString(i + "_title_" + HASHED_EMAIL, UtilityAES.encrypt("AES/CBC/PKCS5Padding", note.getTitle(), UtilityAES.getKeyFromPassword(PAS, getSaltForUser(HASHED_EMAIL, true)), iv));
            editor.putString(i + "_content_" + HASHED_EMAIL, note.getContent());

        }

        editor.apply();
    }


    private void loadNotesFromPreferencesToList() throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_NOTES, MODE_PRIVATE);
        int noteCount = sharedPreferences.getInt("notecount_" + HASHED_EMAIL, 0);

        //tutaj muszę pobrać iv z shared i skonvertować do dobrego formatu
        String ivString = getIVStringFromShared();
        IvParameterSpec iv = stringToIv(ivString);

        for(int i=0; i<noteCount; i++){
            String title = sharedPreferences.getString(i + "_title_" + HASHED_EMAIL, "");
            String content = sharedPreferences.getString(i + "_content_" + HASHED_EMAIL, "");

            Note note = new Note();
            note.setTitle(UtilityAES.decrypt("AES/CBC/PKCS5Padding", title, UtilityAES.getKeyFromPassword(PAS, getSaltForUser(HASHED_EMAIL, true)), iv) );
            note.setContent(content);

            noteList.add(note);
        }

    }

    private void saveIvStringToShared(String ivString){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_NOTES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("iv_" + HASHED_EMAIL, ivString);

        editor.apply();
    }

    private String getIVStringFromShared(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_NAME_NOTES, MODE_PRIVATE);
        String ivString = sharedPreferences.getString("iv_" + HASHED_EMAIL, "err");
        return ivString;
    }

    private static IvParameterSpec stringToIv(String ivString) {
        byte[] ivBytes = Base64.getDecoder().decode(ivString);
        return new IvParameterSpec(ivBytes);
    }

    private static String ivToString(IvParameterSpec ivParameterSpec) {
        byte[] ivBytes = ivParameterSpec.getIV();
        return Base64.getEncoder().encodeToString(ivBytes);
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

