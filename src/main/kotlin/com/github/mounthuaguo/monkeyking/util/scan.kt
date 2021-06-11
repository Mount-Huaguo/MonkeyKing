package com.github.mounthuaguo.monkeyking.util

class ScanString(val start: Int, val end: Int, val value: String)

fun scanString(start: Int, end: Int, text: String): ScanString {
    val chars = text.asSequence().toList()
    var startPosition = start
    var endPosition = end
    var begin = ' '
    var tail = ' '
    while (startPosition > 0) {
        if (chars[startPosition] == '\'' || chars[startPosition] == '"') {
            begin = chars[startPosition]
            break
        }
        startPosition--
    }
    startPosition++
    while (chars.size > endPosition) {
        if (chars[endPosition] == '\'' || chars[endPosition] == '"') {
            tail = chars[endPosition]
            break
        }
        endPosition++
    }
    if (begin == ' ' || begin != tail) {
        return ScanString(-1, -1, "")
    }
    return try {
        ScanString(startPosition, endPosition, chars.subList(startPosition, endPosition).joinToString(""))
    } catch (e: Exception) {
        ScanString(-1, -1, "")
    }
}

fun scanHex(start: Int, end: Int, text: String): ScanString {
    val chars = text.asSequence().toList()
    var startPosition = start
    var endPosition = end

    fun isValid(c: Char): Boolean {
        return (c in '0'..'9') || (c in 'a'..'f') || (c in 'A'..'F')
    }

    while (startPosition >= 0) {
        if (!isValid(chars[startPosition])) {
            break
        }
        startPosition--
    }
    startPosition++
    while (chars.size > endPosition) {
        if (!isValid(chars[endPosition])) {
            break
        }
        endPosition++
    }
    return try {
        ScanString(startPosition, endPosition, chars.subList(startPosition, endPosition).joinToString(""))
    } catch (e: Exception) {
        ScanString(-1, -1, "")
    }
}

class ScanInt(val start: Int, val end: Int, val value: Int)

fun scanInt(start: Int, end: Int, text: String): ScanInt {
    val chars = text.asSequence().toList()
    var startPosition = start
    var endPosition = end
    while (startPosition >= 0) {
        if (chars[startPosition] < '0' || chars[startPosition] > '9') {
            break
        }
        startPosition--
    }
    startPosition++
    while (chars.size > endPosition) {
        if (chars[endPosition] < '0' || chars[endPosition] > '9') {
            break
        }
        endPosition++
    }
    return try {
        ScanInt(startPosition, endPosition, chars.subList(startPosition, endPosition).joinToString("").toInt())
    } catch (e: Exception) {
        ScanInt(-1, -1, 0)
    }
}
