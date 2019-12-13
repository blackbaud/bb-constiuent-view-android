package com.blackbaud.constitview;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.GridRowBuilder;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

public class MySliceProvider extends SliceProvider {

    SharedPreferences sharedPreferences;
    SharedPreferences constitRecordCache;

    @Override
    public boolean onCreateSliceProvider() {
        return true;
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        final String path = sliceUri.getPath();
        switch (path) {
            //Define the slice’s URI; I’m using ‘mainActivity’
            case "/mainActivity":
                return createSlice(sliceUri);
        }
        return null;
    }

    public Slice createSlice(Uri sliceUri) {
        Context context = getContext();

        if (context != null) {
            SliceAction activityAction = createActivityAction();

//            sharedPreferences = context.getSharedPreferences("TokenCache", Context.MODE_PRIVATE);
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            editor.putString("featureName", "Robert Hernandez");
//            editor.commit();
//
//            Intent intent = new Intent();
//            Uri uri = Uri.parse("https://run-main-activity.com?featureName=" + sharedPreferences.getString("featureName", "ABC"));
//            intent.setData(uri);
//            try {
//                activityAction.getAction().send(context, 0, intent);
//            } catch (PendingIntent.CanceledException e) {
//                e.printStackTrace();
//            }
//
//            constitRecordCache = context.getSharedPreferences("ConstitRecordCache", Context.MODE_PRIVATE);

            //Create the ListBuilder
            ListBuilder listBuilder = new ListBuilder(getContext(), sliceUri, ListBuilder.INFINITY);

            // Add a row
            listBuilder.addRow(new ListBuilder.RowBuilder()
                    .setTitle("Constituent Profile Quick View")
                    .setPrimaryAction(activityAction));

            // Image from assets
            AssetManager assetManager = context.getAssets();
            InputStream istr;
            Bitmap bitmap;
            try {
                istr = assetManager.open("ic_headshot-web.png");
                bitmap = BitmapFactory.decodeStream(istr);
                IconCompat profileImage = IconCompat.createWithBitmap(bitmap);

                // Add image using a grid
                listBuilder.addGridRow(new GridRowBuilder()
                        .addCell(new GridRowBuilder.CellBuilder()
                                .addTitleText("Robert Hernandez")
                                .addImage(profileImage, ListBuilder.LARGE_IMAGE))
                        .addCell( new GridRowBuilder.CellBuilder()
                                .addImage(IconCompat.createWithResource(context, R.drawable.ic_person_pin_circle_black_24dp),
                                        ListBuilder.SMALL_IMAGE)
                                .addText("410 17th Street")
                                .addText("Denver, CO 90202"))
                        .addCell( new GridRowBuilder.CellBuilder()
                                .addImage(IconCompat.createWithResource(context, R.drawable.ic_phone_black_24dp),
                                        ListBuilder.SMALL_IMAGE)
                                .addText("(843) 526-8957")));

            } catch (IOException e) {
                // handle exception
            }

            //Build the List
            return listBuilder.build();
        } else {
            return null;
        }
    }

    public SliceAction createActivityAction() {
        return SliceAction.create(
                PendingIntent.getActivity(
                        getContext(), 0, new Intent(getContext(), ConstitRecord.class), 0
                ),
                IconCompat.createWithResource(getContext(), R.drawable.ic_skywarningicon),
                ListBuilder.ICON_IMAGE,
                "Constit Record Activity"
        );
    }
}