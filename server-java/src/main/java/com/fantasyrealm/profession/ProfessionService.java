package com.fantasyrealm.profession;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProfessionService {
    // playerId -> type -> level
    private final ConcurrentHashMap<Long, Map<ProfessionType,Integer>> levels = new ConcurrentHashMap<>();
    // playerId -> type -> exp
    private final ConcurrentHashMap<Long, Map<ProfessionType,Integer>> exps   = new ConcurrentHashMap<>();

    public int getLevel(long playerId, ProfessionType type) {
        return levels.getOrDefault(playerId, Map.of()).getOrDefault(type, 0);
    }

    public void addExp(long playerId, ProfessionType type, int exp) {
        Map<ProfessionType,Integer> lvlMap = levels.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        Map<ProfessionType,Integer> expMap = exps.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        int curLevel = lvlMap.getOrDefault(type, 0);
        int curExp   = expMap.merge(type, exp, Integer::sum);
        int needed   = (curLevel + 1) * 100;
        if (curExp >= needed && curLevel < type.maxLevel) {
            lvlMap.put(type, curLevel + 1);
            expMap.put(type, curExp - needed);
        }
    }

    public Map<ProfessionType,Integer> getAllLevels(long playerId) {
        return Collections.unmodifiableMap(levels.getOrDefault(playerId, Map.of()));
    }
}
