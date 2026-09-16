package com.example.task02;

public class DiscountBill extends Bill {
    private final int discount;

    public DiscountBill(int discount){
        if (0 > discount || discount > 100){
            throw new IllegalArgumentException("Скидка не может такой быть");
        }
        this.discount = discount;
    }

    public int getDiscount(){
        return discount;
    }

    @Override
    public long getPrice(){
        long basePrice = super.getPrice();
        return basePrice - basePrice * discount / 100;
    }

    public long getDiscountValue(){
        return super.getPrice() - getPrice();
    }

    @Override
    public String toString(){
        return super.toString() +
                "\nСкидка: " + getDiscount() + "%"
                + "\nРазмер скидки: " + getDiscountValue();
    }

}
