package com.github.vadymtrach;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RequestReader {
    private static final String TOKEN_SPECIAL_CHARS = "!#$%&'*+-.^_`|~";
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

    static Map<String, List<String>> parseHeaders(List<String> lines) throws BadRequestException {
        Map<String, List<String>> headersMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (String line : lines){
            int index = line.indexOf(":");
            if (index == -1) throw new BadRequestException("Header line has no colon: " + line);
            if (index == 0) throw new BadRequestException("Header name is empty");

            String key = line.substring(0, index);
            for (char c : key.toCharArray()){
                if (!isTokenChar(c)) throw new BadRequestException("Invalid character in header name: " + (int) c);
            }

            String value = line.substring(index + 1).strip();
            headersMap.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(value);
        }
        return headersMap;
    }

    private static boolean isTokenChar(char c){
        return (c >= '0' && c <= '9')
                || (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')
                || TOKEN_SPECIAL_CHARS.indexOf(c) != -1;
    }
}
