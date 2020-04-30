package com.example.clinger;

public class Request {
    private String from,type;

    public Request(String from, String type) {
        this.from = from;
        this.type = type;
    }

    public Request(){}

    public void setFrom(String from) {
        this.from = from;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public String getType() {
        return type;
    }
}
