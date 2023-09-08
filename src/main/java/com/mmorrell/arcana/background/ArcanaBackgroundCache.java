package com.mmorrell.arcana.background;

import com.mmorrell.serum.model.Market;
import com.mmorrell.serum.model.SerumUtils;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.core.Transaction;
import org.p2p.solanaj.programs.ComputeBudgetProgram;
import org.p2p.solanaj.programs.SystemProgram;
import org.p2p.solanaj.programs.TokenProgram;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.RpcException;
import org.p2p.solanaj.rpc.types.ProgramAccount;
import org.p2p.solanaj.rpc.types.config.Commitment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class ArcanaBackgroundCache {

    private RpcClient rpcClient;

    public ArcanaBackgroundCache(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
        this.cachedMarkets = new ArrayList<>();
    }

    private List<Market> cachedMarkets;

    // Caches: List of all markets, ...
    public List<Market> getCachedMarkets() {
        if (cachedMarkets.isEmpty()) {
            backgroundCacheMarkets();
        }

        return cachedMarkets;
    }

    public void backgroundCacheMarkets() {
        final List<ProgramAccount> programAccounts;
        try {
            programAccounts = new ArrayList<>(
                    rpcClient.getApi().getProgramAccounts(
                            new PublicKey("srmqPvymJeFKQ4zGQed1GFppgkRHL9kaELCbyksJtPX"),
                            Collections.emptyList(),
                            SerumUtils.MARKET_ACCOUNT_SIZE
                    )
            );
        } catch (RpcException e) {
            throw new RuntimeException(e);
        }

        cachedMarkets.clear();
        for (ProgramAccount programAccount : programAccounts) {
            Market market = Market.readMarket(programAccount.getAccount().getDecodedData());

            // Ignore fake/erroneous market accounts
            if (market.getOwnAddress().equals(new PublicKey("11111111111111111111111111111111"))) {
                continue;
            }

            cachedMarkets.add(market);
        }
    }

    public PublicKey wrapSol(Account tradingAccount, Double solAmount) {
        Account sessionWsolAccount = new Account();
        Transaction newTx = new Transaction();
        newTx.addInstruction(
                ComputeBudgetProgram.setComputeUnitPrice(
                        1811_500_000
                )
        );
        newTx.addInstruction(
                ComputeBudgetProgram.setComputeUnitLimit(
                        10_700
                )
        );
        double startingAmount = solAmount;
        newTx.addInstruction(
                SystemProgram.createAccount(
                        tradingAccount.getPublicKey(),
                        sessionWsolAccount.getPublicKey(),
                        (long) (startingAmount * 1000000000.0) + 5039280, //.05 SOL
                        165,
                        TokenProgram.PROGRAM_ID
                )
        );
        newTx.addInstruction(
                TokenProgram.initializeAccount(
                        sessionWsolAccount.getPublicKey(),
                        SerumUtils.WRAPPED_SOL_MINT,
                        tradingAccount.getPublicKey()
                )
        );

        try {
            String txId = rpcClient.getApi().sendTransaction(newTx, List.of(tradingAccount, sessionWsolAccount), null);
        } catch (RpcException e) {
            return PublicKey.valueOf("");
        }

        return sessionWsolAccount.getPublicKey();
    }
}
