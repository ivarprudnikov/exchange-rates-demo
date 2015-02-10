package store;

import play.libs.F;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.HttpResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import play.cache.Cache;

import play.Logger;

public class ApiRates {
	
	private static final String API_ENDPOINT = "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist-90d.xml";
	private static final String NODE_TIME_KEY = "time";
	private static final String NODE_CURRENCY_KEY = "currency";
	private static final String NODE_RATE_KEY = "rate";
	private static final String CACHE_TTL = "60s";
	private static final String CACHE_KEY = "API_RATES";
	
	private static final ApiRates instance = new ApiRates();
	
    private ApiRates(){}
    
    public static ApiRates getInstance() {
        return instance;
    }
    
    /**
     * Retrieve exchange rates from ECB API endpoint
     * @return rates either from Cache or fresh from API
     */
    public List<ExchangeRate> recentRates(boolean needToRefresh){
    	
    	if(!needToRefresh && Cache.get(CACHE_KEY) != null){
    		Logger.info("Return cached exchange rates in ApiRates.recentRates()");
			return (List<ExchangeRate>)Cache.get(CACHE_KEY);
    	}
    	
    	Logger.info("Call webservice url to get XML of rates in ApiRates.recentRates()");
    	
    	// retrieve api response
    	HttpResponse remoteCall = WS.url(API_ENDPOINT).get();
    	List<ExchangeRate> res = parseXmlRateData(remoteCall);
    	
		boolean added = Cache.safeAdd(CACHE_KEY,res,CACHE_TTL);
		if(!added){
			Cache.set(CACHE_KEY,res,CACHE_TTL);
		}
    	
    	return res; 
    }
    
    /**
     * Parse currency rates API @HttpResponse response
     * @return parsed exchange rates
     */
    private List<ExchangeRate> parseXmlRateData(HttpResponse httpResponse){
    	
    	// convert xml to Map
        Document doc = httpResponse.getXml();
        Element parent = doc.getDocumentElement();
        NodeList children = parent.getChildNodes();
        NodeList dayResults = children.item(2).getChildNodes();
        List<ExchangeRate> dayData = new ArrayList();
        
        for (int i = 0; i < dayResults.getLength(); i++) {
        	
            Node currentNode = dayResults.item(i);
            
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
            	
            	Element eElement = (Element) currentNode;
            	String time = eElement.getAttribute(NODE_TIME_KEY); // yyyy-MM-dd
            	NodeList dayValues = eElement.getChildNodes();
            	
            	for (int j = 0; j < dayValues.getLength(); j++) {
            		Node currencyValueNode = dayValues.item(j);
            		if (currencyValueNode.getNodeType() == Node.ELEMENT_NODE) {
            			
            			Element el = (Element) currencyValueNode;
            			
            			ExchangeRate dayCurrencyValue = new ExchangeRate(
            					time,
            					el.getAttribute(NODE_CURRENCY_KEY),
            					el.getAttribute(NODE_RATE_KEY)
            					);
            			
            			dayData.add(dayCurrencyValue);
            		}
            	}
            }
        }
        
        return dayData;
    	
    }
	
}
