package drivereplace.aknahs.com.drivexposedmodule;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;

import java.util.concurrent.TimeUnit;

/**
 * Created by aknahs on 14/12/14.
 */
public class MyPendingResult implements PendingResult{
    @Override
    public Result await() {
        return null;
    }

    @Override
    public Result await(long l, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void setResultCallback(ResultCallback resultCallback) {

    }

    @Override
    public void setResultCallback(ResultCallback resultCallback, long l, TimeUnit timeUnit) {

    }

    @Override
    public void a(a a) {

    }
}
