package com.objectdetection.sdk.listener;

import com.objectdetection.sdk.model.DetectionResult;

/**
 * Listener interface for static image detection operations.
 *
 * <p>This interface defines the callback contract for receiving results from
 * single-image object detection operations. It provides a clean separation between
 * the detection process and result handling, enabling asynchronous processing
 * without blocking the UI thread.</p>
 *
 * <p><strong>Detection Scenarios:</strong>
 * This listener is used for all static image detection operations:
 * <ul>
 *   <li><strong>File Detection:</strong> Processing local image files</li>
 *   <li><strong>URI Detection:</strong> Processing images from content URIs</li>
 *   <li><strong>URL Detection:</strong> Processing images from web URLs</li>
 *   <li><strong>Builder Pattern:</strong> Used with DetectorBuilder for fluent API</li>
 * </ul>
 *
 * <p><strong>Callback Threading:</strong>
 * All callback methods are executed on the main thread, making it safe to update
 * UI elements directly from the callback methods without additional thread
 * synchronization.</p>
 *
 * <p><strong>Error Handling Philosophy:</strong>
 * The interface follows a clear success/failure pattern where exactly one of the
 * two methods will be called for each detection operation. This ensures predictable
 * behavior and simplifies error handling logic.</p>
 *
 * <p><strong>Basic Implementation Example:</strong>
 * <pre>{@code
 * ImageDetectionListener listener = new ImageDetectionListener() {
 *     @Override
 *     public void onResult(DetectionResult result) {
 *         if (result.isSuccess() && !result.getDetectedObjects().isEmpty()) {
 *             // Display detection results
 *             showResults(result.getDetectedObjects());
 *             updateStats(result.getProcessingTimeMs());
 *         } else {
 *             // Handle case where detection succeeded but found no objects
 *             showMessage("No objects detected in the image");
 *         }
 *     }
 *
 *     @Override
 *     public void onError(Exception e) {
 *         // Log error for debugging
 *         Log.e(TAG, "Detection failed", e);
 *
 *         // Show user-friendly error message
 *         showError("Could not analyze image: " + e.getMessage());
 *
 *         // Reset UI state
 *         hideLoadingIndicator();
 *     }
 * };
 * }</pre>
 *
 * <p><strong>Advanced Implementation with Error Categorization:</strong>
 * <pre>{@code
 * ImageDetectionListener advancedListener = new ImageDetectionListener() {
 *     @Override
 *     public void onResult(DetectionResult result) {
 *         hideLoadingIndicator();
 *
 *         if (result.isSuccess()) {
 *             List<DetectedObject> objects = result.getDetectedObjects();
 *
 *             // Update overlay view
 *             overlayView.setDetectionResult(result, imageWidth, imageHeight);
 *
 *             // Update statistics
 *             updateDetectionStats(objects.size(), result.getProcessingTimeMs());
 *
 *             // Log successful detection
 *             Log.i(TAG, String.format("Detected %d objects in %dms",
 *                   objects.size(), result.getProcessingTimeMs()));
 *
 *         } else {
 *             showError("Detection failed: " + result.getError());
 *         }
 *     }
 *
 *     @Override
 *     public void onError(Exception e) {
 *         hideLoadingIndicator();
 *
 *         String errorMessage;
 *         if (e instanceof FileNotFoundException) {
 *             errorMessage = "Image file not found";
 *         } else if (e.getMessage().contains("Network")) {
 *             errorMessage = "Network error - check your connection";
 *         } else if (e.getMessage().contains("timeout")) {
 *             errorMessage = "Request timed out - try again";
 *         } else {
 *             errorMessage = "Detection failed: " + e.getMessage();
 *         }
 *
 *         showErrorWithRetry(errorMessage);
 *         logError("Detection error", e);
 *     }
 * };
 * }</pre>
 *
 * <p><strong>Integration Patterns:</strong>
 * <ul>
 *   <li><strong>Activity Integration:</strong> Update UI components safely</li>
 *   <li><strong>Fragment Integration:</strong> Handle lifecycle considerations</li>
 *   <li><strong>Service Integration:</strong> Process results in background</li>
 *   <li><strong>MVVM Pattern:</strong> Update ViewModels and LiveData</li>
 * </ul>
 *
 * @author Object Detection SDK
 * @version 1.0
 * @since 1.0
 * @see DetectionResult
 * @see com.objectdetection.sdk.ImageDetector
 * @see com.objectdetection.sdk.DetectorBuilder
 */
public interface ImageDetectionListener {

    /**
     * Called when the image detection operation completes successfully.
     *
     * <p>This method is invoked on the main thread when the detection service
     * successfully processes an image and returns results. The DetectionResult
     * may contain detected objects or may indicate that no objects were found.</p>
     *
     * <p><strong>Result Validation:</strong>
     * Always check the result status before processing detected objects:
     * <pre>{@code
     * @Override
     * public void onResult(DetectionResult result) {
     *     if (result.isSuccess()) {
     *         // Process detected objects
     *         List<DetectedObject> objects = result.getDetectedObjects();
     *         if (objects != null && !objects.isEmpty()) {
     *             displayDetections(objects);
     *         } else {
     *             showMessage("No objects detected");
     *         }
     *     } else {
     *         // Handle server-side detection failure
     *         showError("Detection failed: " + result.getError());
     *     }
     * }
     * }</pre>
     *
     * <p><strong>Performance Metrics:</strong>
     * The result includes processing time information that can be used for
     * performance monitoring and user feedback:
     * <pre>{@code
     * Long processingTime = result.getProcessingTimeMs();
     * if (processingTime != null) {
     *     updatePerformanceMetrics(processingTime);
     *     Log.d(TAG, "Detection completed in " + processingTime + "ms");
     * }
     * }</pre>
     *
     * <p><strong>UI Update Guidelines:</strong>
     * <ul>
     *   <li>Hide loading indicators and progress bars</li>
     *   <li>Update result displays (overlays, lists, statistics)</li>
     *   <li>Enable user interactions that were disabled during processing</li>
     *   <li>Provide feedback about the number of objects detected</li>
     * </ul>
     *
     * <p><strong>Data Processing:</strong>
     * <ul>
     *   <li>Filter results by confidence threshold if needed</li>
     *   <li>Group or categorize detected objects</li>
     *   <li>Update application state with detection results</li>
     *   <li>Store results for later analysis or export</li>
     * </ul>
     *
     * <p><strong>Error Edge Cases:</strong>
     * Even successful callbacks should handle edge cases:
     * <ul>
     *   <li>Empty detection results (no objects found)</li>
     *   <li>Invalid bounding box coordinates</li>
     *   <li>Missing confidence scores or labels</li>
     *   <li>Unexpected object categories</li>
     * </ul>
     *
     * @param result The detection result containing detected objects and metadata (never null)
     * @see DetectionResult#isSuccess()
     * @see DetectionResult#getDetectedObjects()
     * @see DetectionResult#getProcessingTimeMs()
     */
    void onResult(DetectionResult result);

    /**
     * Called when an error occurs during the image detection process.
     *
     * <p>This method is invoked on the main thread when the detection operation
     * fails due to various reasons including network issues, file access problems,
     * or server-side errors. The exception provides detailed information about
     * the failure cause.</p>
     *
     * <p><strong>Common Error Categories:</strong>
     * <ul>
     *   <li><strong>Network Errors:</strong> Connection timeouts, no internet, server unreachable</li>
     *   <li><strong>File Access Errors:</strong> File not found, permission denied, corrupted files</li>
     *   <li><strong>Server Errors:</strong> API errors, invalid responses, service unavailable</li>
     *   <li><strong>Configuration Errors:</strong> SDK not initialized, invalid API URL</li>
     * </ul>
     *
     * <p><strong>Error Handling Best Practices:</strong>
     * <pre>{@code
     * @Override
     * public void onError(Exception e) {
     *     // Log detailed error for debugging
     *     Log.e(TAG, "Detection failed", e);
     *
     *     // Hide loading indicators
     *     hideLoadingIndicator();
     *     enableUserInteractions();
     *
     *     // Categorize error for user-friendly messages
     *     String userMessage = categorizeError(e);
     *     showErrorMessage(userMessage);
     *
     *     // Offer recovery options when appropriate
     *     if (isNetworkError(e)) {
     *         showRetryOption();
     *     } else if (isPermissionError(e)) {
     *         showPermissionGuidance();
     *     }
     * }
     *
     * private String categorizeError(Exception e) {
     *     if (e instanceof FileNotFoundException) {
     *         return "Image file could not be found";
     *     } else if (e instanceof SecurityException) {
     *         return "Permission denied accessing image";
     *     } else if (e.getMessage().contains("Network") || e.getMessage().contains("timeout")) {
     *         return "Network connection problem";
     *     } else if (e instanceof IllegalStateException && e.getMessage().contains("not initialized")) {
     *         return "Detection service not properly configured";
     *     } else {
     *         return "Unable to analyze image: " + e.getMessage();
     *     }
     * }
     * }</pre>
     *
     * <p><strong>Recovery Strategies:</strong>
     * <ul>
     *   <li><strong>Retry Logic:</strong> For transient network failures</li>
     *   <li><strong>Alternative Methods:</strong> Switch from URL to file upload</li>
     *   <li><strong>User Guidance:</strong> Help users resolve permission issues</li>
     *   <li><strong>Graceful Degradation:</strong> Offer manual alternatives</li>
     * </ul>
     *
     * <p><strong>User Experience Considerations:</strong>
     * <ul>
     *   <li>Avoid showing technical stack traces to end users</li>
     *   <li>Provide actionable guidance when possible</li>
     *   <li>Maintain application state consistency</li>
     *   <li>Log detailed errors for developer analysis</li>
     * </ul>
     *
     * <p><strong>UI State Management:</strong>
     * <ul>
     *   <li>Hide progress indicators and loading states</li>
     *   <li>Re-enable user interface controls</li>
     *   <li>Clear any partial results or intermediate states</li>
     *   <li>Reset form inputs if appropriate</li>
     * </ul>
     *
     * @param e The exception that occurred during detection (never null)
     * @see java.io.FileNotFoundException
     * @see SecurityException
     * @see RuntimeException
     */
    void onError(Exception e);
}