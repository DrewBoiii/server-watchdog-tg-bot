package service

import org.example.service.UbuntuSshLoginsService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class UbuntuSshLoginsServiceTest {

    @Test
    fun `getLastSshLogins should return formatted successful logins`(@TempDir tempDir: Path) {
        val authLog = tempDir.resolve("auth.log").toFile()
        authLog.writeText(
            """
            Apr 15 10:22:20 server sshd[1235]: Accepted publickey for ubuntu from 192.168.1.100 port 52143 ssh2
            Apr 15 10:23:01 server sshd[1236]: Accepted password for root from 10.0.0.5 port 33412 ssh2
            Apr 15 10:27:00 server sshd[1240]: Accepted publickey for admin from 192.168.1.101 port 12345 ssh2
            Apr 22 13:28:26 vps-7077 sshd[4811]: Failed password for root from 2.57.122.191 port 58758 ssh2
            Apr 22 13:28:28 vps-7077 sshd[4813]: Accepted password for root from 158.58.128.103 port 59664 ssh2
            Apr 22 13:28:28 vps-7077 sshd[4813]: pam_unix(sshd:session): session opened for user root(uid=0) by (uid=0)
            Apr 22 13:28:28 vps-7077 systemd-logind[660]: New session 12 of user root.
            Apr 22 13:28:28 vps-7077 systemd: pam_unix(systemd-user:session): session opened for user root(uid=0) by (uid=0)
            Apr 22 13:28:31 vps-7077 sshd[4811]: Failed password for root from 2.57.122.191 port 58758 ssh2
            Apr 22 13:28:37 vps-7077 sshd[4811]: message repeated 2 times: [ Failed password for root from 2.57.122.191 port 58758 ssh2]
            Apr 22 13:28:39 vps-7077 sshd[4811]: Connection reset by authenticating user root 2.57.122.191 port 58758 [preauth]
            Apr 22 13:28:39 vps-7077 sshd[4811]: PAM 4 more authentication failures; logname= uid=0 euid=0 tty=ssh ruser= rhost=2.57.122.191  user=root
            Apr 22 13:28:39 vps-7077 sshd[4811]: PAM service(sshd) ignoring max retries; 5 > 3
        """.trimIndent()
        )

        val service = UbuntuSshLoginsService(authLog)

        val result = service.getLastSuccessSshLogins(5)

        val expected = """
            **Last Success SSH-logins:**
            • 15.04.2026 13:22:20: Accepted publickey for ubuntu from 192.168.1.100 port 52143 ssh2
            • 15.04.2026 13:23:01: Accepted password for root from 10.0.0.5 port 33412 ssh2
            • 15.04.2026 13:27:00: Accepted publickey for admin from 192.168.1.101 port 12345 ssh2
            • 22.04.2026 16:28:28: Accepted password for root from 158.58.128.103 port 59664 ssh2
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `getLastFailedSshLogins should return formatted successful logins`(@TempDir tempDir: Path) {
        val authLog = tempDir.resolve("auth.log").toFile()
        authLog.writeText(
            """
            Apr 15 10:22:20 server sshd[1235]: Accepted publickey for ubuntu from 192.168.1.100 port 52143 ssh2
            Apr 15 10:23:01 server sshd[1236]: Accepted password for root from 10.0.0.5 port 33412 ssh2
            Apr 15 10:27:00 server sshd[1240]: Accepted publickey for admin from 192.168.1.101 port 12345 ssh2
            Apr 22 13:28:26 vps-7077 sshd[4811]: Failed password for root from 2.57.122.191 port 58758 ssh2
            Apr 22 13:28:28 vps-7077 sshd[4813]: Accepted password for root from 158.58.128.103 port 59664 ssh2
            Apr 22 13:28:28 vps-7077 sshd[4813]: pam_unix(sshd:session): session opened for user root(uid=0) by (uid=0)
            Apr 22 13:28:28 vps-7077 systemd-logind[660]: New session 12 of user root.
            Apr 22 13:28:28 vps-7077 systemd: pam_unix(systemd-user:session): session opened for user root(uid=0) by (uid=0)
            Apr 22 13:28:31 vps-7077 sshd[4811]: Failed password for root from 2.57.122.191 port 58758 ssh2
            Apr 22 13:28:37 vps-7077 sshd[4811]: message repeated 2 times: [ Failed password for root from 2.57.122.191 port 58758 ssh2]
            Apr 22 13:28:39 vps-7077 sshd[4811]: Connection reset by authenticating user root 2.57.122.191 port 58758 [preauth]
            Apr 22 13:28:39 vps-7077 sshd[4811]: PAM 4 more authentication failures; logname= uid=0 euid=0 tty=ssh ruser= rhost=2.57.122.191  user=root
            Apr 22 13:28:39 vps-7077 sshd[4811]: PAM service(sshd) ignoring max retries; 5 > 3
            Apr 22 13:30:02 vps-7077 sshd[4912]: Failed password for ubuntu from 182.253.156.173 port 55910 ssh2
            Apr 22 13:30:03 vps-7077 sshd[4912]: Received disconnect from 182.253.156.173 port 55910:11: Bye Bye [preauth]
            Apr 22 13:30:03 vps-7077 sshd[4912]: Disconnected from authenticating user ubuntu 182.253.156.173 port 55910 [preauth]
        """.trimIndent()
        )

        val service = UbuntuSshLoginsService(authLog)

        val result = service.getLastFailedSshLogins(5)

        val expected = """
            **Last Failed SSH-logins:**
            • 22.04.2026 16:28:26: Failed password for root from 2.57.122.191 port 58758 ssh2
            • 22.04.2026 16:28:31: Failed password for root from 2.57.122.191 port 58758 ssh2
            • 22.04.2026 16:28:37: message repeated 2 times: [ Failed password for root from 2.57.122.191 port 58758 ssh2]
            • 22.04.2026 16:30:02: Failed password for ubuntu from 182.253.156.173 port 55910 ssh2
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `getLastSshLogins should handle missing file`() {
        val nonExistentFile = File("/nonexistent/auth.log")
        val service = UbuntuSshLoginsService(nonExistentFile)
        val result = service.getLastSuccessSshLogins(1)

        assertEquals("File with SSH logs wasn't found.", result)
    }

    @Test
    fun `getLastSshLogins should return empty message when no successful entries`(@TempDir tempDir: Path) {
        val authLog = tempDir.resolve("auth.log").toFile()
        authLog.writeText(
            """
            Apr 15 10:22:15 server sshd[1234]: Failed password for invalid user admin from 1.2.3.4 port 22 ssh2
        """.trimIndent()
        )

        val service = UbuntuSshLoginsService(authLog)

        val result = service.getLastSuccessSshLogins(1)

        assertEquals("SSH-logins wasn't found.", result)
    }
}