package io.hypertrack.sendeta.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.hypertrack.lib.HyperTrack;
import com.hypertrack.lib.callbacks.HyperTrackCallback;
import com.hypertrack.lib.models.Action;
import com.hypertrack.lib.models.ErrorResponse;
import com.hypertrack.lib.models.SuccessResponse;

import java.util.ArrayList;

import io.hypertrack.sendeta.R;
import io.hypertrack.sendeta.model.AppDeepLink;
import io.hypertrack.sendeta.model.OnboardingUser;
import io.hypertrack.sendeta.store.UserStore;
import io.hypertrack.sendeta.util.Constants;
import io.hypertrack.sendeta.util.DeepLinkUtil;
import io.hypertrack.sendeta.util.SharedPreferenceManager;
import io.hypertrack.sendeta.util.Utils;

/**
 * Created by piyush on 23/07/16.
 */
public class SplashScreen extends BaseActivity {

    private static final String TAG = SplashScreen.class.getSimpleName();

    private AppDeepLink appDeepLink;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        prepareAppDeepLink();
        proceedToNextScreen();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        prepareAppDeepLink();
        proceedToNextScreen();
    }

    // Method to handle DeepLink Params
    private void prepareAppDeepLink() {
        appDeepLink = new AppDeepLink(DeepLinkUtil.DEFAULT);

        Intent intent = getIntent();
        // if started through deep link
        if (intent != null && !TextUtils.isEmpty(intent.getDataString())) {
            Log.d(TAG, "deeplink " + intent.getDataString());
            appDeepLink = DeepLinkUtil.prepareAppDeepLink(SplashScreen.this, intent.getData());
        }
    }

    private void proceedToNextScreen() {
        boolean isUserOnboard = UserStore.sharedStore.isUserLoggedIn(this);
        if (!isUserOnboard && !TextUtils.isEmpty(OnboardingUser.sharedOnboardingUser().getName())) {
            Intent registerIntent = new Intent(this, Profile.class);
            registerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(registerIntent);
            finish();
        } else if (!isUserOnboard) {
            Intent registerIntent = new Intent(this, CheckPermission.class);
            registerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(registerIntent);
            finish();
        } else {
            Utils.setCrashlyticsKeys(this);
            processAppDeepLink(appDeepLink);
        }
    }

    // Method to proceed to next screen with deepLink params
    private void processAppDeepLink(final AppDeepLink appDeepLink) {
        switch (appDeepLink.mId) {
            case DeepLinkUtil.RECEIVE_ETA:
                TaskStackBuilder.create(this)
                        .addNextIntentWithParentStack(new Intent(this, Home.class)
                                .putExtra(Constants.KEY_PUSH_TASK, true)
                                .putExtra(Constants.KEY_TASK_ID, appDeepLink.uuid)
                                .putExtra(Constants.KEY_PUSH_DESTINATION_LAT, appDeepLink.lat)
                                .putExtra(Constants.KEY_PUSH_DESTINATION_LNG, appDeepLink.lng)
                                .putExtra(Constants.KEY_ADDRESS, appDeepLink.address)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        .startActivities();
                finish();
                break;

            case DeepLinkUtil.TRACK:
                handleTrackingUrlDeepLink(appDeepLink.shortCode);
                break;

            case DeepLinkUtil.DEFAULT:
            default:
                TaskStackBuilder.create(this)
                        .addNextIntentWithParentStack(new Intent(this, Home.class)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        .startActivities();
                finish();
                break;
        }
    }

    private void handleTrackingUrlDeepLink(String shortCode) {
        displayLoader(true);

        HyperTrack.getActionForShortCode(shortCode, new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse response) {
                if (SplashScreen.this.isFinishing())
                    return;

                displayLoader(false);

                Action action = (Action) response.getResponseObject();
                ArrayList<String> taskIDList = new ArrayList<>();
                taskIDList.add(action.getId());

                // Check if current user is sharing location or not
                if (SharedPreferenceManager.getActionID(SplashScreen.this) == null) {
                    // Handle Deeplink on Home Screen with Live Location Sharing enabled
                    TaskStackBuilder.create(SplashScreen.this)
                            .addNextIntentWithParentStack(new Intent(SplashScreen.this, Home.class)
                                    .putExtra(Track.KEY_TASK_ID_LIST, taskIDList)
                                    .putExtra(Track.KEY_LOOKUP_ID, action.getLookupID())
                                    .putExtra(Track.KEY_TRACK_DEEPLINK, true)
                                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                            .startActivities();

                } else {
                    // Handle Deeplink on Track Screen with Live Location Sharing disabled
                    TaskStackBuilder.create(SplashScreen.this)
                            .addNextIntentWithParentStack(new Intent(SplashScreen.this, Track.class)
                                    .putExtra(Track.KEY_TASK_ID_LIST, taskIDList)
                                    .putExtra(Track.KEY_LOOKUP_ID, action.getLookupID())
                                    .putExtra(Track.KEY_TRACK_DEEPLINK, true)
                                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                            .startActivities();
                }
                finish();
            }

            @Override
            public void onError(@NonNull ErrorResponse errorResponse) {
                displayLoader(false);

                TaskStackBuilder.create(SplashScreen.this)
                        .addNextIntentWithParentStack(new Intent(SplashScreen.this, Home.class)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        .startActivities();
                finish();
            }
        });
    }
}
