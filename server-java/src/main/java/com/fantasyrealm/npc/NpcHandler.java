package com.fantasyrealm.npc;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.Packet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NpcHandler {
    @Autowired private DialogService  dialog;
    @Autowired private NpcShopService shop;

    public void onInteract(PlayerSession s, Packet p) {
        long npcId     = p.readLong();
        int  templateId = p.readInt();
        dialog.startDialog(s, npcId, templateId);
    }

    public void onDialogChoice(PlayerSession s, Packet p) {
        long npcId     = p.readLong();
        int  templateId = p.readInt();
        int  choiceIdx  = p.readInt();
        dialog.selectChoice(s, npcId, templateId, choiceIdx);
    }
}
