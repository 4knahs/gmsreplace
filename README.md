gmsreplace
==========
Since there are a considerable number of devices with no Google Play Market and Google Play Services, this project presents a workaround for enabling Google Play Service dependent applications. 

This project consists of a prototype xposed module to replace Google Play Services by the correspondent web apis. 

Currently supports google drive APIs present in both the Google Android Drive API demo applications:

* Android-quickstart - uses the camera to take a picture and uploads the picture on selected Drive folder.
* Android-quickeditor - a text editor that allows you to create, open and modify text files in your Drive.

## Droidnesis integration
This project also provides an API usage example for Droidnesis. 
Droidnesis is a framework for patching and remote code execution.
It is able to perform patch distribution as well as providing tools for code offloading.

While the DriveReplace.java and the DriveUtils.java represent a fully compliant Xposed module, the project also includes the DroidLoader library.
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
