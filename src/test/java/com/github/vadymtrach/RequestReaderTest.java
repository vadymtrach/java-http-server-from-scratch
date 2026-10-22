package com.github.vadymtrach;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequestReaderTest {
    @Test
    void throwsWhenStreamIsEmpty(){
        InputStream input = InputStream.nullInputStream();
        assertThrows(IOException.class, () -> RequestReader.readRequestHead(input));
    }

    @Test
    void returnsHeaderLinesForRequestWithoutBody() throws IOException {
        String request = "GET / HTTP/1.1\r\nHost: localhost:8080\r\nUser-Agent: curl/8.18.0\r\nAccept: */*\r\n\r\n";
        List<String> expected = List.of("GET / HTTP/1.1",
                "Host: localhost:8080",
                "User-Agent: curl/8.18.0",
                "Accept: */*");

        InputStream input = createInputStream(request);
        List<String> actual = RequestReader.readRequestHead(input);

        assertEquals(expected, actual);
    }

    @Test
    void leavesBodyUnreadInStream() throws IOException {
        String expected = "Hello, World!";
        String request = "POST / HTTP/1.1\r\nHost: localhost:8080\r\nUser-Agent: curl/8.18.0\r\nAccept: */*\r\nContent-Length: %d\r\nContent-Type: application/x-www-form-urlencoded\r\n\r\n%s"
                .formatted(expected.getBytes(StandardCharsets.UTF_8).length, expected);

        InputStream input = createInputStream(request);
        RequestReader.readRequestHead(input);

        String body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(expected, body);
    }

    @Test
    void throwsWhenStreamEndsBeforeEmptyLine(){
        String request = "GET / HTTP/1.1\r\nHost: localhost:8080\r\nUser-Agent: curl/8.18.0\r\n";
        InputStream input = createInputStream(request);
        assertThrows(IOException.class, () -> RequestReader.readRequestHead(input));
    }

    @Test
    void throwsWhenStreamEndsMidLine(){
        String request = "GET / HTTP/1.1\r\nHost: localhost:8080\r\nUser-Ag";
        InputStream input = createInputStream(request);
        assertThrows(IOException.class, () -> RequestReader.readRequestHead(input));
    }

    @Test
    void acceptsLineFeedWithoutCarriageReturn() throws IOException {
        String request = "GET / HTTP/1.1\nHost: localhost:8080\nUser-Agent: curl/8.18.0\nAccept: */*\n\n";
        List<String> expected = List.of("GET / HTTP/1.1",
                "Host: localhost:8080",
                "User-Agent: curl/8.18.0",
                "Accept: */*");

        InputStream input = createInputStream(request);
        List<String> actual = RequestReader.readRequestHead(input);

        assertEquals(expected, actual);
    }

    @Test
    void skipsEmptyLinesBeforeRequestLine() throws IOException {
        String request = "\r\n\r\n\r\nGET / HTTP/1.1\r\nHost: localhost:8080\r\nUser-Agent: curl/8.18.0\r\nAccept: */*\r\n\r\n";
        List<String> expected = List.of("GET / HTTP/1.1",
                "Host: localhost:8080",
                "User-Agent: curl/8.18.0",
                "Accept: */*");

        InputStream input = createInputStream(request);
        List<String> actual = RequestReader.readRequestHead(input);
        assertEquals(expected, actual);

    }



    private static InputStream createInputStream(String string){
        return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
    }
}