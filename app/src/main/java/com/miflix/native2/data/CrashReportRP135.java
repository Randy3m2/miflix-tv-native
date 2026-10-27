package com.miflix.native2.data;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import com.miflix.native2.BuildConfig;
import java.util.concurrent.atomic.AtomicBoolean;

/** Records a bounded local Java crash; always delegates to Android's original fatal handler. */
public final class CrashReportRP135 {
    private static final String PREFS="brunio_local_crash_rp135";
    private static final AtomicBoolean installed=new AtomicBoolean(false);
    private static volatile String stage="startup";
    private CrashReportRP135() {}
    public static void stage(String value) {
        stage=value!=null && value.matches("[A-Za-z0-9/_-]{1,80}")?value:"screen";
    }
    public static void install(Context context) {
        if(!installed.compareAndSet(false,true))return;
        final Context app=context.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        final AtomicBoolean saving=new AtomicBoolean(false);
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            try {
                if(saving.compareAndSet(false,true)) {
                    String summary=CrashSummaryRP135.format(error,BuildConfig.VERSION_NAME,Build.VERSION.SDK_INT,Build.MODEL,stage);
                    app.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString("report",summary).commit();
                }
            } catch(Throwable ignored) { /* Diagnostics must not replace the original failure. */ }
            finally {
                if(previous!=null) previous.uncaughtException(thread,error);
                else {android.os.Process.killProcess(android.os.Process.myPid());System.exit(10);}
            }
        });
    }
    public static void showPrevious(Activity activity) {
        if(activity.isFinishing() || activity.isDestroyed())return;
        final SharedPreferences prefs=activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        final String report=prefs.getString("report",null);
        if(report==null || report.isEmpty())return;
        new AlertDialog.Builder(activity).setTitle("BruniO · Diagnóstico / Diagnostics")
            .setMessage("Se registró un cierre. Puedes tomar una foto de este mensaje.\nA crash was recorded. You can photograph this report.\n\n"+report)
            .setPositiveButton("OK",(dialog,which)->{})
            .setNeutralButton("Copiar / Copy",(dialog,which)->{
                ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if(clipboard!=null)clipboard.setPrimaryClip(ClipData.newPlainText("BruniO diagnostics",report));
            }).setOnDismissListener(dialog->prefs.edit().remove("report").apply()).show();
    }
}
