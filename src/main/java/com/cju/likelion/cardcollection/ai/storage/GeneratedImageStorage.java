package com.cju.likelion.cardcollection.ai.storage;

import java.util.UUID;

public interface GeneratedImageStorage {

    String store(UUID resourceId, byte[] imageBytes, String contentType);
}
