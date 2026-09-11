package br.com.jarbas.gol;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.media.AudioManager;
import android.speech.tts.Voice;
import android.view.KeyEvent;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import java.io.File;
import java.io.FileWriter;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private SpeechRecognizer recognizer;
    private Intent speechIntent;
    private TextToSpeech tts;
    private TextView status;
    private AudioManager audioManager;
    private int previousMusicVolume = -1;
    private boolean conversation = false;
    private boolean silentMode = false;
    private boolean handsFreeMode = false;
    private SharedPreferences memory;
    private File learningDirectory;
    private final BroadcastReceiver overlayListenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (OverlayService.ACTION_LISTEN.equals(intent.getAction())) {
                silentMode = false;
                conversation = true;
                status.setText("Fale um comando começando com JARB.");
                listenAgain();
            }
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        Button listen = findViewById(R.id.listen);
        Button stop = findViewById(R.id.stop);
        Button menu = findViewById(R.id.menu);

        tts = new TextToSpeech(this, this);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        memory = getSharedPreferences("jarb_memory", MODE_PRIVATE);
        learningDirectory = new File(getFilesDir(), "jarb_data");
        if (!learningDirectory.exists()) learningDirectory.mkdirs();

        menu.setOnClickListener(v -> showMainMenu(v));

        IntentFilter overlayFilter = new IntentFilter(OverlayService.ACTION_LISTEN);
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(overlayListenReceiver, overlayFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(overlayListenReceiver, overlayFilter);
        }

        startOverlayIfAllowed();

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 10);
        }

        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        speechIntent.putExtra("android.speech.extra.BEEP_SOUND", false);

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle p) { status.setText("JARB ouvindo."); }
            public void onBeginningOfSpeech() { status.setText("..."); }
            public void onRmsChanged(float r) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() { status.setText("Processando..."); }
            public void onError(int e) {
                restoreMusicVolume();
                if (handsFreeMode && !silentMode) {
                    conversation = true;
                    status.setText("Não ouvi. Tentando novamente...");
                    status.postDelayed(() -> {
                        if (handsFreeMode && !silentMode) listenAgain();
                    }, 700);
                } else {
                    conversation = false;
                    status.setText("Toque em OUVIR JARB para falar.");
                }
            }
            public void onResults(Bundle r) {
                restoreMusicVolume();
                conversation = false;
                ArrayList<String> a = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String heard = (a != null && !a.isEmpty()) ? a.get(0) : "";
                respond(heard);
            }
            public void onPartialResults(Bundle r) {}
            public void onEvent(int t, Bundle p) {}
        });

        listen.setOnClickListener(v -> {
            silentMode = false;
            conversation = true;
            status.setText("Fale um comando começando com JARB.");
            listenAgain();
        });

        stop.setOnClickListener(v -> {
            conversation = false;
            silentMode = false;
            recognizer.cancel();
            restoreMusicVolume();
            status.setText("JARB em espera.");
        });
    }

    private void listenAgain() {
        if (!conversation) return;
        lowerMusicVolume();
        recognizer.startListening(speechIntent);
    }

    private void startOverlayIfAllowed() {
        if (!Settings.canDrawOverlays(this)) {
            status.setText("Para usar o botão flutuante, permita sobreposição nas configurações.");
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            return;
        }

        Intent overlayIntent = new Intent(this, OverlayService.class);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            startForegroundService(overlayIntent);
        } else {
            startService(overlayIntent);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (recognizer != null && Settings.canDrawOverlays(this)) {
            startOverlayIfAllowed();
        }
    }

    private void lowerMusicVolume() {
        if (audioManager == null || previousMusicVolume >= 0) return;
        previousMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int reducedVolume = Math.max(0, previousMusicVolume - 2);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, reducedVolume, 0);
    }

    private void restoreMusicVolume() {
        if (audioManager == null || previousMusicVolume < 0) return;
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousMusicVolume, 0);
        previousMusicVolume = -1;
    }

    private void respond(String text) {
        logInteraction(text);
        String q = normalize(text);
        q = removeOptionalAssistantPrefix(q);

        if (startsWithAssistantName(q)) {
            q = q.replaceFirst("^jarb(?:as|s)?\\s*", "").trim();
        }

        if (hasAny(q, "silencio", "fique em silencio", "fica em silencio", "modo silencio", "pare de ouvir",
            "parar de ouvir", "desligue o microfone", "desativar escuta", "microfone desligado")) {
            silentMode = true;
            handsFreeMode = false;
            conversation = false;
            recognizer.cancel();
            status.setText("JARB em silêncio. Toque em OUVIR para reativar.");
            return;
        }

        if (hasAny(q, "desligar microfone livre", "desativar maos livres", "desligar maos livres", "microfone fechado", "fechar microfone")) {
            handsFreeMode = false;
            silentMode = false;
            status.setText("Microfone livre desligado.");
            answerAfterCommand("Microfone livre desligado.");
            return;
        }

        if (hasAny(q, "maos livres", "mão livre", "mao livre", "microfone livre", "ligar microfone livre", "mãos livres", "microfone aberto", "abrir microfone")) {
            handsFreeMode = true;
            silentMode = false;
            status.setText("Microfone livre ativado.");
            answerAfterCommand("Microfone livre ativado.");
            return;
        }

        if (q.isEmpty()) {
            silentMode = false;
            conversation = true;
            status.setText("JARB ativo. Diga o comando.");
            if (conversation) status.postDelayed(() -> listenAgain(), 300);
            return;
        }

        if (silentMode) {
            status.setText("JARB em silêncio. Toque em OUVIR para reativar.");
            return;
        }

        if ((q.startsWith("aprenda que ") || q.startsWith("aprenda ") || q.startsWith("me ensine que ")) && q.contains(" significa ")) {
            String lessonText = q.replaceFirst("^(aprenda que|aprenda|me ensine que)\\s+", "");
            String[] lesson = lessonText.split(" significa ", 2);
            if (lesson.length == 2 && !lesson[0].trim().isEmpty() && !lesson[1].trim().isEmpty()) {
                memory.edit().putString("alias_" + lesson[0].trim(), lesson[1].trim()).apply();
                answerAfterCommand("JARB aprendeu esse comando.");
                return;
            }
        }

        q = applyLearnedAliases(q);

        String answer;

        if (q.startsWith("me chame de ") || q.startsWith("me chama de ")) {
            String preferredName = q.substring(q.indexOf(" de ") + 4).trim();
            memory.edit().putString("preferred_name", preferredName).apply();
            answer = "Combinado. Vou chamar você de " + preferredName + ".";
        } else if (hasAny(q, "quem e voce", "quem e o jarb", "seu nome", "como voce se chama")) {
            answer = "Eu sou JARB, o assistente do seu Gol 1999.";
        } else if (hasAny(q, "como voce esta", "como vc esta", "tudo bem", "voce esta bem")) {
            answer = "Estou bem e pronto para ajudar. O que você precisa?";
        } else if (hasAny(q, "bom dia", "boa tarde", "boa noite", "ola", "oi")) {
            answer = greetingForTime();
        } else if (hasAny(q, "que horas sao", "que horas são", "qual a hora", "horario atual", "horas agora", "hora atual")) {
            answer = "Agora são " + formatCurrentTime() + ".";
        } else if (hasAny(q, "como esta o clima", "como está o clima", "clima hoje", "tempo hoje", "vai chover", "esta chovendo", "chuva", "temperatura hoje", "previsao do tempo", "verificar o clima", "verificar clima")) {
            openWeatherSearch(q);
            answer = "Vou verificar a previsão do tempo para você.";
        } else if (isNavigationCommand(q)) {
            String destination = extractDestination(q);
            if (destination.isEmpty()) {
                answer = "Qual endereço você quer que eu abra no Waze?";
            } else {
                openWaze(destination);
                answer = "Abrindo o Waze para " + destination + ".";
            }
        } else if (hasAny(q, "o que voce sabe fazer", "ajuda", "comandos", "o que voce consegue", "me ajude")) {
            answer = "Posso conversar, pesquisar na internet, controlar volume e música, abrir o Bluetooth, verificar o clima, informar a hora e guardar preferências que você me ensinar.";
        } else if (hasAny(q, "aumentar volume", "aumentar o volume", "aumenta volume", "aumenta o volume", "aumente volume", "aumente o volume", "subir volume", "subir o volume", "sobe volume", "sobe o volume", "mais alto", "aumentar som", "aumentar o som", "aumenta o som", "aumente o som", "volume pra cima")) {
            changeVolume(AudioManager.ADJUST_RAISE);
            answer = "Aumentando o volume.";
        } else if (hasAny(q, "diminuir volume", "diminuir o volume", "diminui volume", "diminui o volume", "diminua volume", "diminua o volume", "abaixar volume", "abaixar o volume", "abaixa volume", "abaixa o volume", "baixar volume", "baixar o volume", "baixa volume", "baixa o volume", "mais baixo", "diminuir som", "diminuir o som", "abaixar som", "abaixar o som", "abaixa o som", "volume pra baixo")) {
            changeVolume(AudioManager.ADJUST_LOWER);
            answer = "Diminuindo o volume.";
        } else if (hasAny(q, "volume maximo", "som maximo", "no maximo")) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            answer = "Volume máximo selecionado.";
        } else if (hasAny(q, "volume minimo", "som minimo", "silenciar", "tirar o som", "sem som", "mudo")) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            answer = "Som silenciado.";
        } else if (hasAny(q, "coloque a musica", "toque a musica", "tocar a musica", "toca a musica", "coloca a musica", "procure a musica", "pesquise a musica", "abrir musica", "musica do spotify", "música do spotify", "coloque a música", "toque a música")) {
            String track = extractMusicQuery(q);
            if (!track.isEmpty()) {
                openSpotifySearch(track);
                answer = "Procurando e abrindo a música " + track + " no Spotify.";
            } else {
                openSpotify();
                answer = "Qual música você quer que eu procure no Spotify?";
            }
        } else if (hasAny(q, "abrir spotify", "abre o spotify", "abrir o spotify", "spotify", "tocar spotify")) {
            openSpotify();
            answer = "Abrindo o Spotify.";
        } else if (hasAny(q, "pausar musica", "pausar a musica", "pausa musica", "pausa a musica", "pause a musica", "coloca em pausa", "pare a musica", "parar musica", "pause")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE);
            answer = "Música pausada.";
        } else if (hasAny(q, "continuar musica", "continuar a musica", "continua musica", "continua a musica", "retomar musica", "retome musica", "voltar a tocar", "da play", "continuando a musica")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);
            answer = "Continuando a música, Carlos.";
        } else if (hasAny(q, "proxima musica", "proxima a musica", "proxima faixa", "proxima", "seguinte", "trocar musica", "mudar musica", "troca musica", "muda musica", "pular musica", "pula musica")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
            answer = "Avançando para a próxima música.";
        } else if (hasAny(q, "musica anterior", "faixa anterior", "voltar musica", "voltar faixa", "musica passada", "anterior")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            answer = "Voltando para a música anterior.";
        } else if (hasAny(q, "tocar musica", "tocar a musica", "tocar", "iniciar musica", "inicia musica", "dar play", "da play", "play", "reproduzir musica")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);
            answer = "Iniciando a música.";
        } else if (hasAny(q, "pesquise", "pesquisar", "procure na internet", "buscar na internet", "veja na internet", "noticias sobre")) {
            String search = q.replaceFirst("^(pesquise|pesquisar|procure(?: na internet)?|buscar(?: na internet)?|veja na internet|noticias sobre|o que e|quem foi|como funciona|qual e)\\s*", "").trim();
            if (search.isEmpty()) search = q;
            openWebSearch(search);
            answer = "Abrindo uma pesquisa na internet.";
        } else if (hasAny(q, "como esta", "estado do carro", "situacao do carro", "status do carro", "diagnostico do carro", "diagnóstico do carro", "carro")) {
            answer = "O módulo de diagnóstico está em demonstração, mas já está preparado para ler sensores e status do Gol 1999.";
        } else if (hasAny(q, "travar portas", "trancar portas", "destravar portas", "abrir porta malas", "abrir porta-malas", "fechar portas", "estado das portas")) {
            answer = "O comando das portas está em demonstração. Para funcionar no carro, será necessário um módulo eletrônico compatível.";
        } else if (hasAny(q, "ligar ar condicionado", "ligar ar", "desligar ar condicionado", "desligar ar")) {
            answer = "O ar-condicionado só pode ser acionado com uma integração eletrônica real do veículo. Neste momento está em demonstração.";
        } else if (hasAny(q, "freio de mao", "freio de mão", "cinto de seguranca", "cinto de segurança", "oleo do motor", "oleo", "óleo")) {
            answer = "Esse item pode ser monitorado com sensores do veículo. A integração do Gol ainda está em modo de demonstração.";
        } else if (hasAny(q, "porta", "janela", "trava", "farol", "farois", "motor", "combustivel", "combustível", "odometro", "odômetro")) {
            answer = "Esses itens do veículo podem ser monitorados com integração real do carro. Hoje a leitura está em modo de demonstração.";
        } else if (hasAny(q, "temperatura", "temperatura do carro", "quente", "frio")) {
            answer = "A temperatura do motor e do ambiente podem ser monitoradas na próxima etapa do módulo veicular. Neste momento está em demonstração.";
        } else if (hasAny(q, "bateria", "tensao da bateria", "voltagem", "carregando")) {
            answer = "A tensão da bateria pode ser lida pelo sistema do carro. O aplicativo está pronto para receber esse dado.";
        } else if (hasAny(q, "bluetooth", "conectar na central", "conectar o carro", "conectar car kit", "abrir bluetooth")) {
            openBluetoothSettings();
            answer = "Abrindo as configurações Bluetooth para você conectar a CAR-KIT.";
        } else if (hasAny(q, "ligar farol", "ligar farois", "farol alto", "luz alta", "luz baixa", "ligar luz", "luz do farol", "ligar os farois", "liga farol", "liga farois")) {
            answer = "Função do veículo em demonstração. O sistema está preparado para receber o comando real do carro quando houver integração com o módulo.";
        } else if (hasAny(q, "ligar limpador", "ligar limpador de parabrisas", "limpador de para brisas", "limpador", "liga limpador", "ligar para brisas")) {
            answer = "O comando de limpador de para-brisas está em modo de demonstração para integração real do veículo.";
        } else if (hasAny(q, "ligar carro", "ligar o carro", "acionar carro", "iniciar carro", "partir carro")) {
            answer = "O acionamento real do motor exige integração com o sistema do veículo. Neste momento está em modo de demonstração.";
        } else if (hasAny(q, "obrigado", "obrigada")) {
            answer = "Sempre às ordens, Carlos.";
        } else {
            answer = "Entendi. Posso pesquisar isso na internet se você disser: JARB, pesquise " + q + ".";
        }

        learnCommand(q);
        status.setText("JARB: " + answer);
        if (!silentMode) say(answer);
        if (handsFreeMode) {
            status.postDelayed(() -> {
                if (handsFreeMode && !silentMode) {
                    listenAgain();
                }
            }, 700);
        }
        conversation = false;
    }

    private void answerAfterCommand(String answer) {
        status.setText("JARB: " + answer);
        if (!silentMode) say(answer);
        conversation = false;
        if (handsFreeMode && !silentMode) {
            status.postDelayed(() -> {
                if (handsFreeMode && !silentMode) {
                    conversation = true;
                    listenAgain();
                }
            }, 700);
        }
    }

    private boolean isQuietMediaCommand(String text) {
        return hasAny(text, "volume", "som", "musica", "faixa", "tocar", "play",
                "pausar", "pausa", "continuar", "retomar", "proxima", "anterior",
                "bluetooth", "conectar");
    }

    private String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\bpor favor\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean hasAny(String text, String... options) {
        for (String option : options) {
            if (text.contains(option)) return true;
        }
        return false;
    }

    private String removeOptionalAssistantPrefix(String text) {
        return text.replaceFirst("^(ia|assistente)\\s+", "").trim();
    }

    private boolean startsWithAssistantName(String text) {
        return text.matches("^jarb(?:as|s)?($|\\s+).*");
    }

    private String applyLearnedAliases(String text) {
        for (java.util.Map.Entry<String, ?> entry : memory.getAll().entrySet()) {
            if (!entry.getKey().startsWith("alias_")) continue;
            String alias = entry.getKey().substring("alias_".length());
            if (text.contains(alias)) return text.replace(alias, String.valueOf(entry.getValue()));
        }
        return text;
    }

    private void learnCommand(String command) {
        String key = "count_" + command;
        memory.edit().putInt(key, memory.getInt(key, 0) + 1).apply();
        if (learningDirectory != null) {
            File commandFile = new File(learningDirectory, "learned_commands.txt");
            try {
                FileWriter writer = new FileWriter(commandFile, true);
                writer.write(command + System.lineSeparator());
                writer.flush();
                writer.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void say(String text) {
        status.setText("JARB: " + text);
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JARB");
    }

    private void changeVolume(int direction) {
        if (audioManager != null) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0);
        }
    }

    private void sendMediaKey(int keyCode) {
        if (audioManager == null) return;
        long eventTime = System.currentTimeMillis();
        audioManager.dispatchMediaKeyEvent(new KeyEvent(eventTime, eventTime,
                KeyEvent.ACTION_DOWN, keyCode, 0));
        audioManager.dispatchMediaKeyEvent(new KeyEvent(eventTime, eventTime,
                KeyEvent.ACTION_UP, keyCode, 0));
    }

    private void openBluetoothSettings() {
        startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
    }

    private void showMainMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, handsFreeMode ? "Microfone livre: ON" : "Microfone livre: OFF");
        popup.getMenu().add(0, 2, 1, "Abrir Spotify");
        popup.getMenu().add(0, 3, 2, "Abrir clima");
        popup.getMenu().add(0, 4, 3, "Abrir Waze");
        popup.getMenu().add(0, 5, 4, "Abrir Bluetooth");
        popup.getMenu().add(0, 6, 5, "Limpar aprendizado");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                toggleHandsFreeMode();
                return true;
            }
            if (item.getItemId() == 2) {
                openSpotify();
                return true;
            }
            if (item.getItemId() == 3) {
                openWeatherSearch("previsao do tempo hoje");
                return true;
            }
            if (item.getItemId() == 4) {
                openWaze("local atual");
                return true;
            }
            if (item.getItemId() == 5) {
                openBluetoothSettings();
                return true;
            }
            if (item.getItemId() == 6) {
                clearLearnedData();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void toggleHandsFreeMode() {
        handsFreeMode = !handsFreeMode;
        if (handsFreeMode) {
            silentMode = false;
            status.setText("Microfone livre ativado.");
            say("Microfone livre ativado.");
            status.postDelayed(() -> {
                if (handsFreeMode) listenAgain();
            }, 600);
        } else {
            status.setText("Microfone livre desligado.");
            say("Microfone livre desligado.");
        }
    }

    private void clearLearnedData() {
        if (learningDirectory != null && learningDirectory.exists()) {
            File[] files = learningDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        memory.edit().clear().apply();
        status.setText("Aprendizado local limpo.");
        say("Aprendizado local limpo.");
    }

    private void logInteraction(String text) {
        if (learningDirectory == null || !learningDirectory.exists()) return;
        try {
            File logFile = new File(learningDirectory, "interactions.txt");
            FileWriter writer = new FileWriter(logFile, true);
            writer.write(text + System.lineSeparator());
            writer.flush();
            writer.close();
        } catch (Exception ignored) {
        }
    }

    private boolean isNavigationCommand(String text) {
        return text.startsWith("ir para ") || text.startsWith("me leve para ")
                || text.startsWith("navegar para ") || text.startsWith("navegue para ")
                || text.startsWith("abrir rota para ") || text.startsWith("rota para ")
                || text.startsWith("waze para ") || text.startsWith("va para ")
                || text.startsWith("vou para ");
    }

    private String extractDestination(String text) {
        return text.replaceFirst("^(ir para|me leve para|navegar para|navegue para|abrir rota para|rota para|waze para|va para|vou para)\\s*", "").trim();
    }

    private void openWaze(String destination) {
        String encodedDestination = Uri.encode(destination);
        Uri destinationUri = Uri.parse("https://waze.com/ul?q=" + encodedDestination + "&navigate=yes");
        Intent wazeIntent = new Intent(Intent.ACTION_VIEW, destinationUri);
        wazeIntent.setPackage("com.waze");
        try {
            startActivity(wazeIntent);
        } catch (Exception ignored) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("waze://ul?q=" + encodedDestination + "&navigate=yes")));
            } catch (Exception ignoredAgain) {
                startActivity(new Intent(Intent.ACTION_VIEW, destinationUri));
            }
        }
    }

    private void openSpotify() {
        Intent spotify = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
        if (spotify != null) {
            startActivity(spotify);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/")));
        }
    }

    private void openSpotifySearch(String query) {
        String safeQuery = query == null ? "" : query.trim();
        if (safeQuery.isEmpty()) {
            openSpotify();
            return;
        }

        try {
            Intent spotifySearch = new Intent(Intent.ACTION_VIEW);
            spotifySearch.setPackage("com.spotify.music");
            spotifySearch.setData(Uri.parse("spotify:search:" + Uri.encode(safeQuery)));
            startActivity(spotifySearch);
        } catch (Exception ignored) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://open.spotify.com/search/" + Uri.encode(safeQuery))));
        }
    }

    private String extractMusicQuery(String text) {
        String cleaned = text.trim();
        String[] patterns = {
                "coloque a musica ", "toque a musica ", "tocar a musica ", "toca a musica ",
                "coloca a musica ", "procure a musica ", "pesquise a musica ", "abrir musica ",
                "musica do spotify ", "musica ", "música ", "toque ", "toca ", "coloque ", "procure ", "pesquise "
        };

        for (String pattern : patterns) {
            if (cleaned.startsWith(pattern)) {
                return removeSpotifySuffix(cleaned.substring(pattern.length()).trim());
            }
        }

        String regexPrefix = "^(coloque|toque|toca|coloca|procure|pesquise|abrir|reproduzir|play)\\s+(a\\s+)?(musica|música|faixa)\\s*";
        String withoutPrefix = cleaned.replaceFirst(regexPrefix, "").trim();
        return removeSpotifySuffix(withoutPrefix);
    }

    private String removeSpotifySuffix(String text) {
        return text.replaceFirst("\\s+(no|na|em)\\s+spotify$", "").trim();
    }

    private String formatCurrentTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(java.util.Calendar.getInstance().getTime());
    }

    private void openWeatherSearch(String query) {
        String safeQuery = query == null ? "previsao do tempo hoje" : query;
        safeQuery = safeQuery.replaceFirst("^(como esta|como está|clima|tempo|vai chover|chuva|previsao do tempo)\\s*", "").trim();
        if (safeQuery.isEmpty()) safeQuery = "previsao do tempo hoje";
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=" + Uri.encode("previsão do tempo " + safeQuery))));
    }

    private void openWebSearch(String query) {
        String safeQuery = query.isEmpty() ? "Gol 1999" : query;
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=" + Uri.encode(safeQuery))));
    }

    @Override protected void onDestroy() {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.destroy();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        unregisterReceiver(overlayListenReceiver);
        super.onDestroy();
    }

    private String greetingForTime() {
        java.util.Calendar now = java.util.Calendar.getInstance();
        int hour = now.get(java.util.Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Bom dia" : hour < 18 ? "Boa tarde" : "Boa noite";
        String preferredName = memory.getString("preferred_name", "Carlos");
        return greeting + ", " + preferredName + ". Como posso ajudar?";
    }

    @Override public void onInit(int result) {
        if (result == TextToSpeech.SUCCESS) {
            Locale portugueseBrazil = new Locale("pt", "BR");
            tts.setLanguage(portugueseBrazil);
            tts.setVoice(findMalePortugueseVoice(portugueseBrazil));
            tts.setPitch(0.88f);
            tts.setSpeechRate(0.90f);
        }
    }

    private Voice findMalePortugueseVoice(Locale locale) {
        Voice fallback = tts.getVoice();
        for (Voice voice : tts.getVoices()) {
            String name = voice.getName().toLowerCase(Locale.ROOT);
            if (voice.getLocale().equals(locale) &&
                    (name.contains("male") || name.contains("masculine") || name.contains("homem"))) {
                return voice;
            }
        }
        return fallback;
    }

}
