package com.sevtinge.cemiuiler.module.systemframework;

import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;

import android.os.Build;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;
import com.sevtinge.cemiuiler.utils.LogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sevtinge.cemiuiler.utils.SdkHelper;
import de.robv.android.xposed.XposedHelpers;
import moralnorm.os.SdkVersion;

public class PackagePermissions extends BaseHook {

    private ArrayList<String> systemPackages = new ArrayList<String>();

    @Override
    public void init() {

        systemPackages.add(Helpers.mAppModulePkg);

        // Allow signature level permissions for module
        String PMSCls = SdkHelper.isAndroidTiramisu() ? "com.android.server.pm.permission.PermissionManagerServiceImpl" : "com.android.server.pm.permission.PermissionManagerService";

        // Allow signature level permissions for module
        hookAllMethods(PMSCls, "shouldGrantPermissionBySignature", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                String pkgName = (String)XposedHelpers.callMethod(param.args[0], "getPackageName");
                if (systemPackages.contains(pkgName)) param.setResult(true);
            }
        });


        hookAllMethodsSilently("com.android.server.pm.PackageManagerServiceUtils", "verifySignatures", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                String pkgName = (String)XposedHelpers.callMethod(param.args[0], "getName");
                if (systemPackages.contains(pkgName)) param.setResult(true);
            }
        });


        // Make module appear as system app
        String ActQueryService = SdkHelper.isAndroidTiramisu() ? "com.android.server.pm.ComputerEngine" : "com.android.server.pm.PackageManagerService";
       hookAllMethods(ActQueryService, "queryIntentActivitiesInternal", new MethodHook() {
            @Override
            @SuppressWarnings("unchecked")
            protected void after(MethodHookParam param) throws Throwable {
                if (param.args.length < 6) return;
                List<ResolveInfo> infos = (List<ResolveInfo>)param.getResult();
                if (infos != null) {
                    for (ResolveInfo info : infos) {
                        if (info != null && info.activityInfo != null && systemPackages.contains(info.activityInfo.packageName)) {
                            XposedHelpers.setObjectField(info, "system", true);
                        }
                    }
                }
            }
       });

        findAndHookMethod("android.content.pm.ApplicationInfo", "isSystemApp", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ApplicationInfo ai = (ApplicationInfo) param.thisObject;
                if (ai != null && systemPackages.contains(ai.packageName)) {
                    param.setResult(true);
                }
            }
        });

        findAndHookMethodSilently("android.content.pm.ApplicationInfo", "isSignedWithPlatformKey", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ApplicationInfo ai = (ApplicationInfo) param.thisObject;
                if (ai != null && systemPackages.contains(ai.packageName)) {
                    param.setResult(true);
                }
            }
        });

        hookAllMethodsSilently("com.android.server.wm.ActivityRecordInjector", "canShowWhenLocked", new Helpers.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });

        try {
            Class<?> dpgpiClass = findClass("com.android.server.pm.MiuiDefaultPermissionGrantPolicy");
            String[] MIUI_SYSTEM_APPS = (String[])XposedHelpers.getStaticObjectField(dpgpiClass, "MIUI_SYSTEM_APPS");
            ArrayList<String> mySystemApps = new ArrayList<String>(Arrays.asList(MIUI_SYSTEM_APPS));
            mySystemApps.addAll(systemPackages);
            XposedHelpers.setStaticObjectField(dpgpiClass, "MIUI_SYSTEM_APPS", mySystemApps.toArray(new String[0]));
        } catch (Throwable t) {
            LogUtils.log(t);
        }
    }
}
