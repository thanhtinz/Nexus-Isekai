import javax.microedition.lcdui.*;
import java.io.*;

public class SellItemScreen extends Form implements CommandListener {
    private final FantasyRealmMIDlet midlet;
    private TextField itemIdField, qtyField, priceField;
    private Command sellCmd, backCmd;

    public SellItemScreen(FantasyRealmMIDlet m) {
        super("Đăng Bán");
        midlet = m;
        itemIdField = new TextField("Item ID", "", 10, TextField.NUMERIC);
        qtyField    = new TextField("Số lượng", "1", 5, TextField.NUMERIC);
        priceField  = new TextField("Giá / cái", "100", 10, TextField.NUMERIC);
        sellCmd = new Command("Đăng bán", Command.OK,   1);
        backCmd = new Command("Quay lại", Command.BACK, 2);
        append(itemIdField); append(qtyField); append(priceField);
        addCommand(sellCmd); addCommand(backCmd);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == sellCmd) {
            try {
                long iid   = Long.parseLong(itemIdField.getString());
                int  qty   = Integer.parseInt(qtyField.getString());
                long price = Long.parseLong(priceField.getString());
                PacketBuilder pb = new PacketBuilder();
                pb.writeShort(0x54); pb.writeLong(iid); pb.writeInt(qty); pb.writeLong(price);
                GameConnection.getInstance().send(pb.toBytes());
                Alert a = new Alert("OK","Đã đăng bán!",null,AlertType.CONFIRMATION);
                a.setTimeout(2000); midlet.getDisplay().setCurrent(a);
            } catch (IOException | NumberFormatException e) {
                Alert a = new Alert("Lỗi",e.getMessage(),null,AlertType.ERROR);
                midlet.getDisplay().setCurrent(a);
            }
        }
        if (c == backCmd) midlet.getDisplay().setCurrent(null);
    }
}
