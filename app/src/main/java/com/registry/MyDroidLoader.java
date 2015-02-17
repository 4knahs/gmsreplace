package com.registry;

import com.aknahs.droidloader.DroidLoader;

import test.aknahs.com.droiddrivereplace.DriveReplace;

/**
 * Created by aknahs on 23/01/15.
 */
public class MyDroidLoader extends DroidLoader{

    /*------------------------------------------------------
    * This class should be present in patches to be loaded
    * in Droidnesis.
    * ------------------------------------------------------*/

    /**
     * This ArrayList holds the package names of the applications to hook
     */
    static {
        packagesToHook.add("com.aknahs.gms.quickstart");
        packagesToHook.add("com.aknahs.gms.quickeditor");
    }

    /*
     * Holds a mapping between the original class name and the class where methods to be injected are implemented.
     */
    //static {}

    /**
     * Holds a xposed module which handleLoadPackage will be invoked.
     */
     static {
        xposedModuleToLoad.add(DriveReplace.class);
    }

    /**
     * Holds the fully qualified classnames to be offloaded.
     */
    static {
        classesToOffload.add("com.google.android.gms.common.api.GoogleApiClient.Builder");
        classesToOffload.add("com.google.android.gms.common.api.GoogleApiClient");
        classesToOffload.add("com.google.android.gms.common.api.b");
        classesToOffload.add("com.google.android.gms.drive.internal.o");
        classesToOffload.add("com.google.android.gms.drive.internal.o$3");
        classesToOffload.add("com.google.android.gms.drive.internal.o$c");
        classesToOffload.add("com.google.android.gms.common.api.Status");
        classesToOffload.add("com.google.android.gms.drive.internal.r");
        classesToOffload.add("com.google.android.gms.drive.internal.s");
        classesToOffload.add("com.google.android.gms.drive.MetadataChangeSet");
        classesToOffload.add("com.google.android.gms.drive.MetadataChangeSet.Builder");
        classesToOffload.add("com.google.android.gms.drive.internal.w$1");
        classesToOffload.add("com.google.android.gms.drive.internal.o$4");
        classesToOffload.add("com.google.android.gms.drive.internal.w");
        classesToOffload.add("com.google.android.gms.drive.internal.w$c");
        classesToOffload.add("com.google.android.gms.drive.internal.l");
        classesToOffload.add("com.google.android.gms.drive.Metadata");
        classesToOffload.add("com.google.android.gms.common.GooglePlayServicesUtil");
        classesToOffload.add("com.google.android.gms.common.ConnectionResult");
        classesToOffload.add("com.google.android.gms.common.api.BaseImplementation$AbstractPendingResult");
        classesToOffload.add("com.google.android.gms.drive.internal.s$2");
        classesToOffload.add("com.google.android.gms.common.api.BaseImplementation$AbstractPendingResult");
        classesToOffload.add("com.google.android.gms.drive.CreateFileActivityBuilder");
        classesToOffload.add("com.google.android.gms.drive.OpenFileActivityBuilder");
        classesToOffload.add("com.google.android.gms.drive.DriveId");
        classesToOffload.add("com.google.android.gms.drive.Contents");
        classesToOffload.add("com.google.android.gms.common.ConnectionResult");
    }
}
