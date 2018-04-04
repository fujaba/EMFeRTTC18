package de.tudresden.inf.st.mquat.jastadd.scanner;

import de.tudresden.inf.st.mquat.jastadd.parser.MquatParser.Terminals;

%%

// define the signature for the generated scanner
%public
%final
%class MquatScanner
%extends beaver.Scanner

// the interface between the scanner and the parser is the nextToken() method
%type beaver.Symbol
%function nextToken
%yylexthrow beaver.Scanner.Exception

// store line and column information in the tokens
%line
%column

// this code will be inlined in the body of the generated scanner class
%{
  private beaver.Symbol sym(short id) {
    return new beaver.Symbol(id, yyline + 1, yycolumn + 1, yylength(), yytext());
  }
%}

WhiteSpace = [ ] | \t | \f | \n | \r | \r\n
Identifier = [:jletter:][:jletterdigit:]*
Unit       = "[" [^\]]* "]"

Integer = [:digit:]+ // | "+" [:digit:]+ | "-" [:digit:]+
Real    = [:digit:]+ "." [:digit:]* | "." [:digit:]+

Comment = "//" [^\n\r]+

%%

// discard whitespace information and comments
{WhiteSpace}  { }
{Comment}     { }

// token definitions

"type"        { return sym(Terminals.TYPE); }
"meta"        { return sym(Terminals.META); }
"of type"     { return sym(Terminals.OFTYPE); }
"resources"   { return sym(Terminals.RESOURCE); }
"resource"    { return sym(Terminals.RESOURCE); }
"request"     { return sym(Terminals.REQUEST); }
"request"     { return sym(Terminals.REQUEST); }
// TODO there should be a maximize too
"minimize"    { return sym(Terminals.MINIMIZE); }
"container"   { return sym(Terminals.CONTAINER); }
//"static"      { return sym(Terminals.STATIC); }
//"runtime"     { return sym(Terminals.RUNTIME); }
//"derived"     { return sym(Terminals.DERIVED); }
"using"       { return sym(Terminals.USING); }
"property"    { return sym(Terminals.PROPERTY); }
"requires"    { return sym(Terminals.REQUIRES); }
//"provides"    { return sym(Terminals.PROVIDES); }
"requiring"   { return sym(Terminals.REQUIRING); }
"providing"   { return sym(Terminals.PROVIDING); }
"contract"    { return sym(Terminals.CONTRACT); }
"component"   { return sym(Terminals.COMPONENT); }
"with"        { return sym(Terminals.WITH); }
//"mode"        { return sym(Terminals.MODE); }
//"from"        { return sym(Terminals.FROM); }
//"to"          { return sym(Terminals.TO); }
//"parameter"   { return sym(Terminals.PARAMETER); }
"solution"    { return sym(Terminals.SOLUTION); }
"->"          { return sym(Terminals.RIGHT_ARROW); }
","           { return sym(Terminals.COMMA); }
"."           { return sym(Terminals.DOT); }
"<"           { return sym(Terminals.LE); }
"<="          { return sym(Terminals.LT); }
"="           { return sym(Terminals.EQ); }
"!="          { return sym(Terminals.NE); }
">"           { return sym(Terminals.GT); }
">="          { return sym(Terminals.GE); }
"+"           { return sym(Terminals.PLUS); }
"*"           { return sym(Terminals.MULT); }
"-"           { return sym(Terminals.MINUS); }
"/"           { return sym(Terminals.DIV); }
"^"           { return sym(Terminals.POW); }
//"sin"         { return sym(Terminals.SIN); }
//"cos"         { return sym(Terminals.COS); }
"("           { return sym(Terminals.LB_ROUND); } // was LP
")"           { return sym(Terminals.RB_ROUND); } // was RP
"{"           { return sym(Terminals.LB_CURLY); } // was LB
"}"           { return sym(Terminals.RB_CURLY); } // was RB
//"["           { return sym(Terminals.LB_SQUARE); } // was LBRACKET
//"]"           { return sym(Terminals.RB_SQUARE); } // was RBRACKET
":"           { return sym(Terminals.COLON); }
{Identifier}  { return sym(Terminals.NAME); }
{Real}        { return sym(Terminals.REAL); }
{Integer}     { return sym(Terminals.INTEGER); }
{Unit}        { return sym(Terminals.UNIT); }
<<EOF>>       { return sym(Terminals.EOF); }
