package example.classes.mmorpg;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

public class ApiCalls {

    public static void getExchangeRate() throws Exception {
        HttpResponse<String> response =
                Unirest.get("https://currency-exchange.p.rapidapi.com/exchange?q=1.0&from=SGD&to=MYR")
                        .header("x-rapidapi-host", "currency-exchange.p.rapidapi.com")
                        .header("x-rapidapi-key", "14e5a268d5mshd3e603cee2da246p162b02jsnb4e5c036d21a")
                        .asString();
        System.out.println(response.getBody());
    }
}
