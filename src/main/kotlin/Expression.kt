
sealed class Expression {
    class Assign(val name: Token, val value: Expression) : Expression()
    class Binary(val left: Expression, val op: Token, val right: Expression) : Expression()
    class Call(val callee: Expression, val paren: Token, val arguments: List<Expression>) : Expression()
    class Get(val obj: Expression, val name: Token) : Expression()
    class Grouping(val expr: Expression) : Expression()
    class Literal(val value: Any?) : Expression()
    class Logical(val left: Expression, val op: Token, val right: Expression) : Expression()
    class Unary(val op: Token, val expr: Expression) : Expression()
    class Variable(val name: Token) : Expression()
    class Set(val obj: Expression, val name: Token, val value: Expression) : Expression()
    class Super(val keyword: Token, val method: Token) : Expression()
    class This(val keyword: Token) : Expression()
}