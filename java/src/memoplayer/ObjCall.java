/*
* This software has been developed by France Telecom, FT/BD/DIH/HDM
*
* Copyright France Telecom 2010
*
* COPYRIGHT:
*    This file is the property of FRANCE TELECOM. It cannot be copied,  
*    used, or modified without obtaining an authorization from the
*    authors or a mandated member of FRANCE TELECOM.
*    If such an authorization is provided, any modified version or copy
*    of the software has to contain this header.
*
* WARRANTIES:
*    This software is made available by the authors in the hope that it
*    will be useful, but without any warranty. France Telecom is not
*    liable for any consequence related to the use of the provided
*    software.
*
* AUTHORS: Renaud Cazoulat, Marc Capdevielle
*
* VERSION: 1.00
*
* DATE: 16/05/2010
*/

package memoplayer;

import java.util.Enumeration;

class ObjCall {
//#ifdef debug.console
    final static String[] methodNames = {"length", "charAt", "substring", "startsWith", "endsWith", "indexOf", "lastIndexOf", "toLower", "toUpper", "strIndexOf", "toInt", "toFloat", "trim", "decodeUrl", "encodeUrl", "replace", "hasMoreElements", "nextElement", "list", "close", "newContact", "deleteContact", "save", "getFirstName", "getLastName", "getFormattedName", "getAddress", "getField", "getNbFields", "getMaxFields", "setName", "setAddress", "setField", "deleteField", "getFieldAttr"};
//#endif
    final static int length = 0;
    final static int charAt = 1;
    final static int substring = 2;
    final static int startsWith = 3;
    final static int endsWith = 4;
    final static int indexOf = 5;
    final static int lastIndexOf = 6;
    final static int toLower = 7;
    final static int toUpper = 8;
    final static int strIndexOf = 9;
    final static int toInt = 10;
    final static int toFloat = 11;
    final static int trim = 12;
    final static int decodeUrl = 13;
    final static int encodeUrl = 14;
    final static int replace = 15;
    final static int hasMoreElements = 16;
    final static int nextElement = 17;
    final static int list = 18;
    final static int close = 19;
    final static int newContact = 20;
    final static int deleteContact = 21;
    final static int save = 22;
    final static int getFirstName = 23;
    final static int getLastName = 24;
    final static int getFormattedName = 25;
    final static int getAddress = 26;
    final static int getField = 27;
    final static int getNbFields = 28;
    final static int getMaxFields = 29;
    final static int setName = 30;
    final static int setAddress = 31;
    final static int setField = 32;
    final static int deleteField = 33;
    final static int getFieldAttr = 34;
    
    static void methodNotFound (String objClass, int method) {
//#ifdef debug.console
        if (method>=0 && method<methodNames.length) {
            Logger.println ("Error: Unsupported method "+methodNames[method]+" on object of type "+objClass);
        } else {
            Logger.println ("Error: Unsupported method "+method+" on object of type "+objClass+": Is the player older than the compiler ?");
        }
//#endif
    }
    
    static void doCall (Machine mc, Context c, int m, Register [] registers, int r, int nbParams) {
        switch (registers[r].getType()) {
        case Register.TYPE_INT: doInt (c, m, registers, r, nbParams); break;
        case Register.TYPE_FLOAT: doFloat (c, m, registers, r, nbParams); break;
        case Register.TYPE_STRING: doString (c, m, registers, r, nbParams); break;
//#ifdef api.pim2
        case Register.TYPE_CONTACT_LIST: doContactList (c, m, registers, r, nbParams); break;
        case Register.TYPE_CONTACT_LIST_ENUMERATION: doEnumeration (c, m, registers, r, nbParams, Register.TYPE_CONTACT); break;
        case Register.TYPE_CONTACT: doContact (c, m, registers, r, nbParams); break;
//#endif
        default: Logger.println ("Unsupported object type: "+registers[r].getType());
        }
    }

    static void doInt (Context c, int m, Register [] registers, int r, int nbParams) {
        methodNotFound ("int", m);
    }

    static void doFloat (Context c, int m, Register [] registers, int r, int nbParams) {
        methodNotFound ("float", m);
    }

    static void doString (Context c, int m, Register [] registers, int r, int nbParams) {
        switch (m) {
        case ObjCall.length: ExternCall.doString (c, 0, registers, r, nbParams); break;
        case ObjCall.charAt: ExternCall.doString (c, 1, registers, r, nbParams); break;
        case ObjCall.substring: ExternCall.doString (c, 2, registers, r, nbParams); break;
        case ObjCall.startsWith: ExternCall.doString (c, 3, registers, r, nbParams); break;
        case ObjCall.endsWith: ExternCall.doString (c, 4, registers, r, nbParams); break;
        case ObjCall.indexOf: ExternCall.doString (c, 5, registers, r, nbParams); break;
        case ObjCall.lastIndexOf: ExternCall.doString (c, 6, registers, r, nbParams); break;
        case ObjCall.toLower: ExternCall.doString (c, 7, registers, r, nbParams); break;
        case ObjCall.toUpper: ExternCall.doString (c, 8, registers, r, nbParams); break;
        case ObjCall.strIndexOf: ExternCall.doString (c, 9, registers, r, nbParams); break;
        case ObjCall.toInt: ExternCall.doString (c, 10, registers, r, nbParams); break;
        case ObjCall.toFloat: ExternCall.doString (c, 11, registers, r, nbParams); break;
        case ObjCall.trim: ExternCall.doString (c, 12, registers, r, nbParams); break;
        case ObjCall.decodeUrl: ExternCall.doString (c, 17, registers, r, nbParams); break;
        case ObjCall.encodeUrl: ExternCall.doString (c, 18, registers, r, nbParams); break;
        case ObjCall.replace: ExternCall.doString (c, 23, registers, r, nbParams); break;
        default: methodNotFound ("String", m); break;
        }
    }
    
    static void doEnumeration (Context c, int m, Register [] registers, int r, int nbParams, int type) {
        Enumeration e = (Enumeration)registers[r].getObject ();
        switch (m) {
        case ObjCall.hasMoreElements:
            registers[r].setBool (e != null && e.hasMoreElements()); 
            break;
        case ObjCall.nextElement:
            try {
                registers[r].setObject (type, e != null ? e.nextElement() : null);
            } catch (Exception ex) {
                registers[r].setBool (false);
            }
            break;
        default: 
            methodNotFound ("Enumeration", m); 
            break;
        }
    }

//#ifdef api.pim2
    static void doContactList (Context c, int m, Register [] registers, int r, int nbParams) {
        Object o = registers[r].getObject (Register.TYPE_CONTACT_LIST);
        Object param = null;
        switch (m) {
        case ObjCall.close: // close (ContactList l)
            JSContact2.closeContactList (o);
            return;
        case ObjCall.list: // ContactListEnumeration list (ContactList l, [Contact c | String match])
            if (nbParams>1) {
                // Second argument is either a Contact or a String
                param = registers[r+1].getObject(Register.TYPE_CONTACT_LIST);
                if (param == null) {
                    param = registers[r+1].getString();
                }
            }
            registers[r].setObject (Register.TYPE_CONTACT_LIST_ENUMERATION, JSContact2.listContacts (o, param));
            return;
        case ObjCall.newContact: // Contact newContact (ContactList l)
            registers[r].setObject (Register.TYPE_CONTACT, JSContact2.newContact (o));
            return;
        case ObjCall.deleteContact: // bool deleteContact (ContactList l, Contact c)
            registers[r].setBool (JSContact2.deleteContact (o, registers[r+1].getObject(Register.TYPE_CONTACT)));
            return;
        case ObjCall.getMaxFields: // int getMaxFields (ContactList l, int fieldType)
            registers[r].setInt (JSContact2.getMaxFields (o, registers[r+1].getInt()));
            return;
        default: methodNotFound ("ContactList", m); break;
        }
    }
    
    static void doContact (Context c, int m, Register [] registers, int r, int nbParams) {
        Object o = registers[r].getObject (Register.TYPE_CONTACT);
        switch (m) {
        case ObjCall.save: // bool save (Contact c)
            registers[r].setBool (JSContact2.saveContact (o));
            return;
        case ObjCall.getFirstName: // String getFirstName (Contact c)
            registers[r].setString (JSContact2.getFirstName (o));
            return;
        case ObjCall.getLastName: // String getLastName (Contact c)
            registers[r].setString (JSContact2.getLastName (o));
            return;
        case ObjCall.getFormattedName: // String getFormattedName (Contact c)
            registers[r].setString (JSContact2.getFormatedName (o));
            return;
        case ObjCall.getAddress: // String getAddress (Contact c, [[int index], int subPart])
            registers[r].setString (JSContact2.getAddress (o,
                    nbParams >= 2 ? registers[r+1].getInt() : 0,
                    nbParams == 3 ? registers[r+2].getInt() : JSContact2.ADDR_ALL));
            return;
        case ObjCall.getField: // String getField (Contact c, int fieldType, [int index])
            registers[r].setString (JSContact2.getField(o,
                    registers[r+1].getInt(),
                    nbParams == 3 ? registers[r+2].getInt() : 0));
            return;
        case ObjCall.getFieldAttr: // int getFieldAttr (Contact c, int fieldType, [int index])
            registers[r].setInt (JSContact2.getFieldAttributes(o,
                    registers[r+1].getInt(),
                    nbParams == 3 ? registers[r+2].getInt() : 0));
            return;
        case ObjCall.getNbFields: // int getNbFields (Contact c, int fieldType)
            registers[r].setInt (JSContact2.getNbFields (o, registers[r+1].getInt()));
            return;
        case ObjCall.setName: // bool setName (Contact c, String firstName, String lastName)
            registers[r].setBool (JSContact2.setName (o, 
                    registers[r+1].getString(), 
                    registers[r+2].getString()));
            return;
        case ObjCall.setAddress: // int setAddress (Contact c, street, postalcode, city, country, [int index, [int attr]])
            registers[r].setInt (JSContact2.setAddress(o,
                    registers[r+1].getString(),
                    registers[r+2].getString(),
                    registers[r+3].getString(),
                    registers[r+4].getString(),
                    registers[r+5].getString(),
                    nbParams >= 7 ? registers[r+6].getInt() : 0,
                    nbParams == 8 ? registers[r+7].getInt() : 0));
            return;
        case ObjCall.setField: // int setField (Contat c, int fieldType, String value, [int index, [int attr]])
            registers[r].setInt (JSContact2.setField (o,
                    registers[r+1].getInt(),
                    registers[r+2].getString(), 
                    nbParams >= 4 ? registers[r+3].getInt() : 0,
                    nbParams == 5 ? registers[r+4].getInt() : 0));
            return;
        case ObjCall.deleteField: // bool deleteField (Contact c, int fieldType, [int index])
            registers[r].setBool (JSContact2.deleteField (o,
                    registers[r+1].getInt(),
                    nbParams == 3 ? registers[r+2].getInt() : 0));
            return;
        default: methodNotFound ("Contact", m); break;
        }
    }
//#endif
}
