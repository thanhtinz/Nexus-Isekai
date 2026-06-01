// NexusIsekai Client - PacketBuilder.cs & PacketReader.cs
// Tương đương ByteBuf Netty ở server. Big-endian, khớp server.
// Format: [2-byte opcode][payload]. Khung 4-byte length do transport thêm.

using System;
using System.IO;
using System.Text;

namespace NexusIsekai.Network
{
    /// <summary>Builder gói tin gửi lên server (fluent API).</summary>
    public class PacketBuilder
    {
        private readonly MemoryStream _ms = new MemoryStream();

        public PacketBuilder(short opcode)
        {
            WriteShortRaw(opcode);
        }

        private void WriteShortRaw(short v)
        {
            _ms.WriteByte((byte)((v >> 8) & 0xFF));
            _ms.WriteByte((byte)(v & 0xFF));
        }

        public PacketBuilder WriteByte(byte v) { _ms.WriteByte(v); return this; }
        public PacketBuilder WriteBool(bool v) { _ms.WriteByte((byte)(v ? 1 : 0)); return this; }
        public PacketBuilder WriteShort(int v)
        {
            _ms.WriteByte((byte)((v >> 8) & 0xFF));
            _ms.WriteByte((byte)(v & 0xFF));
            return this;
        }
        public PacketBuilder WriteInt(int v)
        {
            _ms.WriteByte((byte)((v >> 24) & 0xFF));
            _ms.WriteByte((byte)((v >> 16) & 0xFF));
            _ms.WriteByte((byte)((v >> 8) & 0xFF));
            _ms.WriteByte((byte)(v & 0xFF));
            return this;
        }
        public PacketBuilder WriteLong(long v)
        {
            for (int i = 7; i >= 0; i--) _ms.WriteByte((byte)((v >> (i * 8)) & 0xFF));
            return this;
        }
        public PacketBuilder WriteFloat(float v)
        {
            byte[] b = BitConverter.GetBytes(v);
            if (BitConverter.IsLittleEndian) Array.Reverse(b);
            _ms.Write(b, 0, 4);
            return this;
        }
        public PacketBuilder WriteString(string s)
        {
            byte[] d = Encoding.UTF8.GetBytes(s ?? "");
            WriteShort(d.Length);
            _ms.Write(d, 0, d.Length);
            return this;
        }

        public byte[] Build() => _ms.ToArray();

        /// <summary>Gửi gói qua GameClient (singleton kết nối).</summary>
        public static void Send(PacketBuilder pb)
        {
            GameClient.Instance?.Send(pb);
        }

        // ═══════════════════════════════════════════════════════
        //  CÁC GÓI GỬI (C2S) — convenience methods
        // ═══════════════════════════════════════════════════════

        public static void SendAchievementClaim(int achievementId)
            => Send(new PacketBuilder(PacketOpcode.C2S_ACHIEVEMENT_CLAIM).WriteInt(achievementId));
        public static void SendAchievementList()
            => Send(new PacketBuilder(PacketOpcode.C2S_ACHIEVEMENT_LIST));
        public static void SendAddFriend(long targetCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_ADD_FRIEND).WriteLong(targetCharId));
        public static void SendAnimalCollect(int penIndex)
            => Send(new PacketBuilder(PacketOpcode.C2S_ANIMAL_COLLECT).WriteInt(penIndex));
        public static void SendAnimalFeed(int penIndex)
            => Send(new PacketBuilder(PacketOpcode.C2S_ANIMAL_FEED).WriteInt(penIndex));
        public static void SendAnnouncementList()
            => Send(new PacketBuilder(PacketOpcode.C2S_ANNOUNCEMENT_LIST));
        public static void SendAttack(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_ATTACK).WriteLong(targetId));
        public static void SendAuctionBid(long listingId, long amount)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUCTION_BID).WriteLong(listingId).WriteLong(amount));
        public static void SendAuctionBuyout(long listingId)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUCTION_BUYOUT).WriteLong(listingId));
        public static void SendAuctionCancel(long listingId)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUCTION_CANCEL).WriteLong(listingId));
        public static void SendAuctionCreate(long inventoryId, long startPrice, long buyoutPrice, int hours)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUCTION_CREATE).WriteLong(inventoryId).WriteLong(startPrice).WriteLong(buyoutPrice).WriteInt(hours));
        public static void SendAuctionList(int page = 0)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUCTION_LIST).WriteInt(page));
        public static void SendAuctionMyItems()
            => Send(new PacketBuilder(PacketOpcode.C2S_AUCTION_MY_ITEMS));
        public static void SendAutoConfig(string configJson)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUTO_CONFIG).WriteString(configJson));
        public static void SendAutoPlay(bool enabled)
            => Send(new PacketBuilder(PacketOpcode.C2S_AUTO_PLAY).WriteBool(enabled));
        public static void SendBestiaryClaim(int monsterId) { var p=new PacketBuilder(PacketOpcode.C2S_BESTIARY_CLAIM); p.WriteInt(monsterId); Send(p); }
        public static void SendBestiaryList()        => Send(new PacketBuilder(PacketOpcode.C2S_BESTIARY_LIST));
        public static void SendBlock(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_BLOCK_PLAYER).WriteLong(targetId));
        public static void SendBuyPremiumPass()
            => Send(new PacketBuilder(PacketOpcode.C2S_PASS_BUY_PREMIUM));
        public static void SendCastSkill(int skillId, long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CAST_SKILL).WriteInt(skillId).WriteLong(targetId));
        public static void SendChannelList(int serverId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHANNEL_LIST).WriteInt(serverId));
        public static void SendChannelSelect(int channelId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHANNEL_SELECT).WriteInt(channelId));
        public static void SendCharAction(int actionId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAR_ACTION).WriteInt(actionId));
        public static void SendCharCreate(string name, int classId, int gender)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAR_CREATE)
                .WriteString(name).WriteByte((byte)classId).WriteByte((byte)gender));
        public static void SendCharDelete(long charId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAR_DELETE).WriteLong(charId));
        public static void SendCharList()
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAR_LIST));
        public static void SendCharSelect(long charId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAR_SELECT).WriteLong(charId));
        public static void SendChat(string channel, string message)
        {
            byte ch = ChannelByte(channel);
            Send(new PacketBuilder(PacketOpcode.C2S_CHAT)
                .WriteByte(ch)
                .WriteString(message));
        }
        public static void SendChildFeed(long childId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHILD_FEED).WriteLong(childId));
        public static void SendChildList()
            => Send(new PacketBuilder(PacketOpcode.C2S_CHILD_LIST));
        public static void SendChildToggle(long childId, bool active)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHILD_TOGGLE)
                .WriteLong(childId).WriteBool(active));
        public static void SendClassChange(int classId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CLASS_CHANGE).WriteInt(classId));
        public static void SendClassSkillList()
            => Send(new PacketBuilder(PacketOpcode.C2S_SKILL_CLASS_LIST));
        public static void SendCosmeticEquip(long id)   { var p=new PacketBuilder(PacketOpcode.C2S_COSMETIC_EQUIP); p.WriteLong(id); Send(p); }
        public static void SendFarmFertilize(int plotIndex){ var p=new PacketBuilder(PacketOpcode.C2S_FARM_FERTILIZE); p.WriteInt(plotIndex); Send(p); }
        public static void SendAnimalBreed(int penIndex){ var p=new PacketBuilder(PacketOpcode.C2S_ANIMAL_BREED); p.WriteInt(penIndex); Send(p); }
        public static void SendFarmVisit(long ownerCharId){ var p=new PacketBuilder(PacketOpcode.C2S_FARM_VISIT); p.WriteLong(ownerCharId); Send(p); }

        public static void SendChildShop() => Send(new PacketBuilder(PacketOpcode.C2S_CHILD_SHOP));
        public static void SendChildBuy(long childId, int itemId){ var p=new PacketBuilder(PacketOpcode.C2S_CHILD_BUY); p.WriteLong(childId); p.WriteInt(itemId); Send(p); }
        public static void SendChildHireNanny(long childId, int itemId){ var p=new PacketBuilder(PacketOpcode.C2S_CHILD_HIRE_NANNY); p.WriteLong(childId); p.WriteInt(itemId); Send(p); }
        public static void SendChildInteract(long childId){ var p=new PacketBuilder(PacketOpcode.C2S_CHILD_INTERACT); p.WriteLong(childId); Send(p); }

        public static void SendFurnitureInteract(long furnitureId) { var p=new PacketBuilder(PacketOpcode.C2S_FURNITURE_INTERACT); p.WriteLong(furnitureId); Send(p); }
        public static void SendFurnitureStop()         => Send(new PacketBuilder(PacketOpcode.C2S_FURNITURE_STOP));
        public static void SendFurnitureBuy(int furnitureId) { var p=new PacketBuilder(PacketOpcode.C2S_FURNITURE_BUY); p.WriteInt(furnitureId); Send(p); }

        public static void SendFacilityPortals()      => Send(new PacketBuilder(PacketOpcode.C2S_FACILITY_PORTALS));
        public static void SendEnterFacility(string category) { var p=new PacketBuilder(PacketOpcode.C2S_ENTER_FACILITY); p.WriteString(category); Send(p); }
        public static void SendLeaveFacility()         => Send(new PacketBuilder(PacketOpcode.C2S_LEAVE_FACILITY));

        public static void SendCosmeticList()       => Send(new PacketBuilder(PacketOpcode.C2S_COSMETIC_LIST));
        public static void SendCosmeticUpgrade(long id) { var p=new PacketBuilder(PacketOpcode.C2S_COSMETIC_UPGRADE); p.WriteLong(id); Send(p); }
        public static void SendCrossChat(string message)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_CROSS)
                .WriteString(message));
        public static void SendDailyLoginClaim()
            => Send(new PacketBuilder(PacketOpcode.C2S_DAILY_LOGIN_CLAIM));
        public static void SendDailyLoginInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_DAILY_LOGIN_INFO));
        public static void SendDialogChoice(int dialogId, int choiceIdx)
            => Send(new PacketBuilder(PacketOpcode.C2S_DIALOG_CHOICE).WriteInt(dialogId).WriteInt(choiceIdx));
        public static void SendDialogStart(int npcId)
            => Send(new PacketBuilder(PacketOpcode.C2S_DIALOG_START).WriteInt(npcId));
        public static void SendDropItem(long instanceId, int qty = 1)
            => Send(new PacketBuilder(PacketOpcode.C2S_DROP_ITEM).WriteLong(instanceId).WriteInt(qty));
        public static void SendDuelRequest(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_DUEL_REQUEST).WriteLong(targetId));
        public static void SendDungeonEnter(int templateId)
            => Send(new PacketBuilder(PacketOpcode.C2S_DUNGEON_ENTER).WriteInt(templateId));
        public static void SendDungeonExit()
            => Send(new PacketBuilder(PacketOpcode.C2S_DUNGEON_EXIT));
        public static void SendDungeonList()
            => Send(new PacketBuilder(PacketOpcode.C2S_DUNGEON_LIST));
        public static void SendEmoji(string channel, int emojiUnicodeCode)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_EMOJI)
                .WriteByte(ChannelByte(channel))
                .WriteInt(emojiUnicodeCode));
        public static void SendEmote(int emoteId)
            => Send(new PacketBuilder(PacketOpcode.C2S_EMOTE).WriteInt(emoteId));
        public static void SendEnhanceItem(long instanceId)
            => Send(new PacketBuilder(PacketOpcode.C2S_ENHANCE_ITEM).WriteLong(instanceId));
        public static void SendEquipItem(long instanceId)
            => Send(new PacketBuilder(PacketOpcode.C2S_EQUIP_ITEM).WriteLong(instanceId));
        public static void SendEventCurrencyBuy(int shopItemId)
            => Send(new PacketBuilder(PacketOpcode.C2S_EVENT_CURRENCY_BUY).WriteInt(shopItemId));
        public static void SendEventCurrencyExchange(int currencyId, int amount)
            => Send(new PacketBuilder(PacketOpcode.C2S_EVENT_CURRENCY_EXCHANGE).WriteInt(currencyId).WriteInt(amount));
        public static void SendEventCurrencyList()
            => Send(new PacketBuilder(PacketOpcode.C2S_EVENT_CURRENCY_LIST));
        public static void SendEventCurrencyShop(int currencyId)
            => Send(new PacketBuilder(PacketOpcode.C2S_EVENT_CURRENCY_SHOP).WriteInt(currencyId));
        public static void SendFarmHarvest(int plotIndex)
            => Send(new PacketBuilder(PacketOpcode.C2S_FARM_HARVEST).WriteInt(plotIndex));
        public static void SendFarmPlant(int plotIndex, int seedItemId)
            => Send(new PacketBuilder(PacketOpcode.C2S_FARM_PLANT).WriteInt(plotIndex).WriteInt(seedItemId));
        public static void SendFarmState()
            => Send(new PacketBuilder(PacketOpcode.C2S_FARM_STATE));
        public static void SendFarmWater(int plotIndex)
            => Send(new PacketBuilder(PacketOpcode.C2S_FARM_WATER).WriteInt(plotIndex));
        public static void SendFriendRequest(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_FRIEND_REQUEST).WriteLong(targetId));
        public static void SendGachaBannerList()
            => Send(new PacketBuilder(PacketOpcode.C2S_GACHA_BANNER_LIST));
        public static void SendGachaBuyTicket(int currencyId, int amount)
            => Send(new PacketBuilder(PacketOpcode.C2S_GACHA_BUY_TICKET).WriteInt(currencyId).WriteInt(amount));
        public static void SendGachaCurrency()
            => Send(new PacketBuilder(PacketOpcode.C2S_GACHA_CURRENCY));
        public static void SendGachaHistory(int bannerId)
            => Send(new PacketBuilder(PacketOpcode.C2S_GACHA_HISTORY).WriteInt(bannerId));
        public static void SendGachaPull(int bannerId, int pullCount)
            => Send(new PacketBuilder(PacketOpcode.C2S_GACHA_PULL).WriteInt(bannerId).WriteInt(pullCount));
        public static void SendGemSocket(int equipSlot, int gemId, int socketIdx)
            => Send(new PacketBuilder(PacketOpcode.C2S_GEM_SOCKET).WriteInt(equipSlot).WriteInt(gemId).WriteInt(socketIdx));
        public static void SendGiftCode(string code)
            => Send(new PacketBuilder(PacketOpcode.C2S_GIFTCODE).WriteString(code));
        public static void SendGrabEnvelope(long envelopeId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_GRAB_ENVELOPE)
                .WriteLong(envelopeId));
        public static void SendGuildAccept(long guildId)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_ACCEPT).WriteLong(guildId));
        public static void SendGuildCreate(string name)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_CREATE).WriteString(name));
        public static void SendGuildDisband()
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_DISBAND));
        public static void SendGuildInfo(long guildId = 0)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_INFO).WriteLong(guildId));
        public static void SendGuildInvite(long targetCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_INVITE).WriteLong(targetCharId));
        public static void SendGuildKick(long targetCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_KICK).WriteLong(targetCharId));
        public static void SendGuildLeave()
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_LEAVE));
        public static void SendGuildPromote(long targetCharId, byte role)
            => Send(new PacketBuilder(PacketOpcode.C2S_GUILD_PROMOTE).WriteLong(targetCharId).WriteByte(role));
        public static void SendHouseCatalog()
            => Send(new PacketBuilder(PacketOpcode.C2S_HOUSE_CATALOG));
        public static void SendHouseFurnitureList()
            => Send(new PacketBuilder(PacketOpcode.C2S_HOUSE_FURNITURE));
        public static void SendHouseInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_HOUSE_INFO));
        public static void SendHousePlace(int furnitureId, float x, float y, int rotation)
            => Send(new PacketBuilder(PacketOpcode.C2S_HOUSE_PLACE)
                .WriteInt(furnitureId).WriteFloat(x).WriteFloat(y).WriteInt(rotation));
        public static void SendHouseRemove(long instanceId)
            => Send(new PacketBuilder(PacketOpcode.C2S_HOUSE_REMOVE).WriteLong(instanceId));
        public static void SendInspectPlayer(long charId)
            => Send(new PacketBuilder(PacketOpcode.C2S_INSPECT_PLAYER).WriteLong(charId));
        public static void SendIntroComplete()
            => Send(new PacketBuilder(PacketOpcode.C2S_INTRO_COMPLETE));
        public static void SendIntroRequest()
            => Send(new PacketBuilder(PacketOpcode.C2S_INTRO_REQUEST));
        public static void SendIntroSkip()
            => Send(new PacketBuilder(PacketOpcode.C2S_INTRO_SKIP));
        public static void SendIntroVideoConfigRequest()
            => Send(new PacketBuilder(PacketOpcode.C2S_INTRO_VIDEO_CONFIG));
        public static void SendInventoryList()
            => Send(new PacketBuilder(PacketOpcode.C2S_INVENTORY_OPEN));
        public static void SendItemShowcase(string channel, long instanceId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_ITEM)
                .WriteByte(ChannelByte(channel))
                .WriteLong(instanceId));
        public static void SendLangSet(string langCode)
            => Send(new PacketBuilder(PacketOpcode.C2S_LANG_SET).WriteString(langCode));
        public static void SendLeaderboard(string rankType, int serverId = 0)
            => Send(new PacketBuilder(PacketOpcode.C2S_LEADERBOARD).WriteString(rankType).WriteInt(serverId));
        public static void SendLocation(string channel, int mapId, float x, float y)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_LOCATION)
                .WriteByte(ChannelByte(channel))
                .WriteInt(mapId)
                .WriteFloat(x)
                .WriteFloat(y));
        public static void SendLogin(string username, string password)
            => Send(new PacketBuilder(PacketOpcode.C2S_LOGIN).WriteString(username).WriteString(password));
        public static void SendLoginScreenConfig()
            => Send(new PacketBuilder(PacketOpcode.C2S_LOGIN_SCREEN_CFG));
        public static void SendMailClaim(long mailId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MAIL_CLAIM).WriteLong(mailId));
        public static void SendMailDelete(long mailId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MAIL_DELETE).WriteLong(mailId));
        public static void SendMailList()
            => Send(new PacketBuilder(PacketOpcode.C2S_MAIL_LIST));
        public static void SendMailRead(long mailId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MAIL_READ).WriteLong(mailId));
        public static void SendMapChange(int portalId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MAP_CHANGE).WriteInt(portalId));
        public static void SendMapLoadDone()
            => Send(new PacketBuilder(PacketOpcode.C2S_MAP_LOAD_DONE));
        public static void SendMentorAccept(long mentorId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MENTOR_ACCEPT).WriteLong(mentorId));
        public static void SendMentorGraduate()
            => Send(new PacketBuilder(PacketOpcode.C2S_MENTOR_GRADUATE));
        public static void SendMentorInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_MENTOR_INFO));
        public static void SendMinigameAction(long roomId, int actionType, int value)
            => Send(new PacketBuilder(PacketOpcode.C2S_MINIGAME_ACTION).WriteLong(roomId).WriteInt(actionType).WriteInt(value));
        public static void SendMinigameAnswer(long roomId, byte answerIdx)
            => Send(new PacketBuilder(PacketOpcode.C2S_MINIGAME_ANSWER)
                .WriteLong(roomId).WriteByte(answerIdx));
        public static void SendMinigameBet(long roomId, int symbol, int amount)
            => Send(new PacketBuilder(PacketOpcode.C2S_MINIGAME_BET)
                .WriteLong(roomId).WriteInt(symbol).WriteInt(amount));
        public static void SendMinigameCreate(string gameType, int minBet, int maxBet, byte currency)
            => Send(new PacketBuilder(PacketOpcode.C2S_MINIGAME_CREATE)
                .WriteString(gameType).WriteInt(minBet).WriteInt(maxBet).WriteByte(currency));
        public static void SendMinigameJoin(long roomId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MINIGAME_JOIN).WriteLong(roomId));
        public static void SendMinigameLeave(long roomId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MINIGAME_LEAVE).WriteLong(roomId));
        public static void SendMinigameRoomList(string gameType)
            => Send(new PacketBuilder(PacketOpcode.C2S_MINIGAME_ROOM_LIST).WriteString(gameType));
        public static void SendMissionPassClaim(int level)
            => Send(new PacketBuilder(PacketOpcode.C2S_MISSION_PASS_CLAIM).WriteInt(level));
        public static void SendMissionPassInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_MISSION_PASS_INFO));
        public static void SendMountList()
            => Send(new PacketBuilder(PacketOpcode.C2S_MOUNT_LIST));
        public static void SendMountSetActive(long mountId)
            => Send(new PacketBuilder(PacketOpcode.C2S_MOUNT_SET_ACTIVE).WriteLong(mountId));
        public static void SendMove(float x, float y, byte direction)
            => Send(new PacketBuilder(PacketOpcode.C2S_MOVE)
                .WriteFloat(x).WriteFloat(y).WriteByte(direction));
        public static void SendNewsRequest()
            => Send(new PacketBuilder(PacketOpcode.C2S_NEWS_LIST));
        public static void SendPairAction(int actionId, long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PAIR_ACTION).WriteInt(actionId).WriteLong(targetId));
        public static void SendPairActionAccept(long requesterId, bool accept)
            => Send(new PacketBuilder(PacketOpcode.C2S_PAIR_ACTION_REPLY).WriteLong(requesterId).WriteBool(accept));
        public static void SendPartyAccept(long leaderCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_ACCEPT).WriteLong(leaderCharId));
        public static void SendPartyCreate()
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_CREATE));
        public static void SendPartyDisband()
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_DISBAND));
        public static void SendPartyInvite(long targetCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_INVITE).WriteLong(targetCharId));
        public static void SendPartyKick(long charId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_KICK).WriteLong(charId));
        public static void SendPartyLeave()
            => Send(new PacketBuilder(PacketOpcode.C2S_PARTY_LEAVE));
        public static void SendPassClaim(int level, byte tier)
            => Send(new PacketBuilder(PacketOpcode.C2S_PASS_CLAIM)
                .WriteInt(level).WriteByte(tier));
        public static void SendPassInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_PASS_INFO));
        public static void SendPetFeed(long petId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PET_FEED).WriteLong(petId));
        public static void SendPetList()
            => Send(new PacketBuilder(PacketOpcode.C2S_PET_LIST));
        public static void SendPetSetActive(long petId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PET_SET_ACTIVE).WriteLong(petId));
        public static void SendPing()
            => Send(new PacketBuilder(PacketOpcode.C2S_PING));
        public static void SendPropose(long targetCharId, int ringItemId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PROPOSE)
                .WriteLong(targetCharId).WriteInt(ringItemId));
        public static void SendPvPAttack(long targetCharId, int skillId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_ATTACK).WriteLong(targetCharId).WriteInt(skillId));
        public static void SendPvpAttack(long targetCharId, int skillId = 0)
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_ATTACK_PVP).WriteLong(targetCharId).WriteInt(skillId));
        public static void SendPvpChallenge(long targetCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_CHALLENGE).WriteLong(targetCharId));
        public static void SendPvpRespond(long challengerId, bool accept)
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_RESPOND).WriteLong(challengerId).WriteBool(accept));
        public static void SendPvpSeasonInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_SEASON_INFO));
        public static void SendPvpSeasonRank(int page)
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_SEASON_RANK).WriteInt(page));
        public static void SendPvpSeasonReward()
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_SEASON_REWARD));
        public static void SendPvpSurrender()
            => Send(new PacketBuilder(PacketOpcode.C2S_PVP_SURRENDER));
        public static void SendQuestAbandon(int questId)
            => Send(new PacketBuilder(PacketOpcode.C2S_QUEST_ABANDON).WriteInt(questId));
        public static void SendQuestAccept(int questId)
            => Send(new PacketBuilder(PacketOpcode.C2S_QUEST_ACCEPT).WriteInt(questId));
        public static void SendQuestComplete(int questId)
            => Send(new PacketBuilder(PacketOpcode.C2S_QUEST_COMPLETE).WriteInt(questId));
        public static void SendQuestList()
            => Send(new PacketBuilder(PacketOpcode.C2S_QUEST_LIST));
        public static void SendRedEnvelope(string channel, int totalAmount, byte maxGrabbers,
                                            byte currency, string message)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_RED_ENVELOPE)
                .WriteByte(ChannelByte(channel))
                .WriteInt(totalAmount)
                .WriteByte(maxGrabbers)
                .WriteByte(currency)
                .WriteString(message));
        public static void SendRefine(int equipSlot)
            => Send(new PacketBuilder(PacketOpcode.C2S_REFINE).WriteInt(equipSlot));
        public static void SendRegister(string username, string password)
            => Send(new PacketBuilder(PacketOpcode.C2S_REGISTER).WriteString(username).WriteString(password));
        public static void SendReport(long targetId, string reason)
            => Send(new PacketBuilder(PacketOpcode.C2S_REPORT_PLAYER).WriteLong(targetId).WriteString(reason));
        public static void SendReputationClaim(int factionId, int tier) { var p=new PacketBuilder(PacketOpcode.C2S_REPUTATION_CLAIM); p.WriteInt(factionId); p.WriteInt(tier); Send(p); }
        public static void SendReputationList()      => Send(new PacketBuilder(PacketOpcode.C2S_REPUTATION_LIST));
        public static void SendServerList()
            => Send(new PacketBuilder(PacketOpcode.C2S_SERVER_LIST));
        public static void SendServerSelect(int serverId)
            => Send(new PacketBuilder(PacketOpcode.C2S_SERVER_SELECT).WriteInt(serverId));
        public static void SendSetInfo()             => Send(new PacketBuilder(PacketOpcode.C2S_SET_INFO));
        public static void SendSettingsLoad()
            => Send(new PacketBuilder(PacketOpcode.C2S_SETTINGS_LOAD));
        public static void SendSettingsSave(string settingsJson)
            => Send(new PacketBuilder(PacketOpcode.C2S_SETTINGS_SAVE).WriteString(settingsJson));
        public static void SendShopBuy(int itemId, int qty)
            => Send(new PacketBuilder(PacketOpcode.C2S_SHOP_BUY)
                .WriteInt(itemId).WriteInt(qty));
        public static void SendShopOpen(int shopId)
            => Send(new PacketBuilder(PacketOpcode.C2S_SHOP_OPEN).WriteInt(shopId));
        public static void SendShopSell(long instanceId, int qty)
            => Send(new PacketBuilder(PacketOpcode.C2S_SHOP_SELL)
                .WriteLong(instanceId).WriteInt(qty));
        public static void SendSkillLearn(int skillId)
            => Send(new PacketBuilder(PacketOpcode.C2S_SKILL_LEARN).WriteInt(skillId));
        public static void SendSkillList()
            => Send(new PacketBuilder(PacketOpcode.C2S_SKILL_LIST));
        public static void SendSkillSetSlot(byte slot, int skillId)
            => Send(new PacketBuilder(PacketOpcode.C2S_SKILL_SET_SLOT).WriteByte(slot).WriteInt(skillId));
        public static void SendSkillUpgrade(int skillId)
            => Send(new PacketBuilder(PacketOpcode.C2S_SKILL_UPGRADE).WriteInt(skillId));
        public static void SendSocialLink(string provider, string token)
            => Send(new PacketBuilder(PacketOpcode.C2S_SOCIAL_LINK).WriteString(provider).WriteString(token));
        public static void SendSocialLogin(string provider, string token)
            => Send(new PacketBuilder(PacketOpcode.C2S_SOCIAL_LOGIN).WriteString(provider).WriteString(token));
        public static void SendSocialUnlink(string provider)
            => Send(new PacketBuilder(PacketOpcode.C2S_SOCIAL_UNLINK).WriteString(provider));
        public static void SendStartDating(long targetCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_START_DATING).WriteLong(targetCharId));
        public static void SendSticker(string channel, int stickerId)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_STICKER)
                .WriteByte(ChannelByte(channel))
                .WriteInt(stickerId));
        public static void SendStudentList()
            => Send(new PacketBuilder(PacketOpcode.C2S_STUDENT_LIST));
        public static void SendTeleport(int mapId, float x, float y)
            => Send(new PacketBuilder(PacketOpcode.C2S_TELEPORT).WriteInt(mapId).WriteFloat(x).WriteFloat(y));
        public static void SendTitleEquip(int titleId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TITLE_EQUIP).WriteInt(titleId));
        public static void SendTitleList()
            => Send(new PacketBuilder(PacketOpcode.C2S_TITLE_LIST));
        public static void SendTopupBuy(int packageId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TOPUP_BUY).WriteInt(packageId));
        public static void SendTopupHistory()
            => Send(new PacketBuilder(PacketOpcode.C2S_TOPUP_HISTORY));
        public static void SendTopupPackages()
            => Send(new PacketBuilder(PacketOpcode.C2S_TOPUP_PACKAGES));
        public static void SendTrade(long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_REQUEST).WriteLong(targetId));
        public static void SendTradeAddItem(long tradeId, long inventoryId, int qty)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_ADD_ITEM).WriteLong(tradeId).WriteLong(inventoryId).WriteInt(qty));
        public static void SendTradeCancel(long tradeId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_CANCEL).WriteLong(tradeId));
        public static void SendTradeConfirm(long tradeId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_CONFIRM).WriteLong(tradeId));
        public static void SendTradeRequest(long targetCharId)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_REQUEST).WriteLong(targetCharId));
        public static void SendTradeRespond(long fromCharId, bool accept)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_RESPOND).WriteLong(fromCharId).WriteBool(accept));
        public static void SendTradeSetGold(long tradeId, long gold)
            => Send(new PacketBuilder(PacketOpcode.C2S_TRADE_SET_GOLD).WriteLong(tradeId).WriteLong(gold));
        public static void SendTutorialProgress(string stepKey)
            => Send(new PacketBuilder(PacketOpcode.C2S_TUTORIAL_PROGRESS).WriteString(stepKey));
        public static void SendTutorialSkip()
            => Send(new PacketBuilder(PacketOpcode.C2S_TUTORIAL_SKIP));
        public static void SendUnequipItem(int slot)
            => Send(new PacketBuilder(PacketOpcode.C2S_UNEQUIP_ITEM).WriteInt(slot));
        public static void SendUseItem(long instanceId)
            => Send(new PacketBuilder(PacketOpcode.C2S_USE_ITEM).WriteLong(instanceId));
        public static void SendUseSkill(int skillId, long targetId)
            => Send(new PacketBuilder(PacketOpcode.C2S_USE_SKILL)
                .WriteInt(skillId).WriteLong(targetId));
        public static void SendVoiceUrl(string channel, int durationMs, string url)
            => Send(new PacketBuilder(PacketOpcode.C2S_CHAT_VOICE)
                .WriteByte(ChannelByte(channel))
                .WriteInt(durationMs)
                .WriteString(url));
        // Kho: action 0=cất, 1=lấy, 2=xem danh sách, 3=bán từ kho
        public static void SendWarehouse(int action, int itemId, int qty)
            => Send(new PacketBuilder(PacketOpcode.C2S_WAREHOUSE).WriteInt(action).WriteInt(itemId).WriteInt(qty));
        public static void SendWarehouseList()        => SendWarehouse(2, 0, 0);
        public static void SendWarehouseDeposit(int itemId, int qty) => SendWarehouse(0, itemId, qty);
        public static void SendWarehouseWithdraw(int itemId, int qty)=> SendWarehouse(1, itemId, qty);
        public static void SendWarehouseSell(int itemId, int qty)    => SendWarehouse(3, itemId, qty);

        // AFK
        public static void SendAfkCardList() => Send(new PacketBuilder(PacketOpcode.C2S_AFK_CARD_LIST));
        public static void SendAfkBuy(int cardId) => Send(new PacketBuilder(PacketOpcode.C2S_AFK_BUY).WriteInt(cardId));
        public static void SendAfkClaim() => Send(new PacketBuilder(PacketOpcode.C2S_AFK_CLAIM));
        public static void SendAfkStop() => Send(new PacketBuilder(PacketOpcode.C2S_AFK_STOP));
        public static void SendAfkStatus() => Send(new PacketBuilder(PacketOpcode.C2S_AFK_STATUS));
        // Chợ
        public static void SendMarketList(string category, int currency, int page)
            => Send(new PacketBuilder(PacketOpcode.C2S_MARKET_LIST).WriteString(category).WriteInt(currency).WriteInt(page));
        public static void SendMarketSell(long invId, int qty, int currency, long price)
            => Send(new PacketBuilder(PacketOpcode.C2S_MARKET_SELL).WriteLong(invId).WriteInt(qty).WriteByte((byte)currency).WriteLong(price));
        public static void SendMarketBuy(long listingId) => Send(new PacketBuilder(PacketOpcode.C2S_MARKET_BUY).WriteLong(listingId));
        public static void SendMarketCancel(long listingId) => Send(new PacketBuilder(PacketOpcode.C2S_MARKET_CANCEL).WriteLong(listingId));
        public static void SendMarketMine() => Send(new PacketBuilder(PacketOpcode.C2S_MARKET_MINE));
        // PK mode
        public static void SendSetCombatMode(string mode) => Send(new PacketBuilder(PacketOpcode.C2S_SET_COMBAT_MODE).WriteString(mode));
        public static void SendPkStatus() => Send(new PacketBuilder(PacketOpcode.C2S_PK_STATUS));
        // World boss
        public static void SendWorldBossInfo() => Send(new PacketBuilder(PacketOpcode.C2S_WORLDBOSS_INFO));
        public static void SendWorldBossAttack(int bossId, long dmg) => Send(new PacketBuilder(PacketOpcode.C2S_WORLDBOSS_ATTACK).WriteInt(bossId).WriteLong(dmg));
        public static void SendWorldBossRank(int bossId) => Send(new PacketBuilder(PacketOpcode.C2S_WORLDBOSS_RANK).WriteInt(bossId));
        // Guild war
        public static void SendGuildWarInfo() => Send(new PacketBuilder(PacketOpcode.C2S_GUILDWAR_INFO));
        public static void SendGuildWarDeclare(int targetGuild) => Send(new PacketBuilder(PacketOpcode.C2S_GUILDWAR_DECLARE).WriteInt(targetGuild));
        public static void SendGuildWarJoin(int warId) => Send(new PacketBuilder(PacketOpcode.C2S_GUILDWAR_JOIN).WriteInt(warId));
        // Ngoại vực
        public static void SendOuterFloors() => Send(new PacketBuilder(PacketOpcode.C2S_OUTER_FLOORS));
        public static void SendOuterEnter(int floor) => Send(new PacketBuilder(PacketOpcode.C2S_OUTER_ENTER).WriteInt(floor));
        public static void SendOuterLeave() => Send(new PacketBuilder(PacketOpcode.C2S_OUTER_LEAVE));
        // VIP
        public static void SendVipInfo() => Send(new PacketBuilder(PacketOpcode.C2S_VIP_INFO));
        public static void SendVipClaim(int vipLevel) => Send(new PacketBuilder(PacketOpcode.C2S_VIP_CLAIM).WriteInt(vipLevel));
        public static void SendVipDaily() => Send(new PacketBuilder(PacketOpcode.C2S_VIP_DAILY));
        // Hoat Dong
        public static void SendActivityList() => Send(new PacketBuilder(PacketOpcode.C2S_ACTIVITY_LIST));
        public static void SendActivityDetail(int id) => Send(new PacketBuilder(PacketOpcode.C2S_ACTIVITY_DETAIL).WriteInt(id));
        public static void SendActivityClaim(int id, int order) => Send(new PacketBuilder(PacketOpcode.C2S_ACTIVITY_CLAIM).WriteInt(id).WriteInt(order));
        public static void SendActivityExchange(int id, int milestoneId) => Send(new PacketBuilder(PacketOpcode.C2S_ACTIVITY_EXCHANGE).WriteInt(id).WriteInt(milestoneId));
        public static void SendActivityJoin(int id) => Send(new PacketBuilder(PacketOpcode.C2S_ACTIVITY_JOIN).WriteInt(id));
        public static void SendActivityRanking(int id) => Send(new PacketBuilder(PacketOpcode.C2S_ACTIVITY_RANKING).WriteInt(id));
        // Voice: context 1=class_intro 2=npc_bark
        public static void SendVoiceRequest(int context, int refId) => Send(new PacketBuilder(PacketOpcode.C2S_VOICE_REQUEST).WriteByte((byte)context).WriteInt(refId));
        public static void SendClassIntroVoice(int classId) => SendVoiceRequest(1, classId);
        public static void SendNpcBark(int npcId) => SendVoiceRequest(2, npcId);
        public static void SendSoundConfig() => Send(new PacketBuilder(PacketOpcode.C2S_SOUND_CONFIG));
        public static void SendFxConfig() => Send(new PacketBuilder(PacketOpcode.C2S_FX_CONFIG));
        public static void SendWedding(long targetCharId, int weddingMapId)
            => Send(new PacketBuilder(PacketOpcode.C2S_WEDDING)
                .WriteLong(targetCharId).WriteInt(weddingMapId));
        public static void SendWorldBossInfo()
            => Send(new PacketBuilder(PacketOpcode.C2S_WORLD_BOSS_INFO));
    }

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
