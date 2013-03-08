ohmageAndroidLib
================

ohmage (http://ohmage.org) is an open-source, mobile to web platform that records, 
analyzes, and visualizes data from both prompted experience samples entered by the 
user, as well as continuous streams of data passively collected from sensors or 
applications onboard the mobile device. 

This is the library project which is required by any app which would like to use ohmage. The idea behind this library is that you can easily replace files in a custom project forked from [ohmageApp](https://github.com/ohmage/ohmageApp) without having to update any files in this library. Any resources in your res directory will override resources in the library (such as the server url). You can also override applications in the android manifest to launch custom activities instead of the ones provided by this library. etc. Take a look at the projects to see examples of how to override the resources of this library project.

Projects
--------

These are the projects which currently use the ohmageAndroidLib

* [ohmageApp](https://github.com/ohmage/ohmageApp) - The basic wrapper around the library project.
Fork this project if you which to make your own changes.
* [MobilizeApp](https://github.com/ohmage/MobilizeApp) - The mobilize version of the app.

Dependencies
------------

You will need to download [LogProbe](https://github.com/cens/LogProbe) and make it available to
ohmage as a library apk. (Alternatively you could just change all logging functionality to use
the default android logs instead of using ProbeLog)

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
