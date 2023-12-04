package com.example.bsm_notatnik;

public class Note {
    private String title;
    private String content;

    private int id;

    public Note(){

    }

    public Note(String title, String content, int id){
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
