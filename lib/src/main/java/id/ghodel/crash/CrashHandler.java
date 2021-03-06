package id.ghodel.crash;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import id.ghodel.crash.data.DisplayType;
import id.ghodel.crash.data.model.CrashInfo;
import id.ghodel.crash.data.model.DeviceInfo;
import id.ghodel.crash.ui.crash.CrashActivity;
import id.ghodel.crash.ui.crash.CrashDialogActivity;
import id.ghodel.crash.util.Utils;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    public static final String CRASH_INFO = "crash_info";
    public static final String EMAIL = "email";
    public static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER = Thread.getDefaultUncaughtExceptionHandler();
    private final Context mContext;

    private final String email;
    private final boolean buildType;
    private final boolean isAllowToSave;
    private final DisplayType showAs;
    private final Class<?> customClass;


    private CrashHandler(Builder builder){
        this.mContext = builder.context;
        this.email = builder.email;
        this.isAllowToSave = builder.isAllowToSave;
        this.buildType = builder.buildType;
        this.showAs = builder.showAs;
        this.customClass = builder.customClass;

        Thread.setDefaultUncaughtExceptionHandler(this);

    }

    public String getEmail() {
        return email;
    }

    public boolean getBuildType() {
        return buildType;
    }
    public boolean isAllowToSave() {
        return isAllowToSave;
    }

    public DisplayType getShowAs(){
        return showAs;
    }

    public Class<?> getCustomClass(){
        return customClass;
    }

    @Override
    public void uncaughtException(Thread t , Throwable e) {
        try {
            tryUncaughtException(t, e);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            if (UNCAUGHT_EXCEPTION_HANDLER != null) {
                UNCAUGHT_EXCEPTION_HANDLER.uncaughtException(t, e);
            }
        }
    }

    private void tryUncaughtException(Thread thread, Throwable throwable) {
        String time = new SimpleDateFormat("MM_dd_yyyy_HH:mm:ss").format(new Date());
        File crashFile = new File(mContext.getExternalFilesDir(null), "crash_log_" + time + ".txt");

        CrashInfo crashInfo = new CrashInfo();

        crashInfo.setException(throwable);
        crashInfo.setTimeOfCrash(new Date().getTime());

        if(throwable.getCause() != null){
            throwable = throwable.getCause();
        }
        crashInfo.setExceptionMsg(throwable.getMessage());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();

        String exceptionType = throwable.getClass().getName();

        StackTraceElement element = Utils.parseThrowable(mContext, throwable);

        crashInfo.setLineNumber(element.getLineNumber());
        crashInfo.setClassName(element.getClassName());
        crashInfo.setFileName(element.getFileName());
        crashInfo.setMethodName(element.getMethodName());
        crashInfo.setExceptionType(exceptionType);
        crashInfo.setFullException(sw.toString());

        crashInfo.setVersionCode(Utils.getVersionCode(mContext));
        crashInfo.setVersionName(Utils.getVersionName(mContext));
        crashInfo.setPackageName(mContext.getPackageName());
        crashInfo.setBuildType(buildType);
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setManufacturer(Build.MANUFACTURER);
        deviceInfo.setBrand(Build.BRAND);
        deviceInfo.setModel(Build.MODEL);
        deviceInfo.setVersion(String.valueOf(Build.VERSION.SDK_INT));
        deviceInfo.setRelease(Build.VERSION.RELEASE);
        deviceInfo.setCpuAbi(Build.CPU_ABI);
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        String densityBucket =Utils.getDensityString(displayMetrics);
        deviceInfo.setDensity(displayMetrics.densityDpi + "dpi (" + densityBucket + ")");
        deviceInfo.setResolution(displayMetrics.heightPixels + "x" + displayMetrics.widthPixels);
        crashInfo.setDeviceInfo(deviceInfo);

        String errorLog = Utils.parseCrashInfoToString(crashInfo);
        if(isAllowToSave){
            try {
                Utils.writeFile(crashFile, errorLog);
            } catch (IOException ignored) {

            }
        }

        switch (showAs){
            case CUSTOM_ACTIVITY:
                Utils.startCrash(mContext,customClass, thread, throwable,crashInfo, email);
                break;
            case ACTIVITY:
                Utils.startCrash(mContext,CrashActivity.class, thread, throwable,crashInfo, email);
                break;
            case DIALOG:
                Utils.startCrash(mContext, CrashDialogActivity.class, thread, throwable,crashInfo, email);
                break;
        }

    }

    public static  class Builder {

        private Context context;
        private String email;
        private boolean isAllowToSave;
        private boolean buildType;
        private DisplayType showAs;
        private Class<?> customClass;

        public Builder(){

        }

        public Builder(Context context){
            this.context = context;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder saveCrashToFile(boolean isAllowToSave) {
            this.isAllowToSave = isAllowToSave;
            return this;
        }

        public Builder showAs(DisplayType showAs, Class<?> customClass){
            this.showAs = showAs;
            this.customClass = customClass;
            return this;
        }

        public Builder showAs(DisplayType showAs){
            this.showAs = showAs;
            return this;
        }

        public Builder setBuildType(boolean buildType){
            this.buildType = buildType;
            return this;
        }

        public void build(){
            new CrashHandler(this);
        }
    }
}