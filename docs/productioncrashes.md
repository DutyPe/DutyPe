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
com.example.dutype.di.AppModule.provideDutyPeDatabase
FinalizerDaemon
          FinalizerDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:190)
       at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:211)
       at java.lang.Daemons$FinalizerDaemon.runInternal(Daemons.java:273)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:920)
        
Firebase Background Thread #2
          Firebase Background Thread #2:
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
        
pool-2-thread-1
          pool-2-thread-1:
       at com.android.reverse.apimonitor.FileHook$2.descParam(FileHook.java:104)
       at com.android.reverse.apimonitor.AbstractContentHookCallback.beforeHookedMethod(AbstractContentHookCallback.java:39)
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
        
FinalizerWatchdogDaemon
          FinalizerWatchdogDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.Object.wait(Object.java:568)
       at java.lang.Daemons$FinalizerWatchdogDaemon.sleepUntilNeeded(Daemons.java:341)
       at java.lang.Daemons$FinalizerWatchdogDaemon.runInternal(Daemons.java:321)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:920)
        
Firebase Lite Thread #2
          Firebase Lite Thread #2:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:920)
        
GoogleApiHandler
          GoogleApiHandler:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loopOnce(Looper.java:161)
       at android.os.Looper.loop(Looper.java:288)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
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
        
DefaultDispatcher-worker-2
          DefaultDispatcher-worker-2:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
queued-work-looper
          queued-work-looper:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loopOnce(Looper.java:161)
       at android.os.Looper.loop(Looper.java:288)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
DefaultDispatcher-worker-4
          DefaultDispatcher-worker-4:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
OkHttp ConnectionPool
          OkHttp ConnectionPool:
       at java.lang.Object.wait(Object.java)
       at com.android.okhttp.ConnectionPool$1.run(ConnectionPool.java:106)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:920)
        
Firebase Lite Thread #3
          Firebase Lite Thread #3:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:920)
        
WM.task-2
          WM.task-2:
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
        
TokenRefresher
          TokenRefresher:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loopOnce(Looper.java:161)
       at android.os.Looper.loop(Looper.java:288)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
Firebase Background Thread #3
          Firebase Background Thread #3:
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
        
DefaultDispatcher-worker-3
          DefaultDispatcher-worker-3:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
ConnectivityThread
          ConnectivityThread:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loopOnce(Looper.java:161)
       at android.os.Looper.loop(Looper.java:288)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
Firebase Background Thread #1
          Firebase Background Thread #1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
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
        
DefaultDispatcher-worker-5
          DefaultDispatcher-worker-5:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
ReferenceQueueDaemon
          ReferenceQueueDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.Object.wait(Object.java:568)
       at java.lang.Daemons$ReferenceQueueDaemon.runInternal(Daemons.java:217)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
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
        
Firebase Lite Thread #1
          Firebase Lite Thread #1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:920)
        
main
          main:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.Object.wait(Object.java:568)
       at android.app.SharedPreferencesImpl.awaitLoadedLocked(SharedPreferencesImpl.java:279)
       at android.app.SharedPreferencesImpl.getString(SharedPreferencesImpl.java:301)
       at com.example.dutype.navigation.StartDestinationCache.read(StartDestinationCache.java:35)
       at com.example.dutype.navigation.MainNavGraphKt.MainNavGraph(MainNavGraph.kt:104)
       at com.example.dutype.MainActivity$onCreate$6$1$1.invoke(MainActivity.java:336)
       at com.example.dutype.MainActivity$onCreate$6$1$1.invoke(MainActivity.java:283)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:109)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:35)
       at androidx.compose.runtime.CompositionLocalKt.CompositionLocalProvider(CompositionLocal.kt:401)
       at com.example.dutype.ui.theme.ResponsiveThemeKt.ResponsiveTheme(ResponsiveTheme.kt:107)
       at com.example.dutype.MainActivity$onCreate$6$1.invoke(MainActivity.kt:283)
       at com.example.dutype.MainActivity$onCreate$6$1.invoke(MainActivity.kt:282)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:109)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:35)
       at androidx.compose.runtime.CompositionLocalKt.CompositionLocalProvider(CompositionLocal.kt:401)
       at androidx.compose.material3.TextKt.ProvideTextStyle(Text.kt:349)
       at androidx.compose.material3.MaterialThemeKt$MaterialTheme$1.invoke(MaterialTheme.kt:69)
       at androidx.compose.material3.MaterialThemeKt$MaterialTheme$1.invoke(MaterialTheme.kt:68)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:109)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:35)
       at androidx.compose.runtime.CompositionLocalKt.CompositionLocalProvider(CompositionLocal.kt:380)
       at androidx.compose.material3.MaterialThemeKt.MaterialTheme(MaterialTheme.kt:60)
       at com.example.dutype.ui.theme.ThemeKt$dutypeTheme$2.invoke(Theme.kt:131)
       at com.example.dutype.ui.theme.ThemeKt$dutypeTheme$2.invoke(Theme.kt:130)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:109)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:35)
       at androidx.compose.runtime.CompositionLocalKt.CompositionLocalProvider(CompositionLocal.kt:380)
       at com.example.dutype.ui.theme.ThemeKt.dutypeTheme(Theme.kt:127)
       at com.example.dutype.MainActivity$onCreate$6.invoke(MainActivity.java:282)
       at com.example.dutype.MainActivity$onCreate$6.invoke(MainActivity.java:245)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:109)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:35)
       at androidx.compose.ui.platform.ComposeView.Content(ComposeView.android.kt:441)
       at androidx.compose.ui.platform.AbstractComposeView$ensureCompositionCreated$1.invoke(AbstractComposeView.java:259)
       at androidx.compose.ui.platform.AbstractComposeView$ensureCompositionCreated$1.invoke(AbstractComposeView.java:258)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:109)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:35)
       at androidx.compose.runtime.CompositionLocalKt.CompositionLocalProvider(CompositionLocal.kt:380)
       at androidx.compose.ui.platform.CompositionLocalsKt.ProvideCommonCompositionLocals(CompositionLocals.kt:216)
       at androidx.compose.ui.platform.AndroidCompositionLocals_androidKt$ProvideAndroidCompositionLocals$3.invoke(AndroidCompositionLocals_android.kt:132)
       at androidx.compose.ui.platform.AndroidCompositionLocals_androidKt$ProvideAndroidCompositionLocals$3.invoke(AndroidCompositionLocals_android.kt:131)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:109)
       at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:35)
       at androidx.compose.runtime.CompositionLocalKt.CompositionLocalProvider(CompositionLocal.kt:380)
       at androidx.compose.ui.platform.AndroidCompositionLocals_androidKt.ProvideAndroidCompositionLocals(AndroidCompositionLocals.android.kt:121)
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
        

3.geohash
"tepemv"
(string)


isAvailable
true
(boolean)



location
(map)


lat
17.4742055
(double)


lng
78.3056688
(double)


rating
5
(int64)


totalRatings
1
(int64)


updatedAt
2 May 2026 at 14:19:33 UTC+5:30
please fix this issue sometimmes in the worker profiels collectioon fields are saving , i  dont know from where they are saving

and alsoi sometimes names are not saving ,those should bbe mandaroty 
3.1 phone roles also not saving the names sometiems

4.theres no address for the workers we are not saving like the wemployer while profilesetup along the location 