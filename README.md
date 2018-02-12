
运行前需配置NDK环境变量：ANDROID_NDK

第1步.

```java
      cd videoeditor-armv7a/ffmpeg
      sh compile.sh
```

第2步.

```java
      run videoeditor-example
```


run videoeditor-example 操作会触发 videoeditor-armv7a 的编译工作，所以可直接运行，无需 build.

目前 libijkplayer.so 和 libijksdl.so 由 cmake 自动打包进 apk, libijkffmpeg.so 手动指向。











