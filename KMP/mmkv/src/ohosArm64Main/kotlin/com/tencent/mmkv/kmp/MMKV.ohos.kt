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

/**
 * Initialize MMKV before using any instance. Pass an app-private, writable
 * directory supplied by the OpenHarmony host.
 */
@OptIn(ExperimentalForeignApi::class)
fun MMKV.Companion.initialize(
    rootDir: String,
    logLevel: MMKVLogLevel = MMKVLogLevel.Info,
    handler: MMKVHandler? = null,
): String {
    require(rootDir.isNotBlank()) { "MMKV rootDir must not be blank" }
    nativeGroupRootDir = null
    NativeMMKVHandlerHolder.handler = handler
    if (handler == null) {
        mmkv_initialize(rootDir, logLevel.toNativeLevel())
    } else {
        mmkv_initialize_with_handler(rootDir, logLevel.toNativeLevel(), nativeCallbacks())
    }
    return mmkv_root_dir()?.toKString() ?: ""
}
