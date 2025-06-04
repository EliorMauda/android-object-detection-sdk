package com.objectdetection.sdk.listener;

import com.objectdetection.sdk.model.DetectionResult;

/**
 * Listener interface for live camera object detection operations.
 *
 * <p>This interface defines the callback contract for receiving real-time detection
 * results from camera frame processing. It's designed to handle high-frequency
 * callbacks typical in live detection scenarios while providing frame timing
 * information for synchronization and performance analysis.</p>
 *
 * <p><strong>Live Detection Characteristics:</strong>
 * <ul>
 *   <li><strong>High Frequency:</strong> Called multiple times per second during active detection</li>
 *   <li><strong>Frame Timing:</strong> Each callback includes frame timestamp for synchronization</li>
 *   <li><strong>Performance Optimized:</strong> Designed for efficient real-time processing</li>
 *   <li><strong>Error Resilience:</strong> Individual frame failures don't stop the detection stream</li>
 * </ul>
 *
 * <p><strong>Typical Detection Flow:</strong>
 * <ol>
 *   <li>Camera captures frames at video rate (30 FPS)</li>
 *   <li>Detection system processes frames at reduced rate (~2 FPS)</li>
 *   <li>Each processed frame triggers onDetectionResult() callback</li>
 *   <li>Errors in individual frames trigger onError() callback</li>
 *   <li>Detection continues until explicitly stopped</li>
 * </ol>
 *
 * <p><strong>Performance Considerations:</strong>
 * <ul>
 *   <li>Callbacks are frequent - keep processing lightweight</li>
 *   <li>Avoid heavy UI updates in every callback</li>
 *   <li>Use frame timestamps to throttle expensive operations</li>
 *   <li>Consider buffering results for smoother visual updates</li>
 * </ul>
 *
 * <p><strong>Basic Implementation Example:</strong>
 * <pre>{@code
 * LiveDetectionListener listener = new LiveDetectionListener() {
 *     @Override
 *     public void onDetectionResult(DetectionResult result, long frameTimestamp) {
 *         // Update overlay with latest detection results
 *         overlayView.setDetectionResult(result);
 *
 *         // Update detection statistics (throttled)
 *         updateDetectionStats(result, frameTimestamp);
 *     }
 *
 *     @Override
 *     public void onError(Exception e, long frameTimestamp) {
 *         // Log frame processing error
 *         Log.w(TAG, "Frame processing error at " + frameTimestamp, e);
 *
 *         // Don't stop detection for individual frame errors
 *         // Detection will continue with next frames
 *     }
 * };
 * }</pre>
 *
 * <p><strong>Advanced Implementation with Throttling:</strong>
 * <pre>{@code
 * LiveDetectionListener optimizedListener = new LiveDetectionListener() {
 *     private long lastUIUpdate = 0;
 *     private static final long UI_UPDATE_INTERVAL = 100; // Update UI at most every 100ms
 *
 *     @Override
 *     public void onDetectionResult(DetectionResult result, long frameTimestamp) {
 *         // Always update overlay for smooth visual feedback
 *         overlayView.setDetectionResult(result);
 *
 *         // Throttle expensive UI updates
 *         if (frameTimestamp - lastUIUpdate > UI_UPDATE_INTERVAL) {
 *             updateStatistics(result);
 *             updateObjectList(result.getDetectedObjects());
 *             lastUIUpdate = frameTimestamp;
 *         }
 *
 *         // Log significant detections
 *         if (result.getDetectedObjects().size() > 0) {
 *             Log.d(TAG, "Detected " + result.getDetectedObjects().size() +
 *                   " objects in frame at " + frameTimestamp);
 *         }
 *     }
 *
 *     @Override
 *     public void onError(Exception e, long frameTimestamp) {
 *         // Count errors for quality monitoring
 *         errorCount++;
 *
 *         // Log errors but don't interrupt detection
 *         Log.w(TAG, "Frame " + frameTimestamp + " processing failed", e);
 *
 *         // Show warning if error rate becomes too high
 *         if (errorCount > 10) {
 *             showWarning("Detection quality may be affected");
 *             errorCount = 0; // Reset counter
 *         }
 *     }
 * };
 * }</pre>
 *
 * <p><strong>Integration with UI Components:</strong>
 * <ul>
 *   <li><strong>DetectionOverlayView:</strong> Real-time bounding box visualization</li>
 *   <li><strong>Statistics Displays:</strong> Live performance metrics</li>
 *   <li><strong>Object Lists:</strong> Current detection results</li>
 *   <li><strong>Alert Systems:</strong> Notifications for specific object types</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong>
 * All callback methods are executed on the main thread, making it safe to update
 * UI elements directly. However, consider the frequency of calls when designing
 * UI update logic to maintain smooth performance.</p>
 *
 * @author Object Detection SDK
 * @version 1.0
 * @since 1.0
 * @see DetectionResult
 * @see com.objectdetection.sdk.ImageDetector#startLiveDetection
 * @see com.objectdetection.sdk.view.DetectionOverlayView
 */
public interface LiveDetectionListener {
    /**
     * Called when a camera frame has been processed and objects detected.
     *
     * <p>This method is invoked on the main thread for each successfully processed
     * camera frame. It provides both the detection results and the timestamp of
     * when the frame was captured, enabling temporal analysis and synchronization.</p>
     *
     * <p><strong>Callback Frequency:</strong>
     * This method is called approximately 2 times per second during active detection,
     * though the exact frequency depends on processing speed and device performance.
     * Design implementations to handle this frequency efficiently.</p>
     *
     * <p><strong>Frame Timing Information:</strong>
     * The frameTimestamp parameter provides precise timing information that can be
     * used for various purposes:
     * <pre>{@code
     * @Override
     * public void onDetectionResult(DetectionResult result, long frameTimestamp) {
     *     // Calculate processing latency
     *     long processingLatency = System.currentTimeMillis() - frameTimestamp;
     *
     *     // Throttle expensive operations
     *     if (frameTimestamp - lastExpensiveOperation > THROTTLE_INTERVAL) {
     *         performExpensiveOperation(result);
     *         lastExpensiveOperation = frameTimestamp;
     *     }
     *
     *     // Synchronize with other time-based events
     *     synchronizeWithTimestamp(result, frameTimestamp);
     * }
     * }</pre>
     *
     * <p><strong>Performance Optimization Strategies:</strong>
     * <ul>
     *   <li><strong>Lightweight Updates:</strong> Keep callback processing minimal</li>
     *   <li><strong>Throttled Operations:</strong> Use frame timing to throttle expensive updates</li>
     *   <li><strong>Async Processing:</strong> Offload heavy computations to background threads</li>
     *   <li><strong>Efficient UI Updates:</strong> Update only what's necessary</li>
     * </ul>
     *
     * <p><strong>Common Usage Patterns:</strong>
     * <pre>{@code
     * // Pattern 1: Simple overlay update
     * overlayView.setDetectionResult(result);
     *
     * // Pattern 2: Conditional UI updates
     * if (!result.getDetectedObjects().isEmpty()) {
     *     updateDetectionDisplay(result);
     * }
     *
     * // Pattern 3: Throttled statistics update
     * if (shouldUpdateStats(frameTimestamp)) {
     *     updatePerformanceStats(result, frameTimestamp);
     * }
     *
     * // Pattern 4: Object tracking
     * trackDetectedObjects(result.getDetectedObjects(), frameTimestamp);
     * }</pre>
     *
     * <p><strong>Error Resilience:</strong>
     * Even though this is a success callback, consider handling edge cases:
     * <ul>
     *   <li>Empty detection results (normal occurrence)</li>
     *   <li>Invalid bounding box coordinates</li>
     *   <li>Unexpected object categories</li>
     *   <li>Missing confidence scores</li>
     * </ul>
     *
     * <p><strong>Real-time Applications:</strong>
     * For real-time applications, consider implementing:
     * <ul>
     *   <li>Object persistence tracking across frames</li>
     *   <li>Confidence-based filtering for stable results</li>
     *   <li>Motion analysis using frame timing</li>
     *   <li>Alert triggers for specific detection patterns</li>
     * </ul>
     *
     * @param result The detection result containing detected objects and metadata (never null)
     * @param frameTimestamp Timestamp of when the frame was captured (milliseconds since epoch)
     * @see DetectionResult
     * @see System#currentTimeMillis()
     */
    void onDetectionResult(DetectionResult result, long frameTimestamp);

    /**
     * Called when an error occurs during live detection frame processing.
     *
     * <p>This method is invoked on the main thread when processing of an individual
     * camera frame fails. Unlike static image detection errors, live detection errors
     * typically don't stop the detection process - the system continues processing
     * subsequent frames.</p>
     *
     * <p><strong>Error Characteristics in Live Detection:</strong>
     * <ul>
     *   <li><strong>Non-Fatal:</strong> Individual frame errors don't stop detection</li>
     *   <li><strong>Recoverable:</strong> Next frames may process successfully</li>
     *   <li><strong>Frequent:</strong> Some level of frame errors is normal</li>
     *   <li><strong>Diagnostic:</strong> Useful for monitoring detection quality</li>
     * </ul>
     *
     * <p><strong>Common Error Scenarios:</strong>
     * <ul>
     *   <li><strong>Network Issues:</strong> Temporary connectivity problems</li>
     *   <li><strong>Processing Overload:</strong> System unable to keep up with frame rate</li>
     *   <li><strong>Image Conversion Errors:</strong> Problems converting camera frames</li>
     *   <li><strong>Server Timeouts:</strong> Individual frame processing timeouts</li>
     * </ul>
     *
     * <p><strong>Error Handling Strategies:</strong>
     * <pre>{@code
     * @Override
     * public void onError(Exception e, long frameTimestamp) {
     *     // Log for debugging but don't alarm user for individual frame errors
     *     Log.d(TAG, "Frame processing error at " + frameTimestamp, e);
     *
     *     // Track error rate for quality monitoring
     *     errorTracker.recordError(frameTimestamp, e);
     *
     *     // Show user warning only if error rate becomes problematic
     *     if (errorTracker.getRecentErrorRate() > ERROR_THRESHOLD) {
     *         showQualityWarning("Detection may be affected by poor conditions");
     *     }
     *
     *     // Update error statistics for admin/debug displays
     *     if (debugMode) {
     *         updateErrorStats(e, frameTimestamp);
     *     }
     *
     *     // For critical errors that might indicate systemic problems
     *     if (isCriticalError(e)) {
     *         Log.e(TAG, "Critical detection error", e);
     *         // Consider stopping detection and alerting user
     *     }
     * }
     *
     * private boolean isCriticalError(Exception e) {
     *     return e instanceof OutOfMemoryError ||
     *            e.getMessage().contains("Camera unavailable") ||
     *            e.getMessage().contains("API key invalid");
     * }
     * }</pre>
     *
     * <p><strong>Quality Monitoring:</strong>
     * Use error callbacks to monitor detection quality:
     * <ul>
     *   <li>Track error rates over time</li>
     *   <li>Identify patterns in error types</li>
     *   <li>Correlate errors with environmental conditions</li>
     *   <li>Provide feedback to improve user experience</li>
     * </ul>
     *
     * <p><strong>User Experience Guidelines:</strong>
     * <ul>
     *   <li>Don't show error messages for every frame failure</li>
     *   <li>Aggregate errors before alerting users</li>
     *   <li>Provide actionable guidance when possible</li>
     *   <li>Maintain smooth visual experience despite errors</li>
     * </ul>
     *
     * <p><strong>Recovery and Resilience:</strong>
     * <pre>{@code
     * // Example error recovery implementation
     * private int consecutiveErrors = 0;
     * private long lastSuccessfulFrame = 0;
     *
     * @Override
     * public void onError(Exception e, long frameTimestamp) {
     *     consecutiveErrors++;
     *
     *     // If too many consecutive errors, suggest recovery actions
     *     if (consecutiveErrors > MAX_CONSECUTIVE_ERRORS) {
     *         suggestRecoveryActions();
     *         consecutiveErrors = 0; // Reset counter
     *     }
     *
     *     // Track time since last successful detection
     *     long timeSinceSuccess = frameTimestamp - lastSuccessfulFrame;
     *     if (timeSinceSuccess > MAX_TIME_WITHOUT_SUCCESS) {
     *         showConnectivityCheck();
     *     }
     * }
     *
     * @Override
     * public void onDetectionResult(DetectionResult result, long frameTimestamp) {
     *     // Reset error tracking on successful frame
     *     consecutiveErrors = 0;
     *     lastSuccessfulFrame = frameTimestamp;
     *
     *     // Process successful result...
     * }
     * }</pre>
     *
     * @param e The exception that occurred during frame processing (never null)
     * @param frameTimestamp Timestamp of when the frame was captured (milliseconds since epoch)
     * @see #onDetectionResult(DetectionResult, long)
     */
    void onError(Exception e, long frameTimestamp);
}