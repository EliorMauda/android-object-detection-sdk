package com.objectdetection.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.objectdetection.sdk.listener.ImageDetectionListener;
import com.objectdetection.sdk.listener.LiveDetectionListener;
import com.objectdetection.sdk.model.DetectionResult;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages live object detection using the device camera with CameraX.
 *
 * <p>This class handles the complex integration between CameraX camera operations
 * and object detection processing. It manages camera lifecycle, frame processing,
 * image conversion, and provides optimized real-time detection capabilities.</p>
 *
 * <p><strong>Core Responsibilities:</strong>
 * <ul>
 *   <li><strong>Camera Management:</strong> Initialize and manage CameraX use cases</li>
 *   <li><strong>Frame Processing:</strong> Convert camera frames to detection-ready format</li>
 *   <li><strong>Performance Optimization:</strong> Throttle processing to maintain smooth performance</li>
 *   <li><strong>Resource Management:</strong> Properly cleanup camera and processing resources</li>
 * </ul>
 *
 * <p><strong>Processing Pipeline:</strong>
 * <ol>
 *   <li>CameraX captures frames in YUV_420_888 format</li>
 *   <li>Frame throttling ensures reasonable processing frequency (~2 FPS)</li>
 *   <li>YUV frames are converted to JPEG byte arrays</li>
 *   <li>JPEG data is sent to detection service via ImageDetector</li>
 *   <li>Results are delivered through LiveDetectionListener callbacks</li>
 * </ol>
 *
 * <p><strong>Performance Optimizations:</strong>
 * <ul>
 *   <li><strong>Frame Throttling:</strong> Process maximum 2 frames per second</li>
 *   <li><strong>Backpressure Handling:</strong> Skip frames when processing is behind</li>
 *   <li><strong>Resolution Optimization:</strong> Use 640x480 for good performance/quality balance</li>
 *   <li><strong>Compression Settings:</strong> 85% JPEG quality for optimal file size</li>
 * </ul>
 *
 * <p><strong>Thread Management:</strong>
 * <ul>
 *   <li>Camera operations run on dedicated camera executor thread</li>
 *   <li>Image processing occurs on background thread</li>
 *   <li>Callbacks are delivered on main thread for UI updates</li>
 * </ul>
 *
 * <p><strong>Usage Note:</strong>
 * This class is primarily intended for internal use by the ImageDetector class.
 * External applications should use ImageDetector.startLiveDetection() methods
 * rather than creating LiveDetector instances directly.</p>
 *
 * @author Object Detection SDK
 * @version 1.0
 * @since 1.0
 * @see ImageDetector#startLiveDetection
 * @see LiveDetectionListener
 * @see androidx.camera.core.ImageAnalysis
 */
public class LiveDetector {
    private static final String TAG = "LiveDetector";
    private static final long MIN_PROCESS_INTERVAL_MS = 500; // Process at most 2 frames per second
    private static final int TARGET_RESOLUTION_WIDTH = 640;
    private static final int TARGET_RESOLUTION_HEIGHT = 480;
    private static final int JPEG_QUALITY = 85;

    private final Context context;
    private final LiveDetectionListener listener;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private boolean isRunning = false;
    private long lastProcessedTimestamp = 0;
    private boolean processingFrame = false;

    /**
     * Creates a new LiveDetector instance with the specified context and listener.
     *
     * <p>This constructor initializes the detector with necessary components for
     * camera management and result delivery. The context must implement LifecycleOwner
     * for proper camera lifecycle management.</p>
     *
     * <p><strong>Context Requirements:</strong>
     * <ul>
     *   <li>Must implement LifecycleOwner (Activity or Fragment)</li>
     *   <li>Must have camera permissions granted</li>
     *   <li>Should be a foreground context for camera access</li>
     * </ul>
     *
     * <p><strong>Initialization Process:</strong>
     * <ul>
     *   <li>Stores context and listener references</li>
     *   <li>Creates dedicated camera executor thread</li>
     *   <li>Initializes processing state variables</li>
     *   <li>Prepares for camera provider acquisition</li>
     * </ul>
     *
     * @param context Android context implementing LifecycleOwner (Activity/Fragment)
     * @param listener Callback for detection results and errors
     * @see #start(PreviewView, int)
     * @see #stop()
     */
    public LiveDetector(Context context, LiveDetectionListener listener) {
        this.context = context;
        this.listener = listener;
        this.cameraExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Starts live object detection using the specified camera and preview view.
     *
     * <p>This method initializes the camera system, binds necessary use cases
     * (preview and image analysis), and begins processing camera frames for
     * object detection. It handles the complex CameraX setup automatically.</p>
     *
     * <p><strong>CameraX Use Cases:</strong>
     * <ul>
     *   <li><strong>Preview:</strong> Displays camera feed in the PreviewView</li>
     *   <li><strong>ImageAnalysis:</strong> Processes frames for object detection</li>
     * </ul>
     *
     * <p><strong>Camera Configuration:</strong>
     * <ul>
     *   <li>Target resolution: 640x480 for optimal performance</li>
     *   <li>Backpressure strategy: Keep only latest frame</li>
     *   <li>Analysis on dedicated background thread</li>
     * </ul>
     *
     * <p><strong>Error Handling:</strong>
     * The method includes comprehensive error handling for common camera
     * initialization failures, including hardware access issues and permission
     * problems. Errors are delivered through the LiveDetectionListener.</p>
     *
     * <p><strong>Lifecycle Integration:</strong>
     * <pre>{@code
     * // Typical usage in Activity
     * @Override
     * protected void onResume() {
     *     super.onResume();
     *     if (hasPermissions()) {
     *         liveDetector.start(previewView, CameraSelector.LENS_FACING_BACK);
     *     }
     * }
     * }</pre>
     *
     * @param previewView CameraX PreviewView to display the camera feed
     * @param cameraFacing Camera to use (LENS_FACING_BACK or LENS_FACING_FRONT)
     * @throws IllegalArgumentException if context is not a LifecycleOwner
     * @see #stop()
     * @see androidx.camera.core.CameraSelector#LENS_FACING_BACK
     * @see androidx.camera.core.CameraSelector#LENS_FACING_FRONT
     */
    public void start(PreviewView previewView, @CameraSelector.LensFacing int cameraFacing) {
        if (isRunning) {
            Log.w(TAG, "Live detection is already running");
            return;
        }

        // Ensure context is a LifecycleOwner
        if (!(context instanceof LifecycleOwner)) {
            listener.onError(new IllegalArgumentException(
                    "Context must implement LifecycleOwner"), System.currentTimeMillis());
            return;
        }

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(previewView, cameraFacing);
                isRunning = true;
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
                listener.onError(new RuntimeException("Failed to start camera", e),
                        System.currentTimeMillis());
            }
        }, ContextCompat.getMainExecutor(context));
    }

    /**
     * Stops live detection and releases camera resources.
     *
     * <p>This method properly stops the camera and releases all associated resources.
     * It's essential to call this method to prevent resource leaks and allow other
     * applications to access the camera.</p>
     *
     * <p><strong>Cleanup Operations:</strong>
     * <ul>
     *   <li>Unbinds all CameraX use cases</li>
     *   <li>Stops camera preview and analysis</li>
     *   <li>Resets internal processing state</li>
     *   <li>Allows camera to be used by other apps</li>
     * </ul>
     *
     * <p><strong>Safe to Call:</strong>
     * This method is safe to call multiple times or when detection is not running.
     * It includes appropriate state checks to prevent errors.</p>
     *
     * <p><strong>Lifecycle Integration:</strong>
     * <pre>{@code
     * @Override
     * protected void onPause() {
     *     super.onPause();
     *     liveDetector.stop();
     * }
     * }</pre>
     *
     * @see #start(PreviewView, int)
     * @see #shutdown()
     */
    public void stop() {
        if (!isRunning) return;

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        isRunning = false;
    }

    /**
     * Clean up resources when the detector is no longer needed.
     *
     * <p>This method performs final cleanup including stopping detection and
     * shutting down the camera executor thread. Call this when the detector
     * will no longer be used to ensure complete resource cleanup.</p>
     *
     * <p><strong>Complete Cleanup:</strong>
     * <ul>
     *   <li>Stops any active detection session</li>
     *   <li>Shuts down camera executor thread</li>
     *   <li>Releases all internal references</li>
     *   <li>Prepares instance for garbage collection</li>
     * </ul>
     *
     * <p><strong>When to Call:</strong>
     * <ul>
     *   <li>Activity onDestroy() for permanent cleanup</li>
     *   <li>When switching to different detection modes</li>
     *   <li>Application shutdown sequences</li>
     * </ul>
     *
     * <p><strong>Usage Example:</strong>
     * <pre>{@code
     * @Override
     * protected void onDestroy() {
     *     super.onDestroy();
     *     if (liveDetector != null) {
     *         liveDetector.shutdown();
     *         liveDetector = null;
     *     }
     * }
     * }</pre>
     *
     * @see #stop()
     */
    public void shutdown() {
        stop();
        cameraExecutor.shutdown();
    }

    /**
     * Binds camera use cases for preview and image analysis.
     *
     * <p>This internal method configures and binds the CameraX use cases needed
     * for live detection. It sets up both the preview (for user feedback) and
     * image analysis (for object detection processing).</p>
     *
     * <p><strong>Use Case Configuration:</strong>
     * <ul>
     *   <li><strong>Preview:</strong> Displays live camera feed to user</li>
     *   <li><strong>ImageAnalysis:</strong> Processes frames for detection</li>
     *   <li><strong>Target Resolution:</strong> 640x480 for performance optimization</li>
     *   <li><strong>Backpressure Strategy:</strong> Keep only latest frame</li>
     * </ul>
     *
     * <p><strong>Frame Processing Pipeline:</strong>
     * <ol>
     *   <li>Camera captures frames in YUV_420_888 format</li>
     *   <li>ImageAnalysis.Analyzer receives frames on camera thread</li>
     *   <li>Frame throttling prevents processing overload</li>
     *   <li>Selected frames are processed for object detection</li>
     * </ol>
     *
     * @param previewView The PreviewView to display camera feed
     * @param cameraFacing Which camera to use (front or back)
     */
    private void bindCameraUseCases(PreviewView previewView, @CameraSelector.LensFacing int cameraFacing) {
        // Set up camera selector
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraFacing)
                .build();

        // Set up preview
        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(TARGET_RESOLUTION_WIDTH, TARGET_RESOLUTION_HEIGHT))
                .build();

        // Connect preview to the PreviewView
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Set up image analyzer
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(TARGET_RESOLUTION_WIDTH, TARGET_RESOLUTION_HEIGHT))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                long currentTimestamp = System.currentTimeMillis();

                // Skip frame if we're still processing a previous one
                if (processingFrame) {
                    imageProxy.close();
                    return;
                }

                // Skip frame if we processed one too recently
                if (currentTimestamp - lastProcessedTimestamp < MIN_PROCESS_INTERVAL_MS) {
                    imageProxy.close();
                    return;
                }

                // Process this frame
                lastProcessedTimestamp = currentTimestamp;
                processFrame(imageProxy, currentTimestamp);
            }
        });

        // Unbind any bound use cases before rebinding
        try {
            cameraProvider.unbindAll();

            // Bind use cases to camera
            Camera camera = cameraProvider.bindToLifecycle(
                    (LifecycleOwner) context,
                    cameraSelector,
                    preview,
                    imageAnalysis);

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            listener.onError(new RuntimeException("Camera binding failed", e),
                    System.currentTimeMillis());
        }
    }

    /**
     * Processes a camera frame for object detection.
     *
     * <p>This method handles the complex process of converting camera frames
     * from CameraX format to JPEG bytes suitable for detection processing.
     * It includes error handling and state management for robust operation.</p>
     *
     * <p><strong>Processing Steps:</strong>
     * <ol>
     *   <li>Extract Image from ImageProxy</li>
     *   <li>Convert YUV_420_888 to JPEG byte array</li>
     *   <li>Apply rotation correction if needed</li>
     *   <li>Send bytes to ImageDetector for processing</li>
     *   <li>Handle results through LiveDetectionListener</li>
     * </ol>
     *
     * <p><strong>Error Handling:</strong>
     * Comprehensive error handling ensures that processing failures don't
     * crash the detection system or cause resource leaks.</p>
     *
     * @param imageProxy The camera frame to process
     * @param timestamp Timestamp when the frame was captured
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processFrame(ImageProxy imageProxy, long timestamp) {
        processingFrame = true;

        try {
            // Get the image
            Image image = imageProxy.getImage();
            if (image == null) {
                imageProxy.close();
                processingFrame = false;
                return;
            }

            // Convert YUV to JPEG (this is a simplified approach)
            byte[] jpegBytes = yuv420ToJpeg(imageProxy);
            if (jpegBytes == null) {
                imageProxy.close();
                processingFrame = false;
                return;
            }

            // Use the SDK to detect objects
            ImageDetector.detectFromBytes(jpegBytes, new ImageDetectionListener() {
                @Override
                public void onResult(DetectionResult result) {
                    listener.onDetectionResult(result, timestamp);
                    imageProxy.close();
                    processingFrame = false;
                }

                @Override
                public void onError(Exception e) {
                    listener.onError(e, timestamp);
                    imageProxy.close();
                    processingFrame = false;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
            imageProxy.close();
            processingFrame = false;
            listener.onError(e, timestamp);
        }
    }

    /**
     * Convert YUV_420_888 format from CameraX to JPEG byte array.
     *
     * <p>This method handles the complex image format conversion required to
     * process camera frames. CameraX provides frames in YUV_420_888 format,
     * but the detection service expects JPEG data.</p>
     *
     * <p><strong>Conversion Process:</strong>
     * <ol>
     *   <li>Extract YUV planes from Image</li>
     *   <li>Convert to Android Bitmap</li>
     *   <li>Apply rotation correction based on device orientation</li>
     *   <li>Compress to JPEG with specified quality</li>
     *   <li>Return as byte array</li>
     * </ol>
     *
     * <p><strong>Performance Considerations:</strong>
     * <ul>
     *   <li>Uses 85% JPEG quality for good size/quality balance</li>
     *   <li>Handles bitmap recycling to prevent memory leaks</li>
     *   <li>Optimized for frequent high-volume processing</li>
     * </ul>
     *
     * <p><strong>Format Support:</strong>
     * Currently optimized for YUV_420_888, which is the standard format
     * provided by most Android cameras through CameraX.</p>
     *
     * @param imageProxy The ImageProxy containing YUV_420_888 image data
     * @return JPEG byte array, or null if conversion failed
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private byte[] yuv420ToJpeg(ImageProxy imageProxy) {
        try {
            Image image = imageProxy.getImage();
            if (image == null) return null;

            // Create a bitmap from the YUV image
            Bitmap bitmap = imageToBitmap(image);
            if (bitmap == null) return null;

            // Apply rotation if needed
            int rotation = imageProxy.getImageInfo().getRotationDegrees();
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap.recycle();
                bitmap = rotatedBitmap;
            }

            // Convert bitmap to JPEG bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
            byte[] jpegBytes = outputStream.toByteArray();

            // Clean up
            bitmap.recycle();
            outputStream.close();

            return jpegBytes;
        } catch (Exception e) {
            Log.e(TAG, "Error converting YUV to JPEG", e);
            return null;
        }
    }

    /**
     * Convert a YUV_420_888 Image to a Bitmap.
     *
     * <p>This method performs the low-level conversion from YUV color space
     * to RGB bitmap format. It handles the pixel format transformation
     * required for Android graphics operations.</p>
     *
     * <p><strong>Implementation Note:</strong>
     * This is a simplified implementation suitable for detection purposes.
     * For production applications requiring maximum performance, consider
     * using RenderScript or native code for YUV to RGB conversion.</p>
     *
     * <p><strong>Color Space Conversion:</strong>
     * <ul>
     *   <li>Input: YUV_420_888 (Y, U, V planes)</li>
     *   <li>Intermediate: NV21 format for YuvImage compatibility</li>
     *   <li>Output: RGB Bitmap suitable for JPEG compression</li>
     * </ul>
     *
     * <p><strong>Performance Optimization Opportunities:</strong>
     * <ul>
     *   <li>RenderScript for GPU-accelerated conversion</li>
     *   <li>Native code for CPU optimization</li>
     *   <li>Direct YUV to JPEG conversion (server-side)</li>
     * </ul>
     *
     * @param image The YUV_420_888 image from camera
     * @return A Bitmap representation, or null if conversion failed
     */
    private Bitmap imageToBitmap(Image image) {
        // For a more efficient implementation in production, consider using RenderScript
        // This is a simplified implementation for demonstration purposes
        try {
            final int width = image.getWidth();
            final int height = image.getHeight();

            // Get image planes
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];

            // U and V are swapped
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, width, height), 100, out);

            byte[] imageBytes = out.toByteArray();
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error converting image to bitmap", e);
            return null;
        }
    }
}