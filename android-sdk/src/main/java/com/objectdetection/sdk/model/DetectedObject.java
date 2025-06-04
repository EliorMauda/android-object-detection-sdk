package com.objectdetection.sdk.model;

/**
 * Represents an object detected in an image with its classification label,
 * confidence score, and bounding box coordinates.
 *
 * <p>This class encapsulates all the information about a single detected object,
 * including what it is (label), how confident the AI model is about the detection
 * (confidence), and where it's located in the image (bounding box).</p>
 *
 * <p>Confidence scores are normalized values between 0.0 and 1.0, where:
 * <ul>
 *   <li>0.0 represents no confidence (0%)</li>
 *   <li>1.0 represents maximum confidence (100%)</li>
 *   <li>Typical usable detections have confidence > 0.5 (50%)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * DetectedObject object = new DetectedObject();
 * object.setLabel("car");
 * object.setConfidence(0.87f);
 * object.setBox(boundingBox);
 *
 * System.out.println("Found: " + object.getLabel() +
 *                   " (" + object.getConfidenceAsPercentage() + ")");
 * }</pre>
 *
 * @author Object Detection SDK
 * @version 1.0
 * @since 1.0
 */
public class DetectedObject {
    private String label;
    private Float confidence;
    private BoundingBox box;

    /**
     * Default constructor that creates an empty detected object.
     * All properties will be initialized to null.
     *
     * <p>After creation, you should set the label, confidence, and bounding box
     * using the appropriate setter methods.</p>
     */
    public DetectedObject() {
    }

    /**
     * Gets the classification label of the detected object.
     *
     * <p>The label represents the type of object that was detected, such as
     * "person", "car", "dog", etc. Labels are determined by the AI model
     * used for object detection.</p>
     *
     * @return the object classification label, or null if not set
     * @see #setLabel(String)
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the classification label of the detected object.
     *
     * @param label the object classification label (e.g., "person", "car", "dog")
     * @see #getLabel()
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the confidence score of the detection.
     *
     * <p>The confidence score indicates how certain the AI model is about
     * this detection. Higher values indicate greater confidence.</p>
     *
     * @return the confidence score as a Float between 0.0 and 1.0, or null if not set
     * @see #setConfidence(Float)
     * @see #getConfidenceAsPercentage()
     */
    public Float getConfidence() {
        return confidence;
    }

    /**
     * Sets the confidence score of the detection.
     *
     * @param confidence the confidence score (should be between 0.0 and 1.0)
     * @see #getConfidence()
     * @see #getConfidenceAsPercentage()
     */
    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }

    /**
     * Gets the bounding box coordinates of the detected object.
     *
     * <p>The bounding box defines the rectangular region in the image where
     * the object was detected. Coordinates are normalized to [0.0, 1.0] range.</p>
     *
     * @return the bounding box of the detected object, or null if not set
     * @see #setBox(BoundingBox)
     * @see BoundingBox
     */
    public BoundingBox getBox() {
        return box;
    }

    /**
     * Sets the bounding box coordinates of the detected object.
     *
     * @param box the bounding box defining the object's location in the image
     * @see #getBox()
     * @see BoundingBox
     */
    public void setBox(BoundingBox box) {
        this.box = box;
    }

    /**
     * Returns a formatted string of the confidence as a percentage.
     *
     * <p>This is a convenience method that converts the confidence score
     * from a decimal (0.0-1.0) to a percentage string with one decimal place.
     * Useful for displaying confidence in user interfaces.</p>
     *
     * <p>Examples:
     * <ul>
     *   <li>confidence = 0.855f → returns "85.5%"</li>
     *   <li>confidence = 0.9f → returns "90.0%"</li>
     *   <li>confidence = null → returns "N/A"</li>
     * </ul>
     *
     * @return Confidence as a percentage string (e.g., "95.5%"), or "N/A" if confidence is null
     * @see #getConfidence()
     * @see #setConfidence(Float)
     */
    public String getConfidenceAsPercentage() {
        if (confidence == null) return "N/A";
        return String.format("%.1f%%", confidence * 100);
    }

    /**
     * Returns a string representation of the detected object.
     *
     * <p>The format includes the label, confidence as percentage, and bounding box
     * for debugging and logging purposes. Example output:
     * "DetectedObject{label='car', confidence=87.5%, box=BoundingBox{...}}"</p>
     *
     * @return a string representation of this detected object
     */
    @Override
    public String toString() {
        return "DetectedObject{" +
                "label='" + label + '\'' +
                ", confidence=" + getConfidenceAsPercentage() +
                ", box=" + box +
                '}';
    }
}
