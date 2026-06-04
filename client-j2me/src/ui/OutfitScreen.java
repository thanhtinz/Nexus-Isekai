import javax.microedition.lcdui.*;

public class OutfitScreen extends Form implements CommandListener {
    private final FantasyRealmMIDlet midlet;
    private final String[] hairNames = {"Tóc Ngắn","Tóc Dài","Tóc Xoăn","Ponytail","Tóc Samurai"};
    private final String[] topNames  = {"Áo Thường","Áo Phép","Áo Giáp","Áo Lễ","Áo Ninja"};
    private ChoiceGroup hairChoice, topChoice, colorChoice;
    private Command applyCmd, backCmd;

    public OutfitScreen(FantasyRealmMIDlet m) {
        super("Trang Phục");
        midlet = m;
        hairChoice  = new ChoiceGroup("Kiểu tóc", ChoiceGroup.EXCLUSIVE, hairNames, null);
        topChoice   = new ChoiceGroup("Áo", ChoiceGroup.EXCLUSIVE, topNames, null);
        colorChoice = new ChoiceGroup("Màu", ChoiceGroup.EXCLUSIVE,
            new String[]{"Đỏ","Xanh Lam","Xanh Lá","Vàng","Tím","Trắng"}, null);
        applyCmd = new Command("Áp dụng", Command.OK,   1);
        backCmd  = new Command("Quay lại",Command.BACK, 2);
        append(hairChoice); append(topChoice); append(colorChoice);
        addCommand(applyCmd); addCommand(backCmd);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == applyCmd) {
            int hair = hairChoice.getSelectedIndex();
            int top  = topChoice.getSelectedIndex();
            int col  = colorChoice.getSelectedIndex();
            String json = "{\"hairId\":" + hair + ",\"topId\":" + top + ",\"hairColor\":" + col + "}";
            try {
                PacketBuilder pb = new PacketBuilder();
                pb.writeShort(0x34); pb.writeString(json);
                GameConnection.getInstance().send(pb.toBytes());
            } catch (java.io.IOException e) {}
        }
        if (c == backCmd) midlet.getDisplay().setCurrent(null);
    }
}
