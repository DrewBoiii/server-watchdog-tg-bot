package strategy

import org.example.strategy.SyslogAuthLogParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyslogAuthLogParserTest {

    private val parser = SyslogAuthLogParser()

    @Test
    fun `predicate returns true for valid syslog format`() {
        val lines = listOf("Apr 15 10:22:20 server sshd[1235]: Accepted publickey ...")
        assertTrue(parser.predicate(lines))
    }

    @Test
    fun `predicate returns false for ISO format`() {
        val lines = listOf("2026-07-26T01:15:28.693716+00:00 vps-1513 sshd-session[192413]: Failed ...")
        assertFalse(parser.predicate(lines))
    }

    @Test
    fun `predicate returns false for empty list`() {
        assertFalse(parser.predicate(emptyList()))
    }

    @Test
    fun `parseSshLogins formats success logins correctly`() {
        val lines = listOf(
            "Apr 15 10:22:20 server sshd[1235]: Accepted publickey for ubuntu from 192.168.1.100 port 52143 ssh2",
            "Apr 22 13:28:28 vps-7077 sshd[4813]: Accepted password for root from 158.58.128.103 port 59664 ssh2"
        )
        val stringBuilder = StringBuilder()
        parser.parseSshLogins(stringBuilder, lines)

        val expected = """
            • 15.04.${currentYear} 13:22:20: Accepted publickey for ubuntu from 192.168.1.100 port 52143 ssh2
            
            • 22.04.${currentYear} 16:28:28: Accepted password for root from 158.58.128.103 port 59664 ssh2
            
        """.trimIndent() + "\n"

        assertEquals(expected, stringBuilder.toString())
    }

    @Test
    fun `parseSshLogins handles double space in date`() {
        val lines = listOf(
            "May  1 10:22:20 server sshd[1235]: Accepted publickey for ubuntu from 192.168.1.100 port 52143 ssh2"
        )
        val stringBuilder = StringBuilder()
        parser.parseSshLogins(stringBuilder, lines)

        assert(stringBuilder.toString().contains("• 01.05.${currentYear} 13:22:20"))
    }

    @Test
    fun `parseSshLogins extracts message after first sshd`() {
        val lines = listOf(
            "Apr 22 13:28:26 vps-7077 sshd[4811]: Failed password for root from 2.57.122.191 port 58758 ssh2"
        )
        val stringBuilder = StringBuilder()
        parser.parseSshLogins(stringBuilder, lines)

        assert(stringBuilder.toString().contains("22.04.${currentYear} 16:28:26: Failed password for root from 2.57.122.191 port 58758 ssh2"))
    }

    @Test
    fun `parseSshLogins works with repeated messages`() {
        val lines = listOf(
            "Apr 22 13:28:37 vps-7077 sshd[4811]: message repeated 2 times: [ Failed password for root from 2.57.122.191 port 58758 ssh2]"
        )
        val stringBuilder = StringBuilder()
        parser.parseSshLogins(stringBuilder, lines)

        assert(
            stringBuilder.toString()
                .contains("message repeated 2 times: [ Failed password for root from 2.57.122.191 port 58758 ssh2]")
        )
    }

    companion object {
        val currentYear = LocalDateTime.now().year
    }
}