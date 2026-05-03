Fatal Exception: java.lang.RuntimeException
Unable to start activity ComponentInfo{com.dutype.app/com.google.android.libraries.places.widget.AutocompleteActivity}: java.lang.IllegalStateException: Cannot find caller. startActivityForResult should be used.
          Fatal Exception: java.lang.RuntimeException: Unable to start activity ComponentInfo{com.dutype.app/com.google.android.libraries.places.widget.AutocompleteActivity}: java.lang.IllegalStateException: Cannot find caller. startActivityForResult should be used.
       at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:3433)
       at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:3607)
       at android.app.servertransaction.LaunchActivityItem.execute(LaunchActivityItem.java:85)
       at android.app.servertransaction.TransactionExecutor.executeCallbacks(TransactionExecutor.java:135)
       at android.app.servertransaction.TransactionExecutor.execute(TransactionExecutor.java:95)
       at android.app.ActivityThread$H.handleMessage(ActivityThread.java:2068)
       at android.os.Handler.dispatchMessage(Handler.java:106)
       at android.os.Looper.loop(Looper.java:223)
       at android.app.ActivityThread.main(ActivityThread.java:7680)
       at java.lang.reflect.Method.invokeNative(Method.java)
       at java.lang.reflect.Method.invoke(Method.java:423)
       at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:592)
       at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:947)
        
Caused by java.lang.IllegalStateException
Cannot find caller. startActivityForResult should be used.
          Caused by java.lang.IllegalStateException: Cannot find caller. startActivityForResult should be used.
       at com.google.android.libraries.places.internal.zzmt.zzp(com.google.android.libraries.places:places@@3.5.0:1)
       at com.google.android.libraries.places.widget.AutocompleteActivity.onCreate(com.google.android.libraries.places:places@@3.5.0:3)
       at android.app.Activity.performCreate(Activity.java:7994)
       at android.app.Activity.performCreate(Activity.java:7978)
       at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1548)
       at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:3406)
       at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:3607)
       at android.app.servertransaction.LaunchActivityItem.execute(LaunchActivityItem.java:85)
       at android.app.servertransaction.TransactionExecutor.executeCallbacks(TransactionExecutor.java:135)
       at android.app.servertransaction.TransactionExecutor.execute(TransactionExecutor.java:95)
       at android.app.ActivityThread$H.handleMessage(ActivityThread.java:2068)
       at android.os.Handler.dispatchMessage(Handler.java:106)
       at android.os.Looper.loop(Looper.java:223)
       at android.app.ActivityThread.main(ActivityThread.java:7680)
       at java.lang.reflect.Method.invokeNative(Method.java)
       at java.lang.reflect.Method.invoke(Method.java:423)
       at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:592)
       at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:947)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
FinalizerWatchdogDaemon
          FinalizerWatchdogDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.Object.wait(Object.java:568)
       at java.lang.Daemons$FinalizerWatchdogDaemon.sleepUntilNeeded(Daemons.java:341)
       at java.lang.Daemons$FinalizerWatchdogDaemon.runInternal(Daemons.java:321)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:923)
        
DefaultDispatcher-worker-1
          DefaultDispatcher-worker-1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
ScionFrontendApi
          ScionFrontendApi:
       at m2.ar.b(:com.google.android.gms.dynamite_dynamiteloader@260931008@26.09.31 (040800-0):1)
       at com.google.android.gms.dynamiteloader.DynamiteLoaderV2.loadModule2NoCrashUtils(:com.google.android.gms.dynamite_dynamiteloader@260931008@26.09.31 (040800-0):113)
       at m2.da.a(:com.google.android.gms.dynamite_dynamiteloader@260931008@26.09.31 (040800-0):73)
       at m2.x.onTransact(:com.google.android.gms.dynamite_dynamiteloader@260931008@26.09.31 (040800-0):21)
       at android.os.Binder.transact(Binder.java:1043)
       at com.google.android.gms.internal.common.zza.zzB(com.google.android.gms:play-services-basement@@18.9.0:2)
       at com.google.android.gms.dynamite.zzq.zzf(com.google.android.gms:play-services-basement@@18.9.0:6)
       at com.google.android.gms.dynamite.DynamiteModule.load(com.google.android.gms:play-services-basement@@18.9.0:46)
       at com.google.android.gms.internal.measurement.zzff.zzf(com.google.android.gms:play-services-measurement-sdk-api@@22.4.0:2)
       at com.google.android.gms.internal.measurement.zzdu.zza(com.google.android.gms:play-services-measurement-sdk-api@@22.4.0:3)
       at com.google.android.gms.internal.measurement.zzeu.run(com.google.android.gms:play-services-measurement-sdk-api@@22.4.0:2)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
DefaultDispatcher-worker-2
          DefaultDispatcher-worker-2:
       at org.json.JSONStringer.string(JSONStringer.java:314)
       at org.json.JSONStringer.key(JSONStringer.java:386)
       at org.json.JSONObject.writeTo(JSONObject.java:734)
       at org.json.JSONStringer.value(JSONStringer.java:246)
       at org.json.JSONArray.writeTo(JSONArray.java:616)
       at org.json.JSONArray.toString(JSONArray.java:587)
       at com.mojito.utility.MojitoMessage.toJSONString(MojitoMessage.java:450)
       at com.mojito.utility.MojitoLogger.logMessage(MojitoLogger.java:135)
       at java.net.URI.init$_aroundBody3$advice(URI.java:53)
       at java.net.URI.<init>(URI.java:664)
       at java.net.URI.<init>(URI.java:770)
       at java.io.File.toURI(File.java:731)
       at libcore.io.ClassPathURLStreamHandler.<init>(ClassPathURLStreamHandler.java:51)
       at dalvik.system.DexPathList$NativeLibraryElement.maybeInit(DexPathList.java:859)
       at dalvik.system.DexPathList$NativeLibraryElement.findNativeLibrary(DexPathList.java:880)
       at dalvik.system.DexPathList.findLibrary(DexPathList.java:595)
       at dalvik.system.BaseDexClassLoader.findLibrary(BaseDexClassLoader.java:281)
       at java.lang.Runtime.loadLibrary0(Runtime.java:1105)
       at java.lang.Runtime.loadLibrary0(Runtime.java:1051)
       at java.lang.System.loadLibrary(System.java:1664)
       at androidx.compose.material3.ThumbNode$measure$3.i(Switch.kt:17)
       at androidx.compose.material3.ThumbNode$measure$3.k(Switch.kt:5)
       at org.chromium.android_webview.AwBrowserProcess.g(chromium-TrichromeWebViewGoogle.apk-stable-443021036:9)
       at com.android.webview.chromium.WebViewChromiumFactoryProvider.e(chromium-TrichromeWebViewGoogle.apk-stable-443021036:86)
       at com.android.webview.chromium.WebViewChromiumFactoryProvider.<init>(chromium-TrichromeWebViewGoogle.apk-stable-443021036:12)
       at com.android.webview.chromium.WebViewChromiumFactoryProviderForR.<init>(chromium-TrichromeWebViewGoogle.apk-stable-443021036:1)
       at com.android.webview.chromium.WebViewChromiumFactoryProviderForR.create(chromium-TrichromeWebViewGoogle.apk-stable-443021036:1)
       at java.lang.reflect.Method.invokeNative(Method.java)
       at java.lang.reflect.Method.invoke(Method.java:423)
       at android.webkit.WebViewFactory.getProvider(WebViewFactory.java:266)
       at android.webkit.WebView.getFactory(WebView.java:2576)
       at android.webkit.WebView.setWebContentsDebuggingEnabled(WebView.java:1999)
       at com.example.dutype.ads.AdManager.initialize(AdManager.kt:91)
       at com.example.dutype.DutyPeApplication.initializeMobileAds(DutyPeApplication.java:446)
       at com.example.dutype.DutyPeApplication.initializeNonCriticalComponents(DutyPeApplication.java:417)
       at com.example.dutype.DutyPeApplication.access$initializeNonCriticalComponents(DutyPeApplication.java:30)
       at com.example.dutype.DutyPeApplication$onCreate$4.invokeSuspend(DutyPeApplication.kt:181)
       at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
       at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:101)
       at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.java:589)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.executeTask(CoroutineScheduler.kt:832)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:720)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
Firebase Blocking Thread #3
          Firebase Blocking Thread #3:
       at org.json.JSONStringer.string(JSONStringer.java:313)
       at org.json.JSONStringer.value(JSONStringer.java:261)
       at org.json.JSONObject.writeTo(JSONObject.java:734)
       at org.json.JSONStringer.value(JSONStringer.java:246)
       at org.json.JSONArray.writeTo(JSONArray.java:616)
       at org.json.JSONStringer.value(JSONStringer.java:242)
       at org.json.JSONObject.writeTo(JSONObject.java:734)
       at org.json.JSONObject.toString(JSONObject.java:702)
       at com.mojito.utility.MojitoMessage.toJSONString(MojitoMessage.java:458)
       at com.mojito.utility.MojitoLogger.logMessage(MojitoLogger.java:135)
       at java.security.MessageDigest.digest_aroundBody13$advice(MessageDigest.java:78)
       at java.security.MessageDigest.digest(MessageDigest.java:1)
       at com.google.android.gms.common.util.AndroidUtilsLight.getPackageCertificateHashBytes(AndroidUtilsLight.java:4)
       at com.google.firebase.appcheck.internal.NetworkClient.getFingerprintHashForPackage(NetworkClient.java:220)
       at com.google.firebase.appcheck.internal.NetworkClient.makeNetworkRequest(NetworkClient.java:162)
       at com.google.firebase.appcheck.internal.NetworkClient.generatePlayIntegrityChallenge(NetworkClient.java:139)
       at com.google.firebase.appcheck.playintegrity.internal.PlayIntegrityAppCheckProvider.lambda$getPlayIntegrityAttestation$3(PlayIntegrityAppCheckProvider.java:108)
       at com.google.android.gms.tasks.zzz.run(com.google.android.gms:play-services-tasks@@18.1.0:1)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:923)
        
FirebaseInstanceId
          FirebaseInstanceId:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireSharedNanos(AbstractQueuedSynchronizer.java:1063)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer.tryAcquireSharedNanos(AbstractQueuedSynchronizer.java:1358)
       at java.util.concurrent.CountDownLatch.await(CountDownLatch.java:278)
       at com.google.android.gms.tasks.zzad.zzb(com.google.android.gms:play-services-tasks@@18.1.0:1)
       at com.google.android.gms.tasks.Tasks.await(com.google.android.gms:play-services-tasks@@18.1.0:18)
       at com.google.firebase.iid.FirebaseInstanceId.awaitTask(com.google.firebase:firebase-iid@@21.1.0:1)
       at com.google.firebase.iid.FirebaseInstanceId.getToken(com.google.firebase:firebase-iid@@21.1.0:9)
       at com.google.firebase.iid.FirebaseInstanceId.blockingGetMasterToken(com.google.firebase:firebase-iid@@21.1.0:1)
       at com.google.firebase.iid.SyncTask.maybeRefreshToken(com.google.firebase:firebase-iid@@21.1.0:3)
       at com.google.firebase.iid.SyncTask.run(com.google.firebase:firebase-iid@@21.1.0:10)
       at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:462)
       at java.util.concurrent.FutureTask.run(FutureTask.java:266)
       at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:301)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.android.gms.common.util.concurrent.zza.run(zza.java:2)
       at java.lang.Thread.run(Thread.java:923)
        
firebase-iid-executor
          firebase-iid-executor:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:868)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireSharedInterruptibly(AbstractQueuedSynchronizer.java:1023)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireSharedInterruptibly(AbstractQueuedSynchronizer.java:1334)
       at java.util.concurrent.CountDownLatch.await(CountDownLatch.java:232)
       at com.google.android.gms.tasks.zzad.zza(com.google.android.gms:play-services-tasks@@18.1.0:1)
       at com.google.android.gms.tasks.Tasks.await(com.google.android.gms:play-services-tasks@@18.1.0:8)
       at com.google.firebase.iid.GmsRpc.setDefaultAttributesToBundle(GmsRpc.java:11)
       at com.google.firebase.iid.GmsRpc.startRpc(GmsRpc.java:1)
       at com.google.firebase.iid.GmsRpc.getToken(GmsRpc.java:1)
       at com.google.firebase.iid.FirebaseInstanceId.lambda$getInstanceId$2$FirebaseInstanceId(FirebaseInstanceId.java:1)
       at com.google.firebase.iid.FirebaseInstanceId$$Lambda$3.start(FirebaseInstanceId.java:200)
       at com.google.firebase.iid.RequestDeduplicator.getOrStartGetTokenRequest(RequestDeduplicator.java:7)
       at com.google.firebase.iid.FirebaseInstanceId.lambda$getInstanceId$3$FirebaseInstanceId(FirebaseInstanceId.java:5)
       at com.google.firebase.iid.FirebaseInstanceId$$Lambda$0.then(com.google.firebase:firebase-iid@@21.1.0:28)
       at com.google.android.gms.tasks.zze.run(com.google.android.gms:play-services-tasks@@18.1.0:1)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.android.gms.common.util.concurrent.zza.run(zza.java:2)
       at java.lang.Thread.run(Thread.java:923)
        
DefaultDispatcher-worker-3
          DefaultDispatcher-worker-3:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
Firebase Background Thread #2
          Firebase Background Thread #2:
       at java.lang.String.getChars(String.java:780)
       at java.lang.AbstractStringBuilder.append(AbstractStringBuilder.java:449)
       at java.lang.StringBuilder.append(StringBuilder.java:137)
       at org.json.JSONStringer.string(JSONStringer.java:313)
       at org.json.JSONStringer.key(JSONStringer.java:386)
       at org.json.JSONObject.writeTo(JSONObject.java:734)
       at org.json.JSONStringer.value(JSONStringer.java:246)
       at org.json.JSONArray.writeTo(JSONArray.java:616)
       at org.json.JSONStringer.value(JSONStringer.java:242)
       at org.json.JSONObject.writeTo(JSONObject.java:734)
       at org.json.JSONObject.toString(JSONObject.java:702)
       at com.mojito.utility.MojitoMessage.toJSONString(MojitoMessage.java:458)
       at com.mojito.utility.MojitoLogger.logMessage(MojitoLogger.java:135)
       at java.io.FileOutputStream.init$_aroundBody3$advice(FileOutputStream.java:35)
       at java.io.FileOutputStream.<init>(FileOutputStream.java:220)
       at java.io.FileOutputStream.<init>(FileOutputStream.java:186)
       at com.google.firebase.crashlytics.internal.metadata.MetaDataStore.writeKeyData(MetaDataStore.java:105)
       at com.google.firebase.crashlytics.internal.metadata.UserMetadata$SerializeableKeysMap.serializeIfMarked(UserMetadata.java:343)
       at com.google.firebase.crashlytics.internal.metadata.UserMetadata$SerializeableKeysMap.lambda$scheduleSerializationTaskIfNeeded$0(UserMetadata.java:313)
       at com.google.firebase.crashlytics.internal.concurrency.CrashlyticsWorker.lambda$submit$1(CrashlyticsWorker.java:96)
       at com.google.android.gms.tasks.zze.run(com.google.android.gms:play-services-tasks@@18.1.0:1)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:923)
        
GmsDynamite
          GmsDynamite:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.Object.wait(Object.java:568)
       at com.google.android.gms.dynamite.zza.run(com.google.android.gms:play-services-basement@@18.9.0:2)
        
FinalizerDaemon
          FinalizerDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:190)
       at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:211)
       at java.lang.Daemons$FinalizerDaemon.runInternal(Daemons.java:273)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:923)
        
pool-11-thread-1
          pool-11-thread-1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
pool-6-thread-1
          pool-6-thread-1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
queued-work-looper
          queued-work-looper:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loop(Looper.java:183)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
Firebase Background Thread #3
          Firebase Background Thread #3:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.FutureTask.awaitDone(FutureTask.java:450)
       at java.util.concurrent.FutureTask.get(FutureTask.java:192)
       at com.google.firebase.crashlytics.internal.settings.SettingsController$1.then(SettingsController.java:206)
       at com.google.firebase.crashlytics.internal.settings.SettingsController$1.then(SettingsController.java:194)
       at com.google.android.gms.tasks.zzo.run(com.google.android.gms:play-services-tasks@@18.1.0:1)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:923)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
WM.task-1
          WM.task-1:
       at com.mojito.utility.MojitoMessage.fillCallStack(MojitoMessage.java:345)
       at com.mojito.utility.MojitoLogger.logMessage(MojitoLogger.java:128)
       at java.util.Locale.getLanguage_aroundBody1$advice(Locale.java:42)
       at java.util.Locale.getLanguage(Locale.java:1)
       at java.lang.CaseMapper.toUpperCase(CaseMapper.java:147)
       at java.lang.String.toUpperCase(String.java:2742)
       at android.database.DatabaseUtils.getSqlStatementType(DatabaseUtils.java:1571)
       at android.database.sqlite.SQLiteConnection.acquirePreparedStatement(SQLiteConnection.java:1048)
       at android.database.sqlite.SQLiteConnection.executeForString(SQLiteConnection.java:788)
       at android.database.sqlite.SQLiteConnection.setLocaleFromConfiguration(SQLiteConnection.java:463)
       at android.database.sqlite.SQLiteConnection.open(SQLiteConnection.java:261)
       at android.database.sqlite.SQLiteConnection.open(SQLiteConnection.java:205)
       at android.database.sqlite.SQLiteConnectionPool.openConnectionLocked(SQLiteConnectionPool.java:505)
       at android.database.sqlite.SQLiteConnectionPool.tryAcquireNonPrimaryConnectionLocked(SQLiteConnectionPool.java:989)
       at android.database.sqlite.SQLiteConnectionPool.waitForConnection(SQLiteConnectionPool.java:695)
       at android.database.sqlite.SQLiteConnectionPool.acquireConnection(SQLiteConnectionPool.java:380)
       at android.database.sqlite.SQLiteSession.acquireConnection(SQLiteSession.java:896)
       at android.database.sqlite.SQLiteSession.prepare(SQLiteSession.java:588)
       at android.database.sqlite.SQLiteProgram.<init>(SQLiteProgram.java:61)
       at android.database.sqlite.SQLiteQuery.<init>(SQLiteQuery.java:37)
       at android.database.sqlite.SQLiteDirectCursorDriver.query(SQLiteDirectCursorDriver.java:46)
       at android.database.sqlite.SQLiteDatabase.rawQueryWithFactory(SQLiteDatabase.java:1545)
       at android.database.sqlite.SQLiteDatabase.rawQueryWithFactory(SQLiteDatabase.java:1520)
       at androidx.sqlite.db.framework.FrameworkSQLiteDatabase.query$lambda$1(FrameworkSQLiteDatabase.android.kt:179)
       at androidx.sqlite.db.framework.FrameworkSQLiteDatabase.query(FrameworkSQLiteDatabase.android.kt:179)
       at androidx.room.RoomDatabase.query(RoomDatabase.kt:484)
       at androidx.room.util.DBUtil.query(DBUtil.java:75)
       at androidx.work.impl.model.SystemIdInfoDao_Impl.getWorkSpecIds(SystemIdInfoDao_Impl.java:166)
       at androidx.work.impl.background.systemjob.SystemJobScheduler.reconcileJobs(SystemJobScheduler.java:310)
       at androidx.work.impl.utils.ForceStopRunnable.cleanUp(ForceStopRunnable.java:289)
       at androidx.work.impl.utils.ForceStopRunnable.forceStopRunnable(ForceStopRunnable.java:252)
       at androidx.work.impl.utils.ForceStopRunnable.run(ForceStopRunnable.java:135)
       at androidx.work.impl.utils.SerialExecutorImpl$Task.run(SerialExecutorImpl.java:96)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
Thread-2
          Thread-2:
       at com.google.android.gms.chimera.DynamiteModuleInitializer.initializeModuleV2(:com.google.android.gms.policy_ads_fdr_dynamite@253405701@253405700000.794833465.794833465:4)
       at java.lang.reflect.Method.invokeNative(Method.java)
       at java.lang.reflect.Method.invoke(Method.java:423)
       at m2.al.a(:com.google.android.gms.dynamite_dynamiteloader@260931008@26.09.31 (040800-0):2)
       at com.google.android.gms.chimera.container.DynamiteModuleApi.onApkLoaded(:com.google.android.gms.dynamite_dynamiteloader@260931008@26.09.31 (040800-0):61)
       at m2.ar.b(:com.google.android.gms.dynamite_dynamiteloader@260931008@26.09.31 (040800-0):74)
       at com.google.android.gms.dynamiteloader.DynamiteLoaderV2.loadModule2NoCrashUtils(:com.google.android.gms.dynamite_dynamiteloader@260931008@26.09.31 (040800-0):113)
       at m2.da.a(:com.google.android.gms.dynamite_dynamiteloader@260931008@26.09.31 (040800-0):73)
       at m2.x.onTransact(:com.google.android.gms.dynamite_dynamiteloader@260931008@26.09.31 (040800-0):21)
       at android.os.Binder.transact(Binder.java:1043)
       at com.google.android.gms.internal.common.zza.zzB(com.google.android.gms:play-services-basement@@18.9.0:2)
       at com.google.android.gms.dynamite.zzq.zzf(com.google.android.gms:play-services-basement@@18.9.0:6)
       at com.google.android.gms.dynamite.DynamiteModule.load(com.google.android.gms:play-services-basement@@18.9.0:46)
       at com.google.android.gms.ads.internal.util.client.zzs.zzc(zzs.java:1)
       at com.google.android.gms.ads.internal.util.client.zzs.zzb(zzs.java:1)
       at com.google.android.gms.internal.ads.zzbov.run(com.google.android.gms:play-services-ads-lite@@23.6.0:13)
       at java.lang.Thread.run(Thread.java:923)
        
ReferenceQueueDaemon
          ReferenceQueueDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.Object.wait(Object.java:568)
       at java.lang.Daemons$ReferenceQueueDaemon.runInternal(Daemons.java:217)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:923)
        
MessengerIpcClient
          MessengerIpcClient:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2109)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:1132)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:849)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.android.gms.common.util.concurrent.zza.run(zza.java:2)
       at java.lang.Thread.run(Thread.java:923)
        
Firebase Blocking Thread #1
          Firebase Blocking Thread #1:
       at com.mojito.utility.MojitoMessage.fillCallStack(MojitoMessage.java:345)
       at com.mojito.utility.MojitoLogger.logMessage(MojitoLogger.java:128)
       at java.security.MessageDigest.digest_aroundBody9$advice(MessageDigest.java:55)
       at java.security.MessageDigest.digest(MessageDigest.java:1)
       at java.security.MessageDigest.digest_aroundBody12(MessageDigest.java:448)
       at java.security.MessageDigest.digest_aroundBody13$advice(MessageDigest.java:66)
       at java.security.MessageDigest.digest(MessageDigest.java:1)
       at com.android.org.conscrypt.ct.CTLogInfo.<init>(CTLogInfo.java:45)
       at com.android.org.conscrypt.ct.CTLogStoreImpl.createDefaultFallbackLogs(CTLogStoreImpl.java:158)
       at com.android.org.conscrypt.ct.CTLogStoreImpl.getDefaultFallbackLogs(CTLogStoreImpl.java:147)
       at com.android.org.conscrypt.ct.CTLogStoreImpl.<init>(CTLogStoreImpl.java:88)
       at com.android.org.conscrypt.Platform.newDefaultLogStore(Platform.java:512)
       at com.android.org.conscrypt.TrustManagerImpl.<init>(TrustManagerImpl.java:219)
       at com.android.org.conscrypt.TrustManagerImpl.<init>(TrustManagerImpl.java:175)
       at com.android.org.conscrypt.TrustManagerImpl.<init>(TrustManagerImpl.java:169)
       at android.security.net.config.NetworkSecurityTrustManager.<init>(NetworkSecurityTrustManager.java:60)
       at android.security.net.config.NetworkSecurityConfig.getTrustManager(NetworkSecurityConfig.java:114)
       at android.security.net.config.RootTrustManager.checkServerTrusted(RootTrustManager.java:90)
       at com.android.org.conscrypt.ConscryptEngineSocket$2.checkServerTrusted(ConscryptEngineSocket.java:161)
       at com.android.org.conscrypt.Platform.checkServerTrusted(Platform.java:250)
       at com.android.org.conscrypt.ConscryptEngine.verifyCertificateChain(ConscryptEngine.java:1644)
       at com.android.org.conscrypt.NativeCrypto.ENGINE_SSL_read_direct(NativeCrypto.java)
       at com.android.org.conscrypt.NativeSsl.readDirectByteBuffer(NativeSsl.java:568)
       at com.android.org.conscrypt.ConscryptEngine.readPlaintextDataDirect(ConscryptEngine.java:1095)
       at com.android.org.conscrypt.ConscryptEngine.readPlaintextData(ConscryptEngine.java:1079)
       at com.android.org.conscrypt.ConscryptEngine.unwrap(ConscryptEngine.java:876)
       at com.android.org.conscrypt.ConscryptEngine.unwrap(ConscryptEngine.java:747)
       at com.android.org.conscrypt.ConscryptEngine.unwrap(ConscryptEngine.java:712)
       at com.android.org.conscrypt.ConscryptEngineSocket$SSLInputStream.processDataFromSocket(ConscryptEngineSocket.java:849)
       at com.android.org.conscrypt.ConscryptEngineSocket$SSLInputStream.access$100(ConscryptEngineSocket.java:722)
       at com.android.org.conscrypt.ConscryptEngineSocket.doHandshake(ConscryptEngineSocket.java:238)
       at com.android.org.conscrypt.ConscryptEngineSocket.startHandshake(ConscryptEngineSocket.java:217)
       at com.android.okhttp.internal.io.RealConnection.connectTls(RealConnection.java:196)
       at com.android.okhttp.internal.io.RealConnection.connectSocket(RealConnection.java:153)
       at com.android.okhttp.internal.io.RealConnection.connect(RealConnection.java:116)
       at com.android.okhttp.internal.http.StreamAllocation.findConnection(StreamAllocation.java:186)
       at com.android.okhttp.internal.http.StreamAllocation.findHealthyConnection(StreamAllocation.java:128)
       at com.android.okhttp.internal.http.StreamAllocation.newStream(StreamAllocation.java:97)
       at com.android.okhttp.internal.http.HttpEngine.connect(HttpEngine.java:289)
       at com.android.okhttp.internal.http.HttpEngine.sendRequest(HttpEngine.java:232)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.execute(HttpURLConnectionImpl.java:465)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.connect(HttpURLConnectionImpl.java:131)
       at com.android.okhttp.internal.huc.DelegatingHttpsURLConnection.connect(DelegatingHttpsURLConnection.java:90)
       at com.android.okhttp.internal.huc.HttpsURLConnectionImpl.connect(HttpsURLConnectionImpl.java:30)
       at com.google.firebase.crashlytics.internal.network.HttpGetRequest.execute(HttpGetRequest.java:80)
       at com.google.firebase.crashlytics.internal.settings.DefaultSettingsSpiCall.invoke(DefaultSettingsSpiCall.java:114)
       at com.google.firebase.crashlytics.internal.settings.SettingsController$1.lambda$then$0(SettingsController.java:204)
       at java.util.concurrent.FutureTask.run(FutureTask.java:266)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:923)
        
Firebase Blocking Thread #2
          Firebase Blocking Thread #2:
       at com.mojito.utility.MojitoConfig.getAppUid(MojitoConfig.java:413)
       at com.mojito.utility.MojitoLogger.logMessage(MojitoLogger.java:135)
       at java.net.InetSocketAddress.init$_aroundBody1$advice(InetSocketAddress.java:30)
       at java.net.InetSocketAddress.<init>(InetSocketAddress.java:197)
       at com.android.okhttp.internal.http.RouteSelector.resetNextInetSocketAddress(RouteSelector.java:181)
       at com.android.okhttp.internal.http.RouteSelector.nextProxy(RouteSelector.java:144)
       at com.android.okhttp.internal.http.RouteSelector.next(RouteSelector.java:86)
       at com.android.okhttp.internal.http.StreamAllocation.findConnection(StreamAllocation.java:176)
       at com.android.okhttp.internal.http.StreamAllocation.findHealthyConnection(StreamAllocation.java:128)
       at com.android.okhttp.internal.http.StreamAllocation.newStream(StreamAllocation.java:97)
       at com.android.okhttp.internal.http.HttpEngine.connect(HttpEngine.java:289)
       at com.android.okhttp.internal.http.HttpEngine.sendRequest(HttpEngine.java:232)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.execute(HttpURLConnectionImpl.java:465)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.connect(HttpURLConnectionImpl.java:131)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.getOutputStream(HttpURLConnectionImpl.java:262)
       at com.android.okhttp.internal.huc.DelegatingHttpsURLConnection.getOutputStream(DelegatingHttpsURLConnection.java:219)
       at com.android.okhttp.internal.huc.HttpsURLConnectionImpl.getOutputStream(HttpsURLConnectionImpl.java:30)
       at com.google.firebase.installations.remote.FirebaseInstallationServiceClient.writeRequestBodyToOutputStream(FirebaseInstallationServiceClient.java:235)
       at com.google.firebase.installations.remote.FirebaseInstallationServiceClient.writeFIDCreateRequestBodyToOutputStream(FirebaseInstallationServiceClient.java:217)
       at com.google.firebase.installations.remote.FirebaseInstallationServiceClient.createFirebaseInstallation(FirebaseInstallationServiceClient.java:175)
       at com.google.firebase.installations.FirebaseInstallations.registerFidWithServer(FirebaseInstallations.java:533)
       at com.google.firebase.installations.FirebaseInstallations.doNetworkCallIfNecessary(FirebaseInstallations.java:387)
       at com.google.firebase.installations.FirebaseInstallations.lambda$doRegistrationOrRefresh$3(FirebaseInstallations.java:372)
       at com.google.firebase.concurrent.SequentialExecutor$1.run(SequentialExecutor.java:117)
       at com.google.firebase.concurrent.SequentialExecutor$QueueWorker.workOnQueue(SequentialExecutor.java:229)
       at com.google.firebase.concurrent.SequentialExecutor$QueueWorker.run(SequentialExecutor.java:174)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:923)
        
OkHttp ConnectionPool
          OkHttp ConnectionPool:
       at java.lang.Object.wait(Object.java)
       at com.android.okhttp.ConnectionPool$1.run(ConnectionPool.java:106)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
Firebase Background Thread #0
          Firebase Background Thread #0:
       at dalvik.system.VMStack.getThreadStackTrace(VMStack.java)
       at java.lang.Thread.getStackTrace(Thread.java:1736)
       at java.lang.Thread.getAllStackTraces(Thread.java:1812)
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
       at java.lang.Thread.run(Thread.java:923)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
TokenRefresher
          TokenRefresher:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loop(Looper.java:183)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
and ghis is ai insight gemininfireabse one
This crash, java.lang.IllegalStateException: Cannot find caller. startActivityForResult should be used. , indicates a common issue when integrating with external libraries or APIs that expect a specific context for launching activities and receiving results.
Cause of the Issue:
The IllegalStateException with the message "Cannot find caller. startActivityForResult should be used." typically occurs when an activity launch (like startActivityForResult ) is attempted from a context that is not a direct Activity instance. This often happens when:
Launching from a Non-Activity Context: You might be attempting to launch an activity for a result from a Context obtained from getApplicationContext() , a Service , or a custom View where the direct parent Activity is not accessible.
Incorrect Fragment Usage: When using Fragments, startActivityForResult should be called on the Fragment instance itself ( fragment.startActivityForResult(...) ) rather than getActivity().startActivityForResult(...) . The library likely attempts to resolve the calling activity to properly handle the result, and if it cannot find it, this exception is thrown.
Library-Specific Requirements: The com.google.android.libraries.places.places library (Google Places API) specifically requires a valid Activity context to initiate its UI components, especially when expecting a result (e.g., selecting a place). It internally tries to find the calling activity to manage the onActivityResult callback.
The stack trace points to com.google.android.libraries.places.internal.zzmt.zzp , which suggests an internal method within the Google Places library is encountering this problem when trying to initiate a flow that requires an activity result.
Recommendations to Debug and Resolve:
Identify the Call Site:
Examine your code where you are calling any methods from com.dutype.app that interact with the Google Places library ( com.google.android.libraries.placesplaces ).
Specifically look for calls that might initiate a UI flow (e.g., Autocomplete.IntentBuilder.build() , PlacePicker.IntentBuilder.build() , or similar methods that return an Intent that you then pass to startActivityForResult ).
Verify the Context being passed to these library methods or the Context from which startActivityForResult is being invoked.
Ensure an Activity Context is Used:
From an Activity : If you are calling the Places API directly from an Activity , ensure you are using this (the Activity instance) as the Context for building the Intent and for calling startActivityForResult .
// Example for Autocomplete
Autocomplete.IntentBuilder intentBuilder = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields);
Intent intent = intentBuilder.build(this); // 'this' refers to the Activity
startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
From a Fragment : If calling from a Fragment , use the Fragment 's own startActivityForResult() method.
// Example for Autocomplete within a Fragment
Autocomplete.IntentBuilder intentBuilder = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields);
Intent intent = intentBuilder.build(getContext()); // or requireContext()
startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE); // Call on the Fragment
Avoid calling getActivity().startActivityForResult() from within a Fragment when dealing with APIs that expect the fragment to handle the result.
Review onActivityResult Handling:
Ensure that the onActivityResult method is correctly implemented in the Activity or Fragment that calls startActivityForResult . The Places SDK will deliver its results back to this method.
Actionable Next Step:
Locate the code in your com.dutype.app library where you are initiating a Google Places API UI flow (e.g., place autocomplete, place picker). Verify that startActivityForResult is being called from a direct Activity or Fragment instance, ensuring that the Context provided to the Places API builder is indeed an Activity context.
Best Practices to Avoid Such Issues:
Context Management: Be mindful of the Context you are using. Generally, for UI-related operations, an Activity context is preferred or even required. Use getApplicationContext() sparingly for UI operations, as it lacks the lifecycle of an Activity and can lead to issues like this.
Fragment startActivityForResult : Always call startActivityForResult directly on the Fragment instance ( fragment.startActivityForResult(...) ) when launching an activity for a result from within a Fragment . This ensures that the result is routed back to the correct Fragment 's onActivityResult method.
Library Documentation: When integrating with external libraries, always consult their official documentation for specific Context requirements and best practices for launching UI components or activities that expect results.
Testing Contexts: During development, if you encounter IllegalStateException related to Context , double-check the type of Context being used at the point of the call.
Dependency Injection: If you use dependency injection, ensure that Activity scoped contexts are provided when UI interaction is necessary, rather than application-scoped contexts.

2.
This crash, java.lang.AbstractMethodError: abstract method "void androidx.compose.ui.node.DrawModifierNode.a(androidx.compose.ui.node.LayoutNodeDrawScope)" on receiver java.lang.Class<androidx.compose.foundation.text.modifiers.TextAnnotatedStringNode> , indicates an incompatibility issue between different versions of Compose libraries, specifically between the DrawModifierNode interface and its implementation within TextAnnotatedStringNode .
Cause of the Issue:
AbstractMethodError occurs when a class attempts to call an abstract method that has been added to an interface in a newer version, but the class itself was compiled against an older version of that interface where the method did not exist. In this specific scenario:
Compose Library Mismatch: The application is likely using a version of androidx.compose.foundation (where TextAnnotatedStringNode resides) that was compiled against an older version of the Compose UI library.
Interface Evolution: A newer version of androidx.compose.ui.node.DrawModifierNode (or a related internal interface) has introduced a new abstract method (represented here as a(androidx.compose.ui.node.LayoutNodeDrawScope) ).
Runtime Incompatibility: When the application runs, the TextAnnotatedStringNode class (from the older foundation library) is loaded alongside the newer ui library. The newer ui library expects TextAnnotatedStringNode to implement the new abstract method from DrawModifierNode . Since the older TextAnnotatedStringNode doesn't have this implementation, an AbstractMethodError is thrown.
This often happens when dependency versions are not perfectly aligned, or when a transitive dependency pulls in an older version of a Compose module.
Recommendations to Avoid the Issue:
Unified Compose Bill of Materials (BOM): Always use the Compose BOM to manage your Compose dependencies. The BOM ensures that all Compose libraries are on compatible versions. This is the most effective way to prevent version mismatches.
// build.gradle (app-level)
dependencies {
    val composeBom = platform('androidx.compose:compose-bom:2023.08.00') // Use the latest stable version
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Then declare Compose dependencies without specifying versions
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.ui:ui-graphics'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.compose.foundation:foundation' // Your crashing library
    // ... other Compose dependencies
}
Expand
Explicit Dependency Management: If you cannot use the BOM for some reason, explicitly define the version for all Compose libraries in your build.gradle file, ensuring they all use the exact same major.minor.patch version. This is much harder to maintain than using the BOM.
Dependency Tree Analysis: Regularly analyze your dependency tree ( ./gradlew app:dependencies ) to identify any conflicting versions or unexpected transitive dependencies that might be pulling in older Compose modules. Address these conflicts by explicitly excluding the problematic transitive dependency or by forcing a specific version.
Keep Dependencies Updated: Regularly update your Compose libraries and Android Gradle Plugin (AGP) to the latest stable versions. This helps you benefit from bug fixes and improved compatibility.
Clean Builds: After updating dependencies or changing build configurations, always perform a clean build ( ./gradlew clean build ) to ensure all old artifacts are removed and new ones are correctly compiled.
Actionable Next Step:
The most direct and recommended action to resolve this specific AbstractMethodError is to unify your Compose dependency versions using the Compose Bill of Materials (BOM) .
Identify Current Versions: Check your build.gradle files (both project and app-level) to see what versions of Compose libraries are currently being used.
Implement Compose BOM:
Add the compose-bom dependency to your build.gradle file (app-level module). Choose the latest stable version.
Remove explicit version numbers from all individual Compose library dependencies (e.g., androidx.compose.ui:ui , androidx.compose.foundation:foundation , androidx.compose.material:material , etc.). The BOM will then manage these versions.
Clean and Rebuild: Execute ./gradlew clean followed by ./gradlew assembleDebug (or assembleRelease ) to ensure a fresh build with the new dependency configuration.
This approach will ensure that all Compose modules are on compatible versions, eliminating the AbstractMethodError caused by an interface mismatch.


Fatal Exception: java.lang.AbstractMethodError
abstract method "void androidx.compose.ui.node.DrawModifierNode.a(androidx.compose.ui.node.LayoutNodeDrawScope)"
          Fatal Exception: java.lang.AbstractMethodError: abstract method "void androidx.compose.ui.node.DrawModifierNode.a(androidx.compose.ui.node.LayoutNodeDrawScope)"
       at androidx.compose.ui.node.LayoutNodeDrawScope.drawDirect-eZhPAX0$ui_release(LayoutNodeDrawScope.kt:110)
       at androidx.compose.ui.node.NodeCoordinator.getIntroducesMotionFrameOfReference(NodeCoordinator.kt:89)
       at androidx.compose.ui.node.LayoutNodeDrawScope.draw-eZhPAX0$ui_release(LayoutNodeDrawScope.java:89)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:450)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.access$drawContainedDrawModifiers(NodeCoordinator.kt:58)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:469)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.runtime.snapshots.Snapshot$Companion.observe(Snapshot.java:503)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver$ObservedScopeMap.observe(SnapshotStateObserver.kt:502)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver.observeReads(SnapshotStateObserver.kt:258)
       at androidx.compose.ui.node.OwnerSnapshotObserver.observeReads$ui_release(OwnerSnapshotObserver.kt:133)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:466)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:291)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:289)
       at androidx.compose.ui.graphics.layer.GraphicsLayerV29.record(GraphicsLayerV29.android.kt:245)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.recordInternal(AndroidGraphicsLayer.android.kt:430)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.record-mL-hObY(GraphicsLayer.java:423)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.updateDisplayList(GraphicsLayerOwnerLayer.android.kt:284)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.drawLayer(GraphicsLayerOwnerLayer.android.kt:229)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:434)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutNode.draw$ui_release(LayoutNode.kt:999)
       at androidx.compose.ui.node.InnerNodeCoordinator.performDraw(InnerNodeCoordinator.kt:196)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutNode.draw$ui_release(LayoutNode.kt:999)
       at androidx.compose.ui.node.InnerNodeCoordinator.performDraw(InnerNodeCoordinator.kt:196)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.access$drawContainedDrawModifiers(NodeCoordinator.kt:58)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:469)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.runtime.snapshots.Snapshot$Companion.observe(Snapshot.java:503)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver$ObservedScopeMap.observe(SnapshotStateObserver.kt:502)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver.observeReads(SnapshotStateObserver.kt:258)
       at androidx.compose.ui.node.OwnerSnapshotObserver.observeReads$ui_release(OwnerSnapshotObserver.kt:133)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:466)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:291)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:289)
       at androidx.compose.ui.graphics.layer.GraphicsLayerV29.record(GraphicsLayerV29.android.kt:245)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.recordInternal(AndroidGraphicsLayer.android.kt:430)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.record-mL-hObY(GraphicsLayer.java:423)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.updateDisplayList(GraphicsLayerOwnerLayer.android.kt:284)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.drawLayer(GraphicsLayerOwnerLayer.android.kt:229)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:434)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.access$drawContainedDrawModifiers(NodeCoordinator.kt:58)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:469)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.runtime.snapshots.Snapshot$Companion.observe(Snapshot.java:503)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver$ObservedScopeMap.observe(SnapshotStateObserver.kt:502)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver.observeReads(SnapshotStateObserver.kt:258)
       at androidx.compose.ui.node.OwnerSnapshotObserver.observeReads$ui_release(OwnerSnapshotObserver.kt:133)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:466)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:291)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:289)
       at androidx.compose.ui.graphics.layer.GraphicsLayerV29.record(GraphicsLayerV29.android.kt:245)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.recordInternal(AndroidGraphicsLayer.android.kt:430)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.record-mL-hObY(GraphicsLayer.java:423)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.updateDisplayList(GraphicsLayerOwnerLayer.android.kt:284)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.drawLayer(GraphicsLayerOwnerLayer.android.kt:229)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:434)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutNode.draw$ui_release(LayoutNode.kt:999)
       at androidx.compose.ui.node.InnerNodeCoordinator.performDraw(InnerNodeCoordinator.kt:196)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutNode.draw$ui_release(LayoutNode.kt:999)
       at androidx.compose.ui.node.InnerNodeCoordinator.performDraw(InnerNodeCoordinator.kt:196)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.access$drawContainedDrawModifiers(NodeCoordinator.kt:58)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:469)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.runtime.snapshots.Snapshot$Companion.observe(Snapshot.java:503)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver$ObservedScopeMap.observe(SnapshotStateObserver.kt:502)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver.observeReads(SnapshotStateObserver.kt:258)
       at androidx.compose.ui.node.OwnerSnapshotObserver.observeReads$ui_release(OwnerSnapshotObserver.kt:133)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:466)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:291)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:289)
       at androidx.compose.ui.graphics.layer.GraphicsLayerV29.record(GraphicsLayerV29.android.kt:245)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.recordInternal(AndroidGraphicsLayer.android.kt:430)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.record-mL-hObY(GraphicsLayer.java:423)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.updateDisplayList(GraphicsLayerOwnerLayer.android.kt:284)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.drawLayer(GraphicsLayerOwnerLayer.android.kt:229)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:434)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.LayoutNodeDrawScope.drawContent(LayoutNodeDrawScope.kt:68)
       at androidx.compose.foundation.DrawGlowOverscrollModifier.draw(AndroidOverscroll.android.kt:330)
       at androidx.compose.ui.node.BackwardsCompatNode.draw(BackwardsCompatNode.kt:350)
       at androidx.compose.ui.node.LayoutNodeDrawScope.drawDirect-eZhPAX0$ui_release(LayoutNodeDrawScope.kt:110)
       at androidx.compose.ui.node.NodeCoordinator.getIntroducesMotionFrameOfReference(NodeCoordinator.kt:89)
       at androidx.compose.ui.node.LayoutNodeDrawScope.draw-eZhPAX0$ui_release(LayoutNodeDrawScope.java:89)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:450)
       at androidx.compose.ui.node.NodeCoordinator.access$drawContainedDrawModifiers(NodeCoordinator.kt:58)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:469)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.runtime.snapshots.Snapshot$Companion.observe(Snapshot.java:503)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver$ObservedScopeMap.observe(SnapshotStateObserver.kt:502)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver.observeReads(SnapshotStateObserver.kt:258)
       at androidx.compose.ui.node.OwnerSnapshotObserver.observeReads$ui_release(OwnerSnapshotObserver.kt:133)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:466)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:291)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:289)
       at androidx.compose.ui.graphics.layer.GraphicsLayerV29.record(GraphicsLayerV29.android.kt:245)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.recordInternal(AndroidGraphicsLayer.android.kt:430)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.record-mL-hObY(GraphicsLayer.java:423)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.updateDisplayList(GraphicsLayerOwnerLayer.android.kt:284)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.drawLayer(GraphicsLayerOwnerLayer.android.kt:229)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:434)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutNode.draw$ui_release(LayoutNode.kt:999)
       at androidx.compose.ui.node.InnerNodeCoordinator.performDraw(InnerNodeCoordinator.kt:196)
       at androidx.compose.ui.node.LayoutNodeDrawScope.drawContent(LayoutNodeDrawScope.kt:68)
       at androidx.compose.foundation.BackgroundNode.draw(Background.kt:163)
       at androidx.compose.ui.node.LayoutNodeDrawScope.drawDirect-eZhPAX0$ui_release(LayoutNodeDrawScope.kt:110)
       at androidx.compose.ui.node.NodeCoordinator.getIntroducesMotionFrameOfReference(NodeCoordinator.kt:89)
       at androidx.compose.ui.node.LayoutNodeDrawScope.draw-eZhPAX0$ui_release(LayoutNodeDrawScope.java:89)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:450)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutNode.draw$ui_release(LayoutNode.kt:999)
       at androidx.compose.ui.node.InnerNodeCoordinator.performDraw(InnerNodeCoordinator.kt:196)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.access$drawContainedDrawModifiers(NodeCoordinator.kt:58)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:469)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.runtime.snapshots.Snapshot$Companion.observe(Snapshot.java:503)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver$ObservedScopeMap.observe(SnapshotStateObserver.kt:502)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver.observeReads(SnapshotStateObserver.kt:258)
       at androidx.compose.ui.node.OwnerSnapshotObserver.observeReads$ui_release(OwnerSnapshotObserver.kt:133)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:466)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:291)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:289)
       at androidx.compose.ui.graphics.layer.GraphicsLayerV29.record(GraphicsLayerV29.android.kt:245)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.recordInternal(AndroidGraphicsLayer.android.kt:430)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.record-mL-hObY(GraphicsLayer.java:423)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.updateDisplayList(GraphicsLayerOwnerLayer.android.kt:284)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.drawLayer(GraphicsLayerOwnerLayer.android.kt:229)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:434)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.access$drawContainedDrawModifiers(NodeCoordinator.kt:58)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:469)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.runtime.snapshots.Snapshot$Companion.observe(Snapshot.java:503)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver$ObservedScopeMap.observe(SnapshotStateObserver.kt:502)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver.observeReads(SnapshotStateObserver.kt:258)
       at androidx.compose.ui.node.OwnerSnapshotObserver.observeReads$ui_release(OwnerSnapshotObserver.kt:133)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:466)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:291)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:289)
       at androidx.compose.ui.graphics.layer.GraphicsLayerV29.record(GraphicsLayerV29.android.kt:245)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.recordInternal(AndroidGraphicsLayer.android.kt:430)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.record-mL-hObY(GraphicsLayer.java:423)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.updateDisplayList(GraphicsLayerOwnerLayer.android.kt:284)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.drawLayer(GraphicsLayerOwnerLayer.android.kt:229)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:434)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutNode.draw$ui_release(LayoutNode.kt:999)
       at androidx.compose.ui.node.InnerNodeCoordinator.performDraw(InnerNodeCoordinator.kt:196)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.access$drawContainedDrawModifiers(NodeCoordinator.kt:58)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:469)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.runtime.snapshots.Snapshot$Companion.observe(Snapshot.java:503)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver$ObservedScopeMap.observe(SnapshotStateObserver.kt:502)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver.observeReads(SnapshotStateObserver.kt:258)
       at androidx.compose.ui.node.OwnerSnapshotObserver.observeReads$ui_release(OwnerSnapshotObserver.kt:133)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:466)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:291)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:289)
       at androidx.compose.ui.graphics.layer.GraphicsLayerV29.record(GraphicsLayerV29.android.kt:245)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.recordInternal(AndroidGraphicsLayer.android.kt:430)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.record-mL-hObY(GraphicsLayer.java:423)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.updateDisplayList(GraphicsLayerOwnerLayer.android.kt:284)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.drawLayer(GraphicsLayerOwnerLayer.android.kt:229)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:434)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutNode.draw$ui_release(LayoutNode.kt:999)
       at androidx.compose.ui.node.InnerNodeCoordinator.performDraw(InnerNodeCoordinator.kt:196)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutModifierNodeCoordinator.performDraw(LayoutModifierNodeCoordinator.kt:280)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.access$drawContainedDrawModifiers(NodeCoordinator.kt:58)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:469)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.runtime.snapshots.Snapshot$Companion.observe(Snapshot.java:2441)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver$ObservedScopeMap.observe(SnapshotStateObserver.kt:502)
       at androidx.compose.runtime.snapshots.SnapshotStateObserver.observeReads(SnapshotStateObserver.kt:258)
       at androidx.compose.ui.node.OwnerSnapshotObserver.observeReads$ui_release(OwnerSnapshotObserver.kt:133)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:468)
       at androidx.compose.ui.node.NodeCoordinator$drawBlock$1.invoke(NodeCoordinator.java:466)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:291)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer$recordLambda$1.invoke(GraphicsLayerOwnerLayer.java:289)
       at androidx.compose.ui.graphics.layer.GraphicsLayerV29.record(GraphicsLayerV29.android.kt:245)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.recordInternal(AndroidGraphicsLayer.android.kt:430)
       at androidx.compose.ui.graphics.layer.GraphicsLayer.record-mL-hObY(GraphicsLayer.java:423)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.updateDisplayList(GraphicsLayerOwnerLayer.android.kt:284)
       at androidx.compose.ui.platform.GraphicsLayerOwnerLayer.drawLayer(GraphicsLayerOwnerLayer.android.kt:229)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:434)
       at androidx.compose.ui.node.LayoutNode.draw$ui_release(LayoutNode.kt:999)
       at androidx.compose.ui.node.InnerNodeCoordinator.performDraw(InnerNodeCoordinator.kt:196)
       at androidx.compose.ui.node.NodeCoordinator.drawContainedDrawModifiers(NodeCoordinator.kt:447)
       at androidx.compose.ui.node.NodeCoordinator.draw(NodeCoordinator.kt:439)
       at androidx.compose.ui.node.LayoutNode.draw$ui_release(LayoutNode.kt:999)
       at androidx.compose.ui.platform.AndroidComposeView.dispatchDraw(AndroidComposeView.android.kt:1563)
       at android.view.View.draw(View.java:22357)
       at android.view.View.updateDisplayListIfDirty(View.java:21230)
       at android.view.ViewGroup.recreateChildDisplayList(ViewGroup.java:4500)
       at android.view.ViewGroup.dispatchGetDisplayList(ViewGroup.java:4473)
       at android.view.View.updateDisplayListIfDirty(View.java:21190)
       at android.view.ViewGroup.recreateChildDisplayList(ViewGroup.java:4500)
       at android.view.ViewGroup.dispatchGetDisplayList(ViewGroup.java:4473)
       at android.view.View.updateDisplayListIfDirty(View.java:21190)
       at android.view.ViewGroup.recreateChildDisplayList(ViewGroup.java:4500)
       at android.view.ViewGroup.dispatchGetDisplayList(ViewGroup.java:4473)
       at android.view.View.updateDisplayListIfDirty(View.java:21190)
       at android.view.ViewGroup.recreateChildDisplayList(ViewGroup.java:4500)
       at android.view.ViewGroup.dispatchGetDisplayList(ViewGroup.java:4473)
       at android.view.View.updateDisplayListIfDirty(View.java:21190)
       at android.view.ThreadedRenderer.updateViewTreeDisplayList(ThreadedRenderer.java:559)
       at android.view.ThreadedRenderer.updateRootDisplayList(ThreadedRenderer.java:565)
       at android.view.ThreadedRenderer.draw(ThreadedRenderer.java:642)
       at android.view.ViewRootImpl.draw(ViewRootImpl.java:4106)
       at android.view.ViewRootImpl.performDraw(ViewRootImpl.java:3833)
       at android.view.ViewRootImpl.performTraversals(ViewRootImpl.java:3104)
       at android.view.ViewRootImpl.doTraversal(ViewRootImpl.java:1948)
       at android.view.ViewRootImpl$TraversalRunnable.run(ViewRootImpl.java:8177)
       at android.view.Choreographer$CallbackRecord.run(Choreographer.java:972)
       at android.view.Choreographer.doCallbacks(Choreographer.java:796)
       at android.view.Choreographer.doFrame(Choreographer.java:731)
       at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:957)
       at android.os.Handler.handleCallback(Handler.java:938)
       at android.os.Handler.dispatchMessage(Handler.java:99)
       at android.os.Looper.loop(Looper.java:223)
       at android.app.ActivityThread.main(ActivityThread.java:7680)
       at java.lang.reflect.Method.invokeNative(Method.java)
       at java.lang.reflect.Method.invoke(Method.java:423)
       at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:592)
       at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:947)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
ReferenceQueueDaemon
          ReferenceQueueDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.Object.wait(Object.java:568)
       at java.lang.Daemons$ReferenceQueueDaemon.runInternal(Daemons.java:217)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:923)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
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
        
queued-work-looper
          queued-work-looper:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loop(Looper.java:183)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
arch_disk_io_3
          arch_disk_io_3:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
IntegrityService
          IntegrityService:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loop(Looper.java:183)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
firebase-iid-executor
          firebase-iid-executor:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2109)
       at java.util.concurrent.LinkedBlockingQueue.poll(LinkedBlockingQueue.java:467)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1091)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.android.gms.common.util.concurrent.zza.run(zza.java:2)
       at java.lang.Thread.run(Thread.java:923)
        
DefaultDispatcher-worker-4
          DefaultDispatcher-worker-4:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
DefaultDispatcher-worker-5
          DefaultDispatcher-worker-5:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
grpc-timer-0
          grpc-timer-0:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2109)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:1132)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:849)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
grpc-okhttp-1
          grpc-okhttp-1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.SynchronousQueue$TransferStack.awaitFulfill(SynchronousQueue.java:461)
       at java.util.concurrent.SynchronousQueue$TransferStack.transfer(SynchronousQueue.java:362)
       at java.util.concurrent.SynchronousQueue.poll(SynchronousQueue.java:937)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1091)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
ProcessStablePhenotypeFlag
          ProcessStablePhenotypeFlag:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:1120)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:849)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
MessengerIpcClient
          MessengerIpcClient:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2109)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:1132)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:849)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.android.gms.common.util.concurrent.zza.run(zza.java:2)
       at java.lang.Thread.run(Thread.java:923)
        
GoogleApiHandler
          GoogleApiHandler:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loop(Looper.java:183)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
TokenRefresher
          TokenRefresher:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loop(Looper.java:183)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
arch_disk_io_1
          arch_disk_io_1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
FirestoreWorker
          FirestoreWorker:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2109)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:1132)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:849)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.firestore.util.AsyncQueue$SynchronizedShutdownAwareExecutor$DelayedStartFactory.run(AsyncQueue.java:235)
       at java.lang.Thread.run(Thread.java:923)
        
WM.task-2
          WM.task-2:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
OkHttp ConnectionPool
          OkHttp ConnectionPool:
       at java.lang.Object.wait(Object.java)
       at com.android.okhttp.ConnectionPool$1.run(ConnectionPool.java:106)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
WM.task-1
          WM.task-1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
arch_disk_io_0
          arch_disk_io_0:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
DefaultDispatcher-worker-1
          DefaultDispatcher-worker-1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
grpc-default-executor-0
          grpc-default-executor-0:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.SynchronousQueue$TransferStack.awaitFulfill(SynchronousQueue.java:461)
       at java.util.concurrent.SynchronousQueue$TransferStack.transfer(SynchronousQueue.java:362)
       at java.util.concurrent.SynchronousQueue.poll(SynchronousQueue.java:937)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1091)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
AsyncTask #1
          AsyncTask #1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.SynchronousQueue$TransferStack.awaitFulfill(SynchronousQueue.java:459)
       at java.util.concurrent.SynchronousQueue$TransferStack.transfer(SynchronousQueue.java:362)
       at java.util.concurrent.SynchronousQueue.take(SynchronousQueue.java:920)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
OkHttp TaskRunner
          OkHttp TaskRunner:
       at java.lang.Object.wait(Object.java)
       at okhttp3.internal.concurrent.TaskRunner$RealBackend.coordinatorWait(TaskRunner.java:294)
       at okhttp3.internal.concurrent.TaskRunner.awaitTaskToRun(TaskRunner.kt:218)
       at okhttp3.internal.concurrent.TaskRunner$runnable$1.run(TaskRunner.kt:59)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
kotlinx.coroutines.DefaultExecutor
          kotlinx.coroutines.DefaultExecutor:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at kotlinx.coroutines.DefaultExecutor.run(DefaultExecutor.kt:118)
       at java.lang.Thread.run(Thread.java:923)
        
Firebase Background Thread #0
          Firebase Background Thread #0:
       at dalvik.system.VMStack.getThreadStackTrace(VMStack.java)
       at java.lang.Thread.getStackTrace(Thread.java:1736)
       at java.lang.Thread.getAllStackTraces(Thread.java:1812)
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
       at java.lang.Thread.run(Thread.java:923)
        
ConnectivityThread
          ConnectivityThread:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loop(Looper.java:183)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
FirebaseInstanceId
          FirebaseInstanceId:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2109)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:1132)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:849)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.android.gms.common.util.concurrent.zza.run(zza.java:2)
       at java.lang.Thread.run(Thread.java:923)
        
pool-3-thread-1
          pool-3-thread-1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
Firebase Blocking Thread #1
          Firebase Blocking Thread #1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.SynchronousQueue$TransferStack.awaitFulfill(SynchronousQueue.java:461)
       at java.util.concurrent.SynchronousQueue$TransferStack.transfer(SynchronousQueue.java:362)
       at java.util.concurrent.SynchronousQueue.poll(SynchronousQueue.java:937)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1091)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:923)
        
GoogleApiHandler
          GoogleApiHandler:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loop(Looper.java:183)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
Okio Watchdog
          Okio Watchdog:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2197)
       at okio.AsyncTimeout$Companion.awaitTimeout$okio(AsyncTimeout.java:308)
       at okio.AsyncTimeout$Watchdog.run(AsyncTimeout.java:186)
        
FirebaseSessions_HandlerThread
          FirebaseSessions_HandlerThread:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loop(Looper.java:183)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
FinalizerWatchdogDaemon
          FinalizerWatchdogDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.Object.wait(Object.java:568)
       at java.lang.Daemons$FinalizerWatchdogDaemon.sleepUntilNeeded(Daemons.java:341)
       at java.lang.Daemons$FinalizerWatchdogDaemon.runInternal(Daemons.java:321)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:923)
        
Okio Watchdog
          Okio Watchdog:
       at java.lang.Object.wait(Object.java)
       at com.android.okhttp.okio.AsyncTimeout.awaitTimeout(AsyncTimeout.java:325)
       at com.android.okhttp.okio.AsyncTimeout.access$000(AsyncTimeout.java:42)
       at com.android.okhttp.okio.AsyncTimeout$Watchdog.run(AsyncTimeout.java:288)
        
grpc-okhttp-0
          grpc-okhttp-0:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.SynchronousQueue$TransferStack.awaitFulfill(SynchronousQueue.java:461)
       at java.util.concurrent.SynchronousQueue$TransferStack.transfer(SynchronousQueue.java:362)
       at java.util.concurrent.SynchronousQueue.poll(SynchronousQueue.java:937)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1091)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
firebase-iid-executor
          firebase-iid-executor:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2109)
       at java.util.concurrent.LinkedBlockingQueue.poll(LinkedBlockingQueue.java:467)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1091)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.android.gms.common.util.concurrent.zza.run(zza.java:2)
       at java.lang.Thread.run(Thread.java:923)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
arch_disk_io_2
          arch_disk_io_2:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
FinalizerDaemon
          FinalizerDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:190)
       at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:211)
       at java.lang.Daemons$FinalizerDaemon.runInternal(Daemons.java:273)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:923)
        
3.The AbstractMethodError occurs when a class attempts to call an abstract method that has not been implemented by a concrete subclass. In this specific scenario, the error message indicates that the onViewAttachedToWindow method, which is part of the android.view.View$OnAttachStateChangeListener interface, is being called on an object of type androidx.appcompat.view.menu.CascadingMenuPopup$2 , but this method has not been implemented by that anonymous inner class.
This suggests an incompatibility or an incorrect usage of an AndroidX library component, specifically related to how a CascadingMenuPopup is handling view attachment listeners. It's likely that a version mismatch between different AndroidX libraries or a custom implementation within com.dutype.app is leading to this issue. The crash happens when android.view.View.dispatchAttachedToWindow is invoked, which is part of the Android framework's view lifecycle management, further pointing to an issue during the view's attachment process.
Debugging and Resolution Steps:
Analyze Library Dependencies:
Actionable Step: Examine your build.gradle files (both app-level and any relevant module-level files) for conflicting or outdated versions of AndroidX libraries, especially those related to appcompat , menu , and view .
Best Practice: Use consistent versions for all AndroidX libraries. Gradle's dependency resolution can sometimes pick incompatible versions if not explicitly managed. Use implementation platform('androidx.compose:compose-bom:XXXX.XX.X') (or similar for other AndroidX BOMs) to manage AndroidX dependency versions, or define all versions explicitly. Run ./gradlew app:dependencies to inspect the full dependency tree and identify any version conflicts.
Inspect CascadingMenuPopup Usage:
Actionable Step: Search your project's codebase within com.dutype.app for instances where CascadingMenuPopup or OnAttachStateChangeListener are being used or manipulated. Pay close attention to any custom implementations or overrides.
Best Practice: Avoid direct manipulation of internal AndroidX UI components unless absolutely necessary and thoroughly tested. If custom menu behavior is required, favor using the provided APIs and extension points rather than attempting to modify internal library classes or their anonymous inner classes.
Review AndroidX Release Notes:
Actionable Step: Consult the release notes for the appcompat and recyclerview (if applicable) libraries for any breaking changes related to OnAttachStateChangeListener or menu handling, especially if you recently updated AndroidX versions.
Best Practice: Before updating major versions of AndroidX libraries, review their release notes for potential API changes or behavioral differences that might require code modifications.
Test with Latest Stable AndroidX Versions:
Actionable Step: Update all AndroidX dependencies to their latest stable versions. This often resolves compatibility issues and bugs.
Best Practice: Regularly update your project's dependencies to benefit from bug fixes, performance improvements, and new features. However, always test thoroughly after updates to catch any regressions.
Examine Custom View Attach/Detach Logic:
Actionable Step: If your application has custom views that implement OnAttachStateChangeListener or manually manage view attachment/detachment, review that code for potential interactions with CascadingMenuPopup or its underlying mechanisms.
Best Practice: When implementing custom view lifecycle logic, ensure it adheres to the Android framework's expectations and doesn't interfere with the internal workings of system or library components.
By systematically going through these steps, you should be able to pinpoint the exact cause of the AbstractMethodError and implement a robust solution.


Fatal Exception: java.lang.AbstractMethodError
abstract method "void android.view.View$OnAttachStateChangeListener.onViewAttachedToWindow(android.view.View)"
          Fatal Exception: java.lang.AbstractMethodError: abstract method "void android.view.View$OnAttachStateChangeListener.onViewAttachedToWindow(android.view.View)"
       at android.view.View.dispatchAttachedToWindow(View.java:20494)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3489)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3496)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3496)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3496)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3496)
       at android.view.ViewRootImpl.performTraversals(ViewRootImpl.java:2435)
       at android.view.ViewRootImpl.doTraversal(ViewRootImpl.java:1948)
       at android.view.ViewRootImpl$TraversalRunnable.run(ViewRootImpl.java:8177)
       at android.view.Choreographer$CallbackRecord.run(Choreographer.java:972)
       at android.view.Choreographer.doCallbacks(Choreographer.java:796)
       at android.view.Choreographer.doFrame(Choreographer.java:731)
       at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:957)
       at android.os.Handler.handleCallback(Handler.java:938)
       at android.os.Handler.dispatchMessage(Handler.java:99)
       at android.os.Looper.loop(Looper.java:223)
       at android.app.ActivityThread.main(ActivityThread.java:7680)
       at java.lang.reflect.Method.invokeNative(Method.java)
       at java.lang.reflect.Method.invoke(Method.java:423)
       at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:592)
       at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:947)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
ReferenceQueueDaemon
          ReferenceQueueDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.Object.wait(Object.java:568)
       at java.lang.Daemons$ReferenceQueueDaemon.runInternal(Daemons.java:217)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:923)
        
FinalizerDaemon
          FinalizerDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:190)
       at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:211)
       at java.lang.Daemons$FinalizerDaemon.runInternal(Daemons.java:273)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:923)
        
queued-work-looper
          queued-work-looper:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loop(Looper.java:183)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
OkHttp ConnectionPool
          OkHttp ConnectionPool:
       at java.lang.Object.wait(Object.java)
       at com.android.okhttp.ConnectionPool$1.run(ConnectionPool.java:106)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
DefaultDispatcher-worker-1
          DefaultDispatcher-worker-1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
FinalizerWatchdogDaemon
          FinalizerWatchdogDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.Object.wait(Object.java:568)
       at java.lang.Daemons$FinalizerWatchdogDaemon.sleepUntilNeeded(Daemons.java:341)
       at java.lang.Daemons$FinalizerWatchdogDaemon.runInternal(Daemons.java:321)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:923)
        
pool-13-thread-1
          pool-13-thread-1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
Firebase Blocking Thread #3
          Firebase Blocking Thread #3:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.SynchronousQueue$TransferStack.awaitFulfill(SynchronousQueue.java:461)
       at java.util.concurrent.SynchronousQueue$TransferStack.transfer(SynchronousQueue.java:362)
       at java.util.concurrent.SynchronousQueue.poll(SynchronousQueue.java:937)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1091)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:923)
        
Firebase Blocking Thread #2
          Firebase Blocking Thread #2:
       at java.nio.Buffer.<init>(Buffer.java:209)
       at java.nio.ByteBuffer.<init>(ByteBuffer.java:227)
       at java.nio.HeapByteBuffer.<init>(HeapByteBuffer.java:54)
       at java.nio.HeapByteBuffer.<init>(HeapByteBuffer.java:49)
       at java.nio.ByteBuffer.allocate(ByteBuffer.java:282)
       at com.android.org.conscrypt.ConscryptEngineSocket$SSLOutputStream.<init>(ConscryptEngineSocket.java:623)
       at com.android.org.conscrypt.ConscryptEngineSocket.startHandshake(ConscryptEngineSocket.java:206)
       at com.android.okhttp.internal.io.RealConnection.connectTls(RealConnection.java:196)
       at com.android.okhttp.internal.io.RealConnection.connectSocket(RealConnection.java:153)
       at com.android.okhttp.internal.io.RealConnection.connect(RealConnection.java:116)
       at com.android.okhttp.internal.http.StreamAllocation.findConnection(StreamAllocation.java:186)
       at com.android.okhttp.internal.http.StreamAllocation.findHealthyConnection(StreamAllocation.java:128)
       at com.android.okhttp.internal.http.StreamAllocation.newStream(StreamAllocation.java:97)
       at com.android.okhttp.internal.http.HttpEngine.connect(HttpEngine.java:289)
       at com.android.okhttp.internal.http.HttpEngine.sendRequest(HttpEngine.java:232)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.execute(HttpURLConnectionImpl.java:465)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.connect(HttpURLConnectionImpl.java:131)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.getOutputStream(HttpURLConnectionImpl.java:262)
       at com.android.okhttp.internal.huc.DelegatingHttpsURLConnection.getOutputStream(DelegatingHttpsURLConnection.java:219)
       at com.android.okhttp.internal.huc.HttpsURLConnectionImpl.getOutputStream(HttpsURLConnectionImpl.java:30)
       at com.google.firebase.installations.remote.FirebaseInstallationServiceClient.writeRequestBodyToOutputStream(FirebaseInstallationServiceClient.java:235)
       at com.google.firebase.installations.remote.FirebaseInstallationServiceClient.writeFIDCreateRequestBodyToOutputStream(FirebaseInstallationServiceClient.java:217)
       at com.google.firebase.installations.remote.FirebaseInstallationServiceClient.createFirebaseInstallation(FirebaseInstallationServiceClient.java:175)
       at com.google.firebase.installations.FirebaseInstallations.registerFidWithServer(FirebaseInstallations.java:533)
       at com.google.firebase.installations.FirebaseInstallations.doNetworkCallIfNecessary(FirebaseInstallations.java:387)
       at com.google.firebase.installations.FirebaseInstallations.lambda$doRegistrationOrRefresh$3(FirebaseInstallations.java:372)
       at com.google.firebase.concurrent.SequentialExecutor$1.run(SequentialExecutor.java:117)
       at com.google.firebase.concurrent.SequentialExecutor$QueueWorker.workOnQueue(SequentialExecutor.java:229)
       at com.google.firebase.concurrent.SequentialExecutor$QueueWorker.run(SequentialExecutor.java:174)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:923)
        
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
       at android.os.Looper.loop(Looper.java:183)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
Okio Watchdog
          Okio Watchdog:
       at java.lang.Object.wait(Object.java)
       at com.android.okhttp.okio.AsyncTimeout.awaitTimeout(AsyncTimeout.java:325)
       at com.android.okhttp.okio.AsyncTimeout.access$000(AsyncTimeout.java:42)
       at com.android.okhttp.okio.AsyncTimeout$Watchdog.run(AsyncTimeout.java:288)
        
FirebaseInstanceId
          FirebaseInstanceId:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireSharedNanos(AbstractQueuedSynchronizer.java:1063)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer.tryAcquireSharedNanos(AbstractQueuedSynchronizer.java:1358)
       at java.util.concurrent.CountDownLatch.await(CountDownLatch.java:278)
       at com.google.android.gms.tasks.zzad.zzb(com.google.android.gms:play-services-tasks@@18.1.0:1)
       at com.google.android.gms.tasks.Tasks.await(com.google.android.gms:play-services-tasks@@18.1.0:18)
       at com.google.firebase.iid.FirebaseInstanceId.awaitTask(com.google.firebase:firebase-iid@@21.1.0:1)
       at com.google.firebase.iid.FirebaseInstanceId.getToken(com.google.firebase:firebase-iid@@21.1.0:9)
       at com.google.firebase.iid.FirebaseInstanceId.blockingGetMasterToken(com.google.firebase:firebase-iid@@21.1.0:1)
       at com.google.firebase.iid.SyncTask.maybeRefreshToken(com.google.firebase:firebase-iid@@21.1.0:3)
       at com.google.firebase.iid.SyncTask.run(com.google.firebase:firebase-iid@@21.1.0:10)
       at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:462)
       at java.util.concurrent.FutureTask.run(FutureTask.java:266)
       at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:301)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.android.gms.common.util.concurrent.zza.run(zza.java:2)
       at java.lang.Thread.run(Thread.java:923)
        
Firebase Blocking Thread #0
          Firebase Blocking Thread #0:
       at java.net.SocketInputStream.socketRead0(SocketInputStream.java)
       at java.net.SocketInputStream.socketRead(SocketInputStream.java:119)
       at java.net.SocketInputStream.read(SocketInputStream.java:176)
       at java.net.SocketInputStream.read(SocketInputStream.java:144)
       at com.android.org.conscrypt.ConscryptEngineSocket$SSLInputStream.readFromSocket(ConscryptEngineSocket.java:936)
       at com.android.org.conscrypt.ConscryptEngineSocket$SSLInputStream.processDataFromSocket(ConscryptEngineSocket.java:900)
       at com.android.org.conscrypt.ConscryptEngineSocket$SSLInputStream.readUntilDataAvailable(ConscryptEngineSocket.java:815)
       at com.android.org.conscrypt.ConscryptEngineSocket$SSLInputStream.read(ConscryptEngineSocket.java:788)
       at com.android.okhttp.okio.Okio$2.read(Okio.java:138)
       at com.android.okhttp.okio.AsyncTimeout$2.read(AsyncTimeout.java:213)
       at com.android.okhttp.okio.RealBufferedSource.indexOf(RealBufferedSource.java:307)
       at com.android.okhttp.okio.RealBufferedSource.indexOf(RealBufferedSource.java:301)
       at com.android.okhttp.okio.RealBufferedSource.readUtf8LineStrict(RealBufferedSource.java:197)
       at com.android.okhttp.internal.http.Http1xStream.readResponse(Http1xStream.java:188)
       at com.android.okhttp.internal.http.Http1xStream.readResponseHeaders(Http1xStream.java:129)
       at com.android.okhttp.internal.http.HttpEngine.readNetworkResponse(HttpEngine.java:750)
       at com.android.okhttp.internal.http.HttpEngine.readResponse(HttpEngine.java:622)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.execute(HttpURLConnectionImpl.java:475)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.getResponse(HttpURLConnectionImpl.java:411)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.getResponseCode(HttpURLConnectionImpl.java:542)
       at com.android.okhttp.internal.huc.DelegatingHttpsURLConnection.getResponseCode(DelegatingHttpsURLConnection.java:106)
       at com.android.okhttp.internal.huc.HttpsURLConnectionImpl.getResponseCode(HttpsURLConnectionImpl.java:30)
       at com.google.firebase.crashlytics.internal.network.HttpGetRequest.execute(HttpGetRequest.java:81)
       at com.google.firebase.crashlytics.internal.settings.DefaultSettingsSpiCall.invoke(DefaultSettingsSpiCall.java:114)
       at com.google.firebase.crashlytics.internal.settings.SettingsController$1.lambda$then$0(SettingsController.java:204)
       at java.util.concurrent.FutureTask.run(FutureTask.java:266)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:923)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
Firebase Background Thread #0
          Firebase Background Thread #0:
       at dalvik.system.VMStack.getThreadStackTrace(VMStack.java)
       at java.lang.Thread.getStackTrace(Thread.java:1736)
       at java.lang.Thread.getAllStackTraces(Thread.java:1812)
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
       at java.lang.Thread.run(Thread.java:923)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
Firebase Background Thread #2
          Firebase Background Thread #2:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.FutureTask.awaitDone(FutureTask.java:450)
       at java.util.concurrent.FutureTask.get(FutureTask.java:192)
       at com.google.firebase.crashlytics.internal.settings.SettingsController$1.then(SettingsController.java:206)
       at com.google.firebase.crashlytics.internal.settings.SettingsController$1.then(SettingsController.java:194)
       at com.google.android.gms.tasks.zzo.run(com.google.android.gms:play-services-tasks@@18.1.0:1)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:923)
        
WM.task-1
          WM.task-1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
Firebase Background Thread #1
          Firebase Background Thread #1:
       at libcore.util.NativeAllocationRegistry.registerNativeAllocation(NativeAllocationRegistry.java:234)
       at com.android.icu.charset.NativeConverter.registerConverter(NativeConverter.java:34)
       at com.android.icu.charset.CharsetEncoderICU.newInstance(CharsetEncoderICU.java:87)
       at com.android.icu.charset.CharsetICU.newEncoder(CharsetICU.java:47)
       at sun.nio.cs.StreamEncoder.<init>(StreamEncoder.java:176)
       at sun.nio.cs.StreamEncoder.forOutputStreamWriter(StreamEncoder.java:68)
       at java.io.OutputStreamWriter.<init>(OutputStreamWriter.java:133)
       at com.google.firebase.crashlytics.internal.metadata.MetaDataStore.writeKeyData(MetaDataStore.java:105)
       at com.google.firebase.crashlytics.internal.metadata.UserMetadata$SerializeableKeysMap.serializeIfMarked(UserMetadata.java:343)
       at com.google.firebase.crashlytics.internal.metadata.UserMetadata$SerializeableKeysMap.lambda$scheduleSerializationTaskIfNeeded$0(UserMetadata.java:313)
       at com.google.firebase.crashlytics.internal.concurrency.CrashlyticsWorker.lambda$submit$1(CrashlyticsWorker.java:96)
       at com.google.android.gms.tasks.zze.run(com.google.android.gms:play-services-tasks@@18.1.0:1)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:923)
        
firebase-iid-executor
          firebase-iid-executor:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:868)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireSharedInterruptibly(AbstractQueuedSynchronizer.java:1023)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireSharedInterruptibly(AbstractQueuedSynchronizer.java:1334)
       at java.util.concurrent.CountDownLatch.await(CountDownLatch.java:232)
       at com.google.android.gms.tasks.zzad.zza(com.google.android.gms:play-services-tasks@@18.1.0:1)
       at com.google.android.gms.tasks.Tasks.await(com.google.android.gms:play-services-tasks@@18.1.0:8)
       at com.google.firebase.iid.GmsRpc.setDefaultAttributesToBundle(GmsRpc.java:11)
       at com.google.firebase.iid.GmsRpc.startRpc(GmsRpc.java:1)
       at com.google.firebase.iid.GmsRpc.getToken(GmsRpc.java:1)
       at com.google.firebase.iid.FirebaseInstanceId.lambda$getInstanceId$2$FirebaseInstanceId(FirebaseInstanceId.java:1)
       at com.google.firebase.iid.FirebaseInstanceId$$Lambda$3.start(FirebaseInstanceId.java:200)
       at com.google.firebase.iid.RequestDeduplicator.getOrStartGetTokenRequest(RequestDeduplicator.java:7)
       at com.google.firebase.iid.FirebaseInstanceId.lambda$getInstanceId$3$FirebaseInstanceId(FirebaseInstanceId.java:5)
       at com.google.firebase.iid.FirebaseInstanceId$$Lambda$0.then(com.google.firebase:firebase-iid@@21.1.0:28)
       at com.google.android.gms.tasks.zze.run(com.google.android.gms:play-services-tasks@@18.1.0:1)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.android.gms.common.util.concurrent.zza.run(zza.java:2)
       at java.lang.Thread.run(Thread.java:923)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
DefaultDispatcher-worker-2
          DefaultDispatcher-worker-2:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
Firebase Blocking Thread #1
          Firebase Blocking Thread #1:
       at com.mojito.utility.MojitoMessage.fillCallStack(MojitoMessage.java:345)
       at com.mojito.utility.MojitoLogger.logMessage(MojitoLogger.java:128)
       at java.util.Locale.getLanguage_aroundBody1$advice(Locale.java:42)
       at java.util.Locale.getLanguage(Locale.java:1)
       at java.lang.CaseMapper.toLowerCase(CaseMapper.java:48)
       at java.lang.String.toLowerCase(String.java:2666)
       at sun.security.x509.AVA.toRFC2253CanonicalString(AVA.java:1022)
       at sun.security.x509.RDN.toRFC2253StringInternal(RDN.java:444)
       at sun.security.x509.RDN.toRFC2253String(RDN.java:424)
       at sun.security.x509.X500Name.getRFC2253CanonicalName(X500Name.java:730)
       at sun.security.x509.X500Name.equals(X500Name.java:417)
       at javax.security.auth.x500.X500Principal.equals(X500Principal.java:475)
       at java.security.cert.X509CertSelector.match(X509CertSelector.java:2047)
       at sun.security.provider.certpath.AdaptableX509CertSelector.match(AdaptableX509CertSelector.java:198)
       at sun.security.provider.certpath.PKIXCertPathValidator.validate(PKIXCertPathValidator.java:119)
       at sun.security.provider.certpath.PKIXCertPathValidator.engineValidate(PKIXCertPathValidator.java:79)
       at java.security.cert.CertPathValidator.validate(CertPathValidator.java:301)
       at com.android.org.conscrypt.TrustManagerImpl.verifyChain(TrustManagerImpl.java:720)
       at com.android.org.conscrypt.TrustManagerImpl.checkTrustedRecursive(TrustManagerImpl.java:554)
       at com.android.org.conscrypt.TrustManagerImpl.checkTrustedRecursive(TrustManagerImpl.java:575)
       at com.android.org.conscrypt.TrustManagerImpl.checkTrusted(TrustManagerImpl.java:510)
       at com.android.org.conscrypt.TrustManagerImpl.checkTrusted(TrustManagerImpl.java:428)
       at com.android.org.conscrypt.TrustManagerImpl.getTrustedChainForServer(TrustManagerImpl.java:356)
       at android.security.net.config.NetworkSecurityTrustManager.checkServerTrusted(NetworkSecurityTrustManager.java:94)
       at android.security.net.config.RootTrustManager.checkServerTrusted(RootTrustManager.java:90)
       at com.android.org.conscrypt.ConscryptEngineSocket$2.checkServerTrusted(ConscryptEngineSocket.java:161)
       at com.android.org.conscrypt.Platform.checkServerTrusted(Platform.java:250)
       at com.android.org.conscrypt.ConscryptEngine.verifyCertificateChain(ConscryptEngine.java:1644)
       at com.android.org.conscrypt.NativeCrypto.ENGINE_SSL_read_direct(NativeCrypto.java)
       at com.android.org.conscrypt.NativeSsl.readDirectByteBuffer(NativeSsl.java:568)
       at com.android.org.conscrypt.ConscryptEngine.readPlaintextDataDirect(ConscryptEngine.java:1095)
       at com.android.org.conscrypt.ConscryptEngine.readPlaintextData(ConscryptEngine.java:1079)
       at com.android.org.conscrypt.ConscryptEngine.unwrap(ConscryptEngine.java:876)
       at com.android.org.conscrypt.ConscryptEngine.unwrap(ConscryptEngine.java:747)
       at com.android.org.conscrypt.ConscryptEngine.unwrap(ConscryptEngine.java:712)
       at com.android.org.conscrypt.ConscryptEngineSocket$SSLInputStream.processDataFromSocket(ConscryptEngineSocket.java:849)
       at com.android.org.conscrypt.ConscryptEngineSocket$SSLInputStream.access$100(ConscryptEngineSocket.java:722)
       at com.android.org.conscrypt.ConscryptEngineSocket.doHandshake(ConscryptEngineSocket.java:238)
       at com.android.org.conscrypt.ConscryptEngineSocket.startHandshake(ConscryptEngineSocket.java:217)
       at com.android.okhttp.internal.io.RealConnection.connectTls(RealConnection.java:196)
       at com.android.okhttp.internal.io.RealConnection.connectSocket(RealConnection.java:153)
       at com.android.okhttp.internal.io.RealConnection.connect(RealConnection.java:116)
       at com.android.okhttp.internal.http.StreamAllocation.findConnection(StreamAllocation.java:186)
       at com.android.okhttp.internal.http.StreamAllocation.findHealthyConnection(StreamAllocation.java:128)
       at com.android.okhttp.internal.http.StreamAllocation.newStream(StreamAllocation.java:97)
       at com.android.okhttp.internal.http.HttpEngine.connect(HttpEngine.java:289)
       at com.android.okhttp.internal.http.HttpEngine.sendRequest(HttpEngine.java:232)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.execute(HttpURLConnectionImpl.java:465)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.connect(HttpURLConnectionImpl.java:131)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.getOutputStream(HttpURLConnectionImpl.java:262)
       at com.android.okhttp.internal.huc.DelegatingHttpsURLConnection.getOutputStream(DelegatingHttpsURLConnection.java:219)
       at com.android.okhttp.internal.huc.HttpsURLConnectionImpl.getOutputStream(HttpsURLConnectionImpl.java:30)
       at com.google.firebase.appcheck.internal.NetworkClient.makeNetworkRequest(NetworkClient.java:165)
       at com.google.firebase.appcheck.internal.NetworkClient.generatePlayIntegrityChallenge(NetworkClient.java:139)
       at com.google.firebase.appcheck.playintegrity.internal.PlayIntegrityAppCheckProvider.lambda$getPlayIntegrityAttestation$3(PlayIntegrityAppCheckProvider.java:108)
       at com.google.android.gms.tasks.zzz.run(com.google.android.gms:play-services-tasks@@18.1.0:1)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0(CustomThreadFactory.java:47)
       at java.lang.Thread.run(Thread.java:923)
        
GoogleApiHandler
          GoogleApiHandler:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loop(Looper.java:183)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
MessengerIpcClient
          MessengerIpcClient:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2109)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:1132)
       at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:849)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.android.gms.common.util.concurrent.zza.run(zza.java:2)
       at java.lang.Thread.run(Thread.java:923)
        
