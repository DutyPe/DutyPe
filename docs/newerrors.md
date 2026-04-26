
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


2026-04-26 16:02:44.761  7992-11997 ProxyAndro...gerBackend com.dutype.app                       W  Too many Flogger logs received before configuration. Dropping old logs.
2026-04-26 16:02:45.176  7992-12003 Google Android Maps SDK com.dutype.app                       E  Authorization failure.  Please see ****************************************************************** for how to correctly set up the map.
2026-04-26 16:02:45.178  7992-12003 Google Android Maps SDK com.dutype.app                       E  In the Google Developer Console (**************************************
                                                                                                    Ensure that the "Maps SDK for Android" is enabled.
                                                                                                    Ensure that the following Android Key exists:
                                                                                                    	API Key: AIzaSyC5Wx2Utzd7VnJ3WX46LxFn7l_33_rPYEo
                                                                                                    	Android Application (<cert_fingerprint>;<package_name>): ***********************:***********************:AC:16:D3:BA;com.dutype.app


2026-04-25 22:19:53.864 18039-24522 Google Android Maps SDK com.dutype.app                       E  Authorization failure.  Please see ****************************************************************** for how to correctly set up the map.
2026-04-25 22:19:53.867 18039-24522 Google Android Maps SDK com.dutype.app                       E  In the Google Developer Console (**************************************
                                                                                                    Ensure that the "Maps SDK for Android" is enabled.
                                                                                                    Ensure that the following Android Key exists:
																										API Key: <GOOGLE_MAPS_API_KEY>
                                                                                                    	Android Application (<cert_fingerprint>;<package_name>): 


1.this is nice working aweomely fine ,now the employer can seeing the workerprpifle data but the small issue is there, dont show the application status  text and applied for text ,remove it from the worker profielveiw screen ,
and dont show the total jobs done and location 

and show the data in cleaner manner 

like phonenumer : after that have more space and then every rhing showing but we need the celaner way like the job description how it showing ,here you dont  need to  use he icon thtast fine but show like Gender : Male but not Gender:      Male

keep the applied on time at top but show on top right in the personal infrormation 
2.after rating the worker the rate worker button still sohowing , on clik it saysing you are alredy reated,becaue i rated thats corect but after rating show as the reated warke , show same after the worker rates the employer the message shwoing on the applied card under myjioibsscren exact way show here 
 but keep the rating feature in the worker profiel detail screen by the employer ,in that screen show instead of the applicationsscreen ,
2.2 un fortunately i saw that i think theres no bottom rate employer feautre on the applied jobscreen ,applied job card have a look and add the there
3. dont shoow the job dessctritpion in the employer job card homescreen, i said to fix show the shift is showing as any ,its not afctual value showing
4.employer jobcard is good but remove the elevation of thecard and add the border for the card verylight way
5.gitlegacy roles only as a read fallback so old users don’t break.--dont keep this ,there no one in the database to face the issue ,you can remove it confidenetally from the collecctons,usags and everyhere ,fnctions,etc..