package local.jtweak.xposed.hooks;

import android.content.Context;

import java.io.InputStream;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import local.jtweak.xposed.config.SnapceptSettings;
import local.jtweak.xposed.config.SnapceptSettingsUtil;
import local.jtweak.xposed.snapchat.SnapConstants;
import local.jtweak.xposed.SnapceptSaver;
import local.jtweak.xposed.snapchat.SnapInfo;

public class CbcEncryptionAlgorithmClassHook extends XC_MethodHook {

    private final SnapceptSettings settings;

    private final Context context;

    public CbcEncryptionAlgorithmClassHook(SnapceptSettings settings, Context context) {
        this.settings = settings;
        this.context = context;
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        // Check if the stream is valid.
        InputStream realStream = (InputStream) param.getResult();

        if (realStream == null) {
            return;
        }

        // Check if a SnapInfo object is attached.
        SnapInfo snapInfo = (SnapInfo) XposedHelpers.getAdditionalInstanceField(
                param.thisObject,
                SnapConstants.ADDITIONAL_FIELD_ENCRYPTION_ALGORITHM_SNAP_INFO);

        if (snapInfo == null) {
            return;
        }

        if (snapInfo.getType().equals("UNKNOWN") || (snapInfo.getType().equals("STORIES") && !snapInfo.isVideo())) {
            // Check if we have settings enabled to save this.
            if (!SnapceptSettingsUtil.maySave(this.settings, snapInfo)) {
                return;
            }

            // Save it.
            SnapceptSaver saver = new SnapceptSaver(this.settings, this.context, snapInfo);

            if (!saver.isSaved()) {
                param.setResult(saver.save(realStream));
            }
        }
    }

}
