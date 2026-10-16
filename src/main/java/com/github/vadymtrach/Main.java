package com.github.vadymtrach;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Main {
    static void main() throws IOException {

        try(ServerSocket serverSocket = new ServerSocket(8080, 2)){
            while(true){
                try(Socket socket = serverSocket.accept()) {
                    InputStream input = socket.getInputStream();
                    List<String> requestLines = readRequestHead(input);

                    requestLines.forEach(System.out::println);
                } catch (Exception e){
                    System.out.println(e);
                }
                System.out.println("-".repeat(20));
            }

        }
    }

    static List<String> readRequestHead(InputStream input) throws IOException {
        List<String> requestLines = new ArrayList<>();
        StringBuilder lineBuilder = new StringBuilder();
        int currentByte = input.read();

        while (currentByte != -1){
            if (currentByte == '\n'){
                if (lineBuilder.isEmpty()) {
                    return requestLines;
                } else{
                    requestLines.add(lineBuilder.toString());
                    lineBuilder.setLength(0);
                }

            }
            else if (currentByte != '\r'){
                lineBuilder.append((char) currentByte);
            }
            currentByte = input.read();
        }
        throw new IOException("Connection was terminated unexpectedly");
    }


}
