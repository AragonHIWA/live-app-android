package io.hypertrack.meta.store.callback;

import io.hypertrack.meta.util.SuccessErrorCallback;

/**
 * Created by ulhas on 19/06/16.
 */
public abstract class UserStoreGetTaskCallback {
    public abstract void OnSuccess(String taskID);
    public abstract void OnError();
}
