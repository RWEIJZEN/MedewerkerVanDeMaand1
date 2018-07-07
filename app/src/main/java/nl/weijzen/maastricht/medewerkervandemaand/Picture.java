package nl.weijzen.maastricht.medewerkervandemaand;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.IOException;

public class Picture {
    private final Uri pictureUri;
    private final Bitmap bitmap;

    // Constructor 1
    Picture(Context context, Uri pictureUri) {
        this.pictureUri = pictureUri;
        this.bitmap = loadBitmapFromUri(context);
    }

    // Constructor 2
    Picture(Context context, String pictureUriText) {
        this.pictureUri = Uri.parse(pictureUriText);
        this.bitmap = loadBitmapFromUri(context);
    }

    // Constructor 1 - Picture ---------------------------------------------------------------------
    // Constructor 2 - Picture ---------------------------------------------------------------------
    private Bitmap loadBitmapFromUri(Context context) {
        Bitmap myBitmap = null;
        try {
            myBitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), pictureUri);
            myBitmap = rotateImageIfRequired(myBitmap, pictureUri);
//            myBitmap = getResizedBitmap(myBitmap, 1600);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myBitmap;
    }

    // Getters
    public Uri getPictureUri() {
        return pictureUri;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }


    // loadBitmapFromUri (Constructor 1) -----------------------------------------------------------
    private static Bitmap rotateImageIfRequired(Bitmap bitmap, Uri selectedImage) throws IOException {
        ExifInterface exifInterface = new ExifInterface(selectedImage.getPath());
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateBitmap(bitmap, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateBitmap(bitmap, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateBitmap(bitmap, 270);
            default:
                return bitmap;
        }
    }
    // rotateImageIfRequired (Constructor 1/loadBitmapFromUri) -------------------------------------
    private static Bitmap rotateBitmap(Bitmap bitmap, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        return rotatedImg;
    }
}
