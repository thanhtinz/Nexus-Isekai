// NexusIsekai Client - PacketBuilder.cs & PacketReader.cs
// Tương đương ByteBuf Netty ở server

using System;
using System.IO;
using System.Text;

namespace NexusIsekai.Network
{
    /// <summary>Tạo nhân vật — chọn class + giới tính (giống NRO)</summary>
        public static void SendCharCreate(string name, int classId, int gender)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAR_CREATE)
                .WriteString(name).WriteByte((byte)classId).WriteByte((byte)gender)); // 1-5

        public static void SendCharDelete(long charId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAR_DELETE).WriteLong(charId));

        public static void SendCharSelect(long charId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAR_SELECT).WriteLong(charId));

        // ── WORLD / MOVEMENT ─────────────────────────────────────

        public static void SendMove(float x, float y, byte direction)
            => Send(new PacketBuilder(PacketOpcode.C2S_MOVE)
                .WriteFloat(x).WriteFloat(y).WriteByte(direction));

        public static void SendMapChange(int portalId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MAP_CHANGE).WriteInt(portalId));

        public static void SendMapLoadDone()
            => Send(new PacketBuilder(PacketOpcode.C2S_MAP_LOAD_DONE));

        // ── COMBAT ───────────────────────────────────────────────

        public static void SendAttack(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_ATTACK).WriteLong(targetId));

        public static void SendUseSkill(int skillId, long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_USE_SKILL)
                .WriteInt(skillId).WriteLong(targetId));

        // ── INVENTORY ────────────────────────────────────────────

        public static void SendInventoryList()
            => Send(new PacketBuilder(PacketOpcode.C2S_INVENTORY_OPEN));

        public static void SendUseItem(long instanceId)
            => Send(new PacketBuilder(PacketOpcode.C2S_USE_ITEM).WriteLong(instanceId));

        public static void SendEquipItem(long instanceId)
            => Send(new PacketBuilder(PacketOpcode.C2S_EQUIP_ITEM).WriteLong(instanceId));

        public static void SendUnequipItem(int slot)
            => Send(new PacketBuilder(PacketOpcode.C2S_UNEQUIP_ITEM).WriteInt(slot));

        public static void SendShopOpen(int shopId)
            => Send(new PacketBuilder(PacketOpcode.C2S_SHOP_OPEN).WriteInt(shopId));

        public static void SendShopBuy(int itemId, int qty)
            => Send(new PacketBuilder(PacketOpcode.C2S_SHOP_BUY)
                .WriteInt(itemId).WriteInt(qty));

        public static void SendShopSell(long instanceId, int qty)
            => Send(new PacketBuilder(PacketOpcode.C2S_SHOP_SELL)
                .WriteLong(instanceId).WriteInt(qty));

        // ── QUEST ────────────────────────────────────────────────

        public static void SendQuestList()
            => Send(new PacketBuilder(PacketOpcode.C2S_QUEST_LIST));

        public static void SendQuestAccept(int questId)
            => Send(new PacketBuilder(PacketOpcode.C2S_QUEST_ACCEPT).WriteInt(questId));

        public static void SendQuestComplete(int questId)
            => Send(new PacketBuilder(PacketOpcode.C2S_QUEST_COMPLETE).WriteInt(questId));

        public static void SendQuestAbandon(int questId)
            => Send(new PacketBuilder(PacketOpcode.C2S_QUEST_ABANDON).WriteInt(questId));

        // ── CHAT BASE ────────────────────────────────────────────

        public static void SendChat(string channel, string message)
        {
            byte ch = ChannelByte(channel);
            Send(new PacketBuilder(PacketOpcode.C2S_CHAT)
                .WriteByte(ch)
                .WriteString(message));
        }

        // ── CHAT STICKER ──────────────────────────────────────────

        public static void SendSticker(string channel, int stickerId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_STICKER)
                .WriteByte(ChannelByte(channel))
                .WriteInt(stickerId));

        // ── CHAT EMOJI ────────────────────────────────────────────

        public static void SendEmoji(string channel, int emojiUnicodeCode)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_EMOJI)
                .WriteByte(ChannelByte(channel))
                .WriteInt(emojiUnicodeCode));

        // ── CHAT LOCATION ─────────────────────────────────────────

        public static void SendLocation(string channel, int mapId, float x, float y)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_LOCATION)
                .WriteByte(ChannelByte(channel))
                .WriteInt(mapId)
                .WriteFloat(x)
                .WriteFloat(y));

        // ── CHAT ITEM SHOWCASE ────────────────────────────────────

        public static void SendItemShowcase(string channel, long instanceId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_ITEM)
                .WriteByte(ChannelByte(channel))
                .WriteLong(instanceId));

        // ── RED ENVELOPE (Lì xì) ──────────────────────────────────

        /// <param name="currency">0=gold, 1=diamond</param>
        public static void SendRedEnvelope(string channel, int totalAmount, byte maxGrabbers,
                                            byte currency, string message)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_RED_ENVELOPE)
                .WriteByte(ChannelByte(channel))
                .WriteInt(totalAmount)
                .WriteByte(maxGrabbers)
                .WriteByte(currency)
                .WriteString(message));

        public static void SendGrabEnvelope(long envelopeId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_GRAB_ENVELOPE)
                .WriteLong(envelopeId));

        // ── VOICE ────────────────────────────────────────────────

        /// <summary>Gửi URL của voice message sau khi đã upload lên server</summary>
        public static void SendVoiceUrl(string channel, int durationMs, string url)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_VOICE)
                .WriteByte(ChannelByte(channel))
                .WriteInt(durationMs)
                .WriteString(url));

        // ── CROSS-SERVER ──────────────────────────────────────────

        public static void SendCrossChat(string message)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_CROSS)
                .WriteString(message));

        // ── Channel byte helper ───────────────────────────────────

        public static byte ChannelByte(string channel) => channel switch
        {
            "map"    => 0,
            "world"  => 1,
            "guild"  => 2,
            "pm"     => 3,
            "system" => 4,
            "cross"  => 5,
            _        => 1  // default: world
        };

        // ── SYSTEM ───────────────────────────────────────────────

        public static void SendPing()
            => Send(new PacketBuilder(PacketOpcode.C2S_PING));

        // ── GIFT CODE (Axx) ──────────────────────────────────────

        public static void SendGiftCode(string code)
            => Send(new PacketBuilder(PacketOpcode.C2S_GIFTCODE).WriteString(code));

        // ── MISSION PASS (Bxx) ───────────────────────────────────

        public static void SendPassInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_PASS_INFO));

        /// <param name="tier">0=free, 1=premium</param>
        public static void SendPassClaim(int level, byte tier)
            => Send(new PacketBuilder(PacketOpcode.C2S_PASS_CLAIM)
                .WriteInt(level).WriteByte(tier));

        public static void SendBuyPremiumPass()
            => Send(new PacketBuilder(PacketOpcode.C2S_PASS_BUY_PREMIUM));

        // ── TITLE (Cxx) ──────────────────────────────────────────

        public static void SendTitleList()
            => Send(new PacketBuilder(PacketOpcode.C2S_TITLE_LIST));

        public static void SendTitleEquip(int titleId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TITLE_EQUIP).WriteInt(titleId));

        // ── PET (Dxx) ─────────────────────────────────────────────

        public static void SendPetList()
            => Send(new PacketBuilder(PacketOpcode.C2S_PET_LIST));

        public static void SendPetSetActive(long petId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PET_SET_ACTIVE).WriteLong(petId));

        public static void SendPetFeed(long petId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PET_FEED).WriteLong(petId));

        // ── MOUNT (Dxx) ───────────────────────────────────────────

        public static void SendMountList()
            => Send(new PacketBuilder(PacketOpcode.C2S_MOUNT_LIST));

        public static void SendMountSetActive(long mountId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MOUNT_SET_ACTIVE).WriteLong(mountId));

        // ── SOCIAL / MARRIAGE (Exx) ───────────────────────────────

        public static void SendAddFriend(long targetCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_ADD_FRIEND).WriteLong(targetCharId));

        public static void SendStartDating(long targetCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_START_DATING).WriteLong(targetCharId));

        public static void SendPropose(long targetCharId, int ringItemId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PROPOSE)
                .WriteLong(targetCharId).WriteInt(ringItemId));

        public static void SendWedding(long targetCharId, int weddingMapId)
            => Send(new PacketBuilder(PacketOpcode.C2S_WEDDING)
                .WriteLong(targetCharId).WriteInt(weddingMapId));

        public static void SendChildList()
            => Send(new PacketBuilder(PacketOpcode.C2S_CHILD_LIST));

        public static void SendChildFeed(long childId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHILD_FEED).WriteLong(childId));

        public static void SendChildToggle(long childId, bool active)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHILD_TOGGLE)
                .WriteLong(childId).WriteBool(active));

        // ── MENTOR (Fxx) ──────────────────────────────────────────

        public static void SendMentorInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_MENTOR_INFO));

        /// <param name="mentorId">charId (>0) hoặc NPC id (<0)</param>
        public static void SendMentorAccept(long mentorId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MENTOR_ACCEPT).WriteLong(mentorId));

        public static void SendMentorGraduate()
            => Send(new PacketBuilder(PacketOpcode.C2S_MENTOR_GRADUATE));

        public static void SendStudentList()
            => Send(new PacketBuilder(PacketOpcode.C2S_STUDENT_LIST));

        // ── Guild ────────────────────────────────────────────────

        public static void SendGuildInfo(long guildId = 0)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_INFO).WriteLong(guildId));
        public static void SendGuildCreate(string name)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_CREATE).WriteString(name));
        public static void SendGuildInvite(long targetCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_INVITE).WriteLong(targetCharId));
        public static void SendGuildLeave()
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_LEAVE));
        public static void SendGuildAccept(long guildId)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_ACCEPT).WriteLong(guildId));
        public static void SendGuildKick(long targetCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_KICK).WriteLong(targetCharId));
        public static void SendGuildPromote(long targetCharId, byte role)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_PROMOTE).WriteLong(targetCharId).WriteByte(role));
        public static void SendGuildDisband()
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_DISBAND));

        // ── Skill ────────────────────────────────────────────────

        public static void SendSkillList()
            => Send(new PacketBuilder(PacketOpcode.C2S_SKILL_LIST));
        public static void SendClassSkillList()
            => Send(new PacketBuilder(PacketOpcode.C2S_SKILL_CLASS_LIST));
        public static void SendSkillLearn(int skillId)
            => Send(new PacketBuilder(PacketOpcode.C2S_SKILL_LEARN).WriteInt(skillId));
        public static void SendSkillUpgrade(int skillId)
            => Send(new PacketBuilder(PacketOpcode.C2S_SKILL_UPGRADE).WriteInt(skillId));
        public static void SendSkillSetSlot(byte slot, int skillId)
            => Send(new PacketBuilder(PacketOpcode.C2S_SKILL_SET_SLOT).WriteByte(slot).WriteInt(skillId));

        // ── Enhancement ──────────────────────────────────────────

        public static void SendEnhanceItem(long instanceId)
            => Send(new PacketBuilder(PacketOpcode.C2S_ENHANCE_ITEM).WriteLong(instanceId));

        // ── PvP ──────────────────────────────────────────────────

        public static void SendPvpChallenge(long targetCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_CHALLENGE).WriteLong(targetCharId));
        public static void SendPvpRespond(long challengerId, bool accept)
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_RESPOND).WriteLong(challengerId).WriteBool(accept));
        public static void SendPvpAttack(long targetCharId, int skillId = 0)
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_ATTACK_PVP).WriteLong(targetCharId).WriteInt(skillId));
        public static void SendPvpSurrender()
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_SURRENDER));

        // ── Minigame ─────────────────────────────────────────────

        public static void SendMinigameRoomList(string gameType)
            => Send(new PacketBuilder(PacketOpcode.C2S_MINIGAME_ROOM_LIST).WriteString(gameType));
        public static void SendMinigameCreate(string gameType, int minBet, int maxBet, byte currency)
            => Send(new PacketBuilder(PacketOpcode.C2S_MINIGAME_CREATE)
                .WriteString(gameType).WriteInt(minBet).WriteInt(maxBet).WriteByte(currency));
        public static void SendMinigameJoin(long roomId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MINIGAME_JOIN).WriteLong(roomId));
        public static void SendMinigameLeave(long roomId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MINIGAME_LEAVE).WriteLong(roomId));
        public static void SendMinigameBet(long roomId, int symbol, int amount)
            => Send(new PacketBuilder(PacketOpcode.C2S_MINIGAME_BET)
                .WriteLong(roomId).WriteInt(symbol).WriteInt(amount));
        public static void SendMinigameAnswer(long roomId, byte answerIdx)
            => Send(new PacketBuilder(PacketOpcode.C2S_MINIGAME_ANSWER)
                .WriteLong(roomId).WriteByte(answerIdx));

        // ── Farming ──────────────────────────────────────────────

        public static void SendFarmState()
            => Send(new PacketBuilder(PacketOpcode.C2S_FARM_STATE));
        public static void SendFarmPlant(int plotIndex, int seedItemId)
            => Send(new PacketBuilder(PacketOpcode.C2S_FARM_PLANT).WriteInt(plotIndex).WriteInt(seedItemId));
        public static void SendFarmWater(int plotIndex)
            => Send(new PacketBuilder(PacketOpcode.C2S_FARM_WATER).WriteInt(plotIndex));
        public static void SendFarmHarvest(int plotIndex)
            => Send(new PacketBuilder(PacketOpcode.C2S_FARM_HARVEST).WriteInt(plotIndex));
        public static void SendAnimalFeed(int penIndex)
            => Send(new PacketBuilder(PacketOpcode.C2S_ANIMAL_FEED).WriteInt(penIndex));
        public static void SendAnimalCollect(int penIndex)
            => Send(new PacketBuilder(PacketOpcode.C2S_ANIMAL_COLLECT).WriteInt(penIndex));

        // ── Housing ───────────────────────────────────────────────

        public static void SendHouseInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_HOUSE_INFO));
        public static void SendHouseFurnitureList()
            => Send(new PacketBuilder(PacketOpcode.C2S_HOUSE_FURNITURE));
        public static void SendHousePlace(int furnitureId, float x, float y, int rotation)
            => Send(new PacketBuilder(PacketOpcode.C2S_HOUSE_PLACE)
                .WriteInt(furnitureId).WriteFloat(x).WriteFloat(y).WriteInt(rotation));
        public static void SendHouseRemove(long instanceId)
            => Send(new PacketBuilder(PacketOpcode.C2S_HOUSE_REMOVE).WriteLong(instanceId));
        public static void SendHouseCatalog()
            => Send(new PacketBuilder(PacketOpcode.C2S_HOUSE_CATALOG));

        // ── Leaderboard ──────────────────────────────────────────

        public static void SendLeaderboard(string rankType, int serverId = 0)
            => Send(new PacketBuilder(PacketOpcode.C2S_LEADERBOARD).WriteString(rankType).WriteInt(serverId));

        // ── Drop item ────────────────────────────────────────────

        public static void SendDropItem(long instanceId, int qty = 1)
            => Send(new PacketBuilder(PacketOpcode.C2S_DROP_ITEM).WriteLong(instanceId).WriteInt(qty));

        // ── TRADE ────────────────────────────────────────────────

        public static void SendTradeRequest(long targetCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_REQUEST).WriteLong(targetCharId));
        public static void SendTradeRespond(long fromCharId, bool accept)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_RESPOND).WriteLong(fromCharId).WriteBool(accept));
        public static void SendTradeAddItem(long tradeId, long inventoryId, int qty)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_ADD_ITEM).WriteLong(tradeId).WriteLong(inventoryId).WriteInt(qty));
        public static void SendTradeSetGold(long tradeId, long gold)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_SET_GOLD).WriteLong(tradeId).WriteLong(gold));
        public static void SendTradeConfirm(long tradeId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_CONFIRM).WriteLong(tradeId));
        public static void SendTradeCancel(long tradeId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_CANCEL).WriteLong(tradeId));

        // ── AUCTION ──────────────────────────────────────────────

        public static void SendAuctionList(int page = 0)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUCTION_LIST).WriteInt(page));
        public static void SendAuctionCreate(long inventoryId, long startPrice, long buyoutPrice, int hours)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUCTION_CREATE).WriteLong(inventoryId).WriteLong(startPrice).WriteLong(buyoutPrice).WriteInt(hours));
        public static void SendAuctionBid(long listingId, long amount)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUCTION_BID).WriteLong(listingId).WriteLong(amount));
        public static void SendAuctionBuyout(long listingId)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUCTION_BUYOUT).WriteLong(listingId));
        public static void SendAuctionCancel(long listingId)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUCTION_CANCEL).WriteLong(listingId));
        public static void SendAuctionMyItems()
            => Send(new PacketBuilder(PacketOpcode.C2S_AUCTION_MY_ITEMS));

        // ── PARTY ────────────────────────────────────────────────

        public static void SendPartyCreate()
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_CREATE));
        public static void SendPartyInvite(long targetCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_INVITE).WriteLong(targetCharId));
        public static void SendPartyAccept(long leaderCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_ACCEPT).WriteLong(leaderCharId));
        public static void SendPartyLeave()
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_LEAVE));
        public static void SendPartyKick(long charId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_KICK).WriteLong(charId));
        public static void SendPartyDisband()
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_DISBAND));

        // ── DUNGEON ──────────────────────────────────────────────

        public static void SendDungeonList()
            => Send(new PacketBuilder(PacketOpcode.C2S_DUNGEON_LIST));
        public static void SendDungeonEnter(int templateId)
            => Send(new PacketBuilder(PacketOpcode.C2S_DUNGEON_ENTER).WriteInt(templateId));
        public static void SendDungeonExit()
            => Send(new PacketBuilder(PacketOpcode.C2S_DUNGEON_EXIT));

        // ── NPC DIALOG ───────────────────────────────────────────

        public static void SendDialogStart(int npcId)
            => Send(new PacketBuilder(PacketOpcode.C2S_DIALOG_START).WriteInt(npcId));
        public static void SendDialogChoice(int dialogId, int choiceIdx)
            => Send(new PacketBuilder(PacketOpcode.C2S_DIALOG_CHOICE).WriteInt(dialogId).WriteInt(choiceIdx));

        // ── ANNOUNCEMENTS ────────────────────────────────────────

        public static void SendAnnouncementList()
            => Send(new PacketBuilder(PacketOpcode.C2S_ANNOUNCEMENT_LIST));

        // ── EVENT CURRENCY ───────────────────────────────────────

        public static void SendEventCurrencyList()
            => Send(new PacketBuilder(PacketOpcode.C2S_EVENT_CURRENCY_LIST));
        public static void SendEventCurrencyShop(int currencyId)
            => Send(new PacketBuilder(PacketOpcode.C2S_EVENT_CURRENCY_SHOP).WriteInt(currencyId));
        public static void SendEventCurrencyBuy(int shopItemId)
            => Send(new PacketBuilder(PacketOpcode.C2S_EVENT_CURRENCY_BUY).WriteInt(shopItemId));
        public static void SendEventCurrencyExchange(int currencyId, int amount)
            => Send(new PacketBuilder(PacketOpcode.C2S_EVENT_CURRENCY_EXCHANGE).WriteInt(currencyId).WriteInt(amount));

        // ── AUTH ─────────────────────────────────────────

        public static void SendLogin(string username, string password)
            => Send(new PacketBuilder(PacketOpcode.C2S_LOGIN).WriteString(username).WriteString(password));
        public static void SendRegister(string username, string password)
            => Send(new PacketBuilder(PacketOpcode.C2S_REGISTER).WriteString(username).WriteString(password));
        public static void SendCharList()
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAR_LIST));




        public static void SendIntroVideoConfigRequest()
            => Send(new PacketBuilder(PacketOpcode.C2S_INTRO_VIDEO_CONFIG));

        // ── EXPRESSIONS + ACTIONS + INTERACT ────────────────

        public static void SendCharAction(int actionId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAR_ACTION).WriteInt(actionId));
        public static void SendPairAction(int actionId, long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PAIR_ACTION).WriteInt(actionId).WriteLong(targetId));
        public static void SendPairActionAccept(long requesterId, bool accept)
            => Send(new PacketBuilder(PacketOpcode.C2S_PAIR_ACTION_REPLY).WriteLong(requesterId).WriteBool(accept));
        public static void SendFriendRequest(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_FRIEND_REQUEST).WriteLong(targetId));
        public static void SendPartyInvite(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_INVITE).WriteLong(targetId));
        public static void SendGuildInvite(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_INVITE).WriteLong(targetId));
        public static void SendDuelRequest(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_DUEL_REQUEST).WriteLong(targetId));
        public static void SendTrade(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_REQUEST).WriteLong(targetId));
        public static void SendAutoConfig(string configJson)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUTO_CONFIG).WriteString(configJson));

        // ── MISSING SENDS ───────────────────────────────────

        public static void SendCastSkill(int skillId, long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CAST_SKILL).WriteInt(skillId).WriteLong(targetId));
        public static void SendBlock(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_BLOCK_PLAYER).WriteLong(targetId));
        public static void SendReport(long targetId, string reason)
            => Send(new PacketBuilder(PacketOpcode.C2S_REPORT_PLAYER).WriteLong(targetId).WriteString(reason));
        public static void SendMissionPassInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_MISSION_PASS_INFO));
        public static void SendMissionPassClaim(int level)
            => Send(new PacketBuilder(PacketOpcode.C2S_MISSION_PASS_CLAIM).WriteInt(level));
        public static void SendInspectPlayer(long charId)
            => Send(new PacketBuilder(PacketOpcode.C2S_INSPECT_PLAYER).WriteLong(charId));
        public static void SendAutoPlay(bool enabled)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUTO_PLAY).WriteBool(enabled));
        public static void SendEmote(int emoteId)
            => Send(new PacketBuilder(PacketOpcode.C2S_EMOTE).WriteInt(emoteId));
        public static void SendTeleport(int mapId, float x, float y)
            => Send(new PacketBuilder(PacketOpcode.C2S_TELEPORT).WriteInt(mapId).WriteFloat(x).WriteFloat(y));
        public static void SendWarehouse(int action, int slot)
            => Send(new PacketBuilder(PacketOpcode.C2S_WAREHOUSE).WriteInt(action).WriteInt(slot));
        public static void SendGemSocket(int equipSlot, int gemId, int socketIdx)
            => Send(new PacketBuilder(PacketOpcode.C2S_GEM_SOCKET).WriteInt(equipSlot).WriteInt(gemId).WriteInt(socketIdx));
        public static void SendRefine(int equipSlot)
            => Send(new PacketBuilder(PacketOpcode.C2S_REFINE).WriteInt(equipSlot));
        public static void SendNewsRequest()
            => Send(new PacketBuilder(PacketOpcode.C2S_NEWS_LIST));

        // ── TOPUP IN-GAME ───────────────────────────────

        public static void SendTopupPackages()
            => Send(new PacketBuilder(PacketOpcode.C2S_TOPUP_PACKAGES));
        public static void SendTopupBuy(int packageId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TOPUP_BUY).WriteInt(packageId));
        public static void SendTopupHistory()
            => Send(new PacketBuilder(PacketOpcode.C2S_TOPUP_HISTORY));

        // ── SERVER SELECTION ─────────────────────────────

        public static void SendServerList()
            => Send(new PacketBuilder(PacketOpcode.C2S_SERVER_LIST));
        public static void SendServerSelect(int serverId)
            => Send(new PacketBuilder(PacketOpcode.C2S_SERVER_SELECT).WriteInt(serverId));
        public static void SendChannelList(int serverId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHANNEL_LIST).WriteInt(serverId));
        public static void SendChannelSelect(int channelId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHANNEL_SELECT).WriteInt(channelId));

        // ── INTRO + LOGIN SCREEN ─────────────────────────

        public static void SendIntroRequest()
            => Send(new PacketBuilder(PacketOpcode.C2S_INTRO_REQUEST));
        public static void SendIntroComplete()
            => Send(new PacketBuilder(PacketOpcode.C2S_INTRO_COMPLETE));
        public static void SendIntroSkip()
            => Send(new PacketBuilder(PacketOpcode.C2S_INTRO_SKIP));
        public static void SendLoginScreenConfig()
            => Send(new PacketBuilder(PacketOpcode.C2S_LOGIN_SCREEN_CFG));

        // ── GACHA ───────────────────────────────────────────

        public static void SendGachaBannerList()
            => Send(new PacketBuilder(PacketOpcode.C2S_GACHA_BANNER_LIST));
        public static void SendGachaPull(int bannerId, int pullCount)
            => Send(new PacketBuilder(PacketOpcode.C2S_GACHA_PULL).WriteInt(bannerId).WriteInt(pullCount));
        public static void SendGachaBuyTicket(int currencyId, int amount)
            => Send(new PacketBuilder(PacketOpcode.C2S_GACHA_BUY_TICKET).WriteInt(currencyId).WriteInt(amount));
        public static void SendGachaCurrency()
            => Send(new PacketBuilder(PacketOpcode.C2S_GACHA_CURRENCY));

        public static void SendGachaHistory(int bannerId)
            => Send(new PacketBuilder(PacketOpcode.C2S_GACHA_HISTORY).WriteInt(bannerId));

        // ── PVP SEASON ──────────────────────────────────────

        public static void SendPvpSeasonInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_SEASON_INFO));
        public static void SendPvpSeasonRank(int page)
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_SEASON_RANK).WriteInt(page));
        public static void SendPvpSeasonReward()
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_SEASON_REWARD));

        // ── SOCIAL LOGIN ────────────────────────────────────

        public static void SendSocialLogin(string provider, string token)
            => Send(new PacketBuilder(PacketOpcode.C2S_SOCIAL_LOGIN).WriteString(provider).WriteString(token));
        public static void SendSocialLink(string provider, string token)
            => Send(new PacketBuilder(PacketOpcode.C2S_SOCIAL_LINK).WriteString(provider).WriteString(token));
        public static void SendSocialUnlink(string provider)
            => Send(new PacketBuilder(PacketOpcode.C2S_SOCIAL_UNLINK).WriteString(provider));

        // ── TUTORIAL ────────────────────────────────────────

        public static void SendTutorialProgress(string stepKey)
            => Send(new PacketBuilder(PacketOpcode.C2S_TUTORIAL_PROGRESS).WriteString(stepKey));
        public static void SendTutorialSkip()
            => Send(new PacketBuilder(PacketOpcode.C2S_TUTORIAL_SKIP));

        // ── LOCALIZATION ────────────────────────────────────

        public static void SendLangSet(string langCode)
            => Send(new PacketBuilder(PacketOpcode.C2S_LANG_SET).WriteString(langCode));

        // ── SETTINGS ─────────────────────────────────────

        public static void SendSettingsLoad()
            => Send(new PacketBuilder(PacketOpcode.C2S_SETTINGS_LOAD));
        public static void SendSettingsSave(string settingsJson)
            => Send(new PacketBuilder(PacketOpcode.C2S_SETTINGS_SAVE).WriteString(settingsJson));

        // ── CLASS CHANGE ─────────────────────────────────

        public static void SendClassChange(int classId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CLASS_CHANGE).WriteInt(classId));

        // ── ACHIEVEMENT ──────────────────────────────────────

        public static void SendAchievementList()
            => Send(new PacketBuilder(PacketOpcode.C2S_ACHIEVEMENT_LIST));
        public static void SendAchievementClaim(int achievementId)
            => Send(new PacketBuilder(PacketOpcode.C2S_ACHIEVEMENT_CLAIM).WriteInt(achievementId));

        // ── DAILY LOGIN ──────────────────────────────────────

        public static void SendDailyLoginInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_DAILY_LOGIN_INFO));
        public static void SendDailyLoginClaim()
            => Send(new PacketBuilder(PacketOpcode.C2S_DAILY_LOGIN_CLAIM));

        // ── WORLD BOSS ───────────────────────────────────────

        public static void SendWorldBossInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_WORLD_BOSS_INFO));

        // ── MAIL ─────────────────────────────────────────────

        public static void SendMailList()
            => Send(new PacketBuilder(PacketOpcode.C2S_MAIL_LIST));
        public static void SendMailRead(long mailId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MAIL_READ).WriteLong(mailId));
        public static void SendMailClaim(long mailId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MAIL_CLAIM).WriteLong(mailId));
        public static void SendMailDelete(long mailId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MAIL_DELETE).WriteLong(mailId));


        public static void SendIntroVideoConfigRequest()
            => Send(new PacketBuilder(PacketOpcode.C2S_INTRO_VIDEO_CONFIG));

        // ── EXPRESSIONS + ACTIONS + INTERACT ────────────────

        public static void SendCharAction(int actionId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAR_ACTION).WriteInt(actionId));
        public static void SendPairAction(int actionId, long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PAIR_ACTION).WriteInt(actionId).WriteLong(targetId));
        public static void SendPairActionAccept(long requesterId, bool accept)
            => Send(new PacketBuilder(PacketOpcode.C2S_PAIR_ACTION_REPLY).WriteLong(requesterId).WriteBool(accept));
        public static void SendFriendRequest(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_FRIEND_REQUEST).WriteLong(targetId));
        public static void SendPartyInvite(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_INVITE).WriteLong(targetId));
        public static void SendGuildInvite(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_INVITE).WriteLong(targetId));
        public static void SendDuelRequest(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_DUEL_REQUEST).WriteLong(targetId));
        public static void SendTrade(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_REQUEST).WriteLong(targetId));
        public static void SendAutoConfig(string configJson)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUTO_CONFIG).WriteString(configJson));

        // ── MISSING SENDS ────────────────────────────────────

        public static void SendInventoryList()
            => Send(new PacketBuilder(PacketOpcode.C2S_INVENTORY_LIST));
        public static void SendMinigameAction(long roomId, int actionType, int value)
            => Send(new PacketBuilder(PacketOpcode.C2S_MINIGAME_ACTION).WriteLong(roomId).WriteInt(actionType).WriteInt(value));
        public static void SendPvPAttack(long targetCharId, int skillId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_ATTACK).WriteLong(targetCharId).WriteInt(skillId));

    }

    /// <summary>
    /// Đọc payload của packet (sau khi opcode đã tách ra)
    /// </summary>
    public class PacketReader
    {
        private readonly byte[] _data;
        private int _pos;

        public PacketReader(byte[] payload)
        {
            _data = payload;
            _pos  = 0;
        }

        public int Remaining => _data.Length - _pos;

        public byte ReadByte() => _data[_pos++];
        public bool ReadBool() => _data[_pos++] != 0;

        public short ReadShort()
        {
            short v = (short)((_data[_pos] << 8) | _data[_pos + 1]);
            _pos += 2;
            return v;
        }

        public int ReadInt()
        {
            int v = (_data[_pos]     << 24) | (_data[_pos + 1] << 16)
                  | (_data[_pos + 2] <<  8) |  _data[_pos + 3];
            _pos += 4;
            return v;
        }

        public long ReadLong()
        {
            long hi = (uint)ReadInt();
            long lo = (uint)ReadInt();
            return (hi << 32) | lo;
        }

        public float ReadFloat()
        {
            return BitConverter.Int32BitsToSingle(ReadInt());
        }

        public string ReadString()
        {
            int len = ReadShort() & 0xFFFF;
            string s = Encoding.UTF8.GetString(_data, _pos, len);
            _pos += len;
            return s;
        }

        public byte[] ReadBytes(int count)
        {
            byte[] b = new byte[count];
            Buffer.BlockCopy(_data, _pos, b, 0, count);
            _pos += count;
            return b;
        }
    }
}

        private readonly MemoryStream _ms;
        private readonly BinaryWriter _w;
        private readonly short        _opcode;

        public PacketBuilder(short opcode)
        {
            _opcode = opcode;
            _ms = new MemoryStream();
            _w  = new BinaryWriter(_ms);
        }

        // ── Primitive writes ────────────────────────────────────

        public PacketBuilder WriteByte(byte v)  { _w.Write(v); return this; }
        public PacketBuilder WriteBool(bool v)  { _w.Write(v); return this; }

        /// Big-endian short
        public PacketBuilder WriteShort(short v)
        {
            _w.Write((byte)(v >> 8));
            _w.Write((byte)(v & 0xFF));
            return this;
        }

        /// Big-endian int
        public PacketBuilder WriteInt(int v)
        {
            _w.Write((byte)((v >> 24) & 0xFF));
            _w.Write((byte)((v >> 16) & 0xFF));
            _w.Write((byte)((v >>  8) & 0xFF));
            _w.Write((byte)( v        & 0xFF));
            return this;
        }

        /// Big-endian long
        public PacketBuilder WriteLong(long v)
        {
            WriteInt((int)(v >> 32));
            WriteInt((int)(v & 0xFFFFFFFFL));
            return this;
        }

        /// Big-endian float (IEEE 754)
        public PacketBuilder WriteFloat(float v)
        {
            int bits = BitConverter.SingleToInt32Bits(v);
            WriteInt(bits);
            return this;
        }

        /// UTF-8 string với 2-byte length prefix
        public PacketBuilder WriteString(string s)
        {
            byte[] b = Encoding.UTF8.GetBytes(s ?? "");
            WriteShort((short)b.Length);
            _w.Write(b);
            return this;
        }

        public PacketBuilder WriteBytes(byte[] b) { _w.Write(b); return this; }

        // ── Finalize ────────────────────────────────────────────

        /// Build packet hoàn chỉnh: [4 len][2 opcode][payload]
        public byte[] Build()
        {
            byte[] payload = _ms.ToArray();
            int bodyLen = 2 + payload.Length; // opcode + payload

            using var result = new MemoryStream(4 + bodyLen);
            using var rw     = new BinaryWriter(result);

            // 4-byte big-endian length
            rw.Write((byte)((bodyLen >> 24) & 0xFF));
            rw.Write((byte)((bodyLen >> 16) & 0xFF));
            rw.Write((byte)((bodyLen >>  8) & 0xFF));
            rw.Write((byte)( bodyLen        & 0xFF));

            // 2-byte opcode
            rw.Write((byte)((_opcode >> 8) & 0xFF));
            rw.Write((byte)( _opcode       & 0xFF));

            // Payload
            rw.Write(payload);
            return result.ToArray();
        }

        // ── AUTH ─────────────────────────────────────────

        public static void SendLogin(string username, string password)
            => Send(new PacketBuilder(PacketOpcode.C2S_LOGIN).WriteString(username).WriteString(password));
        public static void SendRegister(string username, string password)
            => Send(new PacketBuilder(PacketOpcode.C2S_REGISTER).WriteString(username).WriteString(password));
        public static void SendCharList()
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAR_LIST));




        public static void SendIntroVideoConfigRequest()
            => Send(new PacketBuilder(PacketOpcode.C2S_INTRO_VIDEO_CONFIG));

        // ── EXPRESSIONS + ACTIONS + INTERACT ────────────────

        public static void SendCharAction(int actionId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAR_ACTION).WriteInt(actionId));
        public static void SendPairAction(int actionId, long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PAIR_ACTION).WriteInt(actionId).WriteLong(targetId));
        public static void SendPairActionAccept(long requesterId, bool accept)
            => Send(new PacketBuilder(PacketOpcode.C2S_PAIR_ACTION_REPLY).WriteLong(requesterId).WriteBool(accept));
        public static void SendFriendRequest(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_FRIEND_REQUEST).WriteLong(targetId));
        public static void SendPartyInvite(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_INVITE).WriteLong(targetId));
        public static void SendGuildInvite(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_INVITE).WriteLong(targetId));
        public static void SendDuelRequest(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_DUEL_REQUEST).WriteLong(targetId));
        public static void SendTrade(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_REQUEST).WriteLong(targetId));
        public static void SendAutoConfig(string configJson)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUTO_CONFIG).WriteString(configJson));

        // ── MISSING SENDS ───────────────────────────────────

        public static void SendCastSkill(int skillId, long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CAST_SKILL).WriteInt(skillId).WriteLong(targetId));
        public static void SendBlock(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_BLOCK_PLAYER).WriteLong(targetId));
        public static void SendReport(long targetId, string reason)
            => Send(new PacketBuilder(PacketOpcode.C2S_REPORT_PLAYER).WriteLong(targetId).WriteString(reason));
        public static void SendMissionPassInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_MISSION_PASS_INFO));
        public static void SendMissionPassClaim(int level)
            => Send(new PacketBuilder(PacketOpcode.C2S_MISSION_PASS_CLAIM).WriteInt(level));
        public static void SendInspectPlayer(long charId)
            => Send(new PacketBuilder(PacketOpcode.C2S_INSPECT_PLAYER).WriteLong(charId));
        public static void SendAutoPlay(bool enabled)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUTO_PLAY).WriteBool(enabled));
        public static void SendEmote(int emoteId)
            => Send(new PacketBuilder(PacketOpcode.C2S_EMOTE).WriteInt(emoteId));
        public static void SendTeleport(int mapId, float x, float y)
            => Send(new PacketBuilder(PacketOpcode.C2S_TELEPORT).WriteInt(mapId).WriteFloat(x).WriteFloat(y));
        public static void SendWarehouse(int action, int slot)
            => Send(new PacketBuilder(PacketOpcode.C2S_WAREHOUSE).WriteInt(action).WriteInt(slot));
        public static void SendGemSocket(int equipSlot, int gemId, int socketIdx)
            => Send(new PacketBuilder(PacketOpcode.C2S_GEM_SOCKET).WriteInt(equipSlot).WriteInt(gemId).WriteInt(socketIdx));
        public static void SendRefine(int equipSlot)
            => Send(new PacketBuilder(PacketOpcode.C2S_REFINE).WriteInt(equipSlot));
        public static void SendNewsRequest()
            => Send(new PacketBuilder(PacketOpcode.C2S_NEWS_LIST));

        // ── TOPUP IN-GAME ───────────────────────────────

        public static void SendTopupPackages()
            => Send(new PacketBuilder(PacketOpcode.C2S_TOPUP_PACKAGES));
        public static void SendTopupBuy(int packageId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TOPUP_BUY).WriteInt(packageId));
        public static void SendTopupHistory()
            => Send(new PacketBuilder(PacketOpcode.C2S_TOPUP_HISTORY));

        // ── SERVER SELECTION ─────────────────────────────

        public static void SendServerList()
            => Send(new PacketBuilder(PacketOpcode.C2S_SERVER_LIST));
        public static void SendServerSelect(int serverId)
            => Send(new PacketBuilder(PacketOpcode.C2S_SERVER_SELECT).WriteInt(serverId));
        public static void SendChannelList(int serverId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHANNEL_LIST).WriteInt(serverId));
        public static void SendChannelSelect(int channelId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHANNEL_SELECT).WriteInt(channelId));

        // ── INTRO + LOGIN SCREEN ─────────────────────────

        public static void SendIntroRequest()
            => Send(new PacketBuilder(PacketOpcode.C2S_INTRO_REQUEST));
        public static void SendIntroComplete()
            => Send(new PacketBuilder(PacketOpcode.C2S_INTRO_COMPLETE));
        public static void SendIntroSkip()
            => Send(new PacketBuilder(PacketOpcode.C2S_INTRO_SKIP));
        public static void SendLoginScreenConfig()
            => Send(new PacketBuilder(PacketOpcode.C2S_LOGIN_SCREEN_CFG));

        // ── GACHA ───────────────────────────────────────────

        public static void SendGachaBannerList()
            => Send(new PacketBuilder(PacketOpcode.C2S_GACHA_BANNER_LIST));
        public static void SendGachaPull(int bannerId, int pullCount)
            => Send(new PacketBuilder(PacketOpcode.C2S_GACHA_PULL).WriteInt(bannerId).WriteInt(pullCount));
        public static void SendGachaBuyTicket(int currencyId, int amount)
            => Send(new PacketBuilder(PacketOpcode.C2S_GACHA_BUY_TICKET).WriteInt(currencyId).WriteInt(amount));
        public static void SendGachaCurrency()
            => Send(new PacketBuilder(PacketOpcode.C2S_GACHA_CURRENCY));

        public static void SendGachaHistory(int bannerId)
            => Send(new PacketBuilder(PacketOpcode.C2S_GACHA_HISTORY).WriteInt(bannerId));

        // ── PVP SEASON ──────────────────────────────────────

        public static void SendPvpSeasonInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_SEASON_INFO));
        public static void SendPvpSeasonRank(int page)
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_SEASON_RANK).WriteInt(page));
        public static void SendPvpSeasonReward()
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_SEASON_REWARD));

        // ── SOCIAL LOGIN ────────────────────────────────────

        public static void SendSocialLogin(string provider, string token)
            => Send(new PacketBuilder(PacketOpcode.C2S_SOCIAL_LOGIN).WriteString(provider).WriteString(token));
        public static void SendSocialLink(string provider, string token)
            => Send(new PacketBuilder(PacketOpcode.C2S_SOCIAL_LINK).WriteString(provider).WriteString(token));
        public static void SendSocialUnlink(string provider)
            => Send(new PacketBuilder(PacketOpcode.C2S_SOCIAL_UNLINK).WriteString(provider));

        // ── TUTORIAL ────────────────────────────────────────

        public static void SendTutorialProgress(string stepKey)
            => Send(new PacketBuilder(PacketOpcode.C2S_TUTORIAL_PROGRESS).WriteString(stepKey));
        public static void SendTutorialSkip()
            => Send(new PacketBuilder(PacketOpcode.C2S_TUTORIAL_SKIP));

        // ── LOCALIZATION ────────────────────────────────────

        public static void SendLangSet(string langCode)
            => Send(new PacketBuilder(PacketOpcode.C2S_LANG_SET).WriteString(langCode));

        // ── SETTINGS ─────────────────────────────────────

        public static void SendSettingsLoad()
            => Send(new PacketBuilder(PacketOpcode.C2S_SETTINGS_LOAD));
        public static void SendSettingsSave(string settingsJson)
            => Send(new PacketBuilder(PacketOpcode.C2S_SETTINGS_SAVE).WriteString(settingsJson));

        // ── CLASS CHANGE ─────────────────────────────────

        public static void SendClassChange(int classId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CLASS_CHANGE).WriteInt(classId));

        // ── ACHIEVEMENT ──────────────────────────────────────

        public static void SendAchievementList()
            => Send(new PacketBuilder(PacketOpcode.C2S_ACHIEVEMENT_LIST));
        public static void SendAchievementClaim(int achievementId)
            => Send(new PacketBuilder(PacketOpcode.C2S_ACHIEVEMENT_CLAIM).WriteInt(achievementId));

        // ── DAILY LOGIN ──────────────────────────────────────

        public static void SendDailyLoginInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_DAILY_LOGIN_INFO));
        public static void SendDailyLoginClaim()
            => Send(new PacketBuilder(PacketOpcode.C2S_DAILY_LOGIN_CLAIM));

        // ── WORLD BOSS ───────────────────────────────────────

        public static void SendWorldBossInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_WORLD_BOSS_INFO));

        // ── MAIL ─────────────────────────────────────────────

        public static void SendMailList()
            => Send(new PacketBuilder(PacketOpcode.C2S_MAIL_LIST));
        public static void SendMailRead(long mailId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MAIL_READ).WriteLong(mailId));
        public static void SendMailClaim(long mailId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MAIL_CLAIM).WriteLong(mailId));
        public static void SendMailDelete(long mailId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MAIL_DELETE).WriteLong(mailId));


        public static void SendIntroVideoConfigRequest()
            => Send(new PacketBuilder(PacketOpcode.C2S_INTRO_VIDEO_CONFIG));

        // ── EXPRESSIONS + ACTIONS + INTERACT ────────────────

        public static void SendCharAction(int actionId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAR_ACTION).WriteInt(actionId));
        public static void SendPairAction(int actionId, long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PAIR_ACTION).WriteInt(actionId).WriteLong(targetId));
        public static void SendPairActionAccept(long requesterId, bool accept)
            => Send(new PacketBuilder(PacketOpcode.C2S_PAIR_ACTION_REPLY).WriteLong(requesterId).WriteBool(accept));
        public static void SendFriendRequest(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_FRIEND_REQUEST).WriteLong(targetId));
        public static void SendPartyInvite(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_INVITE).WriteLong(targetId));
        public static void SendGuildInvite(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_INVITE).WriteLong(targetId));
        public static void SendDuelRequest(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_DUEL_REQUEST).WriteLong(targetId));
        public static void SendTrade(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_REQUEST).WriteLong(targetId));
        public static void SendAutoConfig(string configJson)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUTO_CONFIG).WriteString(configJson));

        // ── MISSING SENDS ────────────────────────────────────

        public static void SendInventoryList()
            => Send(new PacketBuilder(PacketOpcode.C2S_INVENTORY_LIST));
        public static void SendMinigameAction(long roomId, int actionType, int value)
            => Send(new PacketBuilder(PacketOpcode.C2S_MINIGAME_ACTION).WriteLong(roomId).WriteInt(actionType).WriteInt(value));
        public static void SendPvPAttack(long targetCharId, int skillId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_ATTACK).WriteLong(targetCharId).WriteInt(skillId));

    }

    /// <summary>
    /// Đọc payload của packet (sau khi opcode đã bị tách ra)
    /// </summary>
    public class PacketReader
    {
        private readonly byte[] _data;
        private int _pos;

        public PacketReader(byte[] payload)
        {
            _data = payload;
            _pos  = 0;
        }

        public int Remaining => _data.Length - _pos;

        public byte ReadByte() => _data[_pos++];
        public bool ReadBool() => _data[_pos++] != 0;

        public short ReadShort()
        {
            short v = (short)((_data[_pos] << 8) | _data[_pos + 1]);
            _pos += 2;
            return v;
        }

        public int ReadInt()
        {
            int v = (_data[_pos]     << 24) | (_data[_pos + 1] << 16)
                  | (_data[_pos + 2] <<  8) |  _data[_pos + 3];
            _pos += 4;
            return v;
        }

        public long ReadLong()
        {
            long hi = (uint)ReadInt();
            long lo = (uint)ReadInt();
            return (hi << 32) | lo;
        }

        public float ReadFloat()
        {
            int bits = ReadInt();
            return BitConverter.Int32BitsToSingle(bits);
        }

        public string ReadString()
        {
            int len = ReadShort() & 0xFFFF;
            string s = Encoding.UTF8.GetString(_data, _pos, len);
            _pos += len;
            return s;
        }

        public byte[] ReadBytes(int count)
        {
            byte[] b = new byte[count];
            Buffer.BlockCopy(_data, _pos, b, 0, count);
            _pos += count;
            return b;
        }
    }
}
