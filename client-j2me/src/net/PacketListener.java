public interface PacketListener {
    void onPacket(int type, byte[] payload);
}
