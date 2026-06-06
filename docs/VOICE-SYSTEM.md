# 🎤 Voice Proximity — hạ tầng + hướng dẫn tích hợp

## Quan trọng đọc trước
Game server này **KHÔNG truyền âm thanh**. Truyền giọng real-time cần **media server
riêng** (băng thông, codec Opus, NAT traversal) — không nhồi vào game server TCP được.

Cái đã làm (verify được): **hạ tầng proximity** — server tính "ai nghe được ai" theo
khoảng cách + zone (giống proximity voice GTA V), gửi danh sách peer + âm lượng cho client.

Cái bạn cần làm thêm: nối một **voice SDK ngoài** để truyền giọng thật.

## Server đã có
`VoiceProximityService` — mỗi giây tính các peer trong tầm 200px cùng zone, gửi
`S_VOICE_PEERS` (playerId + volume 0-100 theo khoảng cách). C_VOICE_JOIN/LEAVE bật/tắt.

## Client đã có
`VoiceManager.cs` — nhận S_VOICE_PEERS, gọi hook OnPeerInRange/OutOfRange/SetPeerVolume.
**3 hook này là chỗ bạn cắm SDK voice.**

## Lựa chọn voice SDK (đề xuất theo độ dễ)
1. **Photon Voice 2** (Unity) — dễ nhất. Mỗi player 1 Recorder; dùng peer list từ
   server để set Speaker.Volume = volume/100. Có free tier.
2. **Vivox** (Unity) — của Unity Gaming Services, hỗ trợ positional voice sẵn.
3. **Agora** — chất lượng cao, tính phí theo phút.
4. **WebRTC tự dựng (mediasoup/LiveKit)** — miễn phí nhưng phải tự vận hành server, khó nhất.

## Luồng tích hợp (vd Photon Voice)
1. Player nhấn V → C_VOICE_JOIN → tham gia Photon Voice room (theo zone).
2. Server gửi S_VOICE_PEERS mỗi giây.
3. VoiceManager.OnPeerInRange(pid) → bật remote voice của pid.
4. SetPeerVolume(pid, vol) → speaker.Volume = vol/100 (xa nhỏ, gần to).
5. OnPeerOutOfRange(pid) → tắt remote voice.

## Lưu ý nền tảng
- **J2ME KHÔNG hỗ trợ voice** (điện thoại Java cổ thiếu API). Voice chỉ cho client Unity.
- Proximity logic ở server dùng chung — client nào hỗ trợ audio thì nghe được nhau.
