package com.cju.likelion.cardcollection.ai.provider;

public class AiProviderException extends RuntimeException {

    private final boolean rejected;

    private AiProviderException(String message, Throwable cause, boolean rejected) {
        super(message, cause);
        this.rejected = rejected;
    }

    public static AiProviderException failed(String message, Throwable cause) {
        return new AiProviderException(message, cause, false);
    }

    public static AiProviderException rejected(String message) {
        return new AiProviderException(message, null, true);
    }

    public boolean isRejected() {
        return rejected;
    }
}
