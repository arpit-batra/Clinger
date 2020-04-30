package com.example.clinger;

public class Message {
    private String message,seen,time,type,from;

    public Message(String message, String seen, String time, String type, String from) {
        this.message = message;
        this.seen = seen;
        this.time = time;
        this.type = type;
        this.from = from;
    }

    public Message() {
    }

    public String getMessage() {
        return message;
    }

    public String getSeen() {
        return seen;
    }

    public String getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSeen(String seen) {
        this.seen = seen;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
