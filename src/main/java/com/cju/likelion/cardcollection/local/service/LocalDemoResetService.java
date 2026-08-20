package com.cju.likelion.cardcollection.local.service;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"local", "test", "prod"})
public class LocalDemoResetService {

    private static final String DEMO_QR_CONDITION = "qr_token LIKE 'MCM-DEMO-2026-%'";
    private static final String DEMO_CARD_CONDITION = "purchase_qr_id IN (SELECT id FROM purchase_qrs WHERE " + DEMO_QR_CONDITION + ")";

    private final EntityManager entityManager;

    public LocalDemoResetService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public int reset() {
        // 데모 QR 11개에 연결된 카드 이력만 지우고, 해당 QR만 다시 미사용 상태로 되돌린다.
        String demoCardIds = "SELECT id FROM cards WHERE " + DEMO_CARD_CONDITION;
        entityManager.createNativeQuery("DELETE FROM collection_cards WHERE card_id IN (" + demoCardIds + ")").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM ai_resource_generations WHERE card_id IN (" + demoCardIds + ")").executeUpdate();
        entityManager.createNativeQuery("UPDATE cards SET selected_customization_id = NULL WHERE " + DEMO_CARD_CONDITION).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM card_customizations WHERE card_id IN (" + demoCardIds + ")").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM cards WHERE " + DEMO_CARD_CONDITION).executeUpdate();

        return entityManager.createNativeQuery("""
                UPDATE purchase_qrs
                SET is_used = FALSE,
                    used_by = NULL,
                    used_at = NULL
                WHERE qr_token LIKE 'MCM-DEMO-2026-%'
                """).executeUpdate();
    }
}
