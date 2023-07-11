package com.mmorrell.arcana.strategies;

import lombok.Data;
import org.p2p.solanaj.core.PublicKey;

@Data
public class OpenBookBot {

    private Strategy strategy;
    private PublicKey marketId = new PublicKey("8BnEgHoWFysVcuFFX7QztDmzuH8r5ZFvyP3sYwn1XTh6");
    private double bpsSpread;
    private double amountBid, amountAsk;

    private PublicKey ooa;
    private PublicKey baseWallet;
    private PublicKey quoteWallet;

    // marketId, bps spread, amount to bid, amount to ask
    // ooa pubkey, base wallet pubkey, quote pubkey

}
