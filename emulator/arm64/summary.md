# arm64: Android 14, Physical size: 1080x2400
- 1.0.15 arm64-v8a поставился, ABI: primaryCpuAbi=arm64-v8a
- 1.0.15: запуск Status: ok , через 15 с процесс не запущен
    10-09 00:35:51.147  5998  5998 I ndk_translation: Initialized NDK translation (aarch64), version 0.2.3
    10-09 00:35:52.532  5998  6049 E ndk_translation: Undefined instruction 0xd53be001 at 0x000071eec8b428b8
    10-09 00:35:52.532  5998  6049 F libc    : Fatal signal 4 (SIGILL), code -6 (SI_TKILL) in tid 6049 (app.farvater), pid 5998 (app.farvater)
    10-09 00:35:52.545  5998  6056 E ndk_translation: Undefined instruction 0xd53be001 at 0x000071eec8b428b8
    10-09 00:35:52.545  5998  6054 E ndk_translation: Undefined instruction 0xd53be001 at 0x000071eec8b428b8
    10-09 00:35:52.610  5998  6047 E ndk_translation: Undefined instruction 0xd5380000 at 0x000071eec9037e30
    10-09 00:35:52.671  6058  6058 F DEBUG   : pid: 5998, tid: 6049, name: app.farvater  >>> app.farvater <<<
    10-09 00:35:52.696   544  2716 I ActivityManager: Process app.farvater (pid 5998) has died: fg  TOP 
- 1.0.16 arm64-v8a поставился, ABI: primaryCpuAbi=arm64-v8a
- 1.0.16: запуск Status: ok , через 15 с процесс не запущен
    10-09 00:36:10.190  6215  6215 I ndk_translation: Initialized NDK translation (aarch64), version 0.2.3
    10-09 00:36:10.716  6215  6269 E ndk_translation: Undefined instruction 0xd53be001 at 0x000071eebe3ee8b8
    10-09 00:36:10.716  6215  6269 F libc    : Fatal signal 4 (SIGILL), code -6 (SI_TKILL) in tid 6269 (xray-init), pid 6215 (app.farvater)
    10-09 00:36:11.092  6280  6280 F DEBUG   : pid: 6215, tid: 6269, name: xray-init  >>> app.farvater <<<
    10-09 00:36:11.147   544  2698 I ActivityManager: Process app.farvater (pid 6215) has died: fg  TOP 
    10-09 00:36:12.298  6345  6345 I ndk_translation: Initialized NDK translation (aarch64), version 0.2.3
    10-09 00:36:12.524  6345  6372 E ndk_translation: Undefined instruction 0xd53be001 at 0x000071eec832a8b8
    10-09 00:36:12.524  6345  6372 F libc    : Fatal signal 4 (SIGILL), code -6 (SI_TKILL) in tid 6372 (xray-init), pid 6345 (app.farvater)
    10-09 00:36:12.538  6345  6377 E ndk_translation: Undefined instruction 0xd53be001 at 0x000071eec832a8b8
    10-09 00:36:12.539  6345  6376 E ndk_translation: Undefined instruction 0xd53be001 at 0x000071eec832a8b8
    10-09 00:36:12.616  6345  6371 E ndk_translation: Undefined instruction 0xd5380000 at 0x000071eec881fe30
    10-09 00:36:12.681  6378  6378 F DEBUG   : pid: 6345, tid: 6372, name: xray-init  >>> app.farvater <<<
