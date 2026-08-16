package com.cju.likelion.cardcollection.auth.repository;

import com.cju.likelion.cardcollection.auth.domain.SocialAccount;
import com.cju.likelion.cardcollection.auth.domain.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

    Optional<SocialAccount> findByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    );
}
