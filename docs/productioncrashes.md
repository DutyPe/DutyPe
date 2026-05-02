crash_thread	 main	
Common
exception_class	 AbstractMethodError	
Common
exception_message	 abstract method "void android.view.View$OnAttachStateChangeListener.onViewAttachedToWindow(android.view.View)"	
stack_trace_depth	 20

resolution_note	 Pinned androidx.appcompat to 1.7.1 to address the suspected CascadingMenuPopup / OnAttachStateChangeListener mismatch behind this AbstractMethodError. Deployed on 2026-05-02.


and
Fatal Exception: java.lang.AbstractMethodError
abstract method "void android.view.View$OnAttachStateChangeListener.onViewAttachedToWindow(android.view.View)"
          Fatal Exception: java.lang.AbstractMethodError: abstract method "void android.view.View$OnAttachStateChangeListener.onViewAttachedToWindow(android.view.View)"
       at android.view.View.dispatchAttachedToWindow(View.java:22021)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:4291)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:4298)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:4298)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:4298)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:4298)
       at android.view.ViewRootImpl.performTraversals(ViewRootImpl.java:3135)
       at android.view.ViewRootImpl.doTraversal(ViewRootImpl.java:2618)
       at android.view.ViewRootImpl$TraversalRunnable.run(ViewRootImpl.java:9971)
       at android.view.Choreographer$CallbackRecord.run(Choreographer.java:1010)
       at android.view.Choreographer.doCallbacks(Choreographer.java:809)
       at android.view.Choreographer.doFrame(Choreographer.java:744)
       at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:995)
       at android.os.Handler.handleCallback(Handler.java:938)
       at android.os.Handler.dispatchMessage(Handler.java:99)
       at android.os.Looper.loop(Looper.java:246)
       at android.app.ActivityThread.main(ActivityThread.java:8653)
       at java.lang.reflect.Method.invoke(Method.java)
       at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:602)
       at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1130)
        
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
        
ReferenceQueueDaemon
          ReferenceQueueDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.Object.wait(Object.java:568)
       at java.lang.Daemons$ReferenceQueueDaemon.runInternal(Daemons.java:217)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:923)
        
TokenRefresher
          TokenRefresher:
       at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
       at android.os.MessageQueue.next(MessageQueue.java:335)
       at android.os.Looper.loop(Looper.java:206)
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
        
FinalizerDaemon
          FinalizerDaemon:
       at java.lang.Object.wait(Object.java)
       at java.lang.Object.wait(Object.java:442)
       at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:190)
       at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:211)
       at java.lang.Daemons$FinalizerDaemon.runInternal(Daemons.java:273)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
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
       at com.android.okhttp.okio.Okio$2.read(Okio.java:145)
       at com.android.okhttp.okio.AsyncTimeout$2.read(AsyncTimeout.java:213)
       at com.android.okhttp.okio.RealBufferedSource.indexOf(RealBufferedSource.java:317)
       at com.android.okhttp.okio.RealBufferedSource.indexOf(RealBufferedSource.java:311)
       at com.android.okhttp.okio.RealBufferedSource.readUtf8LineStrict(RealBufferedSource.java:207)
       at com.android.okhttp.internal.http.Http1xStream.readResponse(Http1xStream.java:395)
       at com.android.okhttp.internal.http.Http1xStream.readResponseHeaders(Http1xStream.java:146)
       at com.android.okhttp.internal.http.HttpEngine.readNetworkResponse(HttpEngine.java:900)
       at com.android.okhttp.internal.http.HttpEngine.readResponse(HttpEngine.java:772)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.execute(HttpURLConnectionImpl.java:475)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.getResponse(HttpURLConnectionImpl.java:411)
       at com.android.okhttp.internal.huc.HttpURLConnectionImpl.getResponseCode(HttpURLConnectionImpl.java:542)
       at com.android.okhttp.internal.huc.DelegatingHttpsURLConnection.getResponseCode(DelegatingHttpsURLConnection.java:106)
       at com.android.okhttp.internal.huc.HttpsURLConnectionImpl.getResponseCode(HttpsURLConnectionImpl.java:30)
       at com.google.firebase.appcheck.internal.NetworkClient.makeNetworkRequest(NetworkClient.java:169)
       at com.google.firebase.appcheck.internal.NetworkClient.generatePlayIntegrityChallenge(NetworkClient.java:139)
       at com.google.firebase.appcheck.playintegrity.internal.PlayIntegrityAppCheckProvider.lambda$getPlayIntegrityAttestation$3(PlayIntegrityAppCheckProvider.java:108)
       at com.google.android.gms.tasks.zzz.run(com.google.android.gms:play-services-tasks@@18.1.0:1)
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
       at android.os.Looper.loop(Looper.java:206)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
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
        
pool-14-thread-1
          pool-14-thread-1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at java.lang.Thread.run(Thread.java:923)
        
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
       at java.lang.Thread.run(Thread.java:923)
        
DefaultDispatcher-worker-1
          DefaultDispatcher-worker-1:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
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
       at android.os.Looper.loop(Looper.java:206)
       at android.os.HandlerThread.run(HandlerThread.java:67)
        
FinalizerWatchdogDaemon
          FinalizerWatchdogDaemon:
       at java.lang.Thread.sleep(Thread.java)
       at java.lang.Thread.sleep(Thread.java:442)
       at java.lang.Thread.sleep(Thread.java:358)
       at java.lang.Daemons$FinalizerWatchdogDaemon.sleepForNanos(Daemons.java:390)
       at java.lang.Daemons$FinalizerWatchdogDaemon.waitForFinalization(Daemons.java:419)
       at java.lang.Daemons$FinalizerWatchdogDaemon.runInternal(Daemons.java:325)
       at java.lang.Daemons$Daemon.run(Daemons.java:139)
       at java.lang.Thread.run(Thread.java:923)
        
WM.task-3
          WM.task-3:
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
        
DefaultDispatcher-worker-3
          DefaultDispatcher-worker-3:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:858)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:806)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:754)
       at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:707)
        
Firebase-Messaging-Topics-Io
          Firebase-Messaging-Topics-Io:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:868)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireSharedInterruptibly(AbstractQueuedSynchronizer.java:1023)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireSharedInterruptibly(AbstractQueuedSynchronizer.java:1334)
       at java.util.concurrent.CountDownLatch.await(CountDownLatch.java:232)
       at com.google.android.gms.tasks.zzad.zza(com.google.android.gms:play-services-tasks@@18.1.0:1)
       at com.google.android.gms.tasks.Tasks.await(com.google.android.gms:play-services-tasks@@18.1.0:8)
       at com.google.firebase.messaging.FirebaseMessaging.blockingGetToken(FirebaseMessaging.java:634)
       at com.google.firebase.messaging.TopicsSubscriber.blockingSubscribeToTopic(TopicsSubscriber.java:275)
       at com.google.firebase.messaging.TopicsSubscriber.performTopicOperation(TopicsSubscriber.java:234)
       at com.google.firebase.messaging.TopicsSubscriber.syncTopics(TopicsSubscriber.java:188)
       at com.google.firebase.messaging.TopicsSyncTask.run(TopicsSyncTask.java:101)
       at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:462)
       at java.util.concurrent.FutureTask.run(FutureTask.java:266)
       at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:301)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
       at com.google.android.gms.common.util.concurrent.zza.run(zza.java:2)
       at java.lang.Thread.run(Thread.java:923)
        
WM.task-4
          WM.task-4:
       at sun.misc.Unsafe.park(Unsafe.java)
       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
       at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
       at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
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
        
