package test.aknahs.com.droiddrivereplace;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.aknahs.droidloader.Helpers;
import com.aknahs.droidloader.InjectLocation;
import com.aknahs.droidloader.NotInitializedException;
import com.aknahs.droidloader.PatchInfo;
import com.google.api.services.drive.model.File;

import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.instantiator.android.Android10Instantiator;
import org.objenesis.instantiator.android.Android17Instantiator;
import org.objenesis.instantiator.android.Android18Instantiator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by aknahs on 11/12/14.
 * <p/>
 * -------------------------------------------------------------------------------
 * Language                     files          blank        comment           code
 * -------------------------------------------------------------------------------
 * Java                             1            302             88            978
 * -------------------------------------------------------------------------------
 */
public class DriveReplace implements IXposedHookLoadPackage{

    /* Android version */
    public static Integer _androidVersion;

    public static String TAG = "DRIVEREPLACE";

    /* Holds instantiators for known local classes */
    public static HashMap<Class<?>, ObjectInstantiator<?>> _instantiatorCache = new HashMap<Class<?>, ObjectInstantiator<?>>();

    /*-------------------
    /*-------------------
    Since intentRegistered object is accessed by client and backend, it has to be accessed through getters/setters*/

    private static HashSet<Object> intentRegisteredObjects = new HashSet<Object>();

    public static void addRegisteredObject(Integer o) {
        try {
            Log.v(TAG, "Attempting to add on shared storage");
            Helpers.shareObject(o, 0);
        } catch (NotInitializedException e) {
            e.printStackTrace();
            Log.v(TAG, "Helpers.shareObject was not available");
            intentRegisteredObjects.add(o);
        }
    }

    public static boolean containsObject(Integer o) {
        try {
            Object[] objs = Helpers.getSharedObjects();
            if (((Integer) objs[0]) == o)
                return true;
            return false;
        } catch (NotInitializedException e) {
            e.printStackTrace();
            Log.v(TAG, "Helpers.shareObject was not available");
            return intentRegisteredObjects.contains(o);
        }
    }
    /*-------------------*/

    private static HashMap<Object, Integer> pendingResults = new HashMap<Object, Integer>();

    public static volatile Boolean isConnected = false;

    public static final java.lang.String EXTRA_RESPONSE_DRIVE_ID = "response_drive_id";
    public static Object blockFolderSelection = new Object();
    public static Boolean blockFolderIsReady = false;

    public static Object _driveContentsResultCallBack = null;
    public static Object results = new Object();
    public static volatile List<File> files = null;
    public static OutputStream driveOutputStream = null;
    public static InputStream fileInputStream = null;
    public static String _title;
    public static String _fID;
    public static String _mime;

    public static String FILE_NAME = "drive-cache";
    public static File FILE = null;

    public static final int METADATA = 0;
    public static final int FOPEN = 1;
    public static final int STATUS = 2;

    public static Object connectionCallBackObject = null;
    public static Object connectionFailedCallbackObject = null;

     /*
        * DriveFile class : class com.google.android.gms.drive.internal.s
            mMetadataResultPendingResult class = class com.google.android.gms.drive.internal.w$1
            MetadataResult class = class com.google.android.gms.drive.internal.w$c
            DriveContentsResult class = class com.google.android.gms.drive.internal.o$c
            meta.getMetadata() class = class com.google.android.gms.drive.internal.l
        * */

    private static Class<?> googleAPIClientBuilder;
    private static Class<?> googleApiClient;
    private static Class<?> googleApiClientImplementation;
    private static Class<?> googleDriveApiImplementation;
    private static Class<?> googleDriveApiPendingResultImplementation;
    private static Class<?> driveContentsResult;
    private static Class<?> googleStatus;
    private static Class<?> googleDriveContents;
    private static Class<?> googleDriveFile;
    private static Class<?> metadataChangeSet;
    private static Class<?> metadataChangeSetBuilder;
    private static Class<?> pendingResultOfMetadata;
    private static Class<?> discardPendingResult;
    private static Class<?> googleDriveSuper;
    private static Class<?> metadataResult;
    private static Class<?> metadata;
    private static Class<?> meta;
    private static Class<?> googlePlayServicesUtil;
    private static Class<?> googleConnectionResults;
    private static Class<?> pendingResultAwaitImplementation;
    private static Class<?> pendingResultOfFileOpen;
    private static Class<?> pendingRes;
    private static Class<?> fileActivityBuilder;
    private static Class<?> openFileActivityBuilder;
    private static Class<?> driveId;
    private static Class<?> activity;
    private static Class<?> contents;
    private static Class<?> connectionResultClass;

    public void initClasses(XC_LoadPackage.LoadPackageParam lpparam) {
        googleAPIClientBuilder = findClass("com.google.android.gms.common.api.GoogleApiClient.Builder", lpparam.classLoader);
        googleApiClient = findClass("com.google.android.gms.common.api.GoogleApiClient", lpparam.classLoader);
        googleApiClientImplementation = findClass("com.google.android.gms.common.api.b", lpparam.classLoader);
        googleDriveApiImplementation = findClass("com.google.android.gms.drive.internal.o", lpparam.classLoader);
        googleDriveApiPendingResultImplementation = findClass("com.google.android.gms.drive.internal.o$3", lpparam.classLoader);
        driveContentsResult = findClass("com.google.android.gms.drive.internal.o$c", lpparam.classLoader);
        googleStatus = findClass("com.google.android.gms.common.api.Status", lpparam.classLoader);
        googleDriveContents = findClass("com.google.android.gms.drive.internal.r", lpparam.classLoader);
        googleDriveFile = findClass("com.google.android.gms.drive.internal.s", lpparam.classLoader);
        metadataChangeSet = findClass("com.google.android.gms.drive.MetadataChangeSet", lpparam.classLoader);
        metadataChangeSetBuilder = findClass("com.google.android.gms.drive.MetadataChangeSet.Builder", lpparam.classLoader);
        pendingResultOfMetadata = findClass("com.google.android.gms.drive.internal.w$1", lpparam.classLoader);
        discardPendingResult = findClass("com.google.android.gms.drive.internal.o$4", lpparam.classLoader);
        googleDriveSuper = findClass("com.google.android.gms.drive.internal.w", lpparam.classLoader);
        metadataResult = findClass("com.google.android.gms.drive.internal.w$c", lpparam.classLoader);
        metadata = findClass("com.google.android.gms.drive.internal.l", lpparam.classLoader);
        meta = findClass("com.google.android.gms.drive.Metadata", lpparam.classLoader);
        googlePlayServicesUtil = findClass("com.google.android.gms.common.GooglePlayServicesUtil", lpparam.classLoader);
        googleConnectionResults = findClass("com.google.android.gms.common.ConnectionResult", lpparam.classLoader);
        pendingResultAwaitImplementation = findClass("com.google.android.gms.common.api.BaseImplementation$AbstractPendingResult", lpparam.classLoader);
        pendingResultOfFileOpen = findClass("com.google.android.gms.drive.internal.s$2", lpparam.classLoader);
        pendingRes = findClass("com.google.android.gms.common.api.BaseImplementation$AbstractPendingResult", lpparam.classLoader);
        fileActivityBuilder = findClass("com.google.android.gms.drive.CreateFileActivityBuilder", lpparam.classLoader);
        openFileActivityBuilder = findClass("com.google.android.gms.drive.OpenFileActivityBuilder", lpparam.classLoader);
        driveId = findClass("com.google.android.gms.drive.DriveId", lpparam.classLoader);
        activity = findClass("android.app.Activity", lpparam.classLoader);
        contents = findClass("com.google.android.gms.drive.Contents", lpparam.classLoader);
        connectionResultClass = findClass("com.google.android.gms.common.ConnectionResult", lpparam.classLoader);
    }

    public static Activity mCurrentActivity = null;

    public DriveReplace() {
        super();

        /*
         * Used for objenesis generation of objects without using constructor
		 * for setting the required compatibility version
		 */
        _androidVersion = android.os.Build.VERSION.SDK_INT;
    }

    /*
     * Returns a objenesis objectInstantiator that allows to instantiate objects
     * without calling the respective constructors. Why is this useful? to allow
     * creating a proxy, hooked object without it being intercepted by Xposed.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ObjectInstantiator<?> getInstantiator(Class<?> cls) {
        ObjectInstantiator<?> o = _instantiatorCache.get(cls);
        if (o != null)
            return o;

        if (_androidVersion < 17)
            o = new Android10Instantiator(cls);
        else if (_androidVersion == 17)
            o = new Android17Instantiator(cls);
        else if (_androidVersion >= 18)
            o = new Android18Instantiator(cls);

        synchronized (_instantiatorCache) {
            if (!_instantiatorCache.containsKey(cls))
                _instantiatorCache.put(cls, o);
        }

        return o;
    }

    public static Class<?> findClass(String classname, ClassLoader loader) {
        Class<?> cls;
        try {
            cls = XposedHelpers.findClass(classname, loader);
        } catch (Throwable e) {
            Log.v(TAG, "------> Couldnt find class : " + classname);
            return null;
        }

        return cls;
    }

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.v(TAG, "Loaded app: " + lpparam.packageName);

        if (!lpparam.packageName.equals("com.aknahs.gms.quickstart")
                && !lpparam.packageName.equals("com.aknahs.gms.quickeditor"))

            return;

        initClasses(lpparam);

        /*
         * This class contains the newActivity method called when retrieving an
		 * activity.
		 */
        Class<?> instrumentation = findClass(
                "android.app.Instrumentation", lpparam.classLoader);

        /*
         * Hook this method to inject our MqttConnectorEmptyActivity this new
		 * activity binds to the mqtt service using our mqq app connector
		 */
        // TODO: this should intercept only the specific method
        XposedBridge.hookAllMethods(instrumentation, "newActivity", new XC_MethodHook() {

            @PatchInfo(location = InjectLocation.BACKEND)
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                mCurrentActivity = (Activity) param.getResult();

                Log.v(TAG, "Current Activity : " + mCurrentActivity.getClass().getName());
            }
        });

//        XposedBridge.hookAllMethods(googleStatus, "hashCode", new XC_MethodReplacement() {
//            @Override
//            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
//                return 1;
//            }
//        });

        hookGoogleAPIClient();
        hookDriveAPI();
        hookDriveFile();
        hookEditDriveFile();
    }

    public static void logHook(XC_MethodHook.MethodHookParam param) {

        if (param.thisObject != null)
            Log.v(TAG, "-->Replaced : " + param.thisObject.getClass().getName() + " . " + param.method.getName());
        else
            Log.v(TAG, "-->Replaced : " + param.method.getDeclaringClass().getName() + " . " + param.method.getName());
    }

//    public static void hookGooglePlayServiceUtils() {
//
//        if (googlePlayServicesUtil != null)
//            XposedBridge.hookAllMethods(googlePlayServicesUtil, "isGooglePlayServicesAvailable",
//                    new XC_MethodReplacement() {
//                        @Override
//                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
//                            logHook(methodHookParam);
//                            return DriveUtils.SUCCESS;
//                        }
//                    });
//
//
//        if (googleConnectionResults != null)
//            XposedBridge.hookAllConstructors(googleConnectionResults,
//                    new XC_MethodHook() {
//                        @Override
//                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                            logHook(param);
//                            Log.v(TAG, "Printing stack trace:");
//                            Exception e = new Exception();
//                            e.printStackTrace();
//
//                        }
//                    });
//    }

    public void hookEditDriveFile() {

        XposedBridge.hookAllConstructors(metadataChangeSetBuilder, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                logHook(methodHookParam);
                return null;
            }
        });

        XposedBridge.hookAllMethods(metadataChangeSetBuilder, "setTitle", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                logHook(methodHookParam);
                _title = (String) methodHookParam.args[0];
                return methodHookParam.thisObject;
            }
        });

        XposedBridge.hookAllMethods(metadataChangeSetBuilder, "setMimeType", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                logHook(methodHookParam);
                _mime = (String) methodHookParam.args[0];
                return methodHookParam.thisObject;
            }
        });

        XposedBridge.hookAllMethods(metadataChangeSetBuilder, "getMimeType", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return _mime;
            }
        });

        XposedBridge.hookAllMethods(metadataChangeSetBuilder, "build", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                logHook(methodHookParam);

                ObjectInstantiator<?> inst = getInstantiator(metadataChangeSet);
                return inst.newInstance();
            }
        });

        XposedBridge.hookAllMethods(googleDriveSuper, "updateMetadata", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FILE = DriveUtils.updateFile(_fID, _title, "", _mime, mCurrentActivity.getFilesDir() + "/" + FILE_NAME, false);
                    }
                })).start();

                ObjectInstantiator<?> instantiator = getInstantiator(pendingResultOfMetadata);

                Object ret = instantiator.newInstance();

                Log.v(TAG, "Registering metadata object");

                pendingResults.put(ret, METADATA);

                return ret;
            }
        });

        XposedBridge.hookAllMethods(googleDriveContents, "commit",
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        logHook(methodHookParam);


                        ObjectInstantiator<?> inst = getInstantiator(discardPendingResult);

                        Object ret = inst.newInstance();

                        pendingResults.put(ret, STATUS);

                        return ret;
                    }
                });

    }

    public void hookDriveFile() {

        if (googleDriveFile != null) {

            XposedBridge.hookAllMethods(metadataResult, "getMetadata", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    //  meta.getMetadata() class = class com.google.android.gms.drive.internal.l

                    ObjectInstantiator<?> inst = getInstantiator(metadata);

                    return inst.newInstance();
                }
            });

            XposedBridge.hookAllMethods(meta, "getTitle", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    if (FILE == null)
                        Log.v(TAG, "FILE was null");
                    else
                        Log.v(TAG, "File title = " + FILE.getTitle());

                    return FILE.getTitle();
                }
            });

            XposedBridge.hookAllMethods(metadataResult, "getStatus", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);
                    Constructor<?> status = XposedHelpers.findConstructorBestMatch(googleStatus, Integer.class);
                    return status.newInstance(DriveUtils.SUCCESS);
                }
            });

            XposedBridge.hookAllMethods(googleDriveFile, "discardContents", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    ObjectInstantiator<?> inst = getInstantiator(discardPendingResult);
                    //file.discardContents class = class com.google.android.gms.drive.internal.o$4

                    fileInputStream.close();
                    fileInputStream = null;

                    return inst.newInstance();
                }
            });

            XposedBridge.hookAllMethods(pendingResultAwaitImplementation, "await", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    Log.v(TAG, "Await of pendingResultAwaitImplementation - metadata");

                    synchronized (blockFolderSelection) {
                        if (!blockFolderIsReady)
                            blockFolderSelection.wait();
                    }

                    Integer type = pendingResults.get(methodHookParam.thisObject);

                    if (type == null) {
                        Log.v(TAG, "Failed guessing type of result of await");
                        return null;
                    }

                    ObjectInstantiator<?> inst = null;

                    switch (type) {
                        case FOPEN:
                            inst = getInstantiator(driveContentsResult);
                            break;
                        case METADATA:
                            inst = getInstantiator(metadataResult);
                            break;
                        case STATUS:
                            Constructor<?> status = XposedHelpers.findConstructorBestMatch(googleStatus, Integer.class);
                            return status.newInstance(DriveUtils.SUCCESS);
                    }

                    return inst.newInstance();
                }
            });

            XposedBridge.hookAllMethods(googleDriveSuper, "getMetadata", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);
                    //Returned result needs to be MetadataResult (com.google.android.gms.drive.MetadataResult

                    Log.v(TAG, "getMetadata of googleDriveSuper");

                    synchronized (blockFolderSelection) {
                        if (!blockFolderIsReady)
                            blockFolderSelection.wait();
                    }

                    FILE = DriveUtils.getFileMetadata(_fID);

                    ObjectInstantiator<?> instantiator = getInstantiator(pendingResultOfMetadata);

                    Object ret = instantiator.newInstance();

                    Log.v(TAG, "Registering metadata object");

                    pendingResults.put(ret, METADATA);

                    return ret;
                }
            });

            XposedBridge.hookAllMethods(driveContentsResult, "getStatus", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    Constructor<?> status = XposedHelpers.findConstructorBestMatch(googleStatus, Integer.class);
                    return status.newInstance(DriveUtils.SUCCESS);
                }
            });

            XposedBridge.hookAllMethods(googleDriveFile, "open", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    synchronized (blockFolderSelection) {
                        blockFolderIsReady = false;
                    }

                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            fileInputStream = DriveUtils.downloadFile(FILE);

                            synchronized (blockFolderSelection) {
                                blockFolderIsReady = true;
                                blockFolderSelection.notifyAll();
                            }
                        }
                    }).start();

                    ObjectInstantiator<?> inst = getInstantiator(pendingResultOfFileOpen);

                    Object ret = inst.newInstance();

                    pendingResults.put(ret, FOPEN);

                    return ret;

                }
            });
        }
    }

    public static volatile boolean _showDialog = false;

    public void hookDriveAPI() {

        Log.v(TAG, "Hooking newDriveContents ");


        if (googleDriveApiImplementation != null)

            XposedBridge.hookAllMethods(googleDriveApiImplementation, "getFile", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    ObjectInstantiator<?> inst = getInstantiator(googleDriveFile);

                    Log.v(TAG, "On get file");

                    Log.v(TAG, "Attempting to retrieve all files");
                    files = null;
                    files = DriveUtils.retrieveAllFiles(DriveUtils.TEXT_FILES_MIME);

                    if (_showDialog) {
                        createDialogAndWait("Select text file @1");
                        _showDialog = false;
                    }
                    return inst.newInstance();
                }
            });


        /* Drive.DriveApi.newDriveContents(mGoogleApiClient)*/
        //TODO: this should not be a hookAllMethods
        XposedBridge.hookAllMethods(googleDriveApiImplementation, "newDriveContents", new XC_MethodReplacement() {

            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                logHook(methodHookParam);

                final ObjectInstantiator<?> instantiator = getInstantiator(googleDriveApiPendingResultImplementation);

                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        /*Get Contents of Drive!!*/
                        try {

                            //printFilesInFolder(service, "root");
                            files = DriveUtils.retrieveAllFiles(DriveUtils.ALL_FOLDERS_MIME);
                            for (File f : files)
                                Log.v(TAG, "File name : " + f.getTitle());

                            synchronized (results) {
                                if (_driveContentsResultCallBack == null) {
                                    try {
                                        results.wait();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            ObjectInstantiator<?> instantiator1 = getInstantiator(driveContentsResult);

                            Method m = null;
                            for (Method mtz : _driveContentsResultCallBack.getClass().getDeclaredMethods()) {
                                if (mtz.getName().equals("onResult")) {
                                    m = mtz;
                                    break;
                                }
                            }

                            if (m == null)
                                Log.v(TAG, "---> Couldnt find onResult method");
                            else {

                                try {

                                    m.invoke(_driveContentsResultCallBack, instantiator1.newInstance());

                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                } catch (InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        synchronized (results) {
                            results.notifyAll();
                        }
                    }
                })).start();

                Object o = instantiator.newInstance();

                if (o == null)
                    Log.v(TAG, "Failed to create googleDriveApiPendingResultImplementation");
                else
                    Log.v(TAG, "Successfully created googleDriveApiPendingResultImplementation");

                return o;
            }
        });

        Log.v(TAG, "Hooking setResultCallback ");

        if (pendingRes != null)
          /* Drive.DriveApi.newDriveContents(mGoogleApiClient)   googleDriveApiPendingResultImplementation
                .setResultCallback(new ResultCallback<DriveContentsResult>() {*/
            XposedBridge.hookAllMethods(pendingRes, "setResultCallback", new XC_MethodReplacement() {

                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    synchronized (results) {
                        _driveContentsResultCallBack = methodHookParam.args[0];
                        results.notifyAll();
                    }

                    ObjectInstantiator<?> instantiator = getInstantiator(googleDriveApiPendingResultImplementation);

                /*Wait for results, create DriveContentsResult and invoke call back with it*/

                    return instantiator.newInstance();
                }
            });

        if (driveContentsResult != null)
            XposedBridge.hookAllMethods(driveContentsResult, "getStatus", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    Constructor<?> status = XposedHelpers.findConstructorBestMatch(googleStatus, Integer.class);
                    return status.newInstance(DriveUtils.SUCCESS);
                }
            });

        if (driveContentsResult != null)
            XposedBridge.hookAllMethods(driveContentsResult, "getDriveContents", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    ObjectInstantiator<?> instantiator = getInstantiator(googleDriveContents);

                    return instantiator.newInstance();
                }
            });

        if (googleDriveContents != null) {
            XposedBridge.hookAllMethods(googleDriveContents, "getOutputStream", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    //TODO: this technique assumes the outputstream is always new. Have to check the expected behavior
                    driveOutputStream = mCurrentActivity.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);

                    return driveOutputStream;
                }
            });

            XposedBridge.hookAllMethods(googleDriveContents, "getContents", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);


                    ObjectInstantiator<?> inst = getInstantiator(contents);

                    return inst.newInstance();
                }
            });

            XposedBridge.hookAllMethods(contents, "getInputStream", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    return fileInputStream;
                }
            });

        }

        if (fileActivityBuilder != null)
            XposedBridge.hookAllMethods(googleDriveApiImplementation, "newCreateFileActivityBuilder", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    //com.google.android.gms.drive.CreateFileActivityBuilder
                    _showDialog = true;

                    ObjectInstantiator<?> instantiator = getInstantiator(fileActivityBuilder);
                    return instantiator.newInstance();
                }
            });

        if (openFileActivityBuilder != null) {
            XposedBridge.hookAllMethods(googleDriveApiImplementation, "newOpenFileActivityBuilder", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    _mime = null;
                    _showDialog = true;

                    ObjectInstantiator<?> instantiator = getInstantiator(openFileActivityBuilder);
                    return instantiator.newInstance();
                }
            });

            XposedBridge.hookAllMethods(openFileActivityBuilder, "setMimeType", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    String[] mimeTypes = (String[]) methodHookParam.args[0];

                    //TODO: I still dont understand why this would be an array of strings. for now [0] suffices
                    _mime = mimeTypes[0];

                    return methodHookParam.thisObject;
                }
            });

            XposedBridge.hookAllMethods(openFileActivityBuilder, "build", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            logHook(methodHookParam);

                            Log.v(TAG, "Attempting to set injected activity");
                            Intent intent = new Intent(mCurrentActivity, mCurrentActivity.getClass());
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("showDialog", true);

                            IntentSender sender = PendingIntent.getActivity(mCurrentActivity, 0, intent, 0).getIntentSender();

                            addRegisteredObject(System.identityHashCode(sender));

                            return sender;
                        }
                    }
            );
        }

        if (fileActivityBuilder != null)
            XposedBridge.hookAllMethods(fileActivityBuilder, "setInitialMetadata", new XC_MethodReplacement() {

                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    Log.v(TAG, "Mime : " + _mime + " title : " + _title);

                    //com.google.android.gms.drive.CreateFileActivityBuilder
                    return methodHookParam.thisObject;
                }
            });

        if (fileActivityBuilder != null)
            XposedBridge.hookAllMethods(fileActivityBuilder, "setInitialDriveContents", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    //TODO: this is assuming that the driveContents are the ones in .getOutputStream. Probably should be param[0]
                    //driveOutputStream = methodHookParam.args[0];

                    //com.google.android.gms.drive.CreateFileActivityBuilder
                    return methodHookParam.thisObject;
                }
            });

        /*TODO: Note that this pending intent is not really the pendingIntent to retrieve the gms Activity.
        * We rather show our activity instantly and do the work before the startIntentSenderForResult
        * The startIntentSenderForResult just returns to the same activity with the required return code.*/
        if (fileActivityBuilder != null)
            XposedBridge.hookAllMethods(fileActivityBuilder, "build", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    createDialogAndWait("Select folder to create file: @2");

                    Log.v(TAG, "Inserting file");

                    Thread thr = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (blockFolderSelection) {
                                if (!blockFolderIsReady)
                                    try {
                                        blockFolderSelection.wait();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                            }

                            FILE = DriveUtils.insertFile(_title, "", _fID, _mime, mCurrentActivity.getFilesDir() + "/" + FILE_NAME);
                            driveOutputStream = null;
                        }
                    });

                    thr.start();

                    //-------
                    try{Helpers.isBackend();}
                    catch(NotInitializedException e){thr.join();}
                    //-------

                    Log.v(TAG, "Attempting to set injected activity");
                    Intent intent = new Intent(mCurrentActivity, mCurrentActivity.getClass());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("showDialog", true);

                    IntentSender sender = PendingIntent.getActivity(mCurrentActivity, 0, intent, 0).getIntentSender();

                    //addRegisteredObject(System.identityHashCode(sender));

                    return sender;
                }
            });

        Method startIntentSenderForResult = XposedHelpers.findMethodExact(activity, "startIntentSenderForResult", IntentSender.class, int.class, Intent.class, int.class, int.class, int.class);

        XposedBridge.hookMethod(startIntentSenderForResult, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                logHook(param);

                Log.v(TAG, "-------------->>>Intercepted startIntentSenderForResult!!");

                IntentSender intent = (IntentSender) param.args[0];

                if (intent == null)
                    Log.v(TAG, "intent was null");

                final MethodHookParam p = param;

                //if (containsObject(System.identityHashCode(intent)) {

                //---------------------
                Activity current;
                try{ current = ((Activity) Helpers.getCurrentActivity());}
                catch(NotInitializedException e ){current = mCurrentActivity;}
                //---------------------

                current.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Method onActivityResult = XposedHelpers.findMethodBestMatch(p.thisObject.getClass(), "onActivityResult", Integer.class, Integer.class, Intent.class);

                        Intent newIntent = injectIntentOnActivityResult((Integer) p.args[1], (Integer) Activity.RESULT_OK, null);

                        Log.v(TAG, "Setting new intent");
                        try {
                            onActivityResult.invoke(p.thisObject, p.args[1], Activity.RESULT_OK, newIntent);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                });

                param.setResult(null);
                //}
            }
        });
    }

    public static void createDialogAndWait(final String title) {

        Log.v(TAG, "Set folder not ready");
        synchronized (blockFolderSelection) {
            blockFolderIsReady = false;
        }

        mCurrentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "Running createDialogAndWait runnable");

                Dialog dialog = new Dialog(mCurrentActivity);
                AlertDialog.Builder builder = new AlertDialog.Builder(mCurrentActivity);
                builder.setTitle(title);
                builder.setCancelable(false);

                final ListView modeList = new ListView(mCurrentActivity);
                ArrayList<String> filenames = new ArrayList<String>();

                for (File f : files)
                    filenames.add(f.getTitle());

                String[] stringArray = new String[filenames.size()];
                stringArray = filenames.toArray(stringArray);

                ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(mCurrentActivity, android.R.layout.simple_list_item_activated_1, android.R.id.text1, stringArray);
                modeList.setAdapter(modeAdapter);
                modeList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

                builder.setView(modeList);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        String folderID = null;
                        int checkedItem = modeList.getCheckedItemPosition();
                        final String filename = (String) modeList.getItemAtPosition(checkedItem);

                        if (filename == null) {
                            folderID = "root";
                        } else {

                            Log.v(TAG, "Selected file/folder was : " + filename);

                            for (File f : files) {
                                if (f.getTitle().equals(filename)) {
                                    folderID = f.getId();
                                    break;
                                }
                            }
                        }

                        if (folderID == null)
                            Log.v(TAG, "File/FolderID was null!");

                        _fID = folderID;

                        Log.v(TAG, "Notifying folder selection");

                        synchronized (blockFolderSelection) {
                            blockFolderIsReady = true;
                            blockFolderSelection.notifyAll();
                        }

                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });

                Log.v(TAG, "Creating dialog");
                dialog = builder.create();
                dialog.setCanceledOnTouchOutside(false);

                Log.v(TAG, "Showing dialog");
                dialog.show();

                Log.v(TAG, "done with dialog");

                return;
            }
        });

//        synchronized (blockFolderSelection) {
//            if(!blockFolderIsReady)
//                try {
//                    blockFolderSelection.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//        }
    }

    public static Intent injectIntentOnActivityResult(Integer resquestCode, Integer resultCode, Intent data) {

        Intent returnIntent = new Intent();

        if (driveId != null) {

            Constructor<?> ctr = XposedHelpers.findConstructorExact(driveId, int.class, String.class, long.class, long.class);

            Object o = null;

            try {
                o = ctr.newInstance(123, "452342", (long) 0, (long) 0);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            if (o != null)
                returnIntent.putExtra("response_drive_id", (Parcelable) o);
        }

        return returnIntent;
    }

    public class IntentActivity extends Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            Log.v(TAG, "Injected IntentActivity");
            Intent intent = getIntent();
            String id = intent.getStringExtra(EXTRA_RESPONSE_DRIVE_ID);

            Intent returnIntent = new Intent();
            returnIntent.putExtra("EXTRA_RESPONSE_DRIVE_ID", id);

            if (getParent() == null) {
                setResult(Activity.RESULT_OK, returnIntent);
            } else {
                getParent().setResult(Activity.RESULT_OK, returnIntent);
            }

            finish();
        }
    }

    public void hookGoogleAPIClient() {

        if (googleAPIClientBuilder != null)
            XposedBridge.hookAllConstructors(googleAPIClientBuilder, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {

                    logHook(methodHookParam);

                    return null;
                }
            });

        if (googleAPIClientBuilder != null)
            XposedBridge.hookAllMethods(googleAPIClientBuilder, "addApi", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);
                    return methodHookParam.thisObject;
                }
            });

        if (googleAPIClientBuilder != null)
            XposedBridge.hookAllMethods(googleAPIClientBuilder, "addScope", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);
                    return methodHookParam.thisObject;
                }
            });

        if (googleAPIClientBuilder != null)
            XposedBridge.hookAllMethods(googleAPIClientBuilder, "addConnectionCallbacks", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    connectionCallBackObject = methodHookParam.args[0];
                    Log.v(TAG, "Setting connection callback instance : " + connectionCallBackObject.getClass().getName());

                    return methodHookParam.thisObject;
                }
            });

        if (googleAPIClientBuilder != null)
            XposedBridge.hookAllMethods(googleAPIClientBuilder, "addOnConnectionFailedListener", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    connectionFailedCallbackObject = methodHookParam.args[0];
                    Log.v(TAG, "Setting connection failure callback instance : " + connectionFailedCallbackObject.getClass().getName());

                    return methodHookParam.thisObject;
                }
            });

        if (googleAPIClientBuilder != null)
            XposedBridge.hookAllMethods(googleAPIClientBuilder, "build", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {

                    logHook(methodHookParam);
                    ObjectInstantiator<?> instantiator = getInstantiator(googleApiClientImplementation);
                    Object googleApiClientInstance = instantiator.newInstance();

                    return googleApiClientInstance;

//                return new MyGoogleAPIClient(connectionCallBackObject, connectionFailedCallbackObject);
                }
            });

        if (googleApiClientImplementation != null)
            XposedBridge.hookAllMethods(googleApiClientImplementation, "disconnect", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    return null;
                }
            });

        if (googleApiClientImplementation != null)
            XposedBridge.hookAllMethods(googleApiClientImplementation, "isConnected", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);
                    return true;
                }
            });

        if (googleApiClientImplementation != null)
            XposedBridge.hookAllMethods(googleApiClientImplementation, "connect", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    if (!isConnected) {

                        DriveUtils.generateConnectionURL();

                        Log.v(TAG, "Was not connected yet!");

                        // Restore preferences
                        SharedPreferences settings = mCurrentActivity.getSharedPreferences("token", 0);
                        String token = settings.getString("token", null);
                        Long date = settings.getLong("date", 0);
                        Long period = (new Date().getTime()) - date;

                        Log.v(TAG, "Token period : " + period + " from date: " + date);

                        if (token != null && period < 5 * 60 * 1000) {
                            Log.v(TAG, "Was already connected!");


                            DriveUtils.setConnectionToken(token);
                            Log.v(TAG, "Retrieved token! Attempting to resume app by calling onConnected callback");
                            DriveUtils.connect();

                            Log.v(TAG, "Attempting to resume app by calling onConnected callback");
                            mCurrentActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Method onConnected = XposedHelpers.findMethodExact(connectionCallBackObject.getClass(), "onConnected", Bundle.class);
                                    try {
                                        onConnected.setAccessible(true);
                                        onConnected.invoke(connectionCallBackObject, new Bundle());
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    } catch (InvocationTargetException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                        } else {

                            mCurrentActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    final EditText input = new EditText(mCurrentActivity);

                                    AlertDialog.Builder builder = new AlertDialog.Builder(mCurrentActivity)
                                            .setCancelable(false)
                                            .setTitle("Allowing Google API access")
                                            .setMessage("If you have a token already, type it below and press \"Ok\".\n Otherwise press \"Get\".")
                                            .setView(input)
                                            .setPositiveButton("Get", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    Log.v(TAG, "Pressed Get");

                                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DriveUtils.getConnectionURL()));
                                                    mCurrentActivity.startActivity(browserIntent);

                                                    //mCurrentActivity.finish();
                                                    //System.exit(0);

                                                }
                                            })
                                            .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    Log.v(TAG, "Pressed Ok");

                                                    Editable value = input.getText();

                                                    /*NEW CODE FOR OFFLOADING-----------------------------*/
                                                    try {
                                                        Helpers.cancelDialog();
                                                    } catch (NotInitializedException e) {
                                                        e.printStackTrace();
                                                    }
                                                    /*---------------------------------------------------*/

                                                    completeConnection(value.toString());
                                                }
                                            });

                                    AlertDialog dialog = builder.create();
                                    dialog.setCanceledOnTouchOutside(false);

                                    /*NEW CODE FOR OFFLOADING-----------------------------*/
                                    try {
                                        Helpers.registerDialog(dialog, Helpers.DialogBehavior.UNTIL_STOPPED);
                                    } catch (NotInitializedException e) {
                                        e.printStackTrace();
                                    }
                                    /*---------------------------------------------------*/

                                    dialog.show();


                                }
                            });
                        }

                    } else {
                        Log.v(TAG, "Was already connected!");
                        Log.v(TAG, "Attempting to resume app by calling onConnected callback");

                        mCurrentActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Method onConnected = XposedHelpers.findMethodExact(connectionCallBackObject.getClass(), "onConnected", Bundle.class);
                                try {
                                    onConnected.setAccessible(true);
                                    onConnected.invoke(connectionCallBackObject, new Bundle());
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                } catch (InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    return methodHookParam.thisObject;
                }
            });

    }

    public void completeConnection(final String code) {


        Log.v(TAG, "completeConnection");
        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    DriveUtils.setConnectionToken(code);
                    DriveUtils.connect();

                    // Restore preferences
                    SharedPreferences settings = mCurrentActivity.getSharedPreferences("token", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("token", code);
                    editor.putLong("date", new Date().getTime());

                    // Commit the edits!
                    editor.commit();

                    Log.v(TAG, "Attempting to resume app by calling onConnected callback");

                    mCurrentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Method onConnected = XposedHelpers.findMethodExact(connectionCallBackObject.getClass(), "onConnected", Bundle.class);
                            try {
                                onConnected.setAccessible(true);
                                Log.v(TAG, "About to invoke onConnected on connectionCallBackObject. Should probably run on Client");
                                onConnected.invoke(connectionCallBackObject, new Bundle());
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                    });


                } catch (IOException e) {
                    Log.v(TAG, "Failed connection to Google Services : ");
                    e.printStackTrace();

                    mCurrentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            new AlertDialog.Builder(mCurrentActivity)
                                    .setTitle("Allowing Google API access")
                                    .setMessage("Token unrecognized!")
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Log.v(TAG, "Pressed OK");

                                            isConnected = false;
                                            // Restore preferences
                                            SharedPreferences settings = mCurrentActivity.getSharedPreferences("token", 0);
                                            SharedPreferences.Editor editor = settings.edit();
                                            editor.putString("token", code);
                                            editor.putLong("date", 0);

                                            // Commit the edits!
                                            editor.commit();
                                        }
                                    })
                                    .show();
                        }
                    });

                    mCurrentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Method onConnectionFailed = XposedHelpers.findMethodExact(connectionFailedCallbackObject.getClass(), "onConnectionFailed", connectionResultClass);

                            Constructor connectionResultConstructor = XposedHelpers.findConstructorBestMatch(connectionResultClass, Integer.class, PendingIntent.class);

                            try {
                                Object ret = connectionResultConstructor.newInstance(DriveUtils.INVALID_ACCOUNT, null);
                                onConnectionFailed.setAccessible(true);
                                onConnectionFailed.invoke(connectionFailedCallbackObject, ret);

                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InstantiationException e1) {
                                e1.printStackTrace();
                            } catch (InvocationTargetException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
        thr.start();
    }
}

