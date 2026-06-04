import javax.microedition.lcdui.*;
import java.io.*;

public class MarketScreen extends List implements CommandListener {
    private final FantasyRealmMIDlet midlet;
    private Command buyCmd, sellCmd, backCmd;

    public MarketScreen(FantasyRealmMIDlet m) {
        super("Chợ", List.IMPLICIT);
        midlet = m;
        buyCmd  = new Command("Xem chợ", Command.SCREEN, 1);
        sellCmd = new Command("Bán hàng", Command.SCREEN, 2);
        backCmd = new Command("Quay lại", Command.BACK,   3);
        addCommand(buyCmd); addCommand(sellCmd); addCommand(backCmd);
        setCommandListener(this);
        append("[Danh sách chợ]", null);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) midlet.getDisplay().setCurrent(null);
    }
}
