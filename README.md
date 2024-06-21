# Chaos_Mesh

## Strona tytułowa

**Akronim:** Chaos Mesh  
**Tytuł:** Orchestrate complex fault scenarios   
**Autorzy:** Kuba Janczarski, Jan Rodzoń, Rafał Rodzoń, Maria Tkocz, Damian Tworek  
**Rok, Grupa:** 2024 Grupa 1

## 1. Wstęp

Chaos Mesh to narzędzie służące do testowania odporności systemów na awarie i nieprzewidywalne warunki w środowiskach produkcyjnych. Jest to rodzaj narzędzia do Chaos Engineering, które umożliwia inżynierom testowanie zachowania aplikacji w warunkach, które mogą być chaotyczne lub niestabilne. Eksperymenty przeprowadzane są w klastrze Kubernetes.

Do symulowanych rodzajów awarii należą:
- zakłocenia sieciowe, np. opóźnienia, utraty pakietów, przepuszczalność,
- zakłócenia systemowe, np. awarie dysków, zatrzymania procesów, wysokie zużycie zasobów (CPU, pamięć, dysk),
- zakłócenia procesów aplikacji, np. zabijanie procesów, zatrzymywanie serwisów, zmiany konfiguracji,
- utrata węzła.

Chaos Mesh dostarcza również narzędzia do monitorowania i analizy awarii, co pozwala na szybsze wykrywanie i rozwiązywanie problemów w klastrze Kubernetes.

Poprzez przeprowadzanie eksperymentów z użyciem Chaos Mesh zespoły deweloperskie mogą zrozumieć, jak aplikacja reaguje na zdarzenia powodujące awarie i zwiększyć jej odporność na tego typu sytuacje.

## 2. Tło teoretyczne

Chaos Mesh oferuje różne typy eksperymentów dla Kubernetesa:

- awaria pod'a,
- awaria sieci,
- stress testy,
- błędy przy zapisie/odczycie plików,
- awarie DNS,
- awarie czasowe,
- awarie JVM,
- błędy kernela Linuxa,
- błędy AWS, Azure, Google Cloud Platform,
- błędy HTTP,
- zdarzenia blokujące związane z urządzeniami.

Eksperymenty można uruchamiać cyklicznie lub jednorazowo.

Jako wiodąca platforma testów Chaos w branży Chaos Mesh ma następujące główne zalety:

- **stabilne podstawowe funkcje**: Chaos Mesh wywodzi się z platformy testowej TiDB i odziedziczył wiele istniejących doświadczeń z testów TiDB od chwili swojego pierwszego wydania,
- **w pełni uwierzytelniony**: Chaos Mesh jest używany w licznych firmach i organizacjach, a także w systemach testowych wielu znanych systemów rozproszonych, np. Apache APISIX i RabbitMQ,
- **łatwy w użyciu**: Chaos Mesh w pełni wykorzystuje automatyzację za pomocą operacji graficznych i opartych na Kubernetesie,
- **Cloud Native**: Chaos Mesh wspiera środowisko Kubernetes dzięki swojej potężnej zdolności automatyzacji,
- **różnorodne scenariusze symulacji awarii**: Chaos Mesh obejmuje większość scenariuszy podstawowej symulacji awarii w systemie testowym rozproszonym,
- **elastyczne możliwości orkiestracji eksperymentów**: możliwość projektowania własnych scenariuszy eksperymentów Chaos (w tym eksperymentów mieszanych) oraz sprawdzania stanu aplikacji,
- **bezpieczeństwo**: Chaos Mesh został zaprojektowany z wieloma warstwami kontroli bezpieczeństwa i zapewnia wysoki poziom bezpieczeństwa,
- **łatwa skalowalność**: dodawanie nowych typów testów awarii i funkcji do Chaos Mesh jest łatwe,
- **aktywna społeczność**: Chaos Mesh to inkubowany projekt hostowany przez CNCF. Ma coraz większą liczbę współtwórców i użytkowników na całym świecie.

## Analiza wybranych rodzajów eksperymentów

### 2.1. Awarie JVM (JVM Application Faults)

#### Akcja Return 

Jako helloworld Chaos Mesh podaje następujący przykład akcji Return

```java
public class Main {
    public static void main(String []args) {
        for(int x = 0; x < 1000; x = x+1) {
            try {
                Thread.sleep(1000);
                Main.sayhello(x);
            } catch (Exception e) {
                System.out.println("Got an exception!" + e);
            }
        }
    }

    public static void sayhello(int num) throws Exception {
        try {
            num = getnum(num);
            String s=String.valueOf(num);
            System.out.println(s + ". Hello World");
        } catch (Exception e) { 
            throw e;
        }
    }

    public static int getnum(int num) {
        return num;
    }
}
```

Ten program będzie wypisywał kolejno:

```java
0. Hello World
1. Hello World
2. Hello World
```

Możemy zmienić zachowanie tego programu używając JVMChaos, w taki sposób, żeby zawsze metoda getnum zwracała 9999.

```yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: JVMChaos
metadata:
 name: return
 namespace: helloworld
spec:
 action: return
 class: Main
 method: getnum
 value: '9999'
 mode: all
 selector:
   namespaces:
     - helloworld
```

Wstrzykując poniższą metodą:

```
kubectl apply -f ./jvm-return-example.yaml
```

program wypisuje:

``` java
9999. Hello World
9999. Hello World
9999. Hello World
```

#### Akcja Latency

Akcja Latency polega na zwiększeniu czasu wykonywania konkretnej metody w klasie mierzonej w milisekundach.

#### Akcja Exception

Akcja Exception pozwala na nadpisanie domyślnego zachowania metody i rzucenie wyjątku podanego w akcji.

#### Akcja Stress

Akcja Stress pozwala obciążyć maszynę JVM. Można obciążyć pamięć lub procesor. W przypadku pamięci możemy obciążyć zarówno stack jak i heap.

#### Akcja gc

Akcja gc wymusza garbage collection

#### Akcja ruleData

Akcja ruleData pozwala na odpalenie innych akcji poprzez użycie plików konfiguracyjnych Bytemana, którego chaos mesh pod spodem używa.


### 2.2. Awarie sieci (Network Faults)

#### Akcja Delay

Akcja Delay pozwala na ustawienie opóźnienia na sieci

#### Akcja Partition

Akcja Partition pozwala na ustawienie całkowitego blokowania połączeń między elementami

#### Akcja Bandwidth

Akcja Bandwidth pozwala na ustawienie limitu przepustowości między elementami

#### Akcja Reorder

Akcja Reorder pozwala na ustawienie szansy na przestawienie kolejności otrzymania pakietów

#### Akcja Loss

Akcja Loss pozwala na ustawienie symulacji gubienia części przesyłanych pakietów

#### Akcja Duplicate

Akcja Duplicate pozwala na ustawienie symulacji duplikacji części przesyłanych pakietów

#### Akcja Corrupt

Akcja Corrupt pozwala na ustawienie symulacji uszkodzenia części przesyłanych pakietów

### 2.3. Stress testy

Chaos Mesh pozwala na przeprowadzanie eksperymentów StressChaos pozwalających na symulację scenariuszy stresu wewnątrz kontenerów. Eksperymenty można tworzyć przy użyciu Chaos Dashboard lub bezpośrednio w plikach konfiguracyjnych *.yaml.

W eksperymentach StressChaos dostępne są następujące parametry:
- rodzaj stresu: CPU, pamięć,
- planowany czas trwania eksperymentu,
- tryb eksperymentu: losowy pod lub wybór spośród kwalifikujących się podów – wszystkie, wybrana liczba, wybrany/losowy procent,
- nazwa kontenera, do którego wstrzykiwany jest test,
- selektor określający docelowy pod.

#### 2.3.1. Obciążenie pamięci
W przypadku testów obciążeń pamięci - **MemoryStressor** - dostępne są następujące parametry:
- ```workers``` – liczba wątków obciążających pamięć,
- ```size``` – ostateczny rozmiar zajętej pamięci lub procent całkowitego rozmiaru pamięci,
- ```time``` – czas osiągnięcia rozmiaru pamięci określonego w size. Model wzrostu jest modelem liniowym,
- ```oomScoreAdj``` – modyfikacja wyniku procesu, którego używa linuxowy Out-Of-Memory Killer do określenia, który proces zabić w pierwszej kolejności. Wartość -1000 – proces jest całkowicie chroniony przed zabiciem, wartość +1000 – proces jest najbardziej prawdopodobnym kandydatem do zabicia w przypadku braku pamięci.

#### 2.3.2. Obciążenie procesora
W przypadku testów obciążeń procesora - **CPUStressor** - dostępne są następujące parametry:
- ```workers``` – liczba wątków obciążających procesor,
- ```load``` – procent zajętości procesora. Wartość 0 – nie jest dodawany żaden dodatkowy procesor, wartość 100 – pełne obciążenie. Ostateczna suma obciążenia procesora to workers * load.

#### 2.3.3 Przykładowa konfiguracja stress testu
Przykładowy eksperyment zdefiniowany w pliku *.yaml: utworzenie procesu w wybranym kontenerze, który będzie stale przydzielał oraz odczytywał i zapisywał w pamięci, zajmując do 256MB pamięci:

```yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: StressChaos
metadata:
  name: memory-stress-example
  namespace: chaos-mesh
spec:
  mode: one
  selector:
    labelSelectors:
      'app': 'app1'
  stressors:
    memory:
      workers: 4
      size: '256MB'
```

Eksperyment zostanie utworzony po wykonaniu polecenia:

```
kubectl apply -f <filename>.yaml
```

### 2.4. Architektura Chaos Mesh

Chaos Mesh jest oparty na Kubernetes CRD (Custom Resource Definition). Do zarządzania różnymi eksperymentami definiuje wiele typów CRD opartych na różnych rodzajach awarii i implementuje osobne kontrolery dla różnych obiektów CRD. Do głównych komponentów Chaos Mesh należą:

- **Chaos Dashboard**: komponent wizualizacji Chaos Mesh, oferuje zestaw przyjaznych interfejsów internetowych, za pomocą których użytkownicy mogą manipulować i obserwować eksperymenty Chaos. Zapewnia również mechanizm zarządzania uprawnieniami RBAC.
- **Chaos Controller Manager**: główny komponent logiczny Chaos Mesh odpowiedzialny głównie za harmonogramowanie i zarządzanie eksperymentami Chaos. Zawiera kilka kontrolerów CRD takich jak Kontroler Przepływu Pracy, Kontroler Harmonogramu i Kontrolery różnych rodzajów awarii.
- **Chaos Daemon**: główny komponent wykonawczy. Działa w trybie DaemonSet i ma domyślnie uprawnienia Privileged (które można wyłączyć). Ten komponent głównie ingeruje w określone urządzenia sieciowe, systemy plików, jądra, łącząc się z przestrzeniami nazw docelowego Pod.

### 2.5. Stos technologiczny

Chaos Mesh, Docker, Kubernetes (Minikube), JVM (Java, Kotlin)

## 3. Koncepcja studium przypadku

Aplikacja, która została poddana testom to szyfrowany komunikator z czatem tekstowym i audiorozmową. System ten został zaimplementowany na urządzenia mobilne z systemem operacyjnym Android. Został zaprojektowany w oparciu o architekturę mikroserwisów. Do najważniejszych funkcjonalności aplikacji należą:
- przeglądanie czatów z innymi użytkownikami,
- wysyłanie wiadomości tekstowych i zdjęć w czasie rzeczywistym,
- nawiązywanie połączeń audio,
- rejestracja i logowanie użytkowników,
- import i eksport klucza prywatnego użytkownika używanego do odszyfrowywania wiadomości zaszyfrowanych jego kluczem publicznym,
- możliwość integracji z API systemu dowolnego innego klienta oferującego usługę videorozmów.

### 3.1. Architektura systemu

System opiera się na architekturze mikroserwisów. Wydzielono trzy mikroserwisy, każdy z nich posiada swoją własną bazę danych i jest niezależny od pozostałych. Jedną z głównych zalet takiego rozwiązania jest skalowalność - w sytuacji, gdy obciążenie systemu zwiększy się w konkretnym mikroserwisie to nie ma potrzeby duplikowania całej aplikacji. Takie podejście jest wymagane w przypadku skalowania horyzontalnego dla monolitu. W architekturze mikroserwisów duplikujemy tylko najbardziej obciążony mikroserwis. Kolejną zaletą tej architektury jest niezależność, dzięki której zalogowani użytkownicy mogą swobodnie korzystać z aplikacji nawet kiedy serwis odpowiedzialny za logowanie przestanie działać.

<br>
<p align="center">
<img src="https://github.com/Vertemi/Chaos_Mesh/assets/72327045/65e25d5d-2493-48b8-9ce1-26318cdd1973" width="500">
</p>

#### 3.1.1. Backend

Backend składa się z trzech mikroserwisów: serwera autoryzacyjngo, chatu i połączeń. Endpointy pogrupowano ze względu na funkcjonalności. W każdej grupie funkcjonalności znajdują się takie pakiety jak:
- *controller* - zawiera klasy punktu wejścia do aplikacji, posiada metody HTTP takie jak POST, PATCH, PUT, GET oraz DELETE. Dzięki nim możliwa jest realizacja operacji CRUD. Kontroler po otrzymaniu konkretnego zapytania HTTP od klienta deleguje otrzymaną informację do serwisu, który wykonuje zadaną czynność.
- *service* - zawiera klasy serwisów, które są odpowiedzialne za wykonanie zadanego zapytania HTTP, np. utworzenie zasobu, aktualizację istniejącego zasobu lub usunięcie wskazanych zasobów. W celu aktualizacji konkretnego zasobu serwis za pośrednictwem repozytorium pobiera z bazy danych konkretny rekord tabeli w postaci encji bazodanowej (obiekt *DAO*, ang. *Data Access Object*) na podstawie otrzymanego od klienta ID obiektu. Następnie aktualizuje wybrane pola obiektu *DAO* na podstawie zapytania od klienta, zapisuje zaktualizowany obiekt *DAO* w bazie danych oraz zwraca odpowiednią informację do kontrolera, który oddelegowuje ją do klienta.
- *repository* - zawiera interfejsy repozytorium, z których każdy jest skorelowany z konkretną tabelą w bazie danych. Tabela reprezentowana jest w kodzie jako obiekt *DAO*.
- *model* - zawiera klasy zapytań od klienta (requesty), obiekty *DTO* (ang. *Data Transfer Object*, obiekty transferowane z klienta do serwera i odwrotnie) oraz klasy reprezentujące konkretne tabele w bazie danych - encje bazodanowe *DAO*.

<br>
<p align="center">
<img src="https://github.com/Vertemi/Chaos_Mesh/assets/72327045/6eb12e25-16fe-4726-8878-9d85e8545caa" width="700">
</p>

#### 3.1.2. Frontend
    
Projekt klienta to aplikacja na system Android, która łączy się z konkretnymi mikroserwisami za pomocą protokołów HTTP oraz WS. Komunikacja pomiędzy klientem i serwisami odbywa się za pomocą formatu JSON zgodnie ze standardem REST.

Pliki projektu zostały pogrupowane w pakiety dotyczące wybranych ekranów aplikacji. Każdy ekran widoczny w aplikacji posiada odpowiadającą mu klasę dziedziczącą po klasie AppCompatActivity oraz plik XML definiujący jego wygląd. Ekran, który generuje zapytanie HTTP do serwera posiada swój interfejs, w którym zdefiniowane są wszystkie możliwe zapytania HTTP danego ekranu. Dane są przekazywane pomiędzy widokami za pomocą intentów co minimalizuje liczbę requestów HTTP do serwera.

### 3.2. Stos technologiczny i narzędzia

W projekcie aplikacji wykorzystano następujące technologie:

- aplikacja serwera: język Java, framework Spring Boot,
- aplikacja klienta: język Kotlin, środowisko Android Studio,
- baza danych PostgreSQL,
- Docker,
- Git.

### 3.3. Demo aplikacji

#### 3.3.1. Przypadki użycia

Przypadki użycia występujące w aplikacji przedstawiono w formie diagramu UML.

<br>
<p align="center">
<img src="https://github.com/Vertemi/Chaos_Mesh/assets/72327045/43401ee1-cadd-465c-842a-5bb47465848e" width="600">
</p>

#### 3.3.2. Scenariusze przypadków użycia

**Wysłanie wiadomości tekstowej do wybranego użytkownika**

Zalogowany użytkownik z poziomu widoku prywatnych konwersacji lub widoku wyszukiwania użytkowników wybiera osobę, do której chce wysłać wiadomość. Po kliknięciu na danego użytkownika wybiera opcję wysłania wiadomości „Send message”. Po przejściu na ekran konwersacji z wybranym użytkownikiem naciska na pole tekstowe na dole ekranu z napisem „Write message here…”. Po pojawieniu się klawiatury użytkownik wprowadza treść wiadomości i zatwierdza jej przesłanie przyciskiem „Send”. Wiadomość zostaje przesłana do odbiorcy i pojawia się w widoku konwersacji.

**Przesłanie zdjęcia w wybranej konwersacji**

Zalogowany użytkownik wybiera osobę, do której chce przesłać zdjęcie z poziomu widoku prywatnych konwersacji lub widoku wyszukiwania użytkowników. Po kliknięciu na danego użytkownika wybiera opcję wysłania wiadomości „Send message”. Po przejściu na ekran konwersacji z wybranym użytkownikiem naciska na przycisk dodawania zdjęcia w lewym dolnym rogu ekranu. Wyświetlane jest okno dialogowe umożliwiające wybór zdjęcia z plików przechowywanych na urządzeniu. Wygląd okna jest zależny od systemu operacyjnego urządzenia. Użytkownik wybiera zdjęcie. Po wybraniu następuje wysłanie zdjęcia do odbiorcy.

**Nawiązanie połączenia audio z innym użytkownikiem**

Zalogowany użytkownik wybiera osobę, z którą chce nawiązać połączenie audio. Może tego dokonać w widoku prywatnych konwersacji lub w widoku wyszukiwania użytkowników. Po kliknięciu na wybraną osobę wyświetlane są dodatkowe opcje, z których użytkownik wybiera opcję wysłania wiadomości „Send message”. Po przejściu na ekran konwersacji z wybranym użytkownikiem należy nacisnąć przycisk „Call” w prawym górnym rogu ekranu. Wyświetlone zostanie okno dialogowe z wyborem dostawcy oprogramowania obsługującego połączenia. Użytkownik wybiera jedną z opcji, po czym następuje próba nawiązania połączenia z odbiorcą. Do czasu odebrania połączenia przez odbiorcę nadawany jest sygnał dźwiękowy dzwonienia. W przypadku odebrania połączenia przez odbiorcę użytkownicy mogą ze sobą rozmawiać. Opcjonalnie mogą wyciszyć mikrofon. W przypadku anulowania połączenia przychodzącego przez nadawcę lub po upływie minuty połączenie jest przerywane.

**Eksportowanie klucza prywatnego**

Zalogowany użytkownik na głównym ekranie wybiera opcję menu – przycisk z trzema kropkami położony w prawym górnym rogu ekranu. Z listy dostępnych operacji wybiera opcję „Export private key”. Po wyświetleniu okna dialogowego z prośbą o potwierdzenie operacji hasłem użytkownik podaje swoje hasło i zatwierdza czynność przyciskiem „Accept”. Wyświetlane jest okno dialogowe umożliwiające zapis pliku z kluczem prywatnym w wybranej lokalizacji w pamięci urządzenia. Wygląd okna jest zależny od systemu operacyjnego urządzenia. Użytkownik wybiera miejsce, w którym chce zapisać plik oraz opcjonalnie zmienia jego nazwę. Zatwierdza operację przyciskiem „Save”.

**Importowanie klucza prywatnego**

Zalogowany użytkownik na głównym ekranie wybiera opcję menu – przycisk z trzema kropkami położony w prawym górnym rogu ekranu. Z listy dostępnych operacji wybiera opcję „Import private key”. Po wyświetleniu okna dialogowego z informacją o możliwości utraty klucza prywatnego użytkownik akceptuje ostrzeżenie. Wyświetlane jest okno dialogowe umożliwiające wybór pliku z kluczem prywatnym z wybranej lokalizacji w pamięci urządzenia. Wygląd okna jest zależny od systemu operacyjnego urządzenia. Użytkownik wybiera plik z kluczem prywatnym i potwierdza operację importu.

#### 3.3.3. Widoki aplikacji

Zamieszczono zrzuty ekranu przedstawiające przykładowe widoki aplikacji.

<br>
<p align="center">Przykładowy czat z innym użytkownikiem</p>
<p align="center">
<img src="https://github.com/Vertemi/Chaos_Mesh/assets/72327045/f033b36f-b62a-43a4-8124-b2f8d1732973" width="300">
</p>

<br>
<p align="center">Przesłanie zdjęcia</p>
<p align="center">
<img src="https://github.com/Vertemi/Chaos_Mesh/assets/72327045/f565f5d3-e6e5-4235-b6af-3c408563e3c2" width="300">
</p>

<br>
<p align="center">Połączenie przychodzące</p>
<p align="center">
<img src="https://github.com/Vertemi/Chaos_Mesh/assets/72327045/a91590de-b1c9-4101-a7f5-5cbce10e3e09" width="300">
</p>

<br>
<p align="center">Rozmowa głosowa</p>
<p align="center">
<img src="https://github.com/Vertemi/Chaos_Mesh/assets/72327045/53a62bff-5d2e-4dee-836b-198f85bc2751" width="300">
</p>

<br>
<p align="center">Import klucza prywatnego</p>
<p align="center">
<img src="https://github.com/Vertemi/Chaos_Mesh/assets/72327045/8b84eb24-fb0d-4904-a623-8f2afc975e37" width="300">
</p>

<br>
<p align="center">Informacja o błędnym kluczu prywatnym użytkownika</p>
<p align="center">
<img src="https://github.com/Vertemi/Chaos_Mesh/assets/72327045/da833424-6056-4e34-ba89-71764909553c" width="300">
</p>

#### 3.3.4. Film demo aplikacji

Załączono film prezentujący przykładowe użytkowanie aplikacji:

https://github.com/Sweepner/Chaos_Mesh/assets/72269056/127a4655-bb66-4a76-919e-9c0688dbd4e5

## 4. Architektura rozwiązania

Wysokopoziomowe spojrzenie na architekturę rozwiązania przedstawiono za pomocą diagramu. Wyszczególniono najważniejsze elementy klastra.

![Chaos_Mesh_arch](https://github.com/Sweepner/Chaos_Mesh/assets/72269056/90a9de3e-2e66-4554-896f-fb36e2f3baa9)


## 5. Konfiguracja środowiska

### 5.1 Przygotowanie środowiska
Środowisko utworzono na własnym sprzęcie. Zakupiono serwer posiadający 32 GB ramu oraz 1 TB SSD. Za pomocą frps - https://github.com/fatedier/frp wystawiono serwer do internetu. W celu posiadania adresu IP za NATem skorzystano z zawsze darmowej maszyny wirtualnej Oracle (dalej nazwanej tutaj proxy) z 1 GB ramu oraz 1 corem. Maszyna ta służy jako proxy do prywatnej maszyny. Na proxy uruchomiono frps w następującej konfiguracji (frps.toml - plik konfiguracyjny):

```
[common]
bindPort = 7000
vhost_https_port = 443
tls_enable = true
tls_cert_file = /etc/letsencrypt/live/www.rodzon.site/fullchain.pem
tls_key_file = /etc/letsencrypt/live/www.rodzon.site/privkey.pem
```
Stworzono usługę systemd dla frps w celu automatycznego uruchomienia przekierowania na prywatny serwer po restarcie proxy. Zawartość pliku frps.service:

```
[Unit]
Description=FRPS Service
After=network-online.target

[Service]
Type=simple
ExecStart=/usr/local/bin/frps -c /usr/local/bin/frps.toml

[Install]
WantedBy=multi-user.target
```

Uruchomiono usługę komendą:

```
sudo systemctl start frps.service
```

Dodano usługę do autostartu maszyny komendą:
```
sudo systemctl enable frps.service
```

Aby frps oraz frpc komunikowały się otwarto porty 7000 oraz 6000 na proxy używając komend:
```
sudo firewall-cmd --permanent --zone=public --add-port=7000/udp
sudo firewall-cmd --permanent --zone=public --add-port=7000/tcp
sudo firewall-cmd --permanent --zone=public --add-port=6000/udp
sudo firewall-cmd --permanent --zone=public --add-port=6000/tcp
sudo firewall-cmd --reload
```
Port 7000 służy do komunikacji pomiędzy klientem a serwerem frp. Port 6000 służy do przekierowania na port 22 na prywatnej maszynie.


Na prywatnej maszynie zainstalowano system operacyjny Rocky Linux - https://rockylinux.org/pl/. Następnie skonfigurowano oraz uruchomiono frpc z następującą konfiguracją (zawartość pliku frpc.toml):
```
serverAddr = "158.101.209.102"
serverPort = 7000

[[proxies]]
name = "ssh"
type = "tcp"
localIP = "127.0.0.1"
localPort = 22
remotePort = 6000

[[proxies]]
name = "postgres"
type = "tcp"
localIP = "127.0.0.1"
localPort = 5432
remotePort = 5432

[[proxies]]
name = "http"
type = "tcp"
localIP = "127.0.0.1"
localPort = 80
remotePort = 80

[[proxies]]
name = "https"
type = "tcp"
localIP = "127.0.0.1"
localPort = 443
remotePort = 443
```

W celu umożliwienia przekierowania z portu 6000 na proxy na port 22 na lokalnej maszynie wystawiono port 22 na prywatnej maszynie za pomocą komend:

```
sudo firewall-cmd --permanent --zone=public --add-port=22/udp
sudo firewall-cmd --permanent --zone=public --add-port=22/tcp
sudo firewall-cmd --reload
```

Uruchomiono  usługę ssh na porcie 22.

W celu umożliwienia zdalnego rebootowania prywatnego serwera utworzono usługę systemd dla frpc.
Plik frpc.service:

```
[Unit]
Description=FRPC Service
After=network-online.target

[Service]
Type=simple
ExecStart=/usr/local/bin/frpc -c /usr/local/bin/frpc.toml

[Install]
WantedBy=multi-user.target
```
Uruchomiono usługę komendą:

```
sudo systemctl start frpc.service
```

Dodano usługę do autostartu maszyny komendą:
```
sudo systemctl enable frpc.service
```

Na prywatnym serwerze zainstalowano:
- minikube
- docker
- kubectl

### 5.2 Uruchomienie usług w Kubernetesie

#### 5.2.1 Postgres

Utworzono pliki do deploymentu bazy danych Postgres w Kubernetesie:

Utworzenie konfig mapy:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-config
  labels:
    app: postgres
data:
  POSTGRES_DB: postgres
  POSTGRES_USER: postgres
  POSTGRES_PASSWORD: myPassword
```

```
kubectl apply -f postgres-configmap.yaml 
```

Utworzenie volumenu dla kontenera:

```yaml
kind: PersistentVolume
apiVersion: v1
metadata:
  name: postgres-pv-volume
  labels:
    type: local
    app: postgres
spec:
  storageClassName: manual
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteMany
  hostPath:
    path: "/mnt/data"
---
kind: PersistentVolumeClaim
apiVersion: v1
metadata:
  name: postgres-pv-claim
  labels:
    app: postgres
spec:
  storageClassName: manual
  accessModes:
    - ReadWriteMany
  resources:
    requests:
      storage: 10Gi
```

```
kubectl apply -f postgres-volume.yaml
```

Utworzenie deploymentu oraz serwisu:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres-deployment
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
        - name: postgres
          image: postgres:latest
          ports:
            - containerPort: 5432
          envFrom:
            - configMapRef:
                name: postgres-config
          volumeMounts:
            - mountPath: /var/lib/postgresql/data
              name: postgres-volume
      volumes:
        - name: postgres-volume
          persistentVolumeClaim:
            claimName: postgres-pv-claim
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  labels:
    app: postgres
spec:
  type: NodePort
  ports:
    - port: 5432
  selector:
    app: postgres
```

```
kubectl apply -f postgres-deployment.yaml
```
#### 5.2.2 Spring Boot

Utworzenie plików do deploymentu aplikacji Spring Bootowych w Kubernetesie:

#### 5.2.2.1 Serwis autoryzacyjny:

Deployment:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: chat-authorization-deployment
spec:
  replicas: 1
  selector:
    matchLabels:
      app: chat-authorization-deployment
  template:
    metadata:
      labels:
        app: chat-authorization-deployment
    spec:
      containers:
        - name: chat-authorization
          image: rodzonvm/chat-authorization-service:1.0.0
          ports:
            - containerPort: 8081
```

Serwis:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: chat-authorization-service
spec:
  type: LoadBalancer
  selector:
    app: chat-authorization-deployment
  ports:
    - protocol: TCP
      port: 8081
      targetPort: 8081
```

#### 5.2.2.2 Serwis do wysyłania wiadomości:

Deployment:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: chat-message-deployment
spec:
  replicas: 1
  selector:
    matchLabels:
      app: chat-message-deployment
  template:
    metadata:
      labels:
        app: chat-message-deployment
    spec:
      containers:
        - name: chat-message
          image: rodzonvm/chat-message-service:1.0.0
          ports:
            - containerPort: 8082

```

Serwis:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: chat-message-service
spec:
  type: LoadBalancer
  selector:
    app: chat-message-deployment
  ports:
    - protocol: TCP
      port: 8082
      targetPort: 8082

```

#### 5.2.2.3 Serwis do audio rozmów:

Deployment:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: chat-call-deployment
spec:
  replicas: 1
  selector:
    matchLabels:
      app: chat-call-deployment
  template:
    metadata:
      labels:
        app: chat-call-deployment
    spec:
      containers:
        - name: chat-call
          image: rodzonvm/chat-call-service:1.0.0
          ports:
            - containerPort: 8083

```

Serwis:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: chat-call-service
spec:
  type: LoadBalancer
  selector:
    app: chat-call-deployment
  ports:
    - protocol: TCP
      port: 8083
      targetPort: 8083

```

Wszystki trzy aplikacje uruchomiono w następujący sposób:

```
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
```

### 5.3 Ingress route do klastra kubernetesowego.

Aby nie wystawiać wielu portów oparliśmy roting do mikroserwisów aplikacji na path prefixach.

#### 5.3.1 Traefik

Jako ingress użyto technologii Traefik. Zainstalowano go za pomocą paczki helm. Niestandardową konfigurację umieściliśmy w pliku values.yaml:

``` yaml
image:
  name: traefik
  tag: v3.0.3

service:
  enabled: true
  type: LoadBalancer
  annotations: {}
  spec:
    externalTrafficPolicy: Local
  externalIPs:
    - 10.102.13.33
  ports:
    web:
      port: 80
      expose: true
      exposedPort: 80
      protocol: TCP
# Configuring logs
logs:
  general:
    level: TRACE
  access:
    enabled: true
    fields:
      general:
        defaultMode: keep

rbac:
  enabled: true


additionalArguments:
  - "--api.dashboard=true"
```

Instalacja traefika:

```
helm install traefik traefik/traefik -f values.yaml
```

#### 5.3.2 Konfiguracja routingu

#### 5.3.2.1 ChatWithMe app

``` yaml
apiVersion: traefik.io/v1alpha1
kind: IngressRoute
metadata:
  name: spring-boot-ingressroute
spec:
  entryPoints:
    - web
  routes:
    - match: PathPrefix(`/chat/authorization`)
      kind: Rule
      services:
        - name: chat-authorization-service
          port: 80
      middlewares:
        - name: chat-authorization-strip-prefix
    - match: PathPrefix(`/chat/message`)
      kind: Rule
      services:
        - name: chat-message-service
          port: 80
      middlewares:
        - name: chat-message-strip-prefix
    - match: PathPrefix(`/chat/call`)
      kind: Rule
      services:
        - name: chat-call-service
          port: 80
      middlewares:
        - name: chat-call-strip-prefix
```

Aby aplikacja działała poprawie należało wyciąć prefixy służące do routingu.

``` yaml
apiVersion: traefik.io/v1alpha1
kind: Middleware
metadata:
  name: chat-authorization-strip-prefix
spec:
  stripPrefix:
    prefixes:
      - "/chat/authorization"
---
apiVersion: traefik.io/v1alpha1
kind: Middleware
metadata:
  name: chat-call-strip-prefix
spec:
  stripPrefix:
    prefixes:
      - "/chat/call"
---
apiVersion: traefik.io/v1alpha1
kind: Middleware
metadata:
  name: chat-message-strip-prefix
spec:
  stripPrefix:
    prefixes:
      - "/chat/message"
```

#### 5.3.2.2 Kubernetes dashboard

``` yaml
apiVersion: traefik.io/v1alpha1
kind: IngressRoute
metadata:
  name: minikube-dashboard-ingressroute
  namespace: kubernetes-dashboard
spec:
  entryPoints:
    - web
  routes:
    - match: PathPrefix(`/`)
      kind: Rule
      middlewares:
        - name: kubernetes-dashboard-auth
      services:
        - name: kubernetes-dashboard
          port: 80
---
apiVersion: traefik.io/v1alpha1
kind: Middleware
metadata:
  name: kubernetes-dashboard-auth
  namespace: kubernetes-dashboard
spec:
  basicAuth:
    secret: kubernetes-dashboard-auth
    removeHeader: true
```

Wszystkie pliki yaml należy zaaplikować do klastra za pomocą komendy:

```
kubectl apply -f <nazwa_pliku.yaml>
```

## 6. Metoda instalacji

### 6.1. Instalacja aplikacji mobilnej

W celu użytkowania aplikacji należy zainstalować ją na wybranym urządzeniu z systemem operacyjnym Android.

Minimalne wymagania sprzętowe i programowe:
- 150MB wolnego miejsca na dysku,
- 2GB pamięci RAM,
- system operacyjny w wersji *Nougat* (Android 7 - SDK 24) lub wyższej.

W celu zainstalowania aplikacji należy pobrać udostępniony przez administratora systemu plik .apk programu oraz zainstalować go na urządzeniu mobilnym spełniającym minimalne wymagania sprzętowe i programowe.

### 6.2. Instalacja aplikacji Chaos Mesh

Platformę Chaos Mesh zainstalowano z użyciem helm chart.

```
helm install chaos-mesh chaos-mesh/chaos-mesh --namespace=chaos-mesh --create-namespace
```

## 7. Sposób odtworzenia krok po kroku

Przygotowano skrypty, które stawiają całe środowisko od zera:

- [skrypt resetujący środowisko minikube](https://github.com/Sweepner/Chaos_Mesh/blob/main/k8s/resetMinikube.sh)

- [skrypt uruchamiający środowisko od zera](https://github.com/Sweepner/Chaos_Mesh/blob/main/k8s/startEverything.sh)

- [skrypt uruchamiający Kubernetes Dashboard](https://github.com/Sweepner/Chaos_Mesh/blob/main/k8s/startDashboard.sh)

Konfiguracja przykładowych eksperymentów znajduje się w folderze [experiments](https://github.com/Sweepner/Chaos_Mesh/tree/main/experiments). Szczegółowe informacje na temat uruchomienia i monitoringu poszczególnych testów zawarto w kolejnym punkcie dokumentacji.

## 8. Demonstracyjny sposób wdrożenia

### 8.1. JVM

Eksperyment polega na wydłużeniu czasu procesowania metody wysyłania wiadomości.
Po zaaplikowaniu poniższego pliku do kubernetessa, czas wysyłania każdej wiadomości w aplikacji jest opóźniony o 3 sekundy.

Plik delay_messages.yaml

```yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: JVMChaos
metadata:
  name: delay-messages
  namespace: chaos-mesh
spec:
  action: latency
  class: ChatController
  method: sendRealTimeMessage
  latency: 3000
  mode: one
  selector:
    namespaces:
      - default
    labelSelectors:
      app: ch
at-message-deployment
```

Uruchomiemie eksperymentu:

```
kubectl apply -f delay_messages.yaml
```

Demonstracja działania:


https://github.com/Falon452/Chaos_Mesh/assets/64365037/11a69047-3378-4296-9d36-98e0a9459319



### 8.2. Pod Failure

Eksperyment polega na spowodowaniu awarii poda z użyciem technologii Chaos Mesh. Zaaplikowanie do kubernetesa następującego pliku yaml powoduje awarię poda wybranego w pliku (w tym wypadku odpowiadającego za autoryzację użytkowników).

Plik pod_authorization_failure.yaml:

``` yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: PodChaos
metadata:
  name: pod-failure-example
  namespace: chaos-mesh
spec:
  action: pod-failure
  mode: one
  duration: '30s'
  scheduler:
    cron: '@every 1m'
  selector:
    namespaces:
      - default
    labelSelectors:
      app: chat-authorization-deployment
```

Uruchomiemie eksperymentu:

```
kubectl apply -f pod_authorization_failure.yaml
```

Na poniższym filmie przedstawiono poprawne działanie eksperymentu.

[ChaosMesh_pod_failure.webm](https://github.com/Sweepner/Chaos_Mesh/assets/72269056/7368f5f1-6d2b-49d2-b2e7-0212e959e339)

### 8.3. Massive Pod Failure

Eksperyment polega na spowodowaniu masowych awarii losowych podów z użyciem technologii Chaos Mesh. Utworzenie poniższego pliku yaml oraz uruchomienie poniższego skryptu bashowego rozpoczyna eksperyment.
Skrypt co 5 sekund aplikuje plik yaml, który powoduje awarię losowego poda a następnie usuwa utworzony typ PodChaos co kończy pętlę. Pętla trwa do czasu przerwania skryptu przez użytkownika.

Plik massive_pod_failure.yaml:

``` yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: PodChaos
metadata:
  name: pod-kill-all
  namespace: chaos-mesh
spec:
  action: pod-kill
  mode: one
  selector:
    namespaces:
      - default
```

Skrypt bashowy aplikujący i usuwający PodChaos (startMassiveDestruction.sh):

``` bash
while true; do
  kubectl apply -f massive_pod_failure.yaml
  sleep  5
  if  kubectl get PodChaos -n chaos-mesh | grep pod-kill-all; then
    kubectl delete PodChaos pod-kill-all -n chaos-mesh
  fi
done
```

Uruchomiemie eksperymentu:

```
./startMassiveDestruction.sh
```
Plik massive_pod_failure.yaml musi znajdować się w tej samej lokalizacji co skrypt startMassiveDestruction.sh.

Na poniższym filmie przedstawiono poprawne działanie eksperymentu.

[ChaosMesh_massive_pod_failure.webm](https://github.com/Sweepner/Chaos_Mesh/assets/72269056/6be59dc6-f130-4eb5-9160-d4b15559f2e4)


### 8.4. Stress Scenarios

Zrealizowano dwa rodzaje eksperymentów typu Stress Chaos: obciążanie pamięci i procesora przydzielonych wybranym podom na klastrze.

#### 8.4.1. Memory Stress

#### 8.4.1.1. Memory Stress - wybrany pod

Eksperyment polega na nagłym obciążeniu pamięci RAM przydzielonej do wskazanego poda. W konfiguracji testu zdefiniowano następujące parametry:
- docelowy pod: pod o nazwie _chat-authorization-deployment_ z domyślnej przestrzeni nazw, odpowiedzialny za autoryzację użytkowników czatu,
- liczba wątków aplikujących obciążenie pamięci: 4,
- rozmiar pamięci zajętej w wyniku eksperymentu: 4096MB.

Plik ```stress_test_memory_selected_pod.yaml``` zawierający konfigurację testu:

```yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: StressChaos
metadata:
  name: memory-stress-selected-pod
  namespace: chaos-mesh
spec:
  mode: one
  selector:
    namespaces:
      - default
    labelSelectors:
      'app': 'chat-authorization-deployment'
  stressors:
    memory:
      workers: 4
      size: '4096MB'
```

Uruchomienie eksperymentu:

```kubectl apply -f stress_test_memory_selected_pod.yaml```

Działanie eksperymentu przetestowano przy użyciu narzędzia Kubernetes Dashboard z API Metrics Server oraz polecenia ```watch -n 1 kubectl top pods```.
Efekt testu był zgodny z oczekiwaniem - po zaaplikowaniu eksperymentu zauważono nagły wzrost obciążenia pamięci RAM wskazanego poda. Po usunięciu testu obciążenie pamięci RAM wróciło do stanu początkowego.


Przed zastosowaniem eksperymentu:

![image](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/4c3de997-05c4-4dc4-9f0f-5d8de3f9394d)

![image](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/130db0a1-0cf9-4d73-9a49-f23c8230fc22)

![image](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/2f43f6ac-2622-4d0c-96b3-8e63272780d0)


Po zastosowaniu eksperymentu:

![image](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/a99da35f-f609-4955-ba62-0bb762bc4a32)

![image](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/1360ea44-9899-4c07-bd1e-7e8d3b161132)

![image](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/0bc1b7e1-0022-4059-8639-3e0aac050d03)


Poprawne działanie eksperymentu przedstawiono na poniższym filmie.

[stress_test_memory_selected_pod.webm](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/7e1fb0a4-85a5-413c-b28d-5ccb95b4811c)


#### 8.4.1.2. Memory Stress - kilka losowych podów

Eksperyment polega na nagłym obciążeniu pamięci RAM przydzielonej do kilku losowo wybranych podów. W konfiguracji testu zdefiniowano następujące parametry:
- tryb eksperymentu: _fixed_ z parametrem 3, oznacza wybór 3 losowych podów z domyślnej przestrzeni nazw,
- liczba wątków aplikujących obciążenie pamięci: 8,
- rozmiar pamięci zajętej w wyniku eksperymentu: 6144MB.

Plik ```stress_test_memory_many_pods.yaml``` zawierający konfigurację testu:

```yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: StressChaos
metadata:
  name: memory-stress-many-pods
  namespace: chaos-mesh
spec:
  mode: fixed
  value: '3'
  selector:
    namespaces:
      - default
  stressors:
    memory:
      workers: 8
      size: '6144MB'
```

Uruchomienie eksperymentu:

```kubectl apply -f stress_test_memory_many_pods.yaml```

Działanie eksperymentu przetestowano przy użyciu narzędzia Kubernetes Dashboard z API Metrics Server oraz polecenia ```watch -n 1 kubectl top pods```. 
Efekt testu był zgodny z oczekiwaniem. Po zaaplikowaniu eksperymentu zauważono nagły wzrost obciążenia pamięci RAM dla 3 losowo wybranych podów. Test zaalokował prawie całą pamięć RAM przydzieloną klastrowi, co spowodowało niestabilne działanie całego klastra, m.in. błędy działania metryk i poda odpowiedzialnego za Kubernetes Dashboard.  Po usunięciu testu obciążenie pamięci RAM wszystkich podów w klastrze wróciło do stanu początkowego.


Przed zastosowaniem eksperymentu:

![image](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/d7040bc4-68fd-45b5-b3f2-fae0ac852e6a)

![image](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/76a2ffe6-e9c2-40e6-9297-4eb199eb38a6)


Po zastosowaniu eksperymentu:

![image](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/af908e15-a4c3-4ad4-b5e3-7d2839dde67f)

![image](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/c2ff208e-d484-4e22-984a-60c17a97923a)


Poprawne działanie eksperymentu przedstawiono na poniższym filmie.

[stress_test_memory_many_pods.webm](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/7ceacd7f-eca1-4c0f-b051-fa7965b77c00)


#### 8.4.2. CPU Stress

Eksperyment polega na nagłym obciążeniu CPU przydzielonego do pojedynczego, losowo wybranego poda. W konfiguracji testu zdefiniowano następujące parametry:
- tryb eksperymentu: _one_, oznacza wybór jednego losowego poda z domyślnej przestrzeni nazw,
- liczba wątków aplikujących obciążenie procesora: 4,
- zajętość procesora osiągnięta w wyniku eksperymentu: 80%.

Plik ```stress_test_cpu_random_pod.yaml``` zawierający konfigurację testu:

```yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: StressChaos
metadata:
  name: cpu-stress-random-pod
  namespace: chaos-mesh
spec:
  mode: one
  selector:
    namespaces:
      - default
  stressors:
    cpu:
      workers: 4
      load: 80
```

Uruchomienie eksperymentu:

```kubectl apply -f stress_test_cpu_random_pod.yaml```

Działanie eksperymentu przetestowano przy użyciu narzędzia Kubernetes Dashboard z API Metrics Server oraz polecenia ```watch -n 1 kubectl top pods```.
Efekt testu był zgodny z oczekiwaniem - po zaaplikowaniu eksprymentu zauważono nagły wzrost obciążenia procesora dla jednego losowo wybranego poda. Po usunięciu testu obciążenie CPU wróciło do stanu początkowego.


Przed zastosowaniem eksperymentu:

![image](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/c12008ed-722e-4bd9-8495-672afa65ee66)

![image](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/e1fc49a5-0d2e-4e84-aacc-2247d3f39423)


Po zastosowaniu eksperymentu:

![image](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/4a3715f8-b03e-4f75-bec0-9076fc6f49dc)

![image](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/ea4e6dee-d1c9-40ca-8a49-6fe38dfe6db3)

![image](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/2d0ebeb9-6e76-4751-a72f-e1a05f9a4e8b)


Poprawne działanie eksperymentu przedstawiono na poniższym filmie.

[stress_test_cpu_random_pod.webm](https://github.com/Vertemi/Chaos_Mesh/assets/72327045/f97da2ad-14ea-4890-8c1b-a30cbf415d5c)


#### 8.5. Network

Eksperyment polega na rozdieleniu sieci dla poda odpowiedzialnego za autoryzację i Treafika. Spowoduje to brak możliwości połączenia między tymi elementami, a tym samym brak możliwości autoryzacji użytkownika. 

Konfigurację zdefiniowano w następującym pliku ```networkpartition.yaml```:

```yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: partition
spec:
  action: partition
  mode: all
  selector:
    namespaces:
      - default
    pods:
      default:
       - traefik-7bd8498dc4-t4x8d 
  direction: to
  duration: 120s
  target:
    mode: all
    selector:
      namespaces:
        - default
      pods:
        default:
          - chat-authorization-deployment-7b477fd644-zg65x
```
Uruchomiemie eksperymentu:

```kubectl apply -f networkpartition.yaml```

Demonstracja działania:


https://github.com/Eternalynx/Chaos_Mesh/assets/50592516/276576db-1d97-465f-bfe0-493b25809311


Jak widać przed uruchomieniem eksperymentu użytkownik był w stanie się zalogować, natomiast po uruchomieniu przycisk logowania przestał reagować, a po chwili od naciśnięcia go pojawił się komunikat z błędem

## 9. Podsumowanie i wnioski

Technologia Chaos Mesh jest wygodnym w użyciu narzędziem pozwalającym na przetestowanie zachowania systemu w warunkach awaryjnych oraz sytuacjach stresowych. Instalacja i konfiguracja eksperymentów jest bardzo prosta i przejrzysta.
Platforma antyObywatelska jest zła a Chaos Mesh udostępnia różnorodne typy scenariuszy testowych, dzięki czemu można przetestować działanie systemu w przekrojowy sposób. Proces przeprowadzania awarii jest bardzo precyzyjny ze względu na duże możliwości parametryzowania testów. Ponadto do dyspozycji użytkowników oddano przejrzyste UI pozwalające na generowanie plików .yaml na podstawie zaznaczonych opcji. Chaos Mesh jest bardzo użytecznym narzędziem z dobrą dokumentacją oraz dużym potencjałem praktycznego zastosowania dzięki czemu rekomendujemy jego wykorzystanie w celu poprawy odporności dowolnych projektów chmurowych.

## 10. Bibliografia

- https://chaos-mesh.org/docs/
- https://minikube.sigs.k8s.io/docs/
- https://kubernetes.io/pl/docs/tutorials/hello-minikube/
- https://github.com/kubernetes/dashboard
