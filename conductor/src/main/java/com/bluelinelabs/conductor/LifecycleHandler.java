package com.bluelinelabs.conductor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.bluelinelabs.conductor.internal.StringSparseArrayParceler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LifecycleHandler extends Fragment {

    private static final String HANDLER_FRAGMENT_TAG = "LifecycleHandler";
    private static final String KEY_PENDING_PERMISSION_REQUESTS = "LifecycleHandler.pendingPermissionRequests";
    private static final String KEY_PERMISSION_REQUEST_CODES = "LifecycleHandler.permissionRequests";
    private static final String KEY_ACTIVITY_REQUEST_CODES = "LifecycleHandler.activityRequests";
    private static final String KEY_ROUTER_STATE_PREFIX = "LifecycleHandler.routerState";
    private static final String KEY_LOCAL_STATE = "LifecycleHandler.localState";

    private boolean destroyed;
    private boolean attached;
    private boolean hasPreparedForHostDetach;

    private SparseArray<String> permissionRequestMap = new SparseArray<>();
    private SparseArray<String> activityRequestMap = new SparseArray<>();
    private ArrayList<PendingPermissionRequest> pendingPermissionRequests = new ArrayList<>();

    private final Map<Integer, ActivityHostedRouter> routerMap = new HashMap<>();
    private final Bundle localState = new Bundle();

    @NonNull
    public static LifecycleHandler install(@NonNull AppCompatActivity activity) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment existingHandler = fragmentManager.findFragmentByTag(HANDLER_FRAGMENT_TAG);
        if (existingHandler instanceof LifecycleHandler) {
            return (LifecycleHandler) existingHandler;
        } else {
            LifecycleHandler handler = new LifecycleHandler();
            fragmentManager.beginTransaction().add(handler, HANDLER_FRAGMENT_TAG).commit();
            return handler;
        }
    }

    private static int getRouterHashKey(@NonNull ViewGroup viewGroup) {
        return viewGroup.getId();
    }

    public LifecycleHandler() {
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @NonNull
    public Router getRouter(@NonNull ViewGroup container) {
        ActivityHostedRouter router = routerMap.get(getRouterHashKey(container));
        if (router == null) {
            router = new ActivityHostedRouter();
            router.setHost(this, container);

            Bundle routerState = localState.getBundle(KEY_ROUTER_STATE_PREFIX + router.getContainerId());
            if (routerState != null) {
                router.restoreInstanceState(routerState);
            }
            routerMap.put(getRouterHashKey(container), router);
        } else {
            router.setHost(this, container);
        }

        return router;
    }

    @NonNull
    public List<Router> getRouters() {
        return new ArrayList<Router>(routerMap.values());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleOnCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        handleOnCreate(savedInstanceState);
    }

    private void handleOnCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_LOCAL_STATE)) {
                localState.putAll(savedInstanceState.getBundle(KEY_LOCAL_STATE));
            }

            StringSparseArrayParceler permissionParcel = savedInstanceState.getParcelable(KEY_PERMISSION_REQUEST_CODES);
            permissionRequestMap = permissionParcel != null ? permissionParcel.getStringSparseArray() : new SparseArray<String>();

            StringSparseArrayParceler activityParcel = savedInstanceState.getParcelable(KEY_ACTIVITY_REQUEST_CODES);
            activityRequestMap = activityParcel != null ? activityParcel.getStringSparseArray() : new SparseArray<String>();

            ArrayList<PendingPermissionRequest> pendingRequests = savedInstanceState.getParcelableArrayList(KEY_PENDING_PERMISSION_REQUESTS);
            pendingPermissionRequests = pendingRequests != null ? pendingRequests : new ArrayList<PendingPermissionRequest>();
        }

        for (ActivityHostedRouter router : new ArrayList<>(routerMap.values())) {
            router.onContextAvailable();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        hasPreparedForHostDetach = false;

        Activity activity = getActivity();
        if (activity != null) for (Router router : getRouters()) {
            router.onActivityStarted(activity);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Activity activity = getActivity();
        if (activity != null) for (Router router : getRouters()) {
            router.onActivityResumed(activity);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        destroyed = false;

        if (!attached) {
            attached = true;

            for (int i = pendingPermissionRequests.size() - 1; i >= 0; i--) {
                PendingPermissionRequest request = pendingPermissionRequests.remove(i);
                requestPermissions(request.instanceId, request.permissions, request.requestCode);
            }
        }

        for (ActivityHostedRouter router : new ArrayList<>(routerMap.values())) {
            router.onContextAvailable();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        attached = false;
        if (getActivity() != null) {
            destroyRouters(getActivity().isChangingConfigurations());
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        prepareForHostDetachIfNeeded();

        localState.putParcelable(KEY_PERMISSION_REQUEST_CODES, new StringSparseArrayParceler(permissionRequestMap));
        localState.putParcelable(KEY_ACTIVITY_REQUEST_CODES, new StringSparseArrayParceler(activityRequestMap));
        localState.putParcelableArrayList(KEY_PENDING_PERMISSION_REQUESTS, pendingPermissionRequests);

        for (Router router : getRouters()) {
            Bundle bundle = new Bundle();
            router.saveInstanceState(bundle);
            localState.putBundle(KEY_ROUTER_STATE_PREFIX + router.getContainerId(), bundle);
        }

        outState.putBundle(KEY_LOCAL_STATE, localState);
    }

    @Override
    public void onPause() {
        super.onPause();

        Activity activity = getActivity();
        if (activity != null) for (Router router : getRouters()) {
            router.onActivityPaused(activity);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        prepareForHostDetachIfNeeded();

        Activity activity = getActivity();
        if (activity != null) for (Router router : getRouters()) {
            router.onActivityStopped(activity);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        destroyRouters(false);
        routerMap.clear();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String instanceId = activityRequestMap.get(requestCode);
        if (instanceId != null) {
            for (Router router : getRouters()) {
                router.onActivityResult(instanceId, requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        String instanceId = permissionRequestMap.get(requestCode);
        if (instanceId != null) {
            for (Router router : getRouters()) {
                router.onRequestPermissionsResult(instanceId, requestCode, permissions, grantResults);
            }
        }
    }

    @Override
    public boolean shouldShowRequestPermissionRationale(@NonNull String permission) {
        for (Router router : getRouters()) {
            Boolean handled = router.handleRequestedPermission(permission);
            if (handled != null) {
                return handled;
            }
        }

        return super.shouldShowRequestPermissionRationale(permission);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        for (Router router : getRouters()) {
            router.onCreateOptionsMenu(menu, inflater);
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        for (Router router : getRouters()) {
            router.onPrepareOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        for (Router router : getRouters()) {
            if (router.onOptionsItemSelected(item)) {
                return true;
            }
        }

        return false;
    }

    public void registerForActivityResult(@NonNull String instanceId, int requestCode) {
        activityRequestMap.put(requestCode, instanceId);
    }

    public void unregisterForActivityResults(@NonNull String instanceId) {
        for (int i = activityRequestMap.size() - 1; i >= 0; i--) {
            if (instanceId.equals(activityRequestMap.get(activityRequestMap.keyAt(i)))) {
                activityRequestMap.removeAt(i);
            }
        }
    }

    public void startActivityForResult(@NonNull String instanceId, @NonNull Intent intent, int requestCode) {
        registerForActivityResult(instanceId, requestCode);
        startActivityForResult(intent, requestCode);
    }

    public void startActivityForResult(@NonNull String instanceId, @NonNull Intent intent, int requestCode, @Nullable Bundle options) {
        registerForActivityResult(instanceId, requestCode);
        startActivityForResult(intent, requestCode, options);
    }

    @TargetApi(Build.VERSION_CODES.N)
    public void startIntentSenderForResult(
            @NonNull String instanceId, @NonNull IntentSender intent, int requestCode,
            @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags,
            @Nullable Bundle options
    ) throws IntentSender.SendIntentException {
        registerForActivityResult(instanceId, requestCode);
        startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void requestPermissions(@NonNull String instanceId, @NonNull String[] permissions, int requestCode) {
        if (attached) {
            permissionRequestMap.put(requestCode, instanceId);
            requestPermissions(permissions, requestCode);
        } else {
            pendingPermissionRequests.add(new PendingPermissionRequest(instanceId, permissions, requestCode));
        }
    }

    public void invalidateOptionsMenu() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    private void destroyRouters(boolean configurationChange) {
        if (!destroyed) {
            destroyed = true;

            Activity activity = getActivity();
            if (activity != null) {
                for (Router router : getRouters()) {
                    router.onActivityDestroyed(activity, configurationChange);
                }
            }
        }
    }

    private void prepareForHostDetachIfNeeded() {
        if (!hasPreparedForHostDetach) {
            hasPreparedForHostDetach = true;

            for (Router router : getRouters()) {
                router.prepareForHostDetach();
            }
        }
    }

    private static class PendingPermissionRequest implements Parcelable {
        @NonNull
        final String instanceId;
        @NonNull
        final String[] permissions;
        final int requestCode;

        PendingPermissionRequest(@NonNull String instanceId, @NonNull String[] permissions, int requestCode) {
            this.instanceId = instanceId;
            this.permissions = permissions;
            this.requestCode = requestCode;
        }

        PendingPermissionRequest(Parcel in) {
            instanceId = in.readString();
            permissions = in.createStringArray();
            requestCode = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeString(instanceId);
            out.writeStringArray(permissions);
            out.writeInt(requestCode);
        }

        public static final Parcelable.Creator<PendingPermissionRequest> CREATOR = new Parcelable.Creator<PendingPermissionRequest>() {
            @Override
            public PendingPermissionRequest createFromParcel(Parcel in) {
                return new PendingPermissionRequest(in);
            }

            @Override
            public PendingPermissionRequest[] newArray(int size) {
                return new PendingPermissionRequest[size];
            }
        };

    }
}
