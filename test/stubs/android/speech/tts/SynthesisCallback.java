package android.speech.tts;

public interface SynthesisCallback {
    int getMaxBufferSize();
    int start(int sampleRateInHz, int audioFormat, int channelCount);
    int audioAvailable(byte[] buffer, int offset, int length);
    int done();
    void error();
    void error(int errorCode);
    boolean hasStarted();
    boolean hasFinished();
}
