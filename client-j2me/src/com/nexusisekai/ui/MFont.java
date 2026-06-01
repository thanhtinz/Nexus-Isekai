package com.nexusisekai.ui;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Hashtable;

/**
 * MFont — bitmap font goc cho client J2ME (MIDP 2.0 / CLDC 1.1).
 *
 * Tu viet (KHONG sao chep tu game thuong mai). Doc bo font do tools/font/gen_bitmap_font.py
 * sinh ra tu font HOP PHAP (OFL/CC0): 1 file .dat metrics + nhieu sheet PNG theo mau.
 *
 * Format .dat (big-endian): 'BMF1' | height(1) | spacing(1) | count(2) | widths(count) |
 *                           charsetLen(2) | charset(UTF-8).
 * Sheet: dai ngang, glyph i o [offX[i] .. offX[i]+width[i]), cao = height.
 *
 * Dung:
 *   MFont.main().drawString(g, "Vong Linh Gioi", x, y, MFont.YELLOW);
 *   int w = MFont.main().stringWidth(s);
 */
public final class MFont {

    public static final int WHITE = 0, YELLOW = 1, RED = 2, GREEN = 3, BLUE = 4, GREY = 5, ORANGE = 6;
    private static final String[] COLOR_NAMES = { "white", "yellow", "red", "green", "blue", "grey", "orange" };

    private final String fontName;
    private final int height, spacing;
    private final String charset;
    private final int[] width;
    private final int[] offX;
    private final Image[] sheets = new Image[COLOR_NAMES.length];
    private final Hashtable index = new Hashtable();

    private static MFont MAIN, SMALL;

    /** Font chinh (size mac dinh). */
    public static MFont main()  { if (MAIN  == null) MAIN  = load("main");  return MAIN;  }
    /** Font nho (HUD, chu thich). */
    public static MFont small() { if (SMALL == null) SMALL = load("small"); return SMALL; }

    private static MFont load(String name) {
        try { return new MFont(name); } catch (Exception e) { return null; }
    }

    private MFont(String name) throws Exception {
        this.fontName = name;
        InputStream is = getClass().getResourceAsStream("/font/" + name + ".dat");
        DataInputStream dis = new DataInputStream(is);
        byte[] magic = new byte[4];
        dis.readFully(magic); // 'BMF1'
        this.height = dis.readUnsignedByte();
        this.spacing = dis.readUnsignedByte();
        int n = dis.readUnsignedShort();
        this.width = new int[n];
        this.offX = new int[n];
        int x = 0;
        for (int i = 0; i < n; i++) { width[i] = dis.readUnsignedByte(); offX[i] = x; x += width[i]; }
        int clen = dis.readUnsignedShort();
        byte[] cb = new byte[clen];
        dis.readFully(cb);
        this.charset = new String(cb, "UTF-8");
        dis.close();
        for (int i = 0; i < charset.length(); i++)
            index.put(new Character(charset.charAt(i)), new Integer(i));
        sheets[WHITE] = Image.createImage("/font/" + fontName + "_white.png");
    }

    private Image sheet(int color) {
        if (color < 0 || color >= sheets.length) color = WHITE;
        if (sheets[color] == null) {
            try { sheets[color] = Image.createImage("/font/" + fontName + "_" + COLOR_NAMES[color] + ".png"); }
            catch (Exception e) { sheets[color] = sheets[WHITE]; }
        }
        return sheets[color];
    }

    private int idxOf(char c) {
        Object o = index.get(new Character(c));
        if (o != null) return ((Integer) o).intValue();
        o = index.get(new Character('?'));
        return o != null ? ((Integer) o).intValue() : -1;
    }

    public int getHeight() { return height; }

    public int stringWidth(String s) {
        if (s == null) return 0;
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            int idx = idxOf(s.charAt(i));
            if (idx >= 0) w += width[idx] + spacing;
        }
        return w > 0 ? w - spacing : 0;
    }

    /** Ve chuoi, goc tren-trai tai (x,y). */
    public void drawString(Graphics g, String s, int x, int y, int color) {
        if (s == null) return;
        Image sh = sheet(color);
        for (int i = 0; i < s.length(); i++) {
            int idx = idxOf(s.charAt(i));
            if (idx < 0) continue;
            // transform 0 = TRANS_NONE (tranh phu thuoc lcdui.game.Sprite)
            g.drawRegion(sh, offX[idx], 0, width[idx], height, 0, x, y, Graphics.TOP | Graphics.LEFT);
            x += width[idx] + spacing;
        }
    }

    /** Ve voi anchor (HCENTER/RIGHT, VCENTER/BOTTOM). */
    public void drawString(Graphics g, String s, int x, int y, int color, int anchor) {
        if ((anchor & Graphics.HCENTER) != 0) x -= stringWidth(s) / 2;
        else if ((anchor & Graphics.RIGHT) != 0) x -= stringWidth(s);
        if ((anchor & Graphics.VCENTER) != 0) y -= height / 2;
        else if ((anchor & Graphics.BOTTOM) != 0) y -= height;
        drawString(g, s, x, y, color);
    }
}
