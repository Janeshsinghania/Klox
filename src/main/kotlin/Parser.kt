import java.lang.RuntimeException
import java.util.ArrayDeque


class Parser(
    private val tokens: List<Token>, private val errorReporter: ErrorReporterInterface
) {
    private var current: Int = 0

    private class ParseError : RuntimeException()

    fun parse(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }
        return statements
    }

    private fun declaration(): Stmt? {
        return try {
            when{
                match(TokenType.CLASS) -> classDeclaration()
                match(TokenType.FUN) -> function("function")
                match(TokenType.VAR) -> varDeclaration()
                else -> statement()
            }
        } catch (error: ParseError) {
            synchronize()
            null
        }
    }

    private fun classDeclaration(): Stmt.Class{
        val name = consume(TokenType.IDENTIFIER,"Expect class name.")
        val superClass = if (match(TokenType.LESS)){
            consume(TokenType.IDENTIFIER,"Expect superClass name.")
            Expression.Variable(previous())
        }else{
            null
        }
        consume(TokenType.LEFT_BRACE,"Expect '{' before class body.")
        val methods = mutableListOf<Stmt.Function>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()){
            methods.add(function("method"))
        }
        consume(TokenType.RIGHT_BRACE,"Expect '}' after class body.")
        return Stmt.Class(name,superClass,methods)
    }

    private fun function(kind: String): Stmt.Function{
        val name = consume(TokenType.IDENTIFIER,"Expect $kind name.")
        consume(TokenType.LEFT_PAREN, "Expect '(' after $kind name")
        val parameters = mutableListOf<Token>()
        if (!check(TokenType.RIGHT_PAREN)){
            do {
                if (parameters.size >= 255){
                    error(peek(),"Can't have more than 255 parameters")
                }
                parameters.add(consume(TokenType.IDENTIFIER,"Expect parameter name."))
            }while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN,"Expect ')' after parameters.")
        consume(TokenType.LEFT_BRACE,"Expect '{' before $kind body")
        val body = block()
        return Stmt.Function(name,parameters,body)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name")
        val initializer = if (match(TokenType.EQUAL)) expression() else null
        consume(TokenType.SEMICOLON, "expect ';' after variable declaration")
        return Stmt.Var(name, initializer)
    }


    private fun statement(): Stmt {
        if (match(TokenType.PRINT)) {
            return printStatement()
        }
        if (match(TokenType.IF)){
            return ifStatement()
        }
        if (match(TokenType.FOR)){
            return forStatement()
        }
        if (match(TokenType.WHILE)){
            return whileStatement()
        }
        if (match(TokenType.LEFT_BRACE)){
            return Stmt.Block(block())
        }
        if (match(TokenType.RETURN)){
            return returnStatement()
        }
        return expressionStatement()
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun ifStatement(): Stmt{
        consume(TokenType.LEFT_PAREN,"Expect '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN,"Expect ')' after if condition")
        val thenBranch = statement()
        val elseBranch = if(match(TokenType.ELSE)) statement() else null
        return Stmt.If(condition,thenBranch, elseBranch)
    }

    private fun forStatement(): Stmt{
        consume(TokenType.LEFT_PAREN, "Expect '(' after for")
        val initializer = when{
            match(TokenType.SEMICOLON) ->null
            match(TokenType.VAR) -> varDeclaration()
            else -> expressionStatement()
        }
        val condition = if (!check(TokenType.SEMICOLON)) expression() else Expression.Literal(true)
        consume(TokenType.SEMICOLON,"Expect ';' after loop condition")
        val increment = if (!check(TokenType.SEMICOLON)) expression() else null
        consume(TokenType.SEMICOLON, "Expect ')' after for clauses")
        var body = statement()
        increment?.let { body = Stmt.Block(listOf(body,Stmt.Expr(it))) }
        body = Stmt.While(condition,body)
        initializer?.let { body = Stmt.Block(listOf(it,body)) }
        return body
    }

    private fun whileStatement(): Stmt{
        consume(TokenType.LEFT_PAREN, "Expect '(' after while.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after while condition")
        val body = statement()
        return Stmt.While(condition,body)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after value.")
        return Stmt.Expr(expr)
    }

    private fun block():List<Stmt>{
        val statements = mutableListOf<Stmt>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()){
            declaration()?.let { statements.add(it) }
        }
        consume(TokenType.RIGHT_BRACE,"Expected '}' after block.")
        return statements
    }

    private fun returnStatement():Stmt{
        val keyword = previous()
        val value = if (!check(TokenType.SEMICOLON)) expression() else null
        consume(TokenType.SEMICOLON,"Expect ';' after return value.")
        return Stmt.Return(keyword,value)
    }

    private fun expression(): Expression {
        return assignment()
    }

    private fun assignment():Expression{
        val expr = or()
        if(match(TokenType.EQUAL)){
            val equals = previous()
            val value = assignment()
            when(expr){
                is Expression.Variable ->{
                    val name = expr.name
                    return Expression.Assign(name, value)
                }
                is Expression.Get ->{
                    return Expression.Set(expr.obj,expr.name,value)
                }
                else -> error(equals,"Invalid Assignment target")
            }
            error(equals,"Invalid Assignment target")
        }
        return expr
    }

    private fun or(): Expression{
        var expr = and()
        if (match(TokenType.OR)){
            val operator = previous()
            val right = and()
            expr = Expression.Logical(expr,operator,right)
        }
        return expr
    }

    private fun and(): Expression{
        var expr = equality()
        if (match(TokenType.AND)){
            val operator = previous()
            val right = equality()
            expr = Expression.Logical(expr,operator,right)
        }
        return expr
    }


    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) {
            return false
        }
        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) {
            current++
        }
        return previous()
    }

    private fun isAtEnd(): Boolean {
        return peek().type == TokenType.EOF
    }

    private fun peek() = tokens[current]

    private fun previous() = tokens[current - 1]

    private fun equality(): Expression {
        var expr = comparison()
        while (match(TokenType.NOT_EQUAL, TokenType.EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Expression.Binary(expr, operator, right)
        }
        return expr
    }

    private fun comparison(): Expression {
        var expr = term()
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            val operator = previous()
            val right = term()
            expr = Expression.Binary(expr, operator, right)
        }
        return expr
    }

    private fun term(): Expression {
        var expr = factor()
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Expression.Binary(expr, operator, right)
        }
        return expr
    }

    private fun factor(): Expression {
        var expr = unary()
        while (match(TokenType.SLASH, TokenType.STAR)) {
            val operator = previous()
            val right = unary()
            expr = Expression.Binary(expr, operator, right)
        }
        return expr
    }

    private fun unary(): Expression {
        if (match(TokenType.NOT, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            return Expression.Unary(operator, right)
        }
        return call()
    }

    private fun call(): Expression{
        var expr = primary()
        while (true){
            expr = if (match(TokenType.LEFT_PAREN)){
                finishCall(expr)
            }
            else if (match(TokenType.DOT)){
                val name = consume(TokenType.IDENTIFIER,"Expect property name after '.'.")
                Expression.Get(expr,name)
            } else{
                break
            }
        }
        return expr
    }

    private fun finishCall(callee:Expression): Expression{
        val arguments = mutableListOf<Expression>()
        if (match(TokenType.RIGHT_PAREN)){
            do {
                if (arguments.size >= 255){
                    error(peek(),"Can't have more than 255 arguments ")
                }
                arguments.add(expression())
            }while (match(TokenType.COMMA))
        }
        val paren = consume(TokenType.RIGHT_PAREN,"Expect ')' after arguments")
        return Expression.Call(callee,paren,arguments)
    }

    private fun primary(): Expression {
        return when {
            match(TokenType.FALSE) -> Expression.Literal(false)
            match(TokenType.TRUE) -> Expression.Literal(true)
            match(TokenType.NIL) -> Expression.Literal(null)
            match(TokenType.NUMBER, TokenType.STRING) -> Expression.Literal(previous().literal)
            match(TokenType.LEFT_PAREN) -> {
                val expr = expression()
                consume(TokenType.RIGHT_PAREN, "Expect ')' after expression")
                return Expression.Grouping(expr)

            }
            match(TokenType.THIS) -> Expression.This(previous())
            match(TokenType.IDENTIFIER) -> Expression.Variable(previous())
            match(TokenType.SUPER) -> {
                val keyword = previous()
                consume(TokenType.DOT,"Expect '.' after super.")
                val method = consume(TokenType.IDENTIFIER,"Expect superclass method name.")
                return Expression.Super(keyword, method)
            }
            else -> throw error(peek(), "Expect expression")
        }
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) {
            return advance()
        }
        throw error(peek(), message)
    }

    private fun error(token: Token, message: String): ParseError {
        errorReporter.error(token, message)
        return ParseError()
    }

//    private fun report(line: Int, where: String, message: String){
//        println("[line" + line+ "] Error" + where + ":" + message)
//        haderror = true
//    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) {
                return
            }
            when (peek().type) {
                TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR, TokenType.IF,
                TokenType.WHILE, TokenType.PRINT, TokenType.RETURN -> return

                else -> {}
            }
            advance()
        }
    }


}



