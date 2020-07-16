package com.example.debugdetect.interfaces;

import android.graphics.Bitmap;

import com.example.debugdetect.api.Classifier;

import java.util.List;

public interface AnalyzeOsaPlanoDelegate {
    public void analyzeCompletionBitmapResult(Bitmap result, List<Classifier.Recognition> recognitions);
}
