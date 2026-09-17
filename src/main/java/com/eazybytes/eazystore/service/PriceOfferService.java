package com.eazybytes.eazystore.service;

import org.springframework.stereotype.Service;

@Service
public class PriceOfferService {

    private String currentOffer = "NONE";

    public void setOffer(String offer) {
        this.currentOffer = offer;
    }

    public String getCurrentOffer() {
        return currentOffer;
    }

    public double getDiscountPercentage() {
        return switch (currentOffer) {
            case "FESTIVE" -> 50.0;
            case "WEEKEND" -> 20.0;
            default -> 0.0;
        };
    }
}
