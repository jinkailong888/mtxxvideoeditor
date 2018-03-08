
第 1 步.编译 FFmpeg

配置NDK环境变量：ANDROID_NDK

```java
      cd videoeditor-armv7a/ffmpeg
      sh compile.sh
```

第 2 步.

```java
      run example
```

注意：目前第 1 步无法在 windows 下进行。

run example 操作会触发 videoeditor-armv7a 的编译工作，自动生成 so 库打包进 apk.

目前 libijkplayer.so 和 libijksdl.so 由 cmake 自动打包进 apk, libijkffmpeg.so 手动指向。











