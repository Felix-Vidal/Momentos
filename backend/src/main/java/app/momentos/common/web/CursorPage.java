package app.momentos.common.web;

import java.util.List;

/**
 * Paginação por cursor, usada pela linha do tempo: o cursor é opaco para o front
 * e codifica a posição cronológica do último item devolvido.
 */
public record CursorPage<T>(List<T> items, String nextCursor, boolean hasMore) {

    public static <T> CursorPage<T> last(List<T> items) {
        return new CursorPage<>(items, null, false);
    }
}
