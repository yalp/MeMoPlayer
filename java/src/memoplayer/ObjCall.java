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
    final static String[] methodNames = {};
//#endif

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
        default: Logger.println ("Unsupported object type: "+registers[r].getType());
        }
    }
    
    static void doInt (Context c, int m, Register [] registers, int r, int nbParams) {
        methodNotFound ("int", m);
    }
    
    static void doFloat (Context c, int m, Register [] registers, int r, int nbParams) {
        methodNotFound ("float", m);
    }
}
