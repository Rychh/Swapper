package swapper;


import java.util.Collection;
import java.util.Collections;

import swapper.Swapper;

public class CzytelnicyIPisarze {

    private static Swapper<Pozwolenia> swapper = new Swapper<>();

    public enum Pozwolenia {
        NA_PISANIE,
        NA_CZYTANIE,
        MUTEX
    }

    private static final int ILOSC_PISARZY = 15;
    private static final int ILOSC_CZYTELNIKOW = 100;
    private static volatile int iluczyta = 0;
    private static volatile int iluPisze = 0;
    private static volatile int czekajacyCzytelnik = 0;
    private static volatile int czekajacyPisarz = 0;


    public static class Czytelnik implements Runnable {
        Collection<Pozwolenia> nic = Collections.emptySet();
        Collection<Pozwolenia> Ochrona = Collections.singleton(Pozwolenia.MUTEX);
        Collection<Pozwolenia> Pisarze = Collections.singleton(Pozwolenia.NA_PISANIE);
        Collection<Pozwolenia> Czytelnicy = Collections.singleton(Pozwolenia.NA_CZYTANIE);

        int iloscKsiazek;
        long czasCzytania;
        long czasNaWlasneSprawy;

        Czytelnik(int iloscKsiazek, long czasCzytania, long czasNaWlasneSprawy) {
            this.iloscKsiazek = iloscKsiazek;
            this.czasCzytania = czasCzytania;
            this.czasNaWlasneSprawy = czasNaWlasneSprawy;
        }

        private void Czytam() throws InterruptedException {
            Thread.sleep(czasCzytania);
        }

        private void WlasneSprawy() throws InterruptedException {
            Thread.sleep(czasNaWlasneSprawy);
        }

        @Override
        public void run() {
            try {
                while (iloscKsiazek != 0) {
                    iloscKsiazek--;
                    WlasneSprawy();
                    swapper.swap(Ochrona, nic);
                    if (iluPisze + czekajacyPisarz > 0) {
                        czekajacyCzytelnik++;
                        swapper.swap(nic, Ochrona);
                        swapper.swap(Czytelnicy, nic);
                        czekajacyCzytelnik--;
                    }
                    iluczyta++;
                    if (czekajacyCzytelnik > 0) {
                        swapper.swap(nic, Czytelnicy);
                    } else {
                        swapper.swap(nic, Ochrona);
                    }

                    Czytam();

                    swapper.swap(Ochrona, nic);

                    iluczyta--;
                    if ((iluczyta == 0) && czekajacyPisarz > 0) {
                        swapper.swap(nic, Pisarze);
                    } else {
                        swapper.swap(nic, Ochrona);
                    }
                }
            } catch (InterruptedException e) {
                Thread aktualny = Thread.currentThread();
                System.out.println("Przerwany Czytelnik:" + aktualny.getName());
                Thread.currentThread().interrupt();
            }
            System.out.println("Zakonczenie pracy Czytelnika:" + Thread.currentThread().getName());

        }
    }


    public static class Pisarz implements Runnable {
        Collection<Pozwolenia> nic = Collections.emptySet();
        Collection<Pozwolenia> Ochrona = Collections.singleton(Pozwolenia.MUTEX);
        Collection<Pozwolenia> Pisarze = Collections.singleton(Pozwolenia.NA_PISANIE);
        Collection<Pozwolenia> Czytelnicy = Collections.singleton(Pozwolenia.NA_CZYTANIE);

        int iloscPisania;
        long czasPisania;
        long czasNaWlasneSprawy;

        Pisarz(int iloscPisania, long czasPisania, long czasNaWlasneSprawy) {
            this.iloscPisania = iloscPisania;
            this.czasPisania = czasPisania;
            this.czasNaWlasneSprawy = czasNaWlasneSprawy;
        }

        private void Pisze() throws InterruptedException {
            Thread.sleep(czasPisania);
        }

        private void WlasneSprawy() throws InterruptedException {
            Thread.sleep(czasNaWlasneSprawy);
        }

        @Override
        public void run() {
            try {
                while (iloscPisania != 0) {
                    iloscPisania--;
                    WlasneSprawy();
                    swapper.swap(Ochrona, nic);
                    if (iluPisze + czekajacyPisarz > 0) {
                        czekajacyPisarz++;
                        swapper.swap(nic, Ochrona);
                        swapper.swap(Pisarze, nic);
                        czekajacyPisarz--;
                    }
                    iluPisze++;
                    swapper.swap(nic, Ochrona);

                    Pisze();

                    swapper.swap(Ochrona, nic);
                    iluPisze--;
                    if (czekajacyCzytelnik > 0) {
                        swapper.swap(nic, Czytelnicy);
                    } else {
                        if (czekajacyPisarz > 0) {
                            swapper.swap(nic, Pisarze);
                        } else {
                            swapper.swap(nic, Ochrona);
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread aktualny = Thread.currentThread();
                System.out.println("Przerwany Pisarz:" + aktualny.getName());
                Thread.currentThread().interrupt();
            }
            System.out.println("Zakonczenie pracy Pisarza:" + Thread.currentThread().getName());
        }
    }


    public static void main(String[] args) throws InterruptedException {
        swapper.swap(Collections.emptySet(), Collections.singleton(Pozwolenia.MUTEX));
        for (int i = 1; i <= ILOSC_PISARZY; i++) {
            new Thread(new Pisarz(50 + i, i / 4, i / 2)).start();
        }
        for (int i = 1; i <= ILOSC_CZYTELNIKOW; i++) {
            new Thread(new Czytelnik(2 * i + 500, i / 2, i / 2)).start();
        }
    }
}
