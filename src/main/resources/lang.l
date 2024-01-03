%{
#include <stdio.h>

int lineno = 1;  // Line number counter
%}

OP           ">"|"<"|"<="|"="|">="|"\\+"|"-"|"\\*"|"/"|"%"|"<-"|"->"|"!="
SEP          ";"|"\\{"|"\\}"|"\\("|"\\)"
WS           [ \t\n]
DIGIT        [0-9]
LETTER       [A-Za-z]
LETTER_SMALL [a-z]
IDENTIFIER   ({LETTER_SMALL})({LETTER}|{DIGIT})*
NUMBER       ({DIGIT})+
STRING       "\\"([^\"\\\\]|\\\\.)*\\""
CHAR         "\\'([^'\\\\]|\\\\.)\\'"

%%

{OP}         { printf("Operator: %s\n", yytext); }

{SEP}        { printf("Separator: %s\n", yytext); }

"ceva"       { printf("Reserved Word: ceva\n"); }
"amu_ia"     { printf("Reserved Word: amu_ia\n"); }
"atata_cat"  { printf("Reserved Word: atata_cat\n"); }
"ii"         { printf("Reserved Word: ii\n"); }
"nu_i"       { printf("Reserved Word: nu_i\n"); }
"pune"       { printf("Reserved Word: pune\n"); }
"ia"         { printf("Reserved Word: ia\n"); }
"termina"    { printf("Reserved Word: termina\n"); }

{STRING}     { printf("String constant: %s\n", yytext); }

{CHAR}       { printf("Character constant: %s\n", yytext); }

{IDENTIFIER} { printf("Identifier: %s\n", yytext); }

{NUMBER}     { printf("Number: %s\n", yytext); }

\n           { ++lineno; }
{WS}         ;

.            { printf("Unknown character: %s on line %d\n", yytext, lineno); }

%%

int yywrap() {
    return 1;
}

int main() {
    printf("Enter your code:\n");
    yylex();
    return 0;
}
