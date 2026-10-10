# תיקוני הסקירה ובדיקות רגרסיה

ב־Android Studio הרץ את AllFeaturesTestSuite שב־androidApp/src/androidTest/kotlin/org/roy/ecommerce_project/testing. היא כוללת את כל בדיקות המסכים, ViewModels, Repository וחוזה התשלום. בדיקות השרת רצות באמצעות `npm --prefix functions test`. בדיקות iOS נמצאות ב־PhotoPickerLifecycleTest ודורשות Xcode ו־iOS Simulator SDK.

| ממצא | תיקון וכיסוי |
|---|---|
| 1 | capture ואימות בשרת; payment.test.mjs ו־PaypalApiIntegrationTest |
| 2 | הסרת סודות מהאפליקציה; compiledConstantsContainNoMerchantCredentialFields ובדיקת Firebase Bearer |
| 3 | parser בטוח; RegressionUiStateTest |
| 4 | קבוצות מוצרים במקביל; RegressionDataTest, קבוצות 0/10/11/21 |
| 5 | החלפת snapshots; RegressionDataTest, עדכון/מחיקה/כפילויות |
| 6 | טרנזקציות בעגלה; RegressionDataTest, פעולות מקבילות |
| 7 | צילום checkout קבוע; PaymentViewModelIntegrationTest ו־payment.test.mjs |
| 8 | חסימת לחיצה חוזרת, מזהה קבוע ו־idempotency; PaymentViewModelIntegrationTest, RegressionDataTest והשרת |
| 9 | שמירה וניקוי אטומיים; RegressionDataTest ובדיקת כשל commit בשרת |
| 10 | יצירת לקוח ותפקיד אטומית ותיקון תפקיד חסר; RegressionDataTest |
| 11 | loading עד callback; authentication_keepsGoogleButtonLoadingUntilCustomerCreationCompletes |
| 12 | הכנת session בכל ניסיון; temporaryPaypalPreparationFailureRecovers |
| 13 | חישוב באגורות; RegressionDataTest, canonicalMoneyPrecision והשרת |
| 14 | שדה State נפרד; PaymentViewModelIntegrationTest והשרת |
| 15 | postalCodeText שומר אפסים עם תאימות לנתונים ישנים; postalCodeLeadingZeroSurvivesSaveAndShipping |
| 16 | צילום כתובת ושמירת פרטים ששונו; paypalPersistsEditedAddressBeforeOpeningBrowser והשרת |
| 17 | מצב טעינה/שגיאה וחסימת שמירה; RegressionUiStateTest |
| 18 | מוצר חסר נשאר ניתן להסרה; ViewModelIntegrationTest ו־deletedCatalogueProductCanBeRemovedFromCart |
| 19 | חיפוש ללא רגישות לאותיות גדולות; adminSearchMatchesUppercaseAndMixedCaseAgainstStoredLowercaseTitle |
| 20 | delegate חזק; PhotoPickerLifecycleTest, UIKit delegate לאחר GC |
| 21 | חלון key בסצנה פעילה ואיפוס/retry; PhotoPickerLifecycleTest |

בדיקות Repository מריצות את קוד הייצור מעל ShopStore אטומי עם Mutex ו־rollback. בדיקות השרת מריצות את Firestore adapter מעל מסמכים מבודדים. אין Firebase או PayPal אמיתיים בבדיקות.

קוד השרת מחייב הפעלה ידנית, הגדרת סודות והתאמת הרשאות; ראה functions/README.md. בדיקות iOS דורשות Xcode ואינן מאומתות באמצעות הרצת Android.

## תוצאות האימות

- 276 בדיקות Android עברו במכשיר OnePlus IN2013 (Android 13), ללא כישלונות או דילוגים.
- 37 בדיקות שרת עברו, וכן בדיקת תחביר וטעינת מודול Firebase Functions.
- בניית Android ו־lint עברו; נותרו שלוש אזהרות קיימות.
- קוד בוחר התמונות של iOS וחמש בדיקותיו קומפלו. הרצת סימולטור נכשלה בגלל סביבת Swift/Xcode המקומית והרשאות cache, ולכן הבדיקות לא הורצו.
