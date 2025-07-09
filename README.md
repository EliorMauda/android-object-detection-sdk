# Android Object Detection SDK

A powerful and easy-to-use Android library for real-time object detection in images and live camera feeds. This SDK provides seamless integration with object detection APIs and includes advanced features like coordinate transformation, live camera detection, and comprehensive error handling.

## Features

- 🖼️ **Static Image Detection** - Process local files, URIs, and web URLs
- 📹 **Live Camera Detection** - Real-time object detection from camera feed  
- 🎯 **Advanced Coordinate System** - Pixel-perfect bounding box alignment
- 🔄 **Multiple Input Sources** - Files, URIs, URLs, and byte arrays
- ⚡ **Asynchronous Processing** - Non-blocking operations with callbacks
- 🛡️ **Robust Error Handling** - Comprehensive error management
- 📱 **Android 10+ Compatible** - Full support for scoped storage

## Installation

### Step 1: Add JitPack Repository

Add JitPack to your project's `settings.gradle` file:

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' } // Add this line
    }
}
```

### Step 2: Add Dependency

Add the SDK dependency to your app's `build.gradle` file:

```gradle
dependencies {
    implementation 'com.github.EliorMauda:android-sdk:v0.1.5'
}
```

### Step 3: Add Permissions

Add required permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
    android:maxSdkVersion="32" />
```

## Quick Start

### 1. Initialize the SDK

Initialize the SDK in your `Application` class or `Activity`:

```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize with your API endpoint
        ImageDetector.init("https://object-detection-api-production.up.railway.app"); //if this link is still available
    }
}
```

### 2. Basic Image Detection

```java
// Detect objects in a local file
File imageFile = new File("/path/to/image.jpg");

ImageDetector.detectFromFile(imageFile, new ImageDetectionListener() {
    @Override
    public void onResult(DetectionResult result) {
        if (result.isSuccess()) {
            List<DetectedObject> objects = result.getDetectedObjects();
            Log.d("Detection", "Found " + objects.size() + " objects");
            
            for (DetectedObject obj : objects) {
                Log.d("Object", obj.getLabel() + " - " + obj.getConfidenceAsPercentage());
            }
        }
    }
    
    @Override
    public void onError(Exception e) {
        Log.e("Detection", "Error: " + e.getMessage());
    }
});
```

## Usage Examples

### Static Image Detection

#### From Gallery/Camera URI
```java
// Handle image picker result
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
        Uri imageUri = data.getData();
        
        ImageDetector.detectFromUri(this, imageUri, new ImageDetectionListener() {
            @Override
            public void onResult(DetectionResult result) {
                displayResults(result);
            }
            
            @Override
            public void onError(Exception e) {
                showError(e.getMessage());
            }
        });
    }
}
```

#### From Web URL
```java
String imageUrl = "https://example.com/image.jpg";

ImageDetector.detectFromUrl(imageUrl, new ImageDetectionListener() {
    @Override
    public void onResult(DetectionResult result) {
        processWebImageResults(result);
    }
    
    @Override
    public void onError(Exception e) {
        handleNetworkError(e);
    }
});
```

#### Using Builder Pattern
```java
DetectorBuilder.with(this)
    .setListener(new ImageDetectionListener() {
        @Override
        public void onResult(DetectionResult result) {
            // Handle result
        }
        
        @Override
        public void onError(Exception e) {
            // Handle error
        }
    })
    .detectFromFile(imageFile);
```

### Live Camera Detection

#### Basic Setup
```java
public class LiveDetectionActivity extends AppCompatActivity {
    private PreviewView previewView;
    private DetectionOverlayView overlayView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_detection);
        
        previewView = findViewById(R.id.preview);
        overlayView = findViewById(R.id.overlay);
        
        // Start live detection
        startLiveDetection();
    }
    
    private void startLiveDetection() {
        ImageDetector.startLiveDetection(this, previewView, new LiveDetectionListener() {
            @Override
            public void onDetectionResult(DetectionResult result, long frameTimestamp) {
                // Update overlay with detection results
                overlayView.setDetectionResult(result, 
                    cameraWidth, cameraHeight,
                    displayWidth, displayHeight, 
                    offsetX, offsetY);
            }
            
            @Override
            public void onError(Exception e, long frameTimestamp) {
                Log.e("LiveDetection", "Frame error: " + e.getMessage());
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImageDetector.stopLiveDetection();
    }
}
```

#### Layout for Live Detection
```xml
<!-- activity_live_detection.xml -->
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.camera.view.PreviewView
        android:id="@+id/preview"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <com.objectdetection.sdk.view.DetectionOverlayView
        android:id="@+id/overlay"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</FrameLayout>
```

### Advanced Detection Overlay

#### Coordinate Transformation (Recommended)
```java
// For ImageView with fitCenter scaling
Drawable drawable = imageView.getDrawable();
int originalWidth = drawable.getIntrinsicWidth();
int originalHeight = drawable.getIntrinsicHeight();

int viewWidth = imageView.getWidth();
int viewHeight = imageView.getHeight();

// Calculate displayed image area
float imageAspectRatio = (float) originalWidth / originalHeight;
float viewAspectRatio = (float) viewWidth / viewHeight;

int displayWidth, displayHeight, offsetX = 0, offsetY = 0;
if (imageAspectRatio > viewAspectRatio) {
    displayWidth = viewWidth;
    displayHeight = (int) (viewWidth / imageAspectRatio);
    offsetY = (viewHeight - displayHeight) / 2;
} else {
    displayHeight = viewHeight;
    displayWidth = (int) (viewHeight * imageAspectRatio);
    offsetX = (viewWidth - displayWidth) / 2;
}

overlayView.setDetectionResult(result, originalWidth, originalHeight,
                              displayWidth, displayHeight, offsetX, offsetY);
```

#### Legacy Mode (Simple)
```java
// For simple use cases where image fills entire view
overlayView.setDetectionResult(result, imageWidth, imageHeight);
```

## API Reference

### Core Classes

#### ImageDetector
Main entry point for detection operations.

```java
// Initialization
ImageDetector.init(String baseUrl)
boolean isInitialized()
String getApiUrl()

// Static Detection
detectFromFile(File file, ImageDetectionListener listener)
detectFromUri(Context context, Uri uri, ImageDetectionListener listener)  
detectFromUrl(String url, ImageDetectionListener listener)

// Live Detection
startLiveDetection(Context context, PreviewView preview, LiveDetectionListener listener)
startLiveDetectionFrontCamera(Context context, PreviewView preview, LiveDetectionListener listener)
stopLiveDetection()
```

#### DetectionResult
Contains detection results and metadata.

```java
// Properties
List<DetectedObject> getDetectedObjects()
String getError()
Long getProcessingTimeMs()
String getImageUrl()

// Methods
boolean isSuccess()
```

#### DetectedObject
Represents a single detected object.

```java
// Properties
String getLabel()
Float getConfidence()
BoundingBox getBox()

// Methods
String getConfidenceAsPercentage()
```

#### BoundingBox
Normalized bounding box coordinates (0.0 - 1.0).

```java
// Coordinates
Float getXMin(), getYMin(), getXMax(), getYMax()

// Utilities
RectF toRectF(int imageWidth, int imageHeight)
boolean isValid()
```

#### DetectionOverlayView
Custom view for displaying detection results.

```java
// Advanced coordinate transformation (recommended)
setDetectionResult(DetectionResult result, 
                  int originalWidth, int originalHeight,
                  int displayWidth, int displayHeight, 
                  int offsetX, int offsetY)

// Legacy mode
setDetectionResult(DetectionResult result, int imageWidth, int imageHeight)
setDetectionResult(DetectionResult result) // For live camera

// Utilities
clearDetections()
String getTransformationInfo()
```

### Listeners

#### ImageDetectionListener
```java
void onResult(DetectionResult result)
void onError(Exception e)
```

#### LiveDetectionListener
```java
void onDetectionResult(DetectionResult result, long frameTimestamp)
void onError(Exception e, long frameTimestamp)
```

## Error Handling

### Common Error Scenarios

```java
@Override
public void onError(Exception e) {
    String message = e.getMessage();
    
    if (e instanceof FileNotFoundException) {
        showError("Image file not found");
    } else if (e instanceof SecurityException) {
        showError("Permission denied accessing image");
    } else if (message.contains("Network") || message.contains("timeout")) {
        showError("Network error - check connection");
    } else if (e instanceof IllegalStateException && message.contains("not initialized")) {
        showError("SDK not properly initialized");
    } else {
        showError("Detection failed: " + message);
    }
}
```

### Live Detection Error Handling

```java
@Override
public void onError(Exception e, long frameTimestamp) {
    // Log individual frame errors (don't stop detection)
    Log.w(TAG, "Frame processing error", e);
    
    // Track error rate for quality monitoring
    errorCount++;
    if (errorCount > 10) {
        showWarning("Detection quality may be affected");
        errorCount = 0;
    }
}
```

## Performance Optimization

### Image Processing
- Use appropriate image sizes (recommend < 2MB for better performance)
- Consider image compression for large files
- Use JPEG format when possible for better compression

### Live Detection
- Processes ~2 frames per second for optimal performance
- Uses 640x480 resolution by default
- Automatically skips frames when processing is behind

### Memory Management
- Temporary files are automatically cleaned up
- DetectionOverlayView reuses Paint objects for efficiency
- URI conversions use buffered I/O for large files

## Requirements

- **Minimum SDK:** API 26 (Android 8.0)
- **Target SDK:** API 33 (Android 13)
- **Compile SDK:** API 35
- **Java Version:** 17

## Dependencies

The SDK includes the following dependencies:
- CameraX (1.4.2) - Camera operations
- OkHttp (4.10.0) - Network requests  
- Gson (2.10.1) - JSON parsing
- Retrofit (2.9.0) - API client
- AndroidX components

## Migration Guide

### From Legacy Coordinate System

```java
// Old way (deprecated)
overlayView.setDetectionResult(result, imageWidth, imageHeight);

// New way (recommended)
overlayView.setDetectionResult(result, originalWidth, originalHeight,
                              displayWidth, displayHeight, offsetX, offsetY);
```

## Troubleshooting

### Common Issues

**SDK not initialized error**
```java
if (!ImageDetector.isInitialized()) {
    ImageDetector.init("https://object-detection-api-production.up.railway.app"); //if this link is still available
}
```

**Camera permission denied**
```java
if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
        != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this, 
        new String[]{Manifest.permission.CAMERA}, 
        CAMERA_PERMISSION_REQUEST);
}
```

**File access issues**
- Ensure proper permissions for external storage access
- Use content URIs instead of file paths on Android 10+
- Check file existence before processing

## Support

For support and questions:
- Create an issue on GitHub
- Check the troubleshooting section above
- Review the API documentation
