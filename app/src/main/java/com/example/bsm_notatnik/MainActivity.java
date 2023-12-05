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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class MainActivity extends AppCompatActivity {

    Button buttonLogout, buttonChangePassword, buttonAddNewNote;
    private static final String SHARED_NAME_CREDENTIALS = "Credentials";
    private static final String SHARED_NOTES_NAME = "Notes";
    private static String HASHED_EMAIL = "";
    private List<Note> noteList;
    private LinearLayout notesContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String current_username_hashed = intent.getStringExtra("CURRENT_USER_EMAIL_HASH");
        HASHED_EMAIL = current_username_hashed;

        notesContainer = findViewById(R.id.notesContainer);
        noteList = new ArrayList<>();
        loadNotesFromPreferencesToList();
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

        String hashedNewPassword = Utility.hashCredential(newPassword, newSalt);
        editor.putString("user_" + hashedEmail, hashedNewPassword);
        editor.apply();
    }

    private boolean validateOldPassword(String hashedEmail, String oldPassword){
        byte[] salt = getSaltForUser(hashedEmail);
        String hashedOldPassword = Utility.hashCredential(oldPassword, salt);
        String hashedCorrectPassword = getPasswordFromShared(hashedEmail);

        assert hashedOldPassword != null;
        return hashedOldPassword.equals(hashedCorrectPassword);
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

    private void genSecretKey(){

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
            editor.putString(i + "_title_" + HASHED_EMAIL, note.getTitle());
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
            note.setTitle(title);
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





















}

