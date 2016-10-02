package com.example.dimebag.identifybookapp;

/**
 * @author Radu Jica
 * @version 1.0
 *
 * DisplayRecord entity with details for a single book entry as stored in the local DB
 * for book displays.
 */
public class DisplayRecord {

    private int id;
    private String displayName;
    private String bookISBN;
    private String timestamp;
    private String androidId;

    public DisplayRecord() {}

    public DisplayRecord(int id, String displayName, String bookISBN, String timestamp, String androidId) {
        this.id = id;
        this.displayName = displayName;
        this.bookISBN = bookISBN;
        this.timestamp = timestamp;
        this.androidId = androidId;
    }

    public DisplayRecord(String displayName, String bookISBN, String timestamp, String androidId) {
        this.displayName = displayName;
        this.bookISBN = bookISBN;
        this.timestamp = timestamp;
        this.androidId = androidId;
    }

    // Getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBookISBN() {
        return bookISBN;
    }

    public void setBookISBN(String bookISBN) {
        this.bookISBN = bookISBN;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getAndroidId() {
        return androidId;
    }

    public void setAndroidId(String androidId) {
        this.androidId = androidId;
    }
}
