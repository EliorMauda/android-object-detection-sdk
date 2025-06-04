package com.objectdetection.sdk.model;

import android.graphics.RectF;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the bounding box coordinates of a detected object in an image.
 * The coordinates are normalized values between 0.0 and 1.0, making them
 * resolution-independent and suitable for scaling to any target image size.
 *
 * <p>This class follows the standard computer vision convention where:
 * <ul>
 *   <li>Origin (0,0) is at the top-left corner of the image</li>
 *   <li>X-axis increases from left to right</li>
 *   <li>Y-axis increases from top to bottom</li>
 *   <li>All coordinates are normalized to [0.0, 1.0] range</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * BoundingBox box = new BoundingBox();
 * box.setXMin(0.1f);  // 10% from left edge
 * box.setYMin(0.2f);  // 20% from top edge
 * box.setXMax(0.6f);  // 60% from left edge
 * box.setYMax(0.8f);  // 80% from top edge
 *
 * // Convert to pixel coordinates for a 1920x1080 image
 * RectF pixelRect = box.toRectF(1920, 1080);
 * }</pre>
 *
 * @author Object Detection SDK
 * @version 1.0
 * @since 1.0
 */
public class BoundingBox {
    @SerializedName("xmin")
    private Float xMin;

    @SerializedName("ymin")
    private Float yMin;

    @SerializedName("xmax")
    private Float xMax;

    @SerializedName("ymax")
    private Float yMax;

    /**
     * Default constructor that creates an empty bounding box.
     * All coordinate values will be initialized to null.
     *
     * <p>After creation, you must set all coordinate values using the setter methods
     * before the bounding box can be considered valid.</p>
     */
    public BoundingBox() {
    }

    /**
     * Gets the minimum X coordinate (left edge) of the bounding box.
     *
     * @return the normalized X coordinate of the left edge, or null if not set
     * @see #setXMin(Float)
     */
    public Float getXMin() {
        return xMin;
    }

    /**
     * Sets the minimum X coordinate (left edge) of the bounding box.
     *
     * @param xMin the normalized X coordinate of the left edge (should be between 0.0 and 1.0)
     * @throws IllegalArgumentException if xMin is outside the valid range [0.0, 1.0]
     * @see #getXMin()
     */
    public void setXMin(Float xMin) {
        this.xMin = xMin;
    }

    /**
     * Gets the minimum Y coordinate (top edge) of the bounding box.
     *
     * @return the normalized Y coordinate of the top edge, or null if not set
     * @see #setYMin(Float)
     */
    public Float getYMin() {
        return yMin;
    }

    /**
     * Sets the minimum Y coordinate (top edge) of the bounding box.
     *
     * @param yMin the normalized Y coordinate of the top edge (should be between 0.0 and 1.0)
     * @throws IllegalArgumentException if yMin is outside the valid range [0.0, 1.0]
     * @see #getYMin()
     */
    public void setYMin(Float yMin) {
        this.yMin = yMin;
    }

    /**
     * Gets the maximum X coordinate (right edge) of the bounding box.
     *
     * @return the normalized X coordinate of the right edge, or null if not set
     * @see #setXMax(Float)
     */
    public Float getXMax() {
        return xMax;
    }

    /**
     * Sets the maximum X coordinate (right edge) of the bounding box.
     *
     * @param xMax the normalized X coordinate of the right edge (should be between 0.0 and 1.0)
     * @throws IllegalArgumentException if xMax is outside the valid range [0.0, 1.0]
     * @see #getXMax()
     */
    public void setXMax(Float xMax) {
        this.xMax = xMax;
    }

    /**
     * Gets the maximum Y coordinate (bottom edge) of the bounding box.
     *
     * @return the normalized Y coordinate of the bottom edge, or null if not set
     * @see #setYMax(Float)
     */
    public Float getYMax() {
        return yMax;
    }

    /**
     * Sets the maximum Y coordinate (bottom edge) of the bounding box.
     *
     * @param yMax the normalized Y coordinate of the bottom edge (should be between 0.0 and 1.0)
     * @throws IllegalArgumentException if yMax is outside the valid range [0.0, 1.0]
     * @see #getYMax()
     */
    public void setYMax(Float yMax) {
        this.yMax = yMax;
    }

    /**
     * Converts the normalized bounding box to an Android RectF object
     * that can be used with Android's drawing functions.
     *
     * <p>This method scales the normalized coordinates [0.0, 1.0] to pixel coordinates
     * based on the provided image dimensions. The resulting RectF can be used directly
     * with Canvas drawing operations or other Android graphics APIs.</p>
     *
     * <p>Example usage:
     * <pre>{@code
     * BoundingBox box = detectedObject.getBox();
     * RectF drawRect = box.toRectF(imageView.getWidth(), imageView.getHeight());
     * canvas.drawRect(drawRect, paint);
     * }</pre>
     *
     * @param imageWidth Width of the source image in pixels
     * @param imageHeight Height of the source image in pixels
     * @return RectF representation of the bounding box in pixel coordinates
     * @throws IllegalArgumentException if imageWidth or imageHeight are <= 0
     * @throws IllegalStateException if any coordinate values are null
     * @see android.graphics.RectF
     * @see #isValid()
     */
    public RectF toRectF(int imageWidth, int imageHeight) {
        return new RectF(
                xMin * imageWidth,
                yMin * imageHeight,
                xMax * imageWidth,
                yMax * imageHeight
        );
    }

    /**
     * Checks if this bounding box has valid coordinate values.
     *
     * <p>A bounding box is considered valid if all four coordinate values
     * (xMin, yMin, xMax, yMax) are non-null. This method does not validate
     * that the coordinates form a valid rectangle (e.g., xMin < xMax).</p>
     *
     * @return true if all coordinate values are non-null, false otherwise
     * @see #toRectF(int, int)
     */
    public boolean isValid() {
        return xMin != null && yMin != null && xMax != null && yMax != null;
    }

    /**
     * Returns a string representation of the bounding box coordinates.
     *
     * <p>The format includes all four coordinates for debugging and logging purposes.
     * Example output: "BoundingBox{xMin=0.1, yMin=0.2, xMax=0.6, yMax=0.8}"</p>
     *
     * @return a string representation of this bounding box
     */
    @Override
    public String toString() {
        return "BoundingBox{" +
                "xMin=" + xMin +
                ", yMin=" + yMin +
                ", xMax=" + xMax +
                ", yMax=" + yMax +
                '}';
    }
}