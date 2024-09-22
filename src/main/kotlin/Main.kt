import kotlin.system.exitProcess
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object Lox{
    private val errorReporter = ErrorReporter()
    private val interpreter = Interpreter(errorReporter)
    fun main(vararg args: String) {
        if (args.size > 1) {
            println("Usage: klox [script]");
            exitProcess(64);
        } else if (args.size == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private fun runFile(path: String){
        run(File(path).readText())
        if (errorReporter.hadError){
            exitProcess(65)
        }
        if (errorReporter.hadRuntimeError){
            exitProcess(70)
        }
    }

    private fun runPrompt(){
        val input = InputStreamReader(System.`in`)
        val reader = BufferedReader(input)

        while (true) {
            print("> ")
            val line = reader.readLine() ?: break
            run(line)
            errorReporter.reset()
        }
    }

    private fun run(source: String){
        val scanner = Scanner(source, errorReporter)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens.toCollection(ArrayDeque()), errorReporter)

        val statements = parser.parse()
        if (errorReporter.hadError) {
            return
        }

        val resolver = Resolver(interpreter, errorReporter)
        resolver.resolve(statements)
        if (errorReporter.hadError) {
            return
        }

        interpreter.interpret(statements)
    }
}




//fun main() {
//    val errorReporter = ErrorReporter()
//    val scanner = Scanner("print 2+3 ;",errorReporter)
//    val tokens = scanner.scanTokens()
//    val parser = Parser(tokens,errorReporter)
//    val expr: List<Stmt> = parser.parse()
//    println(expr?.let { AstPrinter().print(it) })
//    val interpreter = Interpreter(errorReporter)
//    if (expr != null) {
//        interpreter.interpret(expr)
//    }
//
//}

