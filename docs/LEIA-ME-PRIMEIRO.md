# JARB — pacote específico para a First Option MP5

## O que foi identificado nas fotos
- Marca: First Option
- Plataforma aparente: MP5 / Car-Kit
- MCU informado na tela: V66.22
- Software informado: V7.52BT-FK-TP
- Bluetooth: CAR-KIT

## O que este pacote faz
Este pacote cria um aplicativo Android chamado JARB para usar o celular/Android como cérebro de voz, enquanto a First Option permanece como central de áudio Bluetooth.

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
celular/Android com JARB -> Bluetooth -> CAR-KIT da First Option -> alto-falantes do Gol.

Se a sua central possuir espelhamento compatível, a tela do telefone poderá eventualmente ser exibida nela; isso precisa ser testado no aparelho específico.

## Como instalar
Este diretório é um projeto Android Studio. Abra a pasta no Android Studio, aguarde o Gradle sincronizar e gere o APK de debug.

Permissões:
- microfone;
- Bluetooth (quando exigido pela versão do Android).

Depois:
1. Emparelhe o telefone com CAR-KIT.
2. Selecione a First Option como saída de áudio Bluetooth.
3. Abra JARB.
4. Toque em OUVIR JARB.
5. Fale normalmente.

Comandos de mídia disponíveis:
- depois de tocar em OUVIR JARB, diga diretamente o comando; "JARB" também
	pode ser usado, mas não é obrigatório nesse modo;
- "JARB", "JARBS" e "JARBAS" são aceitos como nomes de ativação;
- "aumentar o volume", "diminuir o volume" ou "silenciar";
- "tocar música", "pausar a música" ou "continuar a música";
- "próxima música" ou "música anterior";
- "abrir Bluetooth" ou "conectar na central" para abrir as configurações e
	conectar manualmente a CAR-KIT.

O botão OUVIR JARB ativa o modo mãos-livres experimental. Diga "JARB" e
o comando na mesma frase; frases sem a palavra de ativação são ignoradas.
PARAR encerra a escuta. Alguns telefones ainda podem emitir um sinal curto ao
iniciar uma captura; esse som é gerado pelo Android e a central pode pausar o
áudio enquanto o reconhecimento usa o microfone.

Para silenciar por voz, diga "JARB, silêncio". O app cancela o microfone e
para de disputar o áudio com a música. Para reativar, toque novamente em OUVIR
JARB; um microfone desligado não consegue escutar a palavra de ativação. O
botão PARAR também desliga completamente o microfone.

Memória local:
- "JARB aprenda que baixa o som significa diminuir volume" cria um alias
	salvo no telefone;
- "JARB me chame de Carlos" salva como você prefere ser chamado;
- os comandos usados são contados localmente para orientar melhorias futuras;
- nenhum áudio ou histórico é enviado automaticamente para a internet.

Internet e conversa:
- "JARB pesquise novidades sobre o Gol 1999" abre uma busca no navegador;
- "JARB o que é injeção eletrônica?" e "JARB como funciona um motor?" também
	abrem uma pesquisa automaticamente;
- os comandos aceitam variações como "aumenta o som", "abaixa o volume",
	"pula a música", "dá play" e "JARB, por favor, pausa a música";
- "JARB bom dia", "JARB ajuda" e "JARB o que você sabe fazer" têm respostas
	próprias;
- o app não é um modelo de IA online: para respostas generativas seria
	necessário um servidor seguro com uma chave protegida fora do APK.

O acesso à internet não é ilimitado: depende da conexão, do navegador e dos
serviços externos. O JARB não envia áudio ou aprende sozinho com a internet;
ele guarda aliases e preferências localmente e abre buscas quando solicitado.

Durante cada captura, o app reduz manualmente dois níveis do volume de mídia e
restaura o valor original ao terminar. Ele não solicita foco de áudio ao
Android, reduzindo a chance de a central pausar a música. Algumas centrais ou
serviços Android ainda podem impor seu próprio comportamento.

A permissão de internet está disponível para uma futura integração com um
servidor de IA. Para aprendizado online real será necessário configurar um
servidor e uma chave de API fora do APK; chaves não devem ser colocadas no
código do aplicativo.

## Próxima etapa
Para ligar sensores reais do Gol, use um microcontrolador separado e protegido. O aplicativo não deve comandar direção, freios ou acelerador.
