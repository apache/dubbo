package com.alibaba.dubbo.common.json;
%%

%{
private StringBuffer sb;
%}

%table
%unicode
%state STR1,STR2

%yylexthrow ParseException

HEX = [a-fA-F0-9]
HEX4 = {HEX}{HEX}{HEX}{HEX}

IDENT = [a-zA-Z_$] [a-zA-Z0-9_$]*
INT_LITERAL = [-]? [0-9]+
FLOAT_LITERAL = {INT_LITERAL} ( ( \.[0-9]+ ) ? ( [eE][-+]? [0-9]+ )? )

ESC1 = [^\"\\]
ESC2 = [^\'\\]

SKIP = [ \t\r\n]
OTHERS = .
%%

<STR1>{
	\"				{ yybegin(YYINITIAL); return new JSONToken(JSONToken.STRING, sb.toString()); }
	{ESC1}+			{ sb.append(yytext()); }
	\\\"			{ sb.append('"'); }
}

<STR2>{
	\'				{ yybegin(YYINITIAL); return new JSONToken(JSONToken.STRING, sb.toString()); }
	{ESC2}+			{ sb.append(yytext()); }
	\\\'			{ sb.append('\''); }
}

<STR1,STR2>{
	\\\\			{ sb.append('\\'); }
	\\\/			{ sb.append('/'); }
	\\b				{ sb.append('\b'); }
	\\f				{ sb.append('\f'); }
	\\n				{ sb.append('\n'); }
	\\r				{ sb.append('\r'); }
	\\t				{ sb.append('\t'); }
	\\u{HEX4}		{ try{ sb.append((char)Integer.parseInt(yytext().substring(2),16)); }catch(Exception e){ throw new ParseException(e.getMessage()); } }
}

<YYINITIAL>{
	\"					{ sb = new StringBuffer(); yybegin(STR1); }
	\'					{ sb = new StringBuffer(); yybegin(STR2); }
	{INT_LITERAL}		{ Long val = Long.valueOf(yytext()); return new JSONToken(JSONToken.INT, val); }
	{FLOAT_LITERAL}		{ Double val = Double.valueOf(yytext()); return new JSONToken(JSONToken.FLOAT, val); }
	"true"|"TRUE"		{ return new JSONToken(JSONToken.BOOL, Boolean.TRUE); }
	"false"|"FALSE"		{ return new JSONToken(JSONToken.BOOL, Boolean.FALSE); }
	"null"|"NULL"		{ return new JSONToken(JSONToken.NULL, null); }
	{IDENT}				{ return new JSONToken(JSONToken.IDENT, yytext()); }
	"{"					{ return new JSONToken(JSONToken.LBRACE); }
	"}"					{ return new JSONToken(JSONToken.RBRACE); }
	"["					{ return new JSONToken(JSONToken.LSQUARE); }
	"]"					{ return new JSONToken(JSONToken.RSQUARE); }
	","					{ return new JSONToken(JSONToken.COMMA); }
	":"					{ return new JSONToken(JSONToken.COLON); }
	{SKIP}+ 			{}
	{OTHERS} 			{ throw new ParseException("Unexpected char [" + yytext() +"]"); }
}
