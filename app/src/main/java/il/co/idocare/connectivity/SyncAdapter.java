package il.co.idocare.connectivity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import il.co.idocare.authentication.AccountAuthenticator;
import il.co.idocare.contentproviders.IDoCareContract;
import il.co.idocare.pojos.RequestItem;
import il.co.idocare.utils.IDoCareJSONUtils;

/**
 * Our sync adapter
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {


    private static final String LOG_TAG = SyncAdapter.class.getSimpleName();


    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }


    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {

        Log.d(LOG_TAG, "onPerformSync() called");


        String authToken = getAuthToken(account);
        if (authToken == null) {
            Log.e(LOG_TAG, "Couldn't obtain auth token for the account" + account.name);
            // TODO: what do we do in that case? Need to use "open" APIs
            return;
        }

//        UserActionsUploader userActionsUploader =
//                new UserActionsUploader(account, authToken, provider);
//
//        // This call will block until all local actions will be synchronized to the server
//        // and the respective ContentProvider will be updated
//        userActionsUploader.uploadAll();


        ServerDataDownloader serverDataDownloader =
                new ServerDataDownloader(account, authToken, provider);

        // This call will block until all relevant data will be synchronized from the server
        // and the respective ContentProvider will be updated
        serverDataDownloader.downloadAll();


    }


    private String getAuthToken(Account account) {
        String authToken = null;
        try {
            authToken = AccountManager.get(this.getContext()).blockingGetAuthToken(
                    account,
                    AccountAuthenticator.AUTH_TOKEN_TYPE_DEFAULT,
                    true);
        } catch (OperationCanceledException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            e.printStackTrace();
        }

        return authToken;
    }

}
