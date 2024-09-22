import java.lang.RuntimeException
import kotlin.math.exp

class Interpreter(private val errorReporter: ErrorReporterInterface){

    private val globals = Environment()
    private var environment = globals
    private val locals = mutableMapOf<Expression,Int>()

    init {
        globals.define("clock", object: LoxCallable{
            override fun arity(): Int {
                return 0
            }

            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
                return System.currentTimeMillis().toDouble()/1000.0
            }

            override fun toString(): String {
                return "<native fn>"
            }
        })
    }


    fun interpret(statements: List<Stmt>){
        try {
            for (s in statements){
                execute(s)
            }
        }catch (error: RunTimeError){
            errorReporter.error(error)
        }
    }

//    private fun runtimeError(error: RunTimeError){
//        println(error.message+"\n[line" + error.token.line+ "]")
//        hadRuntimeError = true
//    }
    private fun stringify(obj: Any?): String{
        if (obj == null){
            return "nil"
        }
        if (obj is Double){
            var text: String = obj.toString()
            if (text.endsWith(".0")){
                text = text.substring(0,text.length-2)
            }
            return text
        }
        return obj.toString()
    }

    private fun execute(stmt: Stmt){
        when(stmt){
            is Stmt.Expr -> evaluate(stmt.expr)
            is Stmt.Print -> {
                val value = evaluate(stmt.value)
                println(stringify(value))
            }
            is Stmt.Return -> visitReturnStmt(stmt)
            is Stmt.Var -> executeVar(stmt)
            is Stmt.Block -> visitBlockStmt(stmt)
            is Stmt.If -> visitIfStmt(stmt)
            is Stmt.While -> visitWhileStmt(stmt)
            is Stmt.Function -> visitFunctionStmt(stmt)
            is Stmt.Class -> visitClassStmt(stmt)
        }
    }

    private fun evaluate(expr: Expression): Any?{
        return when (expr) {
            is Expression.Literal -> visitLiteralExpr(expr)
            is Expression.Grouping -> visitGroupingExpr(expr)
            is Expression.Unary -> visitUnaryExpr(expr)
            is Expression.Binary -> visitBinaryExpr(expr)
            is Expression.Variable -> visitVariableExpr(expr)
            is Expression.Assign -> visitAssignExpr(expr)
            is Expression.Logical -> visitLogicalExpr(expr)
            is Expression.Set -> visitSetExpr(expr)
            is Expression.Call -> visitCallExpr(expr)
            is Expression.Get -> visitGetExpr(expr)
            is Expression.This -> visitThisExpr(expr)
            is Expression.Super -> visitSuperExpr(expr)
            else -> throw IllegalArgumentException("Unknown expression type")
        }
    }

    private fun visitReturnStmt(stmt: Stmt.Return): Any?{
        val value = stmt.value?.let { evaluate(it) }
        throw Return(value)
    }

    private fun visitBlockStmt(stmt:Stmt.Block){
        executeBlock(stmt.statements,Environment(environment))
    }

    fun executeBlock(statements: List<Stmt>,environment: Environment){
        val previous = this.environment
        try {
            this.environment = environment
            for (stmt in statements){
                execute(stmt)
            }
        }finally {
            this.environment = previous
        }
    }

    private fun visitIfStmt(stmt: Stmt.If){
        if (isTruthy(evaluate(stmt.condition))){
            execute(stmt.thenBranch)
        }
        stmt.elseBranch?.let { execute(it) }
    }

    private fun visitWhileStmt(stmt: Stmt.While){
        while (isTruthy(evaluate(stmt.condition))){
            execute(stmt.body)
        }
    }

    private fun visitFunctionStmt(stmt: Stmt.Function): Any?{
        val function = LoxFunction(stmt,environment,false)
        environment.define(stmt.name.lexeme,function)
        return null
    }

    private fun visitClassStmt(stmt: Stmt.Class){
        val superclass = stmt.superClass?.let {
            val superClass = evaluate(it)
            if (superClass !is LoxClass){
                throw RunTimeError(stmt.superClass.name,"SuperClass must be a class.")
            }
            superClass
        }
        environment.define(stmt.name.lexeme,null)
        if (stmt.superClass != null){
            environment = Environment(environment)
            environment.define("super",superclass)
        }
        val methods = mutableMapOf<String,LoxFunction>()
        for (method in stmt.methods){
            val function = LoxFunction(method,environment,method.name.lexeme.equals("init"))
            methods.put(method.name.lexeme,function)
        }
        val klass = LoxClass(stmt.name.lexeme,superclass,methods)
        superclass?.let { environment.enclosing }
        environment.assign(stmt.name,klass)
    }

    private fun executeVar(stmt: Stmt.Var){
        val value = stmt.initializer?.let { evaluate(it) }
        environment.define(stmt.name.lexeme,value)
    }

    private fun visitLiteralExpr(expr: Expression.Literal): Any?{
        return expr.value
    }

    private fun visitGroupingExpr(exp: Expression.Grouping): Any?{
        return evaluate(exp.expr)
    }

    private fun visitUnaryExpr(expr: Expression.Unary): Any {
        val right = evaluate(expr.expr)
        return when (expr.op.type) {
            TokenType.MINUS -> {
                when (right) {
                    is Double -> -right
                    else -> error("Must be a number")
                }
            }

            TokenType.NOT -> !isTruthy(right)
            else -> error("Unsupported unary expression ${expr.op.type}")
        }
    }

    private fun isTruthy(value: Any?): Boolean{
        if (value == null){
            return false
        }
        if (value is Boolean){
            return value
        }
        return true
    }

    private fun visitBinaryExpr(expr: Expression.Binary): Any?{
        fun errorNumbers(){
            throw RunTimeError(expr.op, "Operands for '${expr.op.lexeme}' must be numbers.")
        }

        val left = evaluate(expr.left)
        val right = evaluate(expr.right)
        when(expr.op.type){

            TokenType.MINUS ->{ if (left is Double && right is Double){
                return (left - right)
            }else {
                errorNumbers()
            }}
            TokenType.SLASH ->{ if (left is Double && right is Double){
                return left/right
            }else{
                errorNumbers()
            }}
            TokenType.STAR ->{ if (left is Double && right is Double){
                return left*right
            }else{
                errorNumbers()
            }}
            TokenType.PLUS -> if (left is Double && right is Double){
                return left + right
            }else{if (left is String && right is String){
                return left + right
            }}
            TokenType.GREATER ->{ if (left is Double && right is Double){
                return left>right
            }else{
                errorNumbers()
            }}
            TokenType.GREATER_EQUAL ->{ if (left is Double && right is Double){
                return left >= right
            }else{
                errorNumbers()
            }}
            TokenType.LESS ->{ if (left is Double && right is Double){
                return left<right
            }else{
                errorNumbers()
            }}
            TokenType.LESS_EQUAL ->{ if (left is Double && right is Double){
                return left<=right
            }else{
                errorNumbers()
            }}
            TokenType.NOT_EQUAL -> if (left is Double && right is Double){
                return left!=right
            }
            TokenType.EQUAL_EQUAL -> if (left is Double && right is Double){
                return left==right
            }
            else -> throw RunTimeError(expr.op, "Operands must be two numbers or two strings")
        }
        return null
    }

    private fun visitVariableExpr(expr: Expression.Variable): Any? {
        return lookUpVariable(expr.name,expr)
    }

    private fun lookUpVariable(name:Token, expr: Expression): Any? {
        val distance = locals[expr] ?: return globals.get(name)
        return environment.getAt(distance,name.lexeme)
    }

    private fun visitAssignExpr(expr: Expression.Assign):Any?{
        val value = evaluate(expr.value)
        val dist = locals[expr]
        if (dist!=null){
            environment.assignAt(dist, expr.name,value)
        }else{
            globals.assign(expr.name,value)
        }
        return value
    }

    private fun visitLogicalExpr(expr: Expression.Logical):Any?{
        val left = evaluate(expr.left)
        if (expr.op.type == TokenType.OR){
            if (isTruthy(left)){
                return left
            }
        }else{
            if (!isTruthy(left)){
                return left
            }
        }
        return evaluate(expr.right)
    }

    private fun visitSetExpr(expr: Expression.Set): Any?{
        val obj = evaluate(expr.obj)
        if (obj !is LoxInstance){
            throw RunTimeError(expr.name,"Only instances have fields.")
        }
        val value = evaluate(expr.value)
        obj.set(expr.name,value)
        return value
    }

    private fun visitThisExpr(expr: Expression.This): Any?{
        return lookUpVariable(expr.keyword,expr)
    }

    private fun visitCallExpr(expr: Expression.Call): Any?{
        val callee = evaluate(expr.callee)
        if (callee !is LoxCallable){
            throw RunTimeError(expr.paren,"can only call functions and classes")
        }
        val arguments = mutableListOf<Any>()
        for (arg in expr.arguments){
            evaluate(arg)?.let { arguments.add(it) }
        }
        val function:LoxCallable = callee
        if (arguments.size != function.arity()){
            throw RunTimeError(expr.paren,"Expected ${function.arity()} arguments but got ${arguments.size}")
        }
        return function.call(this,arguments)
    }

    private fun visitGetExpr(expr: Expression.Get): Any?{
        val obj = evaluate(expr.obj)
        if (obj is LoxInstance){
            return obj.get(expr.name)
        }
        throw RunTimeError(expr.name,"Only instances have properties.")
    }

    private fun visitSuperExpr(expr: Expression.Super): Any?{
        val distance = locals[expr]!!
        val superclass = environment.getAt(distance,"super") as LoxClass
        val obj = environment.getAt(distance -1,"this") as LoxInstance
        val method = superclass.findMethod(expr.method.lexeme)?: throw RunTimeError(expr.method,
            "Undefined property '${expr.method.lexeme}'")
        return method.bind(obj)
    }

    fun resolve(expr: Expression, depth: Int){
        locals[expr] = depth
    }

}