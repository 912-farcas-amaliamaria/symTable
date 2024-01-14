%{
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "lang.tab.h"
%}

%option noyywrap

WS           [ \t\n]
DIGIT        [0-9]
LETTER_SMALL [a-z]
IDENTIFIER   ({LETTER_SMALL})+
NUMBER       [1-9][0-9]*|0
STRING       \"[^\"]*\"
CHAR         [\']([a-zA-Z0-9])[\']
DELIMIT_1	 [;]
OPERATOR_1 	 [+*/()<>=]|"-"


%%

{WS}

"ceva"      {return CEVA;}
"amu_ia"    {return AMU_IA;}
"atata_cat" {return ATATA_CAT;}
"ii"        {return II;}
"nu_i"      {return NU_I;}
"pune"      {return PUNE;}
"ia"        {return IA;}
"termina"   {return TERMINA;}


"<="        {return LE;}
">="        {return GE;}
"!="        {return NE;}
"->"        {return FWD;}
"<-"        {return AWD;}


{IDENTIFIER}    {return IDENTIFIER;}

{NUMBER}	{yylval.p_val = yytext; return CONST_NUMBER;}
{STRING}	{yylval.p_val = yytext; return CONST_STRING;}
{CHAR}		{yylval.p_val = yytext; return CONST_CHAR;}

{DELIMIT_1}	{return yytext[0];}
{OPERATOR_1}	{return yytext[0];}

%%