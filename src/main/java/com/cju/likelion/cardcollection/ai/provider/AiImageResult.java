package com.cju.likelion.cardcollection.ai.provider;

public record AiImageResult(byte[] imageBytes, String contentType, String model, String generatedData) {

    public static AiImageResult image(byte[] imageBytes, String contentType, String model) {
        return new AiImageResult(imageBytes, contentType, model, null);
    }

    public static AiImageResult data(String generatedData, String model) {
        return new AiImageResult(null, null, model, generatedData);
    }

    public boolean hasGeneratedData() {
        return generatedData != null && !generatedData.isBlank();
    }
}
