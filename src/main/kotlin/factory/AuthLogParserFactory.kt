package org.example.factory

import org.example.strategy.AuthLogParser
import org.example.strategy.Iso8601AuthLogParser
import org.example.strategy.SyslogAuthLogParser

class AuthLogParserFactory {

    private val strategies = listOf(
        SyslogAuthLogParser(),
        Iso8601AuthLogParser(),
    )

    fun createAuthLogParsers(): List<AuthLogParser> = strategies

}