import java.util.EnumSet;

import xt.audio.Enums.XtDeviceCaps;
import xt.audio.Enums.XtEnumFlags;
import xt.audio.Enums.XtSample;
import xt.audio.Enums.XtSystem;
import xt.audio.Structs.XtBuffer;
import xt.audio.Structs.XtBufferSize;
import xt.audio.Structs.XtChannels;
import xt.audio.Structs.XtDeviceStreamParams;
import xt.audio.Structs.XtFormat;
import xt.audio.Structs.XtMix;
import xt.audio.Structs.XtStreamParams;
import xt.audio.XtAudio;
import xt.audio.XtDevice;
import xt.audio.XtDeviceList;
import xt.audio.XtPlatform;
import xt.audio.XtSafeBuffer;
import xt.audio.XtService;
import xt.audio.XtStream;

public class AudioLoopback implements Runnable {

  private final int bufferSize;

  private final float[] buffer;
  private int bufferIndex = 0;
  private boolean stopped;

  public AudioLoopback(int numSamples) {
    this.bufferSize = 2 * numSamples;
    this.buffer = new float[this.bufferSize];
  }

  public float[] getBuffer() {
    return buffer;
  }

  // audio streaming callback
  int onBuffer(XtStream stream, XtBuffer buffer, Object user) {
    XtSafeBuffer safe = XtSafeBuffer.get(stream);
    if (safe == null) return 0;
    // lock buffer from native into java
    safe.lock(buffer);
    // short[] because we specified INT16 below
    // this is the captured audio data
    short[] audio = (short[]) safe.getInput();
    processAudio(audio, buffer.frames);
    // unlock buffer from java into native
    safe.unlock(buffer);
    return 0;
  }

  void processAudio(short[] audio, int frames) {
    // convert from short[] to byte[]
    for (int frame = 0; frame < frames; frame++) {
      buffer[bufferIndex++] = (float) audio[frame * 2] / Short.MAX_VALUE;
      buffer[bufferIndex++] = (float) audio[frame * 2 + 1] / Short.MAX_VALUE;
      bufferIndex %= bufferSize;
    }
  }

  public void stop() {
    stopped = true;
  }

  @Override
  public void run() {
    // this initializes platform dependent stuff like COM
    try (XtPlatform platform = XtAudio.init(null, null)) {
      // works on windows only, obviously
      XtService service = platform.getService(XtSystem.WASAPI);
      // list input devices (this includes loopback)
      try (XtDeviceList list = service.openDeviceList(EnumSet.of(XtEnumFlags.INPUT))) {
        for (int i = 0; i < list.getCount(); i++) {
          String deviceId = list.getId(i);
          EnumSet<XtDeviceCaps> caps = list.getCapabilities(deviceId);
          // filter loopback devices
          if (caps.contains(XtDeviceCaps.LOOPBACK)) {
            String deviceName = list.getName(deviceId);
            // just to check what output we're recording
            System.out.println(deviceName);
            // open device
            try (XtDevice device = service.openDevice(deviceId)) {
              // 16 bit 48khz
              XtMix mix = new XtMix(192000, XtSample.INT16);
              // 2 channels input, no masking
              XtChannels channels = new XtChannels(2, 0, 0, 0);
              // final audio format
              XtFormat format = new XtFormat(mix, channels);
              // query min/max/default buffer sizes
              XtBufferSize bufferSize = device.getBufferSize(format);
              // true->interleaved, onBuffer->audio stream callback
              XtStreamParams streamParams = new XtStreamParams(true, this::onBuffer, null, null);
              // final initialization params with default buffer size
              XtDeviceStreamParams deviceParams = new XtDeviceStreamParams(streamParams, format, bufferSize.current);
              // run stream
              // safe buffer allows you to get java short[] instead on jna Pointer in the callback
              try (XtStream stream = device.openStream(deviceParams, null);
                   var safeBuffer = XtSafeBuffer.register(stream, true)) {
                stream.start();
                while (!stopped) {
                  Thread.onSpinWait();
                }
                stream.stop();
              }
            }
          }
        }
      }
    }
  }
}
