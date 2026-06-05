package com.fantasyrealm.repository;
import com.fantasyrealm.model.entity.FriendshipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FriendshipJpaRepository extends JpaRepository<FriendshipEntity,Long> {
    boolean existsByCharAAndCharB(Long charA, Long charB);

    @Query("SELECT f FROM FriendshipEntity f WHERE f.charA=:id OR f.charB=:id")
    List<FriendshipEntity> findAllForCharacter(@Param("id") Long id);
}
