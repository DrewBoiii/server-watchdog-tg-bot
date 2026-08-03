package strategy

import org.example.strategy.Iso8601AuthLogParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Iso8601AuthLogParserTest {

    private val parser = Iso8601AuthLogParser()

    @Test
    fun `predicate returns true for valid ISO format`() {
        val lines = listOf("2026-07-26T01:15:28.693716+00:00 vps-1513 sshd-session[192413]: Failed ...")
        assertTrue(parser.predicate(lines))
    }

    @Test
    fun `predicate returns false for syslog format`() {
        val lines = listOf("Apr 15 10:22:20 server sshd[1235]: Accepted ...")
        assertFalse(parser.predicate(lines))
    }

    @Test
    fun `predicate returns false for empty list`() {
        assertFalse(parser.predicate(emptyList()))
    }

    @Test
    fun `parseSshLogins formats ISO logins correctly`() {
        val lines = listOf(
            "2026-07-26T01:15:28.693716+00:00 vps-1513 sshd-session[192413]: Failed password for root from 107.172.57.42 port 33758 ssh2",
            "2026-07-26T01:15:31.143638+00:00 vps-1513 sshd-session[192415]: Failed password for root from 45.148.10.157 port 55654 ssh2"
        )
        val stringBuilder = StringBuilder()
        parser.parseSshLogins(stringBuilder, lines)

        val expected = """
            • 26.07.2026 04:15:28: Failed password for root from 107.172.57.42 port 33758 ssh2
            
            • 26.07.2026 04:15:31: Failed password for root from 45.148.10.157 port 55654 ssh2
            
        """.trimIndent() + "\n"

        assertEquals(expected, stringBuilder.toString())
    }

    @Test
    fun `parseSshLogins handles unix_chkpwd messages`() {
        val lines = listOf(
            "2026-07-26T01:15:29.266543+00:00 vps-1513 unix_chkpwd[192419]: password check failed for user (root)"
        )
        val stringBuilder = StringBuilder()
        parser.parseSshLogins(stringBuilder, lines)

        assert(stringBuilder.toString().contains("password check failed for user (root)"))
        assert(stringBuilder.toString().contains("26.07.2026 04:15:29"))
    }

    @Test
    fun `parseSshLogins works with preauth messages`() {
        val lines = listOf(
            "2026-07-26T01:15:32.066701+00:00 vps-1513 sshd-session[192413]: Connection closed by authenticating user root 107.172.57.42 port 33758 [preauth]"
        )
        val stringBuilder = StringBuilder()
        parser.parseSshLogins(stringBuilder, lines)

        assert(stringBuilder.toString().contains("Connection closed by authenticating user root 107.172.57.42 port 33758 [preauth]"))
    }
}