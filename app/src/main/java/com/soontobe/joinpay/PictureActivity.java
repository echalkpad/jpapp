package com.soontobe.joinpay;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PictureActivity extends Activity implements AlertDialog.OnClickListener {

    // For logging
    private static final String TAG = "picture_activity";

    // The indexes of options that are show to the user
    private static final int TAKE_PHOTO = 0;
    private static final int CHOOSE_PHOTO = 1;
    private static final int CANCEL = 2;

    // View elements to display profile picture on
    private ImageView bigPic;
    private ImageView littlePic;

    // So we can enable or disable buttons
    private Button uploadButton;
    private Button chooseButton;

    // For acquiring images
    private IntentDispatcher mDispatcher;
    private File picture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);    // No Title Bar
        setContentView(R.layout.activity_picture);

        // Collect Views to display images
        bigPic = (ImageView) findViewById(R.id.profileBigImageView);
        littlePic = (ImageView) findViewById(R.id.profileThumbView);

        // Collect other UI elements
        chooseButton = (Button) findViewById(R.id.button_profile_select);
        uploadButton = (Button) findViewById(R.id.button_profile_upload);

        // Can't upload if there's no picture
        uploadButton.setEnabled(false);
        if(picture != null && picture.exists())
            uploadButton.setEnabled(true);

        // Create dispatcher to assign to future dialogs
        mDispatcher = new IntentDispatcher(this);

        Log.d(TAG, "Created picture activity");
    }

    /**
     * Presents a choice to the user for how they want to select their
     * profile picture.
     */
    private void showPictureDialog() {

        // Create the list of options for the user to select
        // based on indexes at the top of the class
        final CharSequence[] options = {
                getString(R.string.profile_take_photo),
                getString(R.string.profile_choose_photo),
                getString(R.string.cancel)
        };

        Log.d(TAG, "Building profile picture dialog");
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.profile_dialog_title));
        dialog.setItems(options, this);

        Log.d(TAG, "Showing dialog to user");
        dialog.show();
    }

    /**
     * Called when the user presses the "choose picture" button.  Starts the process
     * of letting the user select their profile picture.
     * @param view The View that was clicked.
     */
    public void onChoosePicture(View view) {
        Log.d(TAG, "\"" + getString(R.string.profile_choose_button) + "\" clicked");
        showPictureDialog();
    }

    /**
     * Called when the user presses the "upload picture" button.  Uploads the profile
     * picture to the user's profile.
     * @param view The View that was clicked.
     */
    public void onUploadPicture(View view) {
        Log.d(TAG, "\"" + getString(R.string.profile_upload_button) + "\" clicked");

        // Check that there is a photo to upload
        if(picture == null || !picture.exists()) {
            Log.e(TAG, "Upload was clicked without a valid image file: " + picture);
            return;
        }
        // TODO upload the photo
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IntentDispatcher.REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            // Make sure a file was saved for this request (sanity check)
            if(picture == null || !picture.exists()) {
                Log.e(TAG, "Take photo request did not save picture.");
                return;
            }

            // Display the taken image
            String path = picture.getAbsolutePath();
            Log.d(TAG, String.format("Capture image path: %s", path));
            bigPic.setImageBitmap(FileHelper.scaleImage(path, bigPic));
            littlePic.setImageBitmap(FileHelper.scaleImage(path, littlePic));

            // Activate the upload button
            uploadButton.setEnabled(true);

        } else if(requestCode == IntentDispatcher.REQUEST_CHOOSE_PHOTO && resultCode == RESULT_OK) {
            // Make sure there is a picture to get
            if(data.getData() == null) {
                Log.e(TAG, "Choose photo request did not yield a picture");
                return;
            }

            // Find and display the selected image
            Uri selectedImage = data.getData();
            picture = new File(FileHelper.getRealPathFromURI(this, selectedImage));
            Log.d(TAG, String.format("Selected image: %s", picture.getAbsolutePath()));
            bigPic.setImageBitmap(FileHelper.scaleImage(picture.getAbsolutePath(), bigPic));
            littlePic.setImageBitmap(FileHelper.scaleImage(picture.getAbsolutePath(), littlePic));

            // Activate the upload button
            uploadButton.setEnabled(true);

        } else {
            Log.e(TAG, "Did not get valid photo.  Displaying default image.");
            bigPic.setImageResource(R.drawable.profile_placeholder);
            littlePic.setImageResource(R.drawable.profile_placeholder);

            // Deactivate the upload button
            uploadButton.setEnabled(false);

            // Delete previous pictures
            picture = null;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Log.d(TAG, "Option selected");
        if (which == TAKE_PHOTO) {
            Log.d(TAG, "Take photo selected");
            picture = mDispatcher.dispatchTakePhoto();
        } else if (which == CHOOSE_PHOTO) {
            Log.d(TAG, "Choose photo selected");
            mDispatcher.dispatchChoosePhoto();
        } else if (which == CANCEL) {
            Log.d(TAG, "Closing dialog");
            dialog.dismiss();
        }
    }

    /**
     * This helper class dispatches intents related to creating a profile picture
     * for the user's account.
     */
    private class IntentDispatcher {

        // The codes we want to represent our requests
        public static final int REQUEST_TAKE_PHOTO = 101;
        public static final int REQUEST_CHOOSE_PHOTO = 202;

        // For logging
        private static final String TAG = "photo_dispatcher";

        // For broadcasting intents
        public Context mContext;

        /**
         * Constructs a new IntentDispatcher with the given context.
         *
         * @param context The context through which intents should be broadcast.
         */
        public IntentDispatcher(Context context) {
            mContext = context;
        }

        /**
         * Dispatches an intent to choose a photo from the device's gallery.
         */
        public void dispatchChoosePhoto() {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_CHOOSE_PHOTO);
        }

        /**
         * Dispatches an intent to take a photo with the device's camera.
         */
        public File dispatchTakePhoto() {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // Verify that an activity exists to handle taking a photo
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                Log.d(TAG, "Found activity to take picture, creating file");

                // Create a file to store the photo in
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to create picture file");
                }

                // Can't proceed without a file
                if (photoFile != null) {
                    // Give intent a file to write full image to
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                    Log.d(TAG, "Dispatching intent to take photo");
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                    return photoFile;
                }
            } else {
                Log.e(TAG, "No activity available to take picture");
            }
            return null;
        }

        /**
         * Creates a files to store an image in.
         *
         * @return The File handle that was created.
         * @throws IOException Thrown if the File cannot be created.
         */
        public File createImageFile() throws IOException {
            // Create an image file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );

            galleryAddPic(image.getAbsolutePath());

            return image;
        }

        /**
         * Adds the given photo to the gallery so that it is available in the Android
         * Gallery application and other apps.
         *
         * @param filepath The path of the image to add.
         */
        private void galleryAddPic(String filepath) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            try {
                File f = new File(filepath);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                mContext.sendBroadcast(mediaScanIntent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to scan in photo: " + e.getMessage());
            }
        }
    }
}
