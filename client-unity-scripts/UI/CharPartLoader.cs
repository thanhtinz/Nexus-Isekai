using UnityEngine;
using System.Collections.Generic;

namespace FantasyRealm.UI
{
    /// <summary>
    /// Tiện ích load sprite layer cho character creation + paperdoll trong game.
    ///
    /// CÁCH ĐẶT ASSET (2 lựa chọn):
    ///
    /// A) Resources (đơn giản, cho prototype):
    ///    Đặt từng sprite frame idle-south tại:
    ///      Resources/CharParts/skin/{code}.png    (vd humn_v03.png)
    ///      Resources/CharParts/eyes/{code}.png
    ///      Resources/CharParts/hair/{code}.png
    ///      Resources/CharParts/outfit/{code}.png
    ///    CharacterCreationUI.SetLayer() sẽ Resources.Load các file này.
    ///
    /// B) Sprite sheet đã slice (cho game thật, full animation):
    ///    Import sheet 512x512 từ game-assets, slice grid 64x64 (8x8) trong
    ///    Unity Sprite Editor. Sprite con đặt tên {code}_{frameIndex}.
    ///    Dùng SpriteSheetCache bên dưới để lấy frame theo index khi animate.
    ///
    /// Frame index quy ước (page 1 - walk/run):
    ///    0  = idle south (nhìn xuống) — dùng cho preview tạo nhân vật
    ///    16 = idle east, 32 = idle north (tham khảo guide trong game-assets)
    /// </summary>
    public static class CharPartLoader
    {
        const string ROOT = "CharParts";
        static readonly Dictionary<string, Sprite> _cache = new();

        /// <summary>Lấy sprite preview (frame idle) cho 1 layer.</summary>
        public static Sprite Load(string slot, string code) {
            if (string.IsNullOrEmpty(code)) return null;
            string key = $"{slot}/{code}";
            if (_cache.TryGetValue(key, out var sp)) return sp;
            sp = Resources.Load<Sprite>($"{ROOT}/{key}");
            _cache[key] = sp; // cache cả null để khỏi load lại
            return sp;
        }

        /// <summary>Lấy 1 frame cụ thể từ sheet đã slice (đặt tên {code}_{frame}).</summary>
        public static Sprite LoadFrame(string slot, string code, int frame) {
            if (string.IsNullOrEmpty(code)) return null;
            string key = $"{slot}/{code}_{frame}";
            if (_cache.TryGetValue(key, out var sp)) return sp;
            sp = Resources.Load<Sprite>($"{ROOT}/{key}");
            _cache[key] = sp;
            return sp;
        }

        public static void ClearCache() => _cache.Clear();
    }
}
