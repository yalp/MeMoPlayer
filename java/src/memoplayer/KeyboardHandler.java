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

//#ifdef BlackBerry
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.system.Characters;
//#endif

public class KeyboardHandler {
    
//#ifndef BlackBerry
    final private static int KEYBOARD_DEFAULT = 0;
    final private static int KEYBOARD_SUNWTK = 98;
    final private static int KEYBOARD_MICROEMUL = 99;

    private static final int IDX_UNUSED         = 0;
    private static final int IDX_KEYCODE_UP     = 1;
    private static final int IDX_KEYCODE_DOWN   = 2;
    private static final int IDX_KEYCODE_LEFT   = 3;
    private static final int IDX_KEYCODE_RIGHT  = 4;
    private static final int IDX_KEYCODE_OK     = 5;
    private static final int IDX_KEYCODE_LSK    = 6;
    private static final int IDX_KEYCODE_RSK    = 7;
    private static final int IDX_KEYCODE_DELETE = 8;
    private static final int IDX_KEYCODE_RETURN = 9;
    private static final int IDX_KEYCODE_MAX    = 9;
    private static int       NAV_KEYCODES[]  = {0, -1, -2, -3, -4, -5, -6, -7, -8, 10};
    private final static int NAV_KEYVALUES[] = {0,'U','D','L','R','E','A','B','Z', -10};

    static {
    	// init custom key code from jad properties if specified
    	setKeyCodeFromJadProperty("MEMO-KEYCODE_UP",     IDX_KEYCODE_UP    );
    	setKeyCodeFromJadProperty("MEMO-KEYCODE_DOWN",   IDX_KEYCODE_DOWN  );
    	setKeyCodeFromJadProperty("MEMO-KEYCODE_LEFT",   IDX_KEYCODE_LEFT  );
    	setKeyCodeFromJadProperty("MEMO-KEYCODE_RIGHT",  IDX_KEYCODE_RIGHT );
    	setKeyCodeFromJadProperty("MEMO-KEYCODE_OK",     IDX_KEYCODE_OK    );
    	setKeyCodeFromJadProperty("MEMO-KEYCODE_LSK",    IDX_KEYCODE_LSK   );
    	setKeyCodeFromJadProperty("MEMO-KEYCODE_RSK",    IDX_KEYCODE_RSK   );
    	setKeyCodeFromJadProperty("MEMO-KEYCODE_DELETE", IDX_KEYCODE_DELETE);
    	setKeyCodeFromJadProperty("MEMO-KEYCODE_RETURN", IDX_KEYCODE_RETURN);
    }
    
    static void setKeyCodeFromJadProperty(String jadProperty,int idxKeyCode) {
    	try {
            int val = Integer.parseInt(MiniPlayer.getJadProperty(jadProperty));
            if( val != 0 ) {
            	NAV_KEYCODES[idxKeyCode]=val;
            }
        } catch (Exception e) { ; }
    }
    
    MyCanvas m_canvas;
    int m_keyboardType=-1;
    boolean m_useStandardFullKeyboard=false;
    
    
    KeyboardHandler (MyCanvas canvas) {
        m_canvas = canvas;

        // keyboard handler initialization
        String useFullKeyboard = MiniPlayer.getJadProperty("MEMO-USE_FULL_KEYBOARD");
        if (useFullKeyboard != null) {
            // check if use standard full keyboard
            if ( useFullKeyboard.equalsIgnoreCase ("std") ) {
                m_useStandardFullKeyboard = true;
            } else if( useFullKeyboard.equalsIgnoreCase ("true") ) {
                // init keyboard handler according to platform
                if (MyCanvas.s_platform.indexOf("SunMicrosystems_wtk", 0)>=0){
                    // Sun wireless emulator
                    m_keyboardType = KEYBOARD_SUNWTK;
                } else if (MyCanvas.s_platform.indexOf("MicroEmulator", 0)>=0){
                    // Micro emulator
                    m_keyboardType = KEYBOARD_MICROEMUL;
                } else {
                    m_keyboardType = KEYBOARD_DEFAULT;
                }
            }
        }
        Logger.println("Keyboard type: "+m_keyboardType);
    }
    
    public int convertKey(int key) {

        // check if it is a keypad numeric key
        if ( (key>=MyCanvas.KEY_NUM0) && (key<=MyCanvas.KEY_NUM9) ) {
            return ('0'+(key-MyCanvas.KEY_NUM0));
        }
        
        // other keypad keys
        switch (key) {
        case MyCanvas.KEY_STAR: return '*';
        case MyCanvas.KEY_POUND:return '#';
        }

        // check if use standard full keyboard
        if ( m_useStandardFullKeyboard == true ) {
            return convertStandardFullKeyboard (key);
        }
        
        // convert according to keyboard type
        switch (m_keyboardType) {
        case KEYBOARD_MICROEMUL:
        case KEYBOARD_SUNWTK:
            return convertForEmulator (key);
        }

        return convertByDefault (key);
    }

    private int convertByDefault (int key) {
        //Logger.println("Default convertKey: "+key);

        switch (key) {
        case MyCanvas.FIRE:     return 'E';
        // default return carriage
        case 10:              return -10;
        }

        // Game Action (SE)
        int myGameAction = m_canvas.getGameAction(key);
        switch (myGameAction) {
        case MyCanvas.UP:     return 'U';
        case MyCanvas.DOWN:   return 'D';
        case MyCanvas.LEFT:   return 'L';
        case MyCanvas.RIGHT:  return 'R';
        case MyCanvas.FIRE:   return 'E';
        case MyCanvas.GAME_A: return 'A';
        case MyCanvas.GAME_B: return 'B';
        case MyCanvas.GAME_C: return 'C';
        case MyCanvas.GAME_D: return 'Z';
        }
        
        // try keyCode
        try {
            if (key == m_canvas.getKeyCode(MyCanvas.UP)) {
                return 'U';
            } else if (key == m_canvas.getKeyCode(MyCanvas.DOWN)) {
                return 'D';
            } else if (key == m_canvas.getKeyCode(MyCanvas.LEFT)) {
                return 'L';
            } else if (key == m_canvas.getKeyCode(MyCanvas.RIGHT)) {
                return 'R';
            } else if (key == m_canvas.getKeyCode(MyCanvas.FIRE)) {
                return 'E';
            } else if (key == m_canvas.getKeyCode(MyCanvas.GAME_A) || key == -6) {
                return 'A';
            } else if (key == m_canvas.getKeyCode(MyCanvas.GAME_B) || key == -7) {
                return 'B';
            } else if (key == m_canvas.getKeyCode(MyCanvas.GAME_C) || key == -11) {
                return 'C';
            } else if (key == m_canvas.getKeyCode(MyCanvas.GAME_D)
                    || key ==  8
                    || key == -8
                    || key == -16 /* LG KF700 */) {
                return 'Z';
            }
        } catch (Exception e) { ; }

        // special generic
        switch (key) {
        case -21: return 'A'; // motorola left soft key
        case -22: return 'B'; // motorola right soft key
        }

        // ignore all other negative keys
        if (key < 0 ) {
            //Logger.println("Default special key: " + key);
            return 0;
        }

        // definitely a special key
        // Logger.println("Default special key: " + key);
        return 'X';
    }

    private int convertStandardFullKeyboard (int key) {
        // Logger.println("convertStandardFullKeyboard: "+key);

        // navigation key conversion
    	for(int i=1; i<=IDX_KEYCODE_MAX; i++) {
    		if(NAV_KEYCODES[i]==key) {
    			return NAV_KEYVALUES[i];
    		}
    	}

        // ignore all other negative keys
        if (key < 0) {
            return 0;
        }

        // definitely a special key
        // Logger.println("StandardFullKeyboard special key: " + key);
        return 'X';
    }

    private int convertForEmulator (int key) {
        // Logger.println("convertForEmulator: "+key);

        // GameAction
        int myGameAction = m_canvas.getGameAction(key);
        switch (myGameAction) {
        case MyCanvas.UP:   return 'U';
        case MyCanvas.DOWN: return 'D';
        case MyCanvas.LEFT: return 'L';
        case MyCanvas.RIGHT:return 'R';
        case MyCanvas.FIRE: return 'E';
        }

        try {
            if (key == m_canvas.getKeyCode(MyCanvas.UP)) {
                return 'U';
            } else if (key == m_canvas.getKeyCode(MyCanvas.DOWN)) {
                return 'D';
            } else if (key == m_canvas.getKeyCode(MyCanvas.LEFT)) {
                return 'L';
            } else if (key == m_canvas.getKeyCode(MyCanvas.RIGHT)) {
                return 'R';
            } else if ( key == -6) {
                return 'A';
            } else if ( key == -7) {
                return 'B';
            } else if ( key == -11) {
                return 'C';
            } else if (key == 8   // WTK QwertyDevice keyboard
                       || key == -8) {
                return 'Z';
            }
        } catch (Exception e) { ; }

        // definitely a special key
        //Logger.println("Emulator special key: " + key);
        return 'X';
    }
//#else
    KeyboardHandler (MyCanvas canvas) {
    }

    // BlackBerry key management
    public int convertKey(int key) {
        // check Keypad keycodes
        int keyCode = Keypad.key(key); 
        switch( keyCode ) {
            case 4098: // value of left soft key / menu button  
            case Characters.CONTROL_MENU : return 'A';
            case Characters.ESCAPE       : return 'B';
            case Characters.BACKSPACE    : return 'Z';
            case Characters.DELETE       : return 'Z';
            case Characters.CONTROL_RIGHT: return 'R';
            case Characters.CONTROL_UP   : return 'U';
            case Characters.CONTROL_LEFT : return 'L';
            case Characters.CONTROL_DOWN : return 'D';
            // case Characters. : return ''; break;
        }
        // nothing found, ignore key
        return 0;
    }
//#endif
}
