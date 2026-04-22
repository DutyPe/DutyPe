
ï
`
BaselineProfileGeneratorcom.dutype.app.baselineprofilegenerate2Ü“üœÄÄíÙ:õ÷üœÄÜî∆"‘

logcatandroidæ
ªC:\Users\91961\DutyPe\baselineprofile\build\outputs\androidTest-results\connected\nonMinifiedRelease\V2321 - 15\logcat-com.dutype.app.baselineprofile.BaselineProfileGenerator-generate.txt"õ

device-infoandroidÄ
~C:\Users\91961\DutyPe\baselineprofile\build\outputs\androidTest-results\connected\nonMinifiedRelease\V2321 - 15\device-info.pb"õ

device-info.meminfoandroidy
wC:\Users\91961\DutyPe\baselineprofile\build\outputs\androidTest-results\connected\nonMinifiedRelease\V2321 - 15\meminfo"õ

device-info.cpuinfoandroidy
wC:\Users\91961\DutyPe\baselineprofile\build\outputs\androidTest-results\connected\nonMinifiedRelease\V2321 - 15\cpuinfo"Ÿ@÷@
⁄1
com.google.testing.platform“PLUGIN_ERROR"TEST*öException thrown during onAfterAll invocation of plugin AndroidTestApkInstallerPlugin: device 'adb-10BE250M94000HZ-1PnkfQ._adb-tls-connect._tcp' not found:Ñ0com.google.testing.platform.api.plugin.PluginException: ErrorName: PLUGIN_ERROR
NameSpace: com.google.testing.platform
ErrorCode: 2002
ErrorType: TEST
Message: Exception thrown during onAfterAll invocation of plugin AndroidTestApkInstallerPlugin: device 'adb-10BE250M94000HZ-1PnkfQ._adb-tls-connect._tcp' not found
	at com.google.testing.platform.plugin.PluginLifecycleKt.invokeOrThrow(PluginLifecycle.kt:547)
	at com.google.testing.platform.plugin.PluginLifecycleKt.invokeOrThrow$default(PluginLifecycle.kt:517)
	at com.google.testing.platform.plugin.PluginLifecycle$onAfterAll$1$1$1.invoke(PluginLifecycle.kt:361)
	at com.google.testing.platform.plugin.PluginLifecycle$onAfterAll$1$1$1.invoke(PluginLifecycle.kt:358)
	at com.google.testing.platform.lib.cancellation.ProcessCancellationContext.runWithTimeoutDuringCancellation(ProcessCancellationContext.kt:153)
	at com.google.testing.platform.plugin.PluginLifecycle$onAfterAll$1$1.invoke(PluginLifecycle.kt:358)
	at com.google.testing.platform.plugin.PluginLifecycle$onAfterAll$1$1.invoke(PluginLifecycle.kt:341)
	at com.google.testing.platform.core.telemetry.common.noop.NoopDiagnosticsScope.recordEvent(NoopDiagnosticsScope.kt:35)
	at com.google.testing.platform.core.telemetry.SequentialEventRecordRequest.record$java_com_google_testing_platform_core_telemetry_telemetry_api(EventRecordRequest.kt:71)
	at com.google.testing.platform.core.telemetry.DiagnosticsExtKt.record(DiagnosticsExt.kt:27)
	at com.google.testing.platform.core.telemetry.TelemetryKt.createEvent(Telemetry.kt:60)
	at com.google.testing.platform.plugin.PluginLifecycle.onAfterAll(PluginLifecycle.kt:339)
	at com.google.testing.platform.executor.SingleDeviceExecutor.onAfterAll(SingleDeviceExecutor.kt:159)
	at com.google.testing.platform.executor.SingleDeviceExecutor.access$onAfterAll(SingleDeviceExecutor.kt:52)
	at com.google.testing.platform.executor.SingleDeviceExecutor$execute$1.invoke(SingleDeviceExecutor.kt:121)
	at com.google.testing.platform.executor.SingleDeviceExecutor$execute$1.invoke(SingleDeviceExecutor.kt:120)
	at com.google.testing.platform.result.TestResultListenerManager.afterTestSuite(TestResultListenerManager.kt:140)
	at com.google.testing.platform.executor.SingleDeviceExecutor.execute(SingleDeviceExecutor.kt:147)
	at com.google.testing.platform.RunnerImpl.run(RunnerImpl.kt:121)
	at com.google.testing.platform.server.strategy.NonInteractiveServerStrategy$run$4$2.invoke(NonInteractiveServerStrategy.kt:98)
	at com.google.testing.platform.server.strategy.NonInteractiveServerStrategy$run$4$2.invoke(NonInteractiveServerStrategy.kt:98)
	at com.google.testing.platform.core.telemetry.common.noop.NoopDiagnosticsScope.recordEvent(NoopDiagnosticsScope.kt:35)
	at com.google.testing.platform.core.telemetry.SequentialEventRecordRequest.record$java_com_google_testing_platform_core_telemetry_telemetry_api(EventRecordRequest.kt:71)
	at com.google.testing.platform.core.telemetry.DiagnosticsExtKt.record(DiagnosticsExt.kt:27)
	at com.google.testing.platform.core.telemetry.TelemetryKt.createEvent(Telemetry.kt:60)
	at com.google.testing.platform.server.strategy.NonInteractiveServerStrategy.run(NonInteractiveServerStrategy.kt:95)
	at com.google.testing.platform.main.MainKt$main$4.invokeSuspend(Main.kt:75)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
	at kotlinx.coroutines.EventLoopImplBase.processNextEvent(EventLoop.common.kt:284)
	at kotlinx.coroutines.BlockingCoroutine.joinBlocking(Builders.kt:85)
	at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking(Builders.kt:59)
	at kotlinx.coroutines.BuildersKt.runBlocking(Unknown Source)
	at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking$default(Builders.kt:38)
	at kotlinx.coroutines.BuildersKt.runBlocking$default(Unknown Source)
	at com.google.testing.platform.main.MainKt.main(Main.kt:73)
	at com.google.testing.platform.main.MainKt.main$default(Main.kt:35)
	at com.google.testing.platform.main.MainKt.main(Main.kt)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Unknown Source)
	at java.base/java.lang.reflect.Method.invoke(Unknown Source)
	at com.google.testing.platform.launcher.Launcher.main(Launcher.java:154)
Caused by: com.android.ddmlib.AdbCommandRejectedException: device 'adb-10BE250M94000HZ-1PnkfQ._adb-tls-connect._tcp' not found
	at com.android.ddmlib.AdbHelper.setDevice(AdbHelper.java:981)
	at com.android.ddmlib.AdbHelper.setDevice(AdbHelper.java:1000)
	at com.android.ddmlib.internal.DeviceImpl.lambda$executeRemoteCommand$18(DeviceImpl.java:785)
	at com.android.ddmlib.internal.DeviceImpl.logRun1(DeviceImpl.java:1801)
	at com.android.ddmlib.internal.DeviceImpl.executeRemoteCommand(DeviceImpl.java:755)
	at com.android.ddmlib.internal.DeviceImpl.lambda$executeRemoteCommand$15(DeviceImpl.java:618)
	at com.android.ddmlib.internal.DeviceImpl.logRun1(DeviceImpl.java:1801)
	at com.android.ddmlib.internal.DeviceImpl.executeRemoteCommand(DeviceImpl.java:615)
	at com.android.ddmlib.internal.DeviceImpl.lambda$executeShellCommand$14(DeviceImpl.java:573)
	at com.android.ddmlib.internal.DeviceImpl.logRun1(DeviceImpl.java:1801)
	at com.android.ddmlib.internal.DeviceImpl.executeShellCommand(DeviceImpl.java:570)
	at com.android.tools.utp.plugins.deviceprovider.ddmlib.DdmlibAndroidDevice.executeShellCommand(DdmlibAndroidDevice.kt)
	at com.android.tools.utp.plugins.deviceprovider.ddmlib.DdmlibAndroidDeviceController$executeAsync$deferred$1.invokeSuspend(DdmlibAndroidDeviceController.kt:171)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
	at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:570)
	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.executeTask(CoroutineScheduler.kt:749)
	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:677)
	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:664)
ˆ
Û*Cdevice 'adb-10BE250M94000HZ-1PnkfQ._adb-tls-connect._tcp' not found:´com.android.ddmlib.AdbCommandRejectedException: device 'adb-10BE250M94000HZ-1PnkfQ._adb-tls-connect._tcp' not found
	at com.android.ddmlib.AdbHelper.setDevice(AdbHelper.java:981)
	at com.android.ddmlib.AdbHelper.setDevice(AdbHelper.java:1000)
	at com.android.ddmlib.internal.DeviceImpl.lambda$executeRemoteCommand$18(DeviceImpl.java:785)
	at com.android.ddmlib.internal.DeviceImpl.logRun1(DeviceImpl.java:1801)
	at com.android.ddmlib.internal.DeviceImpl.executeRemoteCommand(DeviceImpl.java:755)
	at com.android.ddmlib.internal.DeviceImpl.lambda$executeRemoteCommand$15(DeviceImpl.java:618)
	at com.android.ddmlib.internal.DeviceImpl.logRun1(DeviceImpl.java:1801)
	at com.android.ddmlib.internal.DeviceImpl.executeRemoteCommand(DeviceImpl.java:615)
	at com.android.ddmlib.internal.DeviceImpl.lambda$executeShellCommand$14(DeviceImpl.java:573)
	at com.android.ddmlib.internal.DeviceImpl.logRun1(DeviceImpl.java:1801)
	at com.android.ddmlib.internal.DeviceImpl.executeShellCommand(DeviceImpl.java:570)
	at com.android.tools.utp.plugins.deviceprovider.ddmlib.DdmlibAndroidDevice.executeShellCommand(DdmlibAndroidDevice.kt)
	at com.android.tools.utp.plugins.deviceprovider.ddmlib.DdmlibAndroidDeviceController$executeAsync$deferred$1.invokeSuspend(DdmlibAndroidDeviceController.kt:171)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
	at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:570)
	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.executeTask(CoroutineScheduler.kt:749)
	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:677)
	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:664)
*Å
c
test-results.logOcom.google.testing.platform.runtime.android.driver.AndroidInstrumentationDriverã
àC:\Users\91961\DutyPe\baselineprofile\build\outputs\androidTest-results\connected\nonMinifiedRelease\V2321 - 15\testlog\test-results.log 2
text/plain2™
QOcom.google.testing.platform.runtime.android.driver.AndroidInstrumentationDriver"INSTRUMENTATION_FAILED*9Test run failed to complete. Expected 1 tests, received 0