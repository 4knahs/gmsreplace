package drivereplace.aknahs.com.drivexposedmodule;

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

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
    Class<?> googleDriveFile;

    public static Activity mCurrentActivity = null;
    public static XC_LoadPackage.LoadPackageParam mLpparam = null;

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

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.v(TAG, "Loaded app: " + lpparam.packageName);

        if (!lpparam.packageName.equals("com.aknahs.gms.quickstart")
                && !lpparam.packageName.equals("com.aknahs.gms.quickeditor"))

            return;

        mLpparam = lpparam;

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
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                mCurrentActivity = (Activity) param.getResult();

                Log.v(TAG, "Current Activity : " + mCurrentActivity.getClass().getName());
            }
        });


        googleAPIClientBuilder = findClass("com.google.android.gms.common.api.GoogleApiClient.Builder", lpparam.classLoader);
        googleApiClient = findClass("com.google.android.gms.common.api.GoogleApiClient", lpparam.classLoader);

        /*Can this change from app to app? The api.b?*/
        googleApiClientImplementation = findClass("com.google.android.gms.common.api.b", lpparam.classLoader);

        //The current implementation class of Drive.DriveAPI is : com.google.android.gms.drive.internal.o

        googleDriveApiImplementation = findClass("com.google.android.gms.drive.internal.o", lpparam.classLoader);

        // type of pending result = com.google.android.gms.drive.internal.o$3
        googleDriveApiPendingResultImplementation = findClass("com.google.android.gms.drive.internal.o$3", lpparam.classLoader);

        //type of DriveContentsResult result = com.google.android.gms.drive.internal.o$c
        driveContentsResult = findClass("com.google.android.gms.drive.internal.o$c", mLpparam.classLoader);

        googleDriveFile = findClass("com.google.android.gms.drive.internal.s", mLpparam.classLoader);

        if (googleDriveFile == null)
            Log.v(TAG, "#3333####################################################################################################################################################33");

        googleStatus = findClass("com.google.android.gms.common.api.Status", mLpparam.classLoader);
//        type of DriveContentsResult.getDriveContents result = com.google.android.gms.drive.internal.r
//        type of DriveContentsResult.getDriveContents.getOutputStream() result = java.io.FileOutputStream

        googleDriveContents = findClass("com.google.android.gms.drive.internal.r", mLpparam.classLoader);

        hookGoogleAPIClient();
        hookDriveAPI();
        hookDriveFile();
        hookEditDriveFile();
        //hookGooglePlayServiceUtils();

//        Class<?> pendingRes = findClass("com.google.android.gms.drive.internal.o$c", mLpparam.classLoader);
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
     * Update an existing file's metadata and content.
     *
     * @param service        Drive API service instance.
     * @param fileId         ID of the file to update.
     * @param newTitle       New title for the file.
     * @param newDescription New description for the file.
     * @param newMimeType    New MIME type for the file.
     * @param newFilename    Filename of the new content to upload.
     * @param newRevision    Whether or not to create a new revision for this
     *                       file.
     * @return Updated file metadata if successful, {@code null} otherwise.
     */
    private static File updateFile(Drive service, String fileId, String newTitle,
                                   String newDescription, String newMimeType, String newFilename, boolean newRevision) {
        try {
            // First retrieve the file from the API.
            File file = service.files().get(fileId).execute();

            // File's new metadata.
            file.setTitle(newTitle);
            file.setDescription(newDescription);
            file.setMimeType(newMimeType);

            // File's new content.
            Log.v(TAG, "Trying to access file : " + mCurrentActivity.getFilesDir() + "/" + newFilename);

            // File's content.
            java.io.File fileContent = new java.io.File(mCurrentActivity.getFilesDir() + "/" + newFilename);

            //java.io.File fileContent = new java.io.File(newFilename);
            FileContent mediaContent = new FileContent(newMimeType, fileContent);

            // Send the request to the API.
            //File updatedFile
            FILE = service.files().update(fileId, file, mediaContent).execute();

            return FILE;
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
            return null;
        }
    }


    /**
     * Get a file's metadata.
     *
     * @param service Drive API service instance.
     * @param fileId  ID of the file to print metadata for.
     */
    private static File getFileMetadata(Drive service, String fileId) {

        try {
            Log.v(TAG, "getFileMetadata : Attempting to get fileID = " + fileId);
            return service.files().get(fileId).execute();

//            System.out.println("Title: " + file.getTitle());
//            System.out.println("Description: " + file.getDescription());
//            System.out.println("MIME type: " + file.getMimeType());
        } catch (IOException e) {
            Log.v(TAG, "An error occured: " + e);
        }

        return null;
    }

    public static final String ALL_FOLDERS_MIME = "application/vnd.google-apps.folder";//"mimeType = 'application/vnd.google-apps.folder'";
    public static final String ALL_FILES_MIME = "application/vnd.google-apps.file";//"mimeType = 'application/vnd.google-apps.file'";
    public static final String TEXT_FILES_MIME = "text/plain";

    /**
     * Retrieve a list of File resources.
     *
     * @param service Drive API service instance.
     * @return List of File resources.
     */
    private static List<File> retrieveAllFiles(Drive service, String mimeTypeQuery) throws IOException {
        List<File> result = new ArrayList<File>();
        Drive.Files.List request = service.files().list();
        request.setQ("mimeType = '" + mimeTypeQuery + "'");

        do {
            try {
                FileList files = request.execute();

                result.addAll(files.getItems());
                request.setPageToken(files.getNextPageToken());
            } catch (IOException e) {
                Log.v(TAG, "An error occurred: " + e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null &&
                request.getPageToken().length() > 0);

        return result;
    }

    /**
     * Download a file's content.
     *
     * @param service Drive API service instance.
     * @param file    Drive File instance.
     * @return InputStream containing the file's content if successful,
     * {@code null} otherwise.
     */
    private static InputStream downloadFile(Drive service, File file) {
        if (file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {
            try {
                HttpResponse resp =
                        service.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl()))
                                .execute();
                return resp.getContent();
            } catch (IOException e) {
                // An error occurred.
                e.printStackTrace();
                return null;
            }
        } else {
            // The file doesn't have any content stored on Drive.
            return null;
        }
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

                    File file = getFileMetadata(service, child.getId());
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

    public static String FILE_NAME = "drive-cache";
    public static File FILE = null;

    /**
     * Insert new file.
     *
     * @param service     Drive API service instance.
     * @param title       Title of the file to insert, including the extension.
     * @param description Description of the file to insert.
     * @param parentId    Optional parent folder's ID.
     * @param mimeType    MIME type of the file to insert.
     * @param filename    Filename of the file to insert.
     * @return Inserted file metadata if successful, {@code null} otherwise.
     */
    private static File insertFile(Drive service, String title, String description,
                                   String parentId, String mimeType, String filename) {

       /*
        *
        * Why the filename is different?
        * * @param filename    Filename (and fully qualified path) of the file to insert.
        *
        * Because we always write to a file atm. the result.getDriveContents.getOutputStream() used
        * to write and retrieve body contents is set to a file called :
        * mCurrentActivity.getFilesDir() + "/" + filename
        *
        * */

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

        try {
            if (filename != null) {
                Log.v(TAG, "Trying to access file : " + mCurrentActivity.getFilesDir() + "/" + filename);

                // File's content.
                java.io.File fileContent = new java.io.File(mCurrentActivity.getFilesDir() + "/" + filename);
                if (fileContent.exists()) {

                    FileContent mediaContent = new FileContent(mimeType, fileContent);

                    FILE = service.files().insert(body, mediaContent).execute();

                    // Uncomment the following line to print the File ID.
                    Log.v(TAG, "File ID: " + FILE.getId());

                } else {
                    Log.v(TAG, "There was no file. Creating empty");
                    FILE = service.files().insert(body).execute();
                }

                return FILE;

            } else {
                Log.v(TAG, "There was no file. Creating empty");
                FILE = service.files().insert(body).execute();

                return FILE;
            }
        } catch (IOException e) {
            Log.v(TAG, "An error occured: " + e);
            return null;
        }
    }


//
//    private static File insertFile(Drive service, String title, String description,
//                                   String parentId, String mimeType, String filename) {
//        // File's metadata.
//        File file = new File();
//        file.setTitle(title);
//        file.setDescription(description);
//        file.setmimeType("application/vnd.google-apps.drive-sdk");
//
//        file = service.files.insert(file).execute();
//
//        // Print the new file ID.
//        System.out.println("File ID: %s" + file.getId());
//    }

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

    public static void logHook(XC_MethodHook.MethodHookParam param) {

        if (param.thisObject != null)
            Log.v(TAG, "-->Replaced : " + param.thisObject.getClass().getName() + " . " + param.method.getName());
        else
            Log.v(TAG, "-->Replaced : " + param.method.getDeclaringClass().getName() + " . " + param.method.getName());
    }

    public static void hookGooglePlayServiceUtils() {
        Class<?> googlePlayServicesUtil = findClass("com.google.android.gms.common.GooglePlayServicesUtil", mLpparam.classLoader);

        if (googlePlayServicesUtil != null)
            XposedBridge.hookAllMethods(googlePlayServicesUtil, "isGooglePlayServicesAvailable",
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            logHook(methodHookParam);
                            return SUCCESS;
                        }
                    });

        Class<?> googleConnectionResults = findClass("com.google.android.gms.common.ConnectionResult", mLpparam.classLoader);

        if (googleConnectionResults != null)
            XposedBridge.hookAllConstructors(googleConnectionResults,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            logHook(param);
                            Log.v(TAG, "Printing stack trace:");
                            Exception e = new Exception();
                            e.printStackTrace();

                        }
                    });
    }

    public static Object _driveContentsResultCallBack = null;
    public static Object results = new Object();
    public static volatile List<com.google.api.services.drive.model.File> files = null;
    public static OutputStream driveOutputStream = null;
    public static InputStream fileInputStream = null;
    public static String _title;
    public static String _fID;
    public static String _mime;

    public static final int MODE_READ_ONLY = 268435456;
    public static final int MODE_READ_WRITE = 805306368;
    public static final int MODE_WRITE_ONLY = 536870912;

    public static final int METADATA = 0;
    public static final int FOPEN = 1;
    public static final int STATUS = 2;
    HashMap<Object, Integer> pendingResults = new HashMap<Object, Integer>();
//    public static Constructor<?> metadataChangeSetConstructor = null;


    public void hookEditDriveFile() {
        final Class<?> metadataChangeSet = findClass("com.google.android.gms.drive.MetadataChangeSet", mLpparam.classLoader);
        final Class<?> metadataChangeSetBuilder = findClass("com.google.android.gms.drive.MetadataChangeSet.Builder", mLpparam.classLoader);
        final Class<?> pendingResultOfMetadata = findClass("com.google.android.gms.drive.internal.w$1", mLpparam.classLoader);
        final Class<?> discardPendingResult = findClass("com.google.android.gms.drive.internal.o$4", mLpparam.classLoader);
        final Class<?> googleDriveSuper = findClass("com.google.android.gms.drive.internal.w", mLpparam.classLoader);

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
                        updateFile(service, _fID, _title, "", _mime, FILE_NAME, false);
                    }
                })).start();

                ObjectInstantiator<?> instantiator = getInstantiator(pendingResultOfMetadata);

                Object ret = instantiator.newInstance();

                Log.v(TAG, "Registering metadata object");

                pendingResults.put(ret, METADATA);

                return ret;
            }
        });

        XposedBridge.hookAllMethods(googleDriveContents, "commit", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                logHook(methodHookParam);


                ObjectInstantiator<?> inst = getInstantiator(discardPendingResult);

                Object ret = inst.newInstance();

                pendingResults.put(ret,STATUS);

                return ret;
            }
        });

    }

    public void hookDriveFile() {

        /*final PendingResult<MetadataResult>
                        mMetadataResultPendingResult = file.getMetadata(mGoogleApiClient);*/

        /*
        * DriveFile class : class com.google.android.gms.drive.internal.s
            mMetadataResultPendingResult class = class com.google.android.gms.drive.internal.w$1
            MetadataResult class = class com.google.android.gms.drive.internal.w$c
            DriveContentsResult class = class com.google.android.gms.drive.internal.o$c
            meta.getMetadata() class = class com.google.android.gms.drive.internal.l

        *
        * */
        if (googleDriveFile != null) {

            final Class<?> metadataResult = findClass("com.google.android.gms.drive.internal.w$c", mLpparam.classLoader);
            final Class<?> metadata = findClass("com.google.android.gms.drive.internal.l", mLpparam.classLoader);
            final Class<?> meta = findClass("com.google.android.gms.drive.Metadata", mLpparam.classLoader);

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
                    return status.newInstance(SUCCESS);
                }
            });

            final Class<?> discardPendingResult = findClass("com.google.android.gms.drive.internal.o$4", mLpparam.classLoader);

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

            //XposedBridge.hookAllMethods(driveContentsResult, "getDriveContents")

            final Class<?> googleDriveSuper = findClass("com.google.android.gms.drive.internal.w", mLpparam.classLoader);
            final Class<?> pendingResultOfMetadata = findClass("com.google.android.gms.drive.internal.w$1", mLpparam.classLoader);
            final Class<?> pendingResultAwaitImplementation = findClass("com.google.android.gms.common.api.BaseImplementation$AbstractPendingResult", mLpparam.classLoader);

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
                            return status.newInstance(SUCCESS);
                    }

                    return inst.newInstance();

//                    ObjectInstantiator<?> inst = getInstantiator(cls);
//                    return inst.newInstance();
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

                    FILE = getFileMetadata(service, _fID);

                    ObjectInstantiator<?> instantiator = getInstantiator(pendingResultOfMetadata);

                    Object ret = instantiator.newInstance();

                    Log.v(TAG, "Registering metadata object");

                    pendingResults.put(ret, METADATA);

                    return ret;

                    /*
                    DriveFile class : class com.google.android.gms.drive.internal.s
                    MetadataResult class = class com.google.android.gms.drive.internal.w$c --> metadataResult
                    DriveContentsResult class = class com.google.android.gms.drive.internal.o$c --> driveContentsResult
                    * */


/*
                     PendingResult<Result> pr = new PendingResult<Result>() {

                        @Override
                        public Result await() {
                            Log.v(TAG, "On await()");
                            FILE = getFileMetadata(service, _fID);

                            ObjectInstantiator<?> inst = getInstantiator(metadataResult);
                            return (Result) inst.newInstance();
                        }

                        @Override
                        public Result await(long l, TimeUnit timeUnit) {
                            Log.v(TAG, "On await()");
                            FILE = getFileMetadata(service, _fID);

                            ObjectInstantiator<?> inst = getInstantiator(metadataResult);
                            return (Result) inst.newInstance();
                        }

                        @Override
                        public void cancel() {
                        }

                        @Override
                        public boolean isCanceled() {
                            return false;
                        }

                        @Override
                        public void setResultCallback(ResultCallback<Result> resultResultCallback) {
                        }

                        @Override
                        public void setResultCallback(ResultCallback<Result> resultResultCallback, long l, TimeUnit timeUnit) {
                        }

                        @Override
                        public void a(a a) {
                        }
                    };

                    return pr;
                */
                }
            });

            final Class<?> pendingResultOfFileOpen = findClass("com.google.android.gms.drive.internal.s$2", mLpparam.classLoader);

//            XposedBridge.hookAllMethods(pendingResultOfFileOpen, "await", new XC_MethodReplacement() {
//                @Override
//                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
//                    logHook(methodHookParam);
//
//                    Log.v(TAG, "Await of pendingResultOfFileOpen");
//
//                    //com.google.android.gms.drive.internal.w$c cannot be cast to com.google.android.gms.drive.DriveApi$DriveContentsResult
//
//                    //driveContentsResult = findClass("com.google.android.gms.drive.internal.o$c", mLpparam.classLoader);
//
//                    //DriveContentsResult class = class com.google.android.gms.drive.internal.o$c
//
//                    ObjectInstantiator<?> inst = getInstantiator(driveContentsResult);
//                    return inst.newInstance();
//                }
//            });

            XposedBridge.hookAllMethods(driveContentsResult, "getStatus", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    Constructor<?> status = XposedHelpers.findConstructorBestMatch(googleStatus, Integer.class);
                    return status.newInstance(SUCCESS);
                }
            });

            XposedBridge.hookAllMethods(googleDriveFile, "open", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logHook(methodHookParam);

                    // PendingResult of file.open class = class com.google.android.gms.drive.internal.s$2


                    synchronized (blockFolderSelection) {
                        blockFolderIsReady = false;
                    }

                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            fileInputStream = downloadFile(service, FILE);

                            synchronized (blockFolderSelection) {
                                blockFolderIsReady = true;
                                blockFolderSelection.notifyAll();
                            }
                        }
                    }).start();

//                    synchronized (blockFolderSelection) {
//                        if (!blockFolderIsReady)
//                            blockFolderSelection.wait();
//                    }

                    ObjectInstantiator<?> inst = getInstantiator(pendingResultOfFileOpen);

                    Object ret = inst.newInstance();

                    pendingResults.put(ret, FOPEN);

                    return ret;

//                    PendingResult<Result> pr = new PendingResult<Result>() {
//                        @Override
//                        public Result await() {
//
//                            fileInputStream = downloadFile(service, FILE);
//
//                            ObjectInstantiator<?> inst = getInstantiator(driveContentsResult);
//                            return (Result) inst.newInstance();
//                        }
//
//                        @Override
//                        public Result await(long l, TimeUnit timeUnit) {
//                            fileInputStream = downloadFile(service, FILE);
//
//                            ObjectInstantiator<?> inst = getInstantiator(driveContentsResult);
//                            return (Result) inst.newInstance();
//                        }
//
//                        @Override
//                        public void cancel() {
//                        }
//
//                        @Override
//                        public boolean isCanceled() {
//                            return false;
//                        }
//
//                        @Override
//                        public void setResultCallback(ResultCallback<Result> resultResultCallback) {
//                        }
//
//                        @Override
//                        public void setResultCallback(ResultCallback<Result> resultResultCallback, long l, TimeUnit timeUnit) {
//                        }
//
//                        @Override
//                        public void a(a a) {
//                        }
//                    };
//
//                    return pr;
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
                    files = retrieveAllFiles(service, TEXT_FILES_MIME);

//                    while (files == null) {
//
//                    }

                    if(_showDialog) {
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
                            files = retrieveAllFiles(service, ALL_FOLDERS_MIME);
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

//        for (Method m : googleDriveApiPendingResultImplementation.getDeclaredMethods()) {
//            Log.v(TAG, "Declared Methods of googleDriveApiPendingResultImplementation : " + m.getName() + Modifier.toString(m.getModifiers()));
//        }
//
//        for (Method m : googleDriveApiPendingResultImplementation.getMethods()) {
//            Log.v(TAG, "Methods of googleDriveApiPendingResultImplementation : " + m.getName() + " " + Modifier.toString(m.getModifiers()) + " " + m.getDeclaringClass());
//        }

        Class<?> pendingRes = findClass("com.google.android.gms.common.api.BaseImplementation$AbstractPendingResult", mLpparam.classLoader);

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
                    return status.newInstance(SUCCESS);
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

//                driveOutputStream = new FileOutputStream(DRIVE_FILE_NAME, false);
                    //TODO: this technique assumes the outputstream is always new. Have to check the expected behavior

                    driveOutputStream = mCurrentActivity.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);

                    return driveOutputStream;
                }
            });

            final Class<?> contents = findClass("com.google.android.gms.drive.Contents", mLpparam.classLoader);

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

        final Class<?> fileActivityBuilder = findClass("com.google.android.gms.drive.CreateFileActivityBuilder", mLpparam.classLoader);
        final Class<?> openFileActivityBuilder = findClass("com.google.android.gms.drive.OpenFileActivityBuilder", mLpparam.classLoader);

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

                    //com.google.android.gms.drive.CreateFileActivityBuilder
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

                            registeredObjects.add(sender);

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

//                    Object metadata = methodHookParam.args[0];
//                    Class<?> metaClass = metadata.getClass();
//                    Method getMimeType = XposedHelpers.findMethodBestMatch(metaClass, "getMimeType");
//                    Method getTitle = XposedHelpers.findMethodBestMatch(metaClass, "getTitle");
//
//                    _mime = (String) getMimeType.invoke(metadata);
//                    _title = (String) getTitle.invoke(metadata); // + "-droid";

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

                    new Thread(new Runnable() {
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

                            insertFile(service, _title, "", _fID, _mime, FILE_NAME);
                            driveOutputStream = null;
                        }
                    }).start();

                    Log.v(TAG, "Attempting to set injected activity");
                    Intent intent = new Intent(mCurrentActivity, mCurrentActivity.getClass());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("showDialog", true);

                    IntentSender sender = PendingIntent.getActivity(mCurrentActivity, 0, intent, 0).getIntentSender();

                    registeredObjects.add(sender);

                    return sender;
                }
            });

        Class<?> driveId = findClass("com.google.android.gms.drive.DriveId", mLpparam.classLoader);

        //TODO: hook driveId methods

        Class<?> activity = findClass("android.app.Activity", mLpparam.classLoader);

        Method startIntentSenderForResult = XposedHelpers.findMethodExact(activity, "startIntentSenderForResult", IntentSender.class, int.class, Intent.class, int.class, int.class, int.class);

        XposedBridge.hookMethod(startIntentSenderForResult, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                logHook(param);

                IntentSender intent = (IntentSender) param.args[0];
//                Intent intent = (Intent) param.args[2];



                final MethodHookParam p = param;

                if (DriveReplace.registeredObjects.contains(intent)) {

                    mCurrentActivity.runOnUiThread(new Runnable() {
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
                }
            }
        });
    }

    public static HashSet<Object> registeredObjects = new HashSet<Object>();
//    private static HashMap<Method, XC_MethodReplacement> hookedOnActivityResult = new HashMap<Method, XC_MethodReplacement>();

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

//insertFile(Drive service, String title, String description, String parentId, String mimeType, String filename)

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

                Log.v(TAG, "Showing dialog");
                dialog.show();

                Log.v(TAG, "done with dialog");

                //com.google.android.gms.drive.CreateFileActivityBuilder
                return;
            }
        });
    }

//    public static void removeMethod(Method m) {
//        synchronized (hookedOnActivityResult) {
//            XC_MethodReplacement rep = hookedOnActivityResult.remove(m);
//            XposedBridge.unhookMethod(m, rep);
//        }
//    }

    public static Intent injectIntentOnActivityResult(Integer resquestCode, Integer resultCode, Intent data) {

        Intent returnIntent = new Intent();

        Class<?> driveId = findClass("com.google.android.gms.drive.DriveId", mLpparam.classLoader);

        if (driveId != null) {
//            ObjectInstantiator<?> inst = getInstantiator(driveId);
//            Object o = inst.newInstance();

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

    public static final java.lang.String EXTRA_RESPONSE_DRIVE_ID = "response_drive_id";
    public static Object blockFolderSelection = new Object();
    public static Boolean blockFolderIsReady = false;

    private static String CLIENT_ID = "280386745163-ttaj1raa2ncdbf70btghdvps35adcjcc.apps.googleusercontent.com";
    private static String CLIENT_SECRET = "lULDjUuWpyA-pmEQXVUE4rzP";

    private static String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
    public static String serviceURL = "ERROR";

    public static Object connectionCallBackObject = null;
    public static Object connectionFailedCallbackObject = null;


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
                                //Class<?> connectionResultClass = findClass("com.google.android.gms.common.ConnectionResult", mLpparam.classLoader);
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
                            //Class<?> connectionResultClass = findClass("com.google.android.gms.common.ConnectionResult", mLpparam.classLoader);
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
                            Class<?> connectionResultClass = findClass("com.google.android.gms.common.ConnectionResult", mLpparam.classLoader);
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
