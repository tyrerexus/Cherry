package compiler.backends;

import compiler.CherryType;
import compiler.LangCompiler;
import compiler.Main;
import compiler.ast.*;
import compiler.lib.IndentPrinter;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Compiles an AST into a .sym file.
 *
 * @author Tyrerexus
 * @date 5/4/17.
 */
public class CompilerSYM extends LangCompiler
{
	public IndentPrinter symOutput = null;
	private PrintStream symStream = null;

	@Override
	public void compileClass(ASTClass astClass)
	{
		symOutput.println("ClassName: " + astClass.getName());
		symOutput.println("CompilerVersion: " + Main.VERSION);
		symOutput.println("ExtendsClass: " + astClass.extendsClass);
		symOutput.println();
		for (ASTBase node : astClass.childAsts)
		{
			node.compileSelf(this);
			symOutput.println();
		}
	}

	@Override
	public void compileVariableDeclaration(ASTVariableDeclaration astVariableDeclaration)
	{
		symOutput.println("Var: " +
				astVariableDeclaration.getName() + " " +
				astVariableDeclaration.getExpressionType().getTypeName());
	}

	@Override
	public void compileFunctionDeclaration(ASTFunctionDeclaration astFunctionDeclaration)
	{
		ArrayList<ASTVariableDeclaration> args = astFunctionDeclaration.args;
		for (ASTVariableDeclaration arg : args)
		{
			symOutput.println("Arg: " +
					arg.getName() + " " +
					arg.getExpressionType().getTypeName());
		}
		CherryType returnType = astFunctionDeclaration.returnType;
		symOutput.println("Fun: " +
				astFunctionDeclaration.getParent().getName() + " " +
				returnType.getTypeName());
	}

	@Override
	public void compileIf(ASTIf astIf)
	{

	}

	@Override
	public void compileLoop(ASTLoop astLoop)
	{

	}

	@Override
	public void compileFunctionCall(ASTFunctionCall astFunctionCall)
	{

	}

	@Override
	public void compileFunctionGroup(ASTFunctionGroup astFunctionGroup)
	{
		for (ASTBase node : astFunctionGroup.childAsts)
		{
			node.compileSelf(this);
		}
	}

	@Override
	public void compileVariableUsage(ASTVariableUsage astVariableUsage)
	{

	}

	@Override
	public void compileOperator(ASTOperator astOperator)
	{

	}

	@Override
	public void compileNumber(ASTNumber astNumber)
	{

	}

	@Override
	public void compileString(ASTString astString)
	{

	}

	@Override
	public void compileReturnExpression(ASTReturnExpression astReturnExpression)
	{

	}

	@Override
	public void compileMemberAccess(ASTMemberAccess astMemberAccess)
	{

	}

	@Override
	public void createFileStreams(String fileName)
	{
		try
		{
			symStream = new PrintStream("out/" + fileName + ".sym");
			symOutput = new IndentPrinter(symStream);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

	}

	@Override
	public void closeStreams()
	{
		symStream.flush();
		symStream.close();
	}
}
