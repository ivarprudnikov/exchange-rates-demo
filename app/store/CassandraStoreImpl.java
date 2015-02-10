package store;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import play.Logger;

import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.CqlResult;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;

import play.cache.Cache;

public class CassandraStoreImpl implements CassandraStore {

	private static final String KEYSPACE_NAME = "CurrencyRatesSpace";
	private static final String FAMILY_NAME = "CurrencyRates";
	private static final String POOL_NAME = "CurrencyRatesPool";
	private static final int DB_PORT = 9160;
	private static final String SEEDS_LOCATION = "127.0.0.1:9160";
	private static final int CONN_PER_HOST = 1;
	private static final String CACHE_KEY = "db_rates";
	private static final String CACHE_TTL = "5mn";
	
	// Useful examples
	// https://github.com/Netflix/astyanax/blob/master/astyanax-examples/src/main/java/com/netflix/astyanax/examples/AstCQLClient.java
	// https://github.com/Netflix/recipes-rss/blob/cc2c6daeb22e68d8618cc14b5082aa6a63fdeafe/rss-middletier/src/main/java/com/netflix/recipes/rss/impl/CassandraStoreImpl.java
	// https://github.com/Netflix/astyanax/wiki/Java-driver-composite-columns
	
	// Useful docs
	// Reading data: https://github.com/Netflix/astyanax/wiki/Reading-Data
	// Writing data: https://github.com/Netflix/astyanax/wiki/Writing-data
	
	// Cassandra keyspace
	private static Keyspace ks;

	private static final ColumnFamily<String, String> CF_RATES = new ColumnFamily<String, String>(FAMILY_NAME, StringSerializer.get(), StringSerializer.get());
	
	/**
	 * Check if todays exchange rates are already in DB
	 */
	@Override
	public boolean isUpToDate() throws Exception {
		boolean is = false;
		Date now = new Date();
		SimpleDateFormat format = new SimpleDateFormat(ExchangeRate.DATE_FORMAT);
		String today = format.format(now);
		try {
			OperationResult<Rows<String, String>> result = getKeyspace()
					.prepareQuery(CF_RATES)
					.searchWithIndex()
					.setLimit(5)
					.addExpression()
					.whereColumn(ExchangeRate.TIME).equals().value(today)
					.execute();
			if( result.getResult().size() > 0 ){
				is = true;
			}
		} catch (ConnectionException e) {
			throw new RuntimeException("failed to read from C*", e);
		}
		
		return is;
	}
	
	/**
	 * Get exchange rates from DB
	 */
	@Override
	public List<ExchangeRate> getRates() throws Exception {
		
		if(Cache.get(CACHE_KEY) != null){
    		Logger.info("Return cached DB rates from CassandraStoreImpl getRates()");
			return (List<ExchangeRate>)Cache.get(CACHE_KEY);
    	}
		
		List<ExchangeRate> lst = new ArrayList<ExchangeRate>();
		Logger.info("Get DB rates");
		
		try {
			OperationResult<CqlResult<String, String>> result = getKeyspace()
					.prepareQuery(CF_RATES)
					.withCql(String.format("SELECT * FROM %s;", FAMILY_NAME))
					.execute();
			for (Row<String, String> row : result.getResult().getRows()) {
				ColumnList<String> cols = row.getColumns();
				ExchangeRate er = new ExchangeRate(
							cols.getStringValue(ExchangeRate.TIME, null),
							cols.getStringValue(ExchangeRate.CURRENCY, null),
							cols.getStringValue(ExchangeRate.RATE, null)
						);
				
				lst.add(er);
			}
		} catch (ConnectionException e) {
			throw new RuntimeException("failed to read from C*", e);
		}
		
		Logger.info("Put DB rates to cache");
		boolean added = Cache.safeAdd(CACHE_KEY,lst,CACHE_TTL);
		if(!added){
			Cache.set(CACHE_KEY,lst,CACHE_TTL);
		}
		
		return lst;
		
	}
	
	/**
	 * Save list of exchange rates which most likely
	 * come from parsed API response.
	 * Could not find documentation which would
	 * show an example of saving in batches
	 */
	@Override
	public void saveRates(List<ExchangeRate> rates) throws Exception {
		
		Logger.info("Saving list of ("+ rates.size() +") rates");
		
		for (ExchangeRate er : rates) {
			insert(er);
		}
	}
	
	/**
	 * Save one exchange rate to DB
	 * @param rate
	 */
	public void insert(ExchangeRate rate) {
		
		if(rate == null){
			throw new RuntimeException("ExchangeRate instance is required");
		}
		
		MutationBatch m;
		
		try {
			m = getKeyspace().prepareMutationBatch();
		} catch (Exception e) {
			throw new RuntimeException("failed to prep keyspace", e);
		}
		
		final String rowKey = rate.getTime() + '-' + rate.getCurrency() + '-' + rate.getRate();
		
		// Setting columns in a standard column
		m.withRow(CF_RATES, rowKey)
		    .putColumn(ExchangeRate.TIME, rate.getTime(), null)
		    .putColumn(ExchangeRate.CURRENCY, rate.getCurrency(), null)
		    .putColumn(ExchangeRate.RATE, rate.getRate(), null);

		try {
		    //OperationResult<Void> result = m.execute();
			m.execute();
		} catch (ConnectionException e) {
			throw new RuntimeException("failed to write data to C*", e);
		}
	}

	/**
	 * Connect to Cassandra
	 */
	@SuppressWarnings({ "unused", "deprecation" })
	private static Keyspace getKeyspace() throws Exception {
		if (ks == null) {
			try {
				AstyanaxContext<Keyspace> context = new AstyanaxContext.Builder()
						.forKeyspace(KEYSPACE_NAME)
						.withAstyanaxConfiguration(
								new AstyanaxConfigurationImpl()
										.setDiscoveryType(NodeDiscoveryType.RING_DESCRIBE))
						.withConnectionPoolConfiguration(
								new ConnectionPoolConfigurationImpl(POOL_NAME)
										.setPort(DB_PORT)
										.setMaxConnsPerHost(CONN_PER_HOST)
										.setSeeds(SEEDS_LOCATION))
						.withConnectionPoolMonitor(
								new CountingConnectionPoolMonitor())
						.buildKeyspace(ThriftFamilyFactory.getInstance());

				context.start();
				
				ks = context.getEntity();
			} catch (Exception e) {
				throw e;
			}
		}

		return ks;
	}

}
