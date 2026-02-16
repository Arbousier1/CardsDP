package com.dqc.cardsdp.item;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemStack;

final class ItemListCodec {
    private ItemListCodec() {
    }

    static byte[] encode(List<ItemStack> items) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(items.size());
            for (ItemStack item : items) {
                byte[] raw = item == null ? new byte[0] : item.serializeAsBytes();
                out.writeInt(raw.length);
                out.write(raw);
            }
            out.flush();
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to encode item list", ex);
        }
    }

    static List<ItemStack> decode(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return new ArrayList<>();
        }
        try (DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(raw))) {
            int size = in.readInt();
            List<ItemStack> items = new ArrayList<>(Math.max(size, 0));
            for (int i = 0; i < size; i++) {
                int length = in.readInt();
                if (length <= 0) {
                    continue;
                }
                byte[] itemRaw = in.readNBytes(length);
                if (itemRaw.length != length) {
                    throw new IOException("Unexpected end of item payload");
                }
                items.add(ItemStack.deserializeBytes(itemRaw));
            }
            return items;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to decode item list", ex);
        }
    }
}
