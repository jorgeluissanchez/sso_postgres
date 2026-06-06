package com.co.lowcode.sso.model;

import java.util.Date;

public class ChatMessage {
    public String message;
    public String from;
    public Date time;

    public ChatMessage() {
    }

    public ChatMessage(String message, String from, Date time) {
        this.message = message;
        this.from = from;
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

    
}
