package nl.weijzen.maastricht.medewerkervandemaand;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class Controller1SelectPicture extends AppCompatActivity {
    private final ArrayList<String> permissions = new ArrayList<>();
    private Menu menu;
    private Picture picture;

    private final static int ALL_PERMISSIONS_RESULT = 107;

    // Overrides for state changes
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_1_select_picture);

        requestPermissionsForCamera();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (picture != null) {
            bundle.putString("PICTURE_URI_TEXT", picture.getPictureUri().toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        String pictureUriText = bundle.getString("PICTURE_URI_TEXT");
        if (pictureUriText != null) {
            picture = new Picture(this, pictureUriText);
            showPictureInImageView();
        }
    }

    // ActionBar menu overrides
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        setMenuName();
        getMenuInflater().inflate(R.menu.menu_view_1_select_picture, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu){
        this.menu = menu;
        if(picture != null){
            DisplayMenuButton();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.menu_button_camera:
                startActivityForResult(getAvailableFileSelectors(item.getItemId()), 200);
                return true;
            case R.id.menu_button_gallery:
                startActivityForResult(getAvailableFileSelectors(item.getItemId()), 200);
                return true;
            case R.id.menu_button_edit:
                startController2EditPicture();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri picUri = getPickImageResultUri(data);
            if (picUri != null) {
                picture = new Picture(this, picUri);
                showPictureInImageView();
                DisplayMenuButton();
            } else {
                Bitmap noInput = ((BitmapDrawable) Objects.requireNonNull(getDrawable(R.drawable.ic_launcher_background))).getBitmap();
                showPictureInImageView(noInput);
            }
        }
    }

    // onCreate ------------------------------------------------------------------------------------
    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissionsForCamera() {
        permissions.add(CAMERA);
        permissions.add(READ_EXTERNAL_STORAGE);
        permissions.add(WRITE_EXTERNAL_STORAGE);
        ArrayList<String> permissionsToRequest = findUnAskedPermissions(permissions);
        if (checkSdkTargetApiVersionIsAtleasetMarshmallow()) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT
                );
            }
        }
    }

    // requestPermissionsForCamera (onCreate) ------------------------------------------------------
    private ArrayList<String> findUnAskedPermissions(ArrayList<String> permissionsNeeded) {
        ArrayList<String> result = new ArrayList<>();
        for (String permission : permissionsNeeded) {
            if (!hasPermission(permission)) {
                result.add(permission);
            }
        }
        return result;
    }

    // findUnAskedPermissions (onCreate/RequestPermissionsForCamera) -------------------------------
    @TargetApi(Build.VERSION_CODES.M)
    private boolean hasPermission(String permission) {
        return !checkSdkTargetApiVersionIsAtleasetMarshmallow() || (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
    }

    // hasPermission (onCreate/RequestPermissionsForCamera/findUnAskedPermissions) -----------------
    private boolean checkSdkTargetApiVersionIsAtleasetMarshmallow() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }


    // onCreateOptionsMenu -------------------------------------------------------------------------
    private void setMenuName(){
        setTitle(getResources().getString(R.string.menu_title_view_1_select_picture));
    }

    // onPrepareOptionsMenu ------------------------------------------------------------------------
    private void DisplayMenuButton() {
        MenuItem menuButtonUsePicture = menu.findItem(R.id.menu_button_edit);
        menuButtonUsePicture.setVisible(true);
    }

    // onOptionsItemSelected -----------------------------------------------------------------------
    private Intent getAvailableFileSelectors(Integer menuItemSelected){
        List<Intent> allIntents = getIntentsToChoose(menuItemSelected);
        allIntents = removefallbackIntentFromAllIntentsList(allIntents);
        Intent mainIntent = extractMainIntentFromAllIntentsList(allIntents);
        Intent chooserIntent = Intent.createChooser(mainIntent, "Select source");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));
        return chooserIntent;
    }

    // getAvailableFileSelectors (onOptionsItemSelected) -------------------------------------------
    private List<Intent> getIntentsToChoose(int menuItemId) {
        Uri outputFileUri = getCaptureImageOutputUri();
        List<Intent> allIntents = null;
        if (menuItemId == R.id.menu_button_gallery) {
            allIntents = addAllGalleryIntentsToAllIntentsList();
        } else if (menuItemId == R.id.menu_button_camera) {
            allIntents = addAllCameraIntentsToAllIntentsList(outputFileUri);
        }
        return allIntents;
    }

    // getIntentsToChoose (onOptionsItemSelected/getAvailableFileSelectors) ------------------------
    private List<Intent> addAllGalleryIntentsToAllIntentsList() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        List<Intent> allIntents = new ArrayList<>();
        for (ResolveInfo res : listGallery) {
            Intent intent = buildIntent(galleryIntent, res);
            allIntents.add(intent);
        }
        return allIntents;
    }

    // getIntentsToChoose (onOptionsItemSelected/getAvailableFileSelectors) ------------------------
    private List<Intent> addAllCameraIntentsToAllIntentsList(Uri outputFileUri) {
        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = getPackageManager();
        Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            Intent intent = buildIntent(captureIntent, res);
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            }
            allIntents.add(intent);
        }
        return allIntents;
    }

    // getAvailableFileSelectors (onOptionsItemSelected) -------------------------------------------
    private List<Intent> removefallbackIntentFromAllIntentsList(List<Intent> allIntents) {
        Intent fallbackIntent = null;
        for (Intent intent : allIntents) {
            if (Objects.requireNonNull(intent.getComponent()).getClassName().equals("com.android.fallback.Fallback")) {
                fallbackIntent = intent;
            }
        }
        allIntents.remove(fallbackIntent);
        return allIntents;
    }

    // getAvailableFileSelectors (onOptionsItemSelected) -------------------------------------------
    private Intent extractMainIntentFromAllIntentsList(List<Intent> allIntents) {
        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (Objects.requireNonNull(intent.getComponent()).getClassName().equals("com.android.documentsui.DocumentsActivity")) {
                mainIntent = intent;
            }
        }
        allIntents.remove(mainIntent);
        return mainIntent;
    }

    // addAllCameraIntentsToAllIntentsList (onOptionsItemSelected/getAvailableFileSelectors/getIntentsToChoose) ----
    private Intent buildIntent(Intent targetIntent, ResolveInfo resolveInfo) {
        Intent intent = new Intent(targetIntent);
        intent.setComponent(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
        intent.setPackage(resolveInfo.activityInfo.packageName);
        return intent;
    }

    // onOptionsItemSelected -----------------------------------------------------------------------
    private void startController2EditPicture(){
        Intent startController2EditPicture = new Intent(getApplicationContext(), Controller2EditPicture.class);
        startController2EditPicture.putExtra("PICTURE_URI_TEXT", picture.getPictureUri().toString());
        startActivity(startController2EditPicture);
    }

    // onActivityResult ----------------------------------------------------------------------------
    private Uri getPickImageResultUri(Intent data) {
        boolean isCamera = true;
        if (data != null && data.getData() != null) {
            String action = data.getAction();
            isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
        }
        if (isCamera){
            return getCaptureImageOutputUri();
        }
        return Uri.parse(data.getData().toString());
    }
    // getPickImageResultUri (onActivityResult) ----------------------------------------------------
    private Uri getCaptureImageOutputUri() {
        Resources resource = getResources();
        String cacheFilename = resource.getString(R.string.filename_cache);

        File internalStoragePath = getExternalCacheDir();
        return SaveCapturedImageToInternalStorage(cacheFilename, internalStoragePath);
    }

    // getCaptureImageOutputUri (onActivityResult/getPickImageResultUri) ---------------------------
    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    private Uri SaveCapturedImageToInternalStorage(String fileName, File internalStoragePath) {
        Uri outputFileUri = null;
        if (internalStoragePath != null) {
            outputFileUri = FileProvider.getUriForFile(Controller1SelectPicture.this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    new File(internalStoragePath.getPath(), fileName));
        }
        return outputFileUri;
    }

    // onActivityResult ----------------------------------------------------------------------------
    private void showPictureInImageView() {
        ImageView imageViewSelectPicture = findViewById(R.id.imageViewSelectedPicture);
        imageViewSelectPicture.setImageBitmap(picture.getBitmap());
    }

    // onActivityResult ----------------------------------------------------------------------------
    private void showPictureInImageView(Bitmap bitMap) {
        ImageView imageViewSelectPicture = findViewById(R.id.imageViewSelectedPicture);
        imageViewSelectPicture.setImageBitmap(bitMap);
    }
}
