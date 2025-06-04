package com.objectdetection.sdk.model;

import java.util.List;

/**
 * Represents the complete result of an object detection operation.
 *
 * <p>This class encapsulates all information returned from a detection request,
 * including the detected objects, processing metadata, and any error information.
 * It serves as the primary response object for all detection APIs.</p>
 *
 * <p>A detection result can be in one of two states:
 * <ul>
 *   <li><strong>Success:</strong> Contains a list of detected objects and processing time</li>
 *   <li><strong>Error:</strong> Contains an error message explaining what went wrong</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * DetectionResult result = // ... obtained from detection API
 *
 * if (result.isSuccess()) {
 *     List<DetectedObject> objects = result.getDetectedObjects();
 *     System.out.println("Found " + objects.size() + " objects in " +
 *                       result.getProcessingTimeMs() + "ms");
 * } else {
 *     System.err.println("Detection failed: " + result.getError());
 * }
 * }</pre>
 *
 * @author Object Detection SDK
 * @version 1.0
 * @since 1.0
 */
public class DetectionResult {
    private String imageUrl;
    private List<DetectedObject> detectedObjects;
    private String error;
    private Long processingTimeMs;

    /**
     * Default constructor that creates an empty detection result.
     * All properties will be initialized to null.
     *
     * <p>This constructor is typically used by JSON deserialization frameworks
     * or when building a result programmatically.</p>
     */
    public DetectionResult() {
    }

    /**
     * Gets the URL of the image that was processed.
     *
     * <p>This field is populated when the detection was performed on an image
     * specified by URL. For file-based or byte array detections, this may be null.</p>
     *
     * @return the URL of the processed image, or null if not applicable
     * @see #setImageUrl(String)
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets the URL of the image that was processed.
     *
     * @param imageUrl the URL of the image that was processed
     * @see #getImageUrl()
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Gets the list of objects detected in the image.
     *
     * <p>This list contains all objects that were successfully detected and passed
     * the confidence threshold. The list may be empty if no objects were detected,
     * but will be null only in case of an error.</p>
     *
     * @return the list of detected objects, or null if detection failed
     * @see #setDetectedObjects(List)
     * @see DetectedObject
     */
    public List<DetectedObject> getDetectedObjects() {
        return detectedObjects;
    }

    /**
     * Sets the list of objects detected in the image.
     *
     * @param detectedObjects the list of detected objects
     * @see #getDetectedObjects()
     * @see DetectedObject
     */
    public void setDetectedObjects(List<DetectedObject> detectedObjects) {
        this.detectedObjects = detectedObjects;
    }

    /**
     * Gets the error message if the detection operation failed.
     *
     * <p>This field is populated only when an error occurs during detection.
     * Common error scenarios include:
     * <ul>
     *   <li>Invalid image format or corrupted image data</li>
     *   <li>Network connectivity issues</li>
     *   <li>Server-side processing errors</li>
     *   <li>Authentication or authorization failures</li>
     * </ul>
     *
     * @return the error message, or null if the operation was successful
     * @see #setError(String)
     * @see #isSuccess()
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error message for a failed detection operation.
     *
     * @param error the error message describing what went wrong
     * @see #getError()
     * @see #isSuccess()
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Gets the processing time for the detection operation.
     *
     * <p>This represents the time taken by the server to process the image
     * and perform object detection, measured in milliseconds. It does not
     * include network latency or client-side processing time.</p>
     *
     * @return the processing time in milliseconds, or null if not available
     * @see #setProcessingTimeMs(Long)
     */
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    /**
     * Sets the processing time for the detection operation.
     *
     * @param processingTimeMs the processing time in milliseconds
     * @see #getProcessingTimeMs()
     */
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    /**
     * Checks if the detection operation was successful.
     *
     * <p>A detection result is considered successful if there is no error message
     * or if the error message is empty. Even successful operations may return
     * an empty list of detected objects if no objects were found in the image.</p>
     *
     * @return true if the operation was successful, false if an error occurred
     * @see #getError()
     * @see #setError(String)
     */
    public boolean isSuccess() {
        return error == null || error.isEmpty();
    }

    /**
     * Returns a string representation of the detection result.
     *
     * <p>The format includes the image URL, number of detected objects, error status,
     * and processing time for debugging and logging purposes. Example output:
     * "DetectionResult{imageUrl='...', detectedObjects=3, error='null', processingTimeMs=245}"</p>
     *
     * @return a string representation of this detection result
     */
    @Override
    public String toString() {
        return "DetectionResult{" +
                "imageUrl='" + imageUrl + '\'' +
                ", detectedObjects=" + (detectedObjects != null ? detectedObjects.size() : 0) +
                ", error='" + error + '\'' +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }
}
