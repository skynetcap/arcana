package com.mmorrell.arcana.strategies.openbook;

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
import org.p2p.solanaj.rpc.types.config.Commitment;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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

    // Dynamic
    private double bestBidPrice;
    private double bestAskPrice;

    // Finals
    @Setter
    private Account mmAccount;

    private Market solUsdcMarket;
    private final MarketBuilder solUsdcMarketBuilder;

    @Setter
    public PublicKey marketId = new PublicKey("9Lyhks5bQQxb9EyyX55NtgKQzpM4WK7JCmeaWuQ5MoXD");

    @Setter
    private PublicKey marketOoa = new PublicKey("7ExfcjBVhi4kjJiZA5WTpEzaUhHtZKgdjFg5wVFxfPvx");

    @Setter
    private PublicKey baseWallet = new PublicKey("3UrEoG5UeE214PYQUA487oJRN89bg6fmt3ejkavmvZ81");

    @Setter
    private PublicKey usdcWallet = new PublicKey("A6Jcj1XV6QqDpdimmL7jm1gQtSP62j8BWbyqkdhe4eLe");
    private static final long BID_CLIENT_ID = 113371L;
    private static final long ASK_CLIENT_ID = 14201L;

    private static final float SOL_QUOTE_SIZE = 2.8877f;
    private static float SOL_ASK_AMOUNT = SOL_QUOTE_SIZE;
    private static float USDC_BID_AMOUNT = SOL_QUOTE_SIZE;
    private static final float ASK_SPREAD_MULTIPLIER = 1.0012f;
    private static final float BID_SPREAD_MULTIPLIER = 0.9987f;
    private static final float MIN_MIDPOINT_CHANGE = 0.0010f;

    private float lastPlacedBidPrice = 0.0f, lastPlacedAskPrice = 0.0f;

    // Leaning
    private static final double WSOL_THRESHOLD_TO_LEAN_USDC = SOL_QUOTE_SIZE;
    private static Optional<Double> USDC_BALANCE = Optional.empty();
    private static Optional<Double> MSOL_BALANCE = Optional.empty();

    // Used to delay 2000ms on first order place.
    private static boolean firstLoadComplete = false;

    public OpenBookSplUsdc(final SerumManager serumManager,
                           final RpcClient rpcClient) {
        this.executorService = Executors.newSingleThreadScheduledExecutor();

        this.serumManager = serumManager;
        this.rpcClient = rpcClient;

        this.solUsdcMarketBuilder = new MarketBuilder()
                .setClient(rpcClient)
                .setPublicKey(marketId)
                .setRetrieveOrderBooks(true);
        this.solUsdcMarket = this.solUsdcMarketBuilder.build();
        this.bestBidPrice = this.solUsdcMarket.getBidOrderBook().getBestBid().getFloatPrice();
        this.bestAskPrice = this.solUsdcMarket.getAskOrderBook().getBestAsk().getFloatPrice();

        // Load private key
//        ClassPathResource resource = new ClassPathResource(
//                "/mikefsWLEcNYHgsiwSRr6PVd7yVcoKeaURQqeDE1tXN.json",
//                ArcanaApplication.class
//        );
//
//        try (InputStream inputStream = resource.getInputStream()) {
//            String privateKeyJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
//            this.mmAccount = Account.fromJson(privateKeyJson);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
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
                        Order bestBid = solUsdcMarket.getBidOrderBook().getBestBid();
                        Order bestAsk = solUsdcMarket.getAskOrderBook().getBestAsk();

                        this.bestBidPrice = bestBid.getFloatPrice();
                        this.bestAskPrice = bestAsk.getFloatPrice();

                        boolean isCancelBid =
                                solUsdcMarket.getBidOrderBook().getOrders().stream().anyMatch(order -> order.getOwner().equals(marketOoa));

                        float percentageChangeFromLastBid =
                                1.00f - (lastPlacedBidPrice / ((float) bestBidPrice * BID_SPREAD_MULTIPLIER));

                        // Only place bid if we haven't placed, or the change is >= 0.1% change
                        if (lastPlacedBidPrice == 0 || (Math.abs(percentageChangeFromLastBid) >= MIN_MIDPOINT_CHANGE)) {
                            placeUsdcBid(USDC_BID_AMOUNT, (float) bestBidPrice * BID_SPREAD_MULTIPLIER, isCancelBid);
                            lastPlacedBidPrice = (float) bestBidPrice * BID_SPREAD_MULTIPLIER;
                        }

                        boolean isCancelAsk =
                                solUsdcMarket.getAskOrderBook().getOrders().stream().anyMatch(order -> order.getOwner().equals(marketOoa));

                        float percentageChangeFromLastAsk =
                                1.00f - (lastPlacedAskPrice / ((float) bestAskPrice * ASK_SPREAD_MULTIPLIER));

                        // Only place ask if we haven't placed, or the change is >= 0.1% change
                        if (lastPlacedAskPrice == 0 || (Math.abs(percentageChangeFromLastAsk) >= MIN_MIDPOINT_CHANGE)) {
                            placeSolAsk(SOL_ASK_AMOUNT, (float) bestAskPrice * ASK_SPREAD_MULTIPLIER, isCancelAsk);
                            lastPlacedAskPrice = (float) bestAskPrice * ASK_SPREAD_MULTIPLIER;
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
                        101_420
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
                        "MEMO"
                )
        );

        try {
            String orderTx = rpcClient.getApi().sendTransaction(placeTx, mmAccount);
            log.info("MSOL Ask: " + askOrder.getFloatQuantity() + " @ " + askOrder.getFloatPrice());
        } catch (RpcException e) {
            log.error("OrderTx Error = " + e.getMessage());
        }
    }

    private void placeUsdcBid(float amount, float price, boolean cancel) {
        final Transaction placeTx = new Transaction();

        placeTx.addInstruction(
                ComputeBudgetProgram.setComputeUnitPrice(
                        101_420
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
                        "MEMO"
                )
        );

        try {
            String orderTx = rpcClient.getApi().sendTransaction(placeTx, mmAccount);
            log.info("USDC Bid: " + bidOrder.getFloatQuantity() + " @ " + bidOrder.getFloatPrice());
        } catch (RpcException e) {
            log.error("OrderTx Error = " + e.getMessage());
        }
    }

    private Optional<Double> getUsdcBalance() {
        try {
            double amount = rpcClient.getApi().getTokenAccountBalance(
                            usdcWallet,
                            Commitment.PROCESSED
                    )
                    .getUiAmount();
            return Optional.of(amount);
        } catch (RpcException e) {
            log.error("Unable to get USDC balance: " + e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Double> getBaseBalance() {
        try {
            double amount = rpcClient.getApi().getTokenAccountBalance(
                            baseWallet,
                            Commitment.PROCESSED
                    )
                    .getUiAmount();
            return Optional.of(amount);
        } catch (RpcException e) {
            log.error("Unable to get Base balance: " + e.getMessage());
            return Optional.empty();
        }
    }

}
