class Environment(val enclosing:Environment? = null) {
    private val values = mutableMapOf<String,Any?>()

    fun define(name: String, value: Any?){
        values[name] = value
    }

    fun get(name:Token): Any? {
        if (values.containsValue(name.lexeme)){
            return values.get(name.lexeme)
        }
        if (enclosing!=null){
            return enclosing.get(name)
        }
        return RunTimeError(name,"Undefined variable ${name.lexeme}.")
    }

    fun assign(name: Token,value: Any?){
        if(values.containsKey(name.lexeme)){
            values[name.lexeme] = value
            return
        }
        if (enclosing!=null){
            return enclosing.assign(name,value)
        }
        throw RunTimeError(name,"Undefined Variable ${name.lexeme}.")
    }

    fun getAt(dist: Int, name: String): Any? {
        return ancestor(dist).values[name]
    }

    fun assignAt(dist: Int, name: Token, value:Any?){
        ancestor(dist).values[name.lexeme] = value
    }

    private fun ancestor(dist: Int): Environment{
        var environment = this
        for (i in 0..<dist){
            environment = environment.enclosing!!
        }
        return environment
    }
}