package drivereplace.aknahs.com.drivexposedmodule;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.util.Log;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.BaseImplementation;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.c;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by aknahs on 14/12/14.
 */
public class MyGoogleAPIClient implements com.google.android.gms.common.api.GoogleApiClient {

    public static String TAG = "DRIVEREPLACE";

    public static HttpTransport httpTransport;
    public static JsonFactory jsonFactory;
    public static GoogleAuthorizationCodeFlow flow;
    public static String code = null;
    public static volatile Boolean connected = false;

    private static String CLIENT_ID = "280386745163-ttaj1raa2ncdbf70btghdvps35adcjcc.apps.googleusercontent.com";
    private static String CLIENT_SECRET = "lULDjUuWpyA-pmEQXVUE4rzP";

    private static String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
    public static String serviceURL = "ERROR";

    public static Object connectionCallBackObject = null;
    public static Object connectionFailedCallbackObject = null;

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

    Drive service;
    GoogleTokenResponse response;
    GoogleCredential credential;

    public MyGoogleAPIClient(Object conCallBack, Object conFailBack){
        connectionCallBackObject = conCallBack;
        connectionFailedCallbackObject = conFailBack;
    }

    @Override
    public <A extends Api.a, R extends Result, T extends BaseImplementation.a<R, A>> T a(T t) {
        return null;
    }

    @Override
    public <A extends Api.a, T extends BaseImplementation.a<? extends Result, A>> T b(T t) {
        return null;
    }

    @Override
    public <L> c<L> c(L l) {
        return null;
    }

    @Override
    public <C extends Api.a> C a(Api.c<C> cc) {
        return null;
    }

    @Override
    public boolean a(Scope scope) {
        return false;
    }

    @Override
    public Looper getLooper() {
        return null;
    }

    @Override
    public void connect() {
        Log.v(TAG, "On connect()");

        if (!connected) {
            httpTransport = new NetHttpTransport();
            jsonFactory = new JacksonFactory();

            flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, jsonFactory, CLIENT_ID, CLIENT_SECRET, Arrays.asList(DriveScopes.DRIVE))
                    .setAccessType("online")
                    .setApprovalPrompt("auto").build();

            serviceURL = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
            Log.v(TAG, "Please open the following URL in your browser then type the authorization code:");
            Log.v(TAG, "  " + serviceURL);

            DriveReplace.mCurrentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    final EditText input = new EditText(DriveReplace.mCurrentActivity);

                    new AlertDialog.Builder(DriveReplace.mCurrentActivity)
                            .setTitle("Allowing Google API access")
                            .setMessage("If you have a token already, type it below and press \"Ok\".\n Otherwise press \"Get\".")
                            .setView(input)
                            .setPositiveButton("Get", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Log.v(TAG, "Pressed Get");

                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(serviceURL));
                                    DriveReplace.mCurrentActivity.startActivity(browserIntent);

                                    DriveReplace.mCurrentActivity.finish();
                                    System.exit(0);

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
        return;
    }

    public void completeConnection(final String code) {

        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    Log.v(TAG, "Captured code : " + code);

                    response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
                    credential = new GoogleCredential().setFromTokenResponse(response);

                    //Create a new authorized API client
                    service = new Drive.Builder(httpTransport, jsonFactory, credential).build();

                    DriveReplace.mCurrentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            new AlertDialog.Builder(DriveReplace.mCurrentActivity)
                                    .setTitle("Allowing Google API access")
                                    .setMessage("Google Drive Service established!")
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Log.v(TAG, "Pressed OK");
                                            connected = true;

                                        }
                                    })
                                    .show();
                        }
                    });

                    Log.v(TAG, "Attempting to resume app by calling onConnected callback");

                    DriveReplace.mCurrentActivity.runOnUiThread(new Runnable() {
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

                    DriveReplace.mCurrentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            new AlertDialog.Builder(DriveReplace.mCurrentActivity)
                                    .setTitle("Allowing Google API access")
                                    .setMessage("Token unrecognized!")
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Log.v(TAG, "Pressed OK");
                                            //mCurrentActivity.finish();
                                            //System.exit(0);
                                        }
                                    })
                                    .show();
                        }
                    });

                    DriveReplace.mCurrentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Class<?> connectionResultClass = XposedHelpers.findClass("com.google.android.gms.common.ConnectionResult", DriveReplace.mLpparam.classLoader);
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

    @Override
    public ConnectionResult blockingConnect() {
        return null;
    }

    @Override
    public ConnectionResult blockingConnect(long l, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void reconnect() {

    }

    @Override
    public void stopAutoManage(FragmentActivity fragmentActivity) {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isConnecting() {
        return false;
    }

    @Override
    public void registerConnectionCallbacks(ConnectionCallbacks connectionCallbacks) {

    }

    @Override
    public boolean isConnectionCallbacksRegistered(ConnectionCallbacks connectionCallbacks) {
        return false;
    }

    @Override
    public void unregisterConnectionCallbacks(ConnectionCallbacks connectionCallbacks) {

    }

    @Override
    public void registerConnectionFailedListener(OnConnectionFailedListener onConnectionFailedListener) {

    }

    @Override
    public boolean isConnectionFailedListenerRegistered(OnConnectionFailedListener onConnectionFailedListener) {
        return false;
    }

    @Override
    public void unregisterConnectionFailedListener(OnConnectionFailedListener onConnectionFailedListener) {

    }
}
