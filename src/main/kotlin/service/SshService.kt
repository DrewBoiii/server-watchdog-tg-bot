package org.example.service

interface SshService {

    fun getLastSuccessSshLines(sshLoginCount: Int): List<String>

    fun getLastFailedSshLines(sshLoginCount: Int): List<String>

}