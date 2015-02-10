package store;

import java.util.Comparator;

public class ExchangeRateComparator implements Comparator<ExchangeRate> {
	@Override
    public int compare(ExchangeRate o1, ExchangeRate o2) {
		int c;
	    c = o1.getTime().compareTo(o2.getTime());
	    if (c == 0)
	       c = o1.getCurrency().compareTo(o2.getCurrency());
	    if (c == 0)
	       c = o1.getRate().compareTo(o2.getRate());
	    return c;
    }
}
