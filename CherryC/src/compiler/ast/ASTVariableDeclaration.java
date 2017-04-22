package compiler.ast;

import compiler.CherryType;
import compiler.lib.DebugPrinter;

/**
 * @author Tyrerexus
 * @date 4/12/17.
 */
public class ASTVariableDeclaration extends ASTParent
{
	CherryType type;

	public ASTVariableDeclaration(ASTParent parent, String name, CherryType type, ASTBase value)
	{
		super(parent, name);
		this.type = type;
		if (value != null)
			value.setParent(this);
	}

	public ASTBase getValue()
	{
		return childAsts.get(0);
	}

	@Override
	public CherryType getExpressionType()
	{
		return this.type;
	}

	@Override
	public void debugSelf(DebugPrinter destination)
	{
		destination.print("Variable: " + name +
				" of type: " + type.getName() + " with value of ");
		getValue().debugSelf(destination);

	}
}
