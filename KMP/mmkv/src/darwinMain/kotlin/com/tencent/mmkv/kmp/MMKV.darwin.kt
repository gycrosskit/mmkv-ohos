/*
 * Tencent is pleased to support the open source community by making
 * MMKV available.
 *
 * Copyright (C) 2026 THL A29 Limited, a Tencent company.
 * All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *       https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.mmkv.kmp

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import mmkv.mmkv_initialize
import mmkv.mmkv_initialize_with_handler
import mmkv.mmkv_root_dir
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

// region Platform-specific initialization (Darwin doesn't need Context)

/**
 * Initialize MMKV with customize settings.
 * Call this in main thread, before calling any other MMKV methods.
 *
 * @param rootDir The root dir of MMKV, passing null defaults to {NSDocumentDirectory}/mmkv.
 * @param logLevel MMKVLogInfo by default, MMKVLogNone to disable all logging.
 * @param handler The unified callback handler for MMKV.
 * @return Root dir of MMKV.
 */
@OptIn(ExperimentalForeignApi::class)
fun MMKV.Companion.initialize(
    rootDir: String? = null,
    logLevel: MMKVLogLevel = MMKVLogLevel.Info,
    handler: MMKVHandler? = null,
): String {
    nativeGroupRootDir = null
    return initializeDarwin(rootDir, logLevel, handler)
}

@OptIn(ExperimentalForeignApi::class)
private fun initializeDarwin(
    rootDir: String?,
    logLevel: MMKVLogLevel,
    handler: MMKVHandler?,
): String {
    val effectiveRootDir = rootDir ?: defaultDarwinRootDir()
    NativeMMKVHandlerHolder.handler = handler
    if (handler != null) {
        mmkv_initialize_with_handler(effectiveRootDir, logLevel.toNativeLevel(), nativeCallbacks())
    } else {
        mmkv_initialize(effectiveRootDir, logLevel.toNativeLevel())
    }
    return mmkv_root_dir()?.toKString() ?: ""
}

/**
 * Initialize MMKV with a group directory for multi-process access.
 *
 * @param rootDir The root dir of MMKV, passing null defaults to {NSDocumentDirectory}/mmkv.
 * @param groupDir The root dir of multi-process MMKV.
 * @param logLevel MMKVLogInfo by default.
 * @param handler The unified callback handler for MMKV.
 * @return Root dir of MMKV.
 */
@OptIn(ExperimentalForeignApi::class)
fun MMKV.Companion.initialize(
    rootDir: String? = null,
    groupDir: String,
    logLevel: MMKVLogLevel = MMKVLogLevel.Info,
    handler: MMKVHandler? = null,
): String {
    nativeGroupRootDir = groupDir.trimEnd('/') + "/mmkv"
    return initializeDarwin(rootDir, logLevel, handler)
}

// endregion

private fun defaultDarwinRootDir(): String {
    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
    val documentDir = paths.firstOrNull() as? String
        ?: error("Unable to resolve NSDocumentDirectory for MMKV root")
    return documentDir.trimEnd('/') + "/mmkv"
}
