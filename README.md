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

### 2.4. Architektura Chaos Mesh

Chaos Mesh jest oparty na Kubernetes CRD (Custom Resource Definition). Do zarządzania różnymi eksperymentami definiuje wiele typów CRD opartych na różnych rodzajach awarii i implementuje osobne kontrolery dla różnych obiektów CRD. Do głównych komponentów Chaos Mesh należą:

- **Chaos Dashboard**: komponent wizualizacji Chaos Mesh, oferuje zestaw przyjaznych interfejsów internetowych, za pomocą których użytkownicy mogą manipulować i obserwować eksperymenty Chaos. Zapewnia również mechanizm zarządzania uprawnieniami RBAC.
- **Chaos Controller Manager**: główny komponent logiczny Chaos Mesh odpowiedzialny głównie za harmonogramowanie i zarządzanie eksperymentami Chaos. Zawiera kilka kontrolerów CRD takich jak Kontroler Przepływu Pracy, Kontroler Harmonogramu i Kontrolery różnych rodzajów awarii.
- **Chaos Daemon**: główny komponent wykonawczy. Działa w trybie DaemonSet i ma domyślnie uprawnienia Privileged (które można wyłączyć). Ten komponent głównie ingeruje w określone urządzenia sieciowe, systemy plików, jądra, łącząc się z przestrzeniami nazw docelowego Pod.

### 2.5. Stos technologiczny

Chaos Mesh, Docker, Kubernetes (Minikube), JVM (Java, Kotlin)

## 3. Koncepcja studium przypadku

Aplikacja, która została poddana testom to szyfrowany komunikator z audiorozmową oparta o architekturę mikroserwisów.

### 3.1. Demo aplikacji

https://github.com/Sweepner/Chaos_Mesh/assets/72269056/127a4655-bb66-4a76-919e-9c0688dbd4e5

## 4. Architektura rozwiązania

## 5. Konfiguracja środowiska

Środowisko utworzono na własnym sprzęcie. Zakupiono serwer posiadający 32 GB ramu oraz 1 TB SSD. Za pomocą frps - https://github.com/fatedier/frp wystawiono serwer do internetu. W celu posiadania adresu IP za NATem skorzystano z zawsze darmowej maszyny wirtualnej oracle (dalej nazwanej tutaj proxy) z 1 GB ramu oraz 1 corem. Maszyna ta służy jako proxy do prywatnej maszyny. Na proxy uruchomiono frps w następującej konfiguracji (frps.toml - plik konfiguracyjny):

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

Aby frps oraz frpc komunikowały się otworzono porty 7000 oraz 6000 na proxy używająć komend:
```
sudo firewall-cmd --permanent --zone=public --add-port=7000/udp
sudo firewall-cmd --permanent --zone=public --add-port=7000/tcp
sudo firewall-cmd --permanent --zone=public --add-port=6000/udp
sudo firewall-cmd --permanent --zone=public --add-port=6000/tcp
sudo firewall-cmd --reload
```
Port 7000 służy do komunikacji pomiędzy klientem a serwerem frp. Port 6000 służy do prziekierowania na port 22 na prywatnej maszynie.


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
  
## 6. Metoda instalacji

## 7. Sposób odtworzenia krok po kroku

## 8. Demonstracyjny sposób wdrożenia

## 9. Podsumowanie i wnioski

## 10. Bibliografia

- https://chaos-mesh.org/docs/
