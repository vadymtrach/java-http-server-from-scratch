package com.github.vadymtrach;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestReaderTest {
    @Nested
    class ReadRequestHead{
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
    }

    @Nested
    class ReadBody{
        @Test
        void returnsWholeBodyWhenLengthMatchesData() throws IOException {
            String expected = "Hello, World!";
            byte[] expectedBody = expected.getBytes(StandardCharsets.UTF_8);

            InputStream input = createInputStream(expected);
            byte[] body = RequestReader.readBody(input, expectedBody.length);
            assertArrayEquals(expectedBody, body);
        }

        @Test
        void leavesRestOfStreamUnread() throws IOException {
            String body = "Hello, World!";
            String tail = "GET / HTTP/1.1\r\nHost: localhost:8080";
            String request = body + tail;
            InputStream input = createInputStream(request);

            byte[] expectedBody = body.getBytes(StandardCharsets.UTF_8);

            byte[] actualBody = RequestReader.readBody(input, expectedBody.length);
            byte[] actualTail = input.readAllBytes();

            assertArrayEquals(expectedBody, actualBody);
            assertArrayEquals(tail.getBytes(StandardCharsets.UTF_8), actualTail);
        }

        @Test
        void throwsWhenStreamEndsBeforeBodyIsComplete(){
            String body = "Hello, World!";
            int bodyLength = body.getBytes(StandardCharsets.UTF_8).length;
            int missingBytes = 1;
            InputStream input = createInputStream(body);

            assertThrows(IOException.class, () -> RequestReader.readBody(input, bodyLength + missingBytes));
        }

        @Test
        void returnsEmptyBodyWhenLengthIsZero() throws IOException {
            String tail = "GET / HTTP/1.1\r\nHost: localhost:8080";

            InputStream input = createInputStream(tail);
            byte[] actualBody = RequestReader.readBody(input, 0);
            byte[] actualTail  = input.readAllBytes();
            assertArrayEquals(new byte[0], actualBody);
            assertArrayEquals(tail.getBytes(StandardCharsets.UTF_8), actualTail);
        }
    }

    @Nested
    class ParseHeaders{
        @Test
        void returnsValuesByHeaderName() throws BadRequestException {
            Map<String, List<String>> expectedMap = Map.of("Host", List.of("localhost:8080"),
                    "Content-Length", List.of("3"),
                    "User-Agent", List.of("curl/8.6.0"));

            List<String> lines = List.of("Host:localhost:8080", "Content-Length:3", "User-Agent:curl/8.6.0");

            Map<String, List<String>> headers = RequestReader.parseHeaders(lines);
            assertEquals(expectedMap, headers);
        }

        @Test
        void stripsSpacesAroundValue() throws BadRequestException {
            Map<String, List<String>> expected = Map.of("Host", List.of("localhost"));
            List<String> lines = List.of("Host:    localhost    ");

            Map<String, List<String>> actual = RequestReader.parseHeaders(lines);
            assertEquals(expected, actual);
        }

        @Test
        void stripsTabsAroundValue() throws BadRequestException {
            Map<String, List<String>> expected = Map.of("Host", List.of("localhost"));
            List<String> lines = List.of("Host:\tlocalhost\t");
            Map<String, List<String>> actual = RequestReader.parseHeaders(lines);
            assertEquals(expected, actual);
        }

        @Test
        void keepsNonBreakingSpaceAroundValue() throws BadRequestException {
            Map<String, List<String>> expected = Map.of("Host", List.of("\u00A0localhost\u00A0"));
            List<String> lines = List.of("Host: \u00A0localhost\u00A0 ");
            Map<String, List<String>> actual = RequestReader.parseHeaders(lines);
            assertEquals(expected, actual);
        }

        @Test
        void acceptsEmptyValue() throws BadRequestException {
            Map<String, List<String>> expected = Map.of("Host", List.of(""));
            List<String> lines = List.of("Host:");
            Map<String, List<String>> actual = RequestReader.parseHeaders(lines);
            assertEquals(expected, actual);
        }

        @Test
        void returnsEmptyValueWhenOnlyWhitespace() throws BadRequestException {
            Map<String, List<String>> expected = Map.of("Host", List.of(""));
            List<String> lines = List.of("Host:\t \t");
            Map<String, List<String>> actual = RequestReader.parseHeaders(lines);
            assertEquals(expected, actual);
        }


        @Test
        void findsHeaderRegardlessOfNameCase() throws BadRequestException {
            List<String> lines = List.of("Host:localhost");

            Map<String, List<String>> actual = RequestReader.parseHeaders(lines);
            assertEquals(List.of("localhost"), actual.get("host"));
            assertEquals(List.of("localhost"), actual.get("HOST"));
        }

        @Test
        void collectsRepeatedHeaderValuesInOrder() throws BadRequestException {
            List<String> lines = List.of("Accept:application/json", "Accept:text/html");
            Map<String, List<String>> expected = Map.of("Accept", List.of("application/json", "text/html"));
            Map<String, List<String>> actual = RequestReader.parseHeaders(lines);
            assertEquals(expected, actual);
        }

        @Test
        void collectsRepeatedHeaderValuesInOrderRegardlessOfNameCase() throws BadRequestException {
            List<String> lines = List.of("accept:application/json", "Accept:text/html");
            List<String> expected = List.of("application/json", "text/html");
            Map<String, List<String>> actual = RequestReader.parseHeaders(lines);
            assertEquals(expected, actual.get("ACCEPT"));
            assertEquals(1, actual.size());
        }

        @ParameterizedTest(name = "{index}: [{0}]")
        @ValueSource(strings = {" Host:localhost", "Host :localhost", "Host\t:localhost", ":localhost", "Host localhost", "Hosя: localhost"})
        void throwsForMalformedHeaderLine(String line){
            List<String> lines = List.of(line);
            assertThrows(BadRequestException.class, () -> RequestReader.parseHeaders(lines));
        }
    }

    private static InputStream createInputStream(String string){
        return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
    }
}