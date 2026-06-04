package com.fantasyrealm.repository;
import com.fantasyrealm.model.entity.MarketListingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface MarketListingJpaRepository extends JpaRepository<MarketListingEntity,Long> {
    List<MarketListingEntity> findByActiveTrue();
    List<MarketListingEntity> findByItemIdAndActiveTrue(Long itemId);
    List<MarketListingEntity> findBySellerIdAndActiveTrue(Long sellerId);

    @Modifying @Transactional
    @Query("UPDATE MarketListingEntity m SET m.active=false WHERE m.id=:id")
    void deactivate(@Param("id") Long id);

    @Modifying @Transactional
    @Query("UPDATE MarketListingEntity m SET m.quantity=:q WHERE m.id=:id")
    void updateQuantity(@Param("id") Long id,@Param("q") int q);

    @Modifying @Transactional
    @Query("DELETE FROM MarketListingEntity m WHERE m.expiresAt < CURRENT_TIMESTAMP")
    int deleteExpired();
}
