package nl.weijzen.maastricht.medewerkervandemaand;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

public class Controller2EditPicture extends AppCompatActivity  implements View.OnTouchListener {
    private ImageView view;
    private ImageView imageView2EditPicture;
    private ImageView imageViewMovingGadget;
    private EditText editTextImageTitle;
    private ConstraintLayout layoutMenuGadgetSelection;
    private ConstraintLayout layoutMenuSaveDialog;
    private MenuItem menuItemShare;
    private MenuItem menuItemSubmit;
    // these matrices will be used to move and zoom image
    private Matrix matrix = new Matrix();
    private final Matrix savedMatrix = new Matrix();
    private Matrix scaleMatrix = new Matrix();
    private int currentLayer = 0;
    private int mode = NONE;
    // remember some things for zooming
    private final PointF start = new PointF();
    private final PointF mid = new PointF();
    private float oldDist = 1f;
    private float d = 0f;
    private float[] lastEvent = null;
    // we can be in one of these 3 states
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;

    private CompoundPicture compoundPicture;

    // Overrides for state changes
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_2_edit_picture);

        layoutMenuGadgetSelection = findViewById(R.id.layout_menu_gadget_selection);
        layoutMenuSaveDialog = findViewById(R.id.layout_save_dialog);
        imageView2EditPicture = findViewById(R.id.imageView2EditPicture);
        imageViewMovingGadget = findViewById(R.id.imageView_moving_gadget);
        editTextImageTitle = findViewById(R.id.editTextImageTitle);

        CurrentYearAndMonth currentYearAndMonth = new CurrentYearAndMonth(this);
        @SuppressLint("DefaultLocale")
        String imageTitle = getString(R.string.filename_default) + "_" + currentYearAndMonth.getYear() + "_" + String.format("%02d",currentYearAndMonth.getMonth());
        editTextImageTitle.setText(imageTitle);

        Point displayDimensions = getDisplayDimensions();
        int canvasWidth = displayDimensions.x;

        imageViewMovingGadget.setOnTouchListener(this);

        compoundPicture = addPictureToCompoundPicture(canvasWidth);
        scaleMatrix = calculateScalingFactorToFitPictureOnDisplayInPortraitLayout(compoundPicture.getPicture().getBitmap());
        compoundPicture.setPictureMatrix(scaleMatrix);
        drawPictureOnCanvas(compoundPicture);
    }

    // onCreate ------------------------------------------------------------------------------------
    private Matrix calculateScalingFactorToFitPictureOnDisplayInPortraitLayout(Bitmap bitmap) {
        Point imageDimensions = new Point(bitmap.getWidth(), bitmap.getHeight());
        Point displayDimensions = getDisplayDimensions();
        float factorX = (float) displayDimensions.x/imageDimensions.x;
        float factorY = (float) displayDimensions.y/imageDimensions.y;
        if (factorX < factorY) {
            scaleMatrix.postScale(factorX, factorX);
        } else {
            scaleMatrix.postScale(factorY,factorY);
        }
        return scaleMatrix;
    }

    // onCreate - calculateScalingFactorToFitPictureOnDisplayInPortraitLayout ----------------------
    @NonNull
    private Point getDisplayDimensions() {
        Point displayDimensions = new Point();
        getWindowManager().getDefaultDisplay().getSize(displayDimensions);
        return displayDimensions;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // handle touch events here
        this.view = (ImageView) view;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                lastEvent = new float[4];
                lastEvent[0] = event.getX(0);
                lastEvent[1] = event.getX(1);
                lastEvent[2] = event.getY(0);
                lastEvent[3] = event.getY(1);
                d = rotation(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    float dx = event.getX() - start.x;
                    float dy = event.getY() - start.y;
                    matrix.postTranslate(dx, dy);
                } else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = (newDist / oldDist);
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                    if (lastEvent != null && event.getPointerCount() == 2 || event.getPointerCount() == 3) {
                        float newRot = rotation(event);
                        float r = newRot - d;
                        float[] values = new float[9];
                        matrix.getValues(values);
                        float tx = values[2];
                        float ty = values[5];
                        float sx = values[0];
                        float xc = (this.view.getWidth() / 2) * sx;
                        float yc = (this.view.getHeight() / 2) * sx;
                        matrix.postRotate(r, tx + xc, ty + yc);
                    }
                }
                break;
        }

        imageViewMovingGadget.setImageMatrix(matrix);

        Bitmap bmap = Bitmap.createBitmap(imageViewMovingGadget.getWidth(), imageViewMovingGadget.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bmap);
        imageViewMovingGadget.draw(canvas);

        return true;
    }

    /**
     * Determine the space between the first two fingers
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        float s=x * x + y * y;
        return (float)Math.sqrt(s);
    }

    /**
     * Calculate the mid point of the first two fingers
     */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    // ActionBar menu overrides
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        setMenuName();
        getMenuInflater().inflate(R.menu.menu_view_2_edit_picture, menu);
        menuItemShare = menu.findItem(R.id.menuitem_share_compound_picture);
        menuItemSubmit = menu.findItem(R.id.menu_item_submit_change);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu){
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.menuitem_save_compound_picture:
                layoutMenuGadgetSelection.setVisibility(View.GONE);
                layoutMenuSaveDialog.setVisibility(View.VISIBLE);
                menuItemSubmit.setVisible(false);
                return true;
            case R.id.menuitem_share_compound_picture:
                startController3SharePicture();
                return true;
            case R.id.menuitem_setup_gadgets:
                // checkedout OK
                layoutMenuGadgetSelection.setVisibility(View.VISIBLE);
                menuItemSubmit.setVisible(true);
                layoutMenuSaveDialog.setVisibility(View.GONE);
                return true;
            case R.id.menu_item_submit_change:
                // checkedout OK
                commitChangesToCurrentLayer();
                layoutMenuGadgetSelection.setVisibility(View.VISIBLE);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void ButtonClick(View view){
        this.view = (ImageView) view;

        int selectedButtonId = this.view.getId();
        switch (selectedButtonId){
            case R.id.imageButtonSaveCompoundPicture:
                    String imageTitle = editTextImageTitle.getText().toString();
                    if (validImageTitle(imageTitle)) {
                        saveCompoundPicture(imageTitle);
                        layoutMenuSaveDialog.setVisibility(View.GONE);
                        menuItemShare.setVisible(true);
                        hideSoftwareKeyboard();
                    } else {
                        displayToastMessage(R.string.warning_invalid_image_title);
                    }

                break;
            case R.id.imageButtonSmileScary:
                // checkedout OK
                setDrawableResourceForImageView(R.drawable.gadget_teeth_scary);
                hideMenuGadgetSelectionAndShare();
                break;
            case R.id.imageButtonMustache:
                // checkedout OK
                setDrawableResourceForImageView(R.drawable.gadget_mustache);
                hideMenuGadgetSelectionAndShare();
                break;
            case R.id.imageButtonBigNose:
                // checkedout OK
                setDrawableResourceForImageView(R.drawable.gadget_nose_big);
                hideMenuGadgetSelectionAndShare();
                break;
            case R.id.imageButtonDonkeyEars:
                // checkedout OK
                setDrawableResourceForImageView(R.drawable.gadget_donkey_ears);
                hideMenuGadgetSelectionAndShare();
                break;
            case R.id.imageButtonHat:
                // checkedout OK
                setDrawableResourceForImageView(R.drawable.gadget_hat);
                hideMenuGadgetSelectionAndShare();
                break;
            case R.id.imageButtonButterflies:
                // checkedout OK
                setDrawableResourceForImageView(R.drawable.gadget_butterflies);
                hideMenuGadgetSelectionAndShare();
                break;
            case R.id.imageButtonGadgetNext:
                // checkedout OK
                if (compoundPicture.getNumberOfGadgets() > currentLayer + 1){
                    ++currentLayer;
                    displayToastMessage(R.string.toast_gadget_next_selected);
                } else {
                    displayToastMessage(R.string.toast_gadget_last);
                }
                break;
            case R.id.imageButtonGadgetPrevious:
                // checkedout OK
                if (currentLayer > 0){
                    --currentLayer;
                    displayToastMessage(R.string.toast_gadget_previous_selected);
                } else {
                    displayToastMessage(R.string.toast_gadget_last);
                }
                break;
            case R.id.imageButtonLayerToTop:
                // checkedout OK
                boolean gadgetMovedOneLayerUp = compoundPicture.moveGadgetOneLayerToTop(currentLayer);
                if (gadgetMovedOneLayerUp){
                    Bitmap newCompoundPictureBitmap = compoundPicture.getCompoundPictureBitmap();
                    imageView2EditPicture.setImageBitmap(newCompoundPictureBitmap);
                    ++currentLayer;
                    displayToastMessage(compoundPicture.getGadgets().get(currentLayer).getName());
                } else {
                    displayToastMessage(R.string.toast_gadget_failed_to_move);
                }
                break;
            case R.id.imageButtonLayerToBottom:
                // checkedout OK
                boolean gadgetMovedOneLayerDown = compoundPicture.moveGadgetOneLayerToBottom(currentLayer);
                if (gadgetMovedOneLayerDown){
                    Bitmap newCompoundPictureBitmap = compoundPicture.getCompoundPictureBitmap();
                    imageView2EditPicture.setImageBitmap(newCompoundPictureBitmap);
                    --currentLayer;
                    displayToastMessage(R.string.toast_gadget_moved_down);
                } else {
                    displayToastMessage(R.string.toast_gadget_failed_to_move);
                }
                break;
            case R.id.imageButtonSubmitChange:
                // checkedout OK
                commitChangesToCurrentLayer();
                break;
            case R.id.imageButtonHideMenu:
                // checkedout OK
                layoutMenuGadgetSelection.setVisibility(View.GONE);
                break;
            case R.id.imageButtonDeleteGadget:
                // checkedout OK
                boolean layerRemoved = compoundPicture.removeGadget(currentLayer);
                if (layerRemoved){
                    Bitmap newCompoundPictureBitmap = compoundPicture.getCompoundPictureBitmap();
                    imageView2EditPicture.setImageBitmap(newCompoundPictureBitmap);
                    displayToastMessage(R.string.toast_gadget_deleted);
                    if (currentLayer > 0) {
                        --currentLayer;
                    }
                }
                break;
        }
    }

    // ButtonClick ---------------------------------------------------------------------------------
    private void hideSoftwareKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager)this.getSystemService(Service.INPUT_METHOD_SERVICE);
        Objects.requireNonNull(inputMethodManager).hideSoftInputFromWindow(editTextImageTitle.getWindowToken(),0);
    }

    // ButtonClick ---------------------------------------------------------------------------------
    private void displayToastMessage(int resourceStringToastText) {
        String toastMessage = getResourceString(resourceStringToastText);
        Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_SHORT).show();
    }
    private void displayToastMessage(String gadgetName) {
        String toastMessage = String.format(getResourceString(R.string.toast_gadget_moved_up),gadgetName);
        Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_SHORT).show();
    }

    // ButtonClick ---------------------------------------------------------------------------------
    private void hideMenuGadgetSelectionAndShare() {
        layoutMenuGadgetSelection.setVisibility(View.GONE);
        menuItemShare.setVisible(false);
    }

    // onOptionsItemSelected -----------------------------------------------------------------------
    // buttonClick ---------------------------------------------------------------------------------
    private void commitChangesToCurrentLayer() {
        // checkedout OK
        if (compoundPicture.getNumberOfGadgets() != 0) {
            Matrix gadgetMatrix = new CreateMatrixCloneOMovingGadgetMatrixForCompoundGadget().invoke();
            compoundPicture.getGadgets().get(currentLayer).setMatrix(gadgetMatrix);
            Bitmap newCompoundPictureBitmap = compoundPicture.getCompoundPictureBitmap();
            imageView2EditPicture.setImageBitmap(newCompoundPictureBitmap);
            imageViewMovingGadget.setImageResource(R.drawable.empty_gadget);
            displayToastMessage(R.string.toast_gadget_submitted);
        } else {
            displayToastMessage(R.string.warning_nothing_to_submit);
        }
    }

    // onOptionsItemSelected -----------------------------------------------------------------------
    private void setDrawableResourceForImageView(int drawableResourceId) {
        // checkedout OK
        String resourceName = getResourceNameFromResourceId(drawableResourceId);
        matrix = imageView2EditPicture.getImageMatrix();
        sortGadgedsInCompoundPictureByLayer();
        imageViewMovingGadget.setImageMatrix(matrix);
        imageViewMovingGadget.setImageResource(drawableResourceId);
        imageViewMovingGadget.setTag(drawableResourceId);
        Matrix gadgetMatrix = imageViewMovingGadget.getImageMatrix();
        compoundPicture.addGadget(new CompoundGadget(getApplicationContext(), drawableResourceId, resourceName, gadgetMatrix));
        int lastGadget = compoundPicture.getNumberOfGadgets() - 1;
        currentLayer = compoundPicture.getGadgets().get(lastGadget).getLayer();
    }

    // setDrawableResourceForImageView (onOptionsItemSelected) -------------------------------------
    private String getResourceNameFromResourceId(int drawableResourceId) {
        // checkedout OK
        Resources resources = getResources();
        return resources.getResourceName(drawableResourceId);
    }

    // setDrawableResourceForImageView (onOptionsItemSelected) -------------------------------------
    private void sortGadgedsInCompoundPictureByLayer() {
        // checkedout OK
        Collections.sort(compoundPicture.getGadgets(), new Comparator<CompoundGadget>() {
            @Override
            public int compare(CompoundGadget o1, CompoundGadget o2) {
                return o1.getLayer().compareTo(o2.getLayer());
            }
        });
    }

    // onCreateOptionsMenu -------------------------------------------------------------------------
    private void setMenuName(){
        setTitle(getResourceString(R.string.menu_title_view_2_edit_picture));
    }

    // onCreateOptionsMenu -------------------------------------------------------------------------
    private void drawPictureOnCanvas(CompoundPicture compoundPicture) {
        if (compoundPicture.getPicture() != null){
            imageView2EditPicture.setImageBitmap(compoundPicture.getCompoundPictureBitmap());
            setImageViewMovingGadgetSizeToMatchImageView2EditPictureSize();
        }
    }

    // drawPictureOnCanvas (onCreateOptionsMenu) ---------------------------------------------------
    private void setImageViewMovingGadgetSizeToMatchImageView2EditPictureSize() {
        imageViewMovingGadget.layout(
                imageView2EditPicture.getLeft(),
                imageView2EditPicture.getTop(),
                imageView2EditPicture.getRight(),
                imageView2EditPicture.getBottom());
    }

    // onCreateOptionsMenu -------------------------------------------------------------------------
    private CompoundPicture addPictureToCompoundPicture(int canvasWidth) {
        Picture picture = loadPictureFromUriTextInIntent();
        return new CompoundPicture(this, picture, canvasWidth);
    }

    // addPictureToCompoundPicture (onCreateOptionsMenu) -------------------------------------------
    private Picture loadPictureFromUriTextInIntent() {
        Intent intent = getIntent();
        String pictureUriText = intent.getStringExtra("PICTURE_URI_TEXT");
        if (pictureUriText != null) {
            return new Picture(this, pictureUriText);
        }
        return null;
    }

    // setMenuName (onCreateOptionsMenu) -----------------------------------------------------------
    private String getResourceString(int RecourceId) {
        Resources resources = getResources();
        return resources.getString(RecourceId);
    }
    // ButtonClick ---------------------------------------------------------------------------------
    private void saveCompoundPicture(String imageTitle) {
        ImageView imageView = findViewById(R.id.imageView2EditPicture);
        Bitmap    bitmap    = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        compoundPicture.setCompoundPictureUri(SaveCapturedImageToInternalStorage(imageTitle, bitmap));
    }

    // ButtonClick ---------------------------------------------------------------------------------
    private boolean validImageTitle(String imageTitle){
        return Pattern.matches("[\\w\\s.]+", imageTitle);
    }

    // saveCompoundPicture (ButtonClick) -----------------------------------------------------------
    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    private Uri SaveCapturedImageToInternalStorage(String imageTitle, Bitmap bitmap) {
        ContentResolver contentResolver  = getContentResolver();
        Uri outputFileUri = null;
        if (imageTitle != null && bitmap != null) {
            ContentValues values = new ContentValues();
            values.put(Images.Media.TITLE, imageTitle);
            values.put(Images.Media.DISPLAY_NAME, imageTitle);
            values.put(Images.Media.DESCRIPTION, getString(R.string.app_name));
            values.put(Images.Media.MIME_TYPE,"image/jpeg");
            values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(Images.Media.DATE_ADDED, (int) (System.currentTimeMillis() / 1000));

            OutputStream outputStream = null;
            try {
                outputFileUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                outputStream = contentResolver.openOutputStream(Objects.requireNonNull(outputFileUri));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

                Toast.makeText(getApplicationContext(),"Saved: " + outputFileUri.toString(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    Objects.requireNonNull(outputStream).close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        scanFileToMakeItAppearOnTheGallery(outputFileUri);
        return outputFileUri;
    }



    // SaveCapturedImageToInternalStorage (ButtonClick/saveCompoundPicture) ------------------------
    private void scanFileToMakeItAppearOnTheGallery(Uri outputFileUri) {
        String absoluteFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + File.separator + "Camera";
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(outputFileUri);
        this.sendBroadcast(mediaScanIntent);
        MediaScannerConnection.scanFile(Controller2EditPicture.this,
            new String[]{absoluteFilePath}, null,
            new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                }
            }
        );
    }

    // commitChangesToCurrentLayer -----------------------------------------------------------------
    private class CreateMatrixCloneOMovingGadgetMatrixForCompoundGadget {
        Matrix invoke() {
            return new Matrix(imageViewMovingGadget.getImageMatrix());
        }
    }

    // onOptionsItemSelected -----------------------------------------------------------------------
    private void startController3SharePicture(){
        Intent startController3SharePicture = new Intent(getApplicationContext(), Controller3SharePicture.class);
        startController3SharePicture.putExtra("COMPOUNDPICTURE_URI_TEXT", compoundPicture.getCompoundPictureUri().toString());

        startActivity(startController3SharePicture);
    }
}
