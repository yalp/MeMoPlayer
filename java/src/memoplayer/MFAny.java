package memoplayer;

import java.io.DataInputStream;

public class MFAny extends Field {
    
    int m_size;
    Register[] m_value;
    
    public MFAny() {
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
        if (f instanceof MFAny) {
            set (((MFAny)f));
        }
    }
    
    void set (MFAny f) {
        resize (f.m_size);
        for (int i=0; i<m_size; i++) {
            m_value[i].set (f.m_value[i]);
        }
        notifyChange();
    }
    
    
    public void set(int index, Register r, int offset) {
        if (index == Field.LENGTH_IDX) { // support pre allocation : myStr.length = 10;
            resize(r.getInt());
        }
        if (offset >= 0) {
            if (offset >= m_size) {
                resize(offset+1);
            }
            m_value [offset].set (r);
            notifyChange ();
        }
    }
    
    public void get(int index, Register r, int offset) {
        if (index == Field.LENGTH_IDX) {
            r.setInt (m_size);
        } else if (index == Field.OBJECT_IDX) {
            r.setField (this);
        } else if (offset >= 0 && offset < m_size) {
            r.set (m_value [offset]);
        } else { // out of bounds, return default value
            r.setBool (false);
        }
    }

    
    /**
     * MCP: Resize array but keep allocated slots when downsizing
     * @param size The new size
     */
    void resize (int size) {
        if (size > m_size) { // upsize
            if (m_value == null) { 
                m_value = new Register[size];
            } else if (size > m_value.length) { // upsize (must reallocate)
                Register [] tmp = new Register [size];
                System.arraycopy (m_value, 0, tmp, 0, m_size);
                m_value = tmp;
            }
            for (int i = m_size; i < size; i++) {
                m_value[i] = new Register();
            }
        } else if (size >= 0 && size < m_size) { // downsize (but keep allocated array)
            for (int i = size; i < m_size; i++) {
                m_value[i] = null;
            }
        }
        m_size = size;
    }
}
