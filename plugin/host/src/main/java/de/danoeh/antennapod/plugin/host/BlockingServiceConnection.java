package de.danoeh.antennapod.plugin.host;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.annotation.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class BlockingServiceConnection implements ServiceConnection {
    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile IBinder binder;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        binder = service;
        latch.countDown();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        binder = null;
    }

    @Nullable
    IBinder awaitBinder(long timeoutMs) throws InterruptedException {
        latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        return binder;
    }
}
