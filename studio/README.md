# Nexus Studio — Game Data Tool (doc lap)

Tool bien tap data **tach rieng**, KHONG thuoc web admin. Build & deploy doc lap.
Ket noi toi server game qua API (cau hinh `VITE_API_BASE`), dung dung data he thong.

## Tinh nang
- Module: Skill / Mob-Boss / Effect / Map / Npc / Resource / KHO ASSET.
- Thu vien + editor thuoc tinh (schema-driven: tu render field theo data tra ve).
- AI-assist: tao mo ta, goi y chi so can bang, tao cau hinh VFX (JSON).
- Tach anh + LAM HOAT ANH: upload sprite sheet → tu nhan dien frame → ghep thanh
  hoat anh (dai frame doi thu tu/xoa) → preview Play/Pause/Delay/Zoom/Loop →
  xuat JSON (animation_json/frames_json/config_json) vao data.
- KHO ASSET: duyet + upload sprite/bg/asset (luu file tren server client_assets, co
  version/hash → game client tu tai ve khi thay doi).
- Save = AP VAO GAME: luu xong tu goi hot-reload (/api/reload/maps|monsters|npcs) →
  thay doi mob/map/npc co hieu luc ngay, khong can restart.

## Chay
```
cd studio
npm install
cp .env.example .env   # sua VITE_API_BASE tro toi server game (vd http://localhost:9090)
npm run dev            # http://localhost:5180
npm run build          # ra thu muc dist/ — deploy tinh (nginx/CDN) doc lap
```

## Yeu cau server
Server game can bat cac endpoint (da co trong AdminApiServer):
- Du lieu: /api/skills, /api/monsters, /api/sound-events, /api/maps, /api/npcs, /api/audio-assets
- AI: /api/studio/ai (can ANTHROPIC_API_KEY)
- Tach anh: /api/studio/slice
Key nhap o thanh "Ket noi" (luu localStorage), gui qua header X-Admin-Key.
