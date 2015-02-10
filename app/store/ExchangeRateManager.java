package store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

import play.cache.Cache;
import play.exceptions.UnexpectedException;
import play.libs.F;
import play.libs.WS;
import play.libs.F.Promise;
import play.libs.WS.HttpResponse;
import play.libs.ws.WSAsync.HttpAsyncResponse;
import play.Logger;

public class ExchangeRateManager {
	
	private CassandraStore store;
    private static final ExchangeRateManager instance = new ExchangeRateManager();
	
    private ExchangeRateManager(){
    	store = new CassandraStoreImpl();
    	try {
    		Cache.init();
    	} catch(Exception e){
    		// swallow
    	}
    }
    
    public static ExchangeRateManager getInstance() {
        return instance;
    }
    
    
    /**
     * Retrieve Future object of Exchange rates which
     * will be populated either from API response or from DB or from Cache
     * @param boolean forceRefresh
     * @return Future of rates
     */
    public Future<List<ExchangeRate>> findRecentRates(boolean forceRefresh){
		
    	final boolean needToRefresh = forceRefresh;
    	
		final ExecutorService pool = Executors.newFixedThreadPool(2);
		return pool.submit(new Callable<List<ExchangeRate>>() {
	        @Override
	        public List<ExchangeRate> call() throws Exception {
	        	
	        	final List<ExchangeRate> rates;
	        	
	        	Logger.info("Need to refresh " + needToRefresh);
	        	Logger.info("Store up to date " + store.isUpToDate());
	        	
	        	if( !needToRefresh && store.isUpToDate() ){
	        		Logger.info("Retrieving up to date stored rates");
	        		rates = store.getRates();
	        		
	        		// ensure that both api and db response are sorted
	        		sortRates(rates);
	        		return rates;
	        	}
	        	
	        	rates = ApiRates.getInstance().recentRates(needToRefresh);
        		store.saveRates(rates);
	        	
        		// ensure that both api and db response are sorted
        		sortRates(rates);
	        	return rates;
	        }
		});
	}
    
    private void sortRates(List<ExchangeRate> unsortedRates){
    	Collections.sort(unsortedRates, new ExchangeRateComparator());
    }
    
}
