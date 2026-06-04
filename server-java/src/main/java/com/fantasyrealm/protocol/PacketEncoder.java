package com.fantasyrealm.protocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PacketEncoder extends MessageToByteEncoder<Packet> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) {
        ByteBuf encoded = msg.encode();
        out.writeBytes(encoded);   // ByteBuf → ByteBuf, correct
        encoded.release();
        msg.release();
    }
}
