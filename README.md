ohmageAndroidLib
================

ohmage (http://ohmage.org) is an open-source, mobile to web platform that records, 
analyzes, and visualizes data from both prompted experience samples entered by the 
user, as well as continuous streams of data passively collected from sensors or 
applications onboard the mobile device. 

Due to the requirement that each study will usually require slight modifications to the application, we made this library project. It makes it easy to create new apks with different package names which can be deployed independently to the play store if needed. The library project allows us to easily make slight modifications for specific deployments while still keeping a consistent code base.

This library project allows files or resources to be replaced by the apk which uses it. A simple example project which uses this library can be forked from here [ohmageApp](https://github.com/ohmage/ohmageApp). Any resources with the same name that you define in your project will override the same resources in this library. This allows for simple configuration for different deployments.

Projects
--------

These are the projects which currently use the ohmageAndroidLib

* [ohmageApp](https://github.com/ohmage/ohmageApp) - The basic wrapper around the library project.
Fork this project if you want to make .
* [MobilizeApp](https://github.com/ohmage/MobilizeApp) - The mobilize version of the app.

Dependencies
------------

You will need to download [LogProbe](https://github.com/cens/LogProbe) and make it available to
ohmage as a library apk. (Alternatively you could just change all logging functionality to use
the default android logs instead of using ProbeLog)

You will also need the [Google Play Services](http://developer.android.com/google/play-services/index.html) library apk setup on your system. Information about installing it can be found here: [http://developer.android.com/google/play-services/setup.html](http://developer.android.com/google/play-services/setup.html). In addition, you will need a Google Maps Android API key which you will reference in your manifest. Information on getting a key can be found here: [https://developers.google.com/maps/documentation/android/start](https://developers.google.com/maps/documentation/android/start).


All other external libraries which are needed are included in the libs directory of the project,
but of course you will need the android SDK which can be found here:
http://developer.android.com/sdk/installing.html.

Testing
-------

We are using a combination of [robotium](http://code.google.com/p/robotium/) and
[calabash-android](https://github.com/calabash/calabash-android) (which is basically an android
implementation of [cucumber](https://github.com/cucumber/cucumber)). Robotium tests are in the test folder
and can be run as unit tests. The cucumber tests requires calabash-android to be installed. At this point
this [fork](https://github.com/cketcham/calabash-android) must be used to do the testing as it includes
additional functionality not available in the main branch. Clone the fork, change into the `ruby-gem`
directory and run `rake install` (you might need `sudo rake install` depending on how your gems are
installed.) Then you can run `calabash-android build` to build the testing apk, and finally
`calabash-android run` to start the tests.
