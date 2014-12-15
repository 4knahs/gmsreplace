package drivereplace.aknahs.com.drivexposedmodule;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.instantiator.android.Android10Instantiator;
import org.objenesis.instantiator.android.Android17Instantiator;
import org.objenesis.instantiator.android.Android18Instantiator;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by aknahs on 11/12/14.
 */
public class DriveReplace implements IXposedHookLoadPackage {

    /* Android version */
    public static Integer _androidVersion;

//    public static String DRIVE_FILE_NAME = "/data/local/tmp/drivefile";

    public static String TAG = "DRIVEREPLACE";

    /* Holds instantiators for known local classes */
    public static HashMap<Class<?>, ObjectInstantiator<?>> _instantiatorCache = new HashMap<Class<?>, ObjectInstantiator<?>>();

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

    Class<?> googleAPIClientBuilder;
    Class<?> googleApiClient;
    Class<?> googleApiClientImplementation;
    Class<?> googleDriveApiImplementation;
    Class<?> googleDriveApiPendingResultImplementation;
    Class<?> driveContentsResult;
    Class<?> googleStatus;
    Class<?> googleDriveContents;

    public static Activity mCurrentActivity = null;
    public static XC_LoadPackage.LoadPackageParam mLpparam = null;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("Loaded app: " + lpparam.packageName);

        if (!lpparam.packageName.equals("com.aknahs.gms.quickstart"))
            return;

        mLpparam = lpparam;

        /*
         * This class contains the newActivity method called when retrieving an
		 * activity.
		 */
        Class<?> instrumentation = XposedHelpers.findClass(
                "android.app.Instrumentation", lpparam.classLoader);

        /*
         * Hook this method to inject our MqttConnectorEmptyActivity this new
		 * activity binds to the mqtt service using our mqq app connector
		 */
        // TODO: this should intercept only the specific method
        XposedBridge.hookAllMethods(instrumentation, "newActivity", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                mCurrentActivity = (Activity) param.getResult();

                Log.v(TAG, "Current Activity : " + mCurrentActivity.getClass().getName());
            }
        });


        googleAPIClientBuilder = XposedHelpers.findClass("com.google.android.gms.common.api.GoogleApiClient.Builder", lpparam.classLoader);
        googleApiClient = XposedHelpers.findClass("com.google.android.gms.common.api.GoogleApiClient", lpparam.classLoader);

        /*Can this change from app to app? The api.b?*/
        googleApiClientImplementation = XposedHelpers.findClass("com.google.android.gms.common.api.b", lpparam.classLoader);

        //The current implementation class of Drive.DriveAPI is : com.google.android.gms.drive.internal.o

        googleDriveApiImplementation = XposedHelpers.findClass("com.google.android.gms.drive.internal.o", lpparam.classLoader);

        // type of pending result = com.google.android.gms.drive.internal.o$3
        googleDriveApiPendingResultImplementation = XposedHelpers.findClass("com.google.android.gms.drive.internal.o$3", lpparam.classLoader);

        //type of DriveContentsResult result = com.google.android.gms.drive.internal.o$c
        driveContentsResult = XposedHelpers.findClass("com.google.android.gms.drive.internal.o$c", mLpparam.classLoader);

        googleStatus = XposedHelpers.findClass("com.google.android.gms.common.api.Status", mLpparam.classLoader);
//        type of DriveContentsResult.getDriveContents result = com.google.android.gms.drive.internal.r
//        type of DriveContentsResult.getDriveContents.getOutputStream() result = java.io.FileOutputStream

        googleDriveContents = XposedHelpers.findClass("com.google.android.gms.drive.internal.r", mLpparam.classLoader);

        hookGoogleAPIClientAndBuilder();
        hookDriveAPI();

//        Class<?> pendingRes = XposedHelpers.findClass("com.google.android.gms.drive.internal.o$c", mLpparam.classLoader);
//
//        //com.google.android.gms.drive.internal.o$c
//        XposedBridge.hookAllConstructors(pendingRes, new XC_MethodHook() {
//
//            @Override
//            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
//                for(Object o : methodHookParam.args){
//                    Log.v(TAG, "o$c constructor argument : " + o.getClass().getName());
//                    //o$c constructor argument : com.google.android.gms.common.api.Status
//                    //o$c constructor argument : com.google.android.gms.drive.internal.r
//                }
//            }
//        });
    }

    /**
     * Print a file's metadata.
     *
     * @param service Drive API service instance.
     * @param fileId  ID of the file to print metadata for.
     */
    private static File getFile(Drive service, String fileId) {

        try {
            return service.files().get(fileId).execute();

//            System.out.println("Title: " + file.getTitle());
//            System.out.println("Description: " + file.getDescription());
//            System.out.println("MIME type: " + file.getMimeType());
        } catch (IOException e) {
            System.out.println("An error occured: " + e);
        }

        return null;
    }

    /**
     * Retrieve a list of File resources.
     *
     * @param service Drive API service instance.
     * @return List of File resources.
     */
    private static List<File> retrieveAllFiles(Drive service) throws IOException {
        List<File> result = new ArrayList<File>();
        Drive.Files.List request = service.files().list();
        request.setQ("mimeType = 'application/vnd.google-apps.folder'");
        do {
            try {
                FileList files = request.execute();

                result.addAll(files.getItems());
                request.setPageToken(files.getNextPageToken());
            } catch (IOException e) {
                System.out.println("An error occurred: " + e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null &&
                request.getPageToken().length() > 0);

        return result;
    }


    /**
     * Print files belonging to a folder.
     *
     * @param service  Drive API service instance.
     * @param folderId ID of the folder to print files from.
     */
    private static void printFilesInFolder(Drive service, String folderId)
            throws IOException {
        Drive.Children.List request = service.children().list(folderId);

        do {
            try {
                ChildList children = request.execute();

                for (ChildReference child : children.getItems()) {

                    File file = getFile(service, child.getId());
                    Log.v(TAG, "File Id: " + child.getId() + " name : " + file.getTitle());
                }
                request.setPageToken(children.getNextPageToken());
            } catch (IOException e) {
                Log.v(TAG, "An error occurred: " + e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null &&
                request.getPageToken().length() > 0);
    }

    public static String FILE_NAME = "android-photo";

    /**
     * Insert new file.
     *
     * @param service     Drive API service instance.
     * @param title       Title of the file to insert, including the extension.
     * @param description Description of the file to insert.
     * @param parentId    Optional parent folder's ID.
     * @param mimeType    MIME type of the file to insert.
     * @return Inserted file metadata if successful, {@code null} otherwise.
     */
    private static File insertFile(Drive service, String title, String description,
                                   String parentId, String mimeType) {
        // File's metadata.
        File body = new File();
        body.setTitle(title);
        body.setDescription(description);
        body.setMimeType(mimeType);

        // Set the parent folder.
        if (parentId != null && parentId.length() > 0) {
            body.setParents(
                    Arrays.asList(new ParentReference().setId(parentId)));
        }

        Log.v(TAG, "Trying to access file : " + mCurrentActivity.getFilesDir() +"/" +FILE_NAME);

        // File's content.
        java.io.File fileContent = new java.io.File(mCurrentActivity.getFilesDir() + "/" +FILE_NAME);
        FileContent mediaContent = new FileContent(mimeType, fileContent);
        try {
            File file = service.files().insert(body, mediaContent).execute();

            // Uncomment the following line to print the File ID.
            // System.out.println("File ID: " + file.getId());

            return file;
        } catch (IOException e) {
            System.out.println("An error occured: " + e);
            return null;
        }
    }



    /*
    *  Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveContentsResult>() {

            @Override
            public void onResult(DriveContentsResult result) {
                ...
                result.getStatus().isSuccess()
                ...
                result.getDriveContents().getOutputStream();
                ...
                Drive.DriveApi
                        .newCreateFileActivityBuilder()
                        .setInitialMetadata(metadataChangeSet)
                        .setInitialDriveContents(result.getDriveContents())
                        .build(mGoogleApiClient);

    * */

    public static Object driveContentsResultCallBack = null;
    public static Object results = new Object();
    public static List<com.google.api.services.drive.model.File> files = null;
    public static OutputStream driveOutputStream = null;
    public static String title;
    public static String mime;

    public void hookDriveAPI() {

        Log.v(TAG, "Hooking newDriveContents ");

        /* Drive.DriveApi.newDriveContents(mGoogleApiClient)*/
        //TODO: this should not be a hookAllMethods
        XposedBridge.hookAllMethods(googleDriveApiImplementation, "newDriveContents", new XC_MethodReplacement() {

            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());

                final ObjectInstantiator<?> instantiator = getInstantiator(googleDriveApiPendingResultImplementation);

                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        /*Get Contents of Drive!!*/
                        try {

                            //printFilesInFolder(service, "root");
                            files = retrieveAllFiles(service);
                            for (File f : files)
                                Log.v(TAG, "File name : " + f.getTitle());

                            synchronized (results) {
                                if (driveContentsResultCallBack == null) {
                                    try {
                                        results.wait();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            ObjectInstantiator<?> instantiator1 = getInstantiator(driveContentsResult);

                            Method m = null;
                            for (Method mtz : driveContentsResultCallBack.getClass().getDeclaredMethods()) {
                                if (mtz.getName().equals("onResult")) {
                                    m = mtz;
                                    break;
                                }
                            }

                            if (m == null)
                                Log.v(TAG, "Couldnt find onResult method");

                            try {

                                m.invoke(driveContentsResultCallBack, instantiator1.newInstance());

                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
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

//        for (Method m : googleDriveApiPendingResultImplementation.getDeclaredMethods()) {
//            Log.v(TAG, "Declared Methods of googleDriveApiPendingResultImplementation : " + m.getName() + Modifier.toString(m.getModifiers()));
//        }
//
//        for (Method m : googleDriveApiPendingResultImplementation.getMethods()) {
//            Log.v(TAG, "Methods of googleDriveApiPendingResultImplementation : " + m.getName() + " " + Modifier.toString(m.getModifiers()) + " " + m.getDeclaringClass());
//        }

        Class<?> pendingRes = XposedHelpers.findClass("com.google.android.gms.common.api.BaseImplementation$AbstractPendingResult", mLpparam.classLoader);

          /* Drive.DriveApi.newDriveContents(mGoogleApiClient)   googleDriveApiPendingResultImplementation
                .setResultCallback(new ResultCallback<DriveContentsResult>() {*/
        XposedBridge.hookAllMethods(pendingRes, "setResultCallback", new XC_MethodReplacement() {

            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());

                synchronized (results) {
                    driveContentsResultCallBack = methodHookParam.args[0];
                    results.notifyAll();
                }

                ObjectInstantiator<?> instantiator = getInstantiator(googleDriveApiPendingResultImplementation);

                /*Wait for results, create DriveContentsResult and invoke call back with it*/

                return instantiator.newInstance();
            }
        });

        XposedBridge.hookAllMethods(driveContentsResult, "getStatus", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());

                Constructor<?> status = XposedHelpers.findConstructorBestMatch(googleStatus, Integer.class);
                return status.newInstance(SUCCESS);
            }
        });

        XposedBridge.hookAllMethods(driveContentsResult, "getDriveContents", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());

                ObjectInstantiator<?> instantiator = getInstantiator(googleDriveContents);

                return instantiator.newInstance();
            }
        });

        XposedBridge.hookAllMethods(googleDriveContents, "getOutputStream", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());

//                driveOutputStream = new FileOutputStream(DRIVE_FILE_NAME, false);
                driveOutputStream = mCurrentActivity.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);

                return driveOutputStream;
            }
        });

//        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
//                .setMimeType("image/jpeg").setTitle("Android Photo.png").build();
//        // Create an intent for the file chooser, and start it.
//        IntentSender intentSender = com.google.android.gms.drive.Drive.DriveApi
//                .newCreateFileActivityBuilder()
//                .setInitialMetadata(metadataChangeSet)
//                .setInitialDriveContents(result.getDriveContents())
//                .build(mGoogleApiClient);

        final Class<?> fileActivityBuilder = XposedHelpers.findClass("com.google.android.gms.drive.CreateFileActivityBuilder", mLpparam.classLoader);


        XposedBridge.hookAllMethods(googleDriveApiImplementation, "newCreateFileActivityBuilder", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());

                //com.google.android.gms.drive.CreateFileActivityBuilder

                ObjectInstantiator<?> instantiator = getInstantiator(fileActivityBuilder);
                return instantiator.newInstance();
            }
        });

        XposedBridge.hookAllMethods(fileActivityBuilder, "setInitialMetadata", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());

                Object metadata = methodHookParam.args[0];
                Class<?> metaClass = metadata.getClass();
                Method getMimeType = XposedHelpers.findMethodBestMatch(metaClass, "getMimeType");
                Method getTitle = XposedHelpers.findMethodBestMatch(metaClass, "getTitle");

                mime = (String) getMimeType.invoke(metadata);
                title = (String) getTitle.invoke(metadata) + "-droid";

                Log.v(TAG, "Mime : " + mime + " title : " + title);

                //com.google.android.gms.drive.CreateFileActivityBuilder
                return methodHookParam.thisObject;
            }
        });

        XposedBridge.hookAllMethods(fileActivityBuilder, "setInitialDriveContents", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());

                //com.google.android.gms.drive.CreateFileActivityBuilder
                return methodHookParam.thisObject;
            }
        });

        XposedBridge.hookAllMethods(fileActivityBuilder, "build", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());

                mCurrentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Dialog dialog = new Dialog(mCurrentActivity);
                        AlertDialog.Builder builder = new AlertDialog.Builder(mCurrentActivity);
                        builder.setTitle("Select folder to store image");

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
                                String filename = (String) modeList.getItemAtPosition(checkedItem);

                                if (filename == null) {
                                    folderID = "root";
                                } else {


                                    Log.v(TAG, "Selected folder was : " + filename);

                                    for (File f : files) {
                                        if (f.getTitle().equals(filename)) {
                                            folderID = f.getId();
                                            break;
                                        }
                                    }
                                }

//insertFile(Drive service, String title, String description, String parentId, String mimeType, String filename)

                                if (folderID == null)
                                    Log.v(TAG, "FolderID was null!");

                                final String fID = folderID;

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        insertFile(service, title, "", fID, mime);

                                        synchronized (blockFolderSelection) {
                                            blockFolderIsReady = true;
                                            blockFolderSelection.notifyAll();
                                        }
                                    }
                                }).start();

                            }
                        });

                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                            }
                        });

                        dialog = builder.create();

                        dialog.show();

                        //com.google.android.gms.drive.CreateFileActivityBuilder
                        return;
                    }
                });

                synchronized (blockFolderSelection) {
                    if (blockFolderIsReady == false)
                        blockFolderSelection.wait();
                }

                Intent intent = new Intent(mCurrentActivity, mCurrentActivity.getClass());

                return PendingIntent.getActivity(mCurrentActivity, 0, intent, 0).getIntentSender();
            }
        });


    }

    public static Object blockFolderSelection = new Object();
    public static Boolean blockFolderIsReady = false;

    //    if (mGoogleApiClient == null) {
//        // Create the API client and bind it to an instance variable.
//        // We use this instance as the callback for connection and connection
//        // failures.
//        // Since no account name is passed, the user is prompted to choose.
//        mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addApi(Drive.API)
//                .addScope(Drive.SCOPE_FILE)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .build();
//    }
//
//    // Connect the client. Once connected, the camera is launched.
//    mGoogleApiClient.connect();

    private static String CLIENT_ID = "280386745163-ttaj1raa2ncdbf70btghdvps35adcjcc.apps.googleusercontent.com";
    private static String CLIENT_SECRET = "lULDjUuWpyA-pmEQXVUE4rzP";

    private static String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
    public static String serviceURL = "ERROR";

    public static Object connectionCallBackObject = null;
    public static Object connectionFailedCallbackObject = null;


    public void hookGoogleAPIClientAndBuilder() {
        XposedBridge.hookAllConstructors(googleAPIClientBuilder, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {

                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());

                return null;
            }
        });

        XposedBridge.hookAllMethods(googleAPIClientBuilder, "addApi", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());
                return methodHookParam.thisObject;
            }
        });

        XposedBridge.hookAllMethods(googleAPIClientBuilder, "addScope", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());
                return methodHookParam.thisObject;
            }
        });

        XposedBridge.hookAllMethods(googleAPIClientBuilder, "addConnectionCallbacks", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());

                connectionCallBackObject = methodHookParam.args[0];
                Log.v(TAG, "Setting connection callback instance : " + connectionCallBackObject.getClass().getName());

                return methodHookParam.thisObject;
            }
        });

        XposedBridge.hookAllMethods(googleAPIClientBuilder, "addOnConnectionFailedListener", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());

                connectionFailedCallbackObject = methodHookParam.args[0];
                Log.v(TAG, "Setting connection failure callback instance : " + connectionFailedCallbackObject.getClass().getName());

                return methodHookParam.thisObject;
            }
        });

        XposedBridge.hookAllMethods(googleAPIClientBuilder, "build", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {

                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());
                ObjectInstantiator<?> instantiator = getInstantiator(googleApiClientImplementation);
                Object googleApiClientInstance = instantiator.newInstance();

                return googleApiClientInstance;

//                return new MyGoogleAPIClient(connectionCallBackObject, connectionFailedCallbackObject);
            }
        });

//        XposedBridge.hookAllMethods(googleAPIClientBuilder, "build", new XC_MethodHook() {
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                Log.v(TAG, "Replaced : " + param.thisObject.getClass().getName() + " . " + param.method.getName() + " with return = " + param.getResult().getClass().getName());
//            }
//        });

        XposedBridge.hookAllMethods(googleApiClientImplementation, "disconnect", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return null;
            }
        });

        XposedBridge.hookAllMethods(googleApiClientImplementation, "connect", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.v(TAG, "Replaced : " + methodHookParam.thisObject.getClass().getName() + " . " + methodHookParam.method.getName());

                httpTransport = new NetHttpTransport();
                jsonFactory = new JacksonFactory();

                flow = new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport, jsonFactory, CLIENT_ID, CLIENT_SECRET, Arrays.asList(DriveScopes.DRIVE))
                        .setAccessType("online")
                        .setApprovalPrompt("auto").build();

                serviceURL = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
                Log.v(TAG, "Please open the following URL in your browser then type the authorization code:");
                Log.v(TAG, "  " + serviceURL);

                if (!isConnected) {

                    Log.v(TAG, "Was not connected yet!");

                    // Restore preferences
                    SharedPreferences settings = mCurrentActivity.getSharedPreferences("token", 0);
                    String token = settings.getString("token", null);
                    Long date = settings.getLong("date", 0);
                    Long period = (new Date().getTime()) - date;

                    Log.v(TAG, "Token period : " + period + " from date: " + date);

                    if (token != null && period < 5 * 60 * 1000) {
                        code = token;
                        Log.v(TAG, "Retrieved token! Attempting to resume app by calling onConnected callback");

                        completeConnection(code);

                    } else {

                        mCurrentActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                final EditText input = new EditText(mCurrentActivity);

                                new AlertDialog.Builder(mCurrentActivity)
                                        .setTitle("Allowing Google API access")
                                        .setMessage("If you have a token already, type it below and press \"Ok\".\n Otherwise press \"Get\".")
                                        .setView(input)
                                        .setPositiveButton("Get", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                Log.v(TAG, "Pressed Get");

                                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(serviceURL));
                                                mCurrentActivity.startActivity(browserIntent);

                                                //mCurrentActivity.finish();
                                                //System.exit(0);

                                            }
                                        })
                                        .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                Log.v(TAG, "Pressed Ok");

                                                Editable value = input.getText();

                                                completeConnection(value.toString());
                                            }
                                        }).show();
                            }
                        });
                    }

                } else {
                    Log.v(TAG, "Was already connected!");
                    Log.v(TAG, "Attempting to resume app by calling onConnected callback");

                    mCurrentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Class<?> connectionResultClass = XposedHelpers.findClass("com.google.android.gms.common.ConnectionResult", mLpparam.classLoader);
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

//        XposedBridge.hookAllMethods(XposedHelpers.findClass("com.aknahs.gms.quickstart.MainActivity", mLpparam.classLoader), "method", new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                Log.v(TAG, "The current implementation class of Drive.DriveAPI is : " + param.args[0].getClass().getName());
//            }
//        });


    }

    public static HttpTransport httpTransport;
    public static JsonFactory jsonFactory;
    public static GoogleAuthorizationCodeFlow flow;
    public static String code = null;
    public static volatile Boolean isConnected = false;

    public static final int SUCCESS = 0;
    public static final int SERVICE_MISSING = 1;
    public static final int SERVICE_VERSION_UPDATE_REQUIRED = 2;
    public static final int SERVICE_DISABLED = 3;
    public static final int SIGN_IN_REQUIRED = 4;
    public static final int INVALID_ACCOUNT = 5;
    public static final int RESOLUTION_REQUIRED = 6;
    public static final int NETWORK_ERROR = 7;
    public static final int INTERNAL_ERROR = 8;
    public static final int SERVICE_INVALID = 9;
    public static final int DEVELOPER_ERROR = 10;
    public static final int LICENSE_CHECK_FAILED = 11;
    public static final int CANCELED = 13;
    public static final int TIMEOUT = 14;
    public static final int INTERRUPTED = 15;
    public static final int API_UNAVAILABLE = 16;

    public static volatile Drive service = null;
    public static volatile GoogleTokenResponse response;
    public static volatile GoogleCredential credential;

    public void completeConnection(final String code) {

        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    Log.v(TAG, "Captured code : " + code);

                    if (!isConnected) {
                        response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
                        credential = new GoogleCredential().setFromTokenResponse(response);

                        //Create a new authorized API client
                        service = new Drive.Builder(httpTransport, jsonFactory, credential).build();

                        isConnected = true;
                    }

//                    mCurrentActivity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//
//                            new AlertDialog.Builder(mCurrentActivity)
//                                    .setTitle("Allowing Google API access")
//                                    .setMessage("Google Drive Service established!")
//                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
//                                        public void onClick(DialogInterface dialog, int whichButton) {
//                                            Log.v(TAG, "Pressed OK");
//
//                                        }
//                                    })
//                                    .show();
//                        }
//                    });

                    Log.v(TAG, "Storing token : " + code);

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
                            //Class<?> connectionResultClass = XposedHelpers.findClass("com.google.android.gms.common.ConnectionResult", mLpparam.classLoader);
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
                                            //mCurrentActivity.finish();
                                            //System.exit(0);
                                            service = null;
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
                            Class<?> connectionResultClass = XposedHelpers.findClass("com.google.android.gms.common.ConnectionResult", mLpparam.classLoader);
                            Method onConnectionFailed = XposedHelpers.findMethodExact(connectionFailedCallbackObject.getClass(), "onConnectionFailed", connectionResultClass);

                            Constructor connectionResultConstructor = XposedHelpers.findConstructorBestMatch(connectionResultClass, Integer.class, PendingIntent.class);

                            try {
                                Object ret = connectionResultConstructor.newInstance(INVALID_ACCOUNT, null);
                                onConnectionFailed.setAccessible(true);
                                onConnectionFailed.invoke(connectionFailedCallbackObject, ret);

                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            } catch (InstantiationException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });

        thr.start();

    }

}
