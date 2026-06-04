package com.fantasyrealm.repository;
import com.fantasyrealm.model.entity.CharacterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterJpaRepository extends JpaRepository<CharacterEntity,Long> {
    Optional<CharacterEntity> findByName(String name);
    Optional<CharacterEntity> findByPlayerId(Long playerId);
    List<CharacterEntity> findByZoneId(int zoneId);
    boolean existsByName(String name);

    @Query("SELECT c FROM CharacterEntity c ORDER BY c.fameFashion DESC LIMIT :lim")
    List<CharacterEntity> fashionTop(@Param("lim") int lim);

    @Query("SELECT c FROM CharacterEntity c ORDER BY c.fameFishing DESC LIMIT :lim")
    List<CharacterEntity> fishingTop(@Param("lim") int lim);

    @Query("SELECT c FROM CharacterEntity c ORDER BY c.gold DESC LIMIT :lim")
    List<CharacterEntity> wealthTop(@Param("lim") int lim);

    @Modifying @Transactional
    @Query("UPDATE CharacterEntity c SET c.posX=:x,c.posY=:y,c.zoneId=:z,c.lastSeen=:t WHERE c.id=:id")
    void savePosition(@Param("id") Long id,@Param("x") float x,@Param("y") float y,
                      @Param("z") int z,@Param("t") Instant t);

    @Modifying @Transactional
    @Query("UPDATE CharacterEntity c SET c.gold=:g WHERE c.id=:id")
    void saveGold(@Param("id") Long id,@Param("g") long g);

    @Modifying @Transactional
    @Query("UPDATE CharacterEntity c SET c.outfitJson=:o WHERE c.id=:id")
    void saveOutfit(@Param("id") Long id,@Param("o") String o);

    @Modifying @Transactional
    @Query("UPDATE CharacterEntity c SET c.exp=:e,c.level=:l WHERE c.id=:id")
    void saveExpLevel(@Param("id") Long id,@Param("e") long e,@Param("l") int l);

    @Modifying @Transactional
    @Query("UPDATE CharacterEntity c SET c.playTimeSecs=c.playTimeSecs+:s WHERE c.id=:id")
    void addPlayTime(@Param("id") Long id,@Param("s") long s);
}
