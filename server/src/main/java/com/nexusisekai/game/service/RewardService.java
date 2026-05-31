package com.nexusisekai.game.service;

import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;

/** RewardService — Phát thưởng tập trung (gold/diamond/item/exp) per character */
public class RewardService {

    public static void grant(Connection c, long charId, String type, int amount) throws Exception {
        switch (type) {
            case "gold" -> SqlSafe.update(c, "UPDATE characters SET gold=gold+? WHERE id=?", amount, charId);
            case "diamond" -> SqlSafe.update(c, "UPDATE characters SET diamond=diamond+? WHERE id=?", amount, charId);
            case "exp" -> SqlSafe.update(c, "UPDATE characters SET exp=exp+? WHERE id=?", amount, charId);
            case "item" -> SqlSafe.update(c,
                "INSERT INTO inventory (char_id, item_id, quantity) VALUES (?,?,1) " +
                "ON DUPLICATE KEY UPDATE quantity=quantity+1", charId, amount);
            case "ticket_standard", "ticket_limited", "key_pet", "key_mount", "shard_weapon" -> {
                int currencyId = switch (type) {
                    case "ticket_standard" -> 1; case "ticket_limited" -> 2;
                    case "key_pet" -> 3; case "key_mount" -> 4; default -> 5;
                };
                SqlSafe.update(c,
                    "INSERT INTO player_gacha_currency (char_id, currency_id, amount, total_earned) VALUES (?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE amount=amount+?, total_earned=total_earned+?",
                    charId, currencyId, amount, amount, amount, amount);
            }
            default -> {}
        }
    }
}
