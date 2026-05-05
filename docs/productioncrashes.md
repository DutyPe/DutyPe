Summary of the Issue
The application is experiencing a java.lang.UnsatisfiedLinkError with the message "dlopen failed: library "libsqlcipher.so" not found". This indicates that the Android system failed to load the native library libsqlcipher.so at runtime. This crash specifically occurs within the AppModule.kt file, inside the provideDutyPeDatabase function, which suggests an attempt to initialize a database that relies on this native library. Such issues are moderately common in Android applications that integrate native libraries, especially when dealing with various device architectures or system configurations.
Potential Causes
Missing or Incorrect Native Library Packaging : The libsqlcipher.so file might not be correctly packaged within the application's APK for all target architectures (ABIs). If the device's CPU architecture doesn't find a corresponding native library, this error will occur.
Improper Native Library Loading : The code attempting to load libsqlcipher.so might be executed before the library is available or in an incorrect context.
Corrupted or Incompatible Library : The native library itself might be corrupted, or incompatible with the specific Android version or device architecture.
Device-Specific Issues : While less common, certain device manufacturers or specific Android versions might have stricter policies or unique behaviors regarding native library loading, leading to issues that are not reproducible on other devices. The observation that this issue occurs only on Google devices suggests this might be a contributing factor.
Recommendations for Analysis and Debugging
Verify APK Contents :
Unzip the APK and inspect the lib directory. Ensure that libsqlcipher.so is present in subdirectories corresponding to all supported ABIs (e.g., armeabi-v7a , arm64-v8a , x86 , x86_64 ).
If you only have libsqlcipher.so for a subset of ABIs, devices with other ABIs will crash.
Check Build Configuration :
Examine your build.gradle (module level) to ensure that packagingOptions { pickFirst 'lib/...' } or similar configurations are not accidentally excluding necessary native libraries.
Verify that jniLibs.srcDirs or other source sets correctly point to the location of your native libraries.
Confirm Library Initialization :
Ensure that System.loadLibrary("sqlcipher") (or the equivalent for your database library) is called at an appropriate time, typically early in the application lifecycle, such as in the Application class or before any database operations.
In the provided stack trace, the crash occurs in provideDutyPeDatabase . If this function is part of a dependency injection module (e.g., Hilt/Dagger), ensure that native library loading is handled before the database instance is provided.
Device-Specific Troubleshooting (Google Devices) :
Since the issue is specific to Google devices, test on various Google Pixel models and Android versions to see if it's consistently reproducible.
Investigate if there are any known compatibility issues with SQLCipher or similar native libraries on specific Pixel devices or recent Android versions.
While Android generally standardizes native library loading, subtle differences in the bootloader, SELinux policies, or system libraries on certain devices can sometimes impact dlopen .
Best Practices to Prevent Such Issues
Multi-ABI Support : Always build and package native libraries for all target ABIs that your application supports. This prevents crashes on devices with different CPU architectures.
Use abiFilters in your build.gradle to specify supported ABIs if you want to limit them and reduce APK size, but be aware that devices not matching these filters will not be supported.
android {
    // ...
    defaultConfig {
        // ...
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64' // Example ABIs
        }
    }
}
Expand
Robust Native Library Loading :
Load native libraries in a static block within the class that uses them, or at the earliest possible point in your application's lifecycle (e.g., Application.onCreate() ).
Wrap System.loadLibrary() calls in a try-catch block to gracefully handle UnsatisfiedLinkError and log details for debugging, even if it ultimately results in a crash, it can provide more context.
static {
    try {
        System.loadLibrary("sqlcipher");
    } catch (UnsatisfiedLinkError e) {
        // Log the error for better understanding
        // You might want to crash or show an error message to the user depending on criticality
        Log.e("NativeLibLoader", "Failed to load libsqlcipher.so", e);
        throw e; // Re-throw if the app cannot function without it
    }
}
Expand
Thorough Testing : Test your application on a wide range of devices, including those from various manufacturers and running different Android versions. Emulators can be helpful, but physical devices, especially those that represent your user base, are crucial for identifying device-specific issues.
Android 12 Considerations
Android 12 (API level 31) introduced stricter security measures and changes in how applications interact with the system, including restrictions on non-SDK interfaces and changes to how native libraries might be loaded or linked.
Non-SDK Interface Restrictions : While unlikely to be the primary cause of UnsatisfiedLinkError for a standard System.loadLibrary() call, if libsqlcipher.so internally uses any restricted non-SDK interfaces, this could manifest as other issues, but typically not a "library not found" error.
PAC/BTI on newer ARM CPUs : Newer ARM architectures (like those found in some Google devices) often implement Pointer Authentication Codes (PAC) and Branch Target Identification (BTI) for enhanced security. If libsqlcipher.so is not built correctly with modern toolchains that support these features, it could theoretically lead to runtime issues or crashes, though again, UnsatisfiedLinkError specifically points to loading failure.
Recommendation for Android 12 and Above :
Ensure Up-to-Date Native Libraries : Always use the latest stable version of SQLCipher or any native library you depend on. Library maintainers typically update their native binaries to be compatible with newer Android versions and architectures.
Target API Level : When building your app for Android 12, ensure your targetSdkVersion is set to 31 or higher. This ensures your app is tested against the latest platform behaviors.
Investigate System Logs (Logcat) : When reproducing on an Android 12 Google device, thoroughly examine the logcat output for any additional messages from the system (e.g., linker errors, SELinux denials) that might shed more light on why dlopen failed. Search for messages from linker , system_server , or audit .
Code Snippet Example for Robust Loading (General Best Practice) :
If your AppModule.provideDutyPeDatabase function is where the database instance is created, ensure the native library is loaded before any direct calls to the SQLCipher library that would implicitly try to load it.
// In your AppModule.kt
object AppModule {

    init {
        // Load the native library as early as possible
        // This static block ensures it's attempted to be loaded once the object is initialized
        try {
            System.loadLibrary("sqlcipher")
            Log.d("AppModule", "libsqlcipher.so loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("AppModule", "Failed to load libsqlcipher.so", e)
            // Depending on the criticality, you might want to:
            // 1. Crash the app (as it's currently doing)
            // 2. Fallback to a non-SQLCipher database (if feasible)
            // 3. Show an error to the user and exit
            throw e // Re-throwing ensures the crash report provides context
        }
    }

    @Provides
    @Singleton
    fun provideDutyPeDatabase(
        @ApplicationContext context: Context,
        // ... other dependencies
    ): YourDatabaseType {
        // This function will only be called after the static block has executed.
        // So, if System.loadLibrary fails, this function won't even be reached in a functional state.

        // Your database initialization code that uses SQLCipher
        // e.g., SQLCipherDatabase.builder(...).build()
        return YourDatabaseType(...)
    }
}


Fatal Exception: java.lang.UnsatisfiedLinkError
dlopen failed: library "libsqlcipher.so" not found
          Fatal Exception: java.lang.UnsatisfiedLinkError: dlopen failed: library "libsqlcipher.so" not found
       at java.lang.Runtime.loadLibrary0(Runtime.java:1077)
       at java.lang.Runtime.loadLibrary0(Runtime.java:998)
       at java.lang.System.loadLibrary(System.java:1656)
       at com.example.dutype.di.AppModule.provideDutyPeDatabase(AppModule.kt:160)
       at com.example.dutype.di.AppModule_ProvideDutyPeDatabaseFactory.provideDutyPeDatabase(AppModule_ProvideDutyPeDatabaseFactory.java:45)
       at com.example.dutype.DaggerDutyPeApplication_HiltComponents_SingletonC$SingletonCImpl$SwitchingProvider.get(DaggerDutyPeApplication_HiltComponents_SingletonC.java:1510)
       at dagger.internal.DoubleCheck.get(DoubleCheck.java:47)
       at com.example.dutype.DaggerDutyPeApplication_HiltComponents_SingletonC$SingletonCImpl$SwitchingProvider.get(DaggerDutyPeApplication_HiltComponents_SingletonC.java:1507)
       at dagger.internal.DoubleCheck.get(DoubleCheck.java:47)
       at com.example.dutype.DaggerDutyPeApplication_HiltComponents_SingletonC$SingletonCImpl$SwitchingProvider.get(DaggerDutyPeApplication_HiltComponents_SingletonC.java:1504)
       at dagger.internal.DoubleCheck.get(DoubleCheck.java:47)
       at com.example.dutype.DaggerDutyPeApplication_HiltComponents_SingletonC$SingletonCImpl$SwitchingProvider$3.create(DaggerDutyPeApplication_HiltComponents_SingletonC.java:1499)
       at com.example.dutype.DaggerDutyPeApplication_HiltComponents_SingletonC$SingletonCImpl$SwitchingProvider$3.create(DaggerDutyPeApplication_HiltComponents_SingletonC.java:1496)
       at androidx.hilt.work.HiltWorkerFactory.createWorker(HiltWorkerFactory.java:57)
       at androidx.work.WorkerFactory.createWorkerWithDefaultFallback(WorkerFactory.java:82)
       at androidx.work.impl.WorkerWrapper.runWorker(WorkerWrapper.java:243)
       at androidx.work.impl.WorkerWrapper.run(WorkerWrapper.java:144)
       at androidx.work.impl.utils.SerialExecutorImpl$Task.run(SerialExecutorImpl.java:96)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:920)
        
Firebase Background Thread #1
          Firebase Background Thread #1:
       at dalvik.system.VMStack.getThreadStackTrace(VMStack.java)
       at java.lang.Thread.getStackTrace(Thread.java:1724)
       at java.lang.Thread.getAllStackTraces(Thread.java:1800)
       at com.google.firebase.crashlytics.internal.common.CrashlyticsReportDataCapture.populateThreadsList(CrashlyticsReportDataCapture.java:343)
       at com.google.firebase.crashlytics.internal.common.CrashlyticsReportDataCapture.populateExecutionData(CrashlyticsReportDataCapture.java:314)
       at com.google.firebase.crashlytics.internal.common.CrashlyticsReportDataCapture.populateEventApplicationData(CrashlyticsReportDataCapture.java:261)
       at com.google.firebase.crashlytics.internal.common.CrashlyticsReportDataCapture.captureEventData(CrashlyticsReportDataCapture.java:112)
       at com.google.firebase.crashlytics.internal.common.SessionReportingCoordinator.persistEvent(SessionReportingCoordinator.java:337)
       at com.google.firebase.crashlytics.internal.common.SessionReportingCoordinator.persistFatalEvent(SessionReportingCoordinator.java:130)
       at com.google.firebase.crashlytics.internal.common.CrashlyticsController$2.call(CrashlyticsController.java:212)
       at com.google.firebase.crashlytics.internal.common.CrashlyticsController$2.call(CrashlyticsController.java:198)
       at com.google.firebase.crashlytics.internal.concurrency.CrashlyticsWorker.lambda$submitTask$2(CrashlyticsWorker.java:118)
       at com.google.android.gms.tasks.zze.run(com.google.android.gms:play-services-tasks@@18.1.0:1)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:920)
        
Firebase Blocking Thread #0
          Firebase Blocking Thread #0:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.SynchronousQueue$TransferStack.awaitFulfill(SynchronousQueue.java:461)
       at java.util.concurrent.SynchronousQueue$TransferStack.transfer(SynchronousQueue.java:362)
       at java.util.concurrent.SynchronousQueue.poll(SynchronousQueue.java:937)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1091)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:920)
        
main
          main:
       at androidx.compose.ui.platform.WrappedComposition$setContent$1$1$3.invoke(Wrapper.android.kt:155)
       at androidx.compose.ui.platform.WrappedComposition$setContent$1$1$3.invoke(Wrapper.android.kt:154)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:109)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:35)
       at androidx.compose.runtime.CompositionLocalKt.CompositionLocalProvider(CompositionLocal.kt:401)
       at androidx.compose.ui.platform.WrappedComposition$setContent$1$1.invoke(WrappedComposition.java:154)
       at androidx.compose.ui.platform.WrappedComposition$setContent$1$1.invoke(WrappedComposition.java:133)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:109)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:35)
       at androidx.compose.runtime.ActualJvm_jvmKt.invokeComposable(ActualJvm_jvm.kt:97)
       at androidx.compose.runtime.ComposerImpl.doCompose(Composer.kt:3595)
       at androidx.compose.runtime.ComposerImpl.composeContent$runtime_release(ComposerImpl.java:3522)
       at androidx.compose.runtime.CompositionImpl.composeContent(Composition.kt:743)
       at androidx.compose.runtime.Recomposer.composeInitial$runtime_release(Recomposer.kt:1122)
       at androidx.compose.runtime.CompositionImpl.composeInitial(Composition.kt:649)
       at androidx.compose.runtime.CompositionImpl.setContent(CompositionImpl.java:635)
       at androidx.compose.ui.platform.WrappedComposition$setContent$1.invoke(WrappedComposition.java:133)
       at androidx.compose.ui.platform.WrappedComposition$setContent$1.invoke(WrappedComposition.java:124)
       at androidx.compose.ui.platform.AndroidComposeView.setOnViewTreeOwnersAvailable(AndroidComposeView.android.kt:1625)
       at androidx.compose.ui.platform.WrappedComposition.setContent(Wrapper.android.kt:124)
       at androidx.compose.ui.platform.WrappedComposition.onStateChanged(Wrapper.android.kt:180)
       at androidx.lifecycle.LifecycleRegistry$ObserverWithState.dispatchEvent(LifecycleRegistry.jvm.kt:320)
       at androidx.lifecycle.LifecycleRegistry.addObserver(LifecycleRegistry.jvm.kt:198)
       at androidx.compose.ui.platform.WrappedComposition$setContent$1.invoke(WrappedComposition.java:131)
       at androidx.compose.ui.platform.WrappedComposition$setContent$1.invoke(WrappedComposition.java:124)
       at androidx.compose.ui.platform.AndroidComposeView.onAttachedToWindow(AndroidComposeView.android.kt:1706)
       at android.view.View.dispatchAttachedToWindow(View.java:20753)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3490)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3497)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3497)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3497)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3497)
       at android.view.ViewRootImpl.performTraversals(ViewRootImpl.java:2613)
       at android.view.ViewRootImpl.doTraversal(ViewRootImpl.java:2126)
       at android.view.ViewRootImpl$TraversalRunnable.run(ViewRootImpl.java:8649)
       at android.view.Choreographer$CallbackRecord.run(Choreographer.java:1037)
       at android.view.Choreographer.doCallbacks(Choreographer.java:845)
       at android.view.Choreographer.doFrame(Choreographer.java:780)
       at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:1022)
       at android.os.Handler.handleCallback(Handler.java:938)
       at android.os.Handler.dispatchMessage(Handler.java:99)
       at android.os.Looper.loopOnce(Looper.java:201)
       at android.os.Looper.loop(Looper.java:288)
       at android.app.ActivityThread.main(ActivityThread.java:7842)
       at java.lang.reflect.Method.invoke(Method.java)
       at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:548)
       at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1003)
       at de.robv.android.xp0sed.Xp0sedBridge.main(Xp0sedBridge.java:112)
        
WM.task-3
          WM.task-3:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:920)
        
pool-13-thread-1
          pool-13-thread-1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:920)
        
ReferenceQueueDaemon
          ReferenceQueueDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.Object.wait(Object.java:568)
       at java.lang.Daemons$ReferenceQueueDaemon.runInternal(Daemons.java:217)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:920)
        
DefaultDispatcher-worker-5
          DefaultDispatcher-worker-5:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
DefaultDispatcher-worker-2
          DefaultDispatcher-worker-2:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
DefaultDispatcher-worker-3
          DefaultDispatcher-worker-3:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
TokenRefresher
          TokenRefresher:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loopOnce(Looper.java:161)
       at android.os.Looper.loop(Looper.java:288)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
pool-2-thread-1
          pool-2-thread-1:
       at java.util.HashMap.putVal(HashMap.java:627)
       at java.util.HashMap.put(HashMap.java:611)
       at org.json.JSONObject.put(JSONObject.java:276)
       at com.android.reverse.apimonitor.AbstractContentHookCallback.beforeHookedMethod(AbstractContentHookCallback.java:40)
       at com.android.reverse.apimonitor.AbstractBahaviorHookCallback.beforeHookedMethod(AbstractBahaviorHookCallback.java:27)
       at com.android.reverse.hook.Xp0seHookHelperImpl$Xp0sedBridgeAdapter.beforeHookedMethod(Xp0seHookHelperImpl.java:37)
       at de.robv.android.xp0sed.Xp0sedBridge.handleHookedMethod(Xp0sedBridge.java:362)
       at java.io.FileOutputStream.write(<xps>)
       at sun.nio.cs.StreamEncoder.writeBytes(StreamEncoder.java:221)
       at sun.nio.cs.StreamEncoder.implClose(StreamEncoder.java:316)
       at sun.nio.cs.StreamEncoder.close(StreamEncoder.java:149)
       at java.io.OutputStreamWriter.close(OutputStreamWriter.java:233)
       at com.android.reverse.util.Logger.writeLog(Logger.java:146)
       at com.android.reverse.util.Logger$1.run(Logger.java:91)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:920)
        
WM.task-4
          WM.task-4:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:920)
        
DefaultDispatcher-worker-4
          DefaultDispatcher-worker-4:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
WM.task-1
          WM.task-1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:920)
        
Firebase Background Thread #2
          Firebase Background Thread #2:
       at java.text.SimpleDateFormat.compile(SimpleDateFormat.java:823)
       at java.text.SimpleDateFormat.initialize(SimpleDateFormat.java:717)
       at java.text.SimpleDateFormat.<init>(SimpleDateFormat.java:688)
       at java.text.SimpleDateFormat.<init>(SimpleDateFormat.java:663)
       at com.android.reverse.apimonitor.AbstractContentHookCallback.getTime(AbstractContentHookCallback.java:83)
       at com.android.reverse.apimonitor.AbstractBahaviorHookCallback.afterHookedMethod(AbstractBahaviorHookCallback.java:44)
       at com.android.reverse.apimonitor.PropertyHook$1.afterHookedMethod(PropertyHook.java:73)
       at com.android.reverse.hook.Xp0seHookHelperImpl$Xp0sedBridgeAdapter.afterHookedMethod(Xp0seHookHelperImpl.java:47)
       at de.robv.android.xp0sed.Xp0sedBridge.handleHookedMethod(Xp0sedBridge.java:396)
       at java.lang.System.getProperty(<xps>)
       at sun.security.action.GetPropertyAction.run(GetPropertyAction.java:84)
       at sun.security.action.GetPropertyAction.run(GetPropertyAction.java:49)
       at java.security.AccessController.doPrivileged(AccessController.java:43)
       at java.io.BufferedWriter.<init>(BufferedWriter.java:109)
       at java.io.BufferedWriter.<init>(BufferedWriter.java:88)
       at com.google.firebase.crashlytics.internal.metadata.MetaDataStore.writeKeyData(MetaDataStore.java:105)
       at com.google.firebase.crashlytics.internal.metadata.UserMetadata$SerializeableKeysMap.serializeIfMarked(UserMetadata.java:343)
       at com.google.firebase.crashlytics.internal.metadata.UserMetadata$SerializeableKeysMap.lambda$scheduleSerializationTaskIfNeeded$0(UserMetadata.java:313)
       at com.google.firebase.crashlytics.internal.concurrency.CrashlyticsWorker.lambda$submit$1(CrashlyticsWorker.java:96)
       at com.google.android.gms.tasks.zze.run(com.google.android.gms:play-services-tasks@@18.1.0:1)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:920)
        
FinalizerDaemon
          FinalizerDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:190)
       at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:211)
       at java.lang.Daemons$FinalizerDaemon.runInternal(Daemons.java:273)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:920)
        
FinalizerWatchdogDaemon
          FinalizerWatchdogDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.Object.wait(Object.java:568)
       at java.lang.Daemons$FinalizerWatchdogDaemon.sleepUntilNeeded(Daemons.java:341)
       at java.lang.Daemons$FinalizerWatchdogDaemon.runInternal(Daemons.java:321)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:920)
        
queued-work-looper
          queued-work-looper:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loopOnce(Looper.java:161)
       at android.os.Looper.loop(Looper.java:288)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
ConnectivityThread
          ConnectivityThread:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loopOnce(Looper.java:161)
       at android.os.Looper.loop(Looper.java:288)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
Firebase-Messaging-Topics-Io
          Firebase-Messaging-Topics-Io:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:1120)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:849)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.android.gms.common.util.concurrent.zza.run(zza.java:2)
       at java.lang.Thread.run(Thread.java:920)
        
FirebaseSessions_HandlerThread
          FirebaseSessions_HandlerThread:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loopOnce(Looper.java:161)
       at android.os.Looper.loop(Looper.java:288)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
Firebase Background Thread #3
          Firebase Background Thread #3:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:920)
        
DefaultDispatcher-worker-1
          DefaultDispatcher-worker-1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
Firebase-Messaging-Init
          Firebase-Messaging-Init:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:1120)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:849)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.android.gms.common.util.concurrent.zza.run(zza.java:2)
       at java.lang.Thread.run(Thread.java:920)
        
OkHttp ConnectionPool
          OkHttp ConnectionPool:
       at java.lang.Object.wait(Object.java)
       at com.android.okhttp.ConnectionPool$1.run(ConnectionPool.java:106)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:920)
        
Firebase Background Thread #0
          Firebase Background Thread #0:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:920)
        
Firebase Lite Thread #0
          Firebase Lite Thread #0:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:920)
        

