package com.bluelinelabs.conductor.internal.lifecyclehandler;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface LifecycleHelper {

    String FRAGMENT_TAG = "LifecycleHelper";

    void install(@NonNull Activity activity);

    void onDestroy();

    void onAttach(Activity activity);

    void onAttach(Context context);

    void onDetach();

    void onActivityResult(int requestCode, int resultCode, Intent data);

    void startActivity(@NonNull Intent intent);

    void startActivityForResult(@NonNull Intent intent, int requestCode);

    void startActivityForResult(@NonNull Intent intent, int requestCode, @Nullable Bundle options);

    void startIntentSenderForResult(@NonNull IntentSender intent, int requestCode, @Nullable Intent fillInIntent,
                                    int flagsMask, int flagsValues, int extraFlags, @Nullable Bundle options) throws IntentSender.SendIntentException;

    void requestPermissions(@NonNull String[] permissions, int requestCode);

    void invalidateOptionsMenu();

    LifecycleHandler lifecycleHandler();

}