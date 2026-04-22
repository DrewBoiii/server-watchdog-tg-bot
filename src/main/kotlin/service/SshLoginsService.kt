package org.example.service

interface SshLoginsService {

    fun getLastSuccessSshLogins(sshLoginCount: Int): String

    fun getLastFailedSshLogins(sshLoginCount: Int): String

}