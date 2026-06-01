using UnityEngine;
using UnityEngine.Tilemaps;

namespace NexusIsekai.World
{
    /// <summary>
    /// Doc maps.layout_json (do MapEditorPro xuat) va dung Tilemap ve lop Tile + Water + (debug) Collision.
    ///   { w, h, tileset:[key], tiles:[idx], waterset:[key], water:[idx], collision:[0/1] }
    /// Anh tung tile do caller cung cap (resolve key -> Texture2D, lay tu KHO/Resources).
    /// Quy uoc: 1 o = 1 cell Unity; sprite tao voi pixelsPerUnit = TILE (24) de 1 tile = 1 unit.
    /// Row 0 o tren cung -> dat tai y = -row (Unity y huong len).
    /// </summary>
    public static class MapLayout
    {
        [System.Serializable]
        public class Data
        {
            public int w, h;
            public string[] tileset; public int[] tiles;
            public string[] waterset; public int[] water;
            public int[] collision;
        }

        public static Data Parse(string json)
        {
            if (string.IsNullOrEmpty(json)) return null;
            try { var d = JsonUtility.FromJson<Data>(json); return (d != null && d.w > 0 && d.h > 0) ? d : null; }
            catch { return null; }
        }
    }

    [RequireComponent(typeof(Grid))]
    public class MapTileRenderer : MonoBehaviour
    {
        public float tileSize = 24f;        // px/o — khop server MapData.TILE + MapEditorPro CELL
        public bool showCollision = false;  // ve debug o chan (do mo)
        public Color collisionColor = new Color(1f, 0.23f, 0.23f, 0.35f);

        private Tilemap tileMap, waterMap, collMap;
        private MapLayout.Data data;

        /// <summary>Dung map tu layout JSON. resolve(key) tra Texture2D cua tile (caller load tu KHO).</summary>
        public void Build(string layoutJson, System.Func<string, Texture2D> resolve)
        {
            Build(MapLayout.Parse(layoutJson), resolve);
        }

        public void Build(MapLayout.Data d, System.Func<string, Texture2D> resolve)
        {
            Clear();
            data = d;
            if (d == null) return;

            EnsureMaps();
            var tileCache = new System.Collections.Generic.Dictionary<string, Tile>();

            PlaceLayer(tileMap, d.tiles, d.tileset, resolve, tileCache, 0);
            PlaceLayer(waterMap, d.water, d.waterset, resolve, tileCache, 1);

            if (showCollision && d.collision != null)
            {
                var redTile = SolidTile(collisionColor);
                for (int k = 0; k < d.collision.Length; k++)
                    if (d.collision[k] == 1)
                        collMap.SetTile(new Vector3Int(k % d.w, -(k / d.w), 0), redTile);
            }
        }

        private void PlaceLayer(Tilemap map, int[] grid, string[] set, System.Func<string, Texture2D> resolve,
                                System.Collections.Generic.Dictionary<string, Tile> cache, int z)
        {
            if (grid == null || set == null) return;
            for (int k = 0; k < grid.Length; k++)
            {
                int ix = grid[k];
                if (ix < 0 || ix >= set.Length) continue;
                string key = set[ix];
                Tile t;
                if (!cache.TryGetValue(key, out t))
                {
                    var tex = resolve != null ? resolve(key) : null;
                    t = (tex != null) ? TileFromTexture(tex) : null;
                    cache[key] = t;
                }
                if (t != null) map.SetTile(new Vector3Int(k % data.w, -(k / data.w), z), t);
            }
        }

        private Tile TileFromTexture(Texture2D tex)
        {
            tex.filterMode = FilterMode.Point;
            var sp = Sprite.Create(tex, new Rect(0, 0, tex.width, tex.height), new Vector2(0.5f, 0.5f), Mathf.Max(1f, tileSize));
            var t = ScriptableObject.CreateInstance<Tile>();
            t.sprite = sp;
            return t;
        }

        private Tile SolidTile(Color c)
        {
            var tex = new Texture2D(1, 1); tex.SetPixel(0, 0, c); tex.Apply();
            return TileFromTexture(tex);
        }

        private Tilemap MakeMap(string name, int order)
        {
            var go = new GameObject(name);
            go.transform.SetParent(transform, false);
            var tm = go.AddComponent<Tilemap>();
            var r = go.AddComponent<TilemapRenderer>();
            r.sortingOrder = order;
            return tm;
        }

        private void EnsureMaps()
        {
            if (tileMap == null) tileMap = MakeMap("TileLayer", 0);
            if (waterMap == null) waterMap = MakeMap("WaterLayer", 1);
            if (collMap == null) collMap = MakeMap("CollisionDebug", 50);
        }

        public bool IsBlocked(float worldXPx, float worldYPx)
        {
            if (data == null || data.collision == null || data.w <= 0) return false;
            int c = (int)(worldXPx / tileSize), r = (int)(worldYPx / tileSize);
            if (c < 0 || r < 0 || c >= data.w || r >= data.h) return false;
            int idx = r * data.w + c;
            return idx >= 0 && idx < data.collision.Length && data.collision[idx] == 1;
        }

        public void Clear()
        {
            if (tileMap != null) tileMap.ClearAllTiles();
            if (waterMap != null) waterMap.ClearAllTiles();
            if (collMap != null) collMap.ClearAllTiles();
        }
    }
}
