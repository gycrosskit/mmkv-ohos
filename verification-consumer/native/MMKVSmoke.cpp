#include "MMKVBridge.h"

#include <cstdio>
#include <cstring>

int main() {
    mmkv_initialize("/data/local/tmp/gycrosskit-mmkv-native-probe", 1);
    MMKVConfig_t config = {};
    config.mode = 1;
    config.enableKeyExpire = -1;
    config.recover = -1;
    auto store = mmkv_with_id("probe", config);
    if (!store || !mmkv_encode_string(store, "key", "value")) {
        return 1;
    }
    auto value = mmkv_decode_string(store, "key");
    const bool success = value && std::strcmp(value, "value") == 0;
    mmkv_free(value);
    if (!success) {
        return 2;
    }
    std::puts("MMKV native OHOS read/write passed");
    return 0;
}
