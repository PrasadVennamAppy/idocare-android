package il.co.idocare.controllers.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.ViewGroup;

import com.techyourchance.fragmenthelper.FragmentContainerWrapper;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.FragmentManager;
import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.android.components.ActivityComponent;
import il.co.idocare.R;
import il.co.idocare.mvcviews.mainnavdrawer.MainViewMvc;
import il.co.idocare.screens.common.toolbar.ToolbarDelegate;
import il.co.idocare.screens.navigationdrawer.NavigationDrawerDelegate;
import il.co.idocare.screens.navigationdrawer.events.NavigationDrawerStateChangeEvent;
import il.co.idocarecore.screens.ScreensNavigator;
import il.co.idocarecore.serversync.ServerSyncController;
import il.co.idocarecore.utils.Logger;

@AndroidEntryPoint
public class MainActivity extends AbstractActivity implements
        MainViewMvc.MainNavDrawerViewMvcListener,
        NavigationDrawerDelegate,
        ToolbarDelegate,
        FragmentContainerWrapper {

    private static final String TAG = "MainActivity";

    @EntryPoint
    @InstallIn(ActivityComponent.class)
    public interface MainActivityEntryPoint {
        public FragmentManager getFragmentManager();
        public FragmentFactory getFragmentFactory();
    }

    private static final int PERMISSION_REQUEST_GPS = 1;

    public static final String EXTRA_GPS_PERMISSION_REQUEST_RETRY = "EXTRA_GPS_PERMISSION_REQUEST_RETRY";

    @Inject ServerSyncController mServerSyncController;
    @Inject Logger mLogger;
    @Inject EventBus mEventBus;
    @Inject ScreensNavigator mScreensNavigator;

    private MainViewMvc mMainViewMvc;



    // ---------------------------------------------------------------------------------------------
    //
    // Activity lifecycle management

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MainActivityEntryPoint entryPoint =
                EntryPointAccessors.fromActivity(this, MainActivityEntryPoint.class);
        entryPoint.getFragmentManager().setFragmentFactory(entryPoint.getFragmentFactory());

        super.onCreate(savedInstanceState);

        mMainViewMvc = new MainViewMvc(LayoutInflater.from(this), null, this);
        mMainViewMvc.registerListener(this);
        setContentView(mMainViewMvc.getRootView());

        // Show Home fragment if the app is not restored
        if (savedInstanceState == null) {
            mScreensNavigator.toAllRequests();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mServerSyncController.enableAutomaticSync();
        mServerSyncController.requestImmediateSync();
        checkPermissions();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mServerSyncController.disableAutomaticSync();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mMainViewMvc.syncDrawerToggleState();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        boolean actionsVisibility = !drawerLayout.isDrawerVisible(GravityCompat.START);

        for(int i=0;i<menu.size();i++){
            menu.getItem(i).setVisible(actionsVisibility);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(EXTRA_GPS_PERMISSION_REQUEST_RETRY)) {
            checkGpsPermission();
        }
    }

    // End of activity lifecycle management
    //
    // ---------------------------------------------------------------------------------------------




    @Override
    public void onDrawerVisibilityStateChanged(boolean isVisible) {
        mEventBus.post(new NavigationDrawerStateChangeEvent(isVisible ?
                NavigationDrawerStateChangeEvent.STATE_OPENED :
                NavigationDrawerStateChangeEvent.STATE_CLOSED));
    }

    @Override
    public void openDrawer() {
        mMainViewMvc.openDrawer();
    }

    @Override
    public void closeDrawer() {
        mMainViewMvc.closeDrawer();
    }

    public void setTitle(String title) {
        mMainViewMvc.setTitle(title);
    }

    @Override
    public void onNavigateUpClicked() {
        mScreensNavigator.navigateUp();
    }

    @Override
    public void onBackPressed() {
        if (mMainViewMvc.isDrawerVisible()) {
            closeDrawer();
        } else {
            mScreensNavigator.navigateBack();
        }
    }

    // ---------------------------------------------------------------------------------------------
    //
    // Permissions management



    private void checkPermissions() {
        checkGpsPermission();
    }

    private void checkGpsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_GPS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissionsArray,
                                           @NonNull int[] grantResultsArray) {
        List<String> permissions = Arrays.asList(permissionsArray);
        if (requestCode == PERMISSION_REQUEST_GPS) {
            int gpsPermissionIndex = permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION);
            if (gpsPermissionIndex != -1
                    && grantResultsArray[gpsPermissionIndex] == PackageManager.PERMISSION_GRANTED) {
                // no-op: LocationTrackerService will account for GPS permission being granted
            }
        }
    }


    // End of permissions management
    //
    // ---------------------------------------------------------------------------------------------

    @Override
    public void showNavigateUpButton() {
        mMainViewMvc.setDrawerIndicatorEnabled(false);
    }

    @Override
    public void showNavDrawerButton() {
        mMainViewMvc.setDrawerIndicatorEnabled(true);
    }

    @Override
    public void hideToolbar() {
        mMainViewMvc.hideToolbar();
    }

    @Override
    public void showToolbar() {
        mMainViewMvc.showToolbar();
    }

    @NonNull
    @Override
    public ViewGroup getFragmentContainer() {
        return mMainViewMvc.getFrameContent();
    }
}
