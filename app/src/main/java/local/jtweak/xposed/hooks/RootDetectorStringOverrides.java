package local.jtweak.xposed.hooks;

import de.robv.android.xposed.XC_MethodReplacement;

public class RootDetectorStringOverrides extends XC_MethodReplacement {

    @Override
    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
        return "false";
    }

}
