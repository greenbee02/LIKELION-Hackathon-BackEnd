package com.cju.likelion.cardcollection.catalog.repository;

import com.cju.likelion.cardcollection.catalog.domain.CardDesignAsset;
import com.cju.likelion.cardcollection.catalog.domain.CardDesignAssetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CardDesignAssetRepository extends JpaRepository<CardDesignAsset, UUID> {

    List<CardDesignAsset> findByProductIdAndAssetTypeAndActiveTrueOrderByVariantCodeAsc(
            UUID productId,
            CardDesignAssetType assetType
    );

    List<CardDesignAsset> findByBrandIdAndAssetTypeAndActiveTrueOrderByVariantCodeAsc(
            UUID brandId,
            CardDesignAssetType assetType
    );
}
