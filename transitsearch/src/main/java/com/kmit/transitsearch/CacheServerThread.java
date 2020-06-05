package com.kmit.transitsearch;

import java.net.*;
import java.util.concurrent.ExecutionException;
import java.io.*;
 
public class CacheServerThread extends Thread {
    private Socket socket = null;
    CacheManagement cacheManager = null;
 
    public CacheServerThread(Socket socket) {
        super("CacheServer Multithreaded");
        this.socket = socket;
        cacheManager = new CacheManagement ();
    }
     
    public void run() {
    	 
        try (
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(
                    socket.getInputStream()));

        ) {
            String inputLine, outputLine;
            outputLine = cacheManager.processInput(null);
            out.println(outputLine);
 
            while ((inputLine = in.readLine()) != null) {
                outputLine = cacheManager.processInput(inputLine);
                out.println(outputLine);
                if (outputLine.equals("Bye"))
                    break;
            }
            socket.close();
        } catch (IOException | ExecutionException e) {
            e.printStackTrace();
        }
    }
    
}