# Launcher TV (Android TV)

App Android que abre o seu launcher em tela cheia na TV, com:
- ícones reais dos apps instalados na TV (Netflix, YouTube, Spotify...) e abertura de verdade ao apertar OK;
- botão de Bluetooth e engrenagem abrindo as telas do sistema;
- bandeja da F1 que se atualiza sozinha pela internet.

## 1. Gerar o APK (no computador)
1. Instale o Android Studio (gratuito) e abra esta pasta (`launcher-tv-android`).
2. Aguarde o "Gradle Sync" terminar (precisa de internet na primeira vez).
3. Menu **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. O arquivo fica em `app/build/outputs/apk/debug/app-debug.apk`.

## 2. Instalar na Philco
Ative as opções de desenvolvedor: em **Configurações > Sobre**, toque 7 vezes em **Número da versão**.
Depois use um destes caminhos:

**A) Por ADB (rede Wi-Fi)**
1. Em **Opções do desenvolvedor**, ligue **Depuração USB / ADB**.
2. No computador (mesma rede): `adb connect IP_DA_TV` e depois `adb install app-debug.apk`.

**B) Por pendrive**
1. Copie o `app-debug.apk` para o pendrive e instale com um gerenciador de arquivos da TV
   (permita "fontes desconhecidas" para esse app).

O app aparece na lista de apps como **Launcher TV**.

## 3. Usar como tela inicial (opcional, depende da TV)
Em Android TV puro, o comando abaixo costuma funcionar:
`adb shell cmd package set-home-activity br.com.launchertv/.MainActivity`

Algumas TVs e o Google TV bloqueiam a troca da tela inicial. Nesse caso o Launcher TV
continua funcionando normalmente como um app aberto pela lista. Para desfazer:
`adb uninstall br.com.launchertv`

## Observações
- Na primeira abertura a TV precisa de internet (fontes e calendário da F1).
- Se o efeito de vidro embaçado ficar pesado, reduza `blur(22px)` no `launcher.html`
  (pasta `app/src/main/assets`).
- Pacotes dos apps ficam no objeto `PK` do `launcher.html`. Se algum app não aparecer com ícone,
  ele é procurado também pelo nome.
