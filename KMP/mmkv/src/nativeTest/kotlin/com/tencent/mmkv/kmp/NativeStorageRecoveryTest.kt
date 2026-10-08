package com.tencent.mmkv.kmp

import platform.posix.sleep
import kotlin.test.*

class NativeStorageRecoveryTest {
    @Test fun encryptedDataSurvivesCloseRekeyBackupRestoreAndExpiry() {
        MMKVTestEnv.initialize()
        val id = MMKVTestEnv.uniqueID("rekey-backup")
        val backup = MMKVTestEnv.uniquePath("backup")
        val firstKey = "first-key-123456"
        val secondKey = "second-key-12345"
        var kv = MMKV.mmkvWithID(id, MMKVConfig(cryptKey = firstKey))
        try {
            assertTrue(kv.encodeString("value", "durable")); kv.sync(); kv.close()
            kv = MMKV.mmkvWithID(id, MMKVConfig(cryptKey = firstKey))
            assertEquals("durable", kv.decodeString("value"))
            assertTrue(kv.reKey(secondKey)); kv.sync(); kv.close()
            kv = MMKV.mmkvWithID(id, MMKVConfig(cryptKey = secondKey))
            assertEquals("durable", kv.decodeString("value")); assertTrue(kv.isEncryptionEnabled)
            assertTrue(MMKV.backupOneToDirectory(id, backup))
            assertTrue(kv.encodeString("value", "changed")); kv.sync(); kv.close()
            assertTrue(MMKV.restoreOneFromDirectory(id, backup))
            kv = MMKV.mmkvWithID(id, MMKVConfig(cryptKey = secondKey))
            assertEquals("durable", kv.decodeString("value"))
            assertTrue(kv.enableAutoKeyExpire())
            assertTrue(kv.encodeString("expires", "temporary", 1u)); kv.sync()
            sleep(2u)
            assertNull(kv.decodeString("expires")); assertFalse(kv.containsKey("expires"))
        } finally { kv.close(); MMKV.removeStorage(id) }
    }
}
