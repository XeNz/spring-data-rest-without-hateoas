package com.datarest.demo.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

import java.util.Optional;

public class CustomControllerUtils {
    public static <R> ResponseEntity<?> toResponseEntity(
            HttpStatus status, HttpHeaders headers, Optional<R> resource) {

        HttpHeaders hdrs = new HttpHeaders();

        if (headers != null) {
            hdrs.putAll(headers);
        }

        return new ResponseEntity<>(resource.orElse(null), hdrs, status);
    }

    /**
     * Wrap a resource as a {@link ResponseEntity} and attach given headers and status.
     *
     * @param status
     * @param headers
     * @param resource
     * @param <R>
     * @return
     */
    public static <R> ResponseEntity<?> toResponseEntity(
            HttpStatus status, HttpHeaders headers, R resource) {

        Assert.notNull(status, "Http status must not be null!");
        Assert.notNull(headers, "Http headers must not be null!");
        Assert.notNull(resource, "Payload must not be null!");

        return toResponseEntity(status, headers, Optional.of(resource));
    }

    /**
     * Return an empty response that is only comprised of a status
     *
     * @param status
     * @return
     */
    public static ResponseEntity<?> toEmptyResponse(HttpStatus status) {
        return toEmptyResponse(status, new HttpHeaders());
    }

    /**
     * Return an empty response that is only comprised of headers and a status
     *
     * @param status
     * @param headers
     * @return
     */
    public static ResponseEntity<?> toEmptyResponse(HttpStatus status, HttpHeaders headers) {
        return toResponseEntity(status, headers, Optional.empty());
    }
}
