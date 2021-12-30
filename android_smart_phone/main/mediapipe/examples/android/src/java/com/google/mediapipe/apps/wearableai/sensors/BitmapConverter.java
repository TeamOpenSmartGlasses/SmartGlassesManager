package com.google.mediapipe.apps.wearableai.sensors;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.mediapipe.components.TextureFrameConsumer;
import com.google.mediapipe.components.TextureFrameProducer;
import com.google.mediapipe.framework.AppTextureFrame;
import com.google.mediapipe.glutil.GlThread;
import com.google.mediapipe.glutil.ShaderUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.microedition.khronos.egl.EGLContext;

public class BitmapConverter implements TextureFrameProducer, CustomFrameAvailableListener  {

    private static final String TAG = "BitmapConverter";
    private static final int DEFAULT_NUM_BUFFERS = 2;
    private static final String THREAD_NAME = "BitmapConverter";

    private BitmapConverter.RenderThread thread;
    @Override
    public void setConsumer(TextureFrameConsumer next) {
        thread.setConsumer(next);
    }

    public void addConsumer(TextureFrameConsumer consumer) {
        thread.addConsumer(consumer);
    }

    public void removeConsumer(TextureFrameConsumer consumer) {
        thread.removeConsumer(consumer);
    }

    public BitmapConverter(EGLContext parentContext, int numBuffers){
        thread = new RenderThread(parentContext, numBuffers);
        thread.setName(THREAD_NAME);
        thread.start();
        try {
            thread.waitUntilReady();
        } catch (InterruptedException ie) {
            // Someone interrupted our thread. This is not supposed to happen: we own
            // the thread, and we are not going to interrupt it. Therefore, it is not
            // reasonable for this constructor to throw an InterruptedException
            // (which is a checked exception). If it should somehow happen that the
            // thread is interrupted, let's set the interrupted flag again, log the
            // error, and throw a RuntimeException.
            Thread.currentThread().interrupt();
            Log.e(TAG, "thread was unexpectedly interrupted: " + ie.getMessage());
            throw new RuntimeException(ie);
        }
    }
    public void setTimestampOffsetNanos(long offsetInNanos) {
        thread.setTimestampOffsetNanos(offsetInNanos);
    }
    public BitmapConverter(EGLContext parentContext) {
        this(parentContext, DEFAULT_NUM_BUFFERS);
    }

    public void close() {
        if (thread == null) {
            return;
        }
        //thread.getHandler().post(() -> thread.setSurfaceTexture(null, 0, 0));
        thread.quitSafely();
        try {
            thread.join();
        } catch (InterruptedException ie) {
            // Set the interrupted flag again, log the error, and throw a RuntimeException.
            Thread.currentThread().interrupt();
            Log.e(TAG, "thread was unexpectedly interrupted: " + ie.getMessage());
            throw new RuntimeException(ie);
        }
    }

    @Override
    public void onFrame(Bitmap bitmap) {
        thread.onFrame(bitmap);
    }


    private static class RenderThread extends GlThread implements CustomFrameAvailableListener{
        private static final long NANOS_PER_MICRO = 1000; // Nanoseconds in one microsecond.
        private final List<TextureFrameConsumer> consumers;
        private List<AppTextureFrame> outputFrames = null;
        private int outputFrameIndex = -1;
        private long nextFrameTimestampOffset = 0;
        private long timestampOffsetNanos = 0;
        private long previousTimestamp = 0;
        private Bitmap bitmap;
        private boolean previousTimestampValid = false;

        protected int destinationWidth = 0;
        protected int destinationHeight = 0;
        public RenderThread(@Nullable Object parentContext, int numBuffers) {
            super(parentContext);
            outputFrames = new ArrayList<>();
            outputFrames.addAll(Collections.nCopies(numBuffers, null));
            consumers = new ArrayList<>();
        }
        public void setConsumer(TextureFrameConsumer consumer) {
            synchronized (consumers) {
                consumers.clear();
                consumers.add(consumer);
            }
        }

        public void addConsumer(TextureFrameConsumer consumer) {
            synchronized (consumers) {
                consumers.add(consumer);
            }
        }

        public void removeConsumer(TextureFrameConsumer consumer) {
            synchronized (consumers) {
                consumers.remove(consumer);
            }
        }

        @Override
        public void prepareGl() {
            super.prepareGl();

            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

            //renderer.setup();
        }

        @Override
        public void releaseGl() {
            for (int i = 0; i < outputFrames.size(); ++i) {
                teardownDestination(i);
            }
            //renderer.release();
            super.releaseGl(); // This releases the EGL context, so must do it after any GL calls.
        }

        public void setTimestampOffsetNanos(long offsetInNanos) {
            timestampOffsetNanos = offsetInNanos;
        }

        private void teardownDestination(int index) {
            if (outputFrames.get(index) != null) {
                waitUntilReleased(outputFrames.get(index));
                GLES20.glDeleteTextures(1, new int[] {outputFrames.get(index).getTextureName()}, 0);
                outputFrames.set(index, null);
            }
        }

        private void setupDestination(int index, int destinationTextureId) {
            teardownDestination(index);
            outputFrames.set(
                    index, new AppTextureFrame(destinationTextureId, destinationWidth, destinationHeight));

        }

        @Override
        public void onFrame(Bitmap bitmap) {
//            Log.d(TAG,"New Frame");
            this.bitmap = bitmap;

            handler.post(() -> renderNext());
        }

        protected void renderNext() {
            if (bitmap == null) {
                return;
            }
            try {
                synchronized (consumers) {
                    boolean frameUpdated = false;
                    for (TextureFrameConsumer consumer : consumers) {
                        AppTextureFrame outputFrame = nextOutputFrame(bitmap);
                        updateOutputFrame(outputFrame);
                        frameUpdated = true;
                        if (consumer != null) {
                            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                                Log.v(
                                        TAG,
                                        String.format(
                                                "Locking tex: %d width: %d height: %d",
                                                outputFrame.getTextureName(),
                                                outputFrame.getWidth(),
                                                outputFrame.getHeight()));
                            }
                            outputFrame.setInUse();
                            consumer.onNewFrame(outputFrame);
                        }
                    }
                    if (!frameUpdated) {  // Need to update the frame even if there are no consumers.
                        AppTextureFrame outputFrame = nextOutputFrame(bitmap);
                        updateOutputFrame(outputFrame);
                    }
                }
            } finally {
                //bitmap.recycle();
            }
        }


    /**
     * NOTE: must be invoked on GL thread
     */
    private AppTextureFrame nextOutputFrame(Bitmap bitmap) {
        int textureName = ShaderUtil.createRgbaTexture(bitmap);
        outputFrameIndex = (outputFrameIndex + 1) % outputFrames.size();
        destinationHeight = bitmap.getHeight();
        destinationWidth = bitmap.getWidth();
        setupDestination(outputFrameIndex, textureName);
        AppTextureFrame outputFrame = outputFrames.get(outputFrameIndex);
        waitUntilReleased(outputFrame);
        return outputFrame;
    }
    private long timestamp=1l;
    private void updateOutputFrame(AppTextureFrame outputFrame) {
        // Populate frame timestamp with surface texture timestamp after render() as renderer
        // ensures that surface texture has the up-to-date timestamp. (Also adjust
        // |nextFrameTimestampOffset| to ensure that timestamps increase monotonically.)
        timestamp = timestamp+1;
        long textureTimestamp =
                (timestamp + timestampOffsetNanos) / NANOS_PER_MICRO;
        if (previousTimestampValid
                && textureTimestamp + nextFrameTimestampOffset <= previousTimestamp) {
            nextFrameTimestampOffset = previousTimestamp + 1 - textureTimestamp;
        }
        outputFrame.setTimestamp(textureTimestamp + nextFrameTimestampOffset);
        previousTimestamp = outputFrame.getTimestamp();
        previousTimestampValid = true;
    }

    private void waitUntilReleased(AppTextureFrame frame) {
        try {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(
                        TAG,
                        String.format(
                                "Waiting for tex: %d width: %d height: %d",
                                frame.getTextureName(), frame.getWidth(), frame.getHeight()));
            }
            frame.waitUntilReleased();
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(
                        TAG,
                        String.format(
                                "Finished waiting for tex: %d width: %d height: %d",
                                frame.getTextureName(), frame.getWidth(), frame.getHeight()));
            }
        } catch (InterruptedException ie) {
            // Someone interrupted our thread. This is not supposed to happen: we own
            // the thread, and we are not going to interrupt it. If it should somehow
            // happen that the thread is interrupted, let's set the interrupted flag
            // again, log the error, and throw a RuntimeException.
            Thread.currentThread().interrupt();
            Log.e(TAG, "thread was unexpectedly interrupted: " + ie.getMessage());
            throw new RuntimeException(ie);
        }
    }
}

}
