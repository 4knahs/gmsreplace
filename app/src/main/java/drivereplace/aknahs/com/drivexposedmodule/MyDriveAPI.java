package drivereplace.aknahs.com.drivexposedmodule;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Contents;
import com.google.android.gms.drive.CreateFileActivityBuilder;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.OpenFileActivityBuilder;
import com.google.android.gms.drive.query.Query;

/**
 * Created by aknahs on 14/12/14.
 */
public class MyDriveAPI implements DriveApi{
    @Override
    public PendingResult<MetadataBufferResult> query(GoogleApiClient googleApiClient, Query query) {
        return null;
    }

    @Override
    public PendingResult<ContentsResult> newContents(GoogleApiClient googleApiClient) {
        return null;
    }

    @Override
    public PendingResult<DriveContentsResult> newDriveContents(GoogleApiClient googleApiClient) {
        return null;
    }

    @Override
    public PendingResult<Status> discardContents(GoogleApiClient googleApiClient, Contents contents) {
        return null;
    }

    @Override
    public PendingResult<DriveIdResult> fetchDriveId(GoogleApiClient googleApiClient, String s) {
        return null;
    }

    @Override
    public DriveFolder getRootFolder(GoogleApiClient googleApiClient) {
        return null;
    }

    @Override
    public DriveFolder getAppFolder(GoogleApiClient googleApiClient) {
        return null;
    }

    @Override
    public DriveFile getFile(GoogleApiClient googleApiClient, DriveId driveId) {
        return null;
    }

    @Override
    public DriveFolder getFolder(GoogleApiClient googleApiClient, DriveId driveId) {
        return null;
    }

    @Override
    public OpenFileActivityBuilder newOpenFileActivityBuilder() {
        return null;
    }

    @Override
    public CreateFileActivityBuilder newCreateFileActivityBuilder() {
        return null;
    }

    @Override
    public PendingResult<Status> requestSync(GoogleApiClient googleApiClient) {
        return null;
    }
}
