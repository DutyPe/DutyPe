1.dont sjow the google maps in the employer profielscetup screen , keep in thee job psoting screen only,dont touch the job posting screen, just remove the google maps preview in the employer profielsetup  screen
1.1 in job dscription the actaul education :relatedfield not shiwng even the qualifcaition give ,check its aleardy woriking or not

Maps auth fix notes:

Log symptom: Google Android Maps SDK shows `Authorization failure` for package `com.dutype.app`.

Do not commit the raw Maps API key in repo docs. Keep the key in Google Cloud / app config only.

Steps to fix in Google Cloud Console:

1. Open Google Cloud Console for the Firebase project behind DutyPe.
2. Go to `APIs & Services` -> `Library`.
3. Search for `Maps SDK for Android` and enable it.
4. Go to `APIs & Services` -> `Credentials`.
5. Open the Android Maps API key used by the app.
6. Under `Application restrictions`, choose `Android apps`.
7. Add an Android app restriction with package name `com.dutype.app`.
8. Add the certificate SHA-1 fingerprint for the build you are testing:
	- Debug/local build: run `./gradlew.bat :app:signingReport` and copy the SHA-1 for the debug variant.
	- Play Store/release build: open Play Console -> `Setup` -> `App integrity` and copy the app signing certificate SHA-1.
9. Under `API restrictions`, restrict the key to `Maps SDK for Android`.
10. Save the key, wait a few minutes for propagation, then reinstall/open the app and test the job posting map.

Firebase note: Firebase Console links to the same Google Cloud project, but Maps API enablement and Android key restrictions are managed in Google Cloud Console under `APIs & Services`.




2026-04-25 22:19:53.864 18039-24522 Google Android Maps SDK com.dutype.app                       E  Authorization failure.  Please see ****************************************************************** for how to correctly set up the map.
2026-04-25 22:19:53.867 18039-24522 Google Android Maps SDK com.dutype.app                       E  In the Google Developer Console (**************************************
                                                                                                    Ensure that the "Maps SDK for Android" is enabled.
                                                                                                    Ensure that the following Android Key exists:
																										API Key: <GOOGLE_MAPS_API_KEY>
                                                                                                    	Android Application (<cert_fingerprint>;<package_name>): 




1. shift on the employes posted job card in the employer homesccreen not showing the exact one may be of isssue number 3 listed bewlow
2hiring urgencyy chips are not good like orher chips these are streching 
3, checck eacch and every field in the job posting screen are storing really the original values or not please have a look, some are not saving in the db what i actaully given , some only exactly storing please fix those 
4. edit job screen not showing the shift section-pleas chcck that too
5. add the pencil icon and share icon at the top right of the employercard in the employer homescreen we are using and show the postions,shift under the location /adreess 
keep the active or not -but here it should only show the active or expired status like the job postinng history screen card are showing
6. in the apply for a job screen where the worker see the submit application button- cna you  show the button in the bottombar level andd after submit cliking we are showingthe application sent with return to home
can make this button to not the exact black color fillde means make it the button to be little black color and show the check mark animating or pulsating like that 

7. login screen and register screen are showing the back arrow button little down fromm the top 

8. after the application recived frommthe worker the job applications screen is not showing the worker name -just take the worker name inn the apploications collection along the workerid,jobid,staus, etc..dont addd other fields 

and on click this card -it should take us to the workerr profielsccreen with workerid ,with that id user relaed data we need to extract and show exactly in this screen
  these are the logs 
  --------- beginning of main
2026-04-26 11:16:07.419  3064-3064  EmployerAp...plications com.dutype.app                       D  [EmployerApplicationViewModel] Loading job applications for jobId=QKFHoMmgPWNiz96zHmYV
2026-04-26 11:16:07.420  3064-6467  JobApplica...plications com.dutype.app                       D  [JobApplicationService] Getting applications for jobId: QKFHoMmgPWNiz96zHmYV
2026-04-26 11:16:07.521  3064-8703  JobApplica...plications com.dutype.app                       D  [JobApplicationService] Found 1 applications for jobId: QKFHoMmgPWNiz96zHmYV
2026-04-26 11:16:07.523  3064-8703  JobApplica...plications com.dutype.app                       D  [JobApplicationService] Application QKFHoMmgPWNiz96zHmYV_g2qjCUuxNkUaEkgkZzgCWseecpE3 for job QKFHoMmgPWNiz96zHmYV
2026-04-26 11:16:07.653  3064-8703  JobApplica...plications com.dutype.app                       D  [JobApplicationService] Returning 1 applications for jobId: QKFHoMmgPWNiz96zHmYV
2026-04-26 11:16:07.658  3064-3064  EmployerAp...plications com.dutype.app                       D  [EmployerApplicationViewModel] Successfully loaded 1 applications for job QKFHoMmgPWNiz96zHmYV
2026-04-26 11:16:08.013  3064-6469  FirebaseContextProvider com.dutype.app                       W  Error getting App Check token. Error: com.google.firebase.FirebaseException: Error returned from API. code: 403 body: App attestation failed.
2026-04-26 11:16:08.081  3064-3064  ProfileCom...ionService com.dutype.app                       W  getWorkerProfileForEmployer failed for workerId=g2qjCUuxNkUaEkgkZzgCWseecpE3 (Fix with AI)
                                                                                                    com.google.firebase.functions.FirebaseFunctionsException: Unauthenticated
                                                                                                    	at com.google.firebase.functions.FirebaseFunctionsException$Companion.fromResponse$com_google_firebase_firebase_functions(FirebaseFunctionsException.kt:234)
                                                                                                    	at com.google.firebase.functions.FirebaseFunctions$call$5.onResponse(FirebaseFunctions.kt:273)
                                                                                                    	at okhttp3.internal.connection.RealCall$AsyncCall.run(RealCall.kt:519)
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1154)
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:652)
                                                                                                    	at java.lang.Thread.run(Thread.java:1563)
2026-04-26 11:16:08.082  3064-3064  EmployerAp...nViewModel com.dutype.app                       W  [EmployerVM] Failed to fetch worker profile for g2qjCUuxNkUaEkgkZzgCWseecpE3: Unauthenticated
--------- beginning of system
2026-04-26 11:16:22.285  3064-3064  RetryUtils              com.dutype.app                       D  Attempt 1 of 3
2026-04-26 11:16:22.370  3064-6539  FirebaseContextProvider com.dutype.app                       W  Error getting App Check token. Error: com.google.firebase.FirebaseException: Too many attempts.
2026-04-26 11:16:22.433  3064-3064  ProfileCom...ionService com.dutype.app                       D  🔍 ProfileCompletionService.calculateWorkerProfileCompletion for userId: 9YLSsCagoffBY00iewP2xNCjqVr1
2026-04-26 11:16:22.433  3064-3064  ProfileCom...ionService com.dutype.app                       D  🔍 Firebase userData keys: [createdAt, role, phone, fullName, fcmToken]
2026-04-26 11:16:22.434  3064-3064  ProfileCom...ionService com.dutype.app                       D  🔍 Phone field exists: true
2026-04-26 11:16:22.434  3064-3064  ProfileCom...ionService com.dutype.app                       D  🔍 Worker profile exists: false
2026-04-26 11:16:22.435  3064-3064  ProfileCom...ionService com.dutype.app                       W  ⚠️ ProfileCompletionService - worker_profiles empty/unreadable for 9YLSsCagoffBY00iewP2xNCjqVr1; reporting users-only score
2026-04-26 11:16:22.436  3064-3064  ProfileCom...ionService com.dutype.app                       D  🔍 ProfileCompletionService - Final completion percentage: 60%
2026-04-26 11:16:22.462  3064-3064  ProfileCom...ionService com.dutype.app                       W  getWorkerProfileForEmployer failed for workerId=g2qjCUuxNkUaEkgkZzgCWseecpE3 (Fix with AI)
                                                                                                    com.google.firebase.functions.FirebaseFunctionsException: Unauthenticated
                                                                                                    	at com.google.firebase.functions.FirebaseFunctionsException$Companion.fromResponse$com_google_firebase_firebase_functions(FirebaseFunctionsException.kt:234)
                                                                                                    	at com.google.firebase.functions.FirebaseFunctions$call$5.onResponse(FirebaseFunctions.kt:273)
                                                                                                    	at okhttp3.internal.connection.RealCall$AsyncCall.run(RealCall.kt:519)
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1154)
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:652)
                                                                                                    	at java.lang.Thread.run(Thread.java:1563)
2026-04-26 11:16:22.523  3064-3064  SmartJobAp...pabilities com.dutype.app                       D  🔍 loadUserCapabilities - completion: 60%, canApply: false
2026-04-26 11:16:26.405  3064-6910  BufferPoolAccessor2.0   com.dutype.app                       D  bufferpool2 0xb40000789009bbc8 : 0(0 size) total buffers - 0(0 size) used buffers - 17/23 (recycle/alloc) - 6/23 (fetch/transfer)
2026-04-26 11:16:31.405  3064-6909  BufferPoolAccessor2.0   com.dutype.app                       D  bufferpool2 0xb40000789009bbc8 : 0(0 size) total buffers - 0(0 size) used buffers - 17/23 (recycle/alloc) - 6/23 (fetch/transfer)
2026-04-26 11:16:31.406  3064-6909  BufferPoolAccessor2.0   com.dutype.app                       D  evictor expired: 1, evicted: 1


have look at the logs and just do what i said ,if any firestore related isssue for reading the worker proile please fix that--dont addd morefields in the jobapplciatons fields except the worker name -becasue we need to sjhow in the job applciationscreen
after that with worker id we need to get the workerr prolfiel data fully in the worekr detetails screen

and in this worker profielsivew screen -show the reject button left side and accept button right sde and remove the actionstext 

9.6-04-26 11:22:03.777  3064-6772  Firestore               com.dutype.app                       W  (25.1.4) [Firestore]: Listen for Query(target=Query(applications where jobId==QKFHoMmgPWNiz96zHmYV and status==hired order by __name__);limitType=LIMIT_TO_FIRST) failed: Status{code=PERMISSION_DENIED, description=Missing or insufficient permissions., cause=null}
2026-04-26 11:22:03.794  3064-3064  JobApplicationService   com.dutype.app                       E  Error checking vacancy availability (Fix with AI)
                                                                                                    com.google.firebase.firestore.FirebaseFirestoreException: PERMISSION_DENIED: Missing or insufficient permissions.
                                                                                                    	at com.google.firebase.firestore.util.Util.exceptionFromStatus(Util.java:155)
                                                                                                    	at com.google.firebase.firestore.core.EventManager.onError(EventManager.java:247)
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.removeAndCleanupTarget(SyncEngine.java:642)
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.handleRejectedListen(SyncEngine.java:478)
                                                                                                    	at com.google.firebase.firestore.core.MemoryComponentProvider$RemoteStoreCallback.handleRejectedListen(MemoryComponentProvider.java:125)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.processTargetError(RemoteStore.java:596)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.handleWatchChange(RemoteStore.java:479)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.access$100(RemoteStore.java:60)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore$1.onWatchChange(RemoteStore.java:188)
                                                                                                    	at com.google.firebase.firestore.remote.WatchStream.onNext(WatchStream.java:114)
                                                                                                    	at com.google.firebase.firestore.remote.WatchStream.onNext(WatchStream.java:38)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.lambda$onNext$1$com-google-firebase-firestore-remote-AbstractStream$StreamObserver(AbstractStream.java:126)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver$$ExternalSyntheticLambda0.run(D8$$SyntheticClass:0)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$CloseGuardedRunner.run(AbstractStream.java:67)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.onNext(AbstractStream.java:113)
                                                                                                    	at com.google.firebase.firestore.remote.FirestoreChannel$1.onMessage(FirestoreChannel.java:162)
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1MessagesAvailable.runInternal(ClientCallImpl.java:667)
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1MessagesAvailable.runInContext(ClientCallImpl.java:654)
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
                                                                                                    	at com.google.firebase.firestore.core.EventManager.onError(EventManager.java:247) 
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.removeAndCleanupTarget(SyncEngine.java:642) 
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.handleRejectedListen(SyncEngine.java:478) 
                                                                                                    	at com.google.firebase.firestore.core.MemoryComponentProvider$RemoteStoreCallback.handleRejectedListen(MemoryComponentProvider.java:125) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.processTargetError(RemoteStore.java:596) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.handleWatchChange(RemoteStore.java:479) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.access$100(RemoteStore.java:60) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore$1.onWatchChange(RemoteStore.java:188) 
                                                                                                    	at com.google.firebase.firestore.remote.WatchStream.onNext(WatchStream.java:114) 
                                                                                                    	at com.google.firebase.firestore.remote.WatchStream.onNext(WatchStream.java:38) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.lambda$onNext$1$com-google-firebase-firestore-remote-AbstractStream$StreamObserver(AbstractStream.java:126) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver$$ExternalSyntheticLambda0.run(D8$$SyntheticClass:0) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$CloseGuardedRunner.run(AbstractStream.java:67) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.onNext(AbstractStream.java:113) 
                                                                                                    	at com.google.firebase.firestore.remote.FirestoreChannel$1.onMessage(FirestoreChannel.java:162) 
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1MessagesAvailable.runInternal(ClientCallImpl.java:667) 
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1MessagesAvailable.runInContext(ClientCallImpl.java:654) 
                                                                                                    	at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37) 
                                                                                                    	at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133) 
                                                                                                    	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:520) 
                                                                                                    	at java.util.concurrent.FutureTask.run(FutureTask.java:317) 
                                                                                                    	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:348) 
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1154) 
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:652) 
                                                                                                    	at com.google.firebase.firestore.util.AsyncQueue$SynchronizedShutdownAwareExecutor$DelayedStartFactory.run(AsyncQueue.java:235) 
                                                                                                    	at java.lang.Thread.run(Thread.java:1563) 
2026-04-26 11:22:05.959  3064-6772  Firestore               com.dutype.app                       W  (25.1.4) [Firestore]: Listen for Query(target=Query(applications where jobId==QKFHoMmgPWNiz96zHmYV and status==hired order by __name__);limitType=LIMIT_TO_FIRST) failed: Status{code=PERMISSION_DENIED, description=Missing or insufficient permissions., cause=null}
2026-04-26 11:22:05.968  3064-3064  JobApplicationService   com.dutype.app                       E  Error checking vacancy availability (Fix with AI)
                                                                                                    com.google.firebase.firestore.FirebaseFirestoreException: PERMISSION_DENIED: Missing or insufficient permissions.
                                                                                                    	at com.google.firebase.firestore.util.Util.exceptionFromStatus(Util.java:155)
                                                                                                    	at com.google.firebase.firestore.core.EventManager.onError(EventManager.java:247)
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.removeAndCleanupTarget(SyncEngine.java:642)
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.handleRejectedListen(SyncEngine.java:478)
                                                                                                    	at com.google.firebase.firestore.core.MemoryComponentProvider$RemoteStoreCallback.handleRejectedListen(MemoryComponentProvider.java:125)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.processTargetError(RemoteStore.java:596)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.handleWatchChange(RemoteStore.java:479)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.access$100(RemoteStore.java:60)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore$1.onWatchChange(RemoteStore.java:188)
                                                                                                    	at com.google.firebase.firestore.remote.WatchStream.onNext(WatchStream.java:114)
                                                                                                    	at com.google.firebase.firestore.remote.WatchStream.onNext(WatchStream.java:38)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.lambda$onNext$1$com-google-firebase-firestore-remote-AbstractStream$StreamObserver(AbstractStream.java:126)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver$$ExternalSyntheticLambda0.run(D8$$SyntheticClass:0)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$CloseGuardedRunner.run(AbstractStream.java:67)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.onNext(AbstractStream.java:113)
                                                                                                    	at com.google.firebase.firestore.remote.FirestoreChannel$1.onMessage(FirestoreChannel.java:162)
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1MessagesAvailable.runInternal(ClientCallImpl.java:667)
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1MessagesAvailable.runInContext(ClientCallImpl.java:654)
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
                                                                                                    	at com.google.firebase.firestore.core.EventManager.onError(EventManager.java:247) 
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.removeAndCleanupTarget(SyncEngine.java:642) 
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.handleRejectedListen(SyncEngine.java:478) 
                                                                                                    	at com.google.firebase.firestore.core.MemoryComponentProvider$RemoteStoreCallback.handleRejectedListen(MemoryComponentProvider.java:125) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.processTargetError(RemoteStore.java:596) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.handleWatchChange(RemoteStore.java:479) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.access$100(RemoteStore.java:60) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore$1.onWatchChange(RemoteStore.java:188) 
                                                                                                    	at com.google.firebase.firestore.remote.WatchStream.onNext(WatchStream.java:114) 
                                                                                                    	at com.google.firebase.firestore.remote.WatchStream.onNext(WatchStream.java:38) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.lambda$onNext$1$com-google-firebase-firestore-remote-AbstractStream$StreamObserver(AbstractStream.java:126) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver$$ExternalSyntheticLambda0.run(D8$$SyntheticClass:0) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$CloseGuardedRunner.run(AbstractStream.java:67) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.onNext(AbstractStream.java:113) 
                                                                                                    	at com.google.firebase.firestore.remote.FirestoreChannel$1.onMessage(FirestoreChannel.java:162) 
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1MessagesAvailable.runInternal(ClientCallImpl.java:667) 
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1MessagesAvailable.runInContext(ClientCallImpl.java:654) 
                                                                                                    	at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37) 
                                                                                                    	at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133) 
                                                                                                    	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:520) 
                                                                                                    	at java.util.concurrent.FutureTask.run(FutureTask.java:317) 
                                                                                                    	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:348) 
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1154) 
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:652) 
                                                                                                    	at com.google.firebase.firestore.util.AsyncQueue$SynchronizedShutdownAwareExecutor$DelayedStartFactory.run(AsyncQueue.java:235) 
                                                                                                    	at java.lang.Thread.run(Thread.java:1563) 
2026-04-26 11:22:07.647  3064-6772  Firestore               com.dutype.app                       W  (25.1.4) [Firestore]: Listen for Query(target=Query(applications where jobId==QKFHoMmgPWNiz96zHmYV and status==hired order by __name__);limitType=LIMIT_TO_FIRST) failed: Status{code=PERMISSION_DENIED, description=Missing or insufficient permissions., cause=null}
2026-04-26 11:22:07.658  3064-3064  JobApplicationService   com.dutype.app                       E  Error checking vacancy availability (Fix with AI)
                                                                                                    com.google.firebase.firestore.FirebaseFirestoreException: PERMISSION_DENIED: Missing or insufficient permissions.
                                                                                                    	at com.google.firebase.firestore.util.Util.exceptionFromStatus(Util.java:155)
                                                                                                    	at com.google.firebase.firestore.core.EventManager.onError(EventManager.java:247)
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.removeAndCleanupTarget(SyncEngine.java:642)
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.handleRejectedListen(SyncEngine.java:478)
                                                                                                    	at com.google.firebase.firestore.core.MemoryComponentProvider$RemoteStoreCallback.handleRejectedListen(MemoryComponentProvider.java:125)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.processTargetError(RemoteStore.java:596)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.handleWatchChange(RemoteStore.java:479)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.access$100(RemoteStore.java:60)
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore$1.onWatchChange(RemoteStore.java:188)
                                                                                                    	at com.google.firebase.firestore.remote.WatchStream.onNext(WatchStream.java:114)
                                                                                                    	at com.google.firebase.firestore.remote.WatchStream.onNext(WatchStream.java:38)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.lambda$onNext$1$com-google-firebase-firestore-remote-AbstractStream$StreamObserver(AbstractStream.java:126)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver$$ExternalSyntheticLambda0.run(D8$$SyntheticClass:0)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$CloseGuardedRunner.run(AbstractStream.java:67)
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.onNext(AbstractStream.java:113)
                                                                                                    	at com.google.firebase.firestore.remote.FirestoreChannel$1.onMessage(FirestoreChannel.java:162)
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1MessagesAvailable.runInternal(ClientCallImpl.java:667)
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1MessagesAvailable.runInContext(ClientCallImpl.java:654)
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
                                                                                                    	at com.google.firebase.firestore.core.EventManager.onError(EventManager.java:247) 
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.removeAndCleanupTarget(SyncEngine.java:642) 
                                                                                                    	at com.google.firebase.firestore.core.SyncEngine.handleRejectedListen(SyncEngine.java:478) 
                                                                                                    	at com.google.firebase.firestore.core.MemoryComponentProvider$RemoteStoreCallback.handleRejectedListen(MemoryComponentProvider.java:125) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.processTargetError(RemoteStore.java:596) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.handleWatchChange(RemoteStore.java:479) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore.access$100(RemoteStore.java:60) 
                                                                                                    	at com.google.firebase.firestore.remote.RemoteStore$1.onWatchChange(RemoteStore.java:188) 
                                                                                                    	at com.google.firebase.firestore.remote.WatchStream.onNext(WatchStream.java:114) 
                                                                                                    	at com.google.firebase.firestore.remote.WatchStream.onNext(WatchStream.java:38) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.lambda$onNext$1$com-google-firebase-firestore-remote-AbstractStream$StreamObserver(AbstractStream.java:126) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver$$ExternalSyntheticLambda0.run(D8$$SyntheticClass:0) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$CloseGuardedRunner.run(AbstractStream.java:67) 
                                                                                                    	at com.google.firebase.firestore.remote.AbstractStream$StreamObserver.onNext(AbstractStream.java:113) 
                                                                                                    	at com.google.firebase.firestore.remote.FirestoreChannel$1.onMessage(FirestoreChannel.java:162) 
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1MessagesAvailable.runInternal(ClientCallImpl.java:667) 
                                                                                                    	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1MessagesAvailable.runInContext(ClientCallImpl.java:654) 
                                                                                                    	at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37) 
                                                                                                    	at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133) 
                                                                                                    	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:520) 
                                                                                                    	at java.util.concurrent.FutureTask.run(FutureTask.java:317) 
                                                                                                    	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:348) 
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1154) 
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:652) 
                                                                                                    	at com.google.firebase.firestore.util.AsyncQueue$SynchronizedShutdownAwareExecutor$DelayedStartFactory.run(AsyncQueue.java:235) 
                                                                                                    	at java.lang.Thread.run(Thread.java:1563) 
2026-04-26 11:22:08.036  3064-3064  WindowOnBackDispatcher  com.dutype.app                       W  sendCancelIfRunning: isInProgress=false callback=androidx.activity.OnBackPressedDispatcher$Api34Impl$createOnBackAnimationCallback$1@e54c76a
2026-04-26 11:22:08.048  3064-6470  HWUI                    com.dutype.app                       D  endAllActiveAnimators on 0xb4000077e00601e0 (UnprojectedRipple) with handle 0xb4000078800f2e20
2026-04-26 11:22:08.048  3064-6470  HWUI                    com.dutype.app                       D  endAllActiveAnimators on 0xb4000077e0135c30 (UnprojectedRipple) with handle 0xb4000078800f4620
2026-04-26 11:22:08.053  3064-8689  BLASTBufferQueue        com.dutype.app                       D  [VRI[MainActivity]#4](f:0,a:5) destructor()
2026-04-26 11:22:08.053  3064-8689  BufferQueueConsumer     com.dutype.app                       D  [VRI[MainActivity]#4(BLAST Consumer)4](id:bf800000005,api:0,p:-1,c:3064) disconnect
2026-04-26 11:22:08.108  3064-3064  ImeFocusController      com.dutype.app                       V  onWindowFocus: com.android.internal.policy.DecorView{7ecc396 V.E...... R....... 0,0-1080,2400 aid=0}[] softInputMode=STATE_UNSPECIFIED|ADJUST_PAN

fix this error when im clikng the accept buttonn in the worker profile view screen it says this error
but the same accept and rejecct button are working perfect in the job applications screen 