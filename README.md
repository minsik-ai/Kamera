# Kamera

Sample Android app for showcasing custom implementation of OpenGL camera preview display, written in Kotlin.

## Main Implementation
- [KameraPreviewObserver.kt](https://github.com/trent-dev/Kamera/blob/master/app/src/main/java/com/trent/kamera/kamera/KameraPreviewObserver.kt)
- [KameraActivity.kt](https://github.com/trent-dev/Kamera/blob/master/app/src/main/java/com/trent/kamera/kamera/KameraActivity.kt)
- [CameraExtensions.kt](https://github.com/trent-dev/Kamera/blob/master/app/src/main/java/com/trent/kamera/extensions/CameraExtensions.kt)
- [Shader.kt](https://github.com/trent-dev/Kamera/blob/master/app/src/main/java/com/trent/kamera/gles/shader/Shader.kt)
- [GraphicTypes.kt](https://github.com/trent-dev/Kamera/blob/master/app/src/main/java/com/trent/kamera/customtypes/GraphicTypes.kt)

Structure inspired by `TextureFromCameraActivity` in [Grafika](https://github.com/google/grafika) repository.

## Design Goals

1.   **Stay as close to idiomatic Kotlin as possible.**

     Only place I knowingly depart from idiomatic Kotlin is the use of `lateinit`.
     I decided to use `lateinit` for critical variables to catch bugs faster & fix them,
     rather then letting them pass with use of nullables & `?` keyword.

2.   **Reduce complexity using single Executor & single Class.**

     I intentionally designed main class to be flat, to avoid complexity caused by `Handler`s & `RenderThread`s.
     A single `Executor` replaces most of their functionality.

3.   **Handle all state machines in a separate non-Activity class.**

     Activity lifecycle & Surface lifecycle doesn't match, so there is a need to account for both of them.
     I do this using Android Architecture Components `LifecycleObserver`, to handle all in one single class.
     Grafika implementation handled all this in one Activity, which is fine for a sample app, but lacks extensibility.

4.   **Overcome possible thread problems using Annotations.**

     `@WorkerThread`, `@MainThread` annotations are strategically placed to avoid GL operations on wrong threads.
