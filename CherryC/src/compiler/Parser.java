package compiler;

import compiler.ast.*;
import compiler.builtins.Builtins;

import static compiler.Token.TokenType;


/**
 * This class uses a Lexer to build an AST.
 *
 * @author Tyrerexus
 * @date 4/11/17.
 */
public class Parser
{
	Lexer lexer;
	Token lookAhead;
	Token previous = null;

	public Parser(Lexer lexer)
	{
		this.lexer = lexer;
		lookAhead = lexer.getToken();
	}

	// FIXME: Not implemented yet.
	private ASTBase parseExpression(ASTParent parent)
	{
		if (match(TokenType.NUMBER) || match(TokenType.SYMBOL))
		{
			ASTBase left = null;
			if (previous.tokenType == TokenType.NUMBER)
			{
				left = new ASTNumber(parent, Integer.parseInt(previous.value));
			}
			else
			{
				left = new ASTVariableUsage(parent, previous.value);
			}
			if (match(TokenType.OPERATOR))
			{
				ASTFunctionCall operatorCall = new ASTFunctionCall(parent, previous.value);
				operatorCall.infix = true;
				ASTBase right = parseExpression(parent);

				left.setParent(operatorCall);
				right.setParent(operatorCall);

				return operatorCall;
			}
			else if (match(TokenType.NEWLINE) || match(TokenType.EOF) || lookAhead.tokenType == TokenType.RPAR)
			{
				return left;
			}



		}
		else if (match(TokenType.LPAR))
		{

			ASTBase expression =  parseExpression(parent);
			if (match(TokenType.RPAR))
			{
				return expression;
			} else
			{
				error(")", "Unmatched parenthesis");
			}
		}


		return null;
	}

	private CherryType findType(ASTParent perspective, String name)
	{
		ASTBase f = perspective.findSymbol(name);
		if (f instanceof ASTClass)
			return (ASTClass) f;

		CherryType type = Builtins.getBuiltin(name);
		if (type != null)
			return type;

		return null;
	}

	private CherryType parseType(ASTParent parent)
	{
		if (match(Syntax.OPERATOR_BLOCKSTART))
		{
			if (match(TokenType.SYMBOL))
			{
				return findType(parent, previous.value);
			}
		}
		return null;
	}

	private boolean parseLine(ASTClass dest)
	{


		// Extract indents to get a parent. //
		int line_indent = 0;
		if (match(TokenType.INDENT))
		{
			line_indent = previous.indent;
		}

		// Skip any empty lines.
		if (match(TokenType.NEWLINE))
		{
			return true;
		}

		// Use the indent to find a new parent for the contents of this line. //
		ASTParent parent = dest.getParentForNewCode(line_indent);

		// Check if we are defining a variable. //
		if (match(Syntax.KEYWORD_VAR))
		{
			if (match(TokenType.SYMBOL))
			{
				String name = previous.value;

				// Parse ": Cat". //
				CherryType definedType = parseType(parent);

				// Will be set to the initial value of this variable. //
				ASTBase value = null;

				// Parse the initial value. //
				if (match("="))
				{
					value = parseExpression(parent);
					CherryType valueType = value.getExpressionType();
					if (definedType == null)
						definedType = valueType;
					else if (definedType != valueType)
						System.err.print("ERROR: Type miss-match at line: " + previous.lineNumber);
				}

				ASTVariableDeclaration variable = new ASTVariableDeclaration(parent, name, definedType, value);
				variable.columnNumber = line_indent;
				return variable != null;
			}
			else
			{
				error("symbol", "Syntax: var <VARIABLE NAME> [ = <INITIAL VALUE>]");
				return false;
			}
		}
		else if (match(Syntax.KEYWORD_FUN))
		{
			if (match(TokenType.SYMBOL))
			{
				String name = previous.value;
				ASTFunctionDeclaration function = new ASTFunctionDeclaration(parent, Builtins.getBuiltin("void"));

				// Parse args. //
				if(match(TokenType.LPAR))
				{
					// If we aren't directly followed by a closing parentheses. //
					if (!match(TokenType.RPAR))
					{

						// Match as many arguments as possible. //
						do
						{
							if (match(TokenType.SYMBOL))
							{
								String argName = previous.value;
								CherryType argType = parseType(parent);
								// TODO: Support default value.
								function.args.add(new ASTVariableDeclaration(null, argName, argType, null));
							}
						} while (match(","));

						// Check for matching parentheses. //
						if (!match (TokenType.RPAR))
						{
							error(")", "Unmatched parenthesis");
							return false;
						}
					}
				}

				// Parse return type. //
				if (match(Syntax.OPERATOR_RETURNTYPE))
				{
					if (match(TokenType.SYMBOL))
					{
						function.returnType = findType(parent, previous.value);
					}
					else
					{
						error("return type", "Return type is required after \"->\"");
						return false;
					}
				}

				ASTVariableDeclaration functionAsVariable = // FIXME: Change type to lambda or function
						new ASTVariableDeclaration(parent, name, Builtins.getBuiltin("fun"), function);
				functionAsVariable.columnNumber = line_indent;
				return functionAsVariable != null && function != null;

			}
			else
			{
				error("name", "Expected a name for the function.");
				return false;
			}
		}
		else
		{
			ASTBase expression = parseExpression(parent);
			if (expression != null)
				expression.columnNumber = line_indent;
			return expression != null;
		}
	}

	/**
	 * Parses the whole content of the Lexer
	 *
	 * It does this by calling parseLine as many times as possible.
	 */
	public void parseFile(ASTClass dest)
	{

		// Parse as many lines as possible. //
		while (parseLine(dest));

		// Check for garbage. //
		if (!match(TokenType.EOF))
		{
			error("end of file", "There is un-parsed junk at the end of the file. ");
		}

	}

	private boolean match(String value)
	{
		if (value.equals(lookAhead.value))
		{
			previous = lookAhead;
			lookAhead = lexer.getToken();
			return true;
		}

		return false;
	}

	private boolean match(Token.TokenType value)
	{
		if (value == lookAhead.tokenType)
		{
			previous = lookAhead;
			lookAhead = lexer.getToken();
			return true;
		}

		return false;
	}

	private void error(String expected, String message)
	{
		System.err.println("[Cherry]: Error in file: " + lexer.fileName + "\t at line " + previous.lineNumber + ".");
		System.err.println("\tExpected:\t\t" + expected);
		System.err.println("\tActual:\t\t\t" + previous.value);
		System.err.println("\tMessage: " + (message.equals("") ? "[NONE]" : message));
	}

}
