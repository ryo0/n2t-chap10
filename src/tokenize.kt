sealed class Token {
    data class Number(val num: String) : Token()
    data class StringConst(val string: String) : Token()
    data class Keyword(val key: String) : Token()
    data class VarOrKeyword(val name: String) : Token()
    data class Variable(val varName: String) : Token()
    data class Operand(val op: String) : Token()
    data class Parentheses(val par: String) : Token()
}

val Keywords = listOf("let", "class", "var")

fun tokenizeSub(inputStr: String): List<Token> {
    var i = 0
    val tokens = mutableListOf<Token>()
    while (i < inputStr.length) {
        val str = inputStr[i]
        when (str) {
            ' ', '\n' -> i++
            '+', '-', '*', '/' -> {
                tokens.add(Token.Operand(str.toString()))
                i++
            }
            '(', ')' -> {
                tokens.add(Token.Parentheses(str.toString()))
                i++
            }
            '=' -> if (inputStr[i + 1] == '=') {
                tokens.add(Token.Operand((inputStr[i].plus(inputStr[i + 1].toString()))))
                i += 2
            } else {
                tokens.add(Token.Operand((str.toString())))
                i++
            }
            else -> {
                val strFromIToLast = inputStr.slice(i until inputStr.length)
                if (str.isDigit()) {
                    val digit = getDigit(strFromIToLast)
                    tokens.add(Token.Number(digit))
                    i += digit.length
                } else if (str.isLetter()) {
                    val varName = getVariable(strFromIToLast)
                    tokens.add(Token.VarOrKeyword(varName))
                    i += varName.length
                } else if (str == '\"') {
                    val stringAfterDoubleQuote = inputStr.slice(i + 1 until inputStr.length)
                    val endOfStringConst = stringAfterDoubleQuote.indexOfFirst { it == '\"' }
                    if (endOfStringConst != -1) {
                        val stringConst = stringAfterDoubleQuote.slice(0 until endOfStringConst)
                        tokens.add(Token.StringConst(stringConst))
                        i += endOfStringConst + 2 // stringConstの長さ+両端のダブルクオーテーション2つ
                    } else {
                        i++
                    }
                } else {
                    i++
                }
            }
        }
    }
    return tokens
}

fun isKeyword(string: String): Boolean {
    return string in Keywords
}

fun filterVarOrKeyword(tokens: List<Token>): List<Token> {
    return tokens.map {
        if (it is Token.VarOrKeyword) {
            if (isKeyword(it.name)) {
                Token.Keyword(it.name)
            } else {
                Token.Variable(it.name)
            }
        } else {
            it
        }
    }
}

fun removeComments(string: String): String {
    var nowInOneLineComment = false
    var nowInRangeComment = false
    var result = ""
    var i = 0
    while (i < string.length - 1) {
        val str1 = string[i]
        val str2 = string[i + 1]
        if (str1 == '/' && str2 == '/') {
            nowInOneLineComment = true
        } else if (str1 == '/' && str2 == '*') {
            nowInRangeComment = true
        } else if (nowInOneLineComment) {
            if (str1 == '\n') {
                nowInOneLineComment = false
            }
        } else if (nowInRangeComment) {
            if (str1 == '*' && str2 == '/') {
                nowInRangeComment = false
                i++ // 二文字分スキップしたいので余計に一つ進めておく
            }
        } else {
            result += str1
        }
        i++
    }
    return result
}


fun getVariable(string: String): String {
    var i = 0
    var result = ""
    while (i < string.length) {
        val str = string[i]
        if (str.isLetter() || str.isDigit()) {
            result += str
            i++
        } else {
            break
        }
    }
    return result
}

fun getDigit(string: String): String {
    var i = 0
    var result = ""
    while (i < string.length) {
        val str = string[i]
        if (str.isDigit()) {
            result += str
            i++
        } else {
            break
        }
    }
    return result
}

fun tokenize(string: String): List<Token> {
    return filterVarOrKeyword(tokenizeSub(removeComments(string)))
}

fun main(args: Array<String>) {
    val inputStr = """
        |let x = 2
        |let3 le class
        |(3 + 2 * let) = 3
    """.trimMargin()
    println(tokenize(inputStr))

    val x = """
    	// ねこ
        // かに
        a=="ねこ"1"a" ""
        let x = 2
        /* aaa */ ebi = 5 /* nekoneko */ * 2 + 2
        /* 111 */ b //a
        |let3 le class
        |(3 + 2 * let) = " "3 "
    """
    println(tokenize(x))
    println(tokenize("\""))
}