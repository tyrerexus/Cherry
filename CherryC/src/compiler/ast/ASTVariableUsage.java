package compiler.ast;

import compiler.CherryType;
import compiler.LangCompiler;
import compiler.lib.IndentPrinter;

/**
 * @author david
 * @date 4/12/17.
 */
public class ASTVariableUsage extends ASTNode
{

	public ASTNode declaration;


	public ASTVariableUsage(ASTParent parent, String name)
	{
		super(parent, name);
		declaration = parent.findSymbol(name);
	}

	@Override
	public CherryType getExpressionType()
	{
		return declaration.getExpressionType();
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