package com.cju.likelion.cardcollection.collection.repository;
import com.cju.likelion.cardcollection.collection.domain.UserCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserCollectionRepository extends JpaRepository<UserCollection, UUID> { List<UserCollection> findByUserIdOrderByUpdatedAtDesc(UUID userId); Optional<UserCollection> findByIdAndUserId(UUID id, UUID userId); }
