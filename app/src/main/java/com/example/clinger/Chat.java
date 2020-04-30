package com.example.clinger;

public class Chat {
    private String seen, timestamp;

    public Chat(){

    }

    public Chat(String seen, String timestamp) {
        this.seen = seen;
        this.timestamp = timestamp;
    }

    public String getSeen() {
        return seen;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setSeen(String seen) {
        this.seen = seen;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
