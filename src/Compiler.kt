class Compiler(private val _class: Class) {
    val className = _class.name
    private val table = SymbolTable(_class)
    private var subroutineTable: Map<String, SymbolValue>? = null
    private val vmWriter = VMWriter(className)

    fun compileClass() {
        _class.subroutineDec.forEach { compileSubroutine(it) }
    }

    private fun getSymbolInfo(name: String): SymbolValue {
        val subTable = subroutineTable
        if (subTable != null) {
            return subTable[name] ?: table.classTable[name]
            ?: throw Error("シンボルテーブル")
        } else {
            return table.classTable[name]
                ?: throw Error("シンボルテーブル")
        }
    }

    private fun localVarNum(): Int {
        return table.varIndex
    }

    private fun compileSubroutine(subroutineDec: SubroutineDec) {
        subroutineTable = table.subroutineTableCreator(subroutineDec)
        vmWriter.writeFunction(subroutineDec.name, localVarNum())
        if (subroutineDec.dec == MethodDec.Constructor) {
            if (table.fieldIndex != -1) {
                vmWriter.writePush(Segment.CONSTANT, table.fieldIndex + 1)
                vmWriter.writeCall("Memory.alloc", 1)
                vmWriter.writePop(Segment.POINTER, 0)
            }
        }
        compileStatements(subroutineDec.body.statements)
        val type = subroutineDec.type
    }

    private fun compileStatements(statements: Statements) {
        statements.statements.forEach {
            when (it) {
                is Stmt.Do -> {
                    compileDoStatement(it.stmt)
                }
                is Stmt.Let -> {
                    compileLetStatement(it.stmt)
                }
                is Stmt.Return -> {
                    compileReturn(it.stmt)
                }
            }
        }
    }

    private fun compileReturn(stmt: ReturnStatement) {
        if (stmt.expression == null) {
            vmWriter.writePush(Segment.CONSTANT, 0)
        } else {
            val _term = stmt.expression.expElms[0]
            if (_term is ExpElm._Term) {
                val term = _term.term
                if (term is Term.KeyC) {
                    if (term.const == Keyword.This) {
                        vmWriter.writePush(Segment.POINTER, 0)
                    }
                }
            }
        }
        vmWriter.writeReturn()
    }

    private fun compileLetStatement(letStatement: LetStatement) {
        val symbolInfo = getSymbolInfo(letStatement.varName.name)
        val index = symbolInfo.index
        val exp = letStatement.exp
        val arrayIndex = letStatement.index
        compileExpression(exp)
        if (arrayIndex == null) {
            if (symbolInfo.attribute == Attribute.Field) {
                vmWriter.writePop(Segment.THIS, symbolInfo.index)
            } else if (symbolInfo.attribute == Attribute.Argument) {
                vmWriter.writePop(Segment.ARGUMENT, index)
            } else if (symbolInfo.attribute == Attribute.Var) {
                vmWriter.writePop(Segment.LOCAL, index)
            }
        } else {
        }
    }

    private fun compileDoStatement(doStatement: DoStatement) {
        // TODO メソッドなら呼ぶ前にオブジェクトをpushする
        // TODO そして引数を一つ増やす
        // TODO メソッドかファンクションかを判定するために、シンボルテーブルを拡張するべき

        val classOrVarName = doStatement.subroutineCall.classOrVarName
        val subroutineName = doStatement.subroutineCall.subroutineName
        val expList = doStatement.subroutineCall.expList.expList
        if (classOrVarName != null) {
            expList.forEach { compileExpression(it) }
            vmWriter.writeCall("$className.${subroutineName.name}", expList.count())
        } else {
            expList.forEach { compileExpression(it) }
            vmWriter.writeCall("$className.${subroutineName.name}", expList.count())
        }
        vmWriter.writePop(Segment.TEMP, 0)
    }

    private fun compileExpression(exp: Expression) {
        val first = exp.expElms.first()
        if (exp.expElms.count() > 1) {
            val op = exp.expElms[1]
            val rest = exp.expElms.slice(2 until exp.expElms.count())
            if (first is ExpElm._Term && op is ExpElm._Op) {
                compileTerm(first.term)
                compileExpression(Expression(rest))
                compileOperand(op.op)
            }
        } else if (first is ExpElm._Term) {
            compileTerm(first.term)
        }
    }

    private fun compileTerm(term: Term) {
        if (term is Term.IntC) {
            vmWriter.writePush(Segment.CONSTANT, term.const)
        } else if (term is Term.VarName) {
            val symbolInfo = getSymbolInfo(term.name)
            if (symbolInfo.attribute == Attribute.Field) {
                vmWriter.writePush(Segment.THIS, symbolInfo.index)
            } else if (symbolInfo.attribute == Attribute.Argument) {
                vmWriter.writePush(Segment.ARGUMENT, symbolInfo.index)
            } else if (symbolInfo.attribute == Attribute.Var) {
                vmWriter.writePush(Segment.LOCAL, symbolInfo.index)
            }
        } else if (term is Term._Expression) {
            compileExpression(term.exp)
        }
    }

    private fun compileOperand(op: Op) {
        if (op == Op.Plus) {
            vmWriter.writeArithmetic(Command.ADD)
        } else if (op == Op.Minus) {
            vmWriter.writeArithmetic(Command.SUB)
        } else if (op == Op.Asterisk) {
            vmWriter.writeCall("Math.multiply", 2)
        } else if (op == Op.Slash) {
            vmWriter.writeCall("Math.divide", 2)
        }
    }
}