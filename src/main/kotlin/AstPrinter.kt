import kotlin.text.StringBuilder

class AstPrinter {
    var buffer = StringBuilder()

    fun print(expr: Expression): String {
        buffer = StringBuilder()
        traverse(expr)
        return buffer.toString()
    }

    fun print(stmt: List<Stmt>): String{
        buffer = StringBuilder()
        for(s in stmt){
            traverse(s)
            buffer.append("\n")
        }
        return buffer.toString()
    }

    private fun traverse(stmt: Stmt){
        when(stmt){
            is Stmt.Class ->{
                buffer.append("(class ${stmt.name.lexeme}")
                stmt.superClass?.let {
                    buffer.append(" < ")
                    traverse(it)
                }
                buffer.append("{")
                for (method in stmt.methods){
                    traverse(method)
                    buffer.append(" ")
                }
                buffer.append("})")
            }
            is Stmt.Block ->{
                buffer.append("{")
                for (s in stmt.statements){
                    traverse(s)
                    buffer.append(" ")
                }
                buffer.append("}")
            }
            is Stmt.If ->{
                buffer.append("(if")
                traverse(stmt.condition)
                buffer.append(" ")
                traverse(stmt.thenBranch)
                stmt.elseBranch?.let {
                    buffer.append("else")
                    traverse(it)
                }
                buffer.append(")")
            }
            is Stmt.While -> {
                buffer.append("(while")
                traverse(stmt.condition)
                buffer.append(" ")
                traverse(stmt.body)
                buffer.append(")")
            }
            is Stmt.Expr -> traverse(stmt.expr)
            is Stmt.Print -> {
                buffer.append("(print ")
                traverse(stmt.value)
                buffer.append(")")
            }
            is Stmt.Var -> {
                buffer.append("(var ${stmt.name.lexeme}")
                stmt.initializer?.let{
                    buffer.append("=")
                    traverse(it)
                }
                buffer.append(")")
            }
            is Stmt.Function -> {
                buffer.append("(fun ${stmt.name.lexeme} (")
                for (param in stmt.params){
                    buffer.append(param.lexeme)
                    if (param != stmt.params.last()){
                        buffer.append(" ")
                    }
                }
                buffer.append(") ")
                buffer.append("{")
                for (body in stmt.body){
                    traverse(body)
                    buffer.append(" ")
                }
                buffer.append("})")

            }
            is Stmt.Return -> {
                buffer.append("(return")
                stmt.value?.let {
                    buffer.append(" ")
                    traverse(it)
                }
                buffer.append(")")
            }
            else -> error("no print implemented")
        }
    }

    private fun traverse(expre: Expression){
        when(expre){
            is Expression.Assign -> {
                buffer.append("(")
                buffer.append(expre.name.lexeme)
                buffer.append("=")
                traverse(expre.value)
                buffer.append(")")
            }
            is Expression.Literal -> buffer.append(expre.value.toString())
            is Expression.Binary -> parenthesize(expre.op.lexeme, expre.left,expre.right)
            is Expression.Unary -> parenthesize(expre.op.lexeme, expre.expr)
            is Expression.Grouping -> buffer.append("group",expre.expr)
            is Expression.Logical -> parenthesize(expre.op.lexeme, expre.left, expre.right)
            is Expression.Call -> {
                buffer.append("(call ")
                traverse(expre.callee)
                for (arg in expre.arguments){
                    buffer.append(" ")
                    traverse(arg)
                }
                buffer.append(")")
            }
            is Expression.Get -> {
                buffer.append("(get")
                traverse(expre.obj)
                buffer.append(" ${expre.name.lexeme})")
            }
            is Expression.Set -> {
                buffer.append("(set")
                traverse(expre.obj)
                buffer.append(" ${expre.name.lexeme} ")
                traverse(expre.value)
                buffer.append(")")
            }
            is Expression.This -> {
                buffer.append("this")
            }
            is Expression.Super -> {
                buffer.append("(super ${expre.method.lexeme})")
            }
            else -> error("no print implemented")
        }
    }

    private fun parenthesize(name: String, vararg exprs: Expression){
        buffer.append("(").append(name)
        for(expr in exprs){
            buffer.append(" ")
            traverse(expr)
        }
        buffer.append(")")
    }
}