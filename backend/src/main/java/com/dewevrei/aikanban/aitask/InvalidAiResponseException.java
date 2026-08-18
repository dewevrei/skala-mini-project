package com.dewevrei.aikanban.aitask;

public final class InvalidAiResponseException extends RuntimeException {
    public InvalidAiResponseException(String message) { super(message); }
    public InvalidAiResponseException(String message, Throwable cause) { super(message, cause); }
}
