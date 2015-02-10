package store;

import java.io.Serializable;

public class ExchangeRate implements Serializable {
	
	public static final String TIME = "time";
	public static final String CURRENCY = "currency";
	public static final String RATE = "rate";
	public static final String DATE_FORMAT = "yyyy-MM-dd";
	
	private String time;
	private String currency;
	private String rate;
	
    public ExchangeRate() {}

    public ExchangeRate(String time, String currency, String rate) {
        this.time = time;
        this.currency = currency;
        this.rate = rate;
    }
	
	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getRate() {
		return rate;
	}

	public void setRate(String rate) {
		this.rate = rate;
	}

    @Override
    public String toString() {
        return "ExchangeRate [" + time + ", " + currency + ", " + rate + "]";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((time == null) ? 0 : time.hashCode());
        result = prime * result + ((currency == null) ? 0 : currency.hashCode());
        result = prime * result + ((rate == null) ? 0 : rate.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ExchangeRate other = (ExchangeRate) obj;
        boolean equal = true;
        equal &= (time != null) ? (time.equals(other.time)) : other.time == null; 
        equal &= (currency != null) ? (currency.equals(other.currency)) : other.currency == null; 
        equal &= (rate != null) ? (rate.equals(other.rate)) : other.rate == null;
        return equal;
    }
    
    @Override
    public ExchangeRate clone() {
        return new ExchangeRate(time, currency, rate);
    }
    
}
