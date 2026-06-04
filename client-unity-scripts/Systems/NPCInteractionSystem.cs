using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.Systems
{
    public class NPCInteractionSystem : MonoBehaviour
    {
        [Header("Dialog UI")]
        public GameObject dialogPanel;
        public Text       npcNameText;
        public Text       dialogText;
        public Transform  choicesContainer;
        public GameObject choiceButtonPrefab;

        private long _activeNpcId;
        private int  _activeTemplateId;

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_NPC_DIALOG,   OnDialog);
            PacketRouter.Instance.Register(PacketType.S_NPC_SHOP_DATA, OnShopData);
        }

        public void Interact(long npcId, int templateId) {
            _activeNpcId     = npcId;
            _activeTemplateId = templateId;
            GameNetworkManager.Instance?.Send(
                new Packet(PacketType.C_NPC_INTERACT).WriteLong(npcId).WriteInt(templateId));
        }

        void OnDialog(Packet p) {
            long npcId   = p.ReadLong();
            string text  = p.ReadString();
            int choices  = p.ReadInt();
            if (dialogPanel != null) dialogPanel.SetActive(true);
            if (dialogText  != null) dialogText.text = text;
            // Clear old choices
            foreach (Transform child in choicesContainer) Destroy(child.gameObject);
            // Build choice buttons
            for (int i = 0; i < choices; i++) {
                string cText    = p.ReadString();
                string nextNode = p.ReadString();
                string action   = p.ReadString();
                int idx = i;
                if (choiceButtonPrefab == null) continue;
                var go  = Instantiate(choiceButtonPrefab, choicesContainer);
                var txt = go.GetComponentInChildren<Text>();
                if (txt != null) txt.text = cText;
                go.GetComponent<Button>()?.onClick.AddListener(() => SelectChoice(idx));
            }
        }

        void OnShopData(Packet p) {
            // Route to market UI or a separate shop panel
            Debug.Log("[NPC] Shop data received");
        }

        public void SelectChoice(int idx) {
            GameNetworkManager.Instance?.Send(
                new Packet(PacketType.C_NPC_DIALOG_CHOICE)
                    .WriteLong(_activeNpcId).WriteInt(_activeTemplateId).WriteInt(idx));
        }

        public void Close() { if (dialogPanel != null) dialogPanel.SetActive(false); }
    }
}
