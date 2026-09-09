# JARBAS — pacote específico para a First Option MP5

## O que foi identificado nas fotos
- Marca: First Option
- Plataforma aparente: MP5 / Car-Kit
- MCU informado na tela: V66.22
- Software informado: V7.52BT-FK-TP
- Bluetooth: CAR-KIT

## O que este pacote faz
Este pacote cria um aplicativo Android chamado JARBAS para usar o celular/Android como cérebro de voz, enquanto a First Option permanece como central de áudio Bluetooth.

O aplicativo:
- fala em português brasileiro;
- reconhece comandos por voz;
- responde por voz;
- mantém conversa contínua durante o modo de conversa;
- identifica o proprietário como Carlos Diego da Silva;
- possui tela horizontal pensada para uso automotivo;
- está preparado para receber dados de um futuro controlador veicular.

## Limitação importante
A foto não demonstra que a First Option possui Android aberto ou capacidade de instalar APKs. Por isso NÃO incluí firmware da central e NÃO recomendo gravar firmware de terceiros nela.

O uso previsto é:
celular/Android com JARBAS -> Bluetooth -> CAR-KIT da First Option -> alto-falantes do Gol.

Se a sua central possuir espelhamento compatível, a tela do telefone poderá eventualmente ser exibida nela; isso precisa ser testado no aparelho específico.

## Como instalar
Este diretório é um projeto Android Studio. Abra a pasta no Android Studio, aguarde o Gradle sincronizar e gere o APK de debug.

Permissões:
- microfone;
- Bluetooth (quando exigido pela versão do Android).

Depois:
1. Emparelhe o telefone com CAR-KIT.
2. Selecione a First Option como saída de áudio Bluetooth.
3. Abra JARBAS.
4. Toque em OUVIR JARBAS.
5. Fale normalmente.

Comandos de mídia disponíveis:
- "aumentar o volume", "diminuir o volume" ou "silenciar";
- "tocar música", "pausar a música" ou "continuar a música";
- "próxima música" ou "música anterior";
- "abrir Bluetooth" ou "conectar na central" para abrir as configurações e
	conectar manualmente a CAR-KIT.

## Próxima etapa
Para ligar sensores reais do Gol, use um microcontrolador separado e protegido. O aplicativo não deve comandar direção, freios ou acelerador.
