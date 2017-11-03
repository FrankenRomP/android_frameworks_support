/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.support.v17.leanback.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v17.leanback.media.MediaPlayerGlue;
import android.support.v17.leanback.media.PlaybackGlue;
import android.support.v17.leanback.media.PlaybackGlueHost;
import android.support.v17.leanback.test.R;
import android.support.v17.leanback.testutils.PollingCheck;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class VideoSupportFragmentTest extends SingleSupportFragmentTestBase {

    public static class Fragment_setSurfaceViewCallbackBeforeCreate extends VideoSupportFragment {
        boolean mSurfaceCreated;
        @Override
        public View onCreateView(
                LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            setSurfaceHolderCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    mSurfaceCreated = true;
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                           int height) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mSurfaceCreated = false;
                }
            });

            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }

    @Test
    public void setSurfaceViewCallbackBeforeCreate() {
        final SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(Fragment_setSurfaceViewCallbackBeforeCreate.class, 1000);
        Fragment_setSurfaceViewCallbackBeforeCreate fragment1 =
                (Fragment_setSurfaceViewCallbackBeforeCreate) activity.getTestFragment();
        assertNotNull(fragment1);
        assertTrue(fragment1.mSurfaceCreated);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_frame, new Fragment_setSurfaceViewCallbackBeforeCreate())
                        .commitAllowingStateLoss();
            }
        });
        SystemClock.sleep(500);

        assertFalse(fragment1.mSurfaceCreated);

        Fragment_setSurfaceViewCallbackBeforeCreate fragment2 =
                (Fragment_setSurfaceViewCallbackBeforeCreate) activity.getTestFragment();
        assertNotNull(fragment2);
        assertTrue(fragment2.mSurfaceCreated);
        assertNotSame(fragment1, fragment2);
    }

    @Test
    public void setSurfaceViewCallbackAfterCreate() {
        SingleSupportFragmentTestActivity activity = launchAndWaitActivity(VideoSupportFragment.class, 1000);
        VideoSupportFragment fragment = (VideoSupportFragment) activity.getTestFragment();

        assertNotNull(fragment);

        final boolean[] surfaceCreated = new boolean[1];
        fragment.setSurfaceHolderCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                surfaceCreated[0] = true;
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                surfaceCreated[0] = false;
            }
        });
        assertTrue(surfaceCreated[0]);
    }

    public static class Fragment_withVideoPlayer extends VideoSupportFragment {
        MediaPlayerGlue mGlue;
        int mOnCreateCalled;
        int mOnCreateViewCalled;
        int mOnDestroyViewCalled;
        int mOnDestroyCalled;
        int mGlueAttachedToHost;
        int mGlueDetachedFromHost;
        int mGlueOnReadyForPlaybackCalled;

        public Fragment_withVideoPlayer() {
            setRetainInstance(true);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            mOnCreateCalled++;
            super.onCreate(savedInstanceState);
            mGlue = new MediaPlayerGlue(getActivity()) {
                @Override
                protected void onDetachedFromHost() {
                    mGlueDetachedFromHost++;
                    super.onDetachedFromHost();
                }

                @Override
                protected void onAttachedToHost(PlaybackGlueHost host) {
                    super.onAttachedToHost(host);
                    mGlueAttachedToHost++;
                }
            };
            mGlue.setMode(MediaPlayerGlue.REPEAT_ALL);
            mGlue.setArtist("Leanback");
            mGlue.setTitle("Leanback team at work");
            mGlue.setMediaSource(
                    Uri.parse("android.resource://android.support.v17.leanback.test/raw/video"));
            mGlue.addPlayerCallback(new PlaybackGlue.PlayerCallback() {
                @Override
                public void onPreparedStateChanged(PlaybackGlue glue) {
                    if (glue.isPrepared()) {
                        mGlueOnReadyForPlaybackCalled++;
                        mGlue.play();
                    }
                }
            });
            mGlue.setHost(new VideoSupportFragmentGlueHost(this));
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            mOnCreateViewCalled++;
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public void onDestroyView() {
            mOnDestroyViewCalled++;
            super.onDestroyView();
        }

        @Override
        public void onDestroy() {
            mOnDestroyCalled++;
            super.onDestroy();
        }
    }

    @Test
    public void mediaPlayerGlueInVideoSupportFragment() {
        final SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(Fragment_withVideoPlayer.class, 1000);
        final Fragment_withVideoPlayer fragment = (Fragment_withVideoPlayer)
                activity.getTestFragment();

        PollingCheck.waitFor(5000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return fragment.mGlue.isMediaPlaying();
            }
        });

        assertEquals(1, fragment.mOnCreateCalled);
        assertEquals(1, fragment.mOnCreateViewCalled);
        assertEquals(0, fragment.mOnDestroyViewCalled);
        assertEquals(1, fragment.mGlueOnReadyForPlaybackCalled);
        View fragmentViewBeforeRecreate = fragment.getView();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                activity.recreate();
            }
        });

        PollingCheck.waitFor(5000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return fragment.mOnCreateViewCalled == 2 && fragment.mGlue.isMediaPlaying();
            }
        });
        View fragmentViewAfterRecreate = fragment.getView();

        Assert.assertNotSame(fragmentViewBeforeRecreate, fragmentViewAfterRecreate);
        assertEquals(1, fragment.mOnCreateCalled);
        assertEquals(2, fragment.mOnCreateViewCalled);
        assertEquals(1, fragment.mOnDestroyViewCalled);

        assertEquals(1, fragment.mGlueAttachedToHost);
        assertEquals(0, fragment.mGlueDetachedFromHost);
        assertEquals(1, fragment.mGlueOnReadyForPlaybackCalled);

        activity.finish();
        PollingCheck.waitFor(5000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return fragment.mGlueDetachedFromHost == 1;
            }
        });
        assertEquals(2, fragment.mOnDestroyViewCalled);
        assertEquals(1, fragment.mOnDestroyCalled);
    }

}
