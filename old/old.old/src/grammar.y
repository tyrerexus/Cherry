%{
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "ast.h"

int yyerror(char *str);
int yylex(void);

int line_indent = 0;

%}

%union{
	int tok_int_val;
	char *tok_as_string;

	struct ASTNode *tok_ast_node;

}

%token <tok_int_val> TOKEN_INT_LITERAL
%token <tok_as_string> TOKEN_STRING
%token <tok_as_string> TOKEN_SYMBOL;
%token <tok_as_string> TOKEN_INLINE;

%token TOKEN_NEWLINE
%token TOKEN_COLON
%token TOKEN_ASSIGN
%token TOKEN_INDENT
%token TOKEN_COMMA

%type <tok_int_val> t_num_exp
%type <tok_ast_node> t_any_exp
%type <tok_ast_node> t_var_def
%type <tok_ast_node> t_var_defs
%type <tok_ast_node> t_func_return_type
%type <tok_ast_node> t_func_call
%type <tok_ast_node> t_func_def
%type <tok_ast_node> t_call_args
%type <tok_ast_node> t_tuple_member
%type <tok_ast_node> t_tuple_members

/*
 * The order of these statements matter because they tell us in what order
 * to evluate & reduce tokens.
 */


%nonassoc TOKEN_FUNCTION
%nonassoc TOKEN_VARIABLE
%nonassoc TOKEN_ARROW
%nonassoc TOKEN_COLON


%nonassoc TOKEN_LPAREN TOKEN_RPAREN



%right TOKEN_ASSIGN
%nonassoc TOKEN_COMPARISON


%left TOKEN_MINUS TOKEN_PLUS
/* TODO: Add modulus. */
%left TOKEN_MULTIPLY TOKEN_DIVIDE

%nonassoc TOKEN_SYMBOL
%right TOKEN_ACCESS
%nonassoc TOKEN_COMMA

/* The program starts in the proram state. */
%start program

%%

/* A program consists of lines. */
program:		%empty
				| line program
				;

/* Each line could either be an expression, inline code or a block usage. */
line:			TOKEN_NEWLINE {extern int line_indent; line_indent = 0;}
				| t_func_def TOKEN_COLON TOKEN_NEWLINE {ast_auto_insert_node($1);}
				/*| t_func_call TOKEN_NEWLINE {ast_auto_insert_node($1);}*/
				| TOKEN_VARIABLE t_var_def TOKEN_NEWLINE {ast_auto_insert_node($2);}
				| t_any_exp TOKEN_NEWLINE {ast_auto_insert_node($1);}
				| TOKEN_INLINE {ast_auto_insert_node(ast_make_inline($1)); free($1);}
				;

/* Variables could either be defined by just type. But also with an inital value. */
t_var_def:		TOKEN_SYMBOL TOKEN_SYMBOL {
					$$ = ast_make_var_def($2, $1);
					free($1);
					free($2);
				}
				| TOKEN_SYMBOL TOKEN_SYMBOL TOKEN_ASSIGN t_any_exp {
					$$ = ast_make_var_def($2, $1);
					ast_insert_arg($$, $4);

					free($1);
					free($2);
				}
				;

/* Multiple variable definitions. */
t_var_defs:		t_var_def {$$ = $1;}
				| t_var_def TOKEN_COMMA t_var_defs {
					$$ = $1;
					$$->args_next = $3;
				}
				;

/* A tuple member could have an assigned value. */
/* TODO: Maybe we should rename this to return_arg? */
t_tuple_member:	%empty {$$ = ast_make_type_specifier("void");}
				| TOKEN_SYMBOL {$$ = ast_make_type_specifier($1); free($1);}
				| TOKEN_SYMBOL TOKEN_ASSIGN t_any_exp {
					$$ = ast_make_type_specifier($1);
					ast_insert_arg($$, $3);
					free($1);
				}
				;

/*
 * Defines tuples members.
 * Note that parentheses are not included.
 */
t_tuple_members:
				t_tuple_member {$$ = $1;}
 				| t_tuple_member TOKEN_COMMA t_tuple_members {
					$$ = $1;
					$$->args_next = $3;
				}
				;

/* Can either be emty. */
t_func_return_type:

				/* A function with not specified return type returns void. */
				%empty {$$ = ast_make_tuple(ast_make_type_specifier("void"));}

				/* Here the return type is specified. */
				| TOKEN_ARROW t_tuple_members {
					$$ = ast_make_tuple($2);
				}
				;

/* A function has a name, arguments, and a return type. */
t_func_def:		TOKEN_FUNCTION TOKEN_SYMBOL[name] t_func_return_type[return_type] {
					$$ = ast_make_func_def($name, $return_type, ast_make_tuple(NULL));
					free($2);
				}
				| TOKEN_FUNCTION TOKEN_SYMBOL[name] TOKEN_LPAREN t_var_defs[args] TOKEN_RPAREN t_func_return_type[return_type] {
					$$ = ast_make_func_def($name, $return_type, ast_make_tuple($args));
					free($2);
				}
				;

/* Tries to connect one arg with another. A function could have no arguments. */
t_call_args:	%empty {$$ = ast_make_default_arg();}
				| t_any_exp {$$ = $1;}
				| t_call_args TOKEN_COMMA t_any_exp {$1->args_next = $3;}
				;

/*
 * A basic function call.
 * A function can be called with only one argument and paranthesesless.
 * Or with multiple arguments using parantheses.
 */
t_func_call:	t_any_exp[name] TOKEN_LPAREN t_call_args[args] TOKEN_RPAREN {
					printf("L&R paren style!\n");
	   				$$ = ast_make_func_call($name, $args);}
				| t_any_exp[name] t_any_exp[args] {
					printf("Paranthesesless call!\n");
					$$ = ast_make_func_call($name, $args);}
	   			;

/* Firstly, we simplify any expression only containing numbers. */
t_num_exp:		TOKEN_INT_LITERAL { $$ = $1;}
				| t_num_exp TOKEN_PLUS t_num_exp { $$ = $1 + $3;}
				| t_num_exp TOKEN_MINUS t_num_exp { $$ = $1 - $3;}
				| t_num_exp TOKEN_MULTIPLY t_num_exp { $$ = $1 * $3;}
				| t_num_exp TOKEN_DIVIDE t_num_exp { $$ = $1 / $3;}
				| TOKEN_LPAREN t_num_exp TOKEN_RPAREN {$$ = $2;}
				;

/* t_any_exp is any expression. Strings, numbers, function calls, etc */
t_any_exp:		TOKEN_SYMBOL {$$ = ast_make_symbol($1); free($1);}
				| t_func_call {$$ = $1;}
				| TOKEN_STRING {
					$1[strlen($1) - 1] = 0;
					$$ = ast_make_string($1+1);
					free($1);
				}
				| t_num_exp {$$ = ast_make_number($1);}
				| t_any_exp TOKEN_PLUS t_any_exp {$$ = ast_make_op("+", $1, $3);}
				| t_any_exp TOKEN_MINUS t_any_exp {$$ = ast_make_op("-", $1, $3);}
				| t_any_exp TOKEN_MULTIPLY t_any_exp {$$ = ast_make_op("*", $1, $3);}
				| t_any_exp TOKEN_DIVIDE t_any_exp {$$ = ast_make_op("/", $1, $3);}
				| t_any_exp TOKEN_ASSIGN t_any_exp {$$ = ast_make_op("=", $1, $3);}
				| t_any_exp TOKEN_COMPARISON t_any_exp {$$ = ast_make_op("==", $1, $3);}
				| t_any_exp TOKEN_ACCESS t_any_exp {$$ = ast_make_op(".", $1, $3);}
				| TOKEN_LPAREN t_any_exp TOKEN_RPAREN {$$ = $2;}
				;

%%

int yyerror(char *str)
{

	extern int yylineno;
	extern char *yytext;

	printf("ERROR on line: %d: %s at sym \"%s\"\n", yylineno, str, yytext);
	fflush(stdin);
	extern void exit(int);
	exit(1);
}
