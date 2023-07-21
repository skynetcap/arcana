package com.mmorrell.arcana.strategies.openbook;

import com.mmorrell.arcana.pricing.JupiterPricingSource;
import com.mmorrell.serum.manager.SerumManager;
import com.mmorrell.serum.model.Market;
import com.mmorrell.serum.model.MarketBuilder;
import com.mmorrell.serum.model.Order;
import com.mmorrell.serum.model.OrderTypeLayout;
import com.mmorrell.serum.model.SelfTradeBehaviorLayout;
import com.mmorrell.serum.program.SerumProgram;
import com.mmorrell.arcana.strategies.Strategy;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.core.Transaction;
import org.p2p.solanaj.programs.ComputeBudgetProgram;
import org.p2p.solanaj.programs.MemoProgram;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.RpcException;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

//@Component
@Slf4j
@Getter
public class OpenBookSplUsdc extends Strategy {

    private static final int EVENT_LOOP_INITIAL_DELAY_MS = 0;
    private static final int EVENT_LOOP_DURATION_MS = 5000;

    private final RpcClient rpcClient;
    private final SerumManager serumManager;
    private final ScheduledExecutorService executorService;
    private final JupiterPricingSource jupiterPricingSource;

    // Dynamic
    private boolean useJupiter = false;
    private double bestBidPrice;
    private double bestAskPrice;

    // Finals
    @Setter
    private Account mmAccount;

    private Market solUsdcMarket;
    private final MarketBuilder solUsdcMarketBuilder;

    @Setter
    private PublicKey marketOoa;

    @Setter
    private PublicKey baseWallet;

    @Setter
    private PublicKey usdcWallet;

    private static long BID_CLIENT_ID;
    private static long ASK_CLIENT_ID;

    private static final float SOL_QUOTE_SIZE = 0.1f;

    @Setter
    private float baseAskAmount = SOL_QUOTE_SIZE;

    @Setter
    private float usdcBidAmount = SOL_QUOTE_SIZE;

    @Setter
    private float askSpreadMultiplier = 1.0012f;

    @Setter
    private float bidSpreadMultiplier = 0.9987f;

    private static final float MIN_MIDPOINT_CHANGE = 0.0010f;

    private float lastPlacedBidPrice = 0.0f, lastPlacedAskPrice = 0.0f;

    @Setter
    private Order lastBidOrder;

    @Setter
    private Order lastAskOrder;

    // Used to delay 2000ms on first order place.
    private static boolean firstLoadComplete = false;

    public OpenBookSplUsdc(final SerumManager serumManager,
                           final RpcClient rpcClient,
                           final PublicKey marketId,
                           final JupiterPricingSource jupiterPricingSource,
                           final String pricingStrategy) {
        this.executorService = Executors.newSingleThreadScheduledExecutor();

        this.serumManager = serumManager;
        this.rpcClient = rpcClient;

        this.solUsdcMarketBuilder = new MarketBuilder()
                .setClient(rpcClient)
                .setPublicKey(marketId)
                .setRetrieveOrderBooks(true);
        this.solUsdcMarket = this.solUsdcMarketBuilder.build();
        this.jupiterPricingSource = jupiterPricingSource;

        if (pricingStrategy.equalsIgnoreCase("jupiter")) {
            useJupiter = true;

            Optional<Double> price = jupiterPricingSource.getUsdcPriceForSymbol(solUsdcMarket.getBaseMint().toBase58(),
                    1000);
            if (price.isPresent()) {
                this.bestBidPrice = price.get();
                this.bestAskPrice = price.get();
            }
        }

        BID_CLIENT_ID = ThreadLocalRandom.current().nextLong(1111, 9999999);
        ASK_CLIENT_ID = ThreadLocalRandom.current().nextLong(1111, 9999999);
        log.info("Bid clientId: " + BID_CLIENT_ID + ", Ask: " + ASK_CLIENT_ID);
    }

    @Override
    public void start() {
        log.info(this.getClass().getSimpleName() + " started.");

        // Start loop
        executorService.scheduleAtFixedRate(
                () -> {
                    try {
                        // Get latest prices
                        solUsdcMarket.reload(solUsdcMarketBuilder);

                        if (useJupiter) {
                            Optional<Double> price = jupiterPricingSource.getUsdcPriceForSymbol(solUsdcMarket.getBaseMint().toBase58(),
                                    1000);
                            if (price.isPresent()) {
                                this.bestBidPrice = price.get();
                                this.bestAskPrice = price.get();
                            }
                        } else {
                            this.bestBidPrice = solUsdcMarket.getBidOrderBook().getBestBid().getFloatPrice();
                            this.bestAskPrice = solUsdcMarket.getAskOrderBook().getBestAsk().getFloatPrice();
                        }

                        boolean isCancelBid =
                                solUsdcMarket.getBidOrderBook().getOrders().stream().anyMatch(order -> order.getOwner().equals(marketOoa));

                        float percentageChangeFromLastBid =
                                1.00f - (lastPlacedBidPrice / ((float) bestBidPrice * bidSpreadMultiplier));

                        // Only place bid if we haven't placed, or the change is >= 0.1% change
                        if (lastPlacedBidPrice == 0 || (Math.abs(percentageChangeFromLastBid) >= MIN_MIDPOINT_CHANGE)) {
                            placeUsdcBid(usdcBidAmount, (float) bestBidPrice * bidSpreadMultiplier, isCancelBid);
                            lastPlacedBidPrice = (float) bestBidPrice * bidSpreadMultiplier;
                        }

                        boolean isCancelAsk =
                                solUsdcMarket.getAskOrderBook().getOrders().stream().anyMatch(order -> order.getOwner().equals(marketOoa));

                        float percentageChangeFromLastAsk =
                                1.00f - (lastPlacedAskPrice / ((float) bestAskPrice * askSpreadMultiplier));

                        // Only place ask if we haven't placed, or the change is >= 0.1% change
                        if (lastPlacedAskPrice == 0 || (Math.abs(percentageChangeFromLastAsk) >= MIN_MIDPOINT_CHANGE)) {
                            placeSolAsk(baseAskAmount, (float) bestAskPrice * askSpreadMultiplier, isCancelAsk);
                            lastPlacedAskPrice = (float) bestAskPrice * askSpreadMultiplier;
                        }

                        if (!firstLoadComplete) {
                            try {
                                log.info("Sleeping 2000ms...");
                                Thread.sleep(2000L);
                                log.info("Fist load complete.");
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            firstLoadComplete = true;
                        }
                    } catch (Exception ex) {
                        log.error("Unhandled exception during event loop: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                },
                EVENT_LOOP_INITIAL_DELAY_MS,
                EVENT_LOOP_DURATION_MS,
                TimeUnit.MILLISECONDS
        );
    }

    private void placeSolAsk(float solAmount, float price, boolean cancel) {
        final Transaction placeTx = new Transaction();

        placeTx.addInstruction(
                ComputeBudgetProgram.setComputeUnitPrice(
                        151_420
                )
        );

        placeTx.addInstruction(
                ComputeBudgetProgram.setComputeUnitLimit(
                        54_800
                )
        );

        placeTx.addInstruction(
                SerumProgram.consumeEvents(
                        mmAccount.getPublicKey(),
                        List.of(marketOoa),
                        solUsdcMarket,
                        baseWallet,
                        usdcWallet
                )
        );

        Order askOrder = Order.builder()
                .buy(false)
                .clientOrderId(ASK_CLIENT_ID)
                .orderTypeLayout(OrderTypeLayout.POST_ONLY)
                .selfTradeBehaviorLayout(SelfTradeBehaviorLayout.DECREMENT_TAKE)
                .floatPrice(price)
                .floatQuantity(solAmount)
                .build();

        serumManager.setOrderPrices(askOrder, solUsdcMarket);

        if (cancel) {
            placeTx.addInstruction(
                    SerumProgram.cancelOrderByClientId(
                            solUsdcMarket,
                            marketOoa,
                            mmAccount.getPublicKey(),
                            ASK_CLIENT_ID
                    )
            );
        }


        // Settle - base wallet gets created first then closed after
        placeTx.addInstruction(
                SerumProgram.settleFunds(
                        solUsdcMarket,
                        marketOoa,
                        mmAccount.getPublicKey(),
                        baseWallet, //random wsol acct for settles
                        usdcWallet
                )
        );

        placeTx.addInstruction(
                SerumProgram.placeOrder(
                        mmAccount,
                        baseWallet,
                        marketOoa,
                        solUsdcMarket,
                        askOrder
                )
        );

        placeTx.addInstruction(
                MemoProgram.writeUtf8(
                        mmAccount.getPublicKey(),
                        "Liquidity by Arcana"
                )
        );

        try {
            String orderTx = rpcClient.getApi().sendTransaction(placeTx, mmAccount);
            log.info("Base Ask: " + askOrder.getFloatQuantity() + " @ " + askOrder.getFloatPrice() + ", " + orderTx);
            lastAskOrder = askOrder;
        } catch (RpcException e) {
            log.error("OrderTx Error = " + e.getMessage());
        }
    }

    private void placeUsdcBid(float amount, float price, boolean cancel) {
        final Transaction placeTx = new Transaction();

        placeTx.addInstruction(
                ComputeBudgetProgram.setComputeUnitPrice(
                        151_420
                )
        );

        placeTx.addInstruction(
                ComputeBudgetProgram.setComputeUnitLimit(
                        54_800
                )
        );

        placeTx.addInstruction(
                SerumProgram.consumeEvents(
                        mmAccount.getPublicKey(),
                        List.of(marketOoa),
                        solUsdcMarket,
                        baseWallet,
                        usdcWallet
                )
        );

        Order bidOrder = Order.builder()
                .buy(true)
                .clientOrderId(BID_CLIENT_ID)
                .orderTypeLayout(OrderTypeLayout.POST_ONLY)
                .selfTradeBehaviorLayout(SelfTradeBehaviorLayout.DECREMENT_TAKE)
                .floatPrice(price)
                .floatQuantity(amount)
                .build();

        serumManager.setOrderPrices(bidOrder, solUsdcMarket);

        if (cancel) {
            placeTx.addInstruction(
                    SerumProgram.cancelOrderByClientId(
                            solUsdcMarket,
                            marketOoa,
                            mmAccount.getPublicKey(),
                            BID_CLIENT_ID
                    )
            );
        }


        // Settle - base wallet gets created first then closed after
        placeTx.addInstruction(
                SerumProgram.settleFunds(
                        solUsdcMarket,
                        marketOoa,
                        mmAccount.getPublicKey(),
                        baseWallet, //random wsol acct for settles
                        usdcWallet
                )
        );

        placeTx.addInstruction(
                SerumProgram.placeOrder(
                        mmAccount,
                        usdcWallet,
                        marketOoa,
                        solUsdcMarket,
                        bidOrder
                )
        );

        placeTx.addInstruction(
                MemoProgram.writeUtf8(
                        mmAccount.getPublicKey(),
                        "Liquidity by Arcana"
                )
        );

        try {
            String orderTx = rpcClient.getApi().sendTransaction(placeTx, mmAccount);
            log.info("Quote Bid: " + bidOrder.getFloatQuantity() + " @ " + bidOrder.getFloatPrice() + ", " + orderTx);
            lastBidOrder = bidOrder;
        } catch (RpcException e) {
            log.error("OrderTx Error = " + e.getMessage());
        }
    }

}
