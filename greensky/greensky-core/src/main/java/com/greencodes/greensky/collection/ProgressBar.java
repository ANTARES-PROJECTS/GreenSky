package com.greencodes.greensky.collection;

/** Barra de progresso em texto do registro de descobertas (README seção 140: "██████████ 100%"). */
public final class ProgressBar {

    private static final char FULL = '█';
    private static final char EMPTY = '░';

    private ProgressBar() {}

    /** Ex.: {@code render(3, 5, 10)} = "██████░░░░ 60%". Arredonda para baixo: 100% só quando completa. */
    public static String render(int done, int total, int width) {
        if (total <= 0 || width <= 0) {
            return "";
        }
        int clamped = Math.max(0, Math.min(done, total));
        int filled = clamped * width / total;
        int percent = clamped * 100 / total;
        return String.valueOf(FULL).repeat(filled) + String.valueOf(EMPTY).repeat(width - filled) + " " + percent + "%";
    }
}
