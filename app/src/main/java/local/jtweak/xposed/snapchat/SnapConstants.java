package local.jtweak.xposed.snapchat;

public class SnapConstants {

    // Snapchat app related constants, all found in "AndroidManifest.xml".

    public static final String PACKAGE_NAME = "com.snapchat.android";

    public static final int PACKAGE_VERSION = 1835;

    public static final String PACKAGE_VERSION_STRING = "10.44.5.0 Beta";

    // Snap received, everything is in the same class.

    public static final String SNAP_EVENT_CLASS = "aquy";

    public static final String SNAP_EVENT_FIELD_ID = "u";

    public static final String SNAP_EVENT_IS_VIDEO = "aX_";

    public static final String SNAP_EVENT_USERNAME_FIELD = "aH";

    public static final String SNAP_EVENT_TIMESTAMP_FIELD = "y";

    public static final String SNAP_EVENT_IS_ZIPPED_FIELD = "aI";

    public static final String SNAP_EVENT_METHOD_ORIGIN = "v";

    // Story received, everything is in the same class.

    public static final String STORY_EVENT_CLASS = "aqmi";

    public static final String STORY_EVENT_IS_VIDEO = SNAP_EVENT_IS_VIDEO;

    public static final String STORY_EVENT_IS_ZIPPED_FIELD = SNAP_EVENT_IS_ZIPPED_FIELD;

    public static final String STORY_EVENT_METHOD_GET_ENCRYPTION_ALGORITHM = "ar";

    public static final String STORY_EVENT_METHOD_GET_USERNAME = "aw";

    public static final String STORY_EVENT_FIELD_TIMESTAMP = "v";

    // Snap video decryptor, everything is in the same class.

    public static final String SNAP_VIDEO_DECRYPTOR_CLASS = "com.snapchat.android.app.shared.crypto.SnapVideoDecryptor";

    public static final String SNAP_VIDEO_DECRYPTOR_METHOD_DECRYPT = "a";

    // Media cache entry, everything is in the same class.

    public static final String MEDIA_CACHE_ENTRY_CLASS = "aqzn";

    public static final String MEDIA_CACHE_ENTRY_FIELD_ENCRYPTION_ALGORITHM = "c";

    // Snap received processing, everything is in the same class.

    public static final String SNAP_PROCESSING_CLASS = "alxu";

    public static final String SNAP_PROCESSING_HANDLE_METHOD = "a";

    // Encryption.

    public static final String UNENCRYPTED_ALGORITHM_INTERFACE = "UnencryptedEncryptionAlgorithm";

    public static final String CBC_ENCRYPTION_ALGORITHM_CLASS = "com.snapchat.android.framework.crypto.CbcEncryptionAlgorithm";

    public static final String CBC_ENCRYPTION_ALGORITHM_DECRYPT = "b";

    public static final String ENCRYPTION_ALGORITHM_INTERFACE = "com.snapchat.android.framework.crypto.EncryptionAlgorithm";

    // Root detectors, everything is in the same class.

    public static final String ROOT_DETECTOR_CLASS = "asnk";

    public static final String ROOT_DETECTOR_FIRST = "b";

    public static final String ROOT_DETECTOR_SECOND = "c";

    public static final String ROOT_DETECTOR_THIRD = "d";

    public static final String ROOT_DETECTOR_FORTH = "e";

    public static final String ROOT_DETECTOR_TWO_CLASS = "bcmw";

    public static final String ROOT_DETECTOR_TWO_FIRST = "f";

    public static final String ROOT_DETECTOR_THREE_CLASS = "sm";

    public static final String ROOT_DETECTOR_THREE_FIRST = "a";

    // Screenshot detection.

    public static final String SCREENSHOT_DETECTOR_1_CLASS = "asnm";

    public static final String SCREENSHOT_DETECTOR_1_RUN_METHOD = "a";

    public static final String SCREENSHOT_DETECTOR_2_CLASS = "iuo";

    public static final String SCREENSHOT_DETECTOR_2_RUN_METHOD = "a";

    // Additional info fields

    public static final String ADDITIONAL_FIELD_ENCRYPTION_ALGORITHM_SNAP_INFO = "SnapInfo";

}
