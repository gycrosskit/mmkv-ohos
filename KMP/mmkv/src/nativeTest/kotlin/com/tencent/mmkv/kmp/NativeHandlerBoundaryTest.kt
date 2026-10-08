package com.tencent.mmkv.kmp

import kotlinx.cinterop.*
import kotlin.test.*

@OptIn(ExperimentalForeignApi::class)
class NativeHandlerBoundaryTest {
    @Test fun throwingHostCallbacksCannotCrossCAbiAndRecoveryDefaultsToDiscard() {
        val calls = mutableListOf<String>()
        val throwing = object : MMKVHandler() {
            override fun wantLogRedirect(): Boolean { calls += "log preference"; error("log preference") }
            override fun onMMKVCRCCheckFail(mmapID: String): MMKVRecoverStrategic { calls += "crc"; error("crc") }
            override fun onMMKVFileLengthError(mmapID: String): MMKVRecoverStrategic { calls += "length"; error("length") }
            override fun wantContentChangeNotification(): Boolean { calls += "content preference"; error("content preference") }
            override fun onMMKVContentLoadSuccessfully(mmapID: String) { calls += "load"; error("load") }
        }
        NativeMMKVHandlerHolder.handler = throwing
        try {
            nativeCallbacks().useContents {
                log!!.invoke(0, null, 0, null, null)
                assertEquals(0, error!!.invoke(null, 0)); assertEquals(0, error!!.invoke(null, 1))
                contentChange!!.invoke(null); contentLoad!!.invoke(null)
            }
            assertEquals(listOf("log preference", "crc", "length", "content preference", "load"), calls)
            calls.clear()
            NativeMMKVHandlerHolder.handler = object : MMKVHandler() {
                override fun wantLogRedirect() = true
                override fun mmkvLog(level: MMKVLogLevel, file: String, line: Int, function: String, message: String) {
                    calls += "log body"; error("log body")
                }
                override fun wantContentChangeNotification() = true
                override fun onContentChangedByOuterProcess(mmapID: String) {
                    calls += "content body"; error("content body")
                }
            }
            nativeCallbacks().useContents {
                log!!.invoke(0, null, 0, null, null)
                contentChange!!.invoke(null)
            }
            assertEquals(listOf("log body", "content body"), calls)
            NativeMMKVHandlerHolder.handler = object : MMKVHandler() {
                override fun onMMKVCRCCheckFail(mmapID: String) = MMKVRecoverStrategic.OnErrorRecover
            }
            nativeCallbacks().useContents { assertEquals(1, error!!.invoke(null, 0)) }
        } finally { NativeMMKVHandlerHolder.handler = null }
    }
}
