package org.example.service

interface SshService {

    suspend fun getLastSuccessSshLines(sshLoginCount: Int): List<String>

    suspend fun getLastFailedSshLines(sshLoginCount: Int): List<String>

}