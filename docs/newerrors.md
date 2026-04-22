026-04-22 11:54:43.275 12378-12378 UserMetadata            com.dutype.app                       D  📊 Loading basic profile for user: Tuv3JzF2rQU53fJRPjHSXhe8CaJ2
2026-04-22 11:54:43.420 12378-19531 Firestore               com.dutype.app                       W  (25.1.4) [WriteStream]: (6b5a0c5) Stream closed with status: Status{code=PERMISSION_DENIED, description=Missing or insufficient permissions., cause=null}.
2026-04-22 11:54:43.430 12378-19531 Firestore               com.dutype.app                       W  (25.1.4) [Firestore]: Write failed at users/Tuv3JzF2rQU53fJRPjHSXhe8CaJ2: Status{code=PERMISSION_DENIED, description=Missing or insufficient permissions., cause=null}
2026-04-22 11:54:43.433 12378-12378 UserMetadata            com.dutype.app                       E  📊 Failed to load user stats (Fix with AI)
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
2026-04-22 11:54:43.435 12378-12378 UserMetadata            com.dutype.app                       D  📊 Error fallback - using Auth phone: +919390693988
2026-04-22 11:54:43.435 12378-12378 UserMetadata            com.dutype.app                       D  📊 Basic profile loaded successfully
2026-04-22 11:54:43.436 12378-12378 WorkerProf...fileScreen com.dutype.app                       I  Worker profile (lightweight) - Name: , Phone: +919390693988
2026-04-22 11:54:43.545 12378-19532 NetworkMon...1$callback com.dutype.app                       D  🌐 Network available: 1 network(s)
2026-04-22 11:54:44.976 12378-12378 ProfileSet...ateManager com.dutype.app                       D  isProfileComplete - role: WORKER
2026-04-22 11:54:44.987 12378-12378 ProfileSet...ateManager com.dutype.app                       D  isProfileComplete result: true
2026-04-22 11:54:45.089 12378-12378 ProfileCom...ionService com.dutype.app                       D  🔍 ProfileCompletionService.calculateWorkerProfileCompletion for userId: Tuv3JzF2rQU53fJRPjHSXhe8CaJ2
2026-04-22 11:54:45.090 12378-12378 ProfileCom...ionService com.dutype.app                       D  🔍 Firebase userData keys: [createdAt, role, phone, fullName, fcmToken]
2026-04-22 11:54:45.090 12378-12378 ProfileCom...ionService com.dutype.app                       D  🔍 Phone field exists: true
2026-04-22 11:54:45.090 12378-12378 ProfileCom...ionService com.dutype.app                       D  🔍 Worker profile exists: true
2026-04-22 11:
2026-04-22 11:54:44.976 12378-12378 ProfileSet...ateManager com.dutype.app                       D  isProfileComplete - role: WORKER
2026-04-22 11:54:44.987 12378-12378 ProfileSet...ateManager com.dutype.app                       D  isProfileComplete result: true
2026-04-22 11:54:45.089 12378-12378 ProfileCom...ionService com.dutype.app                       D  🔍 ProfileCompletionService.calculateWorkerProfileCompletion for userId: Tuv3JzF2rQU53fJRPjHSXhe8CaJ2
2026-04-22 11:54:45.090 12378-12378 ProfileCom...ionService com.dutype.app                       D  🔍 Firebase userData keys: [createdAt, role, phone, fullName, fcmToken]
2026-04-22 11:54:45.090 12378-12378 ProfileCom...ionService com.dutype.app                       D  🔍 Phone field exists: true
2026-04-22 11:54:45.090 12378-12378 ProfileCom...ionService com.dutype.app                       D  🔍 Worker profile exists: true
2026-04-22 11:54:45.091 12378-12378 ProfileCom...ionService com.dutype.app                       D  🔍 ProfileCompletionService - Final completion percentage: 95%
2026-04-22 11:54:45.333 12378-19676 FirebaseContextProvider com.dutype.app                       W  Error getting App Check token. Error: com.google.firebase.FirebaseException: Too many attempts.
2026-04-22 11:54:54.146 12378-18992 ReferralSe...gistration com.dutype.app                       D  🎁 REFERRAL: Stats updated from Firestore - Code: VAMSI3739, Total: 0, Successful: 0, Earnings: ₹0.0, Balance: ₹0.0, Tier: BRONZE
2026-04-22 11:55:14.980 12378-12378 MainActivity            com.dutype.app                       D  📱 MainActivity.onPause()
2026-04-22 11:55:14.984 12378-12378 GuestEngag...$Companion com.dutype.app                       D  🔔 EngagementWorker: background scheduled (3m)
2026-04-22 11:55:14.997 12378-12378 VRI[MainActivity]       com.dutype.app                       D  visibilityChanged oldVisibility=true newVisibility=false
2026-04-22 11:55:15.095 12378-12378 ViewRootImpl            com.dutype.app                       D  AppSizeAfterRelayout, size: Point(1080, 2400), rotation: ROTATION_0
2026-04-22 11:55:15.096 12378-12378 BLASTBufferQueue        com.dutype.app                       D  [VRI[MainActivity]#4](f:0,a:5) destructor()
2026-04-22 11:55:15.096 12378-12378 BufferQueueConsumer     com.dutype.app                       D  [VRI[MainActivity]#4(BLAST Consumer)4](id:305a00000004,api:0,p:-1,c:12378) disconnect
2026-04-22 11:55:15.141 12378-18997 WM-PackageManagerHelper com.dutype.app                       D  Skipping component enablement for androidx.work.impl.background.systemalarm.RescheduleReceiver
2026-04-22 11:55:15.144 12378-12378 WindowOnBackDispatcher  com.dutype.app                       W  sendCancelIfRunning: isInProgress=false callback=androidx.activity.OnBackPressedDispatcher$Api34Impl$createOnBackAnimationCallback$1@87a16b3
2026-04-22 11:55:15.242 12378-18997 WM-SystemJobScheduler   com.dutype.app                       D  Scheduling work ID d04611f5-dd7d-45dc-ab50-fb8b920ca4adJob ID 6
2. why referral code taking time to load annd show in therefer and earcn screeen 
3.username ,image after uplaoded is not showing in the profielscreen - you checck the logs yyou can see but when iclicked pn the profielfalmenuitem- i naviagetd to  the workerprofie detailscreen - there every data is there 
checck from which collcetion we are showing the name and phone number inthe  worker profielcreena nd let me know fix and also  same fix for the empoyer

signut button taking too much time to soignnout and showing the bottom sheet with signoutour prgoress more time innthe logut bottomsheet

when the user isatellls the app and seleccts the languate as telugu thats not immedialy updating-after selecting cliking next i came to onbarod screens there it showing the enhglsih, in selecct role sccreen and also  int other sccreens too

spalsh screen is good but the text on the splash screen is not quality and if for the current spalsh screen if there a chance to make the text oin the splash screen animated then make it work but  dont create any other ,because its casuingtheperfiomacen of the app

try to remove /uncomment but dont delete the exsitng on baording images replace with aweome on baording ui/ux makes feel better and it should suit for the both worker and employer becuase we are showing before teh seleting the role

