
sealed class Stmt {
    class Block(val statements: List<Stmt>) : Stmt()
    class Class(val name: Token, val superClass: Expression.Variable?, val methods: List<Function>) : Stmt()
    class Expr(val expr: Expression) : Stmt()
    class Function(val name: Token, val params: List<Token>, val body: List<Stmt>) : Stmt()
    class If(val condition: Expression, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt()
    class Print(val value: Expression) : Stmt()
    class Return(val keyword: Token, val value: Expression?) : Stmt()
    class Var(val name: Token, val initializer: Expression?) : Stmt()
    class While(val condition: Expression, val body: Stmt) : Stmt()
}