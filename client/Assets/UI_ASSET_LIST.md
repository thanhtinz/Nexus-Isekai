# HUD & UI ASSETS REQUIRED

T = Static (1 PNG)
D = Animated (spritesheet)
9S = 9-slice (1 PNG, stretchable)
2L = Bilingual (need VI + EN version = x2 files)

NOTE: Most UI elements are text-free frames/icons.
Text is rendered dynamically from localization (vi.json / en.json).
Items marked 2L have baked text in the image and need both languages.

## 1. MAIN HUD (Gameplay Screen)
- HP bar frame              | T | 1
- HP bar fill               | T | 1
- MP bar frame              | T | 1
- MP bar fill               | T | 1
- EXP bar frame             | T | 1
- EXP bar fill              | T | 1
- Avatar frame              | T | 1
- Level badge               | T | 1
- Minimap frame             | T | 1
- Minimap icon: player      | T | 1
- Minimap icon: NPC         | T | 1
- Minimap icon: monster     | T | 1
- Minimap icon: quest       | T | 1
- Minimap icon: portal      | T | 1
- Minimap icon: shop        | T | 1
- Joystick base             | T | 1
- Joystick knob             | T | 1
- Skill slot frame          | T | 1 (shared for 7 slots)
- Cooldown overlay          | T | 1 (dark transparent)
- Auto-attack button        | T | 2 (on/off)
- Quick item slot           | T | 1
- Menu button               | T | 1
- Chat button               | T | 1
- Notification bell         | T | 1
- Notification count badge  | T | 1
Total: 26

## 2. BUTTONS
- Default button            | T | 3 (normal/pressed/disabled) text-free frame
- Confirm button (green)    | T | 3 text-free frame
- Cancel button (red/gray)  | T | 3 text-free frame
- Tab active                | T | 1
- Tab inactive              | T | 1
- Close button (X)          | T | 1
- Back button               | T | 1
- Checkbox                  | T | 2 (on/off)
- Radio button              | T | 2 (on/off)
- Toggle switch             | T | 2 (on/off)
- Slider bar                | T | 1
- Slider handle             | T | 1
- Dropdown frame            | T | 1
- Dropdown arrow            | T | 1
- Scrollbar track           | T | 1
- Scrollbar thumb           | T | 1
Total: 26

## 3. FRAMES & PANELS
- Dialog frame              | 9S | 1
- Panel background          | 9S | 1
- Tooltip frame             | 9S | 1
- Input field frame         | 9S | 1
- Modal dark overlay        | T  | 1
- Separator line            | T  | 1
- Tab bar background        | 9S | 1
Total: 7

## 4. INVENTORY & EQUIPMENT
- Inventory slot empty      | T | 1
- Inventory slot selected   | T | 1
- Equipment slot icons (25) | T | 25
  Helmet, Armor, Pants, Boots, Gloves, Shield, Spellbook,
  Quiver, Talisman, Charm, Skin, Ring x2, Necklace,
  Earring x2, Bracelet x2, Wings, Cape, Mask, Title,
  Mount, Pet, Weapon
- Quality border Common     | T | 1
- Quality border Rare       | T | 1
- Quality border Epic       | T | 1
- Quality border Legendary  | T | 1
- Quality border Mythic     | T | 1
- Lock/bound icon           | T | 1
- Enhancement glow          | D | 4 (loop 4 frames)
- Gem socket empty          | T | 1
- Gem socket filled         | T | 1
- Quantity badge             | T | 1
Total: 41

## 5. CHARACTER CREATION
- Class card (7 classes)    | T | 7
- Class card selected glow  | T | 1
- Gender toggle male        | T | 1
- Gender toggle female      | T | 1
- Preview frame             | T | 1
- Stat preview bar          | T | 2 (frame + fill)
- Create button             | 2L | 6 (Create / Tao NV, 3 states each)
Total: 16

## 6. CHAT
- Chat box background      | 9S | 1
- Chat tab active           | T  | 1
- Chat tab inactive         | T  | 1
- Chat input frame          | 9S | 1
- Send button               | 2L | 2 (Send / Gui)
- Emoji button              | T  | 1
- Sticker button            | T  | 1
- Voice button              | T  | 2 (idle/recording)
- Chat bubble               | 9S | 1
- System message frame      | 9S | 1
Total: 11

## 7. SHOP & TOPUP
- Product card frame        | 9S | 1
- Gold icon                 | T  | 1
- Diamond icon              | T  | 1
- Event token icon          | T  | 1
- Buy button                | 2L | 2 (Buy / Mua)
- Sell button               | 2L | 2 (Sell / Ban)
- Quantity selector +       | T  | 1
- Quantity selector -       | T  | 1
- Topup package frame      | 9S | 1
- Badge hot                 | 2L | 2 (Hot / Hot)
- Badge best                | 2L | 2 (Best Value / Gia Tri Nhat)
- Badge x2                  | T  | 1 (x2 - no text needed)
- Badge limited             | 2L | 2 (Limited / Gioi Han)
- Badge new                 | 2L | 2 (New / Moi)
Total: 18

## 8. GACHA / SUMMON
- Banner frame              | 9S | 1
- Pull x1 button            | 2L | 2 (Pull x1 / Keo x1)
- Pull x10 button           | 2L | 2 (Pull x10 / Keo x10)
- Summon open effect        | D  | 8 (spritesheet 8 frames)
- Result card N             | T  | 1
- Result card R             | T  | 1
- Result card SR            | T  | 1
- Result card SSR           | T  | 1
- Result card UR            | T  | 1
- Pity bar frame            | T  | 1
- Pity bar fill             | T  | 1
- Ticket standard icon      | T  | 1
- Ticket limited icon       | T  | 1
- Key pet icon              | T  | 1
- Key mount icon            | T  | 1
- Weapon shard icon         | T  | 1
Total: 18

## 9. QUEST & DIALOG
- Quest tracker frame       | 9S | 1
- Quest marker accept (!)   | T  | 1
- Quest marker complete (?) | T  | 1
- Quest marker ongoing      | T  | 1
- NPC dialog box            | 9S | 1
- Dialog choice button      | T  | 1
- NPC portrait frame        | T  | 1
- Quest progress bar        | T  | 2 (frame + fill)
- Reward preview slot       | T  | 1
Total: 10

## 10. PARTY & GUILD
- Party member frame        | 9S | 1
- Mini HP bar               | T  | 2 (frame + fill)
- Leader crown icon         | T  | 1
- Officer icon              | T  | 1
- Member icon               | T  | 1
- Guild emblem frame        | T  | 1
- Online dot (green)        | T  | 1
- Offline dot (gray)        | T  | 1
Total: 9

## 11. PVP ARENA
- Tier icon Bronze          | T  | 1
- Tier icon Silver          | T  | 1
- Tier icon Gold            | T  | 1
- Tier icon Platinum        | T  | 1
- Tier icon Diamond         | T  | 1
- Tier icon Master          | T  | 1
- Tier icon Grandmaster     | T  | 1
- ELO bar frame             | T  | 1
- ELO bar fill              | T  | 1
- Victory frame             | 2L | 2 (Victory / Chien Thang)
- Defeat frame              | 2L | 2 (Defeat / That Bai)
Total: 11

## 12. COMBAT
- Damage font digits 0-9   | T  | 10
- Critical hit burst        | D  | 4 (4 frames)
- MISS text                 | 2L | 2 (MISS / Truot)
- Level up effect           | D  | 6 (6 frames)
- Death overlay             | T  | 1
- Respawn button            | 2L | 2 (Respawn / Hoi Sinh)
- Buff icons (generic)      | T  | 10
- Debuff icons (generic)    | T  | 10
Total: 33

## 13. NOTIFICATIONS & POPUPS
- Toast frame               | 9S | 1
- Achievement banner        | T  | 1
- Daily login slot          | T  | 2 (claimed/unclaimed)
- World boss alert banner   | T  | 1
- Event banner              | T  | 1
- Loading bar frame         | T  | 1
- Loading bar fill          | T  | 1
- Loading background        | T  | 1
- Splash logo               | T  | 1
Total: 10

## 14. SETTINGS (6 tabs)
- Settings panel bg         | 9S | 1
- Tab Game icon             | T  | 1
- Tab Graphics icon         | T  | 1
- Tab Audio icon            | T  | 1
- Tab Controls icon         | T  | 1
- Tab Network icon          | T  | 1
- Tab Account icon          | T  | 1
- Section header bg         | 9S | 1
- Volume slider bar         | T  | 1 (or shared from #2)
- Volume slider handle      | T  | 1 (or shared from #2)
- Quality selector frames   | T  | 5 (Very Low/Low/Medium/High/Ultra)
- FPS selector frames       | T  | 4 (30/60/90/120)
- Key binding slot          | T  | 1
- Reset defaults button     | 2L | 2 (Reset / Mac Dinh)
- Save button               | 2L | 2 (Save / Luu)
- Joystick size preview     | T  | 1
- Camera sensitivity preview| T  | 1
Total: 19

## 15. MAP
- Fullscreen map frame      | 9S | 1
- Zoom in button            | T  | 1
- Zoom out button           | T  | 1
- Waypoint marker           | T  | 1
- Teleport icon             | T  | 1
- Area name label frame     | 9S | 1
- Fog overlay               | T  | 1
Total: 7

## 16. ENHANCEMENT
- Enhancement center slot   | T  | 1
- Material slot             | T  | 1
- Success effect            | D  | 6 (6 frames)
- Failure effect            | D  | 4 (4 frames)
- Chance % bar              | T  | 2 (frame + fill)
- Gem socket UI slot        | T  | 1
- Gem insert effect         | D  | 4 (4 frames)
Total: 8

## 17. SYSTEM ICONS
- 7 class icons             | T  | 7
- 12 stat icons             | T  | 12
  HP, MP, ATK, DEF, Crit, Dodge,
  Accuracy, Speed, Lifesteal, Resist, ASPD, MSPD
- 10 menu icons             | T  | 10
  Settings, Mail, Friends, Guild, Party,
  Shop, Inventory, Skills, Quest, Map
- 4 currency icons          | T  | 4
  Gold, Diamond, Event Token, Gacha Ticket
- 5 social icons            | T  | 5
  Chat, Voice, Sticker, Block, Report
- 4 NPC type icons          | T  | 4
  Merchant, Quest giver, Teleporter, Storage
Total: 42

## 18. INTRO CUTSCENE
- Intro backgrounds (7)     | T  | 7
- Text box frame            | 9S | 1
- Skip button               | 2L | 2 (Skip / Bo Qua)
- Continue button           | 2L | 2 (Continue / Tiep Tuc)
- Fade overlay              | T  | 1
Total: 11

## 19. MAIL
- Mail list frame           | 9S | 1
- Unread mail icon          | T  | 1
- Read mail icon            | T  | 1
- Attachment icon           | T  | 1
- Mail detail frame         | 9S | 1
- Claim reward button       | 2L | 2 (Claim / Nhan)
- Delete button             | T  | 1
Total: 7

## 20. TRADE
- Trade window frame        | 9S | 1
- Trade slot (left + right) | T  | 2
- Confirm trade button      | 2L | 2 (Confirm / Xac Nhan)
- Cancel trade button       | 2L | 2 (Cancel / Huy)
- Lock trade icon           | T  | 1
- Gold input bar            | T  | 1
Total: 7

## 21. AUCTION HOUSE
- Auction list frame        | 9S | 1
- Auction item card         | 9S | 1
- Bid button                | 2L | 2 (Bid / Dau Gia)
- Buyout button             | 2L | 2 (Buyout / Mua Ngay)
- Sell button               | 2L | 2 (Sell / Ban)
- Tab Buy/Sell/History      | 2L | 6 (Buy-Sell-History / Mua-Ban-Lich Su)
- Time remaining icon       | T  | 1
Total: 9

## 22. ACHIEVEMENT
- Achievement list frame    | 9S | 1
- Achievement card locked   | T  | 1
- Achievement card unlocked | T  | 1
- Progress bar              | T  | 2 (frame + fill)
- Claim reward button       | 2L | 2 (Claim / Nhan)
- Category tabs (6)         | T  | 6
Total: 12

## 23. TUTORIAL
- Highlight overlay (dark)  | T  | 1
- Arrow up                  | T  | 1
- Arrow down                | T  | 1
- Arrow left                | T  | 1
- Arrow right               | T  | 1
- Guide text frame          | 9S | 1
- Continue button           | 2L | 2 (Continue / Tiep Tuc)
- Skip button               | 2L | 2 (Skip / Bo Qua)
- Tap hand animation        | D  | 4 (4 frames)
Total: 12

## 24. MISSION PASS
- Mission pass frame        | 9S | 1
- Free reward slot          | T  | 1
- Premium reward slot       | T  | 1
- Claimed slot              | T  | 1
- Level progress bar        | T  | 2 (frame + fill)
- Buy premium button        | 2L | 2 (Buy Premium / Mua Premium)
- Premium lock icon         | T  | 1
Total: 8

## 25. LOGIN SCREEN
- Login background          | T  | 1 (1920x1080)
- Game logo                 | T  | 1
- Username input frame      | 9S | 1
- Password input frame      | 9S | 1
- Login button              | 2L | 2 (Login / Dang Nhap)
- Register button           | 2L | 2 (Register / Dang Ky)
- Google Sign-In button     | T  | 1
- Facebook Sign-In button   | T  | 1
- Apple Sign-In button      | T  | 1
- Notice frame              | 9S | 1
Total: 10

## 26. SERVER SELECT
- Server list frame         | 9S | 1
- Server card               | 9S | 1
- Badge New                 | 2L | 2 (New / Moi)
- Badge Hot                 | 2L | 2 (Hot / Hot)
- Badge Recommended         | 2L | 2 (Recommended / De Xuat)
- Status dot smooth (green) | T  | 1
- Status dot normal (yellow)| T  | 1
- Status dot busy (orange)  | T  | 1
- Status dot full (red)     | T  | 1
- Channel list frame        | 9S | 1
- Channel player bar        | T  | 2 (frame + fill)
Total: 12


## 27. LEADERBOARD / BXH (In-Game)
- Leaderboard panel frame   | 9S | 1
- Tab Level                 | 2L | 2 (Level / Cap Do)
- Tab PvP                   | T  | 1 (PvP)
- Tab Guild                 | 2L | 2 (Guild / Bang Hoi)
- Tab Wealth                | 2L | 2 (Wealth / Dai Gia)
- Entry row background      | 9S | 1
- Entry row highlight (top3)| T  | 1
- Rank #1 medal (gold)      | T  | 1
- Rank #2 medal (silver)    | T  | 1
- Rank #3 medal (bronze)    | T  | 1
- My rank frame             | 9S | 1
- Class icon small (7)      | T  | 7 (shared from #17)
- Scroll list frame         | 9S | 1
- Page prev button          | T  | 1
- Page next button          | T  | 1
Total: 15 files (+ 7 shared)

## ════════════════════════════════════
## SUMMARY
##
## Static (single PNG):        ~300
## Bilingual (VI+EN pairs):     ~60 (30 items x2)
## Animated (spritesheet):       7 sets (~36 frames)
## 9-slice (stretchable):      ~30
## ──────────────────────────────
## GRAND TOTAL:              ~415 PNG files

## 27. LEADERBOARD / BXH (In-Game)
- Leaderboard panel frame   | 9S | 1
- Tab Level                 | 2L | 2 (Level / Cap Do)
- Tab PvP                   | T  | 1 (PvP)
- Tab Guild                 | 2L | 2 (Guild / Bang Hoi)
- Tab Wealth                | 2L | 2 (Wealth / Dai Gia)
- Entry row background      | 9S | 1
- Entry row highlight (top3)| T  | 1
- Rank #1 medal (gold)      | T  | 1
- Rank #2 medal (silver)    | T  | 1
- Rank #3 medal (bronze)    | T  | 1
- My rank frame             | 9S | 1
- Class icon small (7)      | T  | 7 (shared from #17)
- Scroll list frame         | 9S | 1
- Page prev button          | T  | 1
- Page next button          | T  | 1
Total: 15 files (+ 7 shared)

## ════════════════════════════════════
