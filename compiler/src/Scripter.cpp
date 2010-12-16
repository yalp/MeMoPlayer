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

# include "stdio.h"
# include "stdlib.h"
# include "string.h"
# include "Tokenizer.h"
# include "Types.h"
# include "Code.h"
# include "Scripter.h"
# include "ExternCalls.inc"

extern FILE * myStderr;

void ensure (char c, const char * msg, Tokenizer * t) {
    if (t->check (c) == false) {
        fprintf (myStderr, msg, t->getFile(), t->getLine ());
        exit (1);
    }
}

class NameLink {
protected:
    char * m_name;
    int m_index;
    NameLink * m_next;
    
public: 

    NameLink (char * name, int index, NameLink * next) {
        m_name = name;
        m_index = index;
        m_next = next;
    }
    virtual ~NameLink () {}

    NameLink * find (char * name) {
        if (strcmp (name, m_name) == 0) {
            return this;
        }
        if (m_next != NULL) {
            return m_next->find (name);
        }
        return NULL;
    }
    NameLink * find (int index) {
        if (index ==  m_index) {
            return this;
        }
        if (m_next != NULL) {
            return m_next->find (index);
        }
        return NULL;
    }

    int getIndex (char * name) {
        NameLink * nl = find (name);
        return nl ? nl->m_index : -1;
    }

    int getIndex () {
        return m_index;
    }

    virtual void print () {
        if (m_next) {
            m_next->print ();
        }
        fprintf (myStderr, "    %s\n", m_name);
    }

    void dump (FILE * out) {
        if (m_next) {
            m_next->dump (out);
        }
        fprintf (out, "    final static int %s = %i;\n", m_name, m_index);
    }
    void dumpNames (FILE * out) {
        if (m_next) {
            m_next->dumpNames (out);
            fprintf (out, ", ");
        }
        fprintf (out, "\"%s\"", m_name);
    }
};

class ClassLink : public NameLink {
    NameLink * m_functions;
    int m_funcIndex;
    
public:
    ClassLink (char * name, int index, ClassLink * next) : NameLink (name, index, next) {
        m_funcIndex = 0;
        m_functions = NULL;
    }
    void addFunction (char * name) {
        m_functions = new NameLink (name, m_funcIndex++, m_functions);
    }

    int getFuncIndex (char * name) {
        return m_functions->getIndex (name);
    }

    void print () {
        if (m_next) {
            m_next->print ();
        }
        fprintf (myStderr, "%s {\n", m_name);
        m_functions->print ();
        fprintf (myStderr, "}\n");
    }

    void dump (char * n) {
        FILE * out = fopen (n, "wb");
        if (out == NULL) {
            fprintf (stderr, "Error: cannot open %s for writing\n", n);
            exit (1);
        }
        fprintf (out, "package memoplayer;\n\n");
        fprintf (out, "class ObjCall {\n");
        fprintf (out, "    final static String[] methodNames = {");
        if (m_functions != NULL) {
            m_functions->dumpNames (out);
        }
        fprintf (out, "};\n");
        if (m_functions != NULL) {
            m_functions->dump (out);
        }
        fprintf (out, "}\n");
        fclose (out);
    }
};

class ExternClasses {
    // List of static function, grouped by Class
    ClassLink * m_classes;
    int m_classIndex;
    // List of instance functions
    ClassLink * m_instanceFunctions;
public:
    ExternClasses (char * filename) {
        m_classIndex = 0;
        m_classes = NULL;
        m_instanceFunctions = NULL;
        Tokenizer t (filename, false, "ExternCalls", 1);
        ClassLink * c;
        char * token = t.getNextToken ();
        while (token) { // new class
            if (strcmp (token, "Instance") == 0) {
                c = m_instanceFunctions = new ClassLink (token, 0, NULL);
            } else {
                c = m_classes = new ClassLink (token, m_classIndex++, m_classes);
            }
            if (t.check ('{') == false) {
                fprintf (myStderr, "Syntax error in %s line %d '{' expected\n", filename, t.getLine ());
                exit (1);
            }
            token = t.getNextToken ();
            while (token) {
                c->addFunction (token);
                token = t.getNextToken ();
            }
            if (t.check ('}') == false) {
                fprintf (myStderr, "Syntax error in %s line %d '}' expected\n", filename, t.getLine ());
                exit (1);
            }
            token = t.getNextToken ();
        }
        //m_classes->print ();
    }

    int getIndex (char * className) {
        ClassLink * cl = (ClassLink*)m_classes->find (className);
        return cl ? cl->getIndex () : -1;
    }

    int getIndex (int id, char * funcName) {
        ClassLink * cl = (ClassLink*)m_classes->find (id);
        return cl ? cl->getFuncIndex (funcName) : -1;
    }

    int getInstanceFunctionIndex (char * funcName) {
        return m_instanceFunctions ? m_instanceFunctions->getFuncIndex (funcName) : -1;
    }

    void dumpInstanceFunctions (char * n) {
        if (m_instanceFunctions) {
            m_instanceFunctions->dump (n);
        }
    }
};

static ExternClasses * s_externClasses = NULL;

static int getClassID (char * o) {
    return s_externClasses->getIndex (o);
}

static int getMethodID (int objID, char * m) {
    return s_externClasses->getIndex (objID, m);
}

static int getInstanceMethodID (char * m) {
    return s_externClasses->getInstanceFunctionIndex (m);
}

void dumpMethods (char * n) {
    if (s_externClasses == NULL) {
        s_externClasses = new ExternClasses (externCallsDef);
    }
    s_externClasses->dumpInstanceFunctions (n);
}

Var * Var::purgeAll (int level, ByteCode * bc) {
    Var * next = m_next ? m_next->purgeAll (level, bc) : NULL;
    if (m_level >= level) {
        bc->freeRegister (m_index);
        return next;
    }
    m_next = next;
    return (this);
}

Function::Function (Node * node) {
    m_vars = NULL;
    m_nbVars = 0;
    m_code = NULL;
    m_node = node;
    m_blockLevel = 1;
    m_counter = 0;
    m_inLoop = false;
    m_switchLevel = 0;
}

Var * Function::addVar (char * name, int level, int index) {
    //printf (" -------------- adding var %s %d -> %d\n", name, level, index);
    //printVars ();
    m_nbVars++;
    m_vars = new Var (name, level, index, m_vars);
    //printVars ();
    //printf (" --------------\n");
    return m_vars;
}

Var * Function::findVar (char * name) {
    return m_vars ? m_vars->find (name) : NULL;
}

void Function::printVars () {
    Var * v = m_vars;
    printf (" [[ ");
    while (v) {
        printf ("'%s'/%d->%d ", v->m_name, v->m_level, v->m_index);
        v = v->m_next;
    }
    printf (" ]]\n");
}

void Function::removeVar (char * name) {
    //return m_vars ? m_vars->find (name) : NULL;
}

void Function::removeVars (int level, ByteCode * bc) {
    if (m_vars) {
        m_vars = m_vars->purgeAll (level, bc);
    }
    //return m_vars ? m_vars->find (name) : NULL;
}

Field * Function::findField (char * name) {
    if (m_node) {
        return m_node->findField (name);
    } else {
        fprintf (myStderr, "findField %s in NULL node!!!\n", name);
    }
    return (NULL);
}

Code * appendCode (Code * root, Code * val) {
    if (root) {
        root->append (val);
    } else {
        root = val;
    }
    return (root);
}

Code * Function::parseFieldAccess (Tokenizer * t, char * token, int codeVar, int codeField) {
    Code * lvalue = NULL;
    bool isArray = false;
    Var * var = m_vars ? m_vars->find (token) : NULL;
    if (var) {
        if (t->check ('[')) { // parse indexed access
            fprintf (myStderr, "%s:%d: JS syntax error: array access is not supported on variables.\n", t->getFile(), t->getLine());
            exit (1);
        }
        if (t->check('.')) {
            char * s = t->getNextToken ();
            if (s == NULL) {
                fprintf (myStderr, "%s:%d: JS syntax error: expected method name after '%s.'\n", t->getFile(), t->getLine (), token);
                exit (1);
            }
            if (t->check ('(', true) == false) {
                fprintf (myStderr, "%s:%d: JS syntax error: '(' expected after '%s.%s'.\n", t->getFile(), t->getLine (), token, s);
                exit (1);
            }
            int methodID = getInstanceMethodID (s);
            if (methodID < 0) {
                fprintf (myStderr, "%s:%d: JS syntax error: unknown instance method %s\n", t->getFile(), t->getLine (), s);
                exit (1);
            }
            lvalue = new Code (Code::CODE_GET_VAR, new Code (token));
            return new Code (Code::CODE_CALL_METHOD, lvalue, new Code (methodID), parseParams (t));
        }
        return new Code (codeVar, new Code (token));
    }
    Field * lastField = NULL;
    while (true) {
        if (token == NULL) {
            fprintf (myStderr, "%s:%d: JS syntax error: field expected\n", t->getFile(), t->getLine());
            return NULL;
        }
        Field * field = NULL;
        if (lastField) {
            // shoudl check for a SFNode or a MF*
            field = lastField->findField (token);
        } else {
            field = findField (token);
        }
        if (field) { // is it a field ?
            //MCP: is it a reference to a (un)declared function ?
            if (codeField == Code::CODE_GET_FIELD && lvalue == NULL && 
                (field->m_type == Field::TYPE_ID_SFDEFINED || field->m_type == Field::TYPE_ID_SFTMP)) {
                return new Code (field->m_number);
            }
            //fprintf (myStderr, "DBG:parseFieldAcess %s: got field %d\n", token, field->m_number);
            lvalue = appendCode (lvalue, new Code (Code::CODE_USE_FIELD, new Code (field->m_number)));
            lastField = field;
        } else {
            if (lastField != NULL) {
              int index = lastField->findIndex (token, isArray);
              //fprintf (myStderr, "DBG:parseFieldAcess: %s.%s -> %d\n", lastField->m_name, token, index);
              if (index < 0) {
                  fprintf (myStderr, "%s:%d: JS syntax error: unknown field: %s\n", t->getFile(), t->getLine(), token);
                  exit (1);
              }
              return appendCode (lvalue, new Code (codeField, new Code (index)));
            } else if (codeField == Code::CODE_GET_FIELD && lvalue == NULL) {
                //MCP: token is not a field or var but it might be a reference to a not-yet-defined function !
                Field * field = m_node->addField (strdup ("SFTmp"), token, true);
                field->m_lineNum = t->getLine ();
                return new Code (field->m_number);
            } else {
                fprintf (myStderr, "%s:%d: JS syntax error: unknown field %s\n", t->getFile(), t->getLine (), token);
                exit (1);
            }
        }
        if (t->check ('[')) { // parse indexed access
            //fprintf (myStderr, "DBG:parseFieldAcess: got '['\n");
            lvalue = appendCode (lvalue, new Code (Code::CODE_USE_IDX_FIELD, parseExpr (t)));
            if (t->check (']') == false) {
                fprintf (myStderr, "%s:%d: JS syntax error: ']' expected\n", t->getFile(), t->getLine());
                exit (1);
            }
            isArray = true;
            //fprintf (myStderr, "DBG:parseFieldAcess: got ']'\n");
        }
        if (t->check ('.')) {
            char * s = t->getNextToken ();
            if (s == NULL) {
                fprintf (myStderr, "%s:%d: JS syntax error: expected field or method name after '%s.'\n", t->getFile(), t->getLine (), token);
                exit (1);
            } else if (t->check ('(', true)) { // must be an instance method call !
                int methodID = getInstanceMethodID (s);
                if (methodID < 0) {
                    fprintf (myStderr, "%s:%d: JS syntax error: unknown instance method %s() on field %s\n", t->getFile(), t->getLine (), s, token);
                    exit (1);
                }
                lvalue = appendCode (lvalue, new Code (Code::CODE_GET_FIELD, new Code (0)));
                return new Code (Code::CODE_CALL_METHOD, lvalue, new Code (methodID), parseParams (t));
            }
            token = s;
        } else { // should be '='
            int index  = 0;
            if (isArray == false && field != NULL && field->isMFField()) {
                index = 254; // see java Field.OBJECT_IDX: we want to use teh object, not the first element
            }
            //fprintf (myStderr, "DBG: the token is final => using index %d\n", index);
            return appendCode (lvalue, new Code (codeField, new Code (index)));
        }
    }
    return lvalue;
}

Code * Function::parseLValue (Tokenizer * t, char * token) {
    Code * tmp = parseFieldAccess (t, token, Code::CODE_SET_VAR, Code::CODE_SET_FIELD);
    if (tmp == NULL) {
        fprintf (myStderr, "%s:%d: JS syntax error: parsing lvalue\n", t->getFile(), t->getLine());
    }
    return tmp;
}

Code * Function::parseIdent (Tokenizer * t, char * token) {
    Code * tmp;
    if ( t->check ('(', true)) {
        //fprintf (myStderr, "parsing internal func call %s\n", token);
        tmp = parseInternFunc (token, t);
    } else {
        tmp = parseFieldAccess (t, token, Code::CODE_GET_VAR, Code::CODE_GET_FIELD);
    }
    if (tmp == NULL) {
        fprintf (myStderr, "%s:%d: JS syntax error: parsing identifier\n", t->getFile(), t->getLine());
    }
    return tmp;
}

Code * Function::parseAssign (Tokenizer * t) {
    // cases are :
    // lvalue = expr;
    // if (test) { expr; } [ else { expr }©∂

/*
  Code * lvalue = parseLValue (t, token);
  if (t->check ('=') == false) {
  return (NULL);
  }
  Code * expr = parseExpr (t);
  return (new Code (Code::ASSIGN, lvalue, expr));
*/
    return (NULL);
}

int Function::parseAssign (Tokenizer * t, bool & self) {
    self = false;
    if (t->check('+')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_PLUS);
        } else {
            t->UNGETC ('+');
        }
    } else if (t->check('-')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_MINUS);
        } else {
            t->UNGETC ('-');
        }
    } else if (t->check('/')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_DIV);
        } else {
            t->UNGETC ('/');
        }
    } else if (t->check('*')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_MULT);
        } else {
            t->UNGETC ('*');
        }
    } else if (t->check('%')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_MODULO);
        } else {
            t->UNGETC ('%');
        }
    } else if (t->check('&')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_BIT_AND);
        } else {
            t->UNGETC ('&');
        }
    } else if (t->check('|')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_BIT_OR);
        } else {
            t->UNGETC ('|');
        }
    } else if (t->check('^')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_BIT_XOR);
        } else {
            t->UNGETC ('^');
        }
    } else if (t->check('<')) {
        if (t->check('<')) {
            if ( (self = t->CHECK ('=')) == true) {
                return (Code::CODE_BIT_LSHIFT);
            } else {
                t->UNGETC ('<');
            }
        } else {
            t->UNGETC ('<');
        }
    } else if (t->check('>')) {
        if (t->check('>')) {
            if (t->check('>')) {
                if ( (self = t->CHECK ('=')) == true) {
                    return (Code::CODE_BIT_RRSHIFT);
                } else {
                    t->UNGETC ('>');
                }
            } else if ( (self = t->CHECK ('=')) == true) {
                return (Code::CODE_BIT_RSHIFT);
            } else {
                t->UNGETC ('>');
            }
        } else {
          t->UNGETC ('>');
        }
    } else if (t->check('=')) {
        return (Code::CODE_ASSIGN);
    }
    return (Code::CODE_ERROR);
}

int Function::parseOperation (Tokenizer * t, int & arity) {
    arity = 2;
    if (t->check('+')) {
        if (t->CHECK ('+')) {
            arity = 1;
        }
        return Code::CODE_PLUS;
    } else if (t->check('-')) {
        if (t->CHECK ('-')) {
            arity = 1;
        }
        return Code::CODE_MINUS;
    } else if (t->check('/')) {
        return (Code::CODE_DIV);
    } else if (t->check('*')) {
        return (Code::CODE_MULT);
    } else if (t->check('%')) {
        return (Code::CODE_MODULO);
    } else if (t->check('&')) {
        if (t->CHECK ('&')) {
            return (Code::CODE_LOG_AND);
        }
        return (Code::CODE_BIT_AND);
    } else if (t->check('|')) {
        if (t->CHECK ('|')) {
            return (Code::CODE_LOG_OR);
        }
        return (Code::CODE_BIT_OR);
    } else if (t->check('=')) {
        if (t->CHECK ('=')) {
            return (Code::CODE_EQUAL);
        }
    } else if (t->check('!')) {
        if (t->CHECK ('=')) {
            return (Code::CODE_NOTEQUAL);
        }
    } else if (t->check('<')) {
        if (t->CHECK ('=')) {
            return (Code::CODE_LESSEQ);
        } else if (t->CHECK ('<')) {
        	return (Code::CODE_BIT_LSHIFT);
        }
        return (Code::CODE_LESSER);
    } else if (t->check('>')) {
        if (t->CHECK ('=')) {
            return (Code::CODE_GREATEQ);
        } else if (t->CHECK ('>')) {
          if (t->CHECK ('>')) {
            return (Code::CODE_BIT_RRSHIFT);
          }
        	return (Code::CODE_BIT_RSHIFT);
        }
        return (Code::CODE_GREATER);
    } else if (t->check('^')) {
        return (Code::CODE_BIT_XOR);
    } else if (t->check('?')) {
        arity = 3;
        return (Code::CODE_TERNARY_COMP);
    }
    return (Code::CODE_ERROR);
}

static bool checkInside (Tokenizer * t, const char * s) {
    while (*s) {
        if (t->check (*s, true)) {
            return true;
        }
        s++;
    }
    return (false);
}

Code * Function::parseExpr (Tokenizer * t) {
    // cases are :
    // litteral
    // var
    // expr op expr
    // ( expr op expr )
    Code * left = NULL;
    Code * right = NULL;
    int operation = Code::CODE_NOP;
    if (t->check ('-')) { // unary minus operator
        left = parseExpr (t);
        return new Code (Code::CODE_MULT, new Code (-1), left);
    }
    if (t->check ('~')) { // unary ~ bit operator
        left = parseExpr (t);
        return new Code (Code::CODE_BIT_INV, left);
    }
    if (t->check ('!')) { // unary ! not operator
        left = parseExpr (t);
        return new Code (Code::CODE_EQUAL, left, new Code(0));
    }
    if (t->check ('(')) {
        left = parseExpr (t);
        if (t->check (')') == false) {
            fprintf (myStderr, "%s:%d: JS syntax error: missing ')'\n", t->getFile(), t->getLine ());
            exit (1);
        }
    } else {
        left = parseVarOrVal (t);
    }
    if (checkInside (t, ";],):")) {
        return left;
    }
    int arity = 0;
    operation = parseOperation (t, arity);
    if (operation == Code::CODE_ERROR) {
        fprintf (myStderr, "%s:%d: JS syntax error: missing operator\n", t->getFile(), t->getLine ());
        exit (1);
    }
    if (arity == 1) {
        return new Code (operation, left);
    }
    if (t->check ('(')) {
        right = parseExpr (t);
        if (t->check (')') == false) {
            fprintf (myStderr, "%s:%d: JS syntax error: missing ')'\n", t->getFile(), t->getLine ());
            exit (1);
        }
    } else {
        right = parseVarOrVal (t);
    }
    if (arity == 2) {
        Code * tmp = new Code (operation, left, right);
        if (t->check ('+')) { // special case of +, mostly for String concat
            return new Code (Code::CODE_PLUS, tmp, parseExpr (t));
        } else {
            return tmp;
        }
    }
    if (t->check (':')) { // arity == 3
        Code * third = NULL;
        if (t->check ('(')) {
            third = parseExpr (t);
            if (t->check (')') == false) {
                fprintf (myStderr, "%s:%d: JS syntax error: missing ')'\n", t->getFile(), t->getLine ());
                exit (1);
            }
        } else {
            third = parseVarOrVal (t);
        }
        return new Code (operation, left, right, third);
    } else {
        fprintf (myStderr, "%s:%d: JS syntax error: missing ':' after '?' for ternary expression.\n", t->getFile(), t->getLine ());
        exit (1);
    }
}

Code * Function::parseTest (Tokenizer * t) {
    // ( expr )
    Code * code = NULL;
    if (t->check ('(') == false) {
        fprintf (myStderr, "%s:%d: JS syntax error: missing '('\n", t->getFile(), t->getLine ());
        return (NULL);
    }
    code = parseExpr (t);
    if (t->check (')') == false) {
        fprintf (myStderr, "%s%d: JS syntax error: missing ')'\n", t->getFile(), t->getLine ());
        return (NULL);
    }
    return code;
}

Code * Function::parseVarOrVal (Tokenizer * t) {
    // cases are :
    // litteral
    // var

    char * s  = t->getNextString ();

    if (s) {
        //fprintf (myStderr, "DBG: got String '%s'\n", s);
        return new Code (s, true);
    }
    s = t->getNextToken ();
    if (s) {
        if (strcmp (s, "true") == 0) {
            return new Code (1);
        } else if (strcmp (s, "false") == 0 || strcmp (s, "null") == 0) {
            return new Code (0);
        }
        int objID = getClassID (s);
        if (objID > -1) {
            return parseExternFunc (objID, t);
        }
        //fprintf (myStderr, "DBG: got ident '%s'\n", s);
        return parseIdent (t, s);
    }
    bool intFlag = false, isNumber;
    float f = t->getNextFloat (&isNumber, &intFlag);
    if (isNumber) {
        if (intFlag) {
            //fprintf (myStderr, "DBG: got int '%d'\n", int (f));
            return new Code (int (f));
        } else {
            //fprintf (myStderr, "DBG: got float '%g'\n", f);
            return new Code (f);
        }
    }

    return (NULL);
}

Code * Function::parseParams (Tokenizer*t) {
    if (t->check ('(') == false) {
        fprintf (myStderr, "%s:%d: JS syntax error: '(' expecteds\n", t->getFile(), t->getLine ());
        exit (1);
    }
    Code * code = NULL;
    Code * tmp = parseExpr (t);
    if (tmp) {
        code = new Code (Code::CODE_PARAM, tmp);
        while (t->check (',')) {
            tmp = parseExpr (t);
            if (tmp == NULL) {
                fprintf (myStderr, "%s:%d: JS syntax error: expression expected in call\n", t->getFile(), t->getLine ());
                exit (1);
            }
            code->append (new Code (Code::CODE_PARAM, tmp));
        }
    }
    if (t->check (')') == false) {
        fprintf (myStderr, "%s:%d: JS syntax error: ')' expecteds\n", t->getFile(), t->getLine ());
        exit (1);
    }
    //fprintf (myStderr, "DBG: end of params\n");
    return (code);
}

// Code * Function::parseExternCall (int objID, Tokenizer * t) {
//     Code * tmp = parseExternFunc (objID, t);
//     if (t->check (';') == false) {
//         fprintf (myStderr, "JS syntax error line %d: missing ';' at end of method call\n", t->getLine ());
//         exit (1);
//     }
//     return (tmp);
// }

Code * Function::parseExternFunc (int objID, Tokenizer * t) {
    //fprintf (myStderr, "DBG: got static object %d\n", objID);
    if (t->check ('.') == false) {
        fprintf (myStderr, "%s:%d: JS syntax error: missing '.'\n", t->getFile(), t->getLine ());
        exit (1);
    }
    char * s = t->getNextToken ();
    if (s == NULL) {
        fprintf (myStderr, "%s:%d: JS syntax error: missing method after '.' \n", t->getFile(), t->getLine ());
        exit (1);
    }
    int methodID = getMethodID (objID, s);
    if (methodID < 0) {
        fprintf (myStderr, "%s:%d: JS syntax error: unknown  method %s\n", t->getFile(), t->getLine (), s);
        exit (1);
    }
    //fprintf (myStderr, "DBG: got static call %d.%d \n", objID, methodID);
    if (t->check ('(', true) == true) {
        return new Code (Code::CODE_CALL_STATIC, new Code (objID), new Code(methodID), parseParams (t));
    } else {
        return new Code (Code::CODE_CALL_STATIC, new Code (objID), new Code(methodID), NULL);
    }
}

// Code * Function::parseInternCall (char * funcName, Tokenizer * t) {
//     Code * tmp = parseInternFunc (funcName, t);
//     if (t->check (';') == false) {
//         fprintf (myStderr, "JS syntax error line %d: missing ';' at end of function call\n", t->getLine ());
//         exit (1);
//     }
//     return (tmp);
// }

Code * Function::parseInternFunc (char * funcName, Tokenizer * t) {
    //fprintf (myStderr, "DBG %d: got function name %s\n", __LINE__, funcName);
    //fprintf (myStderr, "DBG: got static call %d.%d \n", objID, methodID);
    int funcId = m_node->findFieldIdx (funcName);
    if (funcId == -1) {
        if (strcmp (funcName, "initialize") == 0) {
            funcId = INITIALIZE_ID;
        } else {
            Field * field = m_node->addField (strdup ("SFTmp"), funcName, true);
            funcId = field->m_number;
            field->m_lineNum = t->getLine ();
        }
        //fprintf (myStderr, "DBG %d: got function id %d for name %s\n", __LINE__, funcId, funcName);
    }
    Code * tmp = new Code (Code::CODE_CALL_FUNCTION, new Code (funcId), parseParams (t));
    return (tmp);
}


Code * Function::parseFor (Tokenizer * t) {
    m_blockLevel++;
    ensure ('(', "%s:%d: JS syntax error: '(' expected after 'for'\n", t);
    Code * init = parseInstr (t); // got ';' already parsed
    Code * test = parseExpr (t);
    ensure (';', "%s:%d: JS syntax error: ';' expected after test part of 'for' header\n", t);
    Code * post = parseInstr (t, false);
    ensure (')', "%s:%d: JS syntax error: ')' expected after 'for' header\n", t);
    Code * block = parseLoopBlock (t);
    m_blockLevel--;
    init->append (new Code (Code::CODE_FOR, test, block, post));
    return new Code (Code::CODE_BLOCK, init);
}

Code * Function::parseSwitch (Tokenizer * t) {
    Code * test = parseTest (t);
    if (t->check ('{', true) == false) {
        fprintf (myStderr, "%s:%d: JS syntax error 'switch' must be followed by a block eg. switch (condition) { some code }\n", t->getFile(), t->getLine());
        exit (1);
    }
    int switchLevel = m_switchLevel;
    m_switchLevel = m_blockLevel + 1;
    Code * block = parseBlock (t);
    m_switchLevel = switchLevel;
    return new Code (Code::CODE_SWITCH, test, block);
}

Code * Function::parseSwitchLabel (Tokenizer * t, char * s, int type, Code * next) {
    if (m_switchLevel != m_blockLevel) {
        fprintf (myStderr, "%s:%d: JS syntax error '%s' keyword is only allowed in 'switch' blocks.\n", t->getFile(), t->getLine(), s);
        exit (1);
    }
    if (type == Code::CODE_CASE && next == NULL) {
        fprintf (myStderr, "%s:%d: JS syntax error while parsing expression associated to '%s' within 'switch' blocks.\n", t->getFile(), t->getLine(), s);
        exit (1);
    }
    if (t->check (':') == false) {
        fprintf (myStderr, "%s:%d: JS syntax error: ':' expected after '%s' keyword\n", t->getFile(), t->getLine (), s);
        exit (1);
    }
    return new Code (type, next);
}

Code * Function::parseReturn (Tokenizer * t) {
    bool parent = t->check ('(');
    Code * tmp = parseExpr (t);
    if (parent) {
        ensure (')', "%s:%d: JS syntax error: ')' expected after 'return'\n", t);
    }
    ensure (';', "%s:%d: JS syntax error: ';' expected to end 'return'\n", t);
    return new Code (Code::CODE_RETURN, tmp);
}

Code * Function::parseVarDeclaration (Tokenizer * t, bool hasVar) {
    char * s = hasVar ? (char*)"var" : t->getNextToken ();
    if (s && strcmp (s, "var") == 0) { // var declaration
        //fprintf (myStderr, "DBG: got token VAR\n");
        char * varName = t->getNextToken ();
        if (varName == NULL) {
            fprintf (myStderr, "%s:%d: JS syntax error: variable name expected\n", t->getFile(), t->getLine());
            exit (1);
        }
        //fprintf (myStderr, "DBG: got var name %s\n", varName);
        m_vars = new Var (varName, m_blockLevel, -1, m_vars);
        Code * lvalue = new Code (Code::CODE_NEW_VAR, new Code (varName));
        if (t->check (';', true)) {
            //fprintf (myStderr, "DBG: got token ';'\n");
            return new Code (Code::CODE_ASSIGN, lvalue, NULL);
        } else if (t->check ('=')) {
            //fprintf (myStderr, "DBG: got token '='\n");
            Code * tmp = new Code (Code::CODE_ASSIGN, lvalue, parseExpr (t));
            if (t->check (';', true) == false) {
                fprintf (myStderr, "%s:%d: JS syntax error: missing ';'\n", t->getFile(), t->getLine ());
                exit (1);
            }
            return tmp;
        }
        fprintf (myStderr, "%s:%d: JS syntax error: ';' or '=' expected\n", t->getFile(), t->getLine());
        exit (1);
    }
    return (NULL);
}

Code * Function::parseInstr (Tokenizer * t, bool checkSemi) {
    // var name = expr;
    // name = expr;
    // name.field = expr;
    // Object.method (expr);
    // if (test) { instr; } [else { instr; } ]
    if (checkInside (t, ";)}")) {
        if (checkSemi) { t->check (';'); }
        return new Code (Code::CODE_NOP);
    }
    Code * tmp = NULL;
    char * s = t->getNextToken ();
    if (s) {
        int objID = getClassID (s);
        if (objID > -1) {
            tmp = parseExternFunc (objID, t);
            if (checkSemi) {
                ensure (';', "%s:%d: JS syntax error: missing ';' to end external method call\n", t);
            }
            return (tmp);
        } else if (strcmp (s, "var") == 0) { // var declaration
            tmp = parseVarDeclaration (t, true);
            if (checkSemi) {
                ensure (';', "%s:%d: JS syntax error: missing ';' to end var declaration\n", t);
            }
            return (tmp);
        } else if (strcmp (s, "if") == 0) {
            Code * tmp = new Code (Code::CODE_IF, parseTest (t), parseBlock (t));
            char * token = t->getNextToken ();
            if (token) {
                if (strcmp (token, "else") == 0) {
                    tmp->setThird (parseBlock (t));
                } else {
                    t->push (token);
                }
            }
            return (tmp);
        } else if (strcmp (s, "while") == 0) {
            tmp = new Code (Code::CODE_WHILE, parseTest (t), parseLoopBlock (t));
            return (tmp);
        } else if (strcmp (s, "for") == 0) {
            return parseFor (t);
        } else if (strcmp (s, "return") == 0) {
            return parseReturn (t);
        } else if (strcmp (s, "continue") == 0) {
            if (m_inLoop) {
                ensure (';', "%s:%d: JS syntax error: ';' expected after 'continue'\n", t);
                return new Code (Code::CODE_CONTINUE, NULL);
            }
            fprintf (myStderr, "%s:%d: JS syntax error: 'continue' is only allowed in 'while' and 'for' statements.\n", t->getFile(), t->getLine());
            exit (1);
        } else if (strcmp (s, "break") == 0) {
            if (m_inLoop || m_switchLevel > 0) {
                ensure (';', "%s:%d: JS syntax error: ';' expected after 'break'\n", t);
                return new Code (Code::CODE_BREAK, NULL);
            }
            fprintf (myStderr, "%s:%d: JS syntax error: 'break' is only allowed in 'while', 'for' and 'switch' statements.\n", t->getFile(), t->getLine());
            exit (1);
        } else if (strcmp (s, "switch") == 0 && t->check ('(', true)) {
            return parseSwitch (t);
        } else if (strcmp (s, "case") == 0) {
            return parseSwitchLabel (t, s, Code::CODE_CASE, parseExpr (t));
        } else if (strcmp (s, "default") == 0) {
            return parseSwitchLabel (t, s, Code::CODE_DEFAULT, NULL);
        } else if (t->check ('(', true)) { // function call
            Code *tmp = parseInternFunc (s, t);
            if (checkSemi) {
                ensure (';', "%s:%d: JS syntax error: missing ';' to end function call\n", t);
            }
            return (tmp);
        } else { // instruction
            
            Code * lvalue = parseLValue (t, s);
            if (lvalue != NULL && lvalue->getType() == Code::CODE_CALL_METHOD) {
                ensure (';', "%s:%d: JS syntax error: missing ';' to end instruction\n", t);
                return lvalue;
            }
            bool self = false;
            int operation = parseAssign (t, self);
            if (operation == Code::CODE_ASSIGN || self == true) {
                Code * tmp;
                if (self) {
                    Code * compute = new Code (operation, lvalue->cloneInvertAccess(), parseExpr (t));
                    tmp = new Code (Code::CODE_ASSIGN, lvalue, compute);
                } else {
                    tmp = new Code (Code::CODE_ASSIGN, lvalue, parseExpr (t));
                }
                if (checkSemi) {
                    ensure (';', "%s:%d: JS syntax error: missing ';' to end instruction\n", t);
                }
                return tmp;
            }
            int arity = 0;
            operation = parseOperation (t, arity);
            if (arity == 1) {
                Code * compute = new Code (operation, lvalue->cloneInvertAccess(), new Code ((int)1));
                Code * tmp = new Code (Code::CODE_ASSIGN, lvalue, compute);
                if (checkSemi) {
                    ensure (';', "%s:%d: JS syntax error: missing ';' to end instruction\n", t);
                }
                return (tmp);
            }
            fprintf (myStderr, "%s:%d: JS syntax error (assignement expected)\n", t->getFile(), t->getLine());
        }
    }
    fprintf (myStderr, "%s:%d: JS syntax error : unexpected expression\n", t->getFile(), t->getLine());
    exit (1);
}


Code * Function::parseLoopBlock (Tokenizer * t) {
    int inLoop = m_inLoop;
    m_inLoop = true;
    Code * tmp = parseBlock (t);
    m_inLoop = inLoop;
    return tmp;
}

Code * Function::parseBlock (Tokenizer * t) {
    //fprintf (myStderr, "DBg: start parsing bloc\n");
    if (t->check ('{') == false) {
        //fprintf (myStderr, "'{' expected after end of args line %d\n", t->getLine ());
        return parseInstr (t);
    }
    m_blockLevel++;
    Code * bodyCode = parseInstr (t);
    while (true) {
        if (t->check ('}')) {
            m_blockLevel--;
            return new Code (Code::CODE_BLOCK, bodyCode);
        }
        Code * tmp = parseInstr (t);
        if (tmp) {
            bodyCode->append (tmp);
        } else {
            break;
        }
    }
    m_blockLevel--;
    if (t->check ('}') == false) {
        fprintf (myStderr, "%s:%d: JS syntax error: '}' expected after end of block\n", t->getFile(), t->getLine ());
        return (false);
    }
    return new Code (Code::CODE_BLOCK, bodyCode);
}

Code * Function::parse (Tokenizer * t, bool verbose) {
    m_blockLevel = 0;
    char * token = t->getNextToken ();
    if (token == NULL) {
        if (t->_EOF() == false) {
            fprintf (myStderr, "%s:%d: WARNING: function expected (next char is '%c')\n", t->getFile(), t->getLine (), t->GETC());
        }
        return (NULL);
    }
    if (strcmp (token, "function") != 0) {
        fprintf (myStderr, "%s:%d: 'function' expected\n", t->getFile(), t->getLine ());
        exit (1);
    }
    char * funcName = t->getNextToken ();
    if (funcName == NULL) {
        fprintf (myStderr, "%s:%d: function name expected\n", t->getFile(), t->getLine ());
        exit (1);
    }
    m_name = funcName;
    //fprintf (myStderr, "DBG: got func name %s\n", funcName);
    if (t->check ('(') == false) {
        fprintf (myStderr, "%s:%d: '(' expected after function name\n", t->getFile(), t->getLine ());
        exit (1);
    }
    // parse args
    while (true) {
        char * argName = t->getNextToken ();
        if (argName == NULL) { // empty list
            break;
        }
        m_vars = new Var (argName, 0, -1, m_vars);
        if (t->check (',') == false) {
            break; // end of list
        }
    }
    if (t->check (')') == false) {
        fprintf (myStderr, "%s:%d: missing ',' or ')' in args declaration\n", t->getFile(), t->getLine ());
        exit (1);
    }
    m_code = parseBlock (t);
    //fprintf (myStderr, "Function '%s' parsed correctly\n", funcName);
    if (verbose) {
        printf ("function %s (", funcName);
        if (m_vars) { m_vars->print (); }
        printf (") ");
        if (m_code) {
            m_code->printAll (0);
        }
        printf ("\n");
    }
    return (m_code);
}

void setVarIndex (Var * v, ByteCode * bc) {
    if (v) {
        if (v->m_next) {
            setVarIndex (v->m_next, bc);
        }
        v->setIndex (bc->getRegister());
    }
}

void Function::setParamsIndex (ByteCode * bc) {
    setVarIndex (m_vars, bc);
    Var * v = m_vars;
    while (v) {
        //fprintf (myStderr, "Function::setParamsIndex: param %s => %d\n", v->m_name, v->m_index);
        v = v->m_next;
    }
}

// static void exchangeBytes (char * s) {
//     char t = s[0];
//     s[0] = s[3];
//     s[3] = t;
//     t = s[1];
//     s[1] = s[2];
//     s[2] = t;
// }

Scripter::Scripter (Node * node, char * buffer, FILE * fp, bool verbose, char * fn, int ln, char * filename) {
    m_node = node;
    m_tokenizer = new Tokenizer (buffer, true, fn, ln);
    int totalLen = 0;
    unsigned char totalData [64*1024];
    int len = 0;
    unsigned char * data = NULL;
    if (s_externClasses == NULL) {
        s_externClasses = new ExternClasses (externCallsDef);
    }
//     fprintf (myStderr, "---------------------------------------------------\n");
//     for (int i = 0; i < sizeof(StaticObjectNames)/sizeof(StaticObject); i++) {
//         fprintf (myStderr, "static %s {\n", StaticObjectNames[i].name);
//         for (int j = 0; j < StaticObjectNames[i].nbMethods; j++) {
//             fprintf (myStderr, "    %s\n", StaticObjectNames[i].methods[j]);
//         }
//         fprintf (myStderr, "}\n");
//     }
//     fprintf (myStderr, "---------------------------------------------------\n");

    if (m_tokenizer->checkToken ("javascript")) {
        if (!m_tokenizer->check (':')) {
            fprintf (myStderr, "%s:%d: Warning: the char ':' is expected after token 'javascript'\n", m_tokenizer->getFile(), m_tokenizer->getLine());
        }
    }
    m_maxRegisters = 0;
    Function * f = getFunction (verbose);
    while (f) {
        int index = m_node->findFieldIdx (f->getName ());
        if (index == -1) {
            if (strcmp (f->getName(), "initialize") == 0) {
                index = INITIALIZE_ID;
            } else {
                m_node->addField (strdup ("SFDefined"), f->getName (), true);
                index = m_node->findFieldIdx (f->getName ());
            }
        } else {
            Field * field = m_node->findField (f->getName());
            if (field->m_type == Field::TYPE_ID_SFTMP) {
                field->m_type = Field::TYPE_ID_SFDEFINED;
            }
        }
        if (index > 252) {
            fprintf (myStderr, "%s:%d: Scripting error: Max number of fields and methods reached in Script node ! (function %s has index > 252)\n", 
                     filename, m_tokenizer->getLine(), f->getName ());
            exit (1);
        }
        if (index >= 0) {
            //fprintf (myStderr, "$$ saving func %s with index %d\n", f->getName (), index);
            data = f->getByteCode (len);
            totalData[totalLen++] = (index+1) & 0xFF;
            unsigned char * s = (unsigned char *) &len;
            totalData[totalLen++] = s[3];
            totalData[totalLen++] = s[2];
            totalData[totalLen++] = s[1];
            totalData[totalLen++] = s[0];
            memcpy (&totalData[totalLen], data, len);
            totalLen += len;
        }
        f = getFunction (verbose);
    }
    // now check all fields for remaining SFTmp, meaning that functions have been used but not defined 
    Field * field = m_node->m_field;
    while (field != NULL) {
        if (field->m_type == Field::TYPE_ID_SFTMP) {
            fprintf (stderr, "%s:%d function called but not defined, or syntax error: %s\n", 
                     filename, field->m_lineNum, field->m_name);
            exit (1);
        }
        field = field->m_next;
    }

    //fprintf (myStderr, "Scripter: save bytecode %d bytes\n", totalLen);
//     exchangeBytes ((char *)(&totalLen));
//     fwrite (&totalLen, 1, 4, fp);
    //exchangeBytes ((char *)(&totalLen));
    //fprintf (myStderr, "Script: maximum number of registers: %d\n", m_maxRegisters); 
    fprintf (fp, "%c%c", 255, m_maxRegisters); 
    fwrite (totalData, 1, totalLen, fp);
}

Function * Scripter::getFunction (bool verbose) {

    Function * f = new Function (m_node);
    if (verbose) {
        printf ("----------------------\n");
        printf ("-- Parsing function --\n");
    }
    Code * code = f->parse (m_tokenizer, verbose);
    if (code == NULL) {
        if (verbose) {
            printf ("-- Empty function --\n");
            printf ("--------------------\n");
        }
        return NULL;
    } else {
        if (verbose) {
            printf ("-- Generating bytecode for function %s --\n", f->getName ());
            f->printVars ();
        }
        ByteCode bc;
        f->setParamsIndex (&bc);
        f->removeVars (1, &bc);
        code->generateAll (&bc, f);
        f->removeVars (1, &bc);
        int len = 0;
        unsigned char * data = bc.getCode (len);
        //fprintf (myStderr, "Scripter.getFunction: %s has max regs = %d\n", f->getName (), bc.getMaxRegisters ());
        if (bc.getMaxRegisters () > m_maxRegisters) {
            m_maxRegisters = bc.getMaxRegisters ();
        }
        if (len > 0) {
            if (verbose) ByteCode::dump (data, len);
            f->setByteCode (len, data);
        } else {
            data = NULL;
            fprintf (myStderr, "Scripter.parseFunction: no bytecode generated!\n");
        }
        if (verbose) {
            f->printVars ();
            printf ("-- Bytecode generated --\n");
            printf ("------------------------\n");
        }
    }
    return f;
}
