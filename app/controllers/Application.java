package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;
import play.libs.WS;
import play.libs.F;
import store.*;

public class Application extends Controller {
	
    public static void index() {
        render();
    }
    
    public static void rates() {
        List<ExchangeRate> data = await(ExchangeRateManager.getInstance().findRecentRates(false));
        renderJSON(data);
    }
    
    public static void refresh() {
        List<ExchangeRate> data = await(ExchangeRateManager.getInstance().findRecentRates(true));
        renderJSON(data);
    }

}