package com.fantasyrealm.repository;
import com.fantasyrealm.model.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface PlayerJpaRepository extends JpaRepository<PlayerEntity,Long> {
    Optional<PlayerEntity> findByUsername(String username);
    Optional<PlayerEntity> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Modifying @Transactional
    @Query("UPDATE PlayerEntity p SET p.lastLogin=:now WHERE p.id=:id")
    void updateLastLogin(@Param("id") Long id, @Param("now") Instant now);

    @Modifying @Transactional
    @Query("UPDATE PlayerEntity p SET p.banned=:b,p.banReason=:r WHERE p.id=:id")
    void updateBanStatus(@Param("id") Long id,@Param("b") boolean b,@Param("r") String r);
}
