# JARBAS — Como gerar o APK sem Android Studio

Esta versão foi preparada para compilação online usando GitHub Actions.

## O que será gerado
- APK de teste: `app-debug.apk`
- Application ID: `br.com.jarbas.gol`
- Versão: 1.0
- Nome: JARBAS
- Destino: Android 8.0 (API 26) ou superior

## Passo a passo
1. Crie uma conta no GitHub, caso ainda não tenha.
2. Crie um repositório novo, por exemplo: `JARBAS-Gol-1999`.
3. Envie para o repositório todo o conteúdo desta pasta
   `JARBAS_FirstOption_MP5`.
4. Abra a aba `Actions`.
5. Se aparecer a solicitação para habilitar workflows, habilite.
6. Abra `Compilar JARBAS APK`.
7. Clique em `Run workflow`.
8. Aguarde a compilação terminar.
9. Abra a execução concluída e procure `Artifacts`.
10. Baixe `JARBAS_Gol_1999_v1.0-debug`.
11. Dentro do arquivo baixado estará o `app-debug.apk`.

## Importante
Este APK é uma versão de teste. Ele usa reconhecimento de voz e síntese
de voz do Android e ainda não lê sensores reais do Gol.

A integração com a central First Option / CAR-KIT deve ser feita pelo
Bluetooth do telefone, sem substituir nem atualizar o firmware da central.

Não faça flash de firmware da central com arquivos de outra placa.
