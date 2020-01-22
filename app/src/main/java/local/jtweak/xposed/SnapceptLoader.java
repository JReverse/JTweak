package local.jtweak.xposed;

import java.io.File;
import com.google.common.io.Files;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;

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
import local.jtweak.xposed.config.SnapceptSettings;
import local.jtweak.xposed.hooks.CbcEncryptionAlgorithmClassHook;
import local.jtweak.xposed.hooks.RootDetectorOverrides;
import local.jtweak.xposed.hooks.RootDetectorStringOverrides;
import local.jtweak.xposed.hooks.SnapEventClassHook;
import local.jtweak.xposed.hooks.StoryEventClassHook;
import local.jtweak.xposed.hooks.StoryVideoDecryptorClassHook;
import local.jtweak.xposed.snapchat.SnapConstants;
import local.jtweak.xposed.utils.LogUtils;
import de.robv.android.xposed.XC_MethodReplacement;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class SnapceptLoader implements IXposedHookLoadPackage {

    private final Set<String> processedIds;

    private Context context;

    private SnapceptSettings settings;

    private Timer cleanTimer;

    public SnapceptLoader() {
        this.processedIds = new HashSet<>();
    }

    String replaceLocation = "/storage/emulated/0/Snapchat/";

    BitmapFactory.Options bitmapOptions = new android.graphics.BitmapFactory.Options();

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

    private void hookSharingPicture(ClassLoader loader) {

        findAndHookMethod("adwd", loader, "a", Bitmap.class, Integer.class, String.class, long.class, boolean.class, int.class, "fre$b", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("Image taken. Proceeding with hook.");

                File jpg = new File(replaceLocation + "replace.jpg");
                if (jpg.exists()) {
                    Bitmap replace = BitmapFactory.decodeFile(jpg.getPath(), bitmapOptions);
                    param.args[0] = rotateBitmap(replace, -90);

                    File findAvailable = new File(replaceLocation + "replaced.jpg");
                    int index = 0;

                    while(findAvailable.exists()) {
                        findAvailable = new File(replaceLocation + "replaced" + index++ + ".jpg");
                    }
                    jpg.renameTo(findAvailable);
                    XposedBridge.log("Replaced image.");
                } else XposedBridge.log("Nothing to replace");
            }
        });
    }

    private void hookSharingVideo(ClassLoader loader) {
        findAndHookMethod("adwi", loader, "a" , Uri.class, int.class, boolean.class, "atps", long.class, long.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("Video taken. Proceeding with hook.");
                Uri uri = (Uri) param.args[0];
                File recordedVideo = new File(uri.getPath());
                File videoToShare =  new File(replaceLocation + "replace.mp4");

                // Sometimes, the video rotation is wrong. I wrote some code to rotate it to the correct angle,
                // however the angle is correct more often than not so I won't use this until I figure out a way to pick whether to rotate our not
                //
                // File rotatedShareVideo = rotateMp4File(videoToShare);

                if (videoToShare.exists()) {
                    Files.copy(videoToShare, recordedVideo);
                    File findAvailable = new File(replaceLocation + "replaced.mp4");
                    int index = 0;

                    while(findAvailable.exists()) {
                        findAvailable = new File(replaceLocation + "replaced" + index++ + ".mp4");
                    }
                    videoToShare.renameTo(findAvailable);
                    XposedBridge.log("Replaced Video.");
                } else XposedBridge.log("Nothing to replace");
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
