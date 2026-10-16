package com.github.vadymtrach;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @Test
    void throwsWhenStreamIsEmpty(){
        InputStream input = InputStream.nullInputStream();
        assertThrows(IOException.class, () -> Main.readRequestHead(input));
    }

    @Test
    void returnsHeaderLinesForRequestWithoutBody() throws IOException {
        String request = "GET / HTTP/1.1\r\nHost: localhost:8080\r\nUser-Agent: curl/8.18.0\r\nAccept: */*\r\n\r\n";

        InputStream input = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
        List<String> expected = List.of("GET / HTTP/1.1",
                "Host: localhost:8080",
                "User-Agent: curl/8.18.0",
                "Accept: */*");

        List<String> actual = Main.readRequestHead(input);

        assertEquals(expected, actual);
    }


}