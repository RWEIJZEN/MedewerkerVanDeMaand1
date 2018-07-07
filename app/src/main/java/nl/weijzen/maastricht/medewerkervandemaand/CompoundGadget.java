package nl.weijzen.maastricht.medewerkervandemaand;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

class CompoundGadget {
    private static Long numberOfCompoundGadgetsCreated = 0L;

    private final Context context;
    private Matrix matrix;
    private final int gadgetResourceId;
    private int layer;
    private final String name;

    // Constructors
    CompoundGadget(Context context, int gadgetResourceId, String name, Matrix matrix) {
        ++numberOfCompoundGadgetsCreated;
        this.context = context;
        this.gadgetResourceId = gadgetResourceId;
        this.name = name;
        this.matrix = matrix;
    }

    // Getters
    public Matrix getMatrix() {
        return this.matrix;
    }

    public Bitmap getGadgetBitmap() {
        return BitmapFactory.decodeResource(this.context.getResources(), this.gadgetResourceId);
    }

    public Integer getLayer() {
        return this.layer;
    }

    public String getName(){
        return this.name;
    }

    // Setters
    public void setMatrix(Matrix matrix) {
        this.matrix = matrix;
    }

    public void setLayer(Integer layer) {
        if(0 <= layer){
            this.layer = layer;
        } else {
            throw new InstantiationError("Layer must be greater than 0");
        }
    }

    // Services
    public Integer moveGadgetOneLayerToTop(Integer totalNumberOfGadgets){
        if (totalNumberOfGadgets > this.layer){
            ++this.layer;
        }
        return this.layer;
    }

    public Integer moveGadgetOneLayerToBottom(){
        if (0 < this.layer){
            --this.layer;
        }
        return this.layer;
    }
}