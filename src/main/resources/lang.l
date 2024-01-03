%{
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
int lines = 1;
%}

%option noyywrap

WS           [ \t\n]
DIGIT        [0-9]
LETTER       [A-Za-z]
LETTER_SMALL [a-z]
IDENTIFIER   ({LETTER_SMALL})+
NUMBER       [1-9][0-9]*|0
STRING       \"[^\"]*\"
CHAR         [\']([a-zA-Z0-9])[\']

%%

"ceva"|"amu_ia"|"atata_cat"|"ii"|"nu_i"|"pune"|"ia"|"termina"    {printf("Reserved word: %s\n", yytext);}


";"|"("|")"|"{"|"}"  	{printf( "Separator: %s\n", yytext );}

"+"|"-"|"*"|"/"|"<"|"%"|">"|"<="|">="|"!="|"="|"->"|"<-"	{printf( "Operator: %s\n", yytext );}


{IDENTIFIER}         { printf("Identifier: %s\n", yytext); }

{NUMBER}		{printf("Number: %s\n", yytext);}
{STRING}		{printf("String: %s\n", yytext);}
{CHAR}		{printf("Character: %s\n", yytext);}

[ \t]+	{}

[\r\n]+	{++lines;}


[a-zA-Z0-9_]+	{printf("Illegal identifier at line %d\n", lines); return -1;}

[+|-]?[0][0-9]*([.][0-9]*)?		{printf("Illegal numeric constant at line %d\n", lines);}

[\'][a-zA-Z0-9 ]{2,}[\']|[\'][a-zA-Z0-9 ][a-zA-Z0-9 ][\']		{printf("Illegal character constant at line %d\n", lines);}


.	{printf("Illegal symbol at line %d\n", lines); return -1;}

%%

void main(argc, argv)
int argc;
char** argv;
{
if (argc > 1)
{
    FILE *file;
    file = fopen(argv[1], "r");
    if (!file)
    {
        fprintf(stderr, "Could not open %s\n", argv[1]);
        exit(1);
    }
    yyin = file;
}
yylex();
}