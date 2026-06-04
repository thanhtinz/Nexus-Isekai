package com.fantasyrealm.repository;
import com.fantasyrealm.model.entity.MailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface MailJpaRepository extends JpaRepository<MailEntity,Long> {
    List<MailEntity> findByToIdOrderBySentAtDesc(Long toId);
    long countByToIdAndReadFalse(Long toId);

    @Modifying @Transactional
    @Query("UPDATE MailEntity m SET m.read=true WHERE m.id=:id AND m.toId=:toId")
    void markRead(@Param("id") Long id,@Param("toId") Long toId);

    @Modifying @Transactional
    @Query("DELETE FROM MailEntity m WHERE m.expiresAt < CURRENT_TIMESTAMP")
    int deleteExpired();
}
