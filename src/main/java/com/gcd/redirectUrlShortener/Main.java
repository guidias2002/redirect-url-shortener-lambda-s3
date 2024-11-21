package com.gcd.redirectUrlShortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Main implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final S3Client s3Client = S3Client.builder().build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        String pathParameters = (String) input.get("rawPath");
        String shortUrlCode = pathParameters.replace("/", "");

        if (shortUrlCode == null || shortUrlCode.isEmpty()) {
            throw new IllegalArgumentException("Invalid input: 'shortUrlCode' is required.");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket("url-shortener-storage-gcd")
                .key(shortUrlCode + ".json")
                .build();

        InputStream s3ObjectStream;

        try {
            s3ObjectStream = s3Client.getObject(getObjectRequest);
        } catch (Exception ex) {
            throw new RuntimeException("Error getching data from S3: " + ex.getMessage(), ex);
        }

        UrlData urlData;

        try {
            urlData = objectMapper.readValue(s3ObjectStream, UrlData.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error deserializing URL data: " + ex.getMessage(), ex);
        }

        long currentTimeInSeconds = System.currentTimeMillis() / 1000;

        Map<String, Object> response = new HashMap<>();


        // url expirou
        if (urlData.getExpirationTime() < currentTimeInSeconds) {
            response.put("statusCode", 410);
            response.put("body", "This URL has expired");

            return response;
        }

        // url ainda é válida
        response.put("statusCode", 302);

        Map<String, String> headers = new HashMap<>();
        headers.put("location", urlData.getOriginalUrl());
        response.put("headers", headers);

        return response;
    }
}






