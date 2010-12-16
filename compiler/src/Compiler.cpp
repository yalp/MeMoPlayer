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
# include "unistd.h"
# include "fcntl.h"

# include "Tokenizer.h"
# include "Utils.h"
# include "FontManager.h"
# include "Types.h"
# include "Code.h"
# include "LocaleManager.h"


# define VERSION "1.4.4"

extern void exchangeBytes (char * s);
extern void write (FILE * fp, int f);
extern int lastIndexOf (char * s, char c);
extern void dumpMethods (char * s);

FILE * myStderr = NULL;

char * execPath = (char *)".";

bool endsWith (const char * t, const char * e) {
     int l1 = strlen (t);
     int l2 = strlen (e);
     if (l1 < l2) {
	  return (false);
     }
     while (l2 > 0) {
	  if (t[--l1] != e[--l2]) {
	       return (false);
	  }
     }
     return (true);
}

bool startsWith (const char * t, const char * e) {
    return (strncmp (t, e, strlen(e)) == 0);
}

void dumpTable (char * o) {
    fprintf (myStderr, "dumpTable %s\n", o);
    Scene scene ((char *)"NoName");
    FILE * out = fopen (o, "wb");
    scene.dumpTable (out);
    fclose (out);
}

void compile (char * inName, char * dir, bool verbose) {
    char * orgName = inName;

    MultiPathFile::addPath (inName);
    int cd = open (".", O_RDONLY);
    int i = lastIndexOf (inName, '/');
    if (i >= 0) {
        inName [i] = '\0';
        chdir (inName);
        //MultiPathFile::addPath (inName);
        inName += i+1;
    }

    char * binName = strdup (inName);
    strcpy (binName+strlen(binName)-3, "m4m");
    printf ("compile %s -> %s/%s\n", orgName, dir, binName);

    Scene scene (binName, verbose);

    FILE * in = fopen (inName, "rb");
    if (in == NULL) {
        fprintf (myStderr, "cannot open %s", inName);
        exit (1);
    }
    printf (">> Compiling scene %s\n", inName);
    scene.parse (inName, in, verbose);

    fchdir (cd);

    char * outName = (char *)malloc (strlen (binName)+strlen(dir)+2);
    sprintf (outName, "%s/%s", dir, binName);
    FILE * out = fopen (outName, "wb");
    if (out == NULL) {
        fprintf (myStderr, "Cannot open %s for writing, please check if path exists\n", outName);
        exit (1);
    }
    int total = scene.encode (out, verbose);
    total += FontManager::dumpAll (execPath, out);
    write (out, 0xFFFF); // final end of file
    printf ("<< End of scene %s [%d B]\n", inName, total);

    fclose (in);
    fclose (out);

    LocaleManager::checkMissingTranslations();
}

void usage (char * exeName) {
    printf ("usage: %s [-v] [-o dir] file.wrl : compile in.wrl and save in dir/file.m4m. By default dir is '.'\n", exeName);
    printf ("examples:\n");
    printf ("       ./compiler index.wrl # compile index.wrl and store the result in ./index.m4m\n");
    printf ("       ./compiler -o res index.wrl # compile index.wrl and store the result in ./res/index.m4m\n");
    printf ("usage: %s -d file.java : dump Node table in a java file (as a class with a collection of final static)\n", exeName);
    printf ("usage: %s -b file.java : dump ByteCode table in a java file (as a class with a collection of final static)\n", exeName);
    printf ("usage: %s -a file.java : dump instance methods in a java file (as a class with a collection of final static)\n", exeName);
    exit (1);
}

void setExePath (char * path) {
    execPath = strdup (path);
    for (int i = strlen (path)-1; i >= 0; i--) {
        if (execPath[i] == '/') {
            execPath[i] = 0;
            break;
        }
    }
    //fprintf (stderr, "execPath: %s\n", execPath);
}


int main (int argc, char * argv []) {
    myStderr = stderr;
    printf ("MeMo Compiler v%s\n", VERSION);
    setExePath (argv[0]);
    if (argc < 2) {  usage (argv[0]); }
    if (strcmp (argv[1], "-d") ==0) {
        dumpTable (argv[2]);
        return (0);
    }
    if (strcmp (argv[1], "-b") ==0) {
        ByteCode::generate (argv[2]);
        return (0);
    }
    if (strcmp (argv[1], "-a") ==0) {
        dumpMethods (argv[2]);
        return (0);
    }
    bool verbose = false;
    int start = 1;
    char * dir = (char *)".";
    char * file = NULL;
    while (start < argc) {
        if (strcmp (argv[start], "-v") == 0) {
            verbose = true;
        } else if (strcmp (argv[start], "-s") == 0) {
            myStderr = stdout;
        } else if (strcmp (argv[start], "-o") == 0) {
            dir = strdup (argv[start+1]);
            start++;
        } else if (strcmp (argv[start], "-I") == 0) {
            MultiPathFile::addMultiplePaths (argv[start+1]);
            start++;
        } else {
            file = argv [start];
        }
        start++;
    }
    if (file != NULL && endsWith (file, "wrl")) {
        compile (file, dir, verbose);
    } else {
        usage (argv[0]);
    }
    return (0);
}
