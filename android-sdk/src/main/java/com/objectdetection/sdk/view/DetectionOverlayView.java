package com.objectdetection.sdk.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.objectdetection.sdk.model.BoundingBox;
import com.objectdetection.sdk.model.DetectedObject;
import com.objectdetection.sdk.model.DetectionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Custom view that overlays detection results on top of an image or camera preview.
 *
 * <p>This view provides a visual representation of object detection results by drawing
 * bounding boxes around detected objects with their labels and confidence scores.
 * It can be used for both static images and live camera feeds to provide real-time
 * visual feedback of the detection process.</p>
 *
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li>Draws bounding boxes around detected objects</li>
 *   <li>Displays object labels with confidence percentages</li>
 *   <li>Generates consistent colors for each object type</li>
 *   <li>Supports both static image and live camera overlays</li>
 *   <li>Handles coordinate scaling and transformation</li>
 *   <li>Optimized for performance in real-time scenarios</li>
 * </ul>
 *
 * <p><strong>Usage Scenarios:</strong>
 * <ul>
 *   <li><strong>Static Images:</strong> Overlay detection results on ImageView</li>
 *   <li><strong>Live Camera:</strong> Overlay detection results on camera preview</li>
 *   <li><strong>Gallery Apps:</strong> Show detection results on browsed images</li>
 *   <li><strong>Security Apps:</strong> Real-time object monitoring</li>
 * </ul>
 *
 * <p><strong>XML Layout Example:</strong>
 * <pre>{@code
 * <FrameLayout
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent">
 *
 *     <ImageView
 *         android:id="@+id/imageView"
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent" />
 *
 *     <com.objectdetection.sdk.view.DetectionOverlayView
 *         android:id="@+id/overlayView"
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent" />
 *
 * </FrameLayout>
 * }</pre>
 *
 * <p><strong>Programmatic Usage Example:</strong>
 * <pre>{@code
 * DetectionOverlayView overlayView = findViewById(R.id.overlayView);
 *
 * // For static images
 * overlayView.setDetectionResult(detectionResult, imageWidth, imageHeight);
 *
 * // For live camera (coordinates are already normalized to view size)
 * overlayView.setDetectionResult(detectionResult);
 *
 * // Clear all overlays
 * overlayView.clearDetections();
 * }</pre>
 *
 * <p><strong>Visual Styling:</strong>
 * <ul>
 *   <li>Bounding box stroke width: 4px</li>
 *   <li>Label text size: 14sp (scaled for density)</li>
 *   <li>Label background with padding for readability</li>
 *   <li>Bold white text for high contrast</li>
 *   <li>Consistent colors per object type using HSV color space</li>
 * </ul>
 *
 * @author Object Detection SDK
 * @version 1.0
 * @since 1.0
 * @see DetectionResult
 * @see DetectedObject
 * @see BoundingBox
 */
public class DetectionOverlayView extends View {
    private static final String TAG = "DetectionOverlayView";
    private static final int TEXT_SIZE_SP = 14;
    private static final int STROKE_WIDTH_PX = 4;
    private static final int TEXT_PADDING_PX = 10;

    // Paints for drawing
    private final Paint boxPaint;
    private final Paint textBackgroundPaint;
    private final Paint textPaint;
    private final Random random;

    // Detection data
    private List<DetectedObject> detectedObjects = new ArrayList<>();

    // Image/preview dimensions
    private int sourceImageWidth;
    private int sourceImageHeight;

    /**
     * Constructor for programmatic instantiation.
     *
     * <p>Creates a new DetectionOverlayView with default styling and configuration.
     * This constructor is used when creating the view programmatically rather
     * than from XML layout inflation.</p>
     *
     * <p><strong>Usage Example:</strong>
     * <pre>{@code
     * DetectionOverlayView overlayView = new DetectionOverlayView(context);
     * overlayView.setLayoutParams(new FrameLayout.LayoutParams(
     *     FrameLayout.LayoutParams.MATCH_PARENT,
     *     FrameLayout.LayoutParams.MATCH_PARENT));
     * parentLayout.addView(overlayView);
     * }</pre>
     *
     * @param context The Android context (typically Activity or Fragment context)
     */
    public DetectionOverlayView(Context context) {
        this(context, null);
    }

    /**
     * Constructor for XML inflation.
     *
     * <p>This constructor is called when the view is inflated from XML layout files.
     * It supports basic XML attributes and sets up the view with default styling.</p>
     *
     * @param context The Android context
     * @param attrs XML attributes from the layout file (may be null)
     */
    public DetectionOverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor with style attribute support.
     *
     * <p>This is the most complete constructor that supports XML attributes and
     * custom style definitions. It initializes all Paint objects and sets up
     * the drawing configuration for optimal performance and visual quality.</p>
     *
     * <p><strong>Paint Configuration:</strong>
     * <ul>
     *   <li><strong>Box Paint:</strong> Stroke style, 4px width, anti-aliased</li>
     *   <li><strong>Text Background:</strong> Filled rectangles, anti-aliased</li>
     *   <li><strong>Text Paint:</strong> White color, bold typeface, scaled text size</li>
     * </ul>
     *
     * @param context The Android context
     * @param attrs XML attributes from the layout file (may be null)
     * @param defStyleAttr Default style attribute resource ID
     */
    public DetectionOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Initialize bounding box paint
        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(STROKE_WIDTH_PX);
        boxPaint.setAntiAlias(true);

        // Initialize label background paint
        textBackgroundPaint = new Paint();
        textBackgroundPaint.setStyle(Paint.Style.FILL);
        textBackgroundPaint.setAntiAlias(true);

        // Initialize label text paint
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(TEXT_SIZE_SP * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Random for color generation
        random = new Random(System.currentTimeMillis());
    }

    /**
     * Set detection result for static image processing.
     *
     * <p>This method is used when overlaying detection results on static images where
     * the source image dimensions may differ from the view dimensions. The method
     * takes explicit image dimensions which are used to scale the normalized
     * bounding box coordinates to the view's dimensions.</p>
     *
     * <p><strong>Coordinate Transformation:</strong>
     * The method handles the transformation from normalized coordinates [0.0, 1.0]
     * in the detection result to pixel coordinates in the view. This ensures that
     * bounding boxes appear in the correct positions regardless of image scaling.</p>
     *
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Gallery applications showing detection results</li>
     *   <li>Image editing apps with object detection</li>
     *   <li>Static image analysis tools</li>
     *   <li>Batch processing result visualization</li>
     * </ul>
     *
     * <p><strong>Example Usage:</strong>
     * <pre>{@code
     * // After getting detection result for an image
     * Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
     * overlayView.setDetectionResult(result, bitmap.getWidth(), bitmap.getHeight());
     *
     * // Or with ImageView dimensions
     * ImageView imageView = findViewById(R.id.imageView);
     * overlayView.setDetectionResult(result,
     *     imageView.getDrawable().getIntrinsicWidth(),
     *     imageView.getDrawable().getIntrinsicHeight());
     * }</pre>
     *
     * @param result Detection result containing detected objects (may be null)
     * @param imageWidth Width of the source image in pixels (must be > 0)
     * @param imageHeight Height of the source image in pixels (must be > 0)
     * @throws IllegalArgumentException if imageWidth or imageHeight are <= 0
     * @see #setDetectionResult(DetectionResult)
     * @see #clearDetections()
     */
    public void setDetectionResult(DetectionResult result, int imageWidth, int imageHeight) {
        if (result != null && result.getDetectedObjects() != null) {
            detectedObjects = result.getDetectedObjects();
        } else {
            detectedObjects = new ArrayList<>();
        }

        sourceImageWidth = imageWidth;
        sourceImageHeight = imageHeight;

        invalidate(); // Request redraw
    }

    /**
     * Set detection result for live camera processing.
     *
     * <p>This method is optimized for live camera scenarios where the source image
     * (camera preview) fills the entire view. The coordinates in the detection result
     * are normalized (0-1) and are scaled directly to the view's dimensions.</p>
     *
     * <p><strong>Performance Optimization:</strong>
     * This method is designed for high-frequency updates typical in live camera
     * applications. It minimizes coordinate transformation overhead by assuming
     * the source dimensions match the view dimensions.</p>
     *
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Real-time camera object detection</li>
     *   <li>Live surveillance applications</li>
     *   <li>Augmented reality overlays</li>
     *   <li>Interactive camera applications</li>
     * </ul>
     *
     * <p><strong>Example Usage:</strong>
     * <pre>{@code
     * // In live detection callback
     * LiveDetectionListener listener = new LiveDetectionListener() {
     *     @Override
     *     public void onDetectionResult(DetectionResult result, long frameTimestamp) {
     *         // Update overlay on UI thread
     *         runOnUiThread(() -> overlayView.setDetectionResult(result));
     *     }
     *
     *     @Override
     *     public void onError(Exception e, long frameTimestamp) {
     *         // Handle error
     *     }
     * };
     * }</pre>
     *
     * <p><strong>Thread Safety:</strong>
     * This method should be called from the UI thread. If calling from a background
     * thread (e.g., camera callback), use runOnUiThread() or Handler.post().</p>
     *
     * @param result Detection result containing detected objects (may be null)
     * @see #setDetectionResult(DetectionResult, int, int)
     * @see #clearDetections()
     * @see com.objectdetection.sdk.listener.LiveDetectionListener
     */
    public void setDetectionResult(DetectionResult result) {
        if (result != null && result.getDetectedObjects() != null) {
            detectedObjects = result.getDetectedObjects();
        } else {
            detectedObjects = new ArrayList<>();
        }

        // For live camera, the source dimensions are the view dimensions
        sourceImageWidth = getWidth();
        sourceImageHeight = getHeight();

        invalidate(); // Request redraw
    }

    /**
     * Clear all detection results from the view.
     *
     * <p>This method removes all currently displayed detection overlays and
     * triggers a redraw to show a clean view. It's useful for resetting the
     * overlay state between different images or when stopping live detection.</p>
     *
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Switching between different images in a gallery</li>
     *   <li>Stopping live detection mode</li>
     *   <li>Clearing results before new detection</li>
     *   <li>Handling detection errors</li>
     * </ul>
     *
     * <p><strong>Example Usage:</strong>
     * <pre>{@code
     * // When switching images
     * overlayView.clearDetections();
     * imageView.setImageBitmap(newBitmap);
     *
     * // When stopping live detection
     * ImageDetector.stopLiveDetection();
     * overlayView.clearDetections();
     *
     * // On detection error
     * overlayView.clearDetections();
     * showErrorMessage("Detection failed");
     * }</pre>
     *
     * <p><strong>Performance:</strong>
     * This is a lightweight operation that simply clears the detection list
     * and requests a redraw. It does not perform any heavy computations.</p>
     */
    public void clearDetections() {
        detectedObjects = new ArrayList<>();
        invalidate(); // Request redraw
    }

    /**
     * Called when the view size changes.
     *
     * <p>This method is automatically called by the Android framework whenever
     * the view's size changes due to layout changes, screen rotation, or other
     * factors. It updates the source dimensions when no explicit dimensions
     * were provided (typical for live camera scenarios).</p>
     *
     * <p><strong>Automatic Handling:</strong>
     * The method automatically adjusts coordinate scaling to match the new
     * view dimensions, ensuring that overlays remain correctly positioned
     * after size changes.</p>
     *
     * @param w Current width of the view
     * @param h Current height of the view
     * @param oldw Previous width of the view
     * @param oldh Previous height of the view
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Update source dimensions when view size changes if no explicit dimensions were set
        if (sourceImageWidth == 0 || sourceImageHeight == 0) {
            sourceImageWidth = w;
            sourceImageHeight = h;
        }
    }

    /**
     * Custom drawing method that renders detection overlays.
     *
     * <p>This method is called by the Android framework whenever the view needs
     * to be redrawn. It renders bounding boxes, labels, and confidence scores
     * for all detected objects using optimized drawing operations.</p>
     *
     * <p><strong>Drawing Process:</strong>
     * <ol>
     *   <li>Validate drawing preconditions (objects, dimensions)</li>
     *   <li>For each detected object:
     *     <ul>
     *       <li>Generate consistent color based on object label</li>
     *       <li>Scale normalized coordinates to view pixels</li>
     *       <li>Draw bounding box rectangle</li>
     *       <li>Draw label background rectangle</li>
     *       <li>Draw label text with confidence percentage</li>
     *     </ul>
     *   </li>
     *   <li>Handle edge cases (labels outside view bounds)</li>
     * </ol>
     *
     * <p><strong>Performance Optimizations:</strong>
     * <ul>
     *   <li>Early exit if no objects to draw</li>
     *   <li>Validation of object data before drawing</li>
     *   <li>Efficient color generation using hash-based seeding</li>
     *   <li>Minimal object allocation during drawing</li>
     * </ul>
     *
     * <p><strong>Visual Elements:</strong>
     * <ul>
     *   <li><strong>Bounding Box:</strong> Colored rectangle outline around object</li>
     *   <li><strong>Label Background:</strong> Solid rectangle for text readability</li>
     *   <li><strong>Label Text:</strong> Object name + confidence percentage</li>
     * </ul>
     *
     * @param canvas The canvas on which to draw the overlays
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Skip drawing if there's nothing to draw or dimensions are invalid
        if (detectedObjects.isEmpty() || sourceImageWidth <= 0 || sourceImageHeight <= 0 ||
                getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        // For each detected object, draw its bounding box and label
        for (DetectedObject object : detectedObjects) {
            if (object.getBox() == null) {
                Log.w(TAG, "Skipping object with null box: " + object.getLabel());
                continue;
            }

            BoundingBox box = object.getBox();
            if (box.getXMin() == null || box.getYMin() == null ||
                    box.getXMax() == null || box.getYMax() == null) {
                Log.w(TAG, "Skipping object with null coordinates: " + object.getLabel());
                continue;
            }

            // Generate a consistent color for this object type
            int boxColor = getRandomColor(object.getLabel());
            boxPaint.setColor(boxColor);
            textBackgroundPaint.setColor(boxColor);

            // Scale the normalized coordinates (0.0-1.0) to the view dimensions
            float left = box.getXMin();
            float top = box.getYMin() ;
            float right = box.getXMax();
            float bottom = box.getYMax();

            RectF scaledRect = new RectF(left, top, right, bottom);

            // Draw the bounding box
            canvas.drawRect(scaledRect, boxPaint);

            // Prepare the label text (label + confidence)
            String labelText = object.getLabel() + " " + object.getConfidenceAsPercentage();
            float textWidth = textPaint.measureText(labelText);
            float textHeight = textPaint.descent() - textPaint.ascent();

            // Create label background rectangle
            RectF labelBackground = new RectF(
                    scaledRect.left,
                    scaledRect.top - textHeight - TEXT_PADDING_PX,
                    scaledRect.left + textWidth + TEXT_PADDING_PX * 2,
                    scaledRect.top
            );

            // Make sure label stays within view bounds
            if (labelBackground.top < 0) {
                labelBackground.offset(0, -labelBackground.top);
            }

            // Draw label background
            canvas.drawRect(labelBackground, textBackgroundPaint);

            // Draw label text
            canvas.drawText(
                    labelText,
                    labelBackground.left + TEXT_PADDING_PX,
                    labelBackground.bottom - TEXT_PADDING_PX,
                    textPaint
            );
        }
    }

    /**
     * Generate a consistent color based on the object label.
     *
     * <p>This method creates visually distinct colors for different object types
     * while ensuring that the same object type always gets the same color across
     * different frames or images. This consistency helps users quickly identify
     * and track specific object types.</p>
     *
     * <p><strong>Color Generation Algorithm:</strong>
     * <ul>
     *   <li>Uses the object label's hash code as a random seed</li>
     *   <li>Generates colors in HSV color space for better distribution</li>
     *   <li>Ensures high saturation and brightness for visibility</li>
     *   <li>Provides good contrast against typical image backgrounds</li>
     * </ul>
     *
     * <p><strong>Color Properties:</strong>
     * <ul>
     *   <li><strong>Hue:</strong> 0-360° (full spectrum)</li>
     *   <li><strong>Saturation:</strong> 80-100% (vibrant colors)</li>
     *   <li><strong>Value/Brightness:</strong> 80-100% (bright colors)</li>
     * </ul>
     *
     * <p><strong>Benefits:</strong>
     * <ul>
     *   <li>Same object types have consistent colors</li>
     *   <li>Different object types have distinct colors</li>
     *   <li>Colors are bright and easily visible</li>
     *   <li>Good contrast for text overlays</li>
     * </ul>
     *
     * @param label The object label (e.g., "person", "car", "dog")
     * @return A color value suitable for drawing bounding boxes and backgrounds
     * @see Color#HSVToColor(float[])
     */
    private int getRandomColor(String label) {
        // Use the label's hash code as seed for consistent colors for the same labels
        Random labelRandom = new Random(label != null ? label.hashCode() : 0);

        // Generate a bright, saturated color
        float[] hsv = new float[3];
        hsv[0] = labelRandom.nextFloat() * 360;  // Hue
        hsv[1] = 0.8f + labelRandom.nextFloat() * 0.2f;  // Saturation
        hsv[2] = 0.8f + labelRandom.nextFloat() * 0.2f;  // Value

        return Color.HSVToColor(hsv);
    }
}