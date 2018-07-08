package nl.weijzen.maastricht.medewerkervandemaand;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

@SuppressWarnings("UnusedAssignment")
class CompoundPicture {
    private Context       context;
    private Picture       picture;
    private Bitmap        compoundPicture;
    private Uri           compoundPictureUri;
    private ArrayList<CompoundGadget> gadgets;
    private Matrix        pictureMatrix;
    private int           canvasWidth;
    private int           canvasHeight;
    private Bitmap.Config canvasConfig;

    // Constructor
    CompoundPicture(Context context, Picture picture, int canvasWidth) throws InstantiationError {
        if (null != picture) {
            this.context       = context;
            this.picture       = picture;
            this.canvasWidth   = canvasWidth;
            this.canvasHeight  = (int) (canvasWidth * picture.getBitmap().getHeight()/ (float) picture.getBitmap().getWidth());
            this.canvasConfig  = picture.getBitmap().getConfig();
            gadgets            = new ArrayList<>();
            compoundPicture    = createEmptyCanvasForCompoundPicture();
            compoundPictureUri = getCompoundPictureUri();
        } else {
            throw new InstantiationError("Picture cannot be empty");
        }
    }

    // CompoundPicture -----------------------------------------------------------------------------
    private Bitmap createEmptyCanvasForCompoundPicture(){
        return Bitmap.createBitmap(canvasWidth, canvasHeight, canvasConfig);
    }

    // Getters
    public Picture getPicture() {
        return picture;
    }

    public Uri getCompoundPictureUri() {
        return compoundPictureUri;
    }

    public ArrayList<CompoundGadget> getGadgets() {
        return gadgets;
    }

    // Setters
    public void setCompoundPictureUri(Uri compoundPictureUri) {
        this.compoundPictureUri = compoundPictureUri;
    }

    public void setPictureMatrix(Matrix pictureMatrix) {
        this.pictureMatrix = pictureMatrix;
    }

    // Services
    public void addGadget(CompoundGadget gadget){
        gadget.setLayer(gadgets.size());
        gadgets.add(gadget);
    }

    public boolean removeGadget(int layer){
        if(gadgets.size() > 0) {
            CompoundGadget gadgetToDelete = this.getGadgets().get(layer);
            gadgets.remove(gadgetToDelete);
            for (int i = 0; i < gadgets.size(); ++i) {
                gadgets.get(i).setLayer(i);
            }
            return true;
        }
        return false;
    }

    // Layer movement
    public boolean moveGadgetOneLayerToTop(int layer){
        if(layer < gadgets.size() - 1){
            CompoundGadget gadgetToMoveUp   = gadgets.get(layer);
            CompoundGadget gadgetToMoveDown = gadgets.get(layer + 1);
            if (moveGadgetsToThereNewLayers(gadgetToMoveUp, gadgetToMoveDown)){
                return true;
            }
            gadgetToMoveUp.setLayer(layer);
            gadgetToMoveDown.setLayer(layer + 1);
        }
        return false;
    }

    public boolean moveGadgetOneLayerToBottom(int layer){
        if(layer > 0){
            CompoundGadget gadgetToMoveUp   = gadgets.get(layer - 1);
            CompoundGadget gadgetToMoveDown = gadgets.get(layer);
            if (moveGadgetsToThereNewLayers(gadgetToMoveUp, gadgetToMoveDown)){
                return true;
            }
            gadgetToMoveUp.setLayer(layer - 1);
            gadgetToMoveDown.setLayer(layer);
        }
        return false;
    }

    public Bitmap getCompoundPictureBitmap() {
        Canvas canvas;
        if (this.getPicture() != null) {
            CurrentYearAndMonth currentYearAndMonth = new CurrentYearAndMonth(this.context);
            String currentYear  = currentYearAndMonth.getYear().toString();
            String currentMonth = currentYearAndMonth.getMonthText();

            canvas = createNewEmptyCanvasWithDimensionsAndScalingIdenticalToPicture(compoundPicture, picture, pictureMatrix);
            canvas = drawAllGadgetsOnCanvas(canvas, gadgets);
            canvas = drawFrameOnCanvas(canvas);
            canvas = drawTopLeftAndRightTextOnCanvas(canvas, currentYear, currentMonth);
            canvas = drawBottomCenterText(canvas, context.getString(R.string.frame_bottom_center_text) + " " + currentYearAndMonth.getMonthFullText());
        }
        return compoundPicture;
    }

    public int getNumberOfGadgets(){
        return this.gadgets.size();
    }

    // moveGadgetOneLayerToTop ---------------------------------------------------------------------
    // moveGadgetOneLayerToBottom ------------------------------------------------------------------
    private boolean moveGadgetsToThereNewLayers(CompoundGadget gadgetToMoveUp, CompoundGadget gadgetToMoveDown) {
        int layerOfTheCurrentBottomGadget = gadgetToMoveUp.getLayer();
        if (gadgetToMoveDown.moveGadgetOneLayerToBottom() == layerOfTheCurrentBottomGadget){
            if (gadgetToMoveUp.moveGadgetOneLayerToTop(gadgets.size()) == layerOfTheCurrentBottomGadget + 1){
                this.sortGadgedsByLayer();
                return true;
            }
        }
        return false;
    }

    // getCompoundPictureBitmap --------------------------------------------------------------------
    @NonNull
    private static Canvas createNewEmptyCanvasWithDimensionsAndScalingIdenticalToPicture(Bitmap compoundPicture, Picture picture, Matrix pictureMatrix) {
        Canvas canvas;
        canvas = new Canvas(compoundPicture);
        canvas.drawBitmap(picture.getBitmap(), pictureMatrix, null);
        return canvas;
    }

    // getCompoundPictureBitmap --------------------------------------------------------------------
    private static Canvas drawAllGadgetsOnCanvas(Canvas canvas, ArrayList<CompoundGadget> gadgets) {
        for (CompoundGadget gadget : gadgets) {
            Bitmap gadgetBitmap = gadget.getGadgetBitmap();
            if (null != gadgetBitmap) {
                canvas.drawBitmap(gadget.getGadgetBitmap(), gadget.getMatrix(),null);
            }
        }
        return canvas;
    }

    // getCompoundPictureBitmap --------------------------------------------------------------------
    private Canvas drawFrameOnCanvas(Canvas canvas) {
        Bitmap frameBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.frame);
        frameBitmap        = Bitmap.createScaledBitmap(frameBitmap, canvas.getWidth(), canvas.getHeight(),true);
        canvas.drawBitmap(frameBitmap, new Matrix(), null);
        return canvas;
    }

    // getCompoundPictureBitmap --------------------------------------------------------------------
    private Canvas drawTopLeftAndRightTextOnCanvas(Canvas canvas, String topRightText, String topLeftText) {
        float textTopHeightInPercentageOfScreenHeight = 3.3F;
        int textTopHeight               = (int) (this.canvasHeight * textTopHeightInPercentageOfScreenHeight / 100);
        float textTopPositionOnYaxisInPercentageOfScreenHeight = 7.0F;
        int textTopPositionOnYaxis      = (int) (this.canvasHeight * textTopPositionOnYaxisInPercentageOfScreenHeight / 100);
        float topTextPositionFormSidesInPercentageOfScreenWidth = 17.1F;
        float topTextPositionFormSides  = topTextPositionFormSidesInPercentageOfScreenWidth / 100;
        int textTopLeftPositionOnXaxis  = (int) (canvas.getWidth() *      topTextPositionFormSides  / 2);
        int textTopRightPositionOnXaxis = (int) (canvas.getWidth() * (2 - topTextPositionFormSides) / 2);
        Paint textToPaintTop = new Paint();
        textToPaintTop.setColor(this.context.getColor(R.color.colorTopText));
        textToPaintTop.setTextAlign(Paint.Align.CENTER);
        textToPaintTop.setTextSize(textTopHeight);
        canvas.drawText(topLeftText,textTopLeftPositionOnXaxis, textTopPositionOnYaxis, textToPaintTop);
        canvas.drawText(topRightText,textTopRightPositionOnXaxis, textTopPositionOnYaxis, textToPaintTop);
        return canvas;
    }

    // getCompoundPictureBitmap --------------------------------------------------------------------
    private Canvas drawBottomCenterText(Canvas canvas, String bottomCenterText) {
        float textBottomHeightInPercentageOfScreenHeight = 4.0F;
        int textBottomHeight = (int) (this.canvasHeight * textBottomHeightInPercentageOfScreenHeight / 100);
        int textBottomPositionOnXaxis = canvas.getWidth() / 2;
        float textBottomPositionOnYaxisInPercentageOfScreenHeight = 97.0F;
        int textBottomPositionOnYaxis = (int) (this.canvasHeight * textBottomPositionOnYaxisInPercentageOfScreenHeight / 100);
        Paint textToPaintBottom = new Paint();
        textToPaintBottom.setColor(this.context.getColor(R.color.colorBottomText));
        textToPaintBottom.setTextAlign(Paint.Align.CENTER);
        textToPaintBottom.setTextSize(textBottomHeight);
        //noinspection SuspiciousNameCombination
        canvas.drawText(bottomCenterText,textBottomPositionOnXaxis, textBottomPositionOnYaxis, textToPaintBottom);
        return canvas;
    }

    // moveGadgetsToThereNewLayers (moveGadgetOneLayerToTop) ---------------------------------------
    // moveGadgetsToThereNewLayers (moveGadgetOneLayerToBottom) ------------------------------------
    private void sortGadgedsByLayer() {
        Collections.sort(this.getGadgets(), new Comparator<CompoundGadget>() {
            @Override
            public int compare(CompoundGadget o1, CompoundGadget o2) {
                return o1.getLayer().compareTo(o2.getLayer());
            }
        });
    }
}
