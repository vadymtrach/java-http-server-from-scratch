package com.github.vadymtrach;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RequestReader {

    static List<String> readRequestHead(InputStream input) throws IOException {
        List<String> requestLines = new ArrayList<>();
        StringBuilder lineBuilder = new StringBuilder();
        int currentByte = input.read();

        while (currentByte != -1){
            if (currentByte == '\n'){
                if (lineBuilder.isEmpty()) {
                    if (!requestLines.isEmpty()){
                        return requestLines;
                    }
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

    static byte[] readBody(InputStream input, int length) throws IOException {
        byte[] body = input.readNBytes(length);
        if (body.length != length) throw new IOException("Connection was terminated unexpectedly");
        return body;
    }
}
