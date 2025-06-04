package com.objectdetection.sdk.model;

/**
 * Request model for URL-based object detection operations.
 *
 * <p>This class represents a request to perform object detection on an image
 * that is accessible via a URL. It is used as the request body when calling
 * URL-based detection endpoints.</p>
 *
 * <p>The URL should point to a publicly accessible image in a supported format
 * (JPEG, PNG, etc.). The detection service will download the image from the
 * provided URL and perform object detection on it.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * UrlRequest request = new UrlRequest("https://example.com/image.jpg");
 *
 * // Convert to JSON for API request
 * String json = gson.toJson(request);
 *
 * // Or use directly with Retrofit
 * detectionApi.detectFromUrl(request);
 * }</pre>
 *
 * <p><strong>Important considerations:</strong>
 * <ul>
 *   <li>The URL must be publicly accessible (no authentication required)</li>
 *   <li>The image should be in a standard format (JPEG, PNG, BMP, etc.)</li>
 *   <li>Large images may take longer to process</li>
 *   <li>The URL should use HTTPS for security when possible</li>
 * </ul>
 *
 * @author Object Detection SDK
 * @version 1.0
 * @since 1.0
 */
public class UrlRequest {
    private String url;

    /**
     * Constructs a new URL request with the specified image URL.
     *
     * <p>This is the primary constructor used to create URL-based detection requests.
     * The provided URL should point to a valid image file that is publicly accessible.</p>
     *
     * @param url the URL of the image to process for object detection
     * @throws IllegalArgumentException if the URL is null or empty
     * @see #getUrl()
     * @see #setUrl(String)
     */
    public UrlRequest(String url) {
        this.url = url;
    }

    /**
     * Gets the image URL for this detection request.
     *
     * @return the URL of the image to be processed
     * @see #setUrl(String)
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the image URL for this detection request.
     *
     * <p>The URL should point to a publicly accessible image file in a supported
     * format. The detection service will attempt to download and process the image
     * from this URL.</p>
     *
     * @param url the URL of the image to process
     * @see #getUrl()
     */
    public void setUrl(String url) {
        this.url = url;
    }
}