import com.tencent.mmkv.kmp.MMKV

fun mmkvWriteAndRead(): Boolean {
    val store = MMKV.mmkvWithID("probe")
    return store.encodeString("key", "value") && store.decodeString("key") == "value"
}
