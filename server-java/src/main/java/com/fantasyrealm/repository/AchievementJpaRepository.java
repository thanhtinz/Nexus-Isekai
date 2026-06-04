package com.fantasyrealm.repository;
import com.fantasyrealm.model.entity.AchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AchievementJpaRepository extends JpaRepository<AchievementEntity,Long> {
    List<AchievementEntity> findByCharacterId(Long characterId);
    boolean existsByCharacterIdAndAchievementCode(Long characterId,String code);
    long countByCharacterId(Long characterId);
}
