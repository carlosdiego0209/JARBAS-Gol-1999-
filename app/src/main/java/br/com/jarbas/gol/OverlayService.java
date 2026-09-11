package br.com.jarbas.gol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class OverlayService extends Service {
    public static final String ACTION_LISTEN = "br.com.jarbas.gol.ACTION_LISTEN";
    private static final String CHANNEL_ID = "jarb_overlay";
    private WindowManager windowManager;
    private View overlayButton;

    @Override public void onCreate() {
        super.onCreate();
        startForeground(7, createNotification());
        showOverlayButton();
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Botão flutuante do JARB", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Mantém o botão OUVIR JARB disponível sobre outros aplicativos.");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("JARB ativo")
                .setContentText("O botão OUVIR JARB está disponível sobre outros apps.")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void showOverlayButton() {
        if (!Settings.canDrawOverlays(this)) return;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Button button = new Button(this);
        button.setText("OUVIR JARB");
        button.setTextSize(12);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setContentDescription("Ouvir JARB");
        button.setOnClickListener(v -> sendBroadcast(
                new Intent(ACTION_LISTEN).setPackage(getPackageName())));
        overlayButton = button;

        int windowType = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = 24;
        windowManager.addView(overlayButton, params);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (windowManager != null && overlayButton != null) {
            windowManager.removeView(overlayButton);
        }
        overlayButton = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
