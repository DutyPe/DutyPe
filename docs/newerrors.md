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


2026-04-25 22:19:53.864 18039-24522 Google Android Maps SDK com.dutype.app                       E  Authorization failure.  Please see ****************************************************************** for how to correctly set up the map.
2026-04-25 22:19:53.867 18039-24522 Google Android Maps SDK com.dutype.app                       E  In the Google Developer Console (**************************************
                                                                                                    Ensure that the "Maps SDK for Android" is enabled.
                                                                                                    Ensure that the following Android Key exists:
                                                                                                    	API Key: AIzaSyC5Wx2Utzd7VnJ3WX46LxFn7l_33_rPYEo
                                                                                                    	Android Application (<cert_fingerprint>;<package_name>): ***********************:***********************:AC:16:D3:BA;com.dutype.app
2026-04-25 22:19:55.166 18039-24465 DynamiteModule          com.dutype.app                       W  Local module descriptor class for com.google.android.gms.googlecertificates not found.
2026-04-25 22:19:55.171 18039-24465

for the above error wjat to do and
give me the sha key and steps to add in the google cloud 
2. application relaed ,hiring ,update on review, apllcaition eerecived and more notifcations are coming twice 
except the profiel completetion
3. another thing is that , if the worker applied for the job then the applied for the notifcaion coming twice and employer related notifcation also coming like new application reciveed on worker side itself...
4.Accept,rejecct  button not working in worker profile screenlike the appplcaition screen of the employerside and  where the employer can view the worker profiel from the applcaition screen to coming this screen
and in this screen, only the anme, skills, phone number are showing , not showing the 
field
bio



educationQualification

  dob: convet this to show hoe many years old


experience
 


gender
4.If employer hired count(accepted applocation) equals to vacancy then,hide the from the worker screens,
If the worker applied for the job ,if wasnt the hired for that job then show  job was filled the vacancy..on click that card dont navaigate to jobdescription screeen,keep worker applied history as its like applied for the job  like that
5.add the fearur that employer can only add the single image for the job ppsting --now update relatd this every wher ein the jobdesciption, job preview and collection also.. employer cant add more  thatn one tjhts it
and the uplaod image should show perfectly in the jobdscriptin-have a look,its already working fine
 

6some notifcations still has these symbols d ð/ðŸ-remove it