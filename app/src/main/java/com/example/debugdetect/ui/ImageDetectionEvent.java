package com.example.debugdetect.ui;

public class ImageDetectionEvent {

    public enum State {
        DECODING,
        DETECTING,
        TAGGING,
        DONE
    }

    public State state;

    public ImageDetectionEvent(State state) {
        this.state = state;
    }

}
