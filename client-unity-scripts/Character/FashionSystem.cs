using System.Collections.Generic;
using UnityEngine;
using FantasyRealm.Network;

namespace FantasyRealm.Character
{
    [System.Serializable]
    public class OutfitData
    {
        public int hairId, hairColor;
        public int topId, topColor;
        public int bottomId, bottomColor;
        public int shoesId;
        public int accessoryId;
        public int wingId;
        public int petTemplateId;
    }

    public class FashionSystem : MonoBehaviour
    {
        [Header("Sprite Renderers by slot")]
        public SpriteRenderer hairRenderer;
        public SpriteRenderer topRenderer;
        public SpriteRenderer bottomRenderer;
        public SpriteRenderer shoesRenderer;
        public SpriteRenderer accessoryRenderer;
        public SpriteRenderer wingRenderer;

        [Header("Sprite Atlas")]
        public Sprite[] hairSprites;
        public Sprite[] topSprites;
        public Sprite[] bottomSprites;

        private OutfitData _currentOutfit = new OutfitData();

        public void ApplyOutfitJson(string json) {
            try {
                var outfit = UnityEngine.JsonUtility.FromJson<OutfitData>(json);
                if (outfit != null) ApplyOutfit(outfit);
            } catch { }
        }

        public void ApplyOutfit(OutfitData o) {
            _currentOutfit = o;
            if (hairRenderer   != null && o.hairId   < hairSprites?.Length)   hairRenderer.sprite   = hairSprites[o.hairId];
            if (topRenderer    != null && o.topId    < topSprites?.Length)    topRenderer.sprite    = topSprites[o.topId];
            if (bottomRenderer != null && o.bottomId < bottomSprites?.Length) bottomRenderer.sprite = bottomSprites[o.bottomId];
            if (hairRenderer   != null) hairRenderer.color   = ColorFromId(o.hairColor);
            if (topRenderer    != null) topRenderer.color    = ColorFromId(o.topColor);
            if (bottomRenderer != null) bottomRenderer.color = ColorFromId(o.bottomColor);
        }

        public string SerializeOutfit() => UnityEngine.JsonUtility.ToJson(_currentOutfit);

        public void SendOutfitToServer() {
            GameNetworkManager.Instance?.Send(
                new Packet(PacketType.C_CHANGE_OUTFIT).WriteString(SerializeOutfit()));
        }

        Color ColorFromId(int id) {
            float[] palette = { 1f,0f,0f, 0f,1f,0f, 0f,0f,1f, 1f,1f,0f, 1f,0.5f,0f, 0.5f,0f,1f, 1f,1f,1f, 0f,0f,0f };
            int i = (id * 3) % palette.Length;
            return new Color(palette[i], palette[i+1], palette[i+2]);
        }
    }
}
