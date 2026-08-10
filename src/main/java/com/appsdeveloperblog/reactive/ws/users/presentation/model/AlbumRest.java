package com.appsdeveloperblog.reactive.ws.users.presentation.model;

public class AlbumRest {
    private String id;
    private String title;

    public AlbumRest() {
    }

    public AlbumRest(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
