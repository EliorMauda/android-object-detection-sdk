package com.objectdetection.sdk.network;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.objectdetection.sdk.model.DetectionResult;
import com.objectdetection.sdk.model.UrlRequest;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit-based API client for object detection services.
 *
 * <p>This class provides a high-level interface for communicating with object detection
 * APIs. It handles HTTP requests, response parsing, error handling, and provides
 * convenient methods for different types of detection operations.</p>
 *
 * <p>The client supports three main detection methods:
 * <ul>
 *   <li><strong>File Upload:</strong> Upload local image files</li>
 *   <li><strong>URL Detection:</strong> Process images from public URLs</li>
 *   <li><strong>Byte Array:</strong> Process images from memory (byte arrays)</li>
 * </ul>
 *
 * <p>The client is built on top of Retrofit and OkHttp, providing robust networking
 * capabilities with automatic JSON serialization/deserialization, connection pooling,
 * and configurable timeouts.</p>
 *
 * <p><strong>Configuration:</strong>
 * <ul>
 *   <li>Connect timeout: 30 seconds</li>
 *   <li>Read timeout: 30 seconds</li>
 *   <li>Write timeout: 30 seconds</li>
 *   <li>JPEG quality for byte arrays: 85%</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * ApiClient client = new ApiClient("https://api.example.com/");
 *
 * File imageFile = new File("/path/to/image.jpg");
 * client.detectFromFile(imageFile, new ApiClient.ApiCallback<DetectionResult>() {
 *     @Override
 *     public void onSuccess(DetectionResult result) {
 *         // Handle successful detection
 *         System.out.println("Found " + result.getDetectedObjects().size() + " objects");
 *     }
 *
 *     @Override
 *     public void onFailure(Exception e) {
 *         // Handle error
 *         System.err.println("Detection failed: " + e.getMessage());
 *     }
 * });
 * }</pre>
 *
 * @author Object Detection SDK
 * @version 1.0
 * @since 1.0
 * @see DetectionApi
 * @see DetectionResult
 */
public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final MediaType MEDIA_TYPE_IMAGE = MediaType.parse("image/*");
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    private final DetectionApi detectionApi;
    private final Gson gson;

    /**
     * Constructs a new ApiClient with the specified base URL.
     *
     * <p>This constructor initializes the HTTP client with standard configurations
     * including timeouts, JSON conversion, and the Retrofit interface. The base URL
     * should include the protocol (http/https) and may include a path prefix.</p>
     *
     * <p><strong>Example base URLs:</strong>
     * <ul>
     *   <li>https://api.example.com/</li>
     *   <li>https://api.example.com/v1/</li>
     *   <li>http://localhost:8080/detection/</li>
     * </ul>
     *
     * @param baseUrl The base URL of the object detection API (must include protocol)
     * @throws IllegalArgumentException if baseUrl is null or malformed
     */
    public ApiClient(String baseUrl) {
        // Create Gson instance
        this.gson = new GsonBuilder()
                .setLenient()
                .create();

        // Create OkHttp client with timeouts
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // Create Retrofit instance (similar to your MoviesController)
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        // Create API interface instance
        this.detectionApi = retrofit.create(DetectionApi.class);
    }

    /**
     * Detects objects in an image file using multipart file upload.
     *
     * <p>This method uploads a local image file to the detection service and
     * returns the results asynchronously through the provided callback. The file
     * must exist and be readable.</p>
     *
     * <p><strong>Supported file types:</strong>
     * <ul>
     *   <li>JPEG (.jpg, .jpeg)</li>
     *   <li>PNG (.png)</li>
     *   <li>BMP (.bmp)</li>
     *   <li>Other image formats as supported by the server</li>
     * </ul>
     *
     * <p><strong>Performance considerations:</strong>
     * <ul>
     *   <li>Large files will take longer to upload and process</li>
     *   <li>Consider resizing very large images before detection</li>
     *   <li>Network speed affects upload time</li>
     * </ul>
     *
     * <p><strong>Error scenarios:</strong>
     * <ul>
     *   <li>File does not exist or is not readable</li>
     *   <li>File is not a valid image format</li>
     *   <li>Network connectivity issues</li>
     *   <li>Server-side processing errors</li>
     * </ul>
     *
     * @param file The image file to analyze (must exist and be readable)
     * @param callback Callback interface to receive the detection result or error
     * @throws IllegalArgumentException if file is null
     * @see ApiCallback
     * @see DetectionResult
     */
    public void detectFromFile(File file, final ApiCallback<DetectionResult> callback) {
        if (file == null || !file.exists()) {
            callback.onFailure(new IllegalArgumentException("Invalid file"));
            return;
        }

        // Create multipart body part for the image
        RequestBody requestFile = RequestBody.create(file, MEDIA_TYPE_IMAGE);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

        // Make API call using Retrofit
        Call<DetectionResult> call = detectionApi.detectFromFile(imagePart);
        call.enqueue(new Callback<DetectionResult>() {
            @Override
            public void onResponse(Call<DetectionResult> call, Response<DetectionResult> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Detection successful: " + response.body());
                    callback.onSuccess(response.body());
                } else {
                    Log.e(TAG, "Detection failed: " + response.message());
                    callback.onFailure(new RuntimeException("API Error: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<DetectionResult> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                callback.onFailure(new RuntimeException("Network Error: " + t.getMessage()));
            }
        });
    }

    /**
     * Detects objects in an image from a publicly accessible URL.
     *
     * <p>This method instructs the detection service to download an image from
     * the specified URL and perform object detection on it. The URL must point
     * to a publicly accessible image.</p>
     *
     * <p><strong>URL requirements:</strong>
     * <ul>
     *   <li>Must be publicly accessible (no authentication required)</li>
     *   <li>Should point directly to an image file</li>
     *   <li>HTTPS URLs are recommended for security</li>
     *   <li>URL should be properly encoded</li>
     * </ul>
     *
     * <p><strong>Advantages of URL-based detection:</strong>
     * <ul>
     *   <li>No need to upload large files</li>
     *   <li>Faster for images already online</li>
     *   <li>Reduces client bandwidth usage</li>
     * </ul>
     *
     * <p><strong>Error scenarios:</strong>
     * <ul>
     *   <li>URL is not accessible or returns 404</li>
     *   <li>URL does not point to a valid image</li>
     *   <li>Network connectivity issues</li>
     *   <li>Server cannot download from the URL</li>
     * </ul>
     *
     * @param imageUrl The URL of the image to analyze (must be publicly accessible)
     * @param callback Callback interface to receive the detection result or error
     * @throws IllegalArgumentException if imageUrl is null or empty
     * @see ApiCallback
     * @see DetectionResult
     * @see UrlRequest
     */
    public void detectFromUrl(String imageUrl, final ApiCallback<DetectionResult> callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Invalid URL"));
            return;
        }

        // Create request body for URL
        UrlRequest urlRequest = new UrlRequest(imageUrl);
        String jsonString = gson.toJson(urlRequest);
        RequestBody requestBody = RequestBody.create(jsonString, MEDIA_TYPE_JSON);

        // Make API call using Retrofit
        Call<DetectionResult> call = detectionApi.detectFromUrl(requestBody);
        call.enqueue(new Callback<DetectionResult>() {
            @Override
            public void onResponse(Call<DetectionResult> call, Response<DetectionResult> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "URL detection successful: " + response.body());
                    callback.onSuccess(response.body());
                } else {
                    Log.e(TAG, "URL detection failed: " + response.message());
                    callback.onFailure(new RuntimeException("API Error: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<DetectionResult> call, Throwable t) {
                Log.e(TAG, "URL API call failed", t);
                callback.onFailure(new RuntimeException("Network Error: " + t.getMessage()));
            }
        });
    }

    /**
     * Detects objects from a JPEG image represented as a byte array.
     *
     * <p>This method is primarily used for processing images captured from the camera
     * or images that are already loaded in memory. The byte array should contain
     * valid JPEG image data.</p>
     *
     * <p><strong>Common use cases:</strong>
     * <ul>
     *   <li>Camera frame analysis in live detection</li>
     *   <li>Processing images downloaded from network</li>
     *   <li>Processing images from other in-memory sources</li>
     * </ul>
     *
     * <p><strong>Performance considerations:</strong>
     * <ul>
     *   <li>Large byte arrays consume more memory</li>
     *   <li>JPEG compression quality affects both size and detection accuracy</li>
     *   <li>Consider the trade-off between quality and processing speed</li>
     * </ul>
     *
     * <p><strong>Data format requirements:</strong>
     * <ul>
     *   <li>Byte array must contain valid JPEG image data</li>
     *   <li>Image should have sufficient resolution for detection</li>
     *   <li>Very small images may not produce good detection results</li>
     * </ul>
     *
     * @param imageBytes JPEG image data as byte array (must be valid JPEG format)
     * @param callback Callback interface to receive the detection result or error
     * @throws IllegalArgumentException if imageBytes is null or empty
     * @see ApiCallback
     * @see DetectionResult
     */
    public void detectFromBytes(byte[] imageBytes, final ApiCallback<DetectionResult> callback) {
        if (imageBytes == null || imageBytes.length == 0) {
            callback.onFailure(new IllegalArgumentException("Invalid image bytes"));
            return;
        }

        // Create multipart body part for the image bytes
        RequestBody requestFile = RequestBody.create(imageBytes, MEDIA_TYPE_IMAGE);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", "camera_frame.jpg", requestFile);

        // Make API call using Retrofit
        Call<DetectionResult> call = detectionApi.detectFromFile(imagePart);
        call.enqueue(new Callback<DetectionResult>() {
            @Override
            public void onResponse(Call<DetectionResult> call, Response<DetectionResult> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Bytes detection successful: " + response.body());
                    callback.onSuccess(response.body());
                } else {
                    Log.e(TAG, "Bytes detection failed: " + response.message());
                    callback.onFailure(new RuntimeException("API Error: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<DetectionResult> call, Throwable t) {
                Log.e(TAG, "Bytes API call failed", t);
                callback.onFailure(new RuntimeException("Network Error: " + t.getMessage()));
            }
        });
    }

    /**
     * Callback interface for API responses.
     *
     * <p>This interface defines the contract for receiving results from asynchronous
     * API operations. All detection methods in ApiClient use this callback pattern
     * to return results without blocking the calling thread.</p>
     *
     * <p><strong>Implementation guidelines:</strong>
     * <ul>
     *   <li>Always handle both success and failure cases</li>
     *   <li>Update UI elements only from the main thread</li>
     *   <li>Consider showing loading indicators during API calls</li>
     *   <li>Provide meaningful error messages to users</li>
     * </ul>
     *
     * <p><strong>Thread safety:</strong>
     * The callback methods are called on the main thread (UI thread) in Android,
     * making it safe to update UI elements directly from these methods.</p>
     *
     * <p><strong>Usage example:</strong>
     * <pre>{@code
     * ApiCallback<DetectionResult> callback = new ApiCallback<DetectionResult>() {
     *     @Override
     *     public void onSuccess(DetectionResult result) {
     *         // Update UI with detection results
     *         updateUIWithResults(result);
     *     }
     *
     *     @Override
     *     public void onFailure(Exception e) {
     *         // Show error message to user
     *         showErrorMessage("Detection failed: " + e.getMessage());
     *     }
     * };
     * }</pre>
     *
     * @param <T> The type of result expected from the API call
     * @author Object Detection SDK
     * @version 1.0
     * @since 1.0
     */
    public interface ApiCallback<T> {
        /**
         * Called when the API request completes successfully.
         *
         * <p>This method is invoked on the main thread when the API call succeeds
         * and returns a valid response. The result parameter contains the parsed
         * response data.</p>
         *
         * @param result The successful result from the API call (never null)
         */
        void onSuccess(T result);

        /**
         * Called when the API request fails or encounters an error.
         *
         * <p>This method is invoked on the main thread when the API call fails
         * due to network issues, server errors, or parsing problems. The exception
         * provides details about what went wrong.</p>
         *
         * <p><strong>Common exception types:</strong>
         * <ul>
         *   <li>IllegalArgumentException: Invalid input parameters</li>
         *   <li>RuntimeException: API or network errors</li>
         *   <li>IOException: Network connectivity issues</li>
         * </ul>
         *
         * @param e The exception that caused the failure (never null)
         */
        void onFailure(Exception e);
    }
}
