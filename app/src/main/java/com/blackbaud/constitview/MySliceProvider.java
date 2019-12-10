package com.blackbaud.constitview;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import static androidx.core.graphics.drawable.IconCompat.createWithResource;

public class MySliceProvider extends SliceProvider {
    /**
     * Instantiate any required objects. Return true if the provider was successfully created,
     * false otherwise.
     */
    @Override
    public boolean onCreateSliceProvider() {
        return true;
    }

    /**
     * Converts URL to content URI (i.e. content://com.blackbaud.constitview...)
     */
    @Override
    @NonNull
    public Uri onMapIntentToUri(@Nullable Intent intent) {
        // Note: implementing this is only required if you plan on catching URL requests.
        // This is an example solution.
        Uri.Builder uriBuilder = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT);
        if (intent == null) return uriBuilder.build();
        Uri data = intent.getData();
        if (data != null && data.getPath() != null) {
            String path = data.getPath().replace("/", "");
            uriBuilder = uriBuilder.path(path);
        }
        Context context = getContext();
        if (context != null) {
            uriBuilder = uriBuilder.authority(context.getPackageName());
        }
        return uriBuilder.build();
    }

    /**
     * Construct the Slice and bind data if available.
     */
    public Slice onBindSlice(Uri sliceUri) {
        Context context = getContext();
        if (context == null) {
            return null;
        }
        SliceAction activityAction = createActivityAction();
        ListBuilder listBuilder = new ListBuilder(getContext(), sliceUri, ListBuilder.INFINITY);

        // Name
        listBuilder.addRow(new ListBuilder.RowBuilder()
                .setTitle("Robert Hernandez")
                .setPrimaryAction(activityAction));
        // Image
        listBuilder.addRow(new ListBuilder.RowBuilder()
                .setTitleItem(createWithResource(context, R.drawable.ic_skywarningicon),
                        ListBuilder.ICON_IMAGE));

        // Address
        listBuilder.addRow(new ListBuilder.RowBuilder()
                .setTitleItem(createWithResource(context, R.drawable.ic_skywarningicon),
                        ListBuilder.ICON_IMAGE)
                .setTitle("Address")
                .setPrimaryAction(activityAction));

        // Phone Number
        listBuilder.addRow(new ListBuilder.RowBuilder()
                .setTitleItem(createWithResource(context, R.drawable.ic_skywarningicon),
                        ListBuilder.ICON_IMAGE)
                .setTitle("Phone Number")
                .setPrimaryAction(activityAction));


        return listBuilder.build();
    }

    private SliceAction createActivityAction() {
        return SliceAction.create(
                PendingIntent.getActivity(
                        getContext(), 0, new Intent(getContext(), ConstitRecord.class), 0
                ),
                createWithResource(getContext(), R.drawable.ic_skywarningicon),
                ListBuilder.ICON_IMAGE,
                "Coupon App Main"
        );
    }

    /**
     * Slice has been pinned to external process. Subscribe to data source if necessary.
     */
    @Override
    public void onSlicePinned(Uri sliceUri) {
        // When data is received, call context.contentResolver.notifyChange(sliceUri, null) to
        // trigger MySliceProvider#onBindSlice(Uri) again.
    }

    /**
     * Unsubscribe from data source if necessary.
     */
    @Override
    public void onSliceUnpinned(Uri sliceUri) {
        // Remove any observers if necessary to avoid memory leaks.
    }
}
