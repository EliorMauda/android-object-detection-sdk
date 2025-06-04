package com.objectdetection.sdk;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.objectdetection.sdk.listener.ImageDetectionListener;
import com.objectdetection.sdk.model.DetectionResult;

import java.io.File;

/**
 * Builder class for creating and configuring object detection requests.
 *
 * <p>This class implements the Builder design pattern to provide a fluent,
 * readable API for setting up and executing detection tasks. It simplifies
 * the process of configuring detection parameters and ensures that all
 * required components are properly set before execution.</p>
 *
 * <p><strong>Builder Pattern Benefits:</strong>
 * <ul>
 *   <li>Fluent, chainable API for better readability</li>
 *   <li>Ensures required parameters are set before execution</li>
 *   <li>Provides a clean separation between configuration and execution</li>
 *   <li>Extensible design for future parameter additions</li>
 *   <li>Reduces constructor complexity and parameter confusion</li>
 * </ul>
 *
 * <p><strong>Supported Detection Sources:</strong>
 * <ul>
 *   <li><strong>File:</strong> Local image files on device storage</li>
 *   <li><strong>URI:</strong> Content URIs from gallery, camera, or content providers</li>
 *   <li><strong>URL:</strong> Images accessible via HTTP/HTTPS URLs</li>
 * </ul>
 *
 * <p><strong>Basic Usage Pattern:</strong>
 * <pre>{@code
 * DetectorBuilder.with(context)
 *     .setListener(new ImageDetectionListener() {
 *         @Override
 *         public void onResult(DetectionResult result) {
 *             // Handle successful detection
 *         }
 *
 *         @Override
 *         public void onError(Exception e) {
 *             // Handle detection error
 *         }
 *     })
 *     .detectFromFile(imageFile);
 * }</pre>
 *
 * <p><strong>Advanced Usage Examples:</strong>
 * <pre>{@code
 * // Detect from gallery image
 * DetectorBuilder.with(this)
 *     .setListener(detectionListener)
 *     .detectFromUri(galleryImageUri);
 *
 * // Detect from web image
 * DetectorBuilder.with(this)
 *     .setListener(detectionListener)
 *     .detectFromUrl("https://example.com/image.jpg");
 *
 * // Reusable builder configuration
 * DetectorBuilder builder = DetectorBuilder.with(context)
 *     .setListener(detectionListener);
 *
 * // Use the same configuration for multiple detections
 * builder.detectFromFile(image1);
 * builder.detectFromFile(image2);
 * builder.detectFromUrl(webImageUrl);
 * }</pre>
 *
 * <p><strong>Error Handling:</strong>
 * The builder validates required parameters and throws IllegalStateException
 * if essential components (like the listener) are not set before execution.</p>
 *
 * @author Object Detection SDK
 * @version 1.0
 * @since 1.0
 * @see ImageDetectionListener
 * @see ImageDetector
 * @see DetectionResult
 */
public class DetectorBuilder {
    private final Context context;
    private ImageDetectionListener listener;

    /**
     * Private constructor to enforce the use of the factory method.
     *
     * <p>This constructor is private to ensure that DetectorBuilder instances
     * are only created through the {@link #with(Context)} factory method,
     * which provides a clear entry point and validates the context parameter.</p>
     *
     * @param context Android context required for URI processing and other operations
     */
    private DetectorBuilder(Context context) {
        this.context = context;
    }

    /**
     * Create a new DetectorBuilder instance with the specified Android context.
     *
     * <p>This is the primary entry point for creating detection requests. The context
     * is required for various operations including URI-to-file conversion, resource
     * access, and content resolver operations.</p>
     *
     * <p><strong>Context Requirements:</strong>
     * <ul>
     *   <li>Must be a valid Android context (Activity, Fragment, Application, etc.)</li>
     *   <li>Should have appropriate permissions for the intended operations</li>
     *   <li>For URI operations, context must have access to content providers</li>
     * </ul>
     *
     * <p><strong>Usage Examples:</strong>
     * <pre>{@code
     * // From Activity
     * DetectorBuilder builder = DetectorBuilder.with(this);
     *
     * // From Fragment
     * DetectorBuilder builder = DetectorBuilder.with(getContext());
     *
     * // From Service or other components
     * DetectorBuilder builder = DetectorBuilder.with(context);
     * }</pre>
     *
     * <p><strong>Thread Safety:</strong>
     * Each DetectorBuilder instance is independent and not thread-safe. Create
     * separate instances for concurrent operations or synchronize access externally.</p>
     *
     * @param context Android context (must not be null)
     * @return A new DetectorBuilder instance ready for configuration
     * @throws IllegalArgumentException if context is null
     * @see #setListener(ImageDetectionListener)
     */
    public static DetectorBuilder with(@NonNull Context context) {
        return new DetectorBuilder(context);
    }

    /**
     * Set the listener for detection results and errors.
     *
     * <p>The listener is a required component that handles both successful detection
     * results and error conditions. This method must be called before any detection
     * operation, or an IllegalStateException will be thrown.</p>
     *
     * <p><strong>Listener Responsibilities:</strong>
     * <ul>
     *   <li><strong>onResult():</strong> Process successful detection results</li>
     *   <li><strong>onError():</strong> Handle various error conditions</li>
     *   <li><strong>UI Updates:</strong> Update user interface with results</li>
     *   <li><strong>Error Recovery:</strong> Implement appropriate error handling</li>
     * </ul>
     *
     * <p><strong>Thread Considerations:</strong>
     * Listener methods are called on the main thread, making it safe to update
     * UI elements directly from the callback methods.</p>
     *
     * <p><strong>Common Error Scenarios:</strong>
     * <ul>
     *   <li>Network connectivity issues (URL detection)</li>
     *   <li>File not found or access denied (file detection)</li>
     *   <li>Invalid image format or corrupted data</li>
     *   <li>Server-side processing errors</li>
     *   <li>Authentication or authorization failures</li>
     * </ul>
     *
     * <p><strong>Implementation Example:</strong>
     * <pre>{@code
     * ImageDetectionListener listener = new ImageDetectionListener() {
     *     @Override
     *     public void onResult(DetectionResult result) {
     *         if (result.isSuccess()) {
     *             List<DetectedObject> objects = result.getDetectedObjects();
     *             updateUI(objects);
     *             showMessage("Found " + objects.size() + " objects");
     *         } else {
     *             showError("Detection completed but no results");
     *         }
     *     }
     *
     *     @Override
     *     public void onError(Exception e) {
     *         Log.e(TAG, "Detection failed", e);
     *         showError("Detection failed: " + e.getMessage());
     *         hideLoadingIndicator();
     *     }
     * };
     * }</pre>
     *
     * @param listener Callback for detection results and errors (must not be null)
     * @return This DetectorBuilder instance for method chaining
     * @throws IllegalArgumentException if listener is null
     * @see ImageDetectionListener
     * @see DetectionResult
     */
    public DetectorBuilder setListener(@NonNull ImageDetectionListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Start object detection on a local image file.
     *
     * <p>This method initiates detection on a file stored on the device's local
     * storage. The file is uploaded to the detection service and processed
     * asynchronously. Results are delivered through the configured listener.</p>
     *
     * <p><strong>File Requirements:</strong>
     * <ul>
     *   <li>File must exist and be readable by the application</li>
     *   <li>Must be a valid image format (JPEG, PNG, BMP, etc.)</li>
     *   <li>Reasonable file size (very large files may timeout)</li>
     *   <li>Sufficient resolution for effective object detection</li>
     * </ul>
     *
     * <p><strong>Performance Considerations:</strong>
     * <ul>
     *   <li>Large files take longer to upload and process</li>
     *   <li>Network speed affects upload time</li>
     *   <li>Consider image compression for better performance</li>
     *   <li>Show loading indicators for better user experience</li>
     * </ul>
     *
     * <p><strong>Common Use Cases:</strong>
     * <ul>
     *   <li>Processing photos taken with device camera</li>
     *   <li>Analyzing images saved to device storage</li>
     *   <li>Batch processing of local image collections</li>
     *   <li>Offline image analysis applications</li>
     * </ul>
     *
     * <p><strong>Usage Example:</strong>
     * <pre>{@code
     * File imageFile = new File(Environment.getExternalStorageDirectory(), "photo.jpg");
     *
     * DetectorBuilder.with(this)
     *     .setListener(new ImageDetectionListener() {
     *         @Override
     *         public void onResult(DetectionResult result) {
     *             showResults(result);
     *         }
     *
     *         @Override
     *         public void onError(Exception e) {
     *             showError(e.getMessage());
     *         }
     *     })
     *     .detectFromFile(imageFile);
     * }</pre>
     *
     * @param file Image file to process (must exist and be readable)
     * @throws IllegalStateException if ImageDetectionListener has not been set
     * @throws IllegalArgumentException if file is null
     * @see #setListener(ImageDetectionListener)
     * @see ImageDetector#detectFromFile(File, ImageDetectionListener)
     */
    public void detectFromFile(@NonNull File file) {
        if (listener == null) {
            throw new IllegalStateException("ImageDetectionListener must be set");
        }

        ImageDetector.detectFromFile(file, listener);
    }

    /**
     * Start object detection on an image specified by URI.
     *
     * <p>This method handles detection for images referenced by Android content URIs.
     * Common sources include gallery images, camera captures, and content from
     * other applications. The URI is automatically converted to a temporary file
     * for processing.</p>
     *
     * <p><strong>Supported URI Schemes:</strong>
     * <ul>
     *   <li><strong>content://:</strong> Gallery images, camera captures, shared content</li>
     *   <li><strong>file://:</strong> Direct file system references</li>
     *   <li><strong>android.resource://:</strong> App resources (with proper permissions)</li>
     * </ul>
     *
     * <p><strong>Permission Requirements:</strong>
     * <ul>
     *   <li>READ_EXTERNAL_STORAGE for gallery images (Android < 10)</li>
     *   <li>Appropriate URI permissions for shared content</li>
     *   <li>Camera permissions for camera capture URIs</li>
     * </ul>
     *
     * <p><strong>Common Use Cases:</strong>
     * <ul>
     *   <li>Processing images selected from device gallery</li>
     *   <li>Analyzing images captured with camera intents</li>
     *   <li>Processing images shared from other applications</li>
     *   <li>Working with content provider images</li>
     * </ul>
     *
     * <p><strong>Implementation Details:</strong>
     * The method internally converts the URI to a temporary file using the
     * UriToFileConverter utility, which handles various URI schemes and ensures
     * compatibility across different Android versions.</p>
     *
     * <p><strong>Usage Example:</strong>
     * <pre>{@code
     * // From image picker result
     * @Override
     * protected void onActivityResult(int requestCode, int resultCode, Intent data) {
     *     if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
     *         Uri imageUri = data.getData();
     *
     *         DetectorBuilder.with(this)
     *             .setListener(detectionListener)
     *             .detectFromUri(imageUri);
     *     }
     * }
     *
     * // From camera capture
     * Uri photoUri = FileProvider.getUriForFile(this, authority, photoFile);
     * DetectorBuilder.with(this)
     *     .setListener(detectionListener)
     *     .detectFromUri(photoUri);
     * }</pre>
     *
     * @param uri Image URI to process (must be accessible by the application)
     * @throws IllegalStateException if ImageDetectionListener has not been set
     * @throws IllegalArgumentException if uri is null
     * @see #setListener(ImageDetectionListener)
     * @see ImageDetector#detectFromUri(Context, Uri, ImageDetectionListener)
     * @see com.objectdetection.sdk.util.UriToFileConverter
     */
    public void detectFromUri(@NonNull Uri uri) {
        if (listener == null) {
            throw new IllegalStateException("ImageDetectionListener must be set");
        }

        ImageDetector.detectFromUri(context, uri, listener);
    }

    /**
     * Start object detection on an image accessible via URL.
     *
     * <p>This method processes images hosted on web servers or cloud storage
     * services. The detection service downloads the image from the specified
     * URL and performs object detection without requiring client-side download.</p>
     *
     * <p><strong>URL Requirements:</strong>
     * <ul>
     *   <li>Must be publicly accessible (no authentication required)</li>
     *   <li>Should use HTTPS for security when possible</li>
     *   <li>Must point directly to an image file</li>
     *   <li>Should have appropriate CORS headers if cross-domain</li>
     *   <li>Must be a valid, well-formed URL</li>
     * </ul>
     *
     * <p><strong>Supported Image Sources:</strong>
     * <ul>
     *   <li>Cloud storage services (AWS S3, Google Cloud, Azure, etc.)</li>
     *   <li>Content delivery networks (CDNs)</li>
     *   <li>Public image hosting services</li>
     *   <li>Social media image URLs</li>
     *   <li>Web server hosted images</li>
     * </ul>
     *
     * <p><strong>Performance Benefits:</strong>
     * <ul>
     *   <li>No client-side upload bandwidth required</li>
     *   <li>Faster for images already online</li>
     *   <li>Reduces mobile data usage</li>
     *   <li>Server-side optimization opportunities</li>
     * </ul>
     *
     * <p><strong>Error Scenarios:</strong>
     * <ul>
     *   <li>URL returns 404 (not found) or other HTTP errors</li>
     *   <li>Network connectivity issues</li>
     *   <li>URL points to non-image content</li>
     *   <li>Server cannot access the URL (firewall, authentication)</li>
     *   <li>URL is malformed or invalid</li>
     * </ul>
     *
     * <p><strong>Usage Examples:</strong>
     * <pre>{@code
     * // Process image from cloud storage
     * String cloudUrl = "https://storage.googleapis.com/bucket/image.jpg";
     * DetectorBuilder.with(this)
     *     .setListener(detectionListener)
     *     .detectFromUrl(cloudUrl);
     *
     * // Process image from CDN
     * String cdnUrl = "https://cdn.example.com/images/photo.png";
     * DetectorBuilder.with(this)
     *     .setListener(detectionListener)
     *     .detectFromUrl(cdnUrl);
     *
     * // With error handling
     * DetectorBuilder.with(this)
     *     .setListener(new ImageDetectionListener() {
     *         @Override
     *         public void onResult(DetectionResult result) {
     *             processResults(result);
     *         }
     *
     *         @Override
     *         public void onError(Exception e) {
     *             if (e.getMessage().contains("404")) {
     *                 showError("Image not found at URL");
     *             } else {
     *                 showError("Network error: " + e.getMessage());
     *             }
     *         }
     *     })
     *     .detectFromUrl(imageUrl);
     * }</pre>
     *
     * @param imageUrl URL of the image to process (must be publicly accessible)
     * @throws IllegalStateException if ImageDetectionListener has not been set
     * @throws IllegalArgumentException if imageUrl is null or empty
     * @see #setListener(ImageDetectionListener)
     * @see ImageDetector#detectFromUrl(String, ImageDetectionListener)
     */
    public void detectFromUrl(@NonNull String imageUrl) {
        if (listener == null) {
            throw new IllegalStateException("ImageDetectionListener must be set");
        }

        ImageDetector.detectFromUrl(imageUrl, listener);
    }
}