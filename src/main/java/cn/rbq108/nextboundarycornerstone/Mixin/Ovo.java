package cn.rbq108.nextboundarycornerstone.Mixin;

public interface Ovo extends Runnable {
    @Override
    default void run() {
        System.out.println("欺 负 一 只 小 居 ？");
        System.out.println("可爱居 —— ——");
    }
}
