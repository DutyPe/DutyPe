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

2. employers saved adress are cards are showing not good,default text showing vertically , making the card larefier in geight and defauly text showiung vertical
2.1 only updated locations only show in the job posting screen and dont take the location from the job posting screen ,dont save here
2.2 employer wants to add the addresses, he can comeadd here only... fromm job psoting he can just use the location from this screen which are saved ones, other wise hecan give new one , but dont save that new location here, 
2.3 remove the verified badge from the employer profielscreen 
2.4 employer profile detailscreen not showing the eployer provided email
3.worker can give the reveiw,rating to employer and employer can give the rating,reveiw to  the worker--worker already giving the rating after he hired for the job -

but employer not giving the hired workers rating - show  the rating for employer to give to the workeer after 1 day 

reeiw rating are should be who store with the names, 
suppose employer vamsi  given rating to the worker then worker see the rating reveiw in his  profieldetail scren with actual employer name wiith company gave him this rating ,same way worker given rating employer can esee withh name

in feature we need to recomand the worker to the employers based on the his works with rating and review so we need to keep this way in ligjt wight way