package store;

import java.util.List;
import java.util.Map;

public interface CassandraStore {
	// https://github.com/Netflix/recipes-rss/blob/cc2c6daeb22e68d8618cc14b5082aa6a63fdeafe/rss-middletier/src/main/java/com/netflix/recipes/rss/RSSStore.java
	boolean isUpToDate() throws Exception;
	void saveRates(List<ExchangeRate> rates) throws Exception;
	List<ExchangeRate> getRates() throws Exception;
}
