
第 1 步.编译

需配置NDK环境变量, 如：ANDROID_NDK = /home/yhao/ndk/android-ndk-r14b

```java
      cd videoeditor-armv7a/ffmpeg
      sh compile.sh
```

此步会重新编译 x264、aac 及 ffmpeg 库.

windows 下暂不支持编译操作，为方便 windows 下开发，第 2 步运行所需 so 库已添加至 git 仓库.


第 2 步.

```java
      run example
```

此步会重新编译 cpufeatures、ijkj4a、ijkyuv、ijksdl、android-ndk-prof、ijksoundtouch 以及 ijkplayer 库.

libijkplayer.so 和 libijksdl.so 由 cmake 自动打包进 apk, libijkffmpeg.so 手动指向.




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
















