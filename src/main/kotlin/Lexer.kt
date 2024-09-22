import Token
import TokenType

class Scanner(private val source: String, private val errorReporter: ErrorReporterInterface) {
    private val tokens = mutableListOf<Token>()
    var start = 0
    var current = 0
    var line = 1

    companion object {
        val keywords = mapOf(
            "and" to TokenType.AND,
            "class" to TokenType.CLASS,
            "else" to TokenType.ELSE,
            "false" to TokenType.FALSE,
            "for" to TokenType.FOR,
            "fun" to TokenType.FUN,
            "if" to TokenType.IF,
            "nil" to TokenType.NIL,
            "or" to TokenType.OR,
            "print" to TokenType.PRINT,
            "return" to TokenType.RETURN,
            "super" to TokenType.SUPER,
            "this" to TokenType.THIS,
            "true" to TokenType.TRUE,
            "var" to TokenType.VAR,
            "while" to TokenType.WHILE
        )
    }

//    fun error(line: Int, message: String) {
//        report(line,"",message);
//    }
//    private fun report(line: Int, where: String, message: String){
//        println("[line" + line+ "] Error" + where + ":" + message)
//        haderror = true
//    }

    fun scanTokens(): List<Token>{
        while (!isAtEnd()){
            start = current
            scanToken()
        }
        tokens.add(Token(TokenType.EOF,"",null,line))
        return tokens
    }

    fun isAtEnd(): Boolean{
        return current >= source.length;
    }

    private fun scanToken(){
        var c = advance()
        when(c){
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON)
            '*' -> addToken(TokenType.STAR)
            '!' -> addToken(if (match('=')) TokenType.NOT_EQUAL else TokenType.NOT)
            '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) {
                        advance()
                    }
                } else {
                    addToken(TokenType.SLASH)
                }
            }
            ' ', '\r', '\t' -> {
                // Ignore whitespace
            }
            '\n' -> line++
            '"' -> string()
            else -> when {
                isDigit(c) -> number()
                isAlpha(c) -> identifier()
                else -> errorReporter.error(line, "Unexpected character")
            }
        }
    }

    private fun advance(): Char{
        current++
        return source[current-1]
    }

    private fun addToken(type: TokenType){
        addToken(type,null)
    }

    private fun addToken(type: TokenType, literal: Any?){
        var text: String = source.substring(start,current)
        tokens.add(Token(type,text,literal,line))
    }

    private fun match(expected: Char): Boolean{
        if (isAtEnd()){
            return false
        }
        if (source[current]!=expected){
            return false
        }
        current++
        return true
    }

    private fun peek(): Char{
        if (isAtEnd()){
            return '\u0000'
        }
        return source[current]
    }

    private fun string(){
        while (peek()!= '"' && !isAtEnd()){
            if (peek() == '\n'){
                line++
            }
            advance()
        }
        if (isAtEnd()){
            errorReporter.error(line,"Undetermined String")
            return
        }
        advance()
        val string = source.substring(start+1, current-1)
        addToken(TokenType.STRING, string)
    }

    private fun isDigit(c: Char): Boolean{
        return c in '0'..'9'
    }

    private fun number(){
        while (isDigit(peek())){
            advance()
        }
        if (peek() == '.' && isDigit(peekNext())){
            advance()
            while (isDigit(peek())){
                advance()
            }
        }
        addToken(TokenType.NUMBER, source.substring(start,current).toDouble())
    }

    private fun peekNext(): Char{
        if (current+1 >= source.length){
            return '\u0000'
        }
        return source[current + 1]
    }

    private fun identifier(){
        while (isAlphaNumeric(peek())){
            advance()
        }
        val text: String = source.substring(start, current)
        val type = keywords.getOrDefault(text, TokenType.IDENTIFIER)
        addToken(type)
    }

    private fun isAlpha(c: Char): Boolean{
        return (c in 'a'..'z' || c in 'A'..'Z' || c == '_')
    }

    private fun isAlphaNumeric(c: Char): Boolean{
        return isAlpha(c) || isDigit(c)
    }



}