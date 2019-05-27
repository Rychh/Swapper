package swapper;

import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Ogólna idea:
 * Swaper posiada mapę z elemntów typów E w semafory. Wątek oczekuję na semaforze z kluczem X typu E,
 * jeżeli w poprzenio X był pierwszym niedostępnym do usunięcia elementem. Wątek zostaje obudzony, jeżeli
 * inny wątek dodał do kolekcji X. Następnie obudzony wątek sprawdza czy są wszystkie dostępne elementy
 * do usunięcia. Jeżeli nie to oczekuje na pierwszy niedostępny element w odpowiednim semaforze, w przeciwnym
 * wypadku usuwa z kolekcji elementy removed, dodaje do kolekcji added i dla każdego elementu z added podnosi
 * odpowiadający mu semafor.
 */

public class Swapper<E> {
    private static Semaphore mutex;
    private Set<E> elementy;
    private Map<E, Semaphore> mapaOczekiwan;

    public Swapper() {
        mutex = new Semaphore(1);
        elementy = new HashSet<>();
        mapaOczekiwan = new HashMap<>();
    }

    public void swap(Collection<E> removed, Collection<E> added) throws InterruptedException {
        boolean czyMogeSwap = false;
        E oczekujeNa = null, wczesniejOczekiwalemNa = null;
        Set<E> usuwane = new HashSet<>(removed);
        Set<E> dodawane = new HashSet<>(added);

        while (!czyMogeSwap && !Thread.currentThread().isInterrupted()) {
            mutex.acquire();
            wczesniejOczekiwalemNa = oczekujeNa;
            oczekujeNa = null;
            czyMogeSwap = true;
            for (E doUsuniecia : usuwane) {
                if (!elementy.contains(doUsuniecia)) {
                    oczekujeNa = doUsuniecia;
                    czyMogeSwap = false;
                    break;
                }
            }

            if (!czyMogeSwap && oczekujeNa != null) {
                if (!oczekujeNa.equals(wczesniejOczekiwalemNa)
                        && elementy.contains(wczesniejOczekiwalemNa)) {
                    mapaOczekiwan.get(wczesniejOczekiwalemNa).release();
                }
                if (!mapaOczekiwan.containsKey(oczekujeNa)) {
                    mapaOczekiwan.put(oczekujeNa, new Semaphore(0));
                }
                mutex.release();
                mapaOczekiwan.get(oczekujeNa).acquire();
                if (Thread.currentThread().isInterrupted()) {
                    mapaOczekiwan.get(oczekujeNa).release();
                }
            }
        }

        if (!Thread.currentThread().isInterrupted()) {
            for (E doUsuniecia : usuwane) {
                elementy.remove(doUsuniecia);
            }
            for (E doDodania : dodawane) {
                elementy.add(doDodania);
                if (mapaOczekiwan.containsKey(doDodania) && mapaOczekiwan.get(doDodania).availablePermits() == 0) {
                    mapaOczekiwan.get(doDodania).release();
                }
            }
            mutex.release();
        } else {
            if (czyMogeSwap) {
                mutex.release();
            }
        }
    }
}