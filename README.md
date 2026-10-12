# ShelfSense

ShelfSense is an Android app that helps households waste less food. It keeps track of what's in the fridge, pantry and freezer, works out when each item should be used from both the printed date and any after-opening instruction, and sends a reminder before it's too late. When food is eaten, donated or thrown out, the app records it and shows the results in a simple report.

Built for ITO5046 at Monash University by Nicholas Battle.

## Built with

Kotlin and Jetpack Compose, Room, Retrofit with the Open Food Facts API, WorkManager, DataStore, Firebase Authentication, Cloud Firestore and the Google code scanner.

## Running the app

1. Open the project in Android Studio and let Gradle sync.
2. Check that `app/google-services.json` is present. It's included in the submission but not in the public repository.
3. Run the app on an emulator with a Google Play system image, which the barcode scanner needs.
4. Sign up with an email address you can verify, or use the demo account supplied with the submission.

Debug builds have a Load sample data button on the Profile tab, which adds example items and six months of history.

## Screens

| Screen | Purpose | Key components |
| --- | --- | --- |
| Login | Signs in a returning user | Firebase Authentication, TextField, validation |
| Sign up | Creates an account and records name and household size | Firebase Authentication, ExposedDropdownMenu, validation |
| Verify email | Confirms the email address before the app opens | Firebase Authentication |
| Forgot password | Sends a password reset link | Firebase Authentication |
| Home | Storage counts, the items due soonest and a monthly summary | Navigation bar, LazyColumn, Room |
| Pantry | Browses, searches and filters active items | LazyColumn, SearchBar, FilterChip, FAB |
| Add food | Offers the three ways to add an item | Navigation Compose |
| Scan barcode | Reads a barcode with the camera | Google code scanner, Retrofit |
| Enter barcode | Accepts a typed barcode when the camera can't read it | TextField, check digit validation, Retrofit |
| Food form | Adds or edits an item, including dates, opening details and a photo | DatePicker, ExposedDropdownMenu, Retrofit |
| Food details | Dates, the action date explanation, storage tips and outcomes | ModalBottomSheet, AlertDialog, Snackbar, Room |
| Insights | Reports outcomes over a chosen period | Compose Canvas charts, Room aggregation |
| Profile | Account, reminders, notifications, backup, theme and sign out | DataStore, WorkManager, runtime permissions |

## Functionalities

| Functionality | Description | Where |
| --- | --- | --- |
| Register and authenticate | Account creation, sign in, password reset and sign out | Login, Sign up, Forgot password, Profile |
| Capture user details | Records the display name and household size at sign up | Sign up |
| Summarise by storage location | Counts the items in the fridge, pantry and freezer | Home |
| Look up product by barcode | Retrieves the name, brand and image, plus the category when the product's tags match | Food form |
| Scan a barcode | Reads the number from the package and looks the product up | Scan barcode |
| Enter a barcode | Accepts the number by hand and looks the product up | Enter barcode |
| Manual food entry | Records fresh produce or unlisted products directly | Food form |
| Record storage location | Fridge, freezer or pantry | Food form |
| Record date type | Use by or best before, plus no date on the pack and doesn't expire | Food form |
| Record opening details | Opened date and the after-opening period from the package | Food form, Food details |
| Calculate action date | Earlier of the printed date and the opening deadline | Domain logic |
| Explain prioritisation | States which date drove the result | Food details |
| Display storage guidance | General advice by food category | Food details |
| Browse and search pantry | Sorted, searchable and filterable list of active items | Pantry |
| Record outcome | Marks an item consumed, donated or discarded | Food details |
| Generate report | Outcome counts, avoidance rate and period comparison | Insights |
| Send expiry reminders | Grouped notification from a daily background check | Background, Profile |
| Synchronise to cloud | Uploads pending changes and pulls the pantry back on sign in | Background |

## Android components

| Component | Use in ShelfSense |
| --- | --- |
| Jetpack Compose | All user interface screens |
| Navigation bar | Home, Pantry, Insights and Profile destinations |
| Room | Single pantry table holding active and resolved items |
| LazyColumn | Pantry list and the Home priority list |
| DatePicker | Printed date and opened date |
| ExposedDropdownMenu | Category, storage location, date type, household size, reminder lead time, theme and report period |
| Retrofit | Open Food Facts product requests |
| Google code scanner | Reads barcodes without the app handling the camera permission |
| ViewModel with StateFlow | Screen state exposed to Compose |
| Navigation Compose | Destination graph and notification deep links |
| WorkManager and CoroutineWorker | Reminder and sync jobs |
| NotificationCompat | Grouped reminder notifications |
| Firebase Authentication | Accounts, email verification and password changes |
| Cloud Firestore | Per-user cloud copy of the pantry |
| Runtime permissions | Notification permission request and refusal handling |
| DataStore | Reminder preferences, theme and profile details |
| SearchBar, FilterChip, AlertDialog, Snackbar, ModalBottomSheet, FAB | Search, filtering, confirmation, undo and detail views |
| Coil | Product images and item photos |
| Camera and photo picker | Item photos, stored only on the phone |
