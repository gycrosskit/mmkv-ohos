import com.tencent.mmkv.kmp.MMKV
import com.tencent.mmkv.kmp.initialize
import kotlin.native.CName

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
@CName("mmkv_ohos_probe")
fun mmkvOhosProbe(): Int {
    MMKV.initialize("/data/local/tmp/gycrosskit-mmkv-probe")
    return if (mmkvWriteAndRead()) 1 else 0
}

fun main() {
    check(mmkvOhosProbe() == 1)
    println("MMKV OHOS read/write passed")
}
