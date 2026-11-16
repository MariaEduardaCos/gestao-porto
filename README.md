# Lineup de Navios — Primeira Entrega

Aplicativo Android em Kotlin e Jetpack Compose para o Gêmeo Digital do Porto de Santos. Uma instalação nova abre diretamente no Lineup, com **Modo Local** ativo. Os dados da demonstração vêm com o aplicativo e podem ser usados sem servidor, banco de dados ou internet.

## Como abrir

- **APK** — instale `apk/LineupApp-debug.apk` em um dispositivo ou emulador Android 8.0 (API 26) ou superior. Com um dispositivo conectado, também é possível usar `adb install -r apk/LineupApp-debug.apk`.
- **Projeto** — extraia o ZIP e abra a pasta `LineupApp` no Android Studio. Configure o Gradle para usar o JDK 17, instale o Android SDK 34 e sincronize o projeto. Selecione um dispositivo e clique em **Run**. A primeira sincronização pode baixar dependências.

## O que demonstrar

- **RF003** — 60 berços em 12 páginas de 5, 153 atracações iniciais, busca por berço, navio ou carga, filtros pelas 11 categorias, indicadores e seletor de tábua de marés. O gráfico apresenta 30 dias, curva de maré e faixas sombreadas nos dias críticos. Use **+ Novo Berço** para cadastrar um berço local.
- **RF004** — use **Atracar Navio** para criar uma programação ou toque no casco para editar ou excluir. Deslize em uma área livre para navegar no calendário. Arraste o centro do casco para deslocar a janela ou suas extremidades para ajustar o início e o fim.
- **RN001, RN002 e RN005** — a edição e os gestos validam colisões e dias de maré baixa. As cores identificam os status de operação ou prioridade, programação e confirmação.

As alterações de demonstração ficam em memória durante a execução. Ao encerrar o processo do aplicativo, a próxima abertura carrega novamente os dados iniciais.

## Configuração do backend

O menu **Configurações** permite informar a URL do backend fornecido pelo professor. Para um servidor na máquina que roda o emulador, use `http://10.0.2.2:8080`. Em um dispositivo físico, aponte para o endereço da máquina na mesma rede. Desative **Modo Dados Mocados** e use **Testar Conexão REST** com o servidor em execução.

Esta etapa cobre o Lineup com dados locais. O enunciado geral também prevê os demais módulos e a integração completa com o servidor para a entrega final.

## Testes e compilação

Na pasta `LineupApp`, com JDK 17 e Android SDK 34 configurados:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```
