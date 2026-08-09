package cn.epicmc.client.network;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 简单的字节缓冲区，用于读写二进制数据
 * 格式: [1 byte: type] [2 bytes: length] [n bytes: data]
 */
public class PacketBuffer {
    private final ByteArrayOutputStream buffer;
    private final DataOutputStream output;
    private byte packetType;

    // 用于写入
    public PacketBuffer(byte packetType) {
        this.packetType = packetType;
        this.buffer = new ByteArrayOutputStream();
        this.output = new DataOutputStream(buffer);
    }

    // 写入方法
    public PacketBuffer writeByte(int value) throws IOException {
        output.writeByte(value);
        return this;
    }

    public PacketBuffer writeShort(int value) throws IOException {
        output.writeShort(value);
        return this;
    }

    public PacketBuffer writeInt(int value) throws IOException {
        output.writeInt(value);
        return this;
    }

    public PacketBuffer writeBoolean(boolean value) throws IOException {
        output.writeBoolean(value);
        return this;
    }

    public PacketBuffer writeString(String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeShort(bytes.length);
        output.write(bytes);
        return this;
    }

    public PacketBuffer writeBytes(byte[] bytes) throws IOException {
        output.write(bytes);
        return this;
    }

    // 构建最终的数据包
    public byte[] build() throws IOException {
        output.flush();
        byte[] payload = buffer.toByteArray();

        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(packet);

        out.writeByte(packetType);
        out.writeShort(payload.length);
        out.write(payload);
        out.flush();

        return packet.toByteArray();
    }

    // 用于读取
    public static class Reader {
        private final DataInputStream input;
        private final byte packetType;
        private final int length;

        public Reader(byte[] data) throws IOException {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);

            this.packetType = dis.readByte();
            this.length = dis.readUnsignedShort();
            this.input = dis;
        }

        public byte getPacketType() {
            return packetType;
        }

        public int getLength() {
            return length;
        }

        public byte readByte() throws IOException {
            return input.readByte();
        }

        public short readShort() throws IOException {
            return input.readShort();
        }

        public int readInt() throws IOException {
            return input.readInt();
        }

        public boolean readBoolean() throws IOException {
            return input.readBoolean();
        }

        public String readString() throws IOException {
            int len = input.readUnsignedShort();
            byte[] bytes = new byte[len];
            input.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        public byte[] readBytes(int length) throws IOException {
            byte[] bytes = new byte[length];
            input.readFully(bytes);
            return bytes;
        }
    }
}
