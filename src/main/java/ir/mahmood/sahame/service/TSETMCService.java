package ir.mahmood.sahame.service;

import ir.mahmood.sahame.constant.MarketType;
import ir.mahmood.sahame.dto.StockDto;
import ir.mahmood.sahame.exception.TSETMCException;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Log4j2
public class TSETMCService {

    public String getStockAllPrices() {
        String url = "tsev2/data/InstTradeHistory.aspx?i=44818950263583523&Top=999999&A=0";
        HttpResponse<String> response = Unirest.get(url).asString();
        return response.getBody();
    }

    public String getStockPrices(String stockId) throws TSETMCException {
        String url = "tsev2/data/instinfofast.aspx?i=" + stockId + "&c=57+";
        HttpResponse<String> httpResponse = Unirest.get(url).asString();
        String response = httpResponse.getBody();

        if (response == null || response.equals("")) {
            log.error("TSETMC Invalid Response : API Call Response is " + response);
            throw new TSETMCException("TSETMC Invalid Response : API Call Response is " + response);
        } else {
            String[] responseSplittedArray = response.split(",");

            if (responseSplittedArray.length < 3) {
                log.error("TSETMC Short Response : API Call Response is " + response);
                throw new TSETMCException("TSETMC Short Response : API Call Response is " + response);
            }

            return responseSplittedArray[2];
        }
    }

    public List<String> getStockIds() throws TSETMCException {
        log.info("Start getting stockIds");

        String url = "tsev2/data/MarketWatchPlus.aspx";
        HttpResponse<String> httpResponse = Unirest.get(url).asString();
        String response = httpResponse.getBody();

        if (response == null || response.equals("")) {
            log.error("TSETMC Invalid Response : API Call Response is " + response);
            throw new TSETMCException("TSETMC Invalid Response : API Call Response is " + response);
        } else {
            List<String> allMatches = new ArrayList<>();
            Matcher m = Pattern.compile("\\d{15,20}")
                    .matcher(response);
            while (m.find()) {
                allMatches.add(m.group());
            }

            return allMatches;
        }
    }

    @Async
    public CompletableFuture<StockDto> getStockDetails(String stockId) throws TSETMCException {
        log.info("Start getting stock " + stockId + " Detail");

        String url = "Loader.aspx?ParTree=151311&i=" + stockId;
        HttpResponse<String> httpResponse = Unirest.get(url).asString();
        String response = httpResponse.getBody();

        if (response == null || response.equals("")) {
            log.error("TSETMC Invalid Response : API Call Response is " + response);
            throw new TSETMCException("TSETMC Invalid Response : API Call Response is " + response);
        } else {
            StockDto stockDto = new StockDto();
            stockDto.setTsetmcId(stockId);

            // TODO must be modified
            stockDto.setMarketType(MarketType.BOURSE);

            Matcher nameMatcher = Pattern.compile("Title=\\'([^\\']*)\\',")
                    .matcher(response);
            if (nameMatcher.find()) {
                stockDto.setName(nameMatcher.group(1));
            } else {
                stockDto.setName("");
            }

            Matcher symbolMatcher = Pattern.compile("LVal18AFC=\\'([^\\']*)\\',")
                    .matcher(response);
            if (symbolMatcher.find()) {
                stockDto.setSymbol(symbolMatcher.group(1));
            } else {
                stockDto.setSymbol("");
            }

            return CompletableFuture.completedFuture(stockDto);
        }
    }

}
