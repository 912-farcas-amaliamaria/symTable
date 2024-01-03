%{
int lines = 0;
%}

%option noyywrap

WS           [ \t\n]
DIGIT        [0-9]
LETTER       [A-Za-z]
LETTER_SMALL [a-z]
ID           ({LETTER_SMALL})+
NUMBER       [1-9][0-9]*|0
STRING       ["]([a-zA-Z0-9])*["]
CHAR         [']([a-zA-Z0-9])[']

%%

"ceva"       { printf("Reserved Word: ceva\n"); }
"amu_ia"     { printf("Reserved Word: amu_ia\n"); }
"atata_cat"  { printf("Reserved Word: atata_cat\n"); }
"ii"         { printf("Reserved Word: ii\n"); }
"nu_i"       { printf("Reserved Word: nu_i\n"); }
"pune"       { printf("Reserved Word: pune\n"); }
"ia"         { printf("Reserved Word: ia\n"); }
"termina"    { printf("Reserved Word: termina\n"); }


";" 	{printf( "Separator: %s\n", yytext ); return SEMI_COLON;}
"(" 	{printf( "Separator: %s\n", yytext ); return LEFT_ROUND_PARENTHESIS;}
")"	    {printf( "Separator: %s\n", yytext ); return RIGHT_ROUND_PARENTHESIS;}
"{"	    {printf( "Separator: %s\n", yytext ); return LEFT_CURLY_PARENTHESIS;}
"}"  	{printf( "Separator: %s\n", yytext ); return RIGHT_CURLY_PARENTHESIS;}

"+"  	{printf( "Operator: %s\n", yytext ); return PLUS;}
"-" 	{printf( "Operator: %s\n", yytext ); return MINUS;}
"*"	    {printf( "Operator: %s\n", yytext ); return MULTIPLY;}
"/"	    {printf( "Operator: %s\n", yytext ); return DIVISION;}
"<"  	{printf( "Operator: %s\n", yytext ); return LESS_THAN;}
"%" 	{printf( "Operator: %s\n", yytext ); return LESS_THAN;}
">"	    {printf( "Operator: %s\n", yytext ); return MOD;}
"<="	{printf( "Operator: %s\n", yytext ); return LESS_OR_EQUAL_THAN;}
">="	{printf( "Operator: %s\n", yytext ); return GREATER_OR_EQUAL_THAN;}
"!="	{printf( "Operator: %s\n", yytext ); return DIFFERENT;}
"=" 	{printf( "Operator: %s\n", yytext ); return EQUAL;}
"->"	{printf( "Operator: %s\n", yytext ); return ASSIGNMENT;}
"<-"	{printf( "Operator: %s\n", yytext ); return PRINT;}


{ID}         { printf("Identifier: %s\n", yytext); }

[ \t]+	/* elimina spatii */  	{}

[\n]+	{++lines;}

[a-zA-Z][a-zA-Z0-9]{8,}	{printf("Illegal size of the identifier at line %d\n", lines); return -1;}

[a-zA-Z0-9_]+	{printf("Illegal identifier at line %d\n", lines); return -1;}


.	{printf("Illegal symbol at line\n"); return -1;}

%%