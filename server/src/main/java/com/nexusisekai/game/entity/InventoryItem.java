package com.nexusisekai.game.entity;

/**
 * 1 slot trong inventory nhân vật.
 */
public class InventoryItem {
    private long id;        // ID record trong DB
    private int itemId;
    private String name;
    private int type;
    private int quantity;
    private int slot;       // -1=bag, 0-9=trang bị
    private int enchant;
    private int durability;

    // Stats bonus khi trang bị
    private int atkBonus;
    private int defBonus;
    private int strBonus;
    private int agiBonus;
    private int intBonus;
    private int vitBonus;
    private int hpBonus;
    private int mpBonus;
    private int iconId;

    public InventoryItem() {}

    public byte[] toBytes() {
        byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(
                8 + 4 + 2 + nameBytes.length + 4 + 4 + 4 + 1 + 4 + 4);
        buf.putLong(id);
        buf.putInt(itemId);
        buf.putShort((short) nameBytes.length);
        buf.put(nameBytes);
        buf.putInt(type);
        buf.putInt(quantity);
        buf.putInt(slot);
        buf.put((byte) enchant);
        buf.putInt(durability);
        buf.putInt(iconId);
        return buf.array();
    }

    // Getters/Setters
    public long getId()           { return id; }
    public void setId(long id)    { this.id = id; }
    public int getItemId()        { return itemId; }
    public void setItemId(int i)  { this.itemId = i; }
    public String getName()       { return name; }
    public void setName(String n) { this.name = n; }
    public int getType()          { return type; }
    public void setType(int t)    { this.type = t; }
    public int getQuantity()      { return quantity; }
    public void setQuantity(int q){ this.quantity = q; }
    public int getSlot()          { return slot; }
    public void setSlot(int s)    { this.slot = s; }
    public int getEnchant()       { return enchant; }
    public int getDurability()    { return durability; }
    public int getAtkBonus()      { return atkBonus; }
    public void setAtkBonus(int v){ this.atkBonus = v; }
    public int getDefBonus()      { return defBonus; }
    public void setDefBonus(int v){ this.defBonus = v; }
    public int getIconId()        { return iconId; }
    public void setIconId(int v)  { this.iconId = v; }
}
