package de.egh.easyloop.unittest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.egh.easyloop.logic.PeakStrategy;
import de.egh.easyloop.logic.PeakStrategy.PeakEventListener;

public class PeakStrategyUnittest {

	@Test
	public void test() {
		final List<Short> actList = new ArrayList<Short>();
		final PeakStrategy ps = new PeakStrategy(new PeakEventListener() {

			@Override
			public void onPeakChanged(final short level) {
				actList.add(level);
			}
		});
	}

}
