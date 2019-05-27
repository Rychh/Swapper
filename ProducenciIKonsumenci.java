package swapper;


import java.util.Collection;
import java.util.Collections;

import swapper.Swapper;

public class ProducenciIKonsumenci {
    private static Swapper<Zasob> swapper = new Swapper<>();

    public enum Zasob {
        POBIERZ,
        STWORZ,
        MUTEX
    }

    public static class Produkt {
        Produkt() {
        }
    }

    private static final int POJEMNOSC_MAGAZYNU = 10;
    private static final int ILOSC_PRODUCENTOW = 25;
    private static final int ILOSC_KONSUMENTOW = 25;
    private static volatile int wolneMiejsca = 0;
    private static volatile int zasoby = 0;
    private static volatile Produkt[] magazyn = new Produkt[POJEMNOSC_MAGAZYNU];
    private static volatile int najstarszy = 0;
    private static volatile int najnowszy = 0;

    public static class Producent implements Runnable {
        Collection<Zasob> nic = Collections.emptySet();
        Collection<Zasob> Ochrona = Collections.singleton(Zasob.MUTEX);
        Collection<Zasob> pobranie = Collections.singleton(Zasob.POBIERZ);
        Collection<Zasob> stworzenie = Collections.singleton(Zasob.STWORZ);

        int iloscProduktow;
        long czasProdukcji;
        long czasGraniaNaKompie;

        Producent(int iloscProduktow, long czasProdukcji, long czasGraniaNaKompie) {
            this.iloscProduktow = iloscProduktow;
            this.czasProdukcji = czasProdukcji;
            this.czasGraniaNaKompie = czasGraniaNaKompie;
        }

        void dostarczenie(Produkt produkt) {
            najnowszy = (najnowszy + 1) % POJEMNOSC_MAGAZYNU;
            magazyn[najnowszy] = produkt;
        }

        private Produkt Produkuje() throws InterruptedException {
            Thread.sleep(czasProdukcji);
            return new Produkt();
        }

        private void WlasneSprawy() throws InterruptedException {
            Thread.sleep(czasGraniaNaKompie);
        }

        @Override
        public void run() {
            Produkt produkt;
            try {
                while (iloscProduktow != 0) {
                    iloscProduktow--;
                    WlasneSprawy();
                    swapper.swap(Ochrona, nic);
                    while (wolneMiejsca == 0) {
                        swapper.swap(nic, Ochrona);
                        swapper.swap(stworzenie, nic);
                        swapper.swap(Ochrona, nic);
                    }

                    produkt = Produkuje();
                    dostarczenie(produkt);
                    wolneMiejsca--;
                    zasoby++;

                    swapper.swap(nic, pobranie);
                    swapper.swap(nic, Ochrona);
                }
            } catch (InterruptedException e) {
                Thread aktualny = Thread.currentThread();
                System.out.println("Przerwany Producent:" + aktualny.getName());
                Thread.currentThread().interrupt();
            }
            System.out.println("Zakonczenie pracy Producent:" + Thread.currentThread().getName());

        }
    }


    public static class Konsument implements Runnable {
        Collection<Zasob> nic = Collections.emptySet();
        Collection<Zasob> Ochrona = Collections.singleton(Zasob.MUTEX);
        Collection<Zasob> pobranie = Collections.singleton(Zasob.POBIERZ);
        Collection<Zasob> stworzenie = Collections.singleton(Zasob.STWORZ);

        int iloscPobran;
        long czasKonsumpcji;
        long czasNaWlasneSprawy;

        Konsument(int iloscPobran, long czasKonsumpcji, long czasNaWlasneSprawy) {
            this.iloscPobran = iloscPobran;
            this.czasKonsumpcji = czasKonsumpcji;
            this.czasNaWlasneSprawy = czasNaWlasneSprawy;
        }

        private void konsumuje(Produkt produkt) throws InterruptedException {
            Thread.sleep(czasKonsumpcji);
        }

        private void WlasneSprawy() throws InterruptedException {
            Thread.sleep(czasNaWlasneSprawy);
        }

        Produkt zabranie() {
            Produkt wynik = magazyn[najstarszy];
            najstarszy = (najstarszy + 1) % POJEMNOSC_MAGAZYNU;
            return wynik;
        }

        @Override
        public void run() {
            try {
                Produkt produkt;
                while (iloscPobran != 0) {
                    iloscPobran--;
                    WlasneSprawy();
                    swapper.swap(Ochrona, nic);
                    while (zasoby == 0) {
                        swapper.swap(nic, Ochrona);
                        swapper.swap(pobranie, nic);
                        swapper.swap(Ochrona, nic);
                    }

                    produkt = zabranie();
                    zasoby--;
                    wolneMiejsca++;

                    swapper.swap(nic, stworzenie);
                    swapper.swap(nic, Ochrona);
                    konsumuje(produkt);

                }
            } catch (InterruptedException e) {
                Thread aktualny = Thread.currentThread();
                System.out.println("Przerwany Konsument:" + aktualny.getName());
                Thread.currentThread().interrupt();
            }
            System.out.println("Zakonczenie pracy Konsument:" + Thread.currentThread().getName());
        }
    }


    public static void main(String[] args) throws InterruptedException {
        swapper.swap(Collections.emptySet(), Collections.singleton(Zasob.MUTEX));
        wolneMiejsca = POJEMNOSC_MAGAZYNU;

        for (int i = 1; i <= ILOSC_PRODUCENTOW; i++) {
            new Thread(new Konsument(ILOSC_KONSUMENTOW + i, i / 3, i / 3)).start();
        }
        for (int i = 1; i <= ILOSC_KONSUMENTOW; i++) {
            new Thread(new Producent(ILOSC_PRODUCENTOW + i, i, i)).start();
        }

    }
}
