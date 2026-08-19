package com.cju.likelion.cardcollection.collection.repository;
import com.cju.likelion.cardcollection.collection.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CollectionCardRepository extends JpaRepository<CollectionCard, CollectionCardId> { List<CollectionCard> findByIdCollectionIdOrderByAddedAtDesc(UUID collectionId); long countByIdCollectionId(UUID collectionId); }
