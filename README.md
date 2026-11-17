# Módulo de Lineup de Navios

Projeto Android em Kotlin, Jetpack Compose e Material 3. Uma instalação nova abre o Lineup com o modo local ativado. Os dados acompanham o aplicativo e podem ser usados sem servidor.

## Executar pelo Android Studio

1. Extraia o ZIP e abra a pasta `LineupNavios` no Android Studio.
2. Configure o Gradle para usar o JDK 17 e instale o Android SDK 34. Sincronize o projeto. A primeira sincronização pode baixar dependências.
3. Selecione um emulador ou dispositivo com Android 9 (API 28) ou superior e clique em **Run**.

## Demonstrar o módulo

- Abra o grupo **PORTOS** no menu lateral para acessar Lineup, Berços, Cargas, Navios, Tábuas de Marés e Atracações.
- Novembro começa com 60 berços, 152 embarcações no catálogo e 153 atracações. Junho e setembro têm tábuas próprias e começam sem atracações. Ao retornar a um mês, sua programação reaparece. O gráfico, os indicadores e a lista usam o mês ativo.
- Toque no casco para consultar ou editar, arraste pelo centro para mover a janela ou use as extremidades para redimensionar. As janelas são validadas contra sobreposição e maré baixa. Atracações concluídas permanecem disponíveis para consulta.
- O cadastro calcula o término por `ceil(tonelagem / prancha)`. Na edição, é possível ajustar o dia final ou recalculá-lo pela prancha. O catálogo inclui 15 cargas. As taxas em t/dia de contêineres, veículos e cruzeiros são parâmetros simulados para a demonstração.
- As tábuas usam CSVs com 48 leituras por dia. O gráfico usa as alturas medidas, e a tela de tábuas mostra a mínima, a máxima e o status de cada dia.

As alterações locais são guardadas no dispositivo. Para voltar aos dados iniciais, limpe os dados do aplicativo nas configurações do Android.

## Testar e compilar

Com JDK 17 e Android SDK 34 configurados, execute na pasta do projeto:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

O APK de debug é gerado em `app/build/outputs/apk/debug/app-debug.apk`.
