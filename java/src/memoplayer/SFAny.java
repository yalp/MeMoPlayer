package memoplayer;

import java.io.DataInputStream;

public class SFAny extends Field {
    
    Register m_value = new Register();
    
    public SFAny() {
    }

    void decode(DataInputStream dis, Node[] table, Decoder decoder) {
        // Read size, then skip given size (for future compatibility)
        int size = Decoder.readUnsignedByte (dis);
        if (size == 255) {
            size = Decoder.readUnsignedByte (dis) * 255 + Decoder.readUnsignedByte (dis) ;
        }
        if (size != 0) {
            try {
                dis.skip(size);
            } catch (Exception e) { }
        }
    }
    
    void copyValue(Field f) {
        if (f instanceof SFAny) {
            m_value.set (((SFAny)f).m_value);
            notifyChange ();
        }
    }
    
    public void set(int index, Register r, int offset) {
        m_value.set (r);
        notifyChange ();
    }
    
    public void get(int index, Register r, int offset) {
        r.set (m_value);
    }
}
