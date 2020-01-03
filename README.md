# EucWorldAndroid
EUC World for Android

Originally this project aimed to tailor WheelLog for Android to be better suited for long-range EUC rides. Currently, this 
application has become a separate application, independent of its predecessor. The most important changes include:

1. Vastly improved user interface.
2. Available in multiple languages.
3. Voice messages to keep track on your ride parameters, battery status etc. without constantly staring into your smartphone or smartwatch (if you have one).
4. Flic button integration for easy activation of most used functions (horn, light etc.).
5. Improved and extended alarms.
6. Tour tracking with complete telemetry via https://euc.world online service.
7. Compatible with most recent Android OS versions.
8. Support for Wear OS companion application.

Based on:
https://github.com/slastowski/WheelLogAndroid

Pebble app code:
https://github.com/JumpMaster/WheelLogPebble

Flic library:
https://github.com/50ButtonsEach/fliclib-android

This project contains some works created using Font Awesome Free icons: https://fontawesome.com/license/free

In order to compile this project, after cloning you will need to initialise the 'fliclib' submodule with:
'git submodule init'

Then update with:
'git submodule update'

For more details regarding git submoduels refer to: https://git-scm.com/book/en/v2/Git-Tools-Submodules

Once this code has been imported you will need to convert 'fliclib' to AndroidX. In Android Studio this can be done by clicking 'Refactor' from the menu bar and selecting 'Migrate to AndroidX...'
