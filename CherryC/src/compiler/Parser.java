package compiler;

import compiler.ast.*;
import compiler.builtins.Builtins;
import compiler.builtins.FileType;

import static compiler.Token.TokenType;


/**
 * This class uses a Lexer to build an AST.
 *
 * @author Tyrerexus
 * @date 4/11/17.
 */
@SuppressWarnings("StatementWithEmptyBody")
public class Parser
{

	/** The lexer to read from. */
	private Lexer lexer;


	private Token[] lookAheads = new Token[3];
	private Token previous = null;

	public boolean fileTypeDeclared = false;

	public Syntax syntax;

	/**
	 * Creates a Parser that will read from a lexer.
	 * @param lexer The lexer to read from.
	 */
	public Parser(Lexer lexer)
	{
		this.lexer = lexer;
		for (int i = 0; i < lookAheads.length; i++)
		{
			lookAheads[i] = lexer.getToken();
		}
		syntax = new Syntax();
	}

	private boolean isFundamental(TokenType tokenType)
	{
		return tokenType == TokenType.SYMBOL || tokenType == TokenType.NUMBER || tokenType == TokenType.STRING;
	}

	private ASTBase parseFundamental(ASTParent parent)
	{
		if (match(TokenType.SYMBOL))
			return new ASTVariableUsage(parent, previous.value);
		if (match(TokenType.NUMBER))
			return new ASTNumber(parent, Integer.parseInt(previous.value));
		if (match(TokenType.STRING))
			return null; // FIXME: Not implemented yet!

		System.err.println("COMPILER ERROR! Trying to parse fundamental type on non-fundamental!");
		return null;
	}

	private ASTBase parseFundamentalWithOperator(ASTParent parent)
	{
		ASTBase left = parseFundamental(parent);
		if (match(TokenType.OPERATOR))
		{
			String opName = previous.value;
			ASTBase right = parseFundamentalWithOperator(parent);
			return new ASTOperator(parent, opName, right, left);
		}
		else
		{
			return left;
		}
	}

	private ASTBase parseExpression(ASTParent parent)
	{
		return parseExpression(parent, false);
	}

	// FIXME: Add support for strings.
	private ASTBase parseExpression(ASTParent parent, boolean inPar)
	{
		if (isFundamental(lookAheads[0].tokenType))
		{
			ASTBase left;

			// Create left-hand side. //
			// From number. //
			if (match(TokenType.NUMBER, inPar))
			{
				left = new ASTNumber(parent, Integer.parseInt(previous.value));
			}
			// Or from a symbol... //
			else if (match(TokenType.SYMBOL, inPar))
			{
				String name = previous.value;

				// Check if function call. //
				// FIXME: Replace with Syntax.somethingDesu!
				if (match("!"))
				{
					ASTFunctionCall functionCall = new ASTFunctionCall(parent, name);

					// Parse arguments until we find something un-parsable. //
					while(true)
					{
						if (isFundamental(lookAheads[0].tokenType))
							parseFundamentalWithOperator(functionCall);
						else if (lookAheads[0].tokenType == TokenType.LPAR)
							parseExpression(functionCall);
						else
							break;

					}

					return functionCall;
				}
				// Nope, just normal symbol. //
				else
				{
					left = new ASTVariableUsage(parent, name);
				}
			}
			// A compiler error has occurred. //
			else
			{
				System.err.println("COMPILER ERROR! Fundamental type used is not supported!");
				return null;
			}

			while (inPar && match("\n"));


			// Check if we have hit an end. //
			if (lookAheads[0].value.equals(",") || lookAheads[0].value.equals(":"))
			{
				return left;
			}

			// Either we have an operator, or alternatively left is single-node expression or a function call. //
			else if (match(TokenType.OPERATOR, inPar))
			{
				String opName = previous.value;
				while (inPar && match("\n", true));

				ASTBase right = parseExpression(parent, inPar);

				return new ASTOperator(parent, opName, right, left);
			}



			// Single-node expression. //
			else if ((mathEOLF() && !inPar)
					|| lookAheads[0].tokenType == TokenType.RPAR
					|| Syntax.isKeyword(lookAheads[0].value))
			{
				return left;
			}
			else
			{
				syntaxError("end of expression", "Got garbage!");
			}
		}

		// Try to parse parentheses. //
		else if (match(TokenType.LPAR, inPar))
		{
			while (match("\n"));
			ASTBase expression =  parseExpression(parent, true);
			if (match(TokenType.RPAR, true))
			{
				return expression;
			}
			else
			{
				syntaxError(")", "Unmatched parenthesis");
			}
		}

		return null;
	}

	/**
	 * Mainly used by parseFunctionDeclaration() and parseVariableDeclaration().
	 * @param perspective From what perspective to search from.
	 * @return A type.
	 */
	private CherryType parseType(ASTParent perspective)
	{
		return parseType(perspective, false);
	}

	private CherryType parseType(ASTParent perspective, boolean isFunctionType)
	{
		if (match(Syntax.Op.TYPEDEF) || isFunctionType)
		{
			if (match(TokenType.SYMBOL))
			{
				return findType(perspective, previous.value);
			}
		}
		return null;
	}

	private ASTVariableDeclaration parseFunctionDeclaration(ASTParent parent)
	{

		// Functions always start with a name as their identifier. //
		if (match(TokenType.SYMBOL))
		{
			String name = previous.value;
			ASTVariableDeclaration variableDeclaration =
					new ASTVariableDeclaration(parent, name, Builtins.getBuiltin("function"), null);
			ASTFunctionDeclaration function =
					new ASTFunctionDeclaration(variableDeclaration, Builtins.getBuiltin("void"));

			// Check that we specify the return type of the function (and the parameters). //
			if (match(Syntax.Op.TYPEDEF))
			{
				// Make sure we match a parenthesis which is basically the function indicator. //
				if (match(TokenType.LPAR))
				{
					do
					{
						if (match(TokenType.SYMBOL))
						{
							String argName = previous.value;
							CherryType argType = parseType(parent);
							function.args.add(new ASTVariableDeclaration(null, argName, argType, null));
						}
					} while (match(","));

					if (!match(TokenType.RPAR))
					{
						syntaxError(")", "Unmatched parenthesis");
					}

					if (lookAheads[0].tokenType == TokenType.SYMBOL)
					{
						function.returnType = parseType(parent, true);
					}
					else
					{
						function.returnType = Builtins.getBuiltin("void");
					}
					if (lookAheads[0].value.equals(Syntax.Op.FUNCVAL))
					{
						ASTReturnExpression call = parseReturnExpression(parent);
						if (call != null)
						{
							call.setParent(function);
						}
					}
					System.err.println("Debug: " + function.returnType);
					return variableDeclaration;
				}
				else
				{
					syntaxError("(", "All function type declarations need to start with a parenthesis");
					return null;
				}
			}
			else
			{
				syntaxError(":", "You need to specify a type for the function, void functions use () as their type ");
				return null;
			}
		}
		else
		{
			System.err.println("COMPILER ERROR! Trying to parse function declaration from non-symbol!");
			return null;
		}
	}

	private ASTReturnExpression parseReturnExpression(ASTParent parent)
	{
		if (match(Syntax.Op.FUNCVAL))
		{
			ASTReturnExpression returnExpression = new ASTReturnExpression(parent);

			// Filter out any newlines. //
			while (match(TokenType.NEWLINE));

			ASTBase right = parseExpression(parent);
			if (right != null)
				right.setParent(returnExpression);

			return returnExpression;
		}
		else
		{
			syntaxError(Syntax.Op.FUNCVAL, "COMPILER ERROR!!!");
			return null;
		}
	}

	private ASTVariableDeclaration parseVariableDeclaration(ASTParent parent)
	{
		if (match(TokenType.SYMBOL))
		{
			String name = previous.value;
			if (lookAheads[0].value.equals(Syntax.Op.TYPEDEF))
			{
				CherryType cherryType = parseType(parent);
				ASTBase value = null;


				// Try to parse initial value. //
				if (match("="))
				{
					value = parseExpression(parent);

					if (value == null)
						return null;
					else if (cherryType == null)
						cherryType = value.getExpressionType();
						// Check that the types match. //
					else if (cherryType != value.getExpressionType() && value.getExpressionType() != null)
						error("ERROR: Type miss-match at line: " + previous.lineNumber);
				}

				return new ASTVariableDeclaration(parent, name, cherryType, value);
			}
			else
			{
				return null;
			}
		}
		else
		{
			syntaxError("name", "Names are required for variable declaration.");
			return null;
		}
	}


	private boolean parseFileTypeDeclarationLine(ASTParent parent)
	{

		// Skip indentation . //
		if (lookAheads[1].value.equals(Syntax.Keyword.TYPE))
			match(TokenType.INDENT);

		// Skip any empty lines.
		if (match(TokenType.NEWLINE))
		{
			return true;
		}

		if (match(Syntax.Keyword.TYPE))
		{
			if (match(TokenType.SYMBOL))
			{
				if (FileType.toFileType(previous.value) != FileType.UNDEFINED)
				{
					new ASTFileTypeDeclaration(parent, FileType.toFileType(previous.value));
					return true;
				}
				else
				{
					syntaxError("filetype", previous.value, "The file type provided is not recognized");
				}
			}
			else
			{
				syntaxError("file type", "A file type is required for file type declaration.");
			}
		}

		return false;
	}

	// FIXME: Loops do not work
	private ASTLoop parseLoop(ASTParent parent)
	{
		if (match(Syntax.Keyword.LOOP))
		{
			ASTLoop loop = new ASTLoop(parent);
			if (lookAheads[1].value.equals(Syntax.Op.TYPEDEF)
					&& lookAheads[2].tokenType != TokenType.NEWLINE)
				loop.initialStatement = parseVariableDeclaration(loop);
			else
				loop.initialStatement = parseExpression(loop);

			if (match(","))
			{
				loop.conditionalStatement = parseExpression(loop);
				if (match(","))
				{
					loop.iterationalStatement = parseExpression(loop);
				}
			}

			// TODO: Add as syntax.
			else if (match("as"))
			{
				System.out.println("Not implemented yet");
			}
			else
			{
				ASTBase until = loop.initialStatement;
				if (until.getExpressionType() != Builtins.getBuiltin("int"))
				{
					syntaxError("int", "Can only loop without index with type \"int\".");
					return null;
				}
				final String counterName = "__c_counter";
				loop.initialStatement = new ASTVariableDeclaration(loop, counterName, Builtins.getBuiltin("int"), until);

				loop.conditionalStatement = new ASTOperator(loop, ">",
						new ASTVariableUsage(parent, counterName),
						new ASTNumber(parent, 0));


				loop.iterationalStatement = new ASTOperator(loop, "--",
						null,
						new ASTVariableUsage(parent, counterName));
			}


			return loop;
		}
		else
		{
			System.err.println("COMPILER ERROR! Trying to create loop from non-loop keyword");
			return null;
		}
	}


	private ASTSubclassExpression parseExtendDeclaration(ASTParent parent)
	{
		if (match(Syntax.Keyword.EXTENDS))
		{
			if (match(TokenType.SYMBOL))
			{
				return new ASTSubclassExpression(parent, previous.value);
			}
			else
			{
				unexpectedExpressionError(lookAheads[0].value, "Invalid name for class/object.");
			}
		}
		else
		{
			System.err.println("COMPILER ERROR! There was no subclass expression!");
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




		if (parent == null)
		{
			error("Incorrect line indentation at: " + lexer.getLineNumber() + "\n Tabbing: " + line_indent);
			return false;
		}

		// Check if we are extending a class. //
		if (lookAheads[0].value.equals(Syntax.Keyword.EXTENDS))
		{
			ASTSubclassExpression subclassExpression = parseExtendDeclaration(parent);
			if (subclassExpression != null)
			{
				subclassExpression.columnNumber = line_indent;
				return true;
			}
			return false;
		}
		// Check if we are defining a function. //
		else if (lookAheads[0].tokenType == TokenType.SYMBOL
				&& lookAheads[1].value.equals(Syntax.Op.TYPEDEF)
				&& lookAheads[2].tokenType == TokenType.LPAR)
		{
			ASTVariableDeclaration function = parseFunctionDeclaration(parent);
			if (function != null)
			{
				function.columnNumber = line_indent;
				return true;
			}
			return false;
		}
		// FIXME: If/ELSE does not work!
		else if (match(Syntax.Keyword.IF))
		{
			ASTBase condition = parseExpression(parent);

			new ASTIf(parent, condition);
			return true;



		}

		// FIXME: If/ELSE does not work! Or does it?
		else if (match(Syntax.Keyword.ELSE))
		{
			new ASTElse(parent);
			return true;
		}

		else if (lookAheads[0].value.equals(Syntax.Keyword.LOOP))
		{
			ASTLoop loop = parseLoop(parent);
			if (loop != null)
			{
				loop.columnNumber = line_indent;
				return true;
			}
			return false;
		}

		// Try to parse as a variable declaration. //

		else if (lookAheads[0].tokenType == TokenType.SYMBOL
				&& lookAheads[1].value.equals(Syntax.Op.TYPEDEF))
		{
			ASTVariableDeclaration declaration = parseVariableDeclaration(parent);
			if (declaration != null)
			{
				declaration.columnNumber = line_indent;
				return true;
			}
			return false;
		}

		// Check if it contains the keyword "type" to see if we can see what type it is. //
		else if (match(Syntax.Keyword.TYPE))
		{
			// Error. //
			unexpectedExpressionError(previous.value, "File type has already been declared, was not expecting another declaration");
			return false;
		}

		// Otherwise it's just an expression. //
		else
		{
			ASTBase expression = parseExpression(parent);
			if (expression != null)
			{
				expression.columnNumber = line_indent;
				return true;
			}
			return false;
		}
	}

	/**
	 * Parses the whole content of the Lexer
	 *
	 * It does this by calling parseLine as many times as possible.
	 */
	public void parseFile(ASTClass dest)
	{
		previous = new Token("", TokenType.UNKNOWN, 0, 1);

		// Begin by parsing file type. //
		while (parseFileTypeDeclarationLine(dest));

		// Parse as many lines as possible. //
		while (parseLine(dest));

		// Check for garbage. //
		if (!match(TokenType.EOF))
		{
			syntaxError("end of file", "There is un-parsed junk at the end of the file. ");
		}

	}

	private CherryType findType(ASTParent perspective, String name)
	{
		// FIXME: Is this really the best place?

		ASTBase f = perspective.findSymbol(name);
		if (f instanceof ASTClass)
			return (ASTClass) f;

		CherryType type = Builtins.getBuiltin(name);
		if (type != null)
			return type;

		return null;
	}

	private void step()
	{
		previous = lookAheads[0];
		for (int i = 0; i < lookAheads.length - 1; i++)
		{
			lookAheads[i] = lookAheads[i+1];
		}

		lookAheads[lookAheads.length - 1] = lexer.getToken();
	}

	/**
	 * Matches end of line and and of file.
	 * @return If we matched.
	 */
	private boolean mathEOLF()
	{
		return match(TokenType.EOF) || match (TokenType.NEWLINE);
	}

	private boolean match(String value)
	{
		return match(value, false);
	}

	private boolean match(String value, boolean ignoreNewline)
	{
		while (ignoreNewline && (match(TokenType.NEWLINE) || match(TokenType.INDENT)));

		if (value.equals(lookAheads[0].value))
		{
			step();
			return true;
		}

		return false;
	}

	private boolean match(TokenType value, boolean ignoreNewline)
	{
		while (ignoreNewline && (match(TokenType.NEWLINE) || match(TokenType.INDENT)));
		if (value == lookAheads[0].tokenType)
		{
			step();
			return true;
		}

		return false;
	}

	private boolean match(Token.TokenType value)
	{
		return match(value, false);
	}

	private void syntaxError(String expected, String message)
	{
		syntaxError(expected, lookAheads[0].value, message);
	}

	private void syntaxError(String expected, String actual, String message)
	{
		System.err.println("[Raven]: Syntax Error in file: " + lexer.fileName + "\tat line " + previous.lineNumber + ".");
		System.err.println("\tExpected:\t\t" + expected);
		System.err.println("\tActual:\t\t\t" + actual);
		System.err.println("\tMessage:\t\t" + (message.equals("") ? "[NONE]" : message));
	}

	/*
	private void unexpectedExpressionError(String message)
	{
		unexpectedExpressionError(lookAheads[0].value, message);
	}
	*/

	private void unexpectedExpressionError(String expression, String message)
	{
		System.err.println("[Raven]: Unexpected Expression Error in file: " + lexer.fileName + "\tat line " + previous.lineNumber + ".");
		System.err.println("Expression:\t\t" + expression);
		System.err.println("Message:\t\t" + (message.equals("") ? "[NONE]" : message));
	}

	private void error(String message)
	{
		System.err.println("[Raven]: Error in file: " + lexer.fileName + "\tat line " + previous.lineNumber + ".");
		System.err.println("Message:\t\t" + (message.equals("") ? "[NONE]" : message));
	}

}
