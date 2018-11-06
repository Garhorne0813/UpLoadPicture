package com.garhorne.uploadpicture;

public interface PermissionInterface {

    void requestPermissionSuccess(int callBackCode);

    void requestPermissionFailure(int callBackCode);

}
