2026-05-05 17:02:24.821  9989-9989  OtpViewMod...Credential com.dutype.app                       D  OtpViewModel - Profile data: {userId=g2qjCUuxNkUaEkgkZzgCWseecpE3, role=WORKER, phone=+918019151847, geohash=tepemv, totalRatings=1, rating=5, fullName=Restaurant VL, location={**************, **************}, updatedAt=Timestamp(seconds=1777711773, nanoseconds=986000000)}
2026-05-05 17:02:24.833  9989-9989  AuthManager             com.dutype.app                       D  AuthManager - User saved: g2qjCUuxNkUaEkgkZzgCWseecpE3
2026-05-05 17:02:24.833  9989-9989  AuthManager             com.dutype.app                       D  AuthManager - Login state set: true
2026-05-05 17:02:24.835  9989-9989  MetadataManager         com.dutype.app                       D  📊 Loading authenticated metadata from Firestore...
2026-05-05 17:02:24.836  9989-9989  AppMetadata             com.dutype.app                       D  📊 Loading authenticated AppMetadata...
2026-05-05 17:02:24.836  9989-12498 SessionManager          com.dutype.app                       D  🔐 Session started: 1777980744834_69a76c1e-ac7d-43e0-a92f-86531b6a0e94 (user: g2qjCUuxNkUaEkgkZzgCWseecpE3)
2026-05-05 17:02:24.837  9989-9989  AppMetadata             com.dutype.app                       D  📊 Feature flags using strict defaults (metadata collection removed)
2026-05-05 17:02:24.837  9989-12498 AuthManager$setLoggedIn com.dutype.app                       D  AuthManager - Session started for user: g2qjCUuxNkUaEkgkZzgCWseecpE3
2026-05-05 17:02:24.837  9989-9989  AppMetadata             com.dutype.app                       D  📊 AppMetadata loaded from Firestore
2026-05-05 17:02:24.838  9989-9989  JobMetadata             com.dutype.app                       D  📊 Loading authenticated JobMetadata...
2026-05-05 17:02:24.842  9989-9989  OtpViewMod...Credential com.dutype.app                       I  âœ… User authenticated successfully: g2qjCUuxNkUaEkgkZzgCWseecpE3
2026-05-05 17:02:24.843  9989-9989  ErrorHandler            com.dutype.app                       D  📍 User saved to AuthManager - Authentication complete
2026-05-05 17:02:24.845  9989-9989  FCMTokenManager         com.dutype.app                       D  FCMTokenManager: Got FCM token: d21lOnvfS3i1pCh2fOzQ...
2026-05-05 17:02:24.848  9989-9989  OtpViewMod...Credential com.dutype.app                       I  OtpViewModel - Existing user detected, marking profile as complete
2026-05-05 17:02:24.849  9989-9989  OtpViewModel            com.dutype.app                       D  updateProfileComplete - userId: g2qjCUuxNkUaEkgkZzgCWseecpE3, role: WORKER, isComplete: true
2026-05-05 17:02:24.871  9989-9989  RegisterSc...terContent com.dutype.app                       D  📱 REGISTER - OTP verified, creating new user profile
2026-05-05 17:02:25.305  9989-9989  JobMetadata             com.dutype.app                       D  📊 JobMetadata loaded from jobs collection
2026-05-05 17:02:25.305  9989-9989  MetadataManager         com.dutype.app                       D  📊 Authenticated metadata loaded successfully
2026-05-05 17:02:25.305  9989-9989  OtpViewMod...Credential com.dutype.app                       I  âœ… Metadata initialized after authentication
2026-05-05 17:02:25.306  9989-9989  FCMTokenManager         com.dutype.app                       I  FCMTokenManager: Token saved for user: g2qjCUuxNkUaEkgkZzgCWseecpE3
2026-05-05 17:02:25.306  9989-9989  FCMTokenManager         com.dutype.app                       I  FCMTokenManager: Token with role saved for user: g2qjCUuxNkUaEkgkZzgCWseecpE3, role: WORKER
2026-05-05 17:02:25.307  9989-9989  FCMTokenManager         com.dutype.app                       I  FCMTokenManager: Subscribed to WORKER topics
2026-05-05 17:02:25.308  9989-9989  OtpViewMod...Credential com.dutype.app                       I  âœ… FCM token registered with role for user: g2qjCUuxNkUaEkgkZzgCWseecpE3, role: WORKER
2026-05-05 17:02:25.399  9989-9989  AuthFlowService         com.dutype.app                       E  AuthFlowService.completeRegistration failed (Fix with AI)
                                                                                                    com.google.firebase.firestore.FirebaseFirestoreException: PERMISSION_DENIED: Missing or insufficient permissions.
                                                                                                    	at com.google.firebase.firestore.util.Util.exceptionFromStatus(Util.java:155)
                                                                                                    	at com.google.firebase.firestore.remote.Datastore$1.onClose(Datastore.java:185)
                                                                                                    	at com.google.firebase.firestore.remote.FirestoreChannel$3.onClose(FirestoreChannel.java:237)
                                                                                                    	at io.grpc.internal.ClientCallImpl.closeObserver(ClientCallImpl.java:574)
                                                                                                    	at io.grpc.internal.ClientCallImpl.access$300(ClientCallImpl.java:72)
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInternal(ClientCallImpl.java:742)
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInContext(ClientCallImpl.java:723)
                                                                                                    	at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37)
                                                                                                    	at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133)
                                                                                                    	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:520)
                                                                                                    	at java.util.concurrent.FutureTask.run(FutureTask.java:317)
                                                                                                    	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:348)
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1154)
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:652)
                                                                                                    	at com.google.firebase.firestore.util.AsyncQueue$SynchronizedShutdownAwareExecutor$DelayedStartFactory.run(AsyncQueue.java:235)
                                                                                                    	at java.lang.Thread.run(Thread.java:1563)
                                                                                                    Caused by: io.grpc.StatusException: PERMISSION_DENIED: Missing or insufficient permissions.
                                                                                                    	at io.grpc.Status.asException(Status.java:541)
                                                                                                    	at com.google.firebase.firestore.util.Util.exceptionFromStatus(Util.java:153)
                                                                                                    	at com.google.firebase.firestore.remote.Datastore$1.onClose(Datastore.java:185) 
                                                                                                    	at com.google.firebase.firestore.remote.FirestoreChannel$3.onClose(FirestoreChannel.java:237) 
                                                                                                    	at io.grpc.internal.ClientCallImpl.closeObserver(ClientCallImpl.java:574) 
                                                                                                    	at io.grpc.internal.ClientCallImpl.access$300(ClientCallImpl.java:72) 
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInternal(ClientCallImpl.java:742) 
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInContext(ClientCallImpl.java:723) 
                                                                                                    	at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37) 
                                                                                                    	at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133) 
                                                                                                    	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:520) 
                                                                                                    	at java.util.concurrent.FutureTask.run(FutureTask.java:317) 
                                                                                                    	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:348) 
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1154) 
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:652) 
                                                                                                    	at com.google.firebase.firestore.util.AsyncQueue$SynchronizedShutdownAwareExecutor$DelayedStartFactory.run(AsyncQueue.java:235) 
                                                                                                    	at java.lang.Thread.run(Thread.java:1563) 
2026-05-05 17:02:25.400  9989-9989  RegisterSc...terContent com.dutype.app                       E  REGISTER - Registration finalization failed (Fix with AI)
                                                                                                    com.google.firebase.firestore.FirebaseFirestoreException: PERMISSION_DENIED: Missing or insufficient permissions.
                                                                                                    	at com.google.firebase.firestore.util.Util.exceptionFromStatus(Util.java:155)
                                                                                                    	at com.google.firebase.firestore.remote.Datastore$1.onClose(Datastore.java:185)
                                                                                                    	at com.google.firebase.firestore.remote.FirestoreChannel$3.onClose(FirestoreChannel.java:237)
                                                                                                    	at io.grpc.internal.ClientCallImpl.closeObserver(ClientCallImpl.java:574)
                                                                                                    	at io.grpc.internal.ClientCallImpl.access$300(ClientCallImpl.java:72)
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInternal(ClientCallImpl.java:742)
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInContext(ClientCallImpl.java:723)
                                                                                                    	at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37)
                                                                                                    	at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133)
                                                                                                    	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:520)
                                                                                                    	at java.util.concurrent.FutureTask.run(FutureTask.java:317)
                                                                                                    	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:348)
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1154)
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:652)
                                                                                                    	at com.google.firebase.firestore.util.AsyncQueue$SynchronizedShutdownAwareExecutor$DelayedStartFactory.run(AsyncQueue.java:235)
                                                                                                    	at java.lang.Thread.run(Thread.java:1563)
                                                                                                    Caused by: io.grpc.StatusException: PERMISSION_DENIED: Missing or insufficient permissions.
                                                                                                    	at io.grpc.Status.asException(Status.java:541)
                                                                                                    	at com.google.firebase.firestore.util.Util.exceptionFromStatus(Util.java:153)
                                                                                                    	at com.google.firebase.firestore.remote.Datastore$1.onClose(Datastore.java:185) 
                                                                                                    	at com.google.firebase.firestore.remote.FirestoreChannel$3.onClose(FirestoreChannel.java:237) 
                                                                                                    	at io.grpc.internal.ClientCallImpl.closeObserver(ClientCallImpl.java:574) 
                                                                                                    	at io.grpc.internal.ClientCallImpl.access$300(ClientCallImpl.java:72) 
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInternal(ClientCallImpl.java:742) 
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInContext(ClientCallImpl.java:723) 
                                                                                                    	at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37) 
                                                                                                    	at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133) 
                                                                                                    	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:520) 
                                                                                                    	at java.util.concurrent.FutureTask.run(FutureTask.java:317) 
                                                                                                    	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:348) 
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1154) 
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:652) 
                                                                                                    	at com.google.firebase.firestore.util.AsyncQueue$SynchronizedShutdownAwareExecutor$DelayedStartFactory.run(AsyncQueue.java:235) 
                                                                                                    	at java.lang.Thread.run(Thread.java:1563) 
2026-05-05 17:02:26.019  9989-9989  InsetsController        com.dutype.app                       D  hide(ime(), fromIme=false)
2026-05-05 17:02:26.019  9989-9989  ImeTracker              com.dutype.app                       I  com.dutype.app:67b3caa5: onRequestHide at ORIGIN_CLIENT reason HIDE_SOFT_INPUT_BY_INSETS_API fromUser false
2026-05-05 17:02:26.020  9989-9989  ImeTracker              com.dutype.app                       I  com.dutype.app:4c998d92: onRequestHide at ORIGIN_CLIENT reason HIDE_SOFT_INPUT_REQUEST_HIDE_WITH_CONTROL fromUser false
2026-05-05 17:02:26.022  9989-9989  FCMTokenManager         com.dutype.app                       I  FCMTokenManager: Subscribed to topic: all_users
2026-05-05 17:02:26.025  9989-9989  RemoteInpu...ectionImpl com.dutype.app                       W  requestCursorUpdates on inactive InputConnection
2026-05-05 17:02:26.025  9989-9989  WindowOnBackDispatcher  com.dutype.app                       W  sendCancelIfRunning: isInProgress=false callback=ImeCallback=ImeOnBackInvokedCallback@13762299 Callback=android.window.IOnBackInvokedCallback$Stub$Proxy@3d741bc
2026-05-05 17:02:26.051  9989-9989  InsetsController        com.dutype.app                       D  hide(ime(), fromIme=true)
2026-05-05 17:02:26.052  9989-9989  ImeTracker              com.dutype.app                       I  com.dutype.app:4c998d92: onCancelled at PHASE_CLIENT_APPLY_ANIMATION
2026-05-05 17:02:26.237  9989-9989  ImeTracker              com.dutype.app                       I  com.dutype.app:b7f9ba0b: onRequestHide at ORIGIN_CLIENT reason HIDE_SOFT_INPUT_ON_ANIMATION_STATE_CHANGED fromUser false
2026-05-05 17:02:26.242  9989-9989  ImeTracker              com.dutype.app                       I  com.dutype.app:67b3caa5: onHidden


and> Task :app:compileDebugKotlin
e: file:///C:/Users/vamsi/StudioProjects/DutyPe/app/src/main/java/com/example/dutype/worker/screens/WorkerHomeScreenComponents.kt:636:29 Unresolved reference 'item'.
e: file:///C:/Users/vamsi/StudioProjects/DutyPe/app/src/main/java/com/example/dutype/worker/screens/WorkerHomeScreenComponents.kt:637:33 @Composable invocations can only happen from the context of a @Composable function
e: file:///C:/Users/vamsi/StudioProjects/DutyPe/app/src/main/java/com/example/dutype/worker/screens/WorkerHomeScreenComponents.kt:1777:34 Unresolved reference 'clip'.

> Task :app:compileDebugKotlin FAILED


> Task :app:kspDebugKotlin
e: file:///C:/Users/vamsi/StudioProjects/DutyPe/app/src/main/java/com/example/dutype/worker/screens/WorkerHomeScreenComponents.kt:1921:52 Expecting an element
e: file:///C:/Users/vamsi/StudioProjects/DutyPe/app/src/main/java/com/example/dutype/worker/screens/WorkerHomeScreenComponents.kt:1921:53 Expecting ','
e: file:///C:/Users/vamsi/StudioProjects/DutyPe/app/src/main/java/com/example/dutype/worker/screens/WorkerHomeScreenComponents.kt:1924:30 Expecting ')'
e: file:///C:/Users/vamsi/StudioProjects/DutyPe/app/src/main/java/com/example/dutype/worker/screens/WorkerHomeScreenComponents.kt:1931:1 Expecting a top level declaration

> Task :app:kspDebugKotlin FAILED

 15:18:05.248 12678-12678 ImeTracker              com.dutype.app                       I  com.dutype.app:5a8d1b88: onRequestHide at ORIGIN_CLIENT reason HIDE_SOFT_INPUT_ON_ANIMATION_STATE_CHANGED fromUser false
2026-05-05 15:18:05.249 12678-12678 ImeTracker              com.dutype.app                       I  com.google.android.inputmethod.latin:b7b81fda: onHidden
2026-05-05 15:18:05.925 12678-12678 ScrollIdentify          com.dutype.app                       I  on fling
2026-05-05 15:18:06.945 12678-12678 InsetsController        com.dutype.app                       D  hide(ime(), fromIme=false)
2026-05-05 15:18:06.946 12678-12678 ImeTracker              com.dutype.app                       I  com.dutype.app:1ee10f2e: onRequestHide at ORIGIN_CLIENT reason HIDE_SOFT_INPUT_BY_INSETS_API fromUser false
2026-05-05 15:18:06.946 12678-12678 ImeTracker              com.dutype.app                       I  com.dutype.app:1ee10f2e: onCancelled at PHASE_CLIENT_APPLY_ANIMATION
2026-05-05 15:18:06.986 12678-12678 ViewRootImpl            com.dutype.app                       D  AppSizeAfterRelayout, size: Point(992, 660), rotation: ROTATION_0
2026-05-05 15:18:06.986 12678-12678 BufferQueueConsumer     com.dutype.app                       D  [](id:318600000002,api:0,p:-1,c:12678) connect: controlledByApp=false
2026-05-05 15:18:06.986 12678-12678 BLASTBufferQueue        com.dutype.app                       D  [VRI[Pop-Up Window]#2](f:0,a:0) constructor()
2026-05-05 15:18:06.986 12678-12678 BLASTBufferQueue        com.dutype.app                       D  [VRI[Pop-Up Window]#2](f:0,a:0) update width=992 height=660 format=-3 mTransformHint=0
2026-05-05 15:18:06.988 12678-20384 BLASTBufferQueue        com.dutype.app                       I  QoSAllocBuff Successful. Ret 0 
2026-05-05 15:18:06.993 12678-20384 BLASTBufferQueue        com.dutype.app                       D  [VRI[Pop-Up Window]#2](f:0,a:1) acquireNextBufferLocked size=992x660 mFrameNumber=1 applyTransaction=true mTimestamp=4474214510727(auto) mPendingTransactions.size=0 graphicBufferId=54451595378704 transform=0
2026-05-05 15:18:07.647 12678-12678 MandatoryW...etupScreen com.dutype.app                       D  Form validation - step=3, step1Valid=true, step2Valid=true, step3Valid=true, currentValid=true
2026-05-05 15:18:07.730 12678-12678 WindowOnBackDispatcher  com.dutype.app                       W  sendCancelIfRunning: isInProgress=false callback=androidx.compose.ui.window.Api33Impl$$ExternalSyntheticLambda0@3a3a2bc
2026-05-05 15:18:07.733 12678-20384 HWUI                    com.dutype.app                       D  endAllActiveAnimators on 0xb400006d400f5280 (UnprojectedRipple) with handle 0xb400006ce00b5e90
2026-05-05 15:18:07.743 12678-20349 BLASTBufferQueue        com.dutype.app                       D  [VRI[Pop-Up Window]#2](f:0,a:5) destructor()
2026-05-05 15:18:07.743 12678-20349 BufferQueueConsumer     com.dutype.app                       D  [VRI[Pop-Up Window]#2(BLAST Consumer)2](id:318600000002,api:0,p:-1,c:12678) disconnect
2026-05-05 15:18:07.765 12678-12678 ImeFocusController      com.dutype.app                       V  onWindowFocus: androidx.compose.ui.platform.AndroidComposeView{6eac02d VFED..... .F...... 0,0-1080,2400 aid=1073741825} softInputMode=STATE_UNSPECIFIED|ADJUST_PAN
2026-05-05 15:18:08.144 12678-12678 MandatoryW...etupScreen com.dutype.app                       D  Form validation - step=3, step1Valid=true, step2Valid=true, step3Valid=true, currentValid=true
2026-05-05 15:18:08.845 12678-20399 Firestore               com.dutype.app                       W  (25.1.4) [WriteStream]: (e2bb78e) Stream closed with status: Status{code=PERMISSION_DENIED, description=Missing or insufficient permissions., cause=null}.
2026-05-05 15:18:08.857 12678-20399 Firestore               com.dutype.app                       W  (25.1.4) [Firestore]: Write failed at phoneRoles/+919390693988: Status{code=PERMISSION_DENIED, description=Missing or insufficient permissions., cause=null}
2026-05-05 15:18:08.862 12678-12678 ProfileCom...ionService com.dutype.app                       E  ❌ ProfileCompletionService.saveWorkerProfileData - Error: PERMISSION_DENIED: Missing or insufficient permissions. (Fix with AI)
                                                                                                    com.google.firebase.firestore.FirebaseFirestoreException: PERMISSION_DENIED: Missing or insufficient permissions.
                                                                                                    	at com.google.firebase.firestore.util.Util.exceptionFromStatus(Util.java:155)
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.notifyUser(SyncEngine.java:629)
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.handleRejectedWrite(SyncEngine.java:511)
                                                                                                    	at com.google.firebase.firestore.core.MemoryComponentProvider$RemoteStoreCallback.handleRejectedWrite(MemoryComponentProvider.java:135)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.handleWriteError(RemoteStore.java:748)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.handleWriteStreamClose(RemoteStore.java:704)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.access$600(RemoteStore.java:60)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore$2.onClose(RemoteStore.java:218)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream.close(AbstractStream.java:365)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream.handleServerClose(AbstractStream.java:419)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.lambda$onClose$3$com-google-firebase-firestore-remote-AbstractStream$StreamObserver(AbstractStream.java:160)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver$$ExternalSyntheticLambda3.run(D8$$SyntheticClass:0)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$CloseGuardedRunner.run(AbstractStream.java:67)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.onClose(AbstractStream.java:146)
                                                                                                    	at com.google.firebase.firestore.remote.FirestoreChannel$1.onClose(FirestoreChannel.java:173)
                                                                                                    	at io.grpc.internal.ClientCallImpl.closeObserver(ClientCallImpl.java:574)
                                                                                                    	at io.grpc.internal.ClientCallImpl.access$300(ClientCallImpl.java:72)
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInternal(ClientCallImpl.java:742)
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInContext(ClientCallImpl.java:723)
                                                                                                    	at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37)
                                                                                                    	at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133)
                                                                                                    	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:520)
                                                                                                    	at java.util.concurrent.FutureTask.run(FutureTask.java:317)
                                                                                                    	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:348)
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1154)
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:652)
                                                                                                    	at com.google.firebase.firestore.util.AsyncQueue$SynchronizedShutdownAwareExecutor$DelayedStartFactory.run(AsyncQueue.java:235)
                                                                                                    	at java.lang.Thread.run(Thread.java:1563)
                                                                                                    Caused by: io.grpc.StatusException: PERMISSION_DENIED: Missing or insufficient permissions.
                                                                                                    	at io.grpc.Status.asException(Status.java:541)
                                                                                                    	at com.google.firebase.firestore.util.Util.exceptionFromStatus(Util.java:153)
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.notifyUser(SyncEngine.java:629) 
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.handleRejectedWrite(SyncEngine.java:511) 
                                                                                                    	at com.google.firebase.firestore.core.MemoryComponentProvider$RemoteStoreCallback.handleRejectedWrite(MemoryComponentProvider.java:135) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.handleWriteError(RemoteStore.java:748) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.handleWriteStreamClose(RemoteStore.java:704) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.access$600(RemoteStore.java:60) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore$2.onClose(RemoteStore.java:218) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream.close(AbstractStream.java:365) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream.handleServerClose(AbstractStream.java:419) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.lambda$onClose$3$com-google-firebase-firestore-remote-AbstractStream$StreamObserver(AbstractStream.java:160) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver$$ExternalSyntheticLambda3.run(D8$$SyntheticClass:0) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$CloseGuardedRunner.run(AbstractStream.java:67) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.onClose(AbstractStream.java:146) 
                                                                                                    	at com.google.firebase.firestore.remote.FirestoreChannel$1.onClose(FirestoreChannel.java:173) 
                                                                                                    	at io.grpc.internal.ClientCallImpl.closeObserver(ClientCallImpl.java:574) 
                                                                                                    	at io.grpc.internal.ClientCallImpl.access$300(ClientCallImpl.java:72) 
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInternal(ClientCallImpl.java:742) 
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInContext(ClientCallImpl.java:723) 
                                                                                                    	at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37) 
                                                                                                    	at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133) 
                                                                                                    	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:520) 
                                                                                                    	at java.util.concurrent.FutureTask.run(FutureTask.java:317) 
                                                                                                    	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:348) 
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1154) 
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:652) 
                                                                                                    	at com.google.firebase.firestore.util.AsyncQueue$SynchronizedShutdownAwareExecutor$DelayedStartFactory.run(AsyncQueue.java:235) 
                                                                                                    	at java.lang.Thread.run(Thread.java:1563) 
2026-05-05 15:18:09.512 12678-12678 ScrollIdentify          com.dutype.app                       I  on fling
2026-05-05 15:19:09.518 12678-12678 MainActivity            com.dutype.app                       D  📱 MainActivity.onPause()
2026-05-05 15:19:09.527 12678-12678 GuestEngag...$Companion com.dutype.app                       D  🔔 EngagementWorker: background scheduled (3m)
2026-05-05 15:19:09.539 12678-12678 VRI[MainActivity]       com.dutype.app                       D  visibilityChanged oldVisibility=true newVisibility=false
2026-05-05 15:19:09.572 12678-12678 ViewRootImpl            com.dutype.app                       D  AppSizeAfterRelayout, size: Point(1080, 2400), rotation: ROTATION_0
2026-05-05 15:19:09.572 12678-12678 BLASTBufferQueue        com.dutype.app                       D  [VRI[MainActivity]#0](f:0,a:5) destructor()
2026-05-05 15:19:09.573 12678-12678 BufferQueueConsumer     com.dutype.app                       D  [VRI[MainActivity]#0(BLAST Consumer)0](id:318600000000,api:0,p:-1,c:12678) disconnect
2026-05-05 15:19:09.582 12678-12678 WindowOnBackDispatcher  com.dutype.app                       W  sendCancelIfRunning: isInProgress=false callback=androidx.activity.OnBackPressedDispatcher$Api34Impl$createOnBackAnimationCallback$1@4a8d1f3
2026-05-05 15:19:09.743 12678-20397 WM-PackageManagerHelper com.dutype.app                       D  Skipping component enablement for androidx.work.impl.background.systemalarm.RescheduleReceiver
2026-05-05 15:19:09.776 12678-20397 WM-SystemJobScheduler   com.dutype.app                       D  Scheduling work ID c3bcac22-1738-4710-87ce-db6a8b1f473fJob ID 6

