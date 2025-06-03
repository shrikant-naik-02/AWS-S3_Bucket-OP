package com.excelfore.aws.awstask.exception;

public class PresignedUrlExpiredException extends RuntimeException {
    public PresignedUrlExpiredException(String message) {
        super(message);
    }
}