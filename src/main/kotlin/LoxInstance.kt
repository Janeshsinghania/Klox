class LoxInstance(private val loxClass: LoxClass) {
    private val fields = mutableMapOf<String,Any?>()

    fun get(name: Token): Any?{
        if (fields.containsKey(name.lexeme)){
            return fields[name.lexeme]
        }
        loxClass.findMethod(name.lexeme)?.let{
            return it.bind(this)
        }
        throw RunTimeError(name,"Undefined property ${name.lexeme}.")
    }

    fun set(name: Token,value: Any?){
        fields.put(name.lexeme,value)
    }
    override fun toString(): String {
        return loxClass.name + " instance"
    }
}