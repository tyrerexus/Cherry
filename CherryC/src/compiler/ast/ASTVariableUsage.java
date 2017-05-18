package compiler.ast;

import compiler.CherryType;
import compiler.LangCompiler;
import compiler.lib.IndentPrinter;

/**
 * @author david
 * @date 4/12/17.
 */
public class ASTVariableUsage extends ASTBase implements ASTPath
{
	/**
	 * The path to the declaration.
	 */
	//public ASTBase declaration;

	public ASTBase getDeclaration()
	{
		if (name.equals("super"))
			return this.getContainingClass().extendsClassAST;
		else
			return getParent().findSymbol(name);
	}

	public ASTVariableUsage(ASTParent parent, String name)
	{
		super(parent, name);
	}

	@Override
	public CherryType getExpressionType()
	{
		return getDeclaration().getExpressionType();
	}

	@Override
	public void debugSelf(IndentPrinter destination)
	{
		destination.print(name);
	}

	@Override
	public void compileSelf(LangCompiler compiler)
	{
		compiler.compileVariableUsage(this);
	}
}
