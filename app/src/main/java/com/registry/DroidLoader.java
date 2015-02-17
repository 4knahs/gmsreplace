package com.registry;

import java.util.ArrayList;

import test.aknahs.com.droiddrivereplace.DriveReplace;

/**
 * Created by aknahs on 23/01/15.
 */
public class DroidLoader {

    /*------------------------------------------------------
    * This class should be present in patches to be loaded
    * in Droidnesis.
    * ------------------------------------------------------*/

    /**
     * There are 3 alternatives for loading a module:
     *  -> _dex notation : deprecated
     *  -> xposedModuleToLoad : fully xposed compatible
     *  -> classesToLoad : descriptive hooking language
     *
     *  E.g:
     *     xposedModuleToLoad.add(DriveReplace.class);
     *
     *     DriveReplace is a fully compliant Xposed module class with
     *     handleLoadPackage(...) method
     *
     *
     *
     *  Any of the 3 options can be complemented with the classesToOffload
     *  in order to enable class offloading.
     */

    /**
     * This ArrayList holds the package names of the applications to hook
     */
    public static ArrayList<String> packagesToHook = new ArrayList<String>();

    static {
        //Add ur classes to register here
        packagesToHook.add("com.aknahs.gms.quickstart");
        packagesToHook.add("com.aknahs.gms.quickeditor");
    }

    /**
     * Holds a mapping between the original class name and the class where methods to be injected are implemented.
     */
    public static ArrayList<ClassLoaderInfo> classesToLoad = new ArrayList<ClassLoaderInfo>();

    static {
        //Add ur classes to register here

    }

    /**
     * Holds a xposed module which handleLoadPackage will be invoked.
     */
    public static ArrayList<Class<?>> xposedModuleToLoad = new ArrayList<Class<?>>();

    static {
        //Add ur classes to register here
        xposedModuleToLoad.add(DriveReplace.class);
    }

    public static ArrayList<String> classesToOffload = new ArrayList<String>();

    static {
        //Add ur classes to register here
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

    /*------------------------------------------------------
    * Hook descriptions for future implementations of patches
    * ------------------------------------------------------*/

    public enum InjectTiming {
        BEFORE_METHOD(1), AFTER_METHOD(2), REPLACE(3), UNKNOWN(-1);
        private int value;

        private InjectTiming(int value) {
            this.value = value;
        }

        public Integer getValue() {
            return this.value;
        }

        public static InjectTiming getInjectTimingForValue(Integer value) {
            switch (value) {
                case 1:
                    return BEFORE_METHOD;
                case 2:
                    return AFTER_METHOD;
                case 3:
                    return REPLACE;
            }
            return UNKNOWN;
        }
    }

    public class ClassLoaderInfo {
        public String originalClassName;
        public String injectClassName;
        public Boolean isOffloadable = false;

        /**
         * Holds a mapping between the original method name and the method to inject and respective timing.
         */
        public ArrayList<MethodLoaderInfo> methodMapping = new ArrayList<MethodLoaderInfo>();

        public class MethodLoaderInfo {
            InjectTiming injectTiming;
            String newMethodName;
            String oldMethodName;

            /**
             * Creates a Method replacement description for Droidnesis to interpret how to hook the method.
             *
             * @param newM   name of the new method to be invoked.
             * @param oldM   name of the method to be hooked with the new method
             * @param timing how should new method be invoked. Possible : BEFORE_METHOD, AFTER_METHOD, REPLACE
             */
            public MethodLoaderInfo(String newM, String oldM, InjectTiming timing) {
                injectTiming = timing;
                newMethodName = newM;
                oldMethodName = oldM;
            }
        }

        /**
         * Holds information regarding the class for which some methods will be hooked.
         *
         * @param originalclass The name of the class which methods will be hooked
         * @param newclass      The name of the class with the new hooking methods
         * @param canoffload    true, if this class should be offloaded
         * @param methods       A list with the method descriptions for hooking
         */
        public ClassLoaderInfo(String originalclass, String newclass, Boolean canoffload, ArrayList<MethodLoaderInfo> methods) {
            originalClassName = originalclass;
            injectClassName = newclass;
            methodMapping = methods;
            isOffloadable = canoffload;
        }
    }
}
