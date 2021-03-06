/*
 * Copyright (C) 2010 France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package memoplayer;
import java.io.*;

class SFTime extends Field {
    int m_f;

    SFTime (int f, Observer o) {
        super (o);
        m_f = FixFloat.fix2time(f);
    }
    SFTime (int f) {
        this (f, null);
    }
    
    void decode (DataInputStream dis, Node [] table, Decoder decoder) {
        m_f = Decoder.readInt (dis);
        //System.out.println ("SFTime.decode: "+ FixFloat.toString(m_f));
    }
    
//     int getValue2 () { return FixFloat.time2fix(m_f); }
    
//     void setValue2 (int f) { 
//         m_f = FixFloat.fix2time(f); notifyChange (); 
//     }

    int getValue () { return m_f; }
    
    void setValue (int f) { 
        m_f = f; notifyChange (); 
    }
    
    void copyValue (Field f) {
        setValue (((SFTime)f).getValue ());
        //System.out.println ("SFTime.copyValue: "+m_f);
    }

    public void set (int index, Register r, int offset) {
        m_f = FixFloat.fix2time(r.getFloat ());
        notifyChange ();
    }
    
    public void get (int index, Register r, int offset) {
        r.setFloat (FixFloat.time2fix (m_f)); 
    }

}
