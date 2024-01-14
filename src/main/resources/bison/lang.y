%{
#include <stdio.h>
#include <stdlib.h>
#define YYDEBUG 1
#include "lang.tab.h"
#define YYSTYPE int
%}

%token CEVA
%token AMU_IA
%token ATATA_CAT
%token II
%token NU_I
%token PUNE
%token IA
%token TERMINA

%token LE
%token GE
%token NE
%token FWD
%token AWD

%token IDENTIFIER

%token <p_val> CONST_NUMBER
%token <p_val> CONST_STRING
%token <p_val> CONST_CHAR

%start program

%%

program:          stmtlist
                ;
declaration:      CEVA IDENTIFIER ';'
                ;
assignstmt:       IDENTIFIER AWD expression
                ;
iostmt:           PUNE FWD expression ';'
                | IA FWD IDENTIFIER
                ;
simplstmt:        assignstmt ';'
                | iostmt
                | declaration
                ;
ifstmt:           II '(' condition ')' '{' stmt stmtlist '}' NU_I '{' stmtlist '}'
                ;
whilestmt:        ATATA_CAT '(' condition ')' '{' stmt stmtlist '}'
                ;
forstmt:          AMU_IA '(' assignstmt ';' condition ';' assignstmt ')' '{' stmt stmtlist '}'
                ;
stmtlist:           /* empty */
                | stmt
                | stmt stmtlist
                ;
stmt:             structstmt
                | simplstmt
                | assignstmt
                ;
structstmt:       ifstmt
                | whilestmt
                | forstmt
                ;
condition:        expression relation expression
                ;
expression:       term expression1
                ;
expression1:        /* empty */
                | first_order_op term expression1
                ;
first_order_op:   '+'
                | '-'
                ;
second_order_op:  '/'
                | '*'
                | '%'
                ;
term:             factor term1
                ;
term1:              /* empty */
                | second_order_op factor term1
                ;
factor:           expression
                | IDENTIFIER
                | CONST_NUMBER
                ;
relation:         '<'
                | LE
                | '='
                | GE
                | '>'
                | NE
                ;

%%

yyerror(char *s)
{
  printf("%s\n", s);
}

extern FILE *yyin;

main(int argc, char **argv)
{
  if(argc>1) yyin = fopen(argv[1], "r");
  if((argc>2)&&(!strcmp(argv[2],"-d"))) yydebug = 1;
  if(!yyparse()) fprintf(stderr,"\tO.K.\n");
}

