package com.github.vadymtrach;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class Main {
    static void main() throws IOException {

        try(ServerSocket serverSocket = new ServerSocket(8080, 2)){
            while(true){
                try(Socket socket = serverSocket.accept()) {
                    InputStream input = socket.getInputStream();
                    List<String> requestLines = RequestReader.readRequestHead(input);

                    requestLines.forEach(System.out::println);
                } catch (Exception e){
                    System.out.println(e);
                }
                System.out.println("-".repeat(20));
            }

        }
    }


}
