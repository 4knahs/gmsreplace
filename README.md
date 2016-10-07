gmsreplace
==========
Since there are a considerable number of devices with no Google Play Market and Google Play Services, this project presents a workaround for enabling Google Play Service dependent applications. 

This project consists of a fully compliant Xposed module to replace Google Play Services by the correspondent web apis. 

Currently supports Google Auth and Drive APIs (as well as some common and internal apis) present in both the Google Android Drive API demo applications:

* [Android-quickstart](https://github.com/googledrive/android-quickstart) - uses the camera to take a picture and uploads the picture on selected Drive folder.
* [Android-quickeditor](https://github.com/4knahs/android-quickeditor) - a text editor that allows you to create, open and modify text files in your Drive.
 
There are mechanisms that are very similar in other APIs so it can serve as an example for future implementations. Furthermore if installed with Droidnesis, it allows distributed usage of Google Drive (i.e. automatically place new Drive files in your friends Google Drive).

## INFv/Droidnesis integration

This project also provides an API usage example for INFv (an extension to the former Droidnesis). 
Droidnesis is a framework for patching and remote code execution.
It is able to perform patch distribution as well as providing tools for code offloading.

While the DriveReplace.java and the DriveUtils.java represent a fully compliant Xposed module, the project also includes the [DroidLoader library](https://github.com/4knahs/DroidLoader).
This library provides the Droidnesis APIs.

Projects to be imported into Droidnesis must include a com.registry.Registry class that extends the abstract DroidLoader class.
This class provides collections where you can register xposed modules and/or specific classes/methods to hook, as well as a collection to register classes to be offloaded.

For setting which applications are hooked/offloaded, the packagesToHook List is used:

```java
    /**
     * This ArrayList holds the package names of the applications to hook
     */
    static {
        packagesToHook.add("com.aknahs.gms.quickstart");
        packagesToHook.add("com.aknahs.gms.quickeditor");
    }
```

In this example, both the quickstart and quickeditor applications are supported.
To execute the Xposed handleLoadPackage when one of the previous applications is loaded, there is the xposedModuleToLoad List:

```java
    /**
     * Holds a xposed module which handleLoadPackage will be invoked.
     */
     static { xposedModuleToLoad.add(DriveReplace.class); }
```

We setted our Xposed Module main class where the handleLoadPackage is present.
Finally, because we wanted to add some more functionality, we added a few classes for offloading:

```java
   /**
     * Holds the fully qualified classnames to be offloaded.
     */
    static {
        classesToOffload.add("com.google.android.gms.common.api.GoogleApiClient.Builder");
        classesToOffload.add("com.google.android.gms.common.api.GoogleApiClient");
        classesToOffload.add("com.google.android.gms.common.api.b");
        
        //a few more classes here. Check Registry.java
    }
```
Droidnesis will check with which classes these classes interact and perform the necessary work for it to offload.
When Droidnesis is disabled, the application will work normally. If it is enabled but with no connection, only the patching will be active.
If it is both enabled and with connection, the specified classes will be offloaded automatically.

### Notes
Note how there were only minimal changes to the original Xposed module in order to add Droidnesis support.
In DriveReplace there are only 2 method calls for supporting UI dialogs in backend.

The DroidLoader is composed by 39 lines of code.

* 2 lines for specifying applications supported.

* 1 line to specify the xposed module class.

* 26 lines to specify which classes are supposed to be offloaded.

Remaining lines belong to the DroidLoader class definition.

## Building and installing

### Xposed

If to be used in Xposed, add the typical assets/xposed_init:
```
test.aknahs.com.droiddrivereplace.DriveReplace
```
And the manifest metadata:
```xml
        <meta-data
            android:name="xposedmodule"
            android:value="true" />
        <meta-data
            android:name="xposedminversion"
            android:value="2.0*" />
        <meta-data
            android:name="xposeddescription"
            android:value="DriveReplace demo" />
```
Build and install the application on the device. Remember to enable the module in Xposed.

### Droidnesis

If it is to be loaded in Droidnesis, in Android Studio, open “Run > Edit Configurations…”. Press the plus sign on the upper left and pick “Gradle”. On the “Gradle project” field select the recently created project “build.gradle” present on the project root. In the “Tasks” field write “assemble”, click ok and just run your project (Shift + F10).

You should now have an apk file in <project_root_folder>/app/build/outputs/apk. For debugging purposes, you can push this file in the phone:
```shell
$ adb push <project_root_folder>/app/build/outputs/apk/debug_apk.apk\
    data/local/tmp/dexjars/apk/drive-replace.apk
```

Using [DroidBroker](https://bitbucket.org/aknahs/droidbroker), generate the json representation of the application and place it on the phone as well:
```shell
$ cp <path_to_application>.apk apks/
$ java -jar DroidBroker.java -P -g
$ adb push results/<application>.json data/local/tmp/apkjsons/<application>.json
```

Enable Droidnesis and you are good to go!
