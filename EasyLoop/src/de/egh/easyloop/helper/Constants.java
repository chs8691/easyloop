package de.egh.easyloop.helper;

import android.media.AudioFormat;

public abstract class Constants {

	public class AudioSettings {

		public static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

		public static final int CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO;
		public static final int CHANNEL_OUT_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
		public static final int FREQUENCY = 44100;
		public static final String RECORD_FILENAME = "loop1.pcm";

	}

	public class SharedPreferences {
		public class PreferencesPlayNr {
			public static final String KEY = "preferencesPlayNr";
		}

	}

	public static final int BITS_PER_BYTE = 8;
	public static final int MS_PER_SECOND = 1000;
}
