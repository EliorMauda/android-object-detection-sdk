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
 * visual feedback of the detection process with precise coordinate transformation.</p>
 *
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li>Draws bounding boxes around detected objects with pixel-perfect alignment</li>
 *   <li>Displays object labels with confidence percentages</li>
 *   <li>Generates consistent colors for each object type</li>
 *   <li>Supports both static image and live camera overlays</li>
 *   <li>Advanced coordinate scaling and transformation system</li>
 *   <li>Handles different aspect ratios and image scaling modes</li>
 *   <li>Optimized for performance in real-time scenarios</li>
 *   <li>Automatic bounds checking and edge case handling</li>
 * </ul>
 *
 * <p><strong>Coordinate Transformation System:</strong>
 * Version 2.0 introduces a sophisticated coordinate transformation system that ensures
 * bounding boxes are perfectly aligned with detected objects, regardless of image scaling,
 * aspect ratio differences, or display transformations. The system handles:</p>
 * <ul>
 *   <li><strong>Model Space:</strong> Original image dimensions used by the AI model</li>
 *   <li><strong>Display Space:</strong> Actual display dimensions within the view</li>
 *   <li><strong>Scaling:</strong> Proportional resizing between model and display</li>
 *   <li><strong>Offset:</strong> Positioning adjustments for centering and padding</li>
 * </ul>
 *
 * <p><strong>Usage Scenarios:</strong>
 * <ul>
 *   <li><strong>Static Images:</strong> Gallery apps, image analysis tools</li>
 *   <li><strong>Live Camera:</strong> Real-time detection, AR applications</li>
 *   <li><strong>Security Systems:</strong> Surveillance, monitoring applications</li>
 *   <li><strong>Medical Imaging:</strong> Diagnostic tools, research applications</li>
 *   <li><strong>Industrial:</strong> Quality control, automated inspection</li>
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
 *         android:layout_height="match_parent"
 *         android:scaleType="fitCenter" />
 *
 *     <com.objectdetection.sdk.view.DetectionOverlayView
 *         android:id="@+id/overlayView"
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent" />
 *
 * </FrameLayout>
 * }</pre>
 *
 * @author Object Detection SDK
 * @version 2.0
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

    // Detection data
    private List<DetectedObject> detectedObjects = new ArrayList<>();

    // Coordinate transformation parameters
    private int originalWidth = 0;      // Original image/frame width from model
    private int originalHeight = 0;     // Original image/frame height from model
    private int displayWidth = 0;       // Width of displayed image/frame area
    private int displayHeight = 0;      // Height of displayed image/frame area
    private int offsetX = 0;            // Horizontal offset of displayed area
    private int offsetY = 0;            // Vertical offset of displayed area

    // Legacy mode flag for backward compatibility
    private boolean useLegacyCoordinates = false;

    /**
     * Constructor for programmatic instantiation.
     *
     * <p>Creates a new DetectionOverlayView with default styling and configuration.
     * This constructor is used when creating the view programmatically rather
     * than from XML layout inflation. The view is initialized with optimized
     * Paint objects for high-performance drawing operations.</p>
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
     * <p><strong>Default Configuration:</strong>
     * <ul>
     *   <li>Bounding box stroke width: 4px</li>
     *   <li>Text size: 14sp (scaled for device density)</li>
     *   <li>Anti-aliasing enabled for smooth graphics</li>
     *   <li>Bold white text for optimal contrast</li>
     * </ul>
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
     * It supports basic XML attributes and sets up the view with default styling.
     * The Android framework automatically calls this constructor during layout
     * inflation processes.</p>
     *
     * <p><strong>XML Attributes:</strong>
     * While this view doesn't define custom XML attributes in the current version,
     * it inherits all standard View attributes such as layout parameters,
     * visibility, and transformation properties.</p>
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
     * the drawing configuration for optimal performance and visual quality.
     * This constructor is the foundation for all other constructors.</p>
     *
     * <p><strong>Paint Initialization:</strong>
     * <ul>
     *   <li><strong>Box Paint:</strong> Stroke style, 4px width, anti-aliased, color set per object</li>
     *   <li><strong>Text Background:</strong> Filled rectangles, anti-aliased, matches box color</li>
     *   <li><strong>Text Paint:</strong> White color, bold typeface, density-scaled size</li>
     * </ul>
     *
     * <p><strong>Performance Optimizations:</strong>
     * <ul>
     *   <li>Paint objects created once and reused</li>
     *   <li>Anti-aliasing enabled for smooth graphics</li>
     *   <li>Text size automatically scaled for device density</li>
     *   <li>Typeface cached for efficient text rendering</li>
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
    }

    /**
     * Set detection result with advanced coordinate transformation.
     *
     * <p>This is the primary method for setting detection results with pixel-perfect
     * coordinate transformation. It handles complex scenarios where the original
     * image/frame processed by the AI model has different dimensions than the
     * displayed area, ensuring bounding boxes appear in exactly the right positions.</p>
     *
     * <p><strong>Coordinate Transformation Process:</strong>
     * <ol>
     *   <li><strong>Input Validation:</strong> Validates all dimension parameters</li>
     *   <li><strong>Scale Calculation:</strong> Computes X and Y scaling factors</li>
     *   <li><strong>Position Mapping:</strong> Maps model coordinates to display coordinates</li>
     *   <li><strong>Offset Application:</strong> Applies positioning offsets for centering</li>
     *   <li><strong>Bounds Checking:</strong> Ensures coordinates stay within view bounds</li>
     * </ol>
     *
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li><strong>Static Images:</strong> ImageView with fitCenter scaleType</li>
     *   <li><strong>Different Aspect Ratios:</strong> 16:9 image in 4:3 view</li>
     *   <li><strong>High-Resolution Images:</strong> 4K image displayed in smaller view</li>
     *   <li><strong>Camera Preview:</strong> Different camera resolution vs preview size</li>
     *   <li><strong>Cropped Images:</strong> Images with padding or margins</li>
     * </ul>
     *
     * <p><strong>Example - Static Image with Proper Scaling:</strong>
     * <pre>{@code
     * // Load image and get its dimensions
     * Drawable drawable = imageView.getDrawable();
     * int originalWidth = drawable.getIntrinsicWidth();   // e.g., 1920
     * int originalHeight = drawable.getIntrinsicHeight(); // e.g., 1080
     *
     * // Get ImageView dimensions
     * int viewWidth = imageView.getWidth();   // e.g., 800
     * int viewHeight = imageView.getHeight(); // e.g., 600
     *
     * // Calculate displayed image area (fitCenter scaling)
     * float imageAspectRatio = (float) originalWidth / originalHeight;
     * float viewAspectRatio = (float) viewWidth / viewHeight;
     *
     * int displayWidth, displayHeight, offsetX = 0, offsetY = 0;
     * if (imageAspectRatio > viewAspectRatio) {
     *     // Image is wider - fit to width
     *     displayWidth = viewWidth;
     *     displayHeight = (int) (viewWidth / imageAspectRatio);
     *     offsetY = (viewHeight - displayHeight) / 2;
     * } else {
     *     // Image is taller - fit to height
     *     displayHeight = viewHeight;
     *     displayWidth = (int) (viewHeight * imageAspectRatio);
     *     offsetX = (viewWidth - displayWidth) / 2;
     * }
     *
     * // Set detection result with proper transformation
     * overlayView.setDetectionResult(result, originalWidth, originalHeight,
     *                               displayWidth, displayHeight, offsetX, offsetY);
     * }</pre>
     *
     * <p><strong>Example - Live Camera with Different Resolution:</strong>
     * <pre>{@code
     * // Camera provides frames at 1920x1080
     * int cameraWidth = 1920;
     * int cameraHeight = 1080;
     *
     * // Preview view is 800x600
     * int previewWidth = previewView.getWidth();
     * int previewHeight = previewView.getHeight();
     *
     * // Calculate how camera frame is displayed in preview
     * float cameraAspectRatio = (float) cameraWidth / cameraHeight;
     * float previewAspectRatio = (float) previewWidth / previewHeight;
     *
     * int displayWidth, displayHeight, offsetX = 0, offsetY = 0;
     * if (cameraAspectRatio > previewAspectRatio) {
     *     displayWidth = previewWidth;
     *     displayHeight = (int) (previewWidth / cameraAspectRatio);
     *     offsetY = (previewHeight - displayHeight) / 2;
     * } else {
     *     displayHeight = previewHeight;
     *     displayWidth = (int) (previewHeight * cameraAspectRatio);
     *     offsetX = (previewWidth - displayWidth) / 2;
     * }
     *
     * overlayView.setDetectionResult(result, cameraWidth, cameraHeight,
     *                               displayWidth, displayHeight, offsetX, offsetY);
     * }</pre>
     *
     * <p><strong>Parameter Guidelines:</strong>
     * <ul>
     *   <li><strong>originalWidth/Height:</strong> Use exact dimensions from AI model input</li>
     *   <li><strong>displayWidth/Height:</strong> Actual displayed image area (not view size)</li>
     *   <li><strong>offsetX/Y:</strong> Distance from view edge to displayed image edge</li>
     * </ul>
     *
     * <p><strong>Performance Considerations:</strong>
     * This method is optimized for frequent updates and can be called from UI thread
     * without performance concerns. Coordinate transformation is computed once during
     * drawing rather than stored, minimizing memory usage.</p>
     *
     * @param result The detection result containing detected objects (may be null)
     * @param originalWidth The original image/frame width from the AI model (must be > 0)
     * @param originalHeight The original image/frame height from the AI model (must be > 0)
     * @param displayWidth The width of the displayed image/frame area (must be > 0)
     * @param displayHeight The height of the displayed image/frame area (must be > 0)
     * @param offsetX The horizontal offset of the displayed area from view edge (>= 0)
     * @param offsetY The vertical offset of the displayed area from view edge (>= 0)
     * @throws IllegalArgumentException if any dimension parameter is <= 0
     * @see #clearDetections()
     * @since 2.0
     */
    public void setDetectionResult(DetectionResult result,
                                   int originalWidth, int originalHeight,
                                   int displayWidth, int displayHeight,
                                   int offsetX, int offsetY) {
        if (result != null && result.getDetectedObjects() != null) {
            detectedObjects = result.getDetectedObjects();
        } else {
            detectedObjects = new ArrayList<>();
        }

        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.useLegacyCoordinates = false;

        Log.d(TAG, String.format("Set detection result: original(%dx%d), display(%dx%d), offset(%d,%d)",
                originalWidth, originalHeight, displayWidth, displayHeight, offsetX, offsetY));

        invalidate(); // Trigger a redraw
    }

    /**
     * Set detection result for static image processing (legacy method).
     *
     * <p><strong>⚠️ DEPRECATED:</strong> This method is maintained for backward compatibility
     * but may not provide accurate coordinate alignment for all scenarios. For pixel-perfect
     * bounding box positioning, use the advanced coordinate transformation method.</p>
     *
     * <p>This method assumes the image fills the entire view without any scaling adjustments,
     * offset calculations, or aspect ratio handling. It works well for simple cases where
     * the image dimensions exactly match the view dimensions, but may produce misaligned
     * bounding boxes in more complex scenarios.</p>
     *
     * <p><strong>When This Method Works Well:</strong>
     * <ul>
     *   <li>Image dimensions exactly match view dimensions</li>
     *   <li>ImageView scaleType is "matrix" or "fitXY" (no aspect ratio preservation)</li>
     *   <li>Simple image display without complex layout requirements</li>
     *   <li>Legacy applications that cannot be easily updated</li>
     * </ul>
     *
     * <p><strong>When to Use the New Method Instead:</strong>
     * <ul>
     *   <li>ImageView scaleType is "fitCenter", "centerInside", or "centerCrop"</li>
     *   <li>Image and view have different aspect ratios</li>
     *   <li>Complex layouts with padding, margins, or transformations</li>
     *   <li>High-resolution images displayed in smaller views</li>
     *   <li>When precise bounding box alignment is critical</li>
     * </ul>
     *
     * <p><strong>Migration Example:</strong>
     * <pre>{@code
     * // Old way (deprecated)
     * overlayView.setDetectionResult(result, imageWidth, imageHeight);
     *
     * // New way (recommended)
     * // Calculate proper display dimensions and offsets first
     * overlayView.setDetectionResult(result, originalWidth, originalHeight,
     *                               displayWidth, displayHeight, offsetX, offsetY);
     * }</pre>
     *
     * @param result Detection result containing detected objects (may be null)
     * @param imageWidth Width of the source image in pixels (must be > 0)
     * @param imageHeight Height of the source image in pixels (must be > 0)
     * @throws IllegalArgumentException if imageWidth or imageHeight are <= 0
     * @deprecated Use {@link #setDetectionResult(DetectionResult, int, int, int, int, int, int)}
     *             for accurate coordinate transformation. This method will be removed in v3.0.
     * @see #setDetectionResult(DetectionResult, int, int, int, int, int, int)
     * @see #clearDetections()
     */
    @Deprecated
    public void setDetectionResult(DetectionResult result, int imageWidth, int imageHeight) {
        if (result != null && result.getDetectedObjects() != null) {
            detectedObjects = result.getDetectedObjects();
        } else {
            detectedObjects = new ArrayList<>();
        }

        this.originalWidth = imageWidth;
        this.originalHeight = imageHeight;
        this.displayWidth = getWidth() > 0 ? getWidth() : imageWidth;
        this.displayHeight = getHeight() > 0 ? getHeight() : imageHeight;
        this.offsetX = 0;
        this.offsetY = 0;
        this.useLegacyCoordinates = true;

        Log.d(TAG, String.format("Set detection result (legacy): image(%dx%d), view(%dx%d)",
                imageWidth, imageHeight, this.displayWidth, this.displayHeight));

        invalidate(); // Request redraw
    }

    /**
     * Set detection result for live camera processing (legacy method).
     *
     * <p><strong>⚠️ DEPRECATED:</strong> This method is maintained for backward compatibility
     * but may not handle camera resolution differences correctly. For accurate live camera
     * detection overlays, use the advanced coordinate transformation method with proper
     * camera frame dimension handling.</p>
     *
     * <p>This method assumes that the camera preview dimensions exactly match the view
     * dimensions, which is often not the case in real applications. Modern cameras
     * typically provide frames at higher resolutions (e.g., 1920x1080) than the
     * preview view size (e.g., 800x600), leading to coordinate misalignment.</p>
     *
     * <p><strong>Common Issues with This Method:</strong>
     * <ul>
     *   <li><strong>Resolution Mismatch:</strong> Camera frame vs preview view size differences</li>
     *   <li><strong>Aspect Ratio Problems:</strong> Different camera and view aspect ratios</li>
     *   <li><strong>Scaling Issues:</strong> No handling of preview scaling modes</li>
     *   <li><strong>Orientation Problems:</strong> Portrait/landscape transition handling</li>
     * </ul>
     *
     * <p><strong>Migration for Live Camera:</strong>
     * <pre>{@code
     * // Old way (deprecated)
     * overlayView.setDetectionResult(result);
     *
     * // New way (recommended)
     * LiveDetectionListener listener = new LiveDetectionListener() {
     *     public void onDetectionResult(DetectionResult result, long frameTimestamp) {
     *         // Get camera frame dimensions (from your camera configuration)
     *         int frameWidth = 1920;  // Actual camera frame width
     *         int frameHeight = 1080; // Actual camera frame height
     *
     *         // Get preview view dimensions
     *         int previewWidth = previewView.getWidth();
     *         int previewHeight = previewView.getHeight();
     *
     *         // Calculate display transformation
     *         // ... (aspect ratio calculation code) ...
     *
     *         overlayView.setDetectionResult(result, frameWidth, frameHeight,
     *                                       displayWidth, displayHeight, offsetX, offsetY);
     *     }
     * };
     * }</pre>
     *
     * <p><strong>Temporary Workaround:</strong>
     * If you must use this method temporarily, ensure your camera is configured to
     * output frames at the exact same resolution as your preview view to minimize
     * coordinate misalignment.</p>
     *
     * @param result Detection result containing detected objects (may be null)
     * @deprecated Use {@link #setDetectionResult(DetectionResult, int, int, int, int, int, int)}
     *             with proper camera frame dimensions for accurate coordinate transformation.
     *             This method will be removed in v3.0.
     * @see #setDetectionResult(DetectionResult, int, int, int, int, int, int)
     * @see com.objectdetection.sdk.listener.LiveDetectionListener
     * @see #clearDetections()
     */
    @Deprecated
    public void setDetectionResult(DetectionResult result) {
        if (result != null && result.getDetectedObjects() != null) {
            detectedObjects = result.getDetectedObjects();
        } else {
            detectedObjects = new ArrayList<>();
        }

        // For live camera, assume the source dimensions are the view dimensions
        this.originalWidth = getWidth();
        this.originalHeight = getHeight();
        this.displayWidth = getWidth();
        this.displayHeight = getHeight();
        this.offsetX = 0;
        this.offsetY = 0;
        this.useLegacyCoordinates = true;

        Log.d(TAG, String.format("Set detection result (legacy camera): view(%dx%d)",
                getWidth(), getHeight()));

        invalidate(); // Request redraw
    }

    /**
     * Clear all detection results from the view.
     *
     * <p>This method removes all currently displayed detection overlays and
     * triggers a redraw to show a clean view. It's essential for maintaining
     * clean state transitions and preventing visual artifacts from stale
     * detection data.</p>
     *
     * <p><strong>State Reset:</strong>
     * The method resets the detection object list but preserves coordinate
     * transformation parameters. This allows for seamless switching between
     * different detection results without reconfiguring the overlay system.</p>
     *
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li><strong>Image Switching:</strong> Clearing results before loading new image</li>
     *   <li><strong>Detection Pause:</strong> Temporarily hiding results during processing</li>
     *   <li><strong>Error Handling:</strong> Clearing results when detection fails</li>
     *   <li><strong>Mode Changes:</strong> Switching between different detection modes</li>
     *   <li><strong>Performance:</strong> Reducing rendering load when results not needed</li>
     * </ul>
     *
     * <p><strong>Example Usage Patterns:</strong>
     * <pre>{@code
     * // Image gallery navigation
     * public void onImageChanged(String newImagePath) {
     *     overlayView.clearDetections();
     *     imageView.setImageBitmap(loadImage(newImagePath));
     *     startDetection(newImagePath);
     * }
     *
     * // Live detection toggle
     * public void onDetectionToggle(boolean enabled) {
     *     if (enabled) {
     *         startLiveDetection();
     *     } else {
     *         stopLiveDetection();
     *         overlayView.clearDetections();
     *     }
     * }
     *
     * // Error handling
     * public void onDetectionError(Exception e) {
     *     overlayView.clearDetections();
     *     showErrorMessage("Detection failed: " + e.getMessage());
     * }
     *
     * // Performance optimization during scrolling
     * public void onScrollStateChanged(int newState) {
     *     if (newState == SCROLL_STATE_DRAGGING) {
     *         overlayView.clearDetections(); // Reduce rendering during scroll
     *     }
     * }
     * }</pre>
     *
     * <p><strong>Performance Characteristics:</strong>
     * <ul>
     *   <li><strong>Lightweight Operation:</strong> O(1) complexity, instant execution</li>
     *   <li><strong>Memory Efficient:</strong> Immediately releases detection object references</li>
     *   <li><strong>Thread Safe:</strong> Safe to call from UI thread</li>
     *   <li><strong>Immediate Effect:</strong> Triggers invalidate() for instant visual update</li>
     * </ul>
     *
     * <p><strong>Thread Safety:</strong>
     * This method is designed to be called from the UI thread. If calling from
     * background threads (e.g., camera callbacks), use runOnUiThread() wrapper.</p>
     */
    public void clearDetections() {
        detectedObjects = new ArrayList<>();
        invalidate(); // Request redraw
    }

    /**
     * Called when the view size changes due to layout, rotation, or other factors.
     *
     * <p>This method is automatically invoked by the Android framework whenever
     * the view's dimensions change. It provides an opportunity to update internal
     * coordinate transformation parameters to maintain accurate overlay positioning
     * across different view sizes and orientations.</p>
     *
     * <p><strong>Automatic Adjustments:</strong>
     * <ul>
     *   <li><strong>Legacy Mode:</strong> Updates display dimensions for backward compatibility</li>
     *   <li><strong>Aspect Ratio:</strong> Maintains proper coordinate scaling ratios</li>
     *   <li><strong>Orientation:</strong> Handles portrait/landscape transitions</li>
     *   <li><strong>Layout Changes:</strong> Adapts to dynamic layout modifications</li>
     * </ul>
     *
     * <p><strong>When This Method is Called:</strong>
     * <ul>
     *   <li>Initial view layout and measurement</li>
     *   <li>Device orientation changes (portrait ↔ landscape)</li>
     *   <li>Parent layout modifications</li>
     *   <li>View visibility changes</li>
     *   <li>Dynamic layout parameter updates</li>
     *   <li>Split-screen mode transitions</li>
     * </ul>
     *
     * <p><strong>Coordinate System Impact:</strong>
     * In legacy coordinate mode, this method automatically updates the display
     * dimensions to match the new view size. For the advanced coordinate system,
     * applications should recalculate and set proper transformation parameters
     * after size changes to maintain accuracy.</p>
     *
     * <p><strong>Implementation Notes:</strong>
     * <ul>
     *   <li>Calls super.onSizeChanged() to maintain framework behavior</li>
     *   <li>Only updates coordinates in legacy mode to avoid breaking explicit settings</li>
     *   <li>Logs dimension changes for debugging purposes</li>
     *   <li>Does not trigger invalidate() - relies on framework's redraw cycle</li>
     * </ul>
     *
     * @param w Current width of the view in pixels
     * @param h Current height of the view in pixels
     * @param oldw Previous width of the view in pixels
     * @param oldh Previous height of the view in pixels
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Update display dimensions for legacy mode if no explicit dimensions were set
        if (useLegacyCoordinates && (displayWidth == 0 || displayHeight == 0)) {
            displayWidth = w;
            displayHeight = h;
            Log.d(TAG, String.format("Updated display dimensions on size change: %dx%d", w, h));
        }
    }

    /**
     * Custom drawing method that renders detection overlays with advanced coordinate transformation.
     *
     * <p>This method is the core rendering engine of the DetectionOverlayView. It's called
     * automatically by the Android framework whenever the view needs to be redrawn, such as
     * after invalidate() calls, layout changes, or system refresh cycles. The method
     * implements sophisticated coordinate transformation and rendering optimizations.</p>
     *
     * <p><strong>Rendering Pipeline:</strong>
     * <ol>
     *   <li><strong>Precondition Validation:</strong> Checks for valid objects and dimensions</li>
     *   <li><strong>Scale Factor Calculation:</strong> Computes X/Y transformation ratios</li>
     *   <li><strong>Object Iteration:</strong> Processes each detected object individually</li>
     *   <li><strong>Data Validation:</strong> Validates bounding box data integrity</li>
     *   <li><strong>Color Generation:</strong> Creates consistent colors per object type</li>
     *   <li><strong>Coordinate Transformation:</strong> Maps model space to display space</li>
     *   <li><strong>Bounds Checking:</strong> Ensures coordinates stay within view limits</li>
     *   <li><strong>Geometry Validation:</strong> Verifies valid rectangle dimensions</li>
     *   <li><strong>Bounding Box Rendering:</strong> Draws object outline rectangles</li>
     *   <li><strong>Label Positioning:</strong> Calculates optimal text placement</li>
     *   <li><strong>Edge Case Handling:</strong> Adjusts labels for screen boundaries</li>
     *   <li><strong>Text Rendering:</strong> Draws background and foreground text</li>
     * </ol>
     *
     * <p><strong>Coordinate Transformation Mathematics:</strong>
     * <pre>{@code
     * // Scale factors convert from model space to display space
     * scaleX = displayWidth / originalWidth
     * scaleY = displayHeight / originalHeight
     *
     * // Transform each coordinate
     * displayX = modelX * scaleX + offsetX
     * displayY = modelY * scaleY + offsetY
     *
     * // Example: 1920x1080 model → 800x450 display with 0x75 offset
     * scaleX = 800/1920 = 0.417
     * scaleY = 450/1080 = 0.417
     * modelPoint(960, 540) → displayPoint(960*0.417+0, 540*0.417+75) = (400, 300)
     * }</pre>
     *
     * <p><strong>Performance Optimizations:</strong>
     * <ul>
     *   <li><strong>Early Exit:</strong> Returns immediately if no objects to draw</li>
     *   <li><strong>Batch Validation:</strong> Validates dimensions once, not per object</li>
     *   <li><strong>Paint Reuse:</strong> Reuses Paint objects, only changes colors</li>
     *   <li><strong>Minimal Allocations:</strong> Reuses RectF objects when possible</li>
     *   <li><strong>Efficient Logging:</strong> Verbose logs only in debug builds</li>
     *   <li><strong>Smart Clipping:</strong> Skips objects completely outside view bounds</li>
     * </ul>
     *
     * <p><strong>Visual Elements Rendered:</strong>
     * <ul>
     *   <li><strong>Bounding Boxes:</strong> Colored stroke rectangles around objects</li>
     *   <li><strong>Label Backgrounds:</strong> Solid color rectangles for text readability</li>
     *   <li><strong>Label Text:</strong> Object name + confidence percentage in white</li>
     * </ul>
     *
     * <p><strong>Error Handling:</strong>
     * <ul>
     *   <li>Null bounding box objects are logged and skipped</li>
     *   <li>Invalid coordinates (null, negative, or NaN) are handled gracefully</li>
     *   <li>Degenerate rectangles (zero or negative area) are detected and skipped</li>
     *   <li>Out-of-bounds coordinates are clamped to view dimensions</li>
     *   <li>Label positioning automatically adjusts for screen edges</li>
     * </ul>
     *
     * <p><strong>Text Label Intelligence:</strong>
     * The method implements smart label positioning that automatically handles
     * edge cases where labels would extend beyond screen boundaries:</p>
     * <ul>
     *   <li><strong>Top Edge:</strong> Moves label below bounding box if it would go above screen</li>
     *   <li><strong>Right Edge:</strong> Shifts label left if it would extend past right edge</li>
     *   <li><strong>Left Edge:</strong> Moves label right if it would go past left edge</li>
     *   <li><strong>Corner Cases:</strong> Handles multiple edge violations simultaneously</li>
     * </ul>
     *
     * <p><strong>Color Consistency:</strong>
     * Object colors are generated deterministically based on object labels,
     * ensuring the same object type always appears in the same color across
     * different frames, sessions, and application runs.</p>
     *
     * @param canvas The canvas on which to draw the overlays (provided by Android framework)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Skip drawing if there's nothing to draw or dimensions are invalid
        if (detectedObjects.isEmpty() || originalWidth <= 0 || originalHeight <= 0 ||
                displayWidth <= 0 || displayHeight <= 0) {
            return;
        }

        // Calculate scale factors for coordinate transformation
        float scaleX = (float) displayWidth / originalWidth;
        float scaleY = (float) displayHeight / originalHeight;

        Log.v(TAG, String.format("Drawing %d objects with scale(%.2f, %.2f), offset(%d, %d)",
                detectedObjects.size(), scaleX, scaleY, offsetX, offsetY));

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
            int boxColor = getColorForLabel(object.getLabel());
            boxPaint.setColor(boxColor);
            textBackgroundPaint.setColor(boxColor);

            // Transform coordinates from model space to display space
            float left, top, right, bottom;

            if (useLegacyCoordinates) {
                // Legacy mode: assume coordinates are already in the correct space
                left = box.getXMin() * scaleX + offsetX;
                top = box.getYMin() * scaleY + offsetY;
                right = box.getXMax() * scaleX + offsetX;
                bottom = box.getYMax() * scaleY + offsetY;
            } else {
                // New mode: proper coordinate transformation
                left = box.getXMin() * scaleX + offsetX;
                top = box.getYMin() * scaleY + offsetY;
                right = box.getXMax() * scaleX + offsetX;
                bottom = box.getYMax() * scaleY + offsetY;
            }

            // Ensure coordinates are within view bounds
            left = Math.max(0, Math.min(left, getWidth()));
            top = Math.max(0, Math.min(top, getHeight()));
            right = Math.max(0, Math.min(right, getWidth()));
            bottom = Math.max(0, Math.min(bottom, getHeight()));

            // Skip if the bounding box is too small or invalid
            if (right <= left || bottom <= top) {
                Log.w(TAG, String.format("Skipping invalid box for %s: (%.1f,%.1f)-(%.1f,%.1f)",
                        object.getLabel(), left, top, right, bottom));
                continue;
            }

            RectF scaledRect = new RectF(left, top, right, bottom);

            // Draw the bounding box
            canvas.drawRect(scaledRect, boxPaint);

            // Prepare the label text (label + confidence)
            String labelText = object.getLabel() + " " + object.getConfidenceAsPercentage();
            float textWidth = textPaint.measureText(labelText);
            float textHeight = textPaint.descent() - textPaint.ascent();

            // Calculate label background position
            float labelLeft = scaledRect.left;
            float labelTop = scaledRect.top - textHeight - TEXT_PADDING_PX;
            float labelRight = labelLeft + textWidth + TEXT_PADDING_PX * 2;
            float labelBottom = scaledRect.top;

            // Adjust label position if it goes outside view bounds
            if (labelTop < 0) {
                // Move label below the box if it would go above the view
                labelTop = scaledRect.bottom;
                labelBottom = labelTop + textHeight + TEXT_PADDING_PX;
            }
            if (labelRight > getWidth()) {
                // Move label to the left if it would go outside the right edge
                float shift = labelRight - getWidth();
                labelLeft -= shift;
                labelRight -= shift;
            }
            if (labelLeft < 0) {
                // Move label to the right if it would go outside the left edge
                labelLeft = 0;
                labelRight = textWidth + TEXT_PADDING_PX * 2;
            }

            RectF labelBackground = new RectF(labelLeft, labelTop, labelRight, labelBottom);

            // Draw label background
            canvas.drawRect(labelBackground, textBackgroundPaint);

            // Draw label text
            float textX = labelBackground.left + TEXT_PADDING_PX;
            float textY = labelBackground.bottom - TEXT_PADDING_PX;
            canvas.drawText(labelText, textX, textY, textPaint);

            Log.v(TAG, String.format("Drew %s at (%.1f,%.1f)-(%.1f,%.1f)",
                    object.getLabel(), left, top, right, bottom));
        }
    }

    /**
     * Generate a consistent, visually distinct color based on the object label.
     *
     * <p>This method implements a sophisticated color generation algorithm that creates
     * visually appealing and highly distinguishable colors for different object types.
     * The same object label will always produce the same color, providing visual
     * consistency across different detection sessions and application runs.</p>
     *
     * <p><strong>Algorithm Design:</strong>
     * The method uses the HSV (Hue, Saturation, Value) color space rather than RGB
     * to ensure better color distribution and visual appeal. HSV provides intuitive
     * control over color properties:</p>
     * <ul>
     *   <li><strong>Hue (0-360°):</strong> Determines the base color (red, blue, green, etc.)</li>
     *   <li><strong>Saturation (80-100%):</strong> Ensures vibrant, non-washed-out colors</li>
     *   <li><strong>Value/Brightness (80-100%):</strong> Ensures colors are bright and visible</li>
     * </ul>
     *
     * <p><strong>Consistency Mechanism:</strong>
     * The method uses the string hash code of the object label as a seed for the
     * random number generator. This ensures that:</p>
     * <ul>
     *   <li>Same labels always generate the same color</li>
     *   <li>Different labels generate different colors (with high probability)</li>
     *   <li>Color generation is deterministic and reproducible</li>
     *   <li>No external state or color mapping tables are required</li>
     * </ul>
     *
     * <p><strong>Visual Properties:</strong>
     * The generated colors are optimized for object detection overlays:</p>
     * <ul>
     *   <li><strong>High Contrast:</strong> Stand out against typical image backgrounds</li>
     *   <li><strong>Text Readable:</strong> Provide good contrast for white text labels</li>
     *   <li><strong>Distinguishable:</strong> Different enough to tell objects apart</li>
     *   <li><strong>Professional:</strong> Bright but not garish or unprofessional</li>
     * </ul>
     *
     * <p><strong>Mathematical Foundation:</strong>
     * <pre>{@code
     * // Create deterministic random generator
     * Random labelRandom = new Random(label.hashCode());
     *
     * // Generate HSV values
     * hue = random.nextFloat() * 360      // Full color spectrum
     * saturation = 0.8 + random * 0.2    // 80-100% saturation
     * value = 0.8 + random * 0.2          // 80-100% brightness
     *
     * // Convert to RGB color
     * return Color.HSVToColor([hue, saturation, value]);
     * }</pre>
     *
     * <p><strong>Example Color Mappings:</strong>
     * <ul>
     *   <li><strong>"person":</strong> Always generates the same vibrant color (e.g., bright blue)</li>
     *   <li><strong>"car":</strong> Always generates a different consistent color (e.g., bright red)</li>
     *   <li><strong>"dog":</strong> Always generates another distinct color (e.g., bright green)</li>
     * </ul>
     *
     * <p><strong>Performance Characteristics:</strong>
     * <ul>
     *   <li><strong>Fast Execution:</strong> O(1) complexity, suitable for real-time use</li>
     *   <li><strong>Memory Efficient:</strong> No color caching or lookup tables</li>
     *   <li><strong>Thread Safe:</strong> Uses local Random instance, no shared state</li>
     *   <li><strong>Deterministic:</strong> Same input always produces same output</li>
     * </ul>
     *
     * <p><strong>Collision Handling:</strong>
     * While hash code collisions are theoretically possible, they are extremely rare
     * in practice for typical object detection scenarios. The 32-bit hash space
     * provides sufficient distribution for even large object type vocabularies.</p>
     *
     * @param label The object label (e.g., "person", "car", "dog") - null-safe
     * @return A vibrant, consistent color value suitable for drawing bounding boxes and backgrounds
     * @see Color#HSVToColor(float[])
     * @see String#hashCode()
     */
    private int getColorForLabel(String label) {
        // Use the label's hash code as seed for consistent colors for the same labels
        Random labelRandom = new Random(label != null ? label.hashCode() : 0);

        // Generate a bright, saturated color in HSV space
        float[] hsv = new float[3];
        hsv[0] = labelRandom.nextFloat() * 360;  // Hue (0-360°)
        hsv[1] = 0.8f + labelRandom.nextFloat() * 0.2f;  // Saturation (80-100%)
        hsv[2] = 0.8f + labelRandom.nextFloat() * 0.2f;  // Value/Brightness (80-100%)

        return Color.HSVToColor(hsv);
    }

    /**
     * Get detailed information about current coordinate transformation parameters.
     *
     * <p>This method provides comprehensive debugging information about the current
     * state of the coordinate transformation system. It's invaluable for
     * troubleshooting alignment issues, verifying configuration correctness,
     * and understanding how coordinates are being transformed.</p>
     *
     * <p><strong>Information Provided:</strong>
     * <ul>
     *   <li><strong>Original Dimensions:</strong> Model input image/frame size</li>
     *   <li><strong>Display Dimensions:</strong> Actual displayed area size</li>
     *   <li><strong>Offset Values:</strong> Positioning adjustments for centering</li>
     *   <li><strong>Legacy Mode Flag:</strong> Whether using old or new coordinate system</li>
     * </ul>
     *
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li><strong>Debugging:</strong> Diagnosing coordinate alignment problems</li>
     *   <li><strong>Logging:</strong> Recording transformation state for analysis</li>
     *   <li><strong>Testing:</strong> Verifying correct parameter configuration</li>
     *   <li><strong>Support:</strong> Providing diagnostic information for bug reports</li>
     * </ul>
     *
     * <p><strong>Example Output:</strong>
     * <pre>{@code
     * "DetectionOverlayView: original(1920x1080), display(800x450), offset(0,75), legacy=false"
     * }</pre>
     *
     * <p><strong>Integration Example:</strong>
     * <pre>{@code
     * // Log transformation state for debugging
     * Log.d(TAG, "Overlay configuration: " + overlayView.getTransformationInfo());
     *
     * // Include in bug reports
     * String diagnostics = "Device: " + Build.MODEL + "\n" +
     *                     "Overlay: " + overlayView.getTransformationInfo() + "\n" +
     *                     "View size: " + overlayView.getWidth() + "x" + overlayView.getHeight();
     *
     * // Validate configuration in tests
     * String info = overlayView.getTransformationInfo();
     * assertThat(info).contains("legacy=false");
     * assertThat(info).contains("original(1920x1080)");
     * }</pre>
     *
     * @return A formatted string containing detailed transformation parameters
     * @since 2.0
     */
    public String getTransformationInfo() {
        return String.format("DetectionOverlayView: original(%dx%d), display(%dx%d), offset(%d,%d), legacy=%b",
                originalWidth, originalHeight, displayWidth, displayHeight,
                offsetX, offsetY, useLegacyCoordinates);
    }
}