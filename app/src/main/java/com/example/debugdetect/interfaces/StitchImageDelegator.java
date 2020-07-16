package com.example.debugdetect.interfaces;

import android.graphics.Bitmap;

public interface StitchImageDelegator {
    public void afterImageStitching(Bitmap bitmap);
    public void errorWhileStitching(Error error);
}
