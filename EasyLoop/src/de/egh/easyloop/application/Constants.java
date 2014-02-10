package de.egh.easyloop.application;

import android.media.AudioFormat;

public abstract class Constants {

	public class AudioSettings {

		public static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

		public static final int CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO;
		public static final int CHANNEL_OUT_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
		public static final int FREQUENCY = 44100;
		public static final String RECORD_FILENAME = "loop1.pcm";

	}

	public class PersistantValues {
		public class InChannel {
			public static final String SWITCH = "preferencesInChannelSwitch";
		}

		public class LiveChannel {
			public static final String MUTED = "preferencesLiveDestinationMuted";
			public static final String VOLUME = "preferencesLiveDestinationVolume";
		}

		public class TapeChannel {
			public static final String COUNT_IN_ENABLED = "preferencesTapeDestinationCountInEnabled";
			public static final String MUTED = "preferencesTapeDestinationMuted";
			public static final String VOLUME = "preferencesTapeDestinationVolume";
		}

		/** Settings only for the UI/Activity */
		public class UiOrientation {
			public static final String KEY = "preferencesUiOrientation";
		}

		public static final String NAME = "default";

	}

	/** Keys must be identical to preferences.xml ! */
	public class SharedPreferences {
		public class Orientation {
			public static final String KEY = "preferencesOrientationKey";
			public static final String LANDSCAPE = "landscape";
			public static final String PORTRAIT = "portrait";
			public static final String SENSOR = "sensor";
		}

		public static final String BUFFER_SIZE = "preferencesBufferSizeKey";
		public static final String COUNT_IN_TIME = "preferencesCountInKey";
		public static final String KEEP_SCREEN_ON = "preferencesKeepScreenOnKey";
	}

	public static final int BITS_PER_BYTE = 8;

	public static final int MS_PER_SECOND = 1000;

}
