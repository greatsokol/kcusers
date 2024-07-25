# KCUsers (Keycloak users)
Служба, которая периодически опрашивает API-интерфейс keycloak для получения времени последней аутентификации пользователей.

Пользователи, которые не аутентифицировались определенное в настройках количество дней, автоматически блокируются.

Служба предоставляет web-интерфейс администратора, реализованный как сервлет, а также предоставляет сервис REST API.

## Перечень настроек
Настройки службы и сервлета находятся в файле `application.yaml` в разделах `db`, `server`, `service` и `front`.

### Настройки подключения к БД
Находятся в файле `application.yaml`, в разделе `db`.
* `url` - адрес сервера postgresql
* `username` - имя пользователя БД
* `password` - пароль пользователя БД. Пароль **загружается из переменной среды окружения** `pgpwd`.
_Переменная окружения `pgpwd` заполняется в файле запуска `bin\start` (или `bin\start.bat`).
При запуске из IDE (т.е. при отсутствии переменной окружения `pgpwd`) используется значение по умолчанию `admin` из файла `application.yaml`._

### Настройки запуска web-сервера
Находятся в файле `application.yaml`, в разделе `server`.
* `port` - порт, по которому будет доступен сервлет и сервис API
* `ssl.certificate` - путь к файлу сертификата
* `ssl.certificate-private-key` - путь к файлу ключа

### Настройки службы
Находятся в файле `application.yaml`, в разделе `service`.
* `cron` - крон, определяющий периодичность сканирования. Установлено значение `00 * * * * *`, означающее запуск в начале каждой минуты.
* `keycloakclient.realms` - названия рилмов keycloak, аутентфикацию пользователей которых требуется проверять.
* `keycloakclient.url` - адрес keycloak.
* `keycloakclient.client` - название клиента, настроенного в keycloak для службы KCUsers.
* `keycloakclient.admin.admin` - имя служебного пользователя, от имени которого действует служба KCUsers. Должен обладать ролью `manage-users`.
* `keycloakclient.admin.password` - пароль служебного пользователя. Пароль **загружается из переменной среды окружения** `kсpwd`.
_Переменная окружения `kсpwd` заполняется в файле запуска `bin\start` (или `bin\start.bat`).
При запуске из IDE (т.е. при отсутствии переменной окружения `kсpwd`) используется значение по умолчанию `admin` из файла `application.yaml`._
* `keycloakclient.admin.realm` - рилм, которому принадлежит служебный пользователь.
* `keycloakclient.inactivity.protectedusers` - список пользователей, которые не будут затрагиваться службой KCUsers.
* `keycloakclient.inactivity.days` - количество дней, в течение которох пользователь не должен аутентифицироваться, чтобы считаться неактивным 
* `keycloakclient.inactivity.immunityperiodminutes` - количество минут, в течение которых пользователь должен аутентифицироваться после его разблокирования администратором.
Если пользователь не аутентифицируется за это время, то он будет снова заблокирован службой KCUsers.
* `keycloakclient.inactivity.dryrun` - включение отладочного режима.

### Настройки интерфейса администратора
Находятся в файле `application.yaml`, в разделе `front`.
* `issuer-uri` - URI издателя токена, **должен совпадать со сначением поля ISS токена**
* `client-id` - название клиента, настроенного в keycloak для фронта KCUsers. 
* `client-secret` - секрет клиента (может не потребоваться, в зависимости от настроек аутентфифкации клиента в keycloak). Секрет **загружается из переменной среды окружения** `clsec`.
_Переменная окружения `clsec` заполняется в файле запуска `bin\start` (или `bin\start.bat`).
При запуске из IDE (т.е. при отсутствии переменной окружения `clsec`) используется значение по умолчанию из файла `application.yaml`._
* `adminroles` - список ролей через запятую, которые имеют права на просмотр и на изменение состояния пользователей (роль из списка должна быть присвоена пользователю в keycloak).
* `userroles` - список ролей через запятую, которые имеют права только на просмотр (роль из списка должна быть присвоена пользователю в keycloak).

## Настройка keycloak
_(URL-адреса и названия пользователей приведены для примера, замените на актуальные)_
1. Запустите keycloak по адресу `https://keycloak.local`. Укажите адрес keycloak в настройке `service.keycloakclient.url` файла `application.yaml`.
2. В keycloak, в разделе `Client scopes` проверьте scope `roles`:
   * На закладке `Mappers` добавьте mapper `realm roles`, если отсутствует.
   * В маппере `realm roles` должен быть включен переключатель `Add to ID token`.
3. В keycloak создайте пользователя `admin` с правами `manage-users` для всех рилмов, перечисленных в настройке `service.keycloakclient.realms` файла `application.yaml`.
Укажите его название в настройке `service.keycloakclient.admin.admin` и его пароль в переменной окружения `kсpwd`.
4. В keycloak создайте клиент для службы, укажите его название в настройке `service.keycloakclient.client` файла `application.yaml`.
5. В keycloak создайте клиент `servlet_client` для сервлета интерфейса администрирования:
   * укажите его название в настройке `front.client_id` файла `application.yaml`,
   * в настройках клиента `servlet_client` на закладке `Client scopes` добавьте scope `roles` (Default),
   * в настройках клиента `servlet_client` заполнить адреса сервера интерфейса администрирования:
     * Home URL: `https://kcusers.local`
     * Valid redirect URIs: `https://kcusers.local/*`
     * Web origins: `https://kcusers.local`
6. Сертификат УЦ, которым выпущен сертификат сайта `https://keycloak.local`, нужно, с помощью keytool, добавить в `cacerts` JVM, в которой будет запускаться служба KCUsers. Пример команды:


    keytool -delete -alias "keycloack" -keystore "C:\Program Files\BellSoft\LibericaJDK-17\lib\security\cacerts" -storepass changeit
    keytool -import -alias "keycloack" -keystore "C:\Program Files\BellSoft\LibericaJDK-17\lib\security\cacerts" -storepass changeit -file ca.crt`

## Настройка postgresql
1. Выполните sql-запросы из файла `etc\sql.txt` для создания схемы kcusers и таблиц.
2. Укажите адрес сервера postgresql в настройке `db.url`.
3. Укажите имя пользователя в настройке `db.username`.
4. Укажите пароль пользователя в переменной окружения `pgpwd`.

## Сборка приложения
Выполните команду 

     mvn "-DskipTests=true" -U clean install 

Собранное приложение будет расположено в файле `путь\до\target\kcusers-0.0.1-SNAPSHOT.zip`

## Запуск службы в операционной системе Linux
Выполните инструкции из файла `etc\linux_install`








