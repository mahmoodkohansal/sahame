package ir.mahmood.sahame.controller;

import ir.mahmood.sahame.dto.StockDto;
import ir.mahmood.sahame.exception.TSETMCException;
import ir.mahmood.sahame.service.StockService;
import ir.mahmood.sahame.service.TSETMCService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log4j2
@Component
@RestController
@RequestMapping("/")
public class BaseController {

    private TSETMCService tsetmcService;
    private StockService stockService;

    @Autowired
    public BaseController(TSETMCService tsetmcService, StockService stockService) {
        this.tsetmcService = tsetmcService;
        this.stockService = stockService;
    }

    @GetMapping("/test")
    public String test() throws ExecutionException, InterruptedException {
        try {
//            String stockPrices = tsetmcService.getStockPrices("46348559193224090");
            List<String> newStockIds = tsetmcService.getStockIds();

            log.info("New Stock Ids fetched from TSETMC");

            List<String> persistedStockIds = stockService.list().stream().map(StockDto::getTsetmcId)
                    .collect(Collectors.toList());

            log.info("Persisted Stock Ids fetched from DB");

            List<String> differenceStockIds = newStockIds.stream().filter(Predicate.not(persistedStockIds::contains)).collect(Collectors.toList());

            log.info("Starting to get " + differenceStockIds.size() + " new stock data from TSETMC");

            List<StockDto> stockDtos = new ArrayList<>();
            for (String stockId: differenceStockIds) {
                stockDtos.add(tsetmcService.getStockDetails(stockId).get());
            }
            log.info("Get all stocks data from TSETMC and create DTOs");

            stockService.bulkStore(stockDtos);

            log.info("Stock Details persisted in DB");

            return "Done";
        } catch (TSETMCException e) {
            log.error(e, e);
            return e.getMessage();
        }
    }
}
