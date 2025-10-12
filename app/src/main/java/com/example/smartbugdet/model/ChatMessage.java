package com.example.smartbugdet.model;

public class ChatMessage {
    public enum Sender {
        USER, MODEL
    }

    private String message;
    private Sender sender;

    public ChatMessage(String message, Sender sender) {
        this.message = message;
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public Sender getSender() {
        return sender;
    }
}
