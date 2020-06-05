package com.kmit.transitsearch;

import java.io.Serializable;

public class Message implements Serializable, AutoCloseable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String text;

    public Message(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
		
	}
    
	/*
	 * @Override public void close() {
	 * 
	 * }
	 */
}