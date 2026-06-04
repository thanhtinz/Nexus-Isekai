import javax.microedition.lcdui.*;
import java.io.*;

public class ChatScreen extends Form implements CommandListener {
    private final FantasyRealmMIDlet midlet;
    private final GameCanvas canvas;
    private TextField msgField;
    private ChoiceGroup channelChoice;
    private Command sendCmd, backCmd;

    public ChatScreen(FantasyRealmMIDlet m, GameCanvas c) {
        super("Chat");
        midlet = m; canvas = c;
        channelChoice = new ChoiceGroup("Kênh", ChoiceGroup.EXCLUSIVE,
            new String[]{"Khu vực","Phe","Buôn bán","Tất cả"}, null);
        msgField = new TextField("Tin nhắn", "", 200, TextField.ANY);
        sendCmd = new Command("Gửi", Command.OK, 1);
        backCmd = new Command("Quay lại", Command.BACK, 2);
        append(channelChoice); append(msgField);
        addCommand(sendCmd); addCommand(backCmd);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) { canvas.onChatReturn(); return; }
        if (c == sendCmd) {
            String msg = msgField.getString().trim();
            if (msg.length() == 0) return;
            int ch = channelChoice.getSelectedIndex();
            try {
                PacketBuilder pb = new PacketBuilder();
                pb.writeShort(0x20); pb.writeString(msg); pb.writeByte(ch);
                GameConnection.getInstance().send(pb.toBytes());
            } catch (IOException e) {}
            msgField.setString(""); canvas.onChatReturn();
        }
    }
}
