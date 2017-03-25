#include "ASTWithArgs.hpp"
#include "../ClassCompile.hpp"

void ASTWithArgs::insertArg(ASTBase *arg)
{
	this->arg_nodes.push_back(arg);
	arg->parent_node = this;
}

void ASTWithArgs::debugSelf()
{
	std::cout << '(';	
	for (auto arg : this->arg_nodes) {
		arg->debugSelf();
		if (arg != arg_nodes.back()) {
			std::cout << ", ";
		}
	}
	std::cout << ')';
}

void ASTWithArgs::exportSymToStream(std::ostream& output)
{
	output << "";
	return;
}

bool ASTWithArgs::compileToBackend(ClassCompile *compile_dest)
{

	compile_dest->output_stream << '(';	
	for (ASTBase* arg : arg_nodes) {
		if (!arg->compileToBackend(compile_dest))
			return false;
		if (arg != arg_nodes.back()) {
			compile_dest->output_stream << ", ";
		}
	}
	compile_dest->output_stream << ')';
	return true;
}