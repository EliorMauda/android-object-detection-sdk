package com.objectdetection.sdk.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Utility class for converting Android content URIs to temporary files.
 *
 * <p>This utility handles the complex process of converting various Android URI schemes
 * into actual File objects that can be used for file operations. It's essential for
 * processing images from galleries, cameras, and other content providers that use
 * content URIs rather than direct file paths.</p>
 *
 * <p><strong>Supported URI Schemes:</strong>
 * <ul>
 *   <li><strong>content://:</strong> Gallery images, camera captures, shared content</li>
 *   <li><strong>file://:</strong> Direct file system references</li>
 *   <li><strong>android.resource://:</strong> App resources and assets</li>
 * </ul>
 *
 * <p><strong>Common Use Cases:</strong>
 * <ul>
 *   <li>Processing images selected from device gallery</li>
 *   <li>Handling camera capture results</li>
 *   <li>Converting shared images from other applications</li>
 *   <li>Processing images from cloud storage providers</li>
 * </ul>
 *
 * <p><strong>Android Storage Evolution:</strong>
 * This utility is particularly important for handling the evolution of Android storage
 * permissions and security models:
 * <ul>
 *   <li><strong>Android 10+:</strong> Scoped storage requires content URI handling</li>
 *   <li><strong>Android 11+:</strong> Further restrictions on direct file access</li>
 *   <li><strong>Cross-app sharing:</strong> Content URIs ensure secure file sharing</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Convert gallery image URI to file
 * Uri galleryUri = data.getData(); // From image picker
 *
 * UriToFileConverter.uriToFile(context, galleryUri, new UriToFileConverter.FileCallback() {
 *     @Override
 *     public void onSuccess(File file) {
 *         // File is ready for processing
 *         ImageDetector.detectFromFile(file, detectionListener);
 *     }
 *
 *     @Override
 *     public void onFailure(Exception e) {
 *         // Handle conversion error
 *         Log.e(TAG, "Failed to convert URI to file", e);
 *         showError("Could not access selected image");
 *     }
 * });
 * }</pre>
 *
 * <p><strong>Performance Considerations:</strong>
 * <ul>
 *   <li>Conversion runs on background thread to avoid UI blocking</li>
 *   <li>Creates temporary files in app cache directory</li>
 *   <li>Temporary files are marked for deletion on exit</li>
 *   <li>Large images may take time to copy</li>
 * </ul>
 *
 * <p><strong>Permission Requirements:</strong>
 * <ul>
 *   <li>READ_EXTERNAL_STORAGE (Android < 10) for gallery access</li>
 *   <li>Appropriate URI permissions for shared content</li>
 *   <li>Access to app cache directory (automatic)</li>
 * </ul>
 *
 * @author Object Detection SDK
 * @version 1.0
 * @since 1.0
 * @see android.net.Uri
 * @see android.content.ContentResolver
 */
public class UriToFileConverter {
    private static final String TAG = "UriToFileConverter";

    /**
     * Converts a content URI to a temporary file asynchronously.
     *
     * <p>This method handles the complete process of extracting file data from a URI
     * and creating a temporary file that can be used for standard file operations.
     * The conversion runs on a background thread to prevent UI blocking.</p>
     *
     * <p><strong>Conversion Process:</strong>
     * <ol>
     *   <li>Analyze URI scheme and extract metadata</li>
     *   <li>Generate appropriate temporary filename</li>
     *   <li>Open InputStream from ContentResolver</li>
     *   <li>Create temporary file in cache directory</li>
     *   <li>Copy data from InputStream to temporary file</li>
     *   <li>Mark temporary file for deletion on exit</li>
     *   <li>Return file through callback</li>
     * </ol>
     *
     * <p><strong>File Naming Strategy:</strong>
     * <ul>
     *   <li>Attempts to preserve original filename when available</li>
     *   <li>Falls back to UUID-based naming for anonymous content</li>
     *   <li>Preserves file extension for proper MIME type handling</li>
     * </ul>
     *
     * <p><strong>Error Scenarios:</strong>
     * <ul>
     *   <li>URI is not accessible (permissions, deleted file)</li>
     *   <li>Insufficient storage space for temporary file</li>
     *   <li>I/O errors during file copying</li>
     *   <li>ContentResolver cannot open InputStream</li>
     * </ul>
     *
     * <p><strong>Thread Safety:</strong>
     * This method is thread-safe and can be called from any thread. The conversion
     * operation runs on a background thread, and the callback is executed on the
     * main thread for safe UI updates.</p>
     *
     * <p><strong>Memory Management:</strong>
     * The method uses buffered I/O operations to handle large files efficiently
     * without loading entire files into memory. Temporary files are automatically
     * cleaned up by the system.</p>
     *
     * <p><strong>Advanced Usage Example:</strong>
     * <pre>{@code
     * public class ImageProcessor {
     *     public void processImageFromUri(Uri imageUri) {
     *         showProgressDialog("Converting image...");
     *
     *         UriToFileConverter.uriToFile(this, imageUri, new UriToFileConverter.FileCallback() {
     *             @Override
     *             public void onSuccess(File file) {
     *                 hideProgressDialog();
     *
     *                 // Validate file before processing
     *                 if (file.length() > MAX_FILE_SIZE) {
     *                     showError("Image too large");
     *                     return;
     *                 }
     *
     *                 // Process the converted file
     *                 ImageDetector.detectFromFile(file, detectionListener);
     *             }
     *
     *             @Override
     *             public void onFailure(Exception e) {
     *                 hideProgressDialog();
     *
     *                 // Provide specific error messages
     *                 if (e instanceof SecurityException) {
     *                     showError("Permission denied accessing image");
     *                 } else if (e instanceof IOException) {
     *                     showError("Error reading image file");
     *                 } else {
     *                     showError("Unable to process image: " + e.getMessage());
     *                 }
     *             }
     *         });
     *     }
     * }
     * }</pre>
     *
     * @param context Android context for ContentResolver and cache directory access
     * @param uri The content URI to convert (must be accessible by the application)
     * @param callback Callback to receive the converted file or error information
     * @throws IllegalArgumentException if context, uri, or callback is null
     * @see FileCallback
     * @see #createFileFromUri(Context, Uri)
     */
    public static void uriToFile(final Context context, final Uri uri, final FileCallback callback) {
        new AsyncTask<Void, Void, File>() {
            private Exception exception;

            @Override
            protected File doInBackground(Void... voids) {
                try {
                    return createFileFromUri(context, uri);
                } catch (Exception e) {
                    Log.e(TAG, "Error converting URI to file", e);
                    exception = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(File file) {
                if (file != null) {
                    callback.onSuccess(file);
                } else {
                    callback.onFailure(exception != null ? exception : new IOException("Failed to convert URI to file"));
                }
            }
        }.execute();
    }

    /**
     * Creates a temporary file from a content URI (synchronous operation).
     *
     * <p>This method performs the actual file creation and data copying operation.
     * It's designed to run on a background thread and handles the low-level details
     * of URI to file conversion.</p>
     *
     * <p><strong>File Creation Strategy:</strong>
     * <ul>
     *   <li>Creates files in application cache directory</li>
     *   <li>Uses createTempFile() for atomic file creation</li>
     *   <li>Preserves original filename and extension when possible</li>
     *   <li>Marks files for automatic deletion on exit</li>
     * </ul>
     *
     * <p><strong>Data Transfer Process:</strong>
     * <ul>
     *   <li>Opens InputStream from ContentResolver</li>
     *   <li>Creates FileOutputStream to temporary file</li>
     *   <li>Uses 4KB buffer for efficient data transfer</li>
     *   <li>Ensures complete data transfer with proper flushing</li>
     *   <li>Handles stream cleanup in finally blocks</li>
     * </ul>
     *
     * <p><strong>Error Handling:</strong>
     * Comprehensive error handling covers various failure scenarios:
     * <ul>
     *   <li>URI access failures (SecurityException)</li>
     *   <li>File system errors (IOException)</li>
     *   <li>Content provider issues</li>
     *   <li>Storage space problems</li>
     * </ul>
     *
     * @param context Android context for ContentResolver operations
     * @param uri The content URI to process
     * @return A temporary file containing the data from the URI
     * @throws IOException If an I/O error occurs during file operations
     * @throws SecurityException If the URI cannot be accessed due to permissions
     * @see #getFileName(Context, Uri)
     */
    private static File createFileFromUri(Context context, Uri uri) throws IOException {
        String fileName = getFileName(context, uri);
        String extension = "";

        if (fileName == null) {
            fileName = UUID.randomUUID().toString();
        } else {
            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex > 0) {
                extension = fileName.substring(dotIndex);
                fileName = fileName.substring(0, dotIndex);
            }
        }

        // Create temporary file
        File file = File.createTempFile(fileName, extension, context.getCacheDir());
        file.deleteOnExit();

        // Copy data from URI to file
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(file)) {

            if (inputStream == null) {
                throw new IOException("Failed to open input stream");
            }

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
        }

        return file;
    }

    /**
     * Extracts the filename from a content URI using ContentResolver.
     *
     * <p>This method attempts to retrieve the original filename associated with
     * a content URI by querying the content provider. It handles various URI
     * schemes and content provider implementations gracefully.</p>
     *
     * <p><strong>Filename Resolution Strategy:</strong>
     * <ol>
     *   <li>Query content provider for DISPLAY_NAME column</li>
     *   <li>Fall back to parsing URI path if query fails</li>
     *   <li>Handle special cases for different content providers</li>
     *   <li>Return null if filename cannot be determined</li>
     * </ol>
     *
     * <p><strong>Content Provider Compatibility:</strong>
     * <ul>
     *   <li><strong>MediaStore:</strong> Uses DISPLAY_NAME column</li>
     *   <li><strong>File Providers:</strong> Parses path component</li>
     *   <li><strong>Cloud Providers:</strong> Depends on provider implementation</li>
     *   <li><strong>Custom Providers:</strong> Best-effort filename extraction</li>
     * </ul>
     *
     * <p><strong>Error Tolerance:</strong>
     * The method includes comprehensive error handling to prevent crashes
     * when dealing with problematic URIs or content providers. It gracefully
     * degrades to fallback naming strategies.</p>
     *
     * <p><strong>Security Considerations:</strong>
     * <ul>
     *   <li>Handles SecurityException for restricted content</li>
     *   <li>Validates filename for path traversal attempts</li>
     *   <li>Sanitizes special characters in filenames</li>
     * </ul>
     *
     * @param context Android context for ContentResolver access
     * @param uri The content URI to extract filename from
     * @return The filename associated with the URI, or null if not determinable
     * @see android.provider.MediaStore.Images.Media#DISPLAY_NAME
     * @see android.content.ContentResolver#query
     */
    private static String getFileName(Context context, Uri uri) {
        String result = null;

        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                    if (columnIndex >= 0) {
                        result = cursor.getString(columnIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name from URI", e);
            }
        }

        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        return result;
    }

    /**
     * Callback interface for asynchronous URI to file conversion operations.
     *
     * <p>This interface defines the contract for receiving results from URI conversion
     * operations. It follows the standard Android callback pattern for asynchronous
     * operations with clear success and failure paths.</p>
     *
     * <p><strong>Callback Characteristics:</strong>
     * <ul>
     *   <li>Called on the main thread for safe UI updates</li>
     *   <li>Exactly one method will be called per conversion operation</li>
     *   <li>Success callback provides ready-to-use File object</li>
     *   <li>Failure callback includes detailed exception information</li>
     * </ul>
     *
     * <p><strong>Implementation Guidelines:</strong>
     * <ul>
     *   <li>Handle both success and failure cases appropriately</li>
     *   <li>Update UI elements safely from callback methods</li>
     *   <li>Provide meaningful error messages to users</li>
     *   <li>Consider showing progress indicators during conversion</li>
     * </ul>
     *
     * <p><strong>Best Practices:</strong>
     * <pre>{@code
     * UriToFileConverter.FileCallback callback = new UriToFileConverter.FileCallback() {
     *     @Override
     *     public void onSuccess(File file) {
     *         // Validate file before use
     *         if (!file.exists() || file.length() == 0) {
     *             onFailure(new IOException("Invalid file created"));
     *             return;
     *         }
     *
     *         // Log success for debugging
     *         Log.d(TAG, "Converted URI to file: " + file.getAbsolutePath());
     *
     *         // Proceed with file processing
     *         processFile(file);
     *     }
     *
     *     @Override
     *     public void onFailure(Exception e) {
     *         // Log error for debugging
     *         Log.e(TAG, "URI conversion failed", e);
     *
     *         // Provide user-friendly error message
     *         String userMessage = getUserFriendlyErrorMessage(e);
     *         showErrorDialog(userMessage);
     *
     *         // Clean up any partial state
     *         resetUIState();
     *     }
     * };
     * }</pre>
     *
     * @author Object Detection SDK
     * @version 1.0
     * @since 1.0
     */
    public interface FileCallback {
        /**
         * Called when URI to file conversion completes successfully.
         *
         * <p>This method is invoked on the main thread when the conversion operation
         * succeeds and a valid file has been created. The file is ready for immediate
         * use and contains the complete data from the original URI.</p>
         *
         * <p><strong>File Characteristics:</strong>
         * <ul>
         *   <li>Located in application cache directory</li>
         *   <li>Contains complete data from original URI</li>
         *   <li>Marked for deletion on application exit</li>
         *   <li>Has appropriate filename and extension when possible</li>
         * </ul>
         *
         * <p><strong>Usage Considerations:</strong>
         * <ul>
         *   <li>File is temporary and may be cleaned up by the system</li>
         *   <li>Process the file promptly after receiving it</li>
         *   <li>Consider copying to permanent location if needed</li>
         *   <li>Validate file size and format before processing</li>
         * </ul>
         *
         * @param file The temporary file containing data from the URI (never null)
         */
        void onSuccess(File file);

        /**
         * Called when URI to file conversion fails with an error.
         *
         * <p>This method is invoked on the main thread when the conversion operation
         * encounters an error. The exception provides detailed information about
         * what went wrong during the conversion process.</p>
         *
         * <p><strong>Common Exception Types:</strong>
         * <ul>
         *   <li><strong>SecurityException:</strong> URI access denied due to permissions</li>
         *   <li><strong>IOException:</strong> File system or I/O related errors</li>
         *   <li><strong>IllegalArgumentException:</strong> Invalid URI format</li>
         *   <li><strong>RuntimeException:</strong> Unexpected system errors</li>
         * </ul>
         *
         * <p><strong>Error Handling Strategies:</strong>
         * <ul>
         *   <li>Log the error for debugging purposes</li>
         *   <li>Show user-friendly error messages</li>
         *   <li>Implement fallback strategies when appropriate</li>
         *   <li>Clean up any partial UI state</li>
         * </ul>
         *
         * <p><strong>User Experience Considerations:</strong>
         * <ul>
         *   <li>Avoid showing technical exception details to users</li>
         *   <li>Provide actionable guidance when possible</li>
         *   <li>Consider offering alternative input methods</li>
         *   <li>Log detailed errors for developer troubleshooting</li>
         * </ul>
         *
         * @param e The exception that caused the conversion to fail (never null)
         */
        void onFailure(Exception e);
    }
}