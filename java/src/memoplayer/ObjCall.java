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

class ObjCall {
//#ifdef debug.console
    final static String[] methodNames = {"length", "charAt", "substring", "startsWith", "endsWith", "indexOf", "lastIndexOf", "toLower", "toUpper", "strIndexOf", "toInt", "toFloat", "trim", "decodeUrl", "encodeUrl", "replace"};
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
}
