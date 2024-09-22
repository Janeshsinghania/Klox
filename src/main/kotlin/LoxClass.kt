class LoxClass(val name: String, val superclass: LoxClass?, val methods: Map<String,LoxFunction>): LoxCallable{

    fun findMethod(name: String): LoxFunction?{
        if (methods.containsKey(name)){
            return methods.get(name)
        }
        if (superclass != null){
            return superclass.findMethod(name)
        }
        return null
    }
    override fun toString(): String {
        return name
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val instance = LoxInstance(this)
        val initializer = findMethod("init")
        initializer?.bind(instance)?.call(interpreter, arguments)
        return instance
    }

    override fun arity(): Int {
        val initializer = findMethod("init")
        return initializer?.arity()?: 0
    }
}