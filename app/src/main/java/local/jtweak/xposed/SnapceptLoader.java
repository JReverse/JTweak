package local.jtweak.xposed;

import java.io.File;
import com.google.common.io.Files;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.content.Intent;
import android.app.Activity;
import android.content.ContentResolver;
import android.provider.MediaStore;
import android.os.Bundle;
import android.webkit.URLUtil;
import android.support.v7.app.AppCompatActivity;
import android.content.ComponentName;
import android.content.ActivityNotFoundException;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.IXposedHookLoadPackage;

import java.util.LinkedHashMap;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import local.jtweak.xposed.config.SnapceptSettings;
import local.jtweak.xposed.hooks.CbcEncryptionAlgorithmClassHook;
import local.jtweak.xposed.hooks.RootDetectorOverrides;
import local.jtweak.xposed.hooks.RootDetectorStringOverrides;
import local.jtweak.xposed.hooks.SnapEventClassHook;
import local.jtweak.xposed.hooks.StoryEventClassHook;
import local.jtweak.xposed.hooks.StoryVideoDecryptorClassHook;
import local.jtweak.xposed.snapchat.SnapConstants;
import local.jtweak.xposed.utils.LogUtils;
import local.jtweak.xposed.utils.Media;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class SnapceptLoader extends AppCompatActivity implements IXposedHookLoadPackage {

    private final Set<String> processedIds;

    private Context context;

    private static Uri initializedUri;

    private File sharedMediaDir;

    private Uri sourceImageUri;

    private File tempMediaFile;

    private int rotation = 0;

    private SnapceptSettings settings;

    private Timer cleanTimer;

    final Media mediaImg = new Media(); // a place to store the image

    final Media mediaVid = new Media(); // a place to store the video

    public SnapceptLoader() {
        this.processedIds = new HashSet<>();
    }

    String replaceLocation = "/storage/emulated/0/Snapchat/";

    public static String getPathFromContentUri(ContentResolver contentResolver, Uri contentUri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = contentResolver.query(contentUri, projection, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            String filePath = cursor.getString(column_index);
            cursor.close();
            return filePath;
        } else {
            return null;
        }
    }
    public static Uri getFileUriFromContentUri(ContentResolver contentResolver, Uri contentUri) {
        String filePath = getPathFromContentUri(contentResolver, contentUri);
        if (filePath == null) {
            return null;
        }
        return Uri.fromFile(new File(filePath));
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Check if we are hooking on the correct package.
        if (!lpparam.packageName.equals(SnapConstants.PACKAGE_NAME)) {
            return;
        }

        // Skip if not first.
        if (!lpparam.isFirstApplication) {
            return;
        }

        // Initialize values used by the module.
        initialize();

        // Check if the correct version is used.
        if (!isVersionCorrect()) {
            LogUtils.logTrace("Wrong Snapchat version. Ensure version " + SnapConstants.PACKAGE_VERSION_STRING + " is installed.");
            return;
        }

        // Announce correct version.
        LogUtils.logTrace("Correct Snapchat version, have fun.");

        // Initialize more values used by the module.
        initializePost();

        // Clear processedIds sometimes.
        initializeTimer();

        // Hook root detectors.
        hookRootDetectors(lpparam.classLoader);

        if (settings.isBlockScreenshotDetection()) {
            // Hook screenshot detectors.
            hookScreenshotDetectors(lpparam.classLoader);
        }

        // Hook Intent
        IntentHook(lpparam.classLoader);

        // Hook Video Sharing
        hookSharingVideo(lpparam.classLoader);

        // Hook Picture Sharing
        hookSharingPicture(lpparam.classLoader);

        // Hook snap event.
        hookSnapEventClass(lpparam.classLoader);

        // Hook story event.
        hookStoryEventClass(lpparam.classLoader);

        // Hook encryption.
        hookCbcEncryptionAlgorithmClass(lpparam.classLoader);

        // Hook video story decryptor.
        hookStoryVideoDecryptor(lpparam.classLoader);

        // Just to be sure.
        this.settings.reload();
    }

    private void initialize() {
        Object activityThread = XposedHelpers.callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
        context = (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");
    }

    private void initializePost() {
        settings = new SnapceptSettings(context);
    }

    private void initializeTimer() {
        if (cleanTimer != null) {
            return;
        }

        cleanTimer = new Timer();
        cleanTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Wipe processedIds sometimes so we don't fill up memory.
                processedIds.clear();
            }
        }, 1000 * 60 * 15, 1000 * 60 * 15);
    }

    private boolean isVersionCorrect() throws Throwable {
        PackageInfo snapchatPackage = context.getPackageManager().getPackageInfo(SnapConstants.PACKAGE_NAME, 0);
        return snapchatPackage.versionCode == SnapConstants.PACKAGE_VERSION;
    }

    private void hookRootDetectors(ClassLoader loader) {
        // Snapchat
        findAndHookMethod(SnapConstants.ROOT_DETECTOR_CLASS, loader, SnapConstants.ROOT_DETECTOR_FIRST, new RootDetectorOverrides());
        findAndHookMethod(SnapConstants.ROOT_DETECTOR_CLASS, loader, SnapConstants.ROOT_DETECTOR_SECOND, new RootDetectorOverrides());
        findAndHookMethod(SnapConstants.ROOT_DETECTOR_CLASS, loader, SnapConstants.ROOT_DETECTOR_THIRD, new RootDetectorOverrides());
        findAndHookMethod(SnapConstants.ROOT_DETECTOR_CLASS, loader, SnapConstants.ROOT_DETECTOR_FORTH, new RootDetectorOverrides());

        // Crashlytics
        findAndHookMethod(SnapConstants.ROOT_DETECTOR_TWO_CLASS, loader, SnapConstants.ROOT_DETECTOR_TWO_FIRST, Context.class, new RootDetectorOverrides());

        // Braintree
        findAndHookMethod(SnapConstants.ROOT_DETECTOR_THREE_CLASS, loader, SnapConstants.ROOT_DETECTOR_THREE_FIRST, new RootDetectorStringOverrides());
    }

    private void hookScreenshotDetectors(ClassLoader loader) {
        findAndHookMethod(SnapConstants.SCREENSHOT_DETECTOR_1_CLASS, loader, SnapConstants.SCREENSHOT_DETECTOR_1_RUN_METHOD, LinkedHashMap.class, XC_MethodReplacement.DO_NOTHING);
        findAndHookMethod(SnapConstants.SCREENSHOT_DETECTOR_2_CLASS, loader, SnapConstants.SCREENSHOT_DETECTOR_2_RUN_METHOD, XC_MethodReplacement.DO_NOTHING);
    }

    private Bitmap rotateBitmap(Bitmap src, int ang) { // rotate bitmap to fix image rotation bug when sharing
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(ang);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    private void IntentHook(ClassLoader loader) {

        // This is where the media is loaded and transformed. Hooks after the onCreate() call of the main Activity.
        findAndHookMethod("com.snapchat.android.LandingPageActivity", loader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                // Get intent, action and MIME type
                Intent intent = activity.getIntent();
                String type = intent.getType();
                String action = intent.getAction();
                // Check if this is a normal launch of Snapchat or actually called by Snapshare and if loaded from recents
                if (type != null && Intent.ACTION_SEND.equals(action) && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
                    Uri mediaUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    // Check for bogus call
                    if (mediaUri == null) {
                        return;
                    }
                    /* We check if the current media got already initialized and should exit instead
                     * of doing the media initialization again. This check is necessary
                     * because onCreate() is also called if the phone is just rotated. */
                    if (initializedUri == mediaUri) {
                        return;
                    }

                    ContentResolver contentResolver = activity.getContentResolver();

                    if (type.startsWith("image/")) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(contentResolver, mediaUri);
                            Bitmap Rotate = rotateBitmap(bitmap, -90);
                            // Make Snapchat show the image
                            mediaImg.setContent(Rotate);
                        } catch (Exception e) {
                            return;
                        }
                    } else if (type.startsWith("video/")) {
                        Uri videoUri;
                        // Snapchat expects the video URI to be in the file:// scheme, not content:// scheme
                        if (URLUtil.isFileUrl(mediaUri.toString())) {
                            videoUri = mediaUri;
                        } else { // No file URI, so we have to convert it
                            videoUri = getFileUriFromContentUri(contentResolver, mediaUri);
                            if (videoUri != null) {

                            } else {

                                return;
                            }
                        }

                        File videoFile = new File(videoUri.getPath());
                        try {

                            videoUri = Uri.fromFile(videoFile);
                        } catch (Exception e) {
                            return;
                        }

                        // Get size of video and compare to the maximum size
                        mediaVid.setContent(videoUri);
                    }

                    /**
                     * Mark image as initialized
                     * @see initializedUri
                     */
                    initializedUri = mediaUri;
                } else {
                    initializedUri = null;
                }
            }

        });
    }
    private void hookSharingPicture(ClassLoader loader) {

        findAndHookMethod("adwd", loader, "a", Bitmap.class, Integer.class, String.class, long.class, boolean.class, int.class, "fre$b", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                XposedBridge.log("Image taken. Proceeding with hook.");
                    param.args[0] =  mediaImg.getContent();

                    File findAvailable = new File(replaceLocation + "replaced.jpg");
                    int index = 0;

                    while(findAvailable.exists()) {
                        findAvailable = new File(replaceLocation + "replaced" + index++ + ".jpg");
                    }
                    XposedBridge.log("Replaced image.");
                }
        });
    }

    private void hookSharingVideo(ClassLoader loader) {
        findAndHookMethod("adwi", loader, "a" , Uri.class, int.class, boolean.class, "atps", long.class, long.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("Video taken. Proceeding with hook.");
                param.args[0] = mediaVid.getContent();

                // Sometimes, the video rotation is wrong. I wrote some code to rotate it to the correct angle,
                // however the angle is correct more often than not so I won't use this until I figure out a way to pick whether to rotate our not
                //
                // File rotatedShareVideo = rotateMp4File(videoToShare);

            }
        });
    }

    private void hookSnapEventClass(ClassLoader loader) {
        findAndHookMethod(
                SnapConstants.SNAP_PROCESSING_CLASS,
                loader,
                SnapConstants.SNAP_PROCESSING_HANDLE_METHOD,
                SnapConstants.SNAP_EVENT_CLASS,
                String.class,
                new SnapEventClassHook(this.processedIds));
    }

    private void hookStoryEventClass(ClassLoader loader) {
        findAndHookMethod(
                SnapConstants.STORY_EVENT_CLASS,
                loader,
                SnapConstants.STORY_EVENT_METHOD_GET_ENCRYPTION_ALGORITHM,
                new StoryEventClassHook(this.processedIds));
    }

    private void hookCbcEncryptionAlgorithmClass(ClassLoader loader) {
        findAndHookMethod(
                SnapConstants.CBC_ENCRYPTION_ALGORITHM_CLASS,
                loader,
                SnapConstants.CBC_ENCRYPTION_ALGORITHM_DECRYPT,
                InputStream.class,
                new CbcEncryptionAlgorithmClassHook(this.settings, this.context));
    }

    private void hookStoryVideoDecryptor(ClassLoader loader) {
        findAndHookMethod(
                SnapConstants.SNAP_VIDEO_DECRYPTOR_CLASS,
                loader,
                SnapConstants.SNAP_VIDEO_DECRYPTOR_METHOD_DECRYPT,
                String.class,
                InputStream.class,
                SnapConstants.ENCRYPTION_ALGORITHM_INTERFACE,
                boolean.class,
                boolean.class,
                boolean.class,
                Uri.class,
                boolean.class,
                boolean.class,
                boolean.class,
                new StoryVideoDecryptorClassHook(this.settings, this.context));
    }

}
