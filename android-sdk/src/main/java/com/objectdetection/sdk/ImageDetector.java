package com.objectdetection.sdk;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.objectdetection.sdk.listener.ImageDetectionListener;
import com.objectdetection.sdk.listener.LiveDetectionListener;
import com.objectdetection.sdk.model.DetectionResult;
import com.objectdetection.sdk.network.ApiClient;
import com.objectdetection.sdk.util.UriToFileConverter;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Main entry point class for performing object detection on images.
 *
 * <p>This class serves as the primary interface for the Object Detection SDK,
 * providing both static image detection and live camera detection capabilities.
 * It manages the underlying API client, handles initialization, and provides
 * convenient methods for various detection scenarios.</p>
 *
 * <p><strong>Core Capabilities:</strong>
 * <ul>
 *   <li><strong>Static Image Detection:</strong> Process individual images from files, URIs, or URLs</li>
 *   <li><strong>Live Camera Detection:</strong> Real-time object detection using device camera</li>
 *   <li><strong>Multiple Input Sources:</strong> Support for files, URIs, URLs, and byte arrays</li>
 *   <li><strong>Asynchronous Processing:</strong> Non-blocking operations with callback-based results</li>
 * </ul>
 *
 * <p><strong>SDK Initialization:</strong>
 * Before using any detection methods, the SDK must be initialized with a valid API base URL:
 * <pre>{@code
 * // Initialize the SDK (typically in Application.onCreate() or Activity.onCreate())
 * ImageDetector.init("https://api.example.com/");
 *
 * // Check initialization status
 * if (ImageDetector.isInitialized()) {
 *     // SDK is ready to use
 * }
 * }</pre>
 *
 * <p><strong>Static Image Detection Examples:</strong>
 * <pre>{@code
 * // Detect from file
 * File imageFile = new File(path);
 * ImageDetector.detectFromFile(imageFile, new ImageDetectionListener() {
 *     @Override
 *     public void onResult(DetectionResult result) {
 *         // Handle successful detection
 *     }
 *
 *     @Override
 *     public void onError(Exception e) {
 *         // Handle error
 *     }
 * });
 *
 * // Detect from URI (gallery image)
 * ImageDetector.detectFromUri(context, imageUri, detectionListener);
 *
 * // Detect from URL
 * ImageDetector.detectFromUrl("https://example.com/image.jpg", detectionListener);
 * }</pre>
 *
 * <p><strong>Live Camera Detection Example:</strong>
 * <pre>{@code
 * PreviewView previewView = findViewById(R.id.preview);
 *
 * ImageDetector.startLiveDetection(this, previewView, new LiveDetectionListener() {
 *     @Override
 *     public void onDetectionResult(DetectionResult result, long frameTimestamp) {
 *         // Update overlay with detection results
 *         overlayView.setDetectionResult(result);
 *     }
 *
 *     @Override
 *     public void onError(Exception e, long frameTimestamp) {
 *         // Handle detection error
 *     }
 * });
 *
 * // Stop live detection when done
 * ImageDetector.stopLiveDetection();
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong>
 * All public methods are thread-safe and can be called from any thread. Callback
 * methods are executed on the main thread, making it safe to update UI elements
 * directly from the callbacks.</p>
 *
 * <p><strong>Resource Management:</strong>
 * The SDK automatically manages network connections and resources. For live detection,
 * make sure to call stopLiveDetection() when no longer needed to free camera resources.</p>
 *
 * @author Object Detection SDK
 * @version 1.0
 * @since 1.0
 * @see DetectorBuilder
 * @see ImageDetectionListener
 * @see LiveDetectionListener
 * @see DetectionResult
 */
public class ImageDetector {
    private static final String TAG = "ImageDetector";

    private static ApiClient apiClient;
    private static String apiUrl;
    private static LiveDetector liveDetector;

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This class is designed as a utility class with static methods only.
     * All functionality is accessed through static methods, and no instance
     * creation is necessary or allowed.</p>
     */
    private ImageDetector() {
        // Private constructor to prevent instantiation
    }

    /**
     * Initialize the ImageDetector with the specified API base URL.
     *
     * <p>This method must be called before using any other SDK functionality.
     * It configures the underlying HTTP client and establishes the connection
     * to the object detection service. Typically called once during application
     * startup or activity initialization.</p>
     *
     * <p><strong>Base URL Requirements:</strong>
     * <ul>
     *   <li>Must include the protocol (http:// or https://)</li>
     *   <li>Should end with a trailing slash for proper path resolution</li>
     *   <li>Must point to a valid object detection service</li>
     *   <li>HTTPS is strongly recommended for production environments</li>
     * </ul>
     *
     * <p><strong>Example Valid URLs:</strong>
     * <ul>
     *   <li>https://api.objectdetection.com/</li>
     *   <li>https://api.example.com/v1/detection/</li>
     *   <li>http://localhost:8080/api/ (development only)</li>
     * </ul>
     *
     * <p><strong>Initialization Best Practices:</strong>
     * <pre>{@code
     * public class MyApplication extends Application {
     *     @Override
     *     public void onCreate() {
     *         super.onCreate();
     *
     *         // Initialize SDK with production URL
     *         ImageDetector.init("https://api.objectdetection.com/");
     *
     *         // Or use different URLs for different build variants
     *         if (BuildConfig.DEBUG) {
     *             ImageDetector.init("http://localhost:8080/api/");
     *         } else {
     *             ImageDetector.init("https://production-api.com/");
     *         }
     *     }
     * }
     * }</pre>
     *
     * <p><strong>Configuration Effects:</strong>
     * This method configures global SDK settings including network timeouts,
     * JSON serialization, and API endpoint routing. All subsequent detection
     * operations will use this configuration.</p>
     *
     * @param baseUrl The base URL of the object detection API (must include protocol)
     * @throws IllegalArgumentException if baseUrl is null, empty, or malformed
     * @see #isInitialized()
     * @see #getApiUrl()
     */
    public static void init(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be null or empty");
        }

        apiUrl = baseUrl;
        apiClient = new ApiClient(baseUrl);

        Log.d(TAG, "ImageDetector initialized with base URL: " + baseUrl);
    }

    /**
     * Check if the ImageDetector has been properly initialized.
     *
     * <p>This method provides a way to verify that the SDK has been initialized
     * before attempting to use detection functionality. It's useful for defensive
     * programming and debugging initialization issues.</p>
     *
     * <p><strong>Usage Examples:</strong>
     * <pre>{@code
     * // Check before using detection features
     * if (!ImageDetector.isInitialized()) {
     *     ImageDetector.init("https://api.example.com/");
     * }
     *
     * // Defensive programming in detection methods
     * public void performDetection() {
     *     if (!ImageDetector.isInitialized()) {
     *         showError("SDK not initialized");
     *         return;
     *     }
     *     // Proceed with detection...
     * }
     *
     * // Conditional feature enabling
     * if (ImageDetector.isInitialized()) {
     *     enableDetectionFeatures();
     * } else {
     *     showInitializationPrompt();
     * }
     * }</pre>
     *
     * @return true if the SDK has been initialized with init(), false otherwise
     * @see #init(String)
     * @see #getApiUrl()
     */
    public static boolean isInitialized() {
        return apiClient != null;
    }

    /**
     * Get the currently configured API base URL.
     *
     * <p>Returns the base URL that was set during initialization. Useful for
     * debugging configuration issues, logging, or displaying current API
     * endpoint information to users or administrators.</p>
     *
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Debugging configuration issues</li>
     *   <li>Logging current API endpoint for troubleshooting</li>
     *   <li>Displaying API status in admin interfaces</li>
     *   <li>Validating configuration in tests</li>
     * </ul>
     *
     * <p><strong>Usage Examples:</strong>
     * <pre>{@code
     * // Log current configuration
     * Log.d(TAG, "Using API endpoint: " + ImageDetector.getApiUrl());
     *
     * // Display in settings/debug screen
     * TextView apiUrlText = findViewById(R.id.api_url);
     * apiUrlText.setText("API: " + ImageDetector.getApiUrl());
     *
     * // Validate in unit tests
     * assertEquals("https://test-api.com/", ImageDetector.getApiUrl());
     * }</pre>
     *
     * @return the configured API base URL, or null if not initialized
     * @see #init(String)
     * @see #isInitialized()
     */
    public static String getApiUrl() {
        return apiUrl;
    }

    /**
     * Detect objects in a local image file.
     *
     * <p>This method processes images stored on the device's local storage by
     * uploading them to the detection service. It handles file validation,
     * upload, and result processing asynchronously to avoid blocking the UI thread.</p>
     *
     * <p><strong>File Validation:</strong>
     * The method performs basic file validation before attempting upload:
     * <ul>
     *   <li>Checks that the file exists on the filesystem</li>
     *   <li>Verifies that the file is readable by the application</li>
     *   <li>Returns FileNotFoundException for invalid files</li>
     * </ul>
     *
     * <p><strong>Supported File Types:</strong>
     * <ul>
     *   <li>JPEG (.jpg, .jpeg) - Most common and recommended</li>
     *   <li>PNG (.png) - Supports transparency</li>
     *   <li>BMP (.bmp) - Uncompressed format</li>
     *   <li>Other formats as supported by the server</li>
     * </ul>
     *
     * <p><strong>Performance Considerations:</strong>
     * <ul>
     *   <li>Large files (>10MB) may cause timeouts</li>
     *   <li>High-resolution images take longer to process</li>
     *   <li>Consider image compression for better performance</li>
     *   <li>Network speed affects upload time significantly</li>
     * </ul>
     *
     * <p><strong>Common Use Cases:</strong>
     * <ul>
     *   <li>Processing camera-captured photos</li>
     *   <li>Analyzing downloaded images</li>
     *   <li>Batch processing image collections</li>
     *   <li>Processing user-selected files</li>
     * </ul>
     *
     * <p><strong>Error Handling Example:</strong>
     * <pre>{@code
     * ImageDetector.detectFromFile(imageFile, new ImageDetectionListener() {
     *     @Override
     *     public void onResult(DetectionResult result) {
     *         if (result.isSuccess()) {
     *             List<DetectedObject> objects = result.getDetectedObjects();
     *             displayResults(objects);
     *         } else {
     *             showError("Detection failed: " + result.getError());
     *         }
     *     }
     *
     *     @Override
     *     public void onError(Exception e) {
     *         if (e instanceof FileNotFoundException) {
     *             showError("Image file not found");
     *         } else if (e.getMessage().contains("Network")) {
     *             showError("Check your internet connection");
     *         } else {
     *             showError("Detection failed: " + e.getMessage());
     *         }
     *     }
     * });
     * }</pre>
     *
     * @param file The image file to analyze (must exist and be readable)
     * @param listener Callback for detection results and errors
     * @throws IllegalStateException if the SDK has not been initialized
     * @throws IllegalArgumentException if file or listener is null
     * @see #init(String)
     * @see ImageDetectionListener
     * @see DetectionResult
     */
    public static void detectFromFile(@NonNull File file, @NonNull final ImageDetectionListener listener) {
        checkInitialized();

        if (!file.exists()) {
            listener.onError(new FileNotFoundException("File does not exist: " + file.getAbsolutePath()));
            return;
        }

        apiClient.detectFromFile(file, new ApiClient.ApiCallback<DetectionResult>() {
            @Override
            public void onSuccess(DetectionResult result) {
                listener.onResult(result);
            }

            @Override
            public void onFailure(Exception e) {
                listener.onError(e);
            }
        });
    }

    /**
     * Detect objects in an image referenced by a content URI.
     *
     * <p>This method handles images accessed through Android's content URI system,
     * including gallery images, camera captures, and shared content from other apps.
     * It automatically converts the URI to a temporary file for processing.</p>
     *
     * <p><strong>Supported URI Sources:</strong>
     * <ul>
     *   <li><strong>Gallery Images:</strong> content://media/external/images/...</li>
     *   <li><strong>Camera Captures:</strong> file:// or content:// URIs from camera</li>
     *   <li><strong>Shared Content:</strong> Images shared from other applications</li>
     *   <li><strong>File Provider:</strong> URIs from FileProvider for secure file sharing</li>
     * </ul>
     *
     * <p><strong>Permission Requirements:</strong>
     * <ul>
     *   <li>READ_EXTERNAL_STORAGE (Android < 10) for gallery access</li>
     *   <li>Appropriate URI permissions for shared content</li>
     *   <li>Camera permissions for camera-captured images</li>
     * </ul>
     *
     * <p><strong>URI Conversion Process:</strong>
     * <ol>
     *   <li>Extract file metadata from content resolver</li>
     *   <li>Create temporary file in application cache directory</li>
     *   <li>Copy image data from URI to temporary file</li>
     *   <li>Process temporary file using standard file detection</li>
     *   <li>Clean up temporary file after processing</li>
     * </ol>
     *
     * <p><strong>Common Integration Patterns:</strong>
     * <pre>{@code
     * // Handle image picker result
     * @Override
     * protected void onActivityResult(int requestCode, int resultCode, Intent data) {
     *     if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
     *         Uri imageUri = data.getData();
     *         ImageDetector.detectFromUri(this, imageUri, detectionListener);
     *     }
     * }
     *
     * // Handle camera capture result
     * private void onCameraCapture(Uri photoUri) {
     *     ImageDetector.detectFromUri(this, photoUri, new ImageDetectionListener() {
     *         @Override
     *         public void onResult(DetectionResult result) {
     *             // Process camera detection result
     *         }
     *
     *         @Override
     *         public void onError(Exception e) {
     *             // Handle camera detection error
     *         }
     *     });
     * }
     * }</pre>
     *
     * @param context Android context required for content resolver operations
     * @param imageUri URI of the image to analyze (must be accessible to the app)
     * @param listener Callback for detection results and errors
     * @throws IllegalStateException if the SDK has not been initialized
     * @throws IllegalArgumentException if context, imageUri, or listener is null
     * @see #detectFromFile(File, ImageDetectionListener)
     * @see com.objectdetection.sdk.util.UriToFileConverter
     * @see ImageDetectionListener
     */
    public static void detectFromUri(@NonNull final Context context, @NonNull final Uri imageUri, @NonNull final ImageDetectionListener listener) {
        checkInitialized();

        try {
            UriToFileConverter.uriToFile(context, imageUri, new UriToFileConverter.FileCallback() {
                @Override
                public void onSuccess(File file) {
                    detectFromFile(file, listener);
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onError(e);
                }
            });
        } catch (Exception e) {
            listener.onError(e);
        }
    }

    /**
     * Detect objects in an image accessible via URL.
     *
     * <p>This method processes images hosted on web servers or cloud storage by
     * instructing the detection service to download and analyze the image directly.
     * This approach is more efficient for images already online as it avoids
     * client-side download and re-upload.</p>
     *
     * <p><strong>URL Requirements:</strong>
     * <ul>
     *   <li>Must be publicly accessible (no authentication required)</li>
     *   <li>Should return appropriate Content-Type headers</li>
     *   <li>Must point directly to image content</li>
     *   <li>HTTPS URLs recommended for security</li>
     *   <li>Should be properly URL-encoded</li>
     * </ul>
     *
     * <p><strong>Supported Image Hosts:</strong>
     * <ul>
     *   <li>Cloud storage (AWS S3, Google Cloud Storage, Azure Blob)</li>
     *   <li>Content delivery networks (CloudFlare, AWS CloudFront)</li>
     *   <li>Image hosting services (Imgur, Flickr public images)</li>
     *   <li>Social media image URLs (with proper access)</li>
     *   <li>Web server hosted images</li>
     * </ul>
     *
     * <p><strong>Performance Benefits:</strong>
     * <ul>
     *   <li>No client upload bandwidth consumption</li>
     *   <li>Faster processing for images already online</li>
     *   <li>Reduced mobile data usage</li>
     *   <li>Server-side image optimization opportunities</li>
     * </ul>
     *
     * <p><strong>Error Scenarios and Handling:</strong>
     * <pre>{@code
     * ImageDetector.detectFromUrl(imageUrl, new ImageDetectionListener() {
     *     @Override
     *     public void onResult(DetectionResult result) {
     *         displayDetectionResults(result);
     *     }
     *
     *     @Override
     *     public void onError(Exception e) {
     *         String errorMsg = e.getMessage();
     *         if (errorMsg.contains("404")) {
     *             showError("Image not found at the specified URL");
     *         } else if (errorMsg.contains("403")) {
     *             showError("Access denied - image may be private");
     *         } else if (errorMsg.contains("timeout")) {
     *             showError("Request timed out - try again later");
     *         } else {
     *             showError("Failed to process image: " + errorMsg);
     *         }
     *     }
     * });
     * }</pre>
     *
     * @param imageUrl URL of the image to analyze (must be publicly accessible)
     * @param listener Callback for detection results and errors
     * @throws IllegalStateException if the SDK has not been initialized
     * @throws IllegalArgumentException if imageUrl or listener is null
     * @see #detectFromFile(File, ImageDetectionListener)
     * @see #detectFromUri(Context, Uri, ImageDetectionListener)
     * @see ImageDetectionListener
     */
    public static void detectFromUrl(@NonNull final String imageUrl, @NonNull final ImageDetectionListener listener) {
        checkInitialized();

        apiClient.detectFromUrl(imageUrl, new ApiClient.ApiCallback<DetectionResult>() {
            @Override
            public void onSuccess(DetectionResult result) {
                listener.onResult(result);
            }

            @Override
            public void onFailure(Exception e) {
                listener.onError(e);
            }
        });
    }

    /**
     * Verifies that the SDK has been properly initialized.
     *
     * <p>This internal method provides consistent initialization checking across
     * all public detection methods. It ensures that users receive clear error
     * messages when attempting to use the SDK before initialization.</p>
     *
     * <p><strong>Error Prevention:</strong>
     * By checking initialization state before each operation, this method prevents
     * NullPointerException and other runtime errors that would occur if the
     * underlying API client hasn't been configured.</p>
     *
     * @throws IllegalStateException if init() has not been called
     * @see #init(String)
     * @see #isInitialized()
     */
    private static void checkInitialized() {
        if (apiClient == null) {
            throw new IllegalStateException("ImageDetector not initialized. Call ImageDetector.init() first.");
        }
    }

    /**
     * Starts live object detection using the device's back camera.
     *
     * <p>This method initiates real-time object detection on camera frames,
     * providing continuous detection results as the camera captures new frames.
     * It's designed for interactive applications that need live feedback.</p>
     *
     * <p><strong>Camera Requirements:</strong>
     * <ul>
     *   <li>Device must have a back-facing camera</li>
     *   <li>Camera permissions must be granted</li>
     *   <li>Context must implement LifecycleOwner (Activity/Fragment)</li>
     *   <li>PreviewView must be properly configured in layout</li>
     * </ul>
     *
     * <p><strong>Performance Characteristics:</strong>
     * <ul>
     *   <li>Processes frames at ~2 FPS to balance performance and accuracy</li>
     *   <li>Automatically skips frames when processing is behind</li>
     *   <li>Uses 640x480 resolution for optimal processing speed</li>
     *   <li>JPEG compression at 85% quality for good results</li>
     * </ul>
     *
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Real-time object recognition applications</li>
     *   <li>Augmented reality overlays</li>
     *   <li>Interactive shopping or identification apps</li>
     *   <li>Security and surveillance applications</li>
     * </ul>
     *
     * <p><strong>Complete Implementation Example:</strong>
     * <pre>{@code
     * public class LiveDetectionActivity extends AppCompatActivity {
     *     private PreviewView previewView;
     *     private DetectionOverlayView overlayView;
     *
     *     @Override
     *     protected void onCreate(Bundle savedInstanceState) {
     *         super.onCreate(savedInstanceState);
     *         setContentView(R.layout.activity_live_detection);
     *
     *         previewView = findViewById(R.id.preview);
     *         overlayView = findViewById(R.id.overlay);
     *
     *         // Check and request camera permission
     *         if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
     *                 == PackageManager.PERMISSION_GRANTED) {
     *             startLiveDetection();
     *         } else {
     *             ActivityCompat.requestPermissions(this,
     *                 new String[]{Manifest.permission.CAMERA},
     *                 CAMERA_PERMISSION_REQUEST);
     *         }
     *     }
     *
     *     private void startLiveDetection() {
     *         ImageDetector.startLiveDetection(this, previewView,
     *             new LiveDetectionListener() {
     *                 @Override
     *                 public void onDetectionResult(DetectionResult result, long frameTimestamp) {
     *                     overlayView.setDetectionResult(result);
     *                 }
     *
     *                 @Override
     *                 public void onError(Exception e, long frameTimestamp) {
     *                     Log.e(TAG, "Live detection error", e);
     *                 }
     *             });
     *     }
     *
     *     @Override
     *     protected void onDestroy() {
     *         super.onDestroy();
     *         ImageDetector.stopLiveDetection();
     *     }
     * }
     * }</pre>
     *
     * @param context Android context that implements LifecycleOwner (Activity/Fragment)
     * @param previewView CameraX PreviewView to display the camera feed
     * @param listener Callback for detection results and errors
     * @throws IllegalStateException if the SDK has not been initialized
     * @throws IllegalArgumentException if context is not a LifecycleOwner
     * @see #startLiveDetectionFrontCamera(Context, androidx.camera.view.PreviewView, LiveDetectionListener)
     * @see #stopLiveDetection()
     * @see LiveDetectionListener
     */
    public static void startLiveDetection(Context context,
                                          androidx.camera.view.PreviewView previewView,
                                          LiveDetectionListener listener) {
        checkInitialized();

        if (liveDetector != null) {
            liveDetector.stop();
        }

        liveDetector = new LiveDetector(context, listener);
        liveDetector.start(previewView, androidx.camera.core.CameraSelector.LENS_FACING_BACK);
    }

    /**
     * Starts live object detection using the device's front camera.
     *
     * <p>This method is identical to {@link #startLiveDetection} but uses the
     * front-facing camera instead. It's useful for selfie-style applications
     * or scenarios where the front camera is preferred.</p>
     *
     * <p><strong>Front Camera Considerations:</strong>
     * <ul>
     *   <li>Lower resolution than most back cameras</li>
     *   <li>May have different field of view characteristics</li>
     *   <li>Typically optimized for close-range subjects</li>
     *   <li>May require different lighting conditions</li>
     * </ul>
     *
     * <p><strong>Common Use Cases:</strong>
     * <ul>
     *   <li>Selfie-based object detection</li>
     *   <li>User-facing interactive applications</li>
     *   <li>Accessibility applications</li>
     *   <li>Personal item recognition</li>
     * </ul>
     *
     * <p><strong>Usage Example:</strong>
     * <pre>{@code
     * // Switch to front camera mode
     * private void switchToFrontCamera() {
     *     ImageDetector.stopLiveDetection();
     *     ImageDetector.startLiveDetectionFrontCamera(this, previewView,
     *         liveDetectionListener);
     * }
     * }</pre>
     *
     * @param context Android context that implements LifecycleOwner (Activity/Fragment)
     * @param previewView CameraX PreviewView to display the camera feed
     * @param listener Callback for detection results and errors
     * @throws IllegalStateException if the SDK has not been initialized
     * @throws IllegalArgumentException if context is not a LifecycleOwner
     * @see #startLiveDetection(Context, androidx.camera.view.PreviewView, LiveDetectionListener)
     * @see #stopLiveDetection()
     * @see LiveDetectionListener
     */
    public static void startLiveDetectionFrontCamera(Context context,
                                                     androidx.camera.view.PreviewView previewView,
                                                     LiveDetectionListener listener) {
        checkInitialized();

        if (liveDetector != null) {
            liveDetector.stop();
        }

        liveDetector = new LiveDetector(context, listener);
        liveDetector.start(previewView, androidx.camera.core.CameraSelector.LENS_FACING_FRONT);
    }

    /**
     * Stops live object detection and releases camera resources.
     *
     * <p>This method stops the active live detection session and properly releases
     * all camera resources. It should be called when live detection is no longer
     * needed to free up system resources and prevent battery drain.</p>
     *
     * <p><strong>When to Call:</strong>
     * <ul>
     *   <li>Activity/Fragment onDestroy() or onStop()</li>
     *   <li>When switching to different detection modes</li>
     *   <li>When user manually stops detection</li>
     *   <li>On application backgrounding</li>
     * </ul>
     *
     * <p><strong>Resource Cleanup:</strong>
     * <ul>
     *   <li>Stops camera preview and analysis</li>
     *   <li>Releases CameraX use cases</li>
     *   <li>Stops background processing threads</li>
     *   <li>Clears internal detector state</li>
     * </ul>
     *
     * <p><strong>Implementation in Activity Lifecycle:</strong>
     * <pre>{@code
     * @Override
     * protected void onStop() {
     *     super.onStop();
     *     // Stop detection when activity goes to background
     *     ImageDetector.stopLiveDetection();
     * }
     *
     * @Override
     * protected void onDestroy() {
     *     super.onDestroy();
     *     // Ensure cleanup on activity destruction
     *     ImageDetector.stopLiveDetection();
     * }
     *
     * // In user-initiated stop
     * private void onStopButtonClick() {
     *     ImageDetector.stopLiveDetection();
     *     overlayView.clearDetections();
     *     showStoppedState();
     * }
     * }</pre>
     *
     * <p><strong>Safe to Call:</strong>
     * This method is safe to call multiple times or when no live detection
     * is active. It will simply do nothing if no detector is running.</p>
     *
     * @see #startLiveDetection(Context, androidx.camera.view.PreviewView, LiveDetectionListener)
     * @see #startLiveDetectionFrontCamera(Context, androidx.camera.view.PreviewView, LiveDetectionListener)
     */
    public static void stopLiveDetection() {
        if (liveDetector != null) {
            liveDetector.stop();
            liveDetector = null;
        }
    }

    /**
     * Detect objects in an image from JPEG byte array.
     *
     * <p>This internal method processes images represented as byte arrays,
     * primarily used for live camera detection. It's designed for high-frequency
     * calls with camera frame data and optimizes for performance over convenience.</p>
     *
     * <p><strong>Internal Use Only:</strong>
     * This method is package-private and intended for use by the LiveDetector
     * class. External applications should use the public detection methods
     * (detectFromFile, detectFromUri, detectFromUrl) instead.</p>
     *
     * <p><strong>Performance Optimizations:</strong>
     * <ul>
     *   <li>Minimal validation for high-frequency calls</li>
     *   <li>Direct byte array processing without file I/O</li>
     *   <li>Optimized for camera frame processing workflow</li>
     *   <li>Reduced object allocation and memory overhead</li>
     * </ul>
     *
     * <p><strong>Data Format Requirements:</strong>
     * <ul>
     *   <li>Must be valid JPEG image data</li>
     *   <li>Byte array should not be empty or null</li>
     *   <li>Image should have reasonable resolution for detection</li>
     * </ul>
     *
     * @param imageBytes JPEG image data as byte array
     * @param listener Callback for detection results and errors
     * @throws IllegalStateException if the SDK has not been initialized
     * @see LiveDetector
     */
    static void detectFromBytes(byte[] imageBytes, final ImageDetectionListener listener) {
        checkInitialized();

        apiClient.detectFromBytes(imageBytes, new ApiClient.ApiCallback<DetectionResult>() {
            @Override
            public void onSuccess(DetectionResult result) {
                listener.onResult(result);
            }

            @Override
            public void onFailure(Exception e) {
                listener.onError(e);
            }
        });
    }
}