
第 1 步.编译 FFmpeg (不支持windows)

配置NDK环境变量：ANDROID_NDK

```java
      cd videoeditor-armv7a/ffmpeg
      sh compile.sh
```

第 2 步.

```java
      run example
```

windows 下不支持编译操作，为方便 windows 下开发，第 2 步运行所需 so 库已添加至 git 仓库.

run example 操作会触发 videoeditor-armv7a 的编译工作，自动生成 so 库打包进 apk.

目前 libijkplayer.so 和 libijksdl.so 由 cmake 自动打包进 apk, libijkffmpeg.so 手动指向.


接口设计：

考虑到离屏保存功能需跨界面操作，而播放器的生命周期应随当前界面一同销毁，故有必要将保存模块独立出来。

> 播放器模块 ： 播放视频，添加背景音乐、水印、实时滤镜、转场等效果

> 保存模块   ： 保存功能

播放器实例中记录当前视频的渲染效果，保存模块保存时从播放器中获取渲染效果与原视频结合。

形如：

```java

  Player player = new Player();
  player.setFilter()
  player.setBgMusic()

  VideoState state = player.getCurrentState();

  SaveModule.save(state,outputPath);

```
















