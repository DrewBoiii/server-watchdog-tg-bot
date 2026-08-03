package org.example.strategy

interface AuthLogParser {

    fun parseSshLogins(stringBuilder: StringBuilder, lines: List<String>): StringBuilder

    fun predicate(lines: List<String>): Boolean

}