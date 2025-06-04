package com.objectdetection.sdk.network;

import com.objectdetection.sdk.model.DetectionResult;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Retrofit API interface for object detection endpoints.
 *
 * <p>This interface defines the HTTP endpoints available for object detection operations.
 * It uses Retrofit annotations to define the request methods, paths, and parameter types.
 * The interface supports two main types of detection requests:</p>
 *
 * <ul>
 *   <li><strong>File Upload:</strong> Detect objects in uploaded image files</li>
 *   <li><strong>URL-based:</strong> Detect objects in images accessible via URL</li>
 * </ul>
 *
 * <p>This interface is automatically implemented by Retrofit at runtime. You should
 * not implement this interface directly. Instead, create an instance using Retrofit:</p>
 *
 * <pre>{@code
 * Retrofit retrofit = new Retrofit.Builder()
 *     .baseUrl("https://api.example.com/")
 *     .addConverterFactory(GsonConverterFactory.create())
 *     .build();
 *
 * DetectionApi api = retrofit.create(DetectionApi.class);
 * }</pre>
 *
 * <p>All endpoints return asynchronous {@link Call} objects that can be executed
 * either synchronously or asynchronously. For best practices, always use
 * asynchronous execution in Android applications to avoid blocking the UI thread.</p>
 *
 * @author Object Detection SDK
 * @version 1.0
 * @since 1.0
 * @see retrofit2.Call
 * @see DetectionResult
 */
public interface DetectionApi {

    /**
     * Detect objects in an uploaded image file.
     *
     * <p>This endpoint accepts multipart form data containing an image file and
     * performs object detection on it. The image is uploaded as part of the request
     * body, making this suitable for local files or camera captures.</p>
     *
     * <p><strong>Request Details:</strong>
     * <ul>
     *   <li><strong>Method:</strong> POST</li>
     *   <li><strong>Endpoint:</strong> /api/detect</li>
     *   <li><strong>Content-Type:</strong> multipart/form-data</li>
     *   <li><strong>Parameter Name:</strong> image</li>
     * </ul>
     *
     * <p><strong>Supported Image Formats:</strong>
     * <ul>
     *   <li>JPEG (.jpg, .jpeg)</li>
     *   <li>PNG (.png)</li>
     *   <li>BMP (.bmp)</li>
     *   <li>Other formats as supported by the server</li>
     * </ul>
     *
     * <p><strong>Usage Example:</strong>
     * <pre>{@code
     * File imageFile = new File("/path/to/image.jpg");
     * RequestBody requestFile = RequestBody.create(imageFile, MediaType.parse("image/*"));
     * MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image",
     *                                                                   imageFile.getName(),
     *                                                                   requestFile);
     *
     * Call<DetectionResult> call = api.detectFromFile(imagePart);
     * call.enqueue(new Callback<DetectionResult>() {
     *     @Override
     *     public void onResponse(Call<DetectionResult> call, Response<DetectionResult> response) {
     *         if (response.isSuccessful()) {
     *             DetectionResult result = response.body();
     *             // Process the detection result
     *         }
     *     }
     *
     *     @Override
     *     public void onFailure(Call<DetectionResult> call, Throwable t) {
     *         // Handle the error
     *     }
     * });
     * }</pre>
     *
     * @param image The image file as multipart data with form field name "image"
     * @return Call containing DetectionResult with detected objects and metadata
     * @see DetectionResult
     * @see MultipartBody.Part
     */
    @Multipart
    @POST("api/detect")
    Call<DetectionResult> detectFromFile(@Part MultipartBody.Part image);

    /**
     * Detect objects in an image from URL.
     *
     * <p>This endpoint accepts a JSON request body containing an image URL and
     * performs object detection on the image located at that URL. The server
     * will download the image from the provided URL and process it.</p>
     *
     * <p><strong>Request Details:</strong>
     * <ul>
     *   <li><strong>Method:</strong> POST</li>
     *   <li><strong>Endpoint:</strong> /api/detect/url</li>
     *   <li><strong>Content-Type:</strong> application/json</li>
     *   <li><strong>Request Body:</strong> JSON with "url" field</li>
     * </ul>
     *
     * <p><strong>URL Requirements:</strong>
     * <ul>
     *   <li>Must be publicly accessible (no authentication required)</li>
     *   <li>Should point to a valid image file</li>
     *   <li>HTTPS URLs are recommended for security</li>
     *   <li>Large images may increase processing time</li>
     * </ul>
     *
     * <p><strong>Request Body Format:</strong>
     * <pre>{@code
     * {
     *   "url": "https://example.com/image.jpg"
     * }
     * }</pre>
     *
     * <p><strong>Usage Example:</strong>
     * <pre>{@code
     * UrlRequest urlRequest = new UrlRequest("https://example.com/image.jpg");
     * String jsonBody = gson.toJson(urlRequest);
     * RequestBody requestBody = RequestBody.create(jsonBody,
     *                                              MediaType.parse("application/json"));
     *
     * Call<DetectionResult> call = api.detectFromUrl(requestBody);
     * call.enqueue(new Callback<DetectionResult>() {
     *     @Override
     *     public void onResponse(Call<DetectionResult> call, Response<DetectionResult> response) {
     *         if (response.isSuccessful()) {
     *             DetectionResult result = response.body();
     *             // Process the detection result
     *         }
     *     }
     *
     *     @Override
     *     public void onFailure(Call<DetectionResult> call, Throwable t) {
     *         // Handle the error
     *     }
     * });
     * }</pre>
     *
     * @param urlRequest JSON request body containing the image URL in the format {"url": "..."}
     * @return Call containing DetectionResult with detected objects and metadata
     * @see DetectionResult
     * @see com.objectdetection.sdk.model.UrlRequest
     * @see RequestBody
     */
    @POST("api/detect/url")
    Call<DetectionResult> detectFromUrl(@Body RequestBody urlRequest);
}