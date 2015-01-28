package com.drivereplace;
import android.util.Log;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by aknahs on 23/01/15.
 */
public class DriveUtils {

    public static String TAG = "DRIVEREPLACE";

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

    private static String CLIENT_ID = "280386745163-ttaj1raa2ncdbf70btghdvps35adcjcc.apps.googleusercontent.com";
    private static String CLIENT_SECRET = "lULDjUuWpyA-pmEQXVUE4rzP";
    private static String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
    public static String serviceURL = "ERROR";

    public static final String ALL_FOLDERS_MIME = "application/vnd.google-apps.folder";//"mimeType = 'application/vnd.google-apps.folder'";
    public static final String ALL_FILES_MIME = "application/vnd.google-apps.file";//"mimeType = 'application/vnd.google-apps.file'";
    public static final String TEXT_FILES_MIME = "text/plain";

    public static HttpTransport httpTransport;
    public static JsonFactory jsonFactory;
    public static GoogleAuthorizationCodeFlow flow;
    public static volatile GoogleTokenResponse response;
    public static volatile GoogleCredential credential;
    public static volatile Drive service = null;
    public static String code = null;

    public static void generateConnectionURL() {
        httpTransport = new NetHttpTransport();
        jsonFactory = new JacksonFactory();

        flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, CLIENT_ID, CLIENT_SECRET, Arrays.asList(DriveScopes.DRIVE))
                .setAccessType("online")
                .setApprovalPrompt("auto").build();

        serviceURL = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
        Log.v(TAG, "Please open the following URL in your browser then type the authorization code:");
        Log.v(TAG, "  " + serviceURL);
    }

    public static String getConnectionURL() {
        return serviceURL;
    }

    public static Drive connect() throws IOException {
        Log.v(TAG, "Captured code : " + code);

        response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
        credential = new GoogleCredential().setFromTokenResponse(response);

        //Create a new authorized API client
        service = new Drive.Builder(httpTransport, jsonFactory, credential).build();

        Log.v(TAG, "Storing token : " + code);

        return service;
    }

    public static void setConnectionToken(String token) {
        code = token;
    }

    /**
     * Update an existing file's metadata and content.
     *
     * @param fileId         ID of the file to update.
     * @param newTitle       New title for the file.
     * @param newDescription New description for the file.
     * @param newMimeType    New MIME type for the file.
     * @param newFilename    Filename of the new content to upload.
     * @param newRevision    Whether or not to create a new revision for this
     *                       file.
     * @return Updated file metadata if successful, {@code null} otherwise.
     */
    public static File updateFile(String fileId, String newTitle,
                                  String newDescription, String newMimeType, String newFilename, boolean newRevision) {
        try {

            // First retrieve the file from the API.
            File file = service.files().get(fileId).execute();

            // File's new metadata.
            file.setTitle(newTitle);
            file.setDescription(newDescription);
            file.setMimeType(newMimeType);

            // File's new content.
            Log.v(TAG, "Trying to access file : " + newFilename);

            // File's content.
            java.io.File fileContent = new java.io.File(newFilename);

            //java.io.File fileContent = new java.io.File(newFilename);
            FileContent mediaContent = new FileContent(newMimeType, fileContent);

            // Send the request to the API.
            //File updatedFile
            File newfile = service.files().update(fileId, file, mediaContent).execute();

            return newfile;
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
            return null;
        }
    }

    /**
     * Get a file's metadata.
     *
     * @param fileId ID of the file to print metadata for.
     */
    public static File getFileMetadata(String fileId) {

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

    /**
     * Retrieve a list of File resources.
     *
     * @return List of File resources.
     */
    public static List<File> retrieveAllFiles(String mimeTypeQuery) throws IOException {
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
     * @param file Drive File instance.
     * @return InputStream containing the file's content if successful,
     * {@code null} otherwise.
     */
    public static InputStream downloadFile(File file) {
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
     * @param folderId ID of the folder to print files from.
     */
    public static void printFilesInFolder(String folderId)
            throws IOException {
        Drive.Children.List request = service.children().list(folderId);

        do {
            try {
                ChildList children = request.execute();

                for (ChildReference child : children.getItems()) {

                    File file = getFileMetadata(child.getId());
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

    /**
     * Insert new file.
     *
     * @param title       Title of the file to insert, including the extension.
     * @param description Description of the file to insert.
     * @param parentId    Optional parent folder's ID.
     * @param mimeType    MIME type of the file to insert.
     * @param filename    Filename of the file to insert.
     * @return Inserted file metadata if successful, {@code null} otherwise.
     */
    public static File insertFile(String title, String description,
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

        File newfile;

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
                Log.v(TAG, "Trying to access file : " + filename);

                // File's content.
                java.io.File fileContent = new java.io.File(filename);
                if (fileContent.exists()) {

                    FileContent mediaContent = new FileContent(mimeType, fileContent);

                    newfile = service.files().insert(body, mediaContent).execute();

                    // Uncomment the following line to print the File ID.
                    Log.v(TAG, "File ID: " + newfile.getId());

                } else {
                    Log.v(TAG, "There was no file. Creating empty");
                    newfile = service.files().insert(body).execute();
                }

                return newfile;

            } else {
                Log.v(TAG, "There was no file. Creating empty");
                newfile = service.files().insert(body).execute();

                return newfile;
            }
        } catch (IOException e) {
            Log.v(TAG, "An error occured: " + e);
            return null;
        }
    }

}
