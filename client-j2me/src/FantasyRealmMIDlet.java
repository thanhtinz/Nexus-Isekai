import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

public class FantasyRealmMIDlet extends MIDlet implements CommandListener {
    private Display    display;
    private LoginScreen loginScreen;
    private GameCanvas  gameCanvas;

    public void startApp() {
        display = Display.getDisplay(this);
        if (loginScreen == null) loginScreen = new LoginScreen(this);
        display.setCurrent(loginScreen);
    }
    public void pauseApp()  {}
    public void destroyApp(boolean b) { GameConnection.getInstance().disconnect(); }

    public void onLoginSuccess(String charName, int faction, int level, long gold,
                                int zoneId, float x, float y, String token) {
        gameCanvas = new GameCanvas(this, charName, faction, level, gold, zoneId, x, y);
        display.setCurrent(gameCanvas);
        gameCanvas.start();
    }

    public void commandAction(Command c, Displayable d) {}
    public Display getDisplay() { return display; }
}
