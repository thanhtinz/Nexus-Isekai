# Client Unity — Vọng Linh Giới

Unity **2022.3.21f1 LTS** (xem `ProjectSettings/ProjectVersion.txt`). Mở bằng Unity Hub → Add → chọn thư mục `client/`.

## Cấu trúc đã commit
- `Assets/` — code, sprite, audio, Spine, Resources... (nội dung game)
- `Packages/manifest.json` — khai báo package (TextMeshPro, ugui, modules). Unity tự resolve khi mở.
- `ProjectSettings/` — `ProjectVersion.txt`, `ProjectSettings.asset` (PlayerSettings), `TagManager.asset`,
  `EditorBuildSettings.asset`. Các file ProjectSettings còn lại Unity tự sinh mặc định khi mở lần đầu.

## Các thư mục KHÔNG commit (Unity tự sinh khi mở project)
Đây là lý do bạn không thấy chúng trong repo — chúng được tạo lại tự động trên máy mỗi người,
và đã được liệt kê trong `.gitignore`:

- `Library/` — cache import (artifact, shader, package). Sinh ra khi mở, có thể rất nặng (GB).
- `Temp/` — file tạm khi Editor chạy.
- `Obj/` — build trung gian.
- `UserSettings/` — cấu hình riêng từng máy (layout cửa sổ...).
- `Logs/`, `Build/`, `*.csproj`, `*.sln` — sinh ra bởi Editor/IDE.

KHÔNG commit các thư mục này. Nếu lỡ commit `Library/` sẽ làm repo phình lên rất lớn.

## Mở lần đầu
1. Cài Unity 2022.3.21f1 (hoặc bản 2022.3.x gần nhất — Unity sẽ hỏi upgrade, chọn OK).
2. Unity Hub → Add project from disk → chọn `client/`.
3. Lần mở đầu Unity sẽ tải package + tạo `Library/` (mất vài phút).
4. Chưa có scene (`Assets/Scenes/` trống) — tạo scene Boot/Login/MainGame và thêm vào
   File > Build Settings.

## Font
Xem `Assets/Fonts/README.md` — cần tự thêm font (OFL/đã mua) hỗ trợ tiếng Việt, repo không nhúng sẵn.
