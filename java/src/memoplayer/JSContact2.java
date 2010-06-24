//#condition api.pim2
/*
* This software has been developed by France Telecom, FT/BD/DIH/HDM
*
* Copyright France Telecom 2008
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

import javax.microedition.pim.Contact;
import javax.microedition.pim.ContactList;
import javax.microedition.pim.PIM;

class JSContact2 {

   final static int FIELD_FORMATTED_NAME   = 0;
   final static int FIELD_NAME             = 1;
   final static int FIELD_ADDR             = 2;
   final static int FIELD_EMAIL            = 3;
   final static int FIELD_TEL              = 4;
   final static int FIELD_URL              = 5;
   final static int FIELD_BIRTHDAY         = 6;
   final static int FIELD_NOTE             = 7;
   final static int FIELD_ORG              = 8;
   final static int FIELD_ID               = 9;
   final static int FIELD_SIZE             = 10;

   final static int ADDR_ALL        = -1;
   final static int ADDR_POBOX      = 0;
   final static int ADDR_STREET     = 1;
   final static int ADDR_POSTALCODE = 2;
   final static int ADDR_CITY       = 3;
   final static int ADDR_COUNTRY    = 4;

   final static int ATTR_HOME = Contact.ATTR_HOME;
   final static int ATTR_WORK = Contact.ATTR_WORK;
   final static int ATTR_MOBILE = Contact.ATTR_MOBILE;
   final static int ATTR_PREFERED = Contact.ATTR_PREFERRED;
   final static int ATTR_FAX = Contact.ATTR_FAX;

   static String s_contactListName;
   static int[] s_supportedField;
   static int s_addressArraySize = 0;
   static int s_nameArraySize = 0;

   static void checkSupportedFields (ContactList l) {
       if (!l.getName().equals (s_contactListName)) {
           s_contactListName = l.getName();
           if (s_supportedField == null) {
               s_supportedField = new int[FIELD_SIZE];
           }
           for (int i=0; i<FIELD_SIZE; i++) {
               s_supportedField[i] = -1;
           }
           int[] fields = l.getSupportedFields();
           for (int i=0; i<fields.length; i++) {
               int field = fields[i];
               //Logger.println("DEBUG: Supported field: "+field+": "+l.getFieldLabel(field)+": "+l.maxValues(field));
               switch (field) {
               case Contact.NAME:
                   // Only support name if at least 2 fields are supported (firstname, lastname)
                   if (l.isSupportedArrayElement(Contact.NAME, Contact.NAME_FAMILY) |
                       l.isSupportedArrayElement(Contact.NAME, Contact.NAME_GIVEN)) {
                       s_supportedField[FIELD_NAME] = field;
                       s_nameArraySize = l.stringArraySize(Contact.NAME);
                   } else {
//#ifdef debug.console
                       Logger.println("PIM API: Warning: Name not supported ! Size: "+l.stringArraySize(Contact.NAME));
                       Logger.println("PIM API: Warning: Name family: "+l.isSupportedArrayElement(Contact.NAME, Contact.NAME_FAMILY));
                       Logger.println("PIM API: Warning: Name given: "+l.isSupportedArrayElement(Contact.NAME, Contact.NAME_GIVEN));
//#endif
                   }
                   break;
               case Contact.FORMATTED_ADDR:
                   // Only set formatted address if complete address is not supported
                   if (s_supportedField[FIELD_ADDR] == -1) {
                       s_supportedField[FIELD_ADDR] = field;
                   }
                   break;
               case Contact.ADDR:
                   // Only support address if at least 5 fields are supported (n°, street, postalcode, city, contry)
                   if (l.isSupportedArrayElement(Contact.ADDR, Contact.ADDR_POBOX)      |
                       l.isSupportedArrayElement(Contact.ADDR, Contact.ADDR_STREET)     |
                       l.isSupportedArrayElement(Contact.ADDR, Contact.ADDR_POSTALCODE) |
                       l.isSupportedArrayElement(Contact.ADDR, Contact.ADDR_LOCALITY)   |
                       l.isSupportedArrayElement(Contact.ADDR, Contact.ADDR_COUNTRY)) {
                       s_supportedField[FIELD_ADDR] = field;
                       s_addressArraySize = l.stringArraySize(Contact.ADDR);
                   } else {
//#ifdef debug.console
                       Logger.println("PIM API: Warning: Address split not supported ! Size: "+l.stringArraySize(Contact.ADDR));
                       Logger.println("PIM API: Warning: Address pobox: "+l.isSupportedArrayElement(Contact.ADDR, Contact.ADDR_POBOX));
                       Logger.println("PIM API: Warning: Address street: "+l.isSupportedArrayElement(Contact.ADDR, Contact.ADDR_STREET));
                       Logger.println("PIM API: Warning: Address postalcode: "+l.isSupportedArrayElement(Contact.ADDR, Contact.ADDR_POSTALCODE));
                       Logger.println("PIM API: Warning: Address locality: "+l.isSupportedArrayElement(Contact.ADDR, Contact.ADDR_LOCALITY));
                       Logger.println("PIM API: Warning: Address country: "+l.isSupportedArrayElement(Contact.ADDR, Contact.ADDR_COUNTRY));
//#endif
                   }
                   break;
               case Contact.FORMATTED_NAME:
                   s_supportedField[FIELD_FORMATTED_NAME] = field; break;
               case Contact.EMAIL:
                   s_supportedField[FIELD_EMAIL] = field; break;
               case Contact.TEL:
                   s_supportedField[FIELD_TEL] = field; break;
               case Contact.URL:
                   s_supportedField[FIELD_URL] = field; break;
               case Contact.BIRTHDAY:
                   s_supportedField[FIELD_BIRTHDAY] = field; break;
               case Contact.UID:
                   s_supportedField[FIELD_ID] = field; break;
               case Contact.ORG:
                   s_supportedField[FIELD_ORG] = field; break;
               case Contact.NOTE:
                   s_supportedField[FIELD_NOTE] = field; break;
               }
           }
       }
   }

   static int getNativeField (int field) {
       if (field>=0 && field<s_supportedField.length) {
           return s_supportedField[field];
       }
       return -1;
   }

   static String listContactLists () {
       try {
           String[] list = PIM.getInstance().listPIMLists (PIM.CONTACT_LIST);
           StringBuffer sb = new StringBuffer();
           for (int i=0; i<list.length; i++) {
               if (i!=0) sb.append (',');
               sb.append (list[i]);
           }
           return sb.toString();
       } catch (Exception e) {
           return "";
       }
   }

   static ContactList openContactList (String name) {
       try {
           ContactList l;
           if (name == null) {
               l = (ContactList) PIM.getInstance().openPIMList (PIM.CONTACT_LIST, PIM.READ_WRITE);
           } else {
               l = (ContactList) PIM.getInstance().openPIMList (PIM.CONTACT_LIST, PIM.READ_WRITE, name);
           }
           checkSupportedFields (l);
           return l;
       } catch (Exception e) {
           return null;
       }
   }

   static boolean closeContactList (Object o) {
       if (o instanceof ContactList) {
           ContactList cl = (ContactList)o;
           try { 
               cl.close();
               return true;
           } catch (Exception e) { }
       }
       return false;
   }

   static int getMaxFields (Object o, int field) {
       field = getNativeField (field);
       if (field != -1 && o instanceof ContactList) {
           ContactList c = (ContactList)o;
           try {
               int v = c.maxValues (field);
               // -1 => unlimited fields return MAX_VALUE as -1 is already reserved for error
               return v == -1 ? Integer.MAX_VALUE : v; 
           } catch (Exception e) { }
       }
       return -1;
   }

   static Enumeration listContacts (Object o, Object o2) {
       try {
           if (o instanceof ContactList) {
               ContactList cl = (ContactList)o;
               if (o2 instanceof Contact) {
                   // Search matching contacts
                   Contact c = (Contact)o2;
                   return cl.items (c);
               } else if (o2 instanceof String) {
                   // Search matchin string
                   String search = (String)o2;
                   return cl.items (search);
               }
               // List all contacts
               return cl.items();
           }
       } catch (Exception e) { }
       return null;
   }

   static Object newContact (Object o) {
       if (o instanceof ContactList) {
           ContactList l = (ContactList)o;
           try {
               return l.createContact();
           } catch (Exception e) { }
       }
       return null;
   }

   static boolean deleteContact (Object o, Object o2) {
       if (o instanceof ContactList &&
           o2 instanceof Contact) {
           ContactList l = (ContactList)o;
           Contact c = (Contact)o2;
           try {
               l.removeContact(c);
               return true;
           } catch (Exception e) { }
       }
       return false;
   }

   static boolean saveContact (Object o) {
       if (o instanceof Contact) {
           Contact c = (Contact)o;
           try {
               c.commit();
               return true;
           } catch (Exception e) { }
       }
       return false;
   }

   static boolean setName (Object o, String firstName, String lastName) {
       if (o instanceof Contact) {
           Contact c = (Contact)o;
           try {
               if (s_supportedField[FIELD_NAME] != -1) {
                   String[] name = new String[s_nameArraySize];
                   name[Contact.NAME_FAMILY] = firstName;
                   name[Contact.NAME_GIVEN] = lastName;
                   if (c.countValues(Contact.NAME) == 0) {
                       c.addStringArray(Contact.NAME, Contact.ATTR_NONE, name);
                   } else {
                       c.setStringArray(Contact.NAME, 0, Contact.ATTR_NONE, name);
                   }
               }
               if (s_supportedField[FIELD_FORMATTED_NAME] != -1) {
                   if (c.countValues(Contact.FORMATTED_NAME) == 0) {
                       c.addString(Contact.FORMATTED_NAME, Contact.ATTR_NONE, firstName+" "+lastName);
                   } else {
                       c.setString(Contact.FORMATTED_NAME, 0, Contact.ATTR_NONE, firstName+" "+lastName);
                   }
               }
               return true;
           } catch (Exception e) { }
       }
       return false;
   }

   static int setAddress (Object o, String pobox, String street, String postalcode, String city, String country, int index, int attr) {
       int nb = getNbFields (o, FIELD_ADDR);
       if (nb >= 0) { // supported field
           try {
               int field = getNativeField (FIELD_ADDR);
               if (field == Contact.ADDR) {
                   Contact c = (Contact)o; // already checked by getNbFields()
                   String[] adress = new String [s_addressArraySize];
                   adress[Contact.ADDR_POBOX] = pobox;
                   adress[Contact.ADDR_STREET] = street;
                   adress[Contact.ADDR_POSTALCODE] = postalcode;
                   adress[Contact.ADDR_LOCALITY] = city;
                   adress[Contact.ADDR_COUNTRY] = country;
                   if (index>=0 && index <nb) {
                       c.setStringArray (Contact.ADDR, index, attr, adress);
                   } else {
                       c.addStringArray (Contact.ADDR, attr, adress);
                       index = nb;
                   }
                   return index;
               } else if (field == Contact.FORMATTED_ADDR) {
                   Contact c = (Contact)o; // already checked by getNbFields()
                   StringBuffer sb = new StringBuffer();
                   if (sb.length() != 0) sb.append (' '); 
                   sb.append (pobox);
                   if (sb.length() != 0) sb.append (' ');
                   sb.append (street);
                   if (sb.length() != 0) sb.append (' ');
                   sb.append (postalcode);
                   if (sb.length() != 0) sb.append (' '); 
                   sb.append(city);
                   if (sb.length() != 0) sb.append (' ');
                   sb.append(country);
                   if (index>=0 && index <nb) {
                       c.setString (Contact.FORMATTED_ADDR, index, attr, sb.toString());
                   } else {
                       c.addString (Contact.FORMATTED_ADDR, attr, sb.toString());
                       index = nb;
                   }
                   return index;
               }
           } catch (Exception e) { }
       }
       return -1;
   }

   static int setField (Object o, int field, String value, int index, int attr) {
       int nb = getNbFields(o, field);
       if (nb >= 0) { // supported field
           field = getNativeField (field);
           try {
               Contact c = (Contact)o; // already checked by getNbFields()
               if (index >= 0 && index < nb) { // exists, overwrite
                   if (field == Contact.BIRTHDAY) {
                       c.setDate (field, index, attr, Long.parseLong (value));
                   } else {
                       c.setString (field, index, attr, value);
                   }
               } else {
                   if (field == Contact.BIRTHDAY) {
                       c.addDate (field, index, Long.parseLong (value));
                   } else {
                       c.addString (field, attr, value);
                   }
                   index = nb;
               }
               return index;
           } catch (Exception e) { }
       }
       return -1;
   }

   static boolean deleteField (Object o, int field, int index) {
       if (o instanceof Contact) {
           Contact c = (Contact)o;
           try {
               c.removeValue(getNativeField(field), index);
               return true;
           } catch (Exception e) { }
       }
       return true;
   } 

   static String getField (Object o, int field, int index) {
       if (o instanceof Contact) {
           Contact c = (Contact)o;
           field = getNativeField (field);
           try {
              if (field == Contact.BIRTHDAY) {
                  return String.valueOf (c.getDate (field, index));
              } else {
                  return c.getString (field, index);
              }
           } catch (Exception e) { }
       }
       return "";
   }

   static int getFieldAttributes (Object o, int field, int index) {
       field = getNativeField (field);
       if (field != -1 && o instanceof Contact) {
           Contact c = (Contact)o;
           try {
               return c.getAttributes (field, index);
           } catch (Exception e) { }
       }
       return -1;
   }

   static int getNbFields (Object o, int field) {
       field = getNativeField (field);
       if (field != -1 && o instanceof Contact) {
           Contact c = (Contact)o;
           try {
               return c.countValues (field);
           } catch (Exception e) { }
       }
       return -1;
   }

   static String getFirstName (Object o) {
       if (o instanceof Contact) {
           Contact c = (Contact)o;
           try {
               return c.getStringArray(Contact.NAME, 0)[Contact.NAME_FAMILY];
           } catch (Exception e) { }
       }
       return "";
   }

   static String getLastName (Object o) {
       if (o instanceof Contact) {
           Contact c = (Contact)o;
           try {
               return c.getStringArray(Contact.NAME, 0)[Contact.NAME_GIVEN];
           } catch (Exception e) { }
       }
       return "";
   }

   static String getFormatedName (Object o) {
       if (o instanceof Contact) {
           Contact c = (Contact)o;
           try {
               return c.getString(Contact.FORMATTED_NAME, 0);
           } catch (Exception e) {
               // Fallback on NAME_FAMILY NAME_GIVEN
               try {
                   String[] name = c.getStringArray(Contact.NAME, 0);
                   return name[Contact.NAME_FAMILY]+" "+name[Contact.NAME_GIVEN];
               } catch (Exception e2) { }
           }
       }
       return "";
   }

   static String getAddress (Object o, int index, int subPart) {
       if (o instanceof Contact) {
           Contact c = (Contact)o;
           try {
               int field = getNativeField (FIELD_ADDR);
               if (field == Contact.ADDR) {
                   String[] address =  c.getStringArray(Contact.ADDR, index);
                   switch (subPart) {
                   case ADDR_ALL:
                       StringBuffer sb = new StringBuffer();
                       for (int i=0; i<address.length; i++) {
                           if (address[i] != null && address[i].length() > 0) {
                               if (i != 0) sb.append (" ");
                               sb.append (address[i]);
                           }
                       }
                       return sb.toString();
                   case ADDR_POBOX:
                       return address[Contact.ADDR_POBOX];
                   case ADDR_STREET:
                       return address[Contact.ADDR_STREET];
                   case ADDR_POSTALCODE:
                       return address[Contact.ADDR_POSTALCODE];
                   case ADDR_CITY:
                       return address[Contact.ADDR_LOCALITY];
                   case ADDR_COUNTRY:
                       return address[Contact.ADDR_COUNTRY];
                   }
               } else if (field == Contact.FORMATTED_ADDR && subPart == ADDR_ALL) {
                   return c.getString (Contact.FORMATTED_ADDR, index);
               }
           } catch (Exception e) { }
       }
       return "";   
   }
}
