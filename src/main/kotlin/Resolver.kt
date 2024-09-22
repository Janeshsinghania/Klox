import java.util.Stack
import kotlin.math.exp

class Resolver(
    private val interpreter: Interpreter,
    private val errorReporter: ErrorReporterInterface
) {
    enum class FunctionType{
        NONE,
        CLASS,
        FUNCTION,
        INITIALIZER,
        SUBCLASS,
        METHOD
    }
    private val scopes = Stack<MutableMap<String,Boolean>>()
    private var currentFunction = FunctionType.NONE
    private var currentClass = FunctionType.NONE
    fun resolve(statements: List<Stmt>){
        for (statement in statements){
            resolve(statement)
        }
    }

    fun resolve(stmt: Stmt){
        when(stmt){
            is Stmt.Block -> visitBlockStmt(stmt)
            is Stmt.Var -> visitVarStmt(stmt)
            is Stmt.Function -> visitFunctionStmt(stmt)
            is Stmt.Expr -> visitExprStmt(stmt)
            is Stmt.If -> visitIfStmt(stmt)
            is Stmt.Print -> resolve(stmt.value)
            is Stmt.Return -> visitReturnStmt(stmt)
            is Stmt.While -> visitWhileStmt(stmt)
            is Stmt.Class -> visitClassStmt(stmt)
        }.let{ }
    }

    fun resolve(expr: Expression){
        when(expr){
            is Expression.Variable -> visitVarExpr(expr)
            is Expression.Assign -> visitAssignExpr(expr)
            is Expression.Binary -> visitBinaryExpr(expr)
            is Expression.Call -> visitCallExpr(expr)
            is Expression.Grouping -> resolve(expr.expr)
            is Expression.Literal -> {}
            is Expression.Logical -> visitLogicalExpr(expr)
            is Expression.Set -> visitSetExpr(expr)
            is Expression.Super -> visitSuperExpr(expr)
            is Expression.This -> visitThisExpr(expr)
            is Expression.Unary -> resolve(expr.expr)
            is Expression.Get -> visitGetExpr(expr)
        }
    }

    private fun resolveFunction(stmt: Stmt.Function, type: FunctionType){
        val enclosingFunction = currentFunction
        currentFunction = type
        beginScope()
        for (param in stmt.params){
            declare(param)
            define(param)
        }
        resolve(stmt.body)
        endScope()
        currentFunction = enclosingFunction
    }

    private fun visitBlockStmt(stmt: Stmt.Block){
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    private fun visitVarStmt(stmt: Stmt.Var){
        declare(stmt.name)
        if (stmt.initializer!=null){
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    private fun visitFunctionStmt(stmt: Stmt.Function){
        declare(stmt.name)
        define(stmt.name)
        resolveFunction(stmt,FunctionType.FUNCTION)
    }

    private fun visitExprStmt(stmt: Stmt.Expr){
        resolve(stmt.expr)
    }

    private fun visitIfStmt(stmt: Stmt.If){
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        stmt.elseBranch?.let { resolve(it) }
    }

    private fun visitReturnStmt(stmt: Stmt.Return){
        if (currentFunction == FunctionType.NONE){
            errorReporter.error(stmt.keyword, "Can not return from Top level code.")
        }
        stmt.value?.let {
            if (currentFunction == FunctionType.INITIALIZER){
                errorReporter.error(stmt.keyword,"Can't return a value from an initializer.")
            }
            resolve(it)
        }
    }

    private fun visitWhileStmt(stmt: Stmt.While){
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    private fun visitClassStmt(stmt: Stmt.Class){
        val enclosingClass = currentClass
        currentClass = FunctionType.CLASS
        declare(stmt.name)
        define(stmt.name)
        if (stmt.superClass!=null && stmt.name.lexeme.equals(stmt.superClass.name.lexeme)){
            errorReporter.error(stmt.superClass.name,"A class cannot inherit from itself.")
        }
        if (stmt.superClass!=null){
            currentClass = FunctionType.SUBCLASS
            resolve(stmt.superClass)
        }
        stmt.superClass?.let {
            beginScope()
            scopes.peek().put("super",true)
        }
        beginScope()
        scopes.peek().put("this",true)

        for (method in stmt.methods){
            var declaration = FunctionType.METHOD
            if (method.name.lexeme.equals("init")){
                declaration = FunctionType.INITIALIZER
            }
            resolveFunction(method,declaration)
        }
        endScope()
        stmt.superClass?.let { endScope() }
        currentClass = enclosingClass
    }

    private fun visitVarExpr(expr: Expression.Variable){
         if (!scopes.isEmpty() && scopes.peek()[expr.name.lexeme]==false){
             errorReporter.error(expr.name,"Can't read local variable in its own initializer")
         }
        resolveLocal(expr,expr.name)
    }

    private fun visitAssignExpr(expr: Expression.Assign){
        resolve(expr.value)
        resolveLocal(expr,expr.name)
    }

    private fun visitBinaryExpr(expr: Expression.Binary){
        resolve(expr.left)
        resolve(expr.right)
    }

    private fun visitCallExpr(expr: Expression.Call){
        resolve(expr.callee)
        for (arg in expr.arguments){
            resolve(arg)
        }
    }

    private fun visitGetExpr(expr: Expression.Get){
        resolve(expr.obj)
    }

    private fun visitLogicalExpr(expr: Expression.Logical){
        resolve(expr.left)
        resolve(expr.right)
    }

    private fun visitSetExpr(expr: Expression.Set){
        resolve(expr.value)
        resolve(expr.obj)
    }

    private fun visitThisExpr(expr: Expression.This){
        if (currentClass == FunctionType.NONE){
            errorReporter.error(expr.keyword,"Can't use 'this' outside of a class.")
            return
        }
        resolveLocal(expr,expr.keyword)
    }

    private fun visitSuperExpr(expr: Expression.Super){
        if (currentClass == FunctionType.NONE){
            errorReporter.error(expr.keyword,"Can't use 'super' outside of a class.")
        }else if (currentClass != FunctionType.SUBCLASS){
            errorReporter.error(expr.keyword,"Can't use 'super' in a class with no superclass.")
        }
        resolveLocal(expr,expr.keyword)
    }

    private fun declare(name: Token){
        if (scopes.isEmpty()){
            return
        }
        val scope = scopes.peek()
        if (scope.containsKey(name.lexeme)){
            errorReporter.error(name,"Already a variable with this name in this scope.")
            return
        }
        scope[name.lexeme] = false
    }

    private fun define(name: Token){
        if (scopes.isEmpty()){
            return
        }
        scopes.peek()[name.lexeme] = true
    }

    private fun  resolveLocal(expr: Expression,name: Token){
        for (i in scopes.indices.reversed()){
            if (scopes[i].containsKey(name.lexeme)){
                interpreter.resolve(expr, scopes.size - 1 - i)
            }
        }
    }

    private fun beginScope(){
        scopes.push(mutableMapOf())
    }

    private fun endScope(){
        scopes.pop()
    }
}